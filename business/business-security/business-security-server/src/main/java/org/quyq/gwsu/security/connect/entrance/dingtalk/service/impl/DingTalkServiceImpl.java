package org.quyq.gwsu.security.connect.entrance.dingtalk.service.impl;


import cn.hutool.core.util.IdUtil;
import com.dingtalk.open.app.api.models.bot.ChatbotMessage;
import com.dingtalk.open.app.api.models.bot.MessageContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.IAccountInfoClientApi;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.HeadlessResourceDTO;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.kit.api.file.dto.FileProperty;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkMessage;
import org.quyq.gwsu.security.connect.entrance.dingtalk.domain.DingTalkUser;
import org.quyq.gwsu.security.connect.entrance.dingtalk.enums.DingTalkMsgType;
import org.quyq.gwsu.security.connect.entrance.dingtalk.enums.MsgSourceType;
import org.quyq.gwsu.security.connect.entrance.dingtalk.service.IDingTalkService;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkCardUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.utils.DingTalkMsgUtils;
import org.quyq.gwsu.security.connect.entrance.dingtalk.vo.UserStaffIdMappingInfo;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DingTalkServiceImpl implements IDingTalkService {

    private static final Pattern CONTENT_DISPOSITION_FILENAME_STAR_PATTERN = Pattern.compile("filename\\*=([^']*)''([^;]+)");

    private static final Pattern CONTENT_DISPOSITION_FILENAME_PATTERN = Pattern.compile("filename=\"?([^\";]+)\"?");

    private final DingTalkMsgUtils dingTalkMsgUtils;

    private final DingTalkCardUtils dingTalkCardUtils;

    private final CacheUtils cacheUtils;

    private final IAccountInfoClientApi accountInfoClientApi;

    private final HeadlessClientApi headlessClientApi;

    private final static String DING_TALK_USER_MAPPING = "dingtalk:user_mapping:";

    private final String errMsg = "出错啦，请联系管理员";

    @Override
    public void call(ChatbotMessage chatbotMessage) {
        String staffId = chatbotMessage.getSenderStaffId();

        try {
            UserStaffIdMappingInfo mappingInfo = getUserMappingInfo(staffId);

            if (!StringUtils.hasText(mappingInfo.getSubjectId())) {
                dingTalkMsgUtils.toMessages(Collections.singletonList(staffId), DingTalkMessage.sampleText()
                        .content("⚠️该账号暂时未和系统用户绑定 ，请到管理系统系统绑定或联系管理员😊")
                        .build());
                return;
            }
            agentCall(MsgSourceType.getMsgSourceType(chatbotMessage.getConversationType()), mappingInfo, getContent(chatbotMessage));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dingTalkMsgUtils.toMessages(Collections.singletonList(staffId), DingTalkMessage.sampleText()
                    .content("出错啦，请联系管理员")
                    .build());
        }

    }


    protected void agentCall(MsgSourceType sourceType, UserStaffIdMappingInfo mappingInfo, UserContent content) {

        String outTrackId = IdUtil.getSnowflakeNextIdStr();

        AtomicReference<HeadlessAgentStatus> status = new AtomicReference<>(HeadlessAgentStatus.CONNECTION);
        String[] errInfo = new String[1];
        AtomicReference<String> imageUrlRef = new AtomicReference<>();
        AtomicReference<String> videoUrlRef = new AtomicReference<>();

        AtomicInteger progress = new AtomicInteger(0);
        AtomicReference<Map<String, String>> lastParam = new AtomicReference<>(buildParam(HeadlessAgentStatus.CONNECTION, 0, null, null));

        //发送卡片
        dingTalkCardUtils.sendCard(outTrackId, sourceType, mappingInfo.getStaffId(), lastParam.get());

        AssistantResponse response = new AssistantResponse();
        headlessClientApi.stream(mappingInfo.getSubjectId(), SIGN, buildHeadlessDTO(content))
                .doOnNext(event -> {
                    if (status.get() == HeadlessAgentStatus.ERROR) {
                        return;
                    }

                    if (event instanceof AguiEvent.Custom custom) {
                        if ("status".equals(custom.name())) {
                            HeadlessAgentStatus currentStatus = parseStatus(custom);
                            if (currentStatus != null) {
                                status.set(currentStatus);
                                setProgress(progress, status.get());
                                if (HeadlessAgentStatus.CONNECTION == status.get()) {
                                    dingTalkCardUtils.streamAiCard(outTrackId, "content", "loading", false);
                                }
                                refreshCard(outTrackId, lastParam, status.get(), progress.get(), imageUrlRef.get(), videoUrlRef.get());
                            }
                        } else if ("output_image".equals(custom.name())) {
                            imageUrlRef.set(extractMediaUrl(custom.value()));
                            refreshCard(outTrackId, lastParam, status.get(), progress.get(), imageUrlRef.get(), videoUrlRef.get());
                        } else if ("output_video".equals(custom.name())) {
                            videoUrlRef.set(extractMediaUrl(custom.value()));
                            refreshCard(outTrackId, lastParam, status.get(), progress.get(), imageUrlRef.get(), videoUrlRef.get());
                        }
                    } else if (event instanceof AguiEvent.TextMessageContent textEvent) {
                        String tmp = response.cacheContent(textEvent.delta());
                        if (StringUtils.hasText(tmp)) {
                            dingTalkCardUtils.streamAiCard(outTrackId, "content", tmp, false);
                        }
                    } else if (event instanceof AguiEvent.Raw rawEvent) {
                        if (HeadlessAgentStatus.ERROR == status.get() || HeadlessAgentStatus.BUSY == status.get()) {
                            errInfo[0] = extractRawMessage(rawEvent.rawEvent());
                            log.error("智能体返回错误: {}", errInfo[0]);
                        }
                    }

                    if (HeadlessAgentStatus.ERROR == status.get() || HeadlessAgentStatus.BUSY == status.get()) {
                        return;
                    }

                })
                .doOnComplete(() -> {
                    HeadlessAgentStatus tmp = status.get();
                    String msg = response.getContent();
                    if (HeadlessAgentStatus.ERROR == tmp) {
                        msg = errMsg;
                    } else if (HeadlessAgentStatus.BUSY == tmp) {
                        msg = errInfo[0];
                    }
                    dingTalkCardUtils.streamAiCard(outTrackId, "content", msg, true);
                })
                .doOnError(throwable -> {
                    log.error(throwable.getMessage(), throwable);
                    dingTalkCardUtils.streamAiCard(outTrackId, "content", errMsg, true);
                })
                .subscribe();

    }


    private HeadlessDTO buildHeadlessDTO(UserContent content) {
        StringBuilder text = new StringBuilder();
        List<HeadlessResourceDTO> resources = new ArrayList<>();

        MessageContent mc = content.content;
        switch (content.type) {
            case TEXT:
                text = new StringBuilder(mc.getContent());
                break;
            case RICHTEXT:
                for (MessageContent temp : mc.getRichText()) {
                    if(StringUtils.hasText(temp.getText())){
                        text.append(temp.getText());
                    }else if(StringUtils.hasText(temp.getDownloadCode())){
                        resources.add(saveToFileServer(dingTalkMsgUtils.getFileDownloadUrl(temp.getDownloadCode()) , temp.getFileName()));
                    }
                }
                break;
            case AUDIO:
                text = new StringBuilder(mc.getRecognition());
                break;
            case PICTURE,FILE,VIDEO:
                text.append("<systemInfo>用户上传了一个资源文件，请等待用户进一步指示<systemInfo>");
                resources.add(saveToFileServer(dingTalkMsgUtils.getFileDownloadUrl(mc.getDownloadCode()) , mc.getFileName()));
                break;

        }

        return new HeadlessDTO(text.toString(), resources, null, null, null);
    }

    private HeadlessAgentStatus parseStatus(AguiEvent.Custom custom) {
        if (!(custom.value() instanceof Map<?, ?> valueMap)) {
            return null;
        }
        Object statusValue = valueMap.get("status");
        if (statusValue == null) {
            return null;
        }
        try {
            return HeadlessAgentStatus.valueOf(String.valueOf(statusValue));
        } catch (IllegalArgumentException e) {
            log.warn("未知的 headless status: {}", statusValue);
            return null;
        }
    }

    private String extractMediaUrl(Object value) {
        if (!(value instanceof Map<?, ?> valueMap)) {
            return null;
        }
        Object url = valueMap.get("url");
        if (url == null) {
            return null;
        }
        String urlStr = String.valueOf(url);
        if (urlStr.startsWith("http://") || urlStr.startsWith("https://")) {
            return urlStr;
        }
        if (urlStr.startsWith("/")) {
            BaseProjectInfoProperties properties = ConfigInfoUtils.getByObject(
                    BaseProjectInfoProperties.CONFIG_KEY, BaseProjectInfoProperties.class);
            return properties.apiBaseUrl() + urlStr;
        }
        return urlStr;
    }

    private String extractRawMessage(Object rawValue) {
        if (rawValue instanceof Map<?, ?> rawMap) {
            Object message = rawMap.get("message");
            if (message != null) {
                return String.valueOf(message);
            }
        }
        return rawValue != null ? String.valueOf(rawValue) : errMsg;
    }

    private HeadlessResourceDTO saveToFileServer(String fileDownloadUrl , String fileName){
        Path tempFile = null;
        try {
            URLConnection connection = new URL(fileDownloadUrl).openConnection();
            connection.connect();

            String contentDisposition = connection.getHeaderField("Content-Disposition");
            String resolvedFileName = resolveFileName(contentDisposition, fileName, connection.getContentType());
            String suffix = getFileSuffix(resolvedFileName, connection.getContentType());
            tempFile = Files.createTempFile("dingtalk_", suffix);

            try (InputStream inputStream = connection.getInputStream()) {
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            KitFileInfoVO fileInfo = FileUtils.upload(tempFile.toFile(), buildFileProperty());
            return new HeadlessResourceDTO(fileInfo.getFileId());
        } catch (Exception e) {
            log.error("钉钉文件下载并上传失败, fileDownloadUrl={}", fileDownloadUrl, e);
            throw new RuntimeException("钉钉文件保存到文件服务失败", e);
        } finally {
            deleteQuietly(tempFile);
        }
    }

    private FileProperty buildFileProperty() {
        return FileProperty.builder()
                .categorize("dingtalk")
                .scopePublic()
                .expiredTime(LocalDateTime.now().plusMonths(1))
                .build();
    }

    private String resolveFileName(String contentDisposition, String fileName, String contentType) {
        String headerFileName = extractFileNameFromContentDisposition(contentDisposition);
        if (StringUtils.hasText(headerFileName)) {
            return headerFileName;
        }
        if (StringUtils.hasText(fileName)) {
            return fileName;
        }
        return IdUtil.fastSimpleUUID() + getFileSuffix(null, contentType);
    }

    private String extractFileNameFromContentDisposition(String contentDisposition) {
        if (!StringUtils.hasText(contentDisposition)) {
            return null;
        }

        Matcher filenameStarMatcher = CONTENT_DISPOSITION_FILENAME_STAR_PATTERN.matcher(contentDisposition);
        if (filenameStarMatcher.find()) {
            String charset = filenameStarMatcher.group(1);
            String encodedFileName = filenameStarMatcher.group(2);
            try {
                Charset decodeCharset = StringUtils.hasText(charset) ? Charset.forName(charset) : StandardCharsets.UTF_8;
                return URLDecoder.decode(encodedFileName, decodeCharset);
            } catch (Exception e) {
                log.warn("解析 Content-Disposition filename* 失败: {}", contentDisposition, e);
            }
        }

        Matcher filenameMatcher = CONTENT_DISPOSITION_FILENAME_PATTERN.matcher(contentDisposition);
        if (filenameMatcher.find()) {
            return filenameMatcher.group(1);
        }
        return null;
    }

    private String getFileSuffix(String fileName, String contentType) {
        if (StringUtils.hasText(fileName)) {
            int index = fileName.lastIndexOf('.');
            if (index >= 0 && index < fileName.length() - 1) {
                return fileName.substring(index);
            }
        }
        if (!StringUtils.hasText(contentType)) {
            return ".tmp";
        }
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "audio/mpeg" -> ".mp3";
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "application/pdf" -> ".pdf";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "text/plain" -> ".txt";
            default -> ".tmp";
        };
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("清理临时文件失败: {}", path, e);
        }
    }

    private void setProgress(AtomicInteger progress, HeadlessAgentStatus status) {
        int p = progress.get();
        if (HeadlessAgentStatus.CONNECTION == status) {
            progress.set(0);
        } else if (HeadlessAgentStatus.INITING == status) {
            progress.set(20);
        } else if (HeadlessAgentStatus.THINKING == status && p < 40) {
            progress.set(40);
        } else if (HeadlessAgentStatus.OUTPUTTING == status) {
            if (p < 90) {
                progress.set(p + 5);
            }
        } else if (HeadlessAgentStatus.SHOWING == status) {
            progress.set(90);
        } else if (HeadlessAgentStatus.COMPLETE == status) {
            progress.set(100);
        }
    }

    private Map<String, String> buildParam(HeadlessAgentStatus status, int progress, String imageUrl, String videoUrl) {
        Map<String, String> param = new HashMap<>();
        param.put("status", status.getStatus());
        param.put("image_url", imageUrl);
        param.put("video_url", videoUrl);
        param.put("progress", progress + "");
        param.put("newChat", "true");
        return param;
    }

    private void refreshCard(String outTrackId, AtomicReference<Map<String, String>> lastParam,
                             HeadlessAgentStatus status, int progress, String imageUrl, String videoUrl) {
        Map<String, String> params = buildParam(status, progress, imageUrl, videoUrl);
        if (!lastParam.get().equals(params)) {
            lastParam.set(params);
            dingTalkCardUtils.updateCard(outTrackId, params);
        }
    }


    /**
     * 通过钉钉staffId找到映射的本系统userId
     *
     * @param staffId
     * @return
     */
    public UserStaffIdMappingInfo getUserMappingInfo(String staffId) {
        UserStaffIdMappingInfo info = cacheUtils.get(DING_TALK_USER_MAPPING + staffId);
        if (Objects.nonNull(info) && StringUtils.hasText(info.getSubjectId())) {
            return info;
        }

        if (Objects.isNull(info) || !StringUtils.hasText(info.getUnionId())) {
            DingTalkUser dingUserInfo = dingTalkMsgUtils.getDingUserInfo(staffId);
            AssertUtils.hasText(dingUserInfo.getUnionId(), SecurityErrorCode.E07010);

            info = new UserStaffIdMappingInfo();
            info.setStaffId(staffId);
            info.setUnionId(dingUserInfo.getUnionId());
        }


        try {
            String userId = FeignUtils.data(accountInfoClientApi.getUserIdByDingTalkUnionId(info.getUnionId()));
            if (!StringUtils.hasText(userId)) {
                return info;
            }
            info.setSubjectId(userId);

            return info;
        } finally {
            cacheUtils.set(DING_TALK_USER_MAPPING + staffId, info, 30, TimeUnit.DAYS);
        }

    }


    protected UserContent getContent(ChatbotMessage message) {
        DingTalkMsgType type = DingTalkMsgType.getByCode(message.getMsgtype());
        return new UserContent(type,
                DingTalkMsgType.TEXT == type ? message.getText() : message.getContent());
    }


    protected record UserContent(
            DingTalkMsgType type,
            MessageContent content) {
    }

    protected static class AssistantResponse {

        private final StringBuilder think = new StringBuilder();

        private final StringBuilder content = new StringBuilder();

        private final int cacheLength = 10;

        private final AtomicInteger thinkLength = new AtomicInteger(0);
        private final AtomicInteger contentLength = new AtomicInteger(0);


        /**
         * 缓存think内容
         *
         * @param delta
         * @return 返回内容时，需要更新到前端
         */
        public String cacheThink(String delta) {
            think.append(delta);
            if (think.length() - thinkLength.get() > cacheLength) {
                thinkLength.set(think.length());
                return think.toString();
            }
            return null;
        }

        /**
         * 缓存输出内容
         *
         * @param delta
         * @return 返回内容时，需要更新到前端
         */
        public String cacheContent(String delta) {
            content.append(delta);
            if (content.length() - contentLength.get() > cacheLength) {
                contentLength.set(content.length());
                return content.toString();
            }
            return null;
        }

        public String getContent() {
            return content.toString();
        }


    }

}
