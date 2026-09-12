package org.quyq.gwsu.headless.core.session;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.jcodec.api.awt.AWTSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 无头浏览器页面操作包装器
 * <p>
 * 封装 BrowserContext 和 Page 的高级操作，在 HeadlessAgentListener 事件回调中暴露，
 * 供调用方在事件处理过程中对浏览器界面进行操作（截图、录制等）。
 * <p>
 * 功能：
 * 1. 开始屏幕录制（基于定时 page.screenshot()，受 deviceScaleFactor 影响可高 DPI）
 * 2. 结束屏幕录制（将截图帧序列编码为 MP4 视频文件）
 * 3. 获取截图（支持整个页面或指定元素）
 */
@Slf4j
public class HeadlessPageWrapper {

    private final BrowserContext context;
    private final Page page;
    private final ReentrantLock pageOperationLock;

    /** 录制帧率（每秒帧数） */
    private static final int RECORDING_FPS = 2;

    /** 录制截图间隔（毫秒） */
    private static final int RECORDING_INTERVAL_MS = 1000 / RECORDING_FPS;

    /** 是否正在录制 */
    private final AtomicBoolean recording = new AtomicBoolean(false);

    /** 是否已关闭（由 HeadlessBrowserSession.close() 设置） */
    private volatile boolean closed = false;

    /** 录制帧缓冲区（内存中保存 PNG 字节） */
    private final List<byte[]> frameBuffer = Collections.synchronizedList(new ArrayList<>());

    /** 下一帧允许采集的单调时钟时间点 */
    private long nextFrameCaptureNanos;

    HeadlessPageWrapper(BrowserContext context, Page page, ReentrantLock pageOperationLock) {
        this.context = context;
        this.page = page;
        this.pageOperationLock = pageOperationLock;
    }

    /**
     * 标记为已关闭，由 HeadlessBrowserSession.close() 调用
     */
    void markClosed() {
        closed = true;
        recording.set(false);
        frameBuffer.clear();
    }

    /**
     * 检查底层 Playwright 资源是否仍然可用
     */
    private boolean isTargetAlive() {
        pageOperationLock.lock();
        try {
            if (closed) return false;
            try {
                if (page != null && page.isClosed()) return false;
            } catch (com.microsoft.playwright.impl.TargetClosedError e) {
                return false;
            } catch (Exception e) {
                return false;
            }
            return true;
        } finally {
            pageOperationLock.unlock();
        }
    }

    // ==================== 屏幕录制 ====================

    /**
     * 开始屏幕录制
     * <p>
     * 基于 SSE 事件泵线程定时调用 page.screenshot() 实现，截图受 BrowserContext 的 deviceScaleFactor 影响，
     * 设为 2.0 时可获取 Retina 级别的清晰截图。
     * 调用 {@link #stopRecording()} 后会将截图帧序列编码为 MP4 视频文件。
     * <p>
     * 注意：同一时间只能有一个录制会话，重复调用会被忽略。
     */
    public void startRecording() {
        if (!recording.compareAndSet(false, true)) {
            log.warn("录制已在进行中，忽略重复调用");
            return;
        }
        pageOperationLock.lock();
        try {
            if (!isTargetAlive()) {
                recording.set(false);
                log.warn("浏览器已关闭，无法开始屏幕录制");
                return;
            }
            frameBuffer.clear();
            nextFrameCaptureNanos = 0L;
            // 立即采集首帧，避免短任务在下一次事件泵轮询前进入审批状态。
            captureRecordingFrameIfDue();
        } finally {
            pageOperationLock.unlock();
        }

        log.info("屏幕录制已开始 (Playwright事件泵同线程采帧, fps={}, interval={}ms)",
                RECORDING_FPS, RECORDING_INTERVAL_MS);
    }

