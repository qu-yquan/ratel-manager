package org.quyq.gwsu.common.ai.agui.utils;


import io.agentscope.core.message.ToolResultBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.web.WebToolInfo;
import org.quyq.gwsu.common.ai.agui.web.WebToolStatus;
import org.quyq.gwsu.common.ai.agui.web.WebToolTask;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Web工具执行服务
 * 封装：生成toolCallId → 发送CUSTOM事件 → Redis存储 → 信号量等待 → 返回结果
 * <p>
 * 通过Spring注入使用，操作Redis统一使用CacheUtils
 */
@Slf4j
@RequiredArgsConstructor
public class WebToolUtils {


    /**
     * Redis key 前缀
     */
    private static final String KEY_PREFIX = "web-tool:";

    /**
     * 信号量 key 前缀
     */
    private static final String SEMAPHORE_PREFIX = "web-tool:sem:";

    /**
     * 默认超时时间（秒）
     */
    private static final long DEFAULT_TIMEOUT_SECONDS = 60;

    /**
     * Redis TTL（秒），大于超时时间确保回调时数据仍存在
     */
    private static final long REDIS_TTL_SECONDS = 120;


    private final CacheUtils cacheUtils;

    /**
     * 通知web端执行指定工具，阻塞等待前端结果后返回
     *
     * @param wrapper  工具发射器
     * @param toolName 工具名称
     * @param params   工具参数
     * @return 工具执行结果
     */
    public ToolResultBlock webExecuteTool(AIRunnerInstanceWrapper wrapper, String toolName,
                                          Map<String, Object> params) {
        return webExecuteTool(wrapper, toolName, params, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 通知web端执行指定工具，阻塞等待前端结果后返回
     *
     * @param wrapper        工具发射器
     * @param toolName       工具名称
     * @param params         工具参数
     * @param timeoutSeconds 超时时间（秒）
     * @return 工具执行结果
     */
    public ToolResultBlock webExecuteTool(AIRunnerInstanceWrapper wrapper, String toolName,
                                          Map<String, Object> params,
                                          long timeoutSeconds) {
        if (Objects.isNull(wrapper)) {
            throw new RuntimeException("AI runner instance wrapper is null");
        }
        String toolCallId = UUID.randomUUID().toString();

        // 1. 发送CUSTOM事件给前端（通过ToolEmitter → Hook → SSE）
        WebToolInfo info = new WebToolInfo(toolCallId, toolName, params);

        AguiEvent.Custom customAguiEvent = new AguiEvent.Custom(
                wrapper.input().getThreadId(), wrapper.input().getRunId(),
                AIConstants.AguiCustomEvent.TOOL_EXECUTE
                , info);


        // 2. Redis存储任务信息
        String taskKey = KEY_PREFIX + toolCallId;
        cacheUtils.set(taskKey, WebToolTask.pending(toolCallId, toolName, params), REDIS_TTL_SECONDS, TimeUnit.SECONDS);

        wrapper.sendEvent(customAguiEvent);

        // 3. 信号量等待前端结果
        String semKey = SEMAPHORE_PREFIX + toolCallId;
        boolean acquired = cacheUtils.tryAcquirePermit(semKey, 1, timeoutSeconds, TimeUnit.SECONDS);
        // 信号量key设置过期时间，防止Redis残留
        cacheUtils.expire(semKey, REDIS_TTL_SECONDS, TimeUnit.SECONDS);

        if (!acquired) {
            // 超时：更新Redis状态
            WebToolTask task = cacheUtils.get(taskKey);
            if (task != null) {
                cacheUtils.set(taskKey, task.withResult(WebToolStatus.TIMEOUT, "执行超时"), 60, TimeUnit.SECONDS);
            }
            throw new RuntimeException("执行超时");
        }

        // 4. 获取并返回结果
        WebToolTask task = cacheUtils.get(taskKey);
        if (task == null) {
            throw new AgentException("工具执行结果丢失");
        }

        if (task.status() == WebToolStatus.SUCCESS) {
            return ToolResultBlock.text(task.result());
        } else {
            throw new AgentException(task.result());
        }
    }

    /**
     * 处理前端工具执行结果回调
     *
     * @return 处理结果
     */
    public boolean handleCallback(String toolCallId, boolean success, String result) {
        String taskKey = KEY_PREFIX + toolCallId;
        WebToolTask task = cacheUtils.get(taskKey);

        if (task == null) {
            log.warn("无效的toolCallId: {}", toolCallId);
            return false;
        }

        if (task.status() != WebToolStatus.PENDING) {
            log.warn("工具任务已处理: {}", toolCallId);
            return false;
        }

        // 更新状态和结果
        WebToolStatus status = success ? WebToolStatus.SUCCESS : WebToolStatus.FAILED;
        cacheUtils.set(taskKey, task.withResult(status, result), 60, TimeUnit.SECONDS);

        // 释放信号量，唤醒等待的webExecuteTool()
        String semKey = SEMAPHORE_PREFIX + toolCallId;
        cacheUtils.releasePermit(semKey, 1);

        return true;
    }

    /**
     * 获取Redis key前缀
     */
    public static String getKeyPrefix() {
        return KEY_PREFIX;
    }
}
