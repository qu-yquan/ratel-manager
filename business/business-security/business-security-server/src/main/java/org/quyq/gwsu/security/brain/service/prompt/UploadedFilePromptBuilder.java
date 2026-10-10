package org.quyq.gwsu.security.brain.service.prompt;

import io.agentscope.core.message.Msg;
import org.quyq.gwsu.common.ai.agui.converter.CustomAguiMessageConverter;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.MessageContent;
import io.agentscope.core.agui.model.AudioInputContent;
import io.agentscope.core.agui.model.DocumentInputContent;
import io.agentscope.core.agui.model.ImageInputContent;
import io.agentscope.core.agui.model.VideoInputContent;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class UploadedFilePromptBuilder {

    private static final int MAX_FILE_COUNT = 10;

    private UploadedFilePromptBuilder() {
    }

    public static String build(List<AguiMessage> currentMessages, List<Msg> historyMessages) {
        List<FileInfoItem> merged = new ArrayList<>();
        if (historyMessages != null) {
            for (Msg historyMessage : historyMessages) {
                merged.addAll(extractFromHistoryMessage(historyMessage));
            }
        }

        AguiMessage latestUserMessage = findLatestUserMessage(currentMessages);
        if (latestUserMessage != null) {
            merged.addAll(extractFromCurrentMessage(latestUserMessage));
        }

        LinkedHashMap<String, FileInfoItem> uniqueItems = new LinkedHashMap<>();
        for (FileInfoItem item : merged) {
            if (!StringUtils.hasText(item.fileId())) {
                continue;
            }
            uniqueItems.remove(item.fileId());
            uniqueItems.put(item.fileId(), item);
        }

        List<FileInfoItem> orderedItems = new ArrayList<>(uniqueItems.values());
        if (orderedItems.size() > MAX_FILE_COUNT) {
            orderedItems = new ArrayList<>(
                    orderedItems.subList(orderedItems.size() - MAX_FILE_COUNT, orderedItems.size())
            );
        }
        return toPrompt(orderedItems);
    }

    private static AguiMessage findLatestUserMessage(List<AguiMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int index = messages.size() - 1; index >= 0; index--) {
            AguiMessage message = messages.get(index);
            if (message != null && message.isUserMessage()) {
                return message;
            }
        }
        return null;
    }

    private static List<FileInfoItem> extractFromCurrentMessage(AguiMessage message) {
        if (message == null || !(message.getContent() instanceof MessageContent.Blocks partsContent)) {
            return List.of();
        }
        return extractFromPartObjects(partsContent.parts());
    }

    private static List<FileInfoItem> extractFromHistoryMessage(Msg message) {
        if (message == null || message.getMetadata() == null || message.getMetadata().isEmpty()) {
            return List.of();
        }
        Object originalContent = message.getMetadata().get(CustomAguiMessageConverter.METADATA_AGUI_ORIGINAL_CONTENT);
        if (originalContent instanceof MessageContent.Blocks partsContent) {
            return extractFromPartObjects(partsContent.parts());
        }
        if (originalContent instanceof Map<?, ?> originalContentMap) {
            Object parts = originalContentMap.get("parts");
            if (parts instanceof List<?> partList) {
                return extractFromPartObjects(partList);
            }
        }
        return List.of();
    }

    private static List<FileInfoItem> extractFromPartObjects(List<?> parts) {
        if (parts == null || parts.isEmpty()) {
            return List.of();
        }
        List<FileInfoItem> items = new ArrayList<>();
        for (Object part : parts) {
            FileInfoItem item = extractFromPart(part);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private static FileInfoItem extractFromPart(Object part) {
        if (part instanceof DocumentInputContent documentPart) {
            return buildFileInfo(documentPart.metadata(), resolveMimeType(documentPart.source()));
        }
        if (part instanceof ImageInputContent imagePart) {
            return buildFileInfo(imagePart.metadata(), resolveMimeType(imagePart.source()));
        }
        if (part instanceof AudioInputContent audioPart) {
            return buildFileInfo(audioPart.metadata(), resolveMimeType(audioPart.source()));
        }
        if (part instanceof VideoInputContent videoPart) {
            return buildFileInfo(videoPart.metadata(), resolveMimeType(videoPart.source()));
        }
        if (part instanceof Map<?, ?> partMap) {
            Object metadata = partMap.get("metadata");
            Object source = partMap.get("source");
            if (metadata instanceof Map<?, ?> metadataMap) {
                return buildFileInfo(castMap(metadataMap), resolveMimeType(source));
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    private static FileInfoItem buildFileInfo(Map<String, Object> metadata, String mediaType) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        String fileId = stringValue(metadata.get("fileId"));
        if (!StringUtils.hasText(fileId)) {
            fileId = stringValue(metadata.get("id"));
        }
        String fileName = stringValue(metadata.get("fileName"));
        if (!StringUtils.hasText(fileName)) {
            fileName = stringValue(metadata.get("filename"));
        }
        if (!StringUtils.hasText(fileId) || !StringUtils.hasText(fileName)) {
            return null;
        }
        return new FileInfoItem(fileId, fileName, resolveFileSuffix(fileName), mediaType);
    }

    private static String resolveMimeType(Object source) {
        if (source == null) {
            return "";
        }
        try {
            Object mimeType = source.getClass().getMethod("mimeType").invoke(source);
            return stringValue(mimeType);
        } catch (Exception ignored) {
            if (source instanceof Map<?, ?> sourceMap) {
                return stringValue(sourceMap.get("mimeType"));
            }
            return "";
        }
    }

    private static String resolveFileSuffix(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }

    private static String stringValue(Object value) {
        return Objects.toString(value, "");
    }

    private static String toPrompt(List<FileInfoItem> items) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("<fileInfos>\n");
        prompt.append("规则：\n");
        prompt.append("- 仅可使用以下文件信息进行上传操作。\n");
        prompt.append("- 文件按上传顺序排列，序号越大表示上传越晚。\n");
        if (items.isEmpty()) {
            prompt.append("- 当前没有可用的已上传文件。\n");
        }
        for (int index = 0; index < items.size(); index++) {
            FileInfoItem item = items.get(index);
            prompt.append("<fileItem order=\"")
                    .append(index + 1)
                    .append("\">fileId=")
                    .append(item.fileId())
                    .append(",fileName=")
                    .append(item.fileName())
                    .append(",fileSuffix=")
                    .append(item.fileSuffix())
                    .append(",mediaType=")
                    .append(item.mediaType())
                    .append("</fileItem>\n");
        }
        prompt.append("</fileInfos>");
        return prompt.toString();
    }

    private record FileInfoItem(String fileId, String fileName, String fileSuffix, String mediaType) {
    }
}