    /**
     * 在 Playwright 事件泵线程中按录制帧率采集一帧。
     * <p>
     * 调用方必须已持有 {@code pageOperationLock}，确保所有 Page 操作在同一事件泵调用链中串行执行。
     */
    void captureRecordingFrameIfDue() {
        if (!recording.get() || closed) {
            return;
        }

        long nowNanos = System.nanoTime();
        if (nowNanos < nextFrameCaptureNanos) {
            return;
        }
        nextFrameCaptureNanos = nowNanos + RECORDING_INTERVAL_MS * 1_000_000L;

        try {
            if (page == null || page.isClosed()) {
                return;
            }
            frameBuffer.add(page.screenshot());
        } catch (com.microsoft.playwright.impl.TargetClosedError e) {
            log.warn("录制截图时浏览器已关闭: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("录制截图失败: {}", e.getMessage());
        }
    }

    /**
     * 结束屏幕录制并生成 MP4 视频文件
     * <p>
     * 结束采帧，将内存中的截图帧序列使用 JCodec 编码为 MP4 视频文件。
     * 调用方负责在合适时机上传该文件或使用完毕后删除。
     *
     * @return MP4 视频文件，没有正在录制的会话时返回 null
     */
    public File stopRecording() {
        if (!recording.compareAndSet(true, false)) {
            log.warn("没有正在进行的录制，忽略调用");
            return null;
        }

        List<byte[]> frames;
        synchronized (frameBuffer) {
            frames = new ArrayList<>(frameBuffer);
            frameBuffer.clear();
        }

        if (frames.isEmpty()) {
            log.warn("录制期间未捕获到任何截图帧");
            return null;
        }

        log.info("屏幕录制已停止，共捕获 {} 帧", frames.size());

        try {
            Path mp4File = Files.createTempFile("headless-recording-", ".mp4");
            encodeToMp4(frames, mp4File);
            log.info("MP4 视频已生成: {}，大小: {} bytes", mp4File, Files.size(mp4File));
            return mp4File.toFile();
        } catch (Exception e) {
            log.error("结束屏幕录制失败", e);
            throw new RuntimeException("结束屏幕录制失败", e);
        }
    }

    /**
     * 停止录制并丢弃所有截图数据
     * <p>
     * 结束采帧，清空内存中的所有帧数据，不生成视频文件。
     * 适用于录制中途放弃、不需要保存视频的场景。
     */
    public void discardRecording() {
        if (!recording.compareAndSet(true, false)) {
            log.warn("没有正在进行的录制，忽略调用");
            return;
        }

        int discarded;
        synchronized (frameBuffer) {
            discarded = frameBuffer.size();
            frameBuffer.clear();
        }
        log.info("录制已丢弃，共丢弃 {} 帧截图数据", discarded);
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return recording.get();
    }

    // ==================== 截图 ====================

    /**
     * 获取整个浏览器页面的截图
     *
     * @return PNG 格式的截图文件
     */
    public File screenshot() {
        return screenshot(null);
    }

    /** 元素等待超时（毫秒） */
    private static final int ELEMENT_WAIT_TIMEOUT_MS = 5_000;

    /**
     * 获取指定元素的截图
     * <p>
     * 通过 CSS 选择器定位元素，截取该元素渲染后的样式截图（包含所有 CSS 效果）。
     * 如果选择器为 null 或空，则截取整个页面。
     * 指定元素时会先等待元素出现在 DOM 中（最多 {@value #ELEMENT_WAIT_TIMEOUT_MS}ms），
     * 超时则降级为整页截图。
     * <p>
     * 当元素内容超出可见区域（存在滚动条）时，会通过 JavaScript 临时展开元素及其祖先容器，
     * 截取完整内容后再恢复原始样式，从而实现长截图效果。
     *
     * @param selector CSS 选择器，如 "#app"、".chat-container"、"[data-testid='xxx']"
     * @return PNG 格式的截图文件
     */
    public File screenshot(String selector) {
        pageOperationLock.lock();
        try {
            if (!isTargetAlive()) {
                log.warn("浏览器已关闭，无法获取截图");
                return null;
            }
            byte[] bytes;
            if (selector != null && !selector.isEmpty()) {
                Locator locator = page.locator(selector);
                try {
                    locator.waitFor(new Locator.WaitForOptions()
                            .setState(WaitForSelectorState.VISIBLE)
                            .setTimeout(ELEMENT_WAIT_TIMEOUT_MS));
                } catch (Exception e) {
                    log.warn("等待元素出现超时，降级为整页截图: selector={}, error={}", selector, e.getMessage());
                    bytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
                    Path tempFile = Files.createTempFile("headless-screenshot-", ".png");
                    Files.write(tempFile, bytes);
                    return tempFile.toFile();
                }
                bytes = screenshotFullElement(selector, locator);
            } else {
                bytes = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            }
            Path tempFile = Files.createTempFile("headless-screenshot-", ".png");
            Files.write(tempFile, bytes);
            return tempFile.toFile();
        } catch (Exception e) {
            log.error("获取截图失败: selector={}", selector, e);
            throw new RuntimeException("获取截图失败", e);
        } finally {
            pageOperationLock.unlock();
        }
    }

    /**
     * 截取元素的完整内容（长截图）
     * <p>
     * 通过 JavaScript 临时展开目标元素及其所有祖先容器的滚动约束，
     * 使元素的全部内容可见，截取后再恢复原始样式。
     * <p>
     * 处理步骤：
     * 1. 获取目标元素的 scrollHeight，将 height 设为 scrollHeight 以展开全部内容
     * 2. 向上遍历祖先元素，将 overflow/overflowY 设为 visible，防止被裁剪
     * 3. 截取展开后的元素
     * 4. 恢复所有被修改的样式
     *
     * @param selector CSS 选择器
     * @param locator  Playwright Locator
     * @return 截图的 PNG 字节数组
     */
    private byte[] screenshotFullElement(String selector, Locator locator) {
        // 临时展开元素及其祖先，返回用于恢复的 JSON token
        String restoreToken = expandElementForFullScreenshot(selector);

        try {
            // 等待一小段时间让浏览器完成重排
            page.waitForTimeout(100);
            return locator.screenshot(new Locator.ScreenshotOptions());
        } finally {
            // 无论截图成功与否，都必须恢复原始样式
            restoreElementStyles(restoreToken);
        }
    }

    /**
     * 临时展开目标元素及其祖先容器，以便截取完整内容
     * <p>
     * 修改内容：
     * - 目标元素：height → scrollHeight，overflow → visible，overflowY → visible
     * - 祖先元素：overflow → visible，overflowY → visible（仅修改有滚动裁剪的祖先）
     * <p>
     * 返回一个 JSON 字符串 token，包含所有被修改的原始样式，
     * 供 {@link #restoreElementStyles(String)} 恢复使用。
     *
     * @param selector CSS 选择器
     * @return 恢复用的 JSON token；如果元素不存在则返回 null
     */
    private String expandElementForFullScreenshot(String selector) {
        // JavaScript 执行展开操作，返回包含原始样式的 JSON token
        String expandJs = """
                (selector) => {
                    const el = document.querySelector(selector);
                    if (!el) return null;

                    const modifications = [];

                    // 1. 展开目标元素：将 height 设为 scrollHeight，移除滚动约束
                    const targetOriginal = {
                        height: el.style.height,
                        overflow: el.style.overflow,
                        overflowY: el.style.overflowY,
                        minHeight: el.style.minHeight,
                        maxHeight: el.style.maxHeight
                    };
                    modifications.push({ element: el, original: targetOriginal });

                    el.style.height = el.scrollHeight + 'px';
                    el.style.overflow = 'visible';
                    el.style.overflowY = 'visible';
                    el.style.minHeight = '';
                    el.style.maxHeight = '';

                    // 2. 向上遍历祖先元素，移除可能裁剪内容的 overflow 限制
                    let ancestor = el.parentElement;
                    while (ancestor && ancestor !== document.body && ancestor !== document.documentElement) {
                        const computed = window.getComputedStyle(ancestor);
                        // 仅处理有滚动裁剪的祖先（overflow 为 hidden/scroll/auto）
                        if (['hidden', 'scroll', 'auto'].includes(computed.overflow) ||
                            ['hidden', 'scroll', 'auto'].includes(computed.overflowY)) {
                            const ancestorOriginal = {
                                overflow: ancestor.style.overflow,
                                overflowY: ancestor.style.overflowY
                            };
                            modifications.push({ element: ancestor, original: ancestorOriginal });
                            ancestor.style.overflow = 'visible';
                            ancestor.style.overflowY = 'visible';
                        }
                        ancestor = ancestor.parentElement;
                    }

                    // 返回序列化的修改记录（用于恢复）
                    // 因为无法直接传递 DOM 引用，改用路径索引定位元素
                    const serializable = modifications.map(m => {
                        const path = [];
                        let node = m.element;
                        while (node.parentElement) {
                            const siblings = Array.from(node.parentElement.children);
                            path.unshift(siblings.indexOf(node));
                            node = node.parentElement;
                        }
                        return { path, original: m.original };
                    });

                    return JSON.stringify(serializable);
                }
                """;

        Object result = page.evaluate(expandJs, selector);
        return result != null ? result.toString() : null;
    }

    /**
     * 恢复被 {@link #expandElementForFullScreenshot(String)} 修改的元素样式
     * <p>
     * 根据 token 中的 DOM 路径索引重新定位元素，并恢复原始样式属性。
     *
     * @param restoreToken 由 expandElementForFullScreenshot 返回的 JSON token
     */
    private void restoreElementStyles(String restoreToken) {
        if (restoreToken == null || restoreToken.isEmpty()) {
            return;
        }
        try {
            String restoreJs = """
                    (token) => {
                        const modifications = JSON.parse(token);
                        for (const mod of modifications) {
                            // 根据路径索引从 document 重新定位元素
                            let node = document.documentElement;
                            for (const idx of mod.path) {
                                node = node.children[idx];
                                if (!node) break;
                            }
                            if (!node) continue;

                            // 恢复原始样式
                            const orig = mod.original;
                            if ('height' in orig) node.style.height = orig.height;
                            if ('overflow' in orig) node.style.overflow = orig.overflow;
                            if ('overflowY' in orig) node.style.overflowY = orig.overflowY;
                            if ('minHeight' in orig) node.style.minHeight = orig.minHeight;
                            if ('maxHeight' in orig) node.style.maxHeight = orig.maxHeight;
                        }
                    }
                    """;
            page.evaluate(restoreJs, restoreToken);
        } catch (Exception e) {
            log.warn("恢复元素样式失败，页面布局可能需要刷新: {}", e.getMessage());
        }
    }

    // ==================== 文件上传 ====================

    /**
     * 上传文件到文件服务
     * <p>
     * 通过 {@link FileUtils} 将文件上传，返回文件 ID。
     * 上传完成后自动删除本地文件。
     *
     * @param file 要上传的文件（如 screenshot 或 stopRecording 返回的文件）
     * @return 上传后的文件 ID
     */
    public KitFileInfoVO upload(File file) {
        return upload(file, buildProperty());
    }

    /**
     * 上传文件到文件服务（自定义属性）
     *
     * @param file     要上传的文件
     * @param property 文件属性（有效期、权限等）
     * @return 上传后的文件 ID
     */
    public KitFileInfoVO upload(File file, FileProperty property) {
        try {
            KitFileInfoVO fileInfo = FileUtils.upload(file, property);
            log.info("文件已上传，fileId={}, fileName={}", fileInfo.getFileId(), file.getName());
            return fileInfo;
        } catch (Exception e) {
            log.error("文件上传失败: {}", file.getName(), e);
            throw new BusinessException( e);
        } finally {
            deleteQuietly(file.toPath());
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 构建默认文件属性：公共权限、15天有效期
     */
    private FileProperty buildProperty() {
        return FileProperty.builder()
                .categorize("headless")
                .scopePublic()
                .expiredTime(LocalDateTime.now().plusDays(15))
                .build();
    }

    /**
     * 将截图帧序列（内存中的 PNG 字节）编码为 MP4 视频
     */
    private void encodeToMp4(List<byte[]> frames, Path output) throws IOException {
        SeekableByteChannel channel = NIOUtils.writableFileChannel(output.toString());
        AWTSequenceEncoder encoder = null;
        try {
            encoder = AWTSequenceEncoder.createSequenceEncoder(output.toFile(), RECORDING_FPS);
            for (byte[] frameData : frames) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(frameData));
                if (image != null) {
                    encoder.encodeImage(image);
                }
            }
            encoder.finish();
        } catch (Exception e) {
            log.error("视频编码异常", e);
            throw new IOException("视频编码异常", e);
        } finally {
            NIOUtils.closeQuietly(channel);
        }
    }

    /**
     * 静默删除文件
     */
    private void deleteQuietly(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (IOException e) { log.debug("清理临时文件失败", e); }
        }
    }
}
