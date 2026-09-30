package org.quyq.gwsu.common.ai.mcp.tool;

import io.agentscope.core.message.AudioBlock;
import io.agentscope.core.message.Base64Source;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.DataBlock;
import io.agentscope.core.message.ImageBlock;
import io.agentscope.core.message.Source;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.URLSource;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class McpToolResultMapper {

    private final McpJsonMapper jsonMapper;

    public McpToolResultMapper(McpJsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public McpSchema.CallToolResult map(ToolResultBlock result, boolean structuredOutput) {
        if (result == null) {
            return error("工具未返回结果");
        }
        List<McpSchema.Content> content = new ArrayList<>();
        for (ContentBlock block : result.getOutput()) {
            content.add(toContent(block));
        }
        if (content.isEmpty()) {
            content.add(new McpSchema.TextContent(""));
        }
        // AgentScope 的普通 ToolResultBlock 默认状态也是 RUNNING，不能据此判定失败。
        boolean error = result.isSuspended()
                || result.getState() == ToolResultState.ERROR
                || result.getState() == ToolResultState.DENIED
                || result.getState() == ToolResultState.INTERRUPTED;
        McpSchema.CallToolResult.Builder builder = McpSchema.CallToolResult.builder()
                .content(List.copyOf(content))
                .isError(error);
        if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
            builder.meta(result.getMetadata());
        }
        if (structuredOutput && !error) {
            Object structured = structuredContent(result.getOutput());
            if (structured != null) {
                builder.structuredContent(structured);
            }
        }
        return builder.build();
    }

    public McpSchema.CallToolResult error(String message) {
        return McpSchema.CallToolResult.builder()
                .addTextContent(message)
                .isError(true)
                .build();
    }

    private McpSchema.Content toContent(ContentBlock block) {
        return switch (block) {
            case TextBlock text -> new McpSchema.TextContent(text.getText());
            case ImageBlock image -> imageContent(image.getSource());
            case AudioBlock audio -> audioContent(audio.getSource());
            case DataBlock data -> new McpSchema.TextContent(readData(data.getSource()));
            default -> new McpSchema.TextContent(writeJson(block));
        };
    }

    private McpSchema.Content imageContent(Source source) {
        if (source instanceof Base64Source base64) {
            return new McpSchema.ImageContent(null, base64.getData(), base64.getMediaType());
        }
        return new McpSchema.TextContent(sourceLocation(source));
    }

    private McpSchema.Content audioContent(Source source) {
        if (source instanceof Base64Source base64) {
            return new McpSchema.AudioContent(null, base64.getData(), base64.getMediaType());
        }
        return new McpSchema.TextContent(sourceLocation(source));
    }

    private String readData(Source source) {
        if (source instanceof Base64Source base64) {
            try {
                return new String(Base64.getDecoder().decode(base64.getData()), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
                return base64.getData();
            }
        }
        return sourceLocation(source);
    }

    private String sourceLocation(Source source) {
        if (source instanceof URLSource url) {
            return url.getUrl();
        }
        return writeJson(source);
    }

    private Object structuredContent(List<ContentBlock> output) {
        if (output.size() != 1) {
            return null;
        }
        ContentBlock block = output.getFirst();
        String value = switch (block) {
            case TextBlock text -> text.getText();
            case DataBlock data -> readData(data.getSource());
            default -> null;
        };
        if (value == null) {
            return null;
        }
        try {
            return jsonMapper.readValue(value, Object.class);
        } catch (Exception ignored) {
            return value;
        }
    }

    private String writeJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return String.valueOf(value);
        }
    }
}
