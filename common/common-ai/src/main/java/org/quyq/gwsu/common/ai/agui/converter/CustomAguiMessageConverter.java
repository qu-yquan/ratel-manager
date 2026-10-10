package org.quyq.gwsu.common.ai.agui.converter;

import io.agentscope.core.message.AudioBlock;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.DataBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.Source;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.URLSource;
import io.agentscope.core.message.VideoBlock;
import io.agentscope.core.util.JsonException;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.config.properties.ModelLlmConfigDTO;
import io.agentscope.core.agui.model.AudioInputContent;
import io.agentscope.core.agui.model.InputContent;
import io.agentscope.core.agui.model.InputContentSource;
import io.agentscope.core.agui.model.InputContentDataSource;
import io.agentscope.core.agui.model.DocumentInputContent;
import io.agentscope.core.agui.model.AguiFunctionCall;
import io.agentscope.core.agui.model.ImageInputContent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.MessageContent;
import io.agentscope.core.agui.model.TextInputContent;
import io.agentscope.core.agui.model.AguiToolCall;
import io.agentscope.core.agui.model.InputContentUrlSource;
import io.agentscope.core.agui.model.VideoInputContent;
import org.quyq.gwsu.common.ai.model.ModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class CustomAguiMessageConverter {

    private static final Logger log = LoggerFactory.getLogger(CustomAguiMessageConverter.class);

    public static final String METADATA_AGUI_ORIGINAL_CONTENT = "agui_original_content";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final RestClient RESOURCE_REST_CLIENT = RestClient.builder().build();

    public Msg toMsg(AguiMessage aguiMessage) {
        ModelLlmConfigDTO llmConfig = readCurrentLlmConfig();
        MsgRole role = convertRole(aguiMessage.getRole());
        List<ContentBlock> blocks = new ArrayList<>();
        Map<String, Object> metadata = new HashMap<>();
        if (aguiMessage.getContent() != null) {
            if (aguiMessage.isToolMessage() && aguiMessage.getToolCallId() != null) {
                blocks.add(ToolResultBlock.of(
                        aguiMessage.getToolCallId(),
                        null,
                        TextBlock.builder().text(Objects.toString(aguiMessage.getTextContent(), "")).build()));
            } else {
                blocks.addAll(toContentBlocks(aguiMessage.getContent(), llmConfig));
            }
            if (aguiMessage.getContent() instanceof MessageContent.Blocks) {
                metadata.put(METADATA_AGUI_ORIGINAL_CONTENT, aguiMessage.getContent());
            }
        }
        if (aguiMessage.hasToolCalls()) {
            for (AguiToolCall tc : aguiMessage.getToolCalls()) {
                blocks.add(toToolUseBlock(tc));
            }
        }
        return Msg.builder()
                .id(aguiMessage.getId())
                .role(role)
                .content(blocks)
                .metadata(metadata)
                .build();
    }

    public AguiMessage toAguiMessage(Msg msg) {
        String role = convertRole(msg.getRole());
        StringBuilder textContent = new StringBuilder();
        List<AguiToolCall> toolCalls = new ArrayList<>();
        String toolCallId = null;
        MessageContent originalContent = readOriginalContent(msg);
        List<InputContent> parts = new ArrayList<>();

        for (ContentBlock block : msg.getContent()) {
            if (block instanceof TextBlock tb) {
                if (originalContent == null) {
                    parts.add(new TextInputContent(tb.getText()));
                }
                if (!textContent.isEmpty()) {
                    textContent.append("\n");
                }
                textContent.append(tb.getText());
            } else if (block instanceof ImageBlock imageBlock) {
                parts.add(new ImageInputContent(fromSource(imageBlock.getSource()), Map.of()));
            } else if (block instanceof AudioBlock audioBlock) {
                parts.add(new AudioInputContent(fromSource(audioBlock.getSource()), Map.of()));
            } else if (block instanceof VideoBlock videoBlock) {
                parts.add(new VideoInputContent(fromSource(videoBlock.getSource()), Map.of()));
            } else if (block instanceof DataBlock dataBlock) {
                parts.add(toDocumentPart(dataBlock));
            } else if (block instanceof ToolUseBlock tub) {
                toolCalls.add(toAguiToolCall(tub));
            } else if (block instanceof ToolResultBlock trb) {
                toolCallId = trb.getId();
                for (ContentBlock output : trb.getOutput()) {
                    if (output instanceof TextBlock tb) {
                        if (!textContent.isEmpty()) {
                            textContent.append("\n");
                        }
                        textContent.append(tb.getText());
                    }
                }
            }
        }

        MessageContent content = originalContent != null
                ? originalContent
                : buildContentFromParts(parts, textContent.toString());

        return new AguiMessage(msg.getId(), role, content,
                toolCalls.isEmpty() ? null : toolCalls, toolCallId);
    }

    public List<Msg> toMsgList(List<AguiMessage> aguiMessages) {
        return aguiMessages.stream().map(this::toMsg).collect(Collectors.toList());
    }

    public List<AguiMessage> toAguiMessageList(List<Msg> msgs) {
        return msgs.stream().map(this::toAguiMessage).collect(Collectors.toList());
    }

    private MsgRole convertRole(String role) {
        return switch (role.toLowerCase()) {
            case "assistant" -> MsgRole.ASSISTANT;
            case "system" -> MsgRole.SYSTEM;
            case "tool" -> MsgRole.TOOL;
            default -> MsgRole.USER;
        };
    }

    private String convertRole(MsgRole role) {
        return switch (role) {
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case SYSTEM -> "system";
            case TOOL -> "tool";
        };
    }

    private ToolUseBlock toToolUseBlock(AguiToolCall tc) {
        Map<String, Object> input = parseJsonArguments(tc.getFunction().getArguments());
        return ToolUseBlock.builder().id(tc.getId()).name(tc.getFunction().getName()).input(input).build();
    }

    private AguiToolCall toAguiToolCall(ToolUseBlock tub) {
        String arguments = serializeArguments(tub.getInput());
        AguiFunctionCall function = new AguiFunctionCall(tub.getName(), arguments);
        return new AguiToolCall(tub.getId(), function);
    }

    private Map<String, Object> parseJsonArguments(String arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(arguments, Map.class);
        } catch (JsonException e) {
            return Map.of();
        } catch (RuntimeException e) {
            return Map.of();
        }
    }

    private String serializeArguments(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        try {
            return JsonUtils.getJsonCodec().toJson(arguments);
        } catch (JsonException e) {
            return "{}";
        }
    }

    private List<ContentBlock> toContentBlocks(MessageContent content, ModelLlmConfigDTO llmConfig) {
        List<ContentBlock> blocks = new ArrayList<>();
        if (content instanceof MessageContent.Text(String text)) {
            if (text != null && !text.isEmpty()) {
                blocks.add(TextBlock.builder().text(text).build());
            }
            return blocks;
        }
        if (content instanceof MessageContent.Blocks(List<InputContent> parts)) {
            for (InputContent part : parts) {
                if (part instanceof TextInputContent textPart) {
                    if (textPart.text() != null && !textPart.text().isEmpty()) {
                        blocks.add(TextBlock.builder().text(textPart.text()).build());
                    }
                } else if (part instanceof ImageInputContent imagePart) {
                    blocks.add(ImageBlock.builder().source(toSource(imagePart.source(), llmConfig)).build());
                } else if (part instanceof AudioInputContent audioPart) {
                    blocks.add(AudioBlock.builder().source(toSource(audioPart.source(), llmConfig)).build());
                } else if (part instanceof VideoInputContent videoPart) {
                    blocks.add(VideoBlock.builder().source(toSource(videoPart.source(), llmConfig)).build());
                } else if (part instanceof DocumentInputContent documentPart) {
                    blocks.add(toDataBlock(documentPart, llmConfig));
                }
            }
        }
        return blocks;
    }

    private Source toSource(InputContentSource source, ModelLlmConfigDTO llmConfig) {
        if (source == null) {
            throw new IllegalArgumentException("Media source cannot be null");
        }
        if (source instanceof InputContentDataSource dataSource) {
            return Base64Source.builder()
                    .mediaType(dataSource.mimeType())
                    .data(dataSource.value())
                    .build();
        }
        if (source instanceof InputContentUrlSource urlSource) {
            if (shouldConvertUrlToBase64(llmConfig)) {
                InputContentDataSource dataSource = downloadAsDataSource(urlSource);
                if (dataSource != null) {
                    return Base64Source.builder()
                            .mediaType(dataSource.mimeType())
                            .data(dataSource.value())
                            .build();
                }
            }
            return URLSource.builder()
                    .url(urlSource.value())
                    .build();
        }
        throw new IllegalArgumentException("Unsupported media source type: " + source.getClass().getName());
    }

    private InputContentSource fromSource(Source source) {
        if (source instanceof Base64Source base64Source) {
            return new InputContentDataSource(base64Source.getData(), base64Source.getMediaType());
        }
        if (source instanceof URLSource urlSource) {
            return new InputContentUrlSource(urlSource.getUrl(), null);
        }
        return null;
    }

    private DataBlock toDataBlock(DocumentInputContent documentPart, ModelLlmConfigDTO llmConfig) {
        DataBlock.Builder builder = DataBlock.builder()
                .source(toSource(documentPart.source(), llmConfig));
        Object id = documentPart.metadata().get("id");
        if (id != null) {
            builder.id(Objects.toString(id, null));
        }
        Object filename = documentPart.metadata().get("filename");
        if (filename != null) {
            builder.name(Objects.toString(filename, null));
        }
        return builder.build();
    }

    private DocumentInputContent toDocumentPart(DataBlock dataBlock) {
        Map<String, Object> metadata = new HashMap<>();
        if (dataBlock.getId() != null && !dataBlock.getId().isBlank()) {
            metadata.put("id", dataBlock.getId());
        }
        if (dataBlock.getName() != null && !dataBlock.getName().isBlank()) {
            metadata.put("filename", dataBlock.getName());
        }
        return new DocumentInputContent(fromSource(dataBlock.getSource()), metadata);
    }

    private ModelLlmConfigDTO readCurrentLlmConfig() {
        try {
            return ModelProvider.currentConfig();
        } catch (RuntimeException | LinkageError ex) {
            log.debug("读取 LLM 配置失败，使用默认 URL 直传策略", ex);
            return null;
        }
    }

    private boolean shouldConvertUrlToBase64(ModelLlmConfigDTO llmConfig) {
        return llmConfig != null
                && llmConfig.getMultimodalOptions() != null
                && Boolean.TRUE.equals(llmConfig.getMultimodalOptions().getResourceUrlToBase64());
    }

    private InputContentDataSource downloadAsDataSource(InputContentUrlSource urlSource) {
        if (urlSource.value() == null || urlSource.value().isBlank()) {
            return null;
        }
        try {
            byte[] body = RESOURCE_REST_CLIENT
                    .get()
                    .uri(urlSource.value())
                    .retrieve()
                    .body(byte[].class);
            if (body == null || body.length == 0) {
                log.warn("资源 URL 转 Base64 失败，下载内容为空: {}", urlSource.value());
                return null;
            }
            String mimeType = resolveMimeType(urlSource, body);
            return new InputContentDataSource(Base64.getEncoder().encodeToString(body), mimeType);
        } catch (Exception ex) {
            log.warn("资源 URL 转 Base64 失败，继续保留 URLSource: {}", urlSource.value(), ex);
            return null;
        }
    }

    private String resolveMimeType(InputContentUrlSource urlSource, byte[] body) {
        if (urlSource.mimeType() != null && !urlSource.mimeType().isBlank()) {
            return urlSource.mimeType();
        }
        try {
            return RESOURCE_REST_CLIENT
                    .get()
                    .uri(urlSource.value())
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getFirst(HttpHeaders.CONTENT_TYPE);
        } catch (Exception ignored) {
            try {
                String probeMimeType = java.net.URLConnection.guessContentTypeFromStream(new java.io.ByteArrayInputStream(body));
                if (probeMimeType != null && !probeMimeType.isBlank()) {
                    return probeMimeType;
                }
            } catch (IOException ignored2) {
                // ignore
            }
            String fromName = java.net.URLConnection.guessContentTypeFromName(urlSource.value());
            if (fromName != null && !fromName.isBlank()) {
                return fromName;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    private MessageContent buildContentFromParts(List<InputContent> parts, String mergedText) {
        if (parts.isEmpty()) {
            return mergedText == null || mergedText.isEmpty() ? null : new MessageContent.Text(mergedText);
        }
        boolean containsNonText = parts.stream().anyMatch(part -> !(part instanceof TextInputContent));
        if (!containsNonText) {
            return mergedText == null || mergedText.isEmpty() ? null : new MessageContent.Text(mergedText);
        }
        return new MessageContent.Blocks(parts);
    }

    private MessageContent readOriginalContent(Msg msg) {
        if (msg.getMetadata() == null) {
            return null;
        }
        Object raw = msg.getMetadata().get(METADATA_AGUI_ORIGINAL_CONTENT);
        if (raw == null) {
            return null;
        }
        if (raw instanceof MessageContent aguiMessageContent) {
            return aguiMessageContent;
        }
        JsonNode rawNode = OBJECT_MAPPER.valueToTree(raw);
        if (rawNode == null || rawNode.isNull() || rawNode.isMissingNode()) {
            return null;
        }
        if (rawNode.isObject()) {
            JsonNode partsNode = rawNode.get("parts");
            if (partsNode != null && partsNode.isArray()) {
                return OBJECT_MAPPER.convertValue(partsNode, MessageContent.class);
            }
            JsonNode textNode = rawNode.get("text");
            if (textNode != null && !textNode.isObject() && !textNode.isArray()) {
                return OBJECT_MAPPER.convertValue(textNode, MessageContent.class);
            }
        }
        return OBJECT_MAPPER.convertValue(rawNode, MessageContent.class);
    }
}
