package org.quyq.gwsu.security.brain.service.impl;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.agui.model.AguiFunctionCall;
import org.quyq.gwsu.common.ai.agui.model.AguiMessage;
import org.quyq.gwsu.common.ai.agui.model.AguiToolCall;
import org.quyq.gwsu.common.ai.agui.model.content.AguiTextContent;
import org.quyq.gwsu.common.core.config.GsonConfiguration;
import org.quyq.gwsu.security.api.brain.dto.BrainHistoryQueryDTO;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionSliceVo;
import org.quyq.gwsu.security.api.brain.vo.BrainHistorySessionVo;
import org.quyq.gwsu.security.brain.service.IBrainHistoryService;
import org.quyq.gwsu.security.brain.service.history.BrainHistoryBaseStoreRepository;
import org.quyq.gwsu.security.brain.service.history.BrainHistorySessionIndexEntry;
import org.quyq.gwsu.security.brain.service.history.BrainHistorySessionIndexRepository;
import org.quyq.gwsu.security.brain.service.history.BrainHistorySessionIndexService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import io.agentscope.core.state.AgentStateStore;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 大脑历史会话服务实现。
 */
@Service
@RequiredArgsConstructor
public class BrainHistoryServiceImpl implements IBrainHistoryService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final String TOOL_CALL_PREFIX = "[tool_call:";
    private static final String TOOL_RESULT_PREFIX = "[tool_result";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final BrainHistorySessionIndexRepository sessionIndexRepository;
    private final BrainHistorySessionIndexService sessionIndexService;
    private final BrainHistoryBaseStoreRepository baseStoreRepository;
    private final AgentStateStore agentStateStore;

    @Override
    public BrainHistorySessionSliceVo pageHistorySessions(BrainHistoryQueryDTO query, String userId) {
        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : DEFAULT_PAGE_NUM;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : DEFAULT_PAGE_SIZE;
        long offset = (long) (pageNum - 1) * pageSize;

        List<BrainHistorySessionIndexEntry> pageEntries = sessionIndexRepository.page(userId, offset, pageSize + 1);
        boolean hasMore = pageEntries.size() > pageSize;
        List<BrainHistorySessionVo> records = pageEntries.stream()
                .limit(pageSize)
                .map(this::toSessionVo)
                .toList();

        BrainHistorySessionSliceVo result = new BrainHistorySessionSliceVo();
        result.setRecords(records);
        result.setHasMore(hasMore);
        result.setNextPageNum(hasMore ? pageNum + 1 : null);
        return result;
    }

    @Override
    public List<AguiMessage> getSessionMessages(String sessionId, String userId) {
        BrainHistorySessionIndexEntry entry = sessionIndexRepository.get(userId, sessionId);
        if (entry == null) {
            return List.of();
        }
        List<BrainHistorySessionIndexService.StoredMessageEntry> messages = sessionIndexService
                .loadMessages(userId, entry).stream()
                .filter(message -> !sessionIndexService.isCondensedSummaryPrompt(message))
                .toList();
        List<AguiMessage> restoredMessages = new ArrayList<>(messages.size());
        for (int index = 0; index < messages.size(); index++) {
            BrainHistorySessionIndexService.StoredMessageEntry message = messages.get(index);
            if (isStructuredToolUse(message)) {
                restoredMessages.add(toStructuredToolUseMessage(message));
            } else if (isStructuredToolResult(message)) {
                restoredMessages.add(toStructuredToolResultMessage(message));
            } else if ("assistant".equals(normalizeRole(message.role()))) {
                restoredMessages.add(toAssistantMessage(message, resolveFollowingToolCallIds(messages, index)));
            } else {
                restoredMessages.add(toAguiMessage(message));
            }
        }
        return restoredMessages;
    }

    @Override
    public Boolean deleteSession(String sessionId, String userId) {
        BrainHistorySessionIndexEntry entry = sessionIndexRepository.get(userId, sessionId);
        boolean sessionDeleted = baseStoreRepository.deleteSessionFiles(
                userId,
                sessionId,
                entry != null ? entry.getLogPath() : null);
        sessionIndexService.deleteSessionIndex(sessionId, userId);
        agentStateStore.delete(userId, sessionId);
        return sessionDeleted || entry != null;
    }

    private BrainHistorySessionVo toSessionVo(BrainHistorySessionIndexEntry entry) {
        BrainHistorySessionVo vo = new BrainHistorySessionVo();
        vo.setSessionId(entry.getSessionId());
        vo.setTitle(entry.getTitle());
        vo.setMessageCount(entry.getMessageCount());
        LocalDateTime updatedAt = parseTime(entry.getUpdatedAt());
        vo.setUpdatedAt(updatedAt);
        vo.setTimeDisplay(formatTimeDisplay(updatedAt));
        return vo;
    }

    private AguiMessage toAguiMessage(BrainHistorySessionIndexService.StoredMessageEntry entry) {
        String role = normalizeRole(entry.role());
        if ("tool".equals(role)) {
            return toToolMessage(entry);
        }
        return new AguiMessage(
                StringUtils.hasText(entry.id()) ? entry.id() : Instant.now().toString(),
                role,
                new AguiTextContent(entry.content()),
                null,
                entry.toolCallId());
    }

    private boolean isStructuredToolUse(BrainHistorySessionIndexService.StoredMessageEntry entry) {
        return "tool_use".equalsIgnoreCase(entry.type());
    }

    private boolean isStructuredToolResult(BrainHistorySessionIndexService.StoredMessageEntry entry) {
        return "tool_result".equalsIgnoreCase(entry.type());
    }

    private AguiMessage toStructuredToolUseMessage(
            BrainHistorySessionIndexService.StoredMessageEntry entry) {
        String messageId = StringUtils.hasText(entry.id()) ? entry.id() : Instant.now().toString();
        String toolCallId = StringUtils.hasText(entry.toolCallId()) ? entry.toolCallId() : messageId;
        String toolName = StringUtils.hasText(entry.name()) ? entry.name() : "unknown_tool";
        AguiToolCall toolCall = new AguiToolCall(
                toolCallId,
                new AguiFunctionCall(toolName, serializeToolInput(entry.input())));
        return new AguiMessage(messageId, "assistant", null, List.of(toolCall), null);
    }

    private AguiMessage toStructuredToolResultMessage(
            BrainHistorySessionIndexService.StoredMessageEntry entry) {
        String messageId = StringUtils.hasText(entry.id()) ? entry.id() : Instant.now().toString();
        String toolCallId = StringUtils.hasText(entry.toolCallId()) ? entry.toolCallId() : messageId;
        return new AguiMessage(
                messageId,
                "tool",
                new AguiTextContent(entry.output() != null ? entry.output() : ""),
                null,
                toolCallId);
    }

    private String serializeToolInput(java.util.Map<String, Object> input) {
        if (input == null || input.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(input);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private AguiMessage toAssistantMessage(
            BrainHistorySessionIndexService.StoredMessageEntry entry,
            List<String> followingToolCallIds) {
        RestoredAssistantMessage restored = restoreAssistantMessage(entry, followingToolCallIds);
        return new AguiMessage(
                StringUtils.hasText(entry.id()) ? entry.id() : Instant.now().toString(),
                "assistant",
                StringUtils.hasText(restored.content()) ? new AguiTextContent(restored.content()) : null,
                restored.toolCalls().isEmpty() ? null : restored.toolCalls(),
                null);
    }

    /**
     * 工具结果紧跟在发起调用的助手消息之后，且保留了每个调用的真实 ID。
     * 历史恢复时按顺序回填这些 ID，确保一条消息中的多个调用都能与结果正确关联。
     */
    private List<String> resolveFollowingToolCallIds(
            List<BrainHistorySessionIndexService.StoredMessageEntry> messages,
            int assistantIndex) {
        List<String> toolCallIds = new ArrayList<>();
        for (int index = assistantIndex + 1; index < messages.size(); index++) {
            BrainHistorySessionIndexService.StoredMessageEntry message = messages.get(index);
            if (isStructuredToolUse(message)) {
                continue;
            }
            if (isStructuredToolResult(message)) {
                if (StringUtils.hasText(message.toolCallId())) {
                    toolCallIds.add(message.toolCallId());
                }
                continue;
            }
            if (!"tool".equals(normalizeRole(message.role()))) {
                break;
            }
            if (StringUtils.hasText(message.toolCallId())) {
                toolCallIds.add(message.toolCallId());
            }
        }
        return toolCallIds;
    }

    private AguiMessage toToolMessage(BrainHistorySessionIndexService.StoredMessageEntry entry) {
        return new AguiMessage(
                StringUtils.hasText(entry.id()) ? entry.id() : Instant.now().toString(),
                "tool",
                new AguiTextContent(stripToolResultPrefix(entry.content())),
                null,
                entry.toolCallId());
    }

    /**
     * AgentScope 会话日志是可读文本，这里把其中的 [tool_call: Name(args)] 恢复成 AG-UI toolCalls。
     */
    private RestoredAssistantMessage restoreAssistantMessage(
            BrainHistorySessionIndexService.StoredMessageEntry entry,
            List<String> followingToolCallIds) {
        String content = entry.content();
        if (!StringUtils.hasText(content) || !content.contains(TOOL_CALL_PREFIX)) {
            return new RestoredAssistantMessage(content, List.of());
        }

        List<AguiToolCall> toolCalls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int cursor = 0;
        int toolIndex = 0;
        while (cursor < content.length()) {
            int markerStart = content.indexOf(TOOL_CALL_PREFIX, cursor);
            if (markerStart < 0) {
                text.append(content.substring(cursor));
                break;
            }
            text.append(content, cursor, markerStart);

            ToolCallMarker marker = parseToolCallMarker(
                    content,
                    markerStart,
                    entry,
                    followingToolCallIds,
                    toolIndex);
            if (marker == null) {
                text.append(content.substring(markerStart));
                break;
            }
            toolCalls.add(marker.toolCall());
            cursor = marker.endExclusive();
            toolIndex++;
        }

        return new RestoredAssistantMessage(trimTrailingBlankLines(text.toString()), toolCalls);
    }

    private ToolCallMarker parseToolCallMarker(
            String content,
            int markerStart,
            BrainHistorySessionIndexService.StoredMessageEntry entry,
            List<String> followingToolCallIds,
            int toolIndex) {
        int nameStart = markerStart + TOOL_CALL_PREFIX.length();
        int nameEnd = findToolNameEnd(content, nameStart);
        if (nameEnd <= nameStart) {
            return null;
        }

        String toolName = content.substring(nameStart, nameEnd).trim();
        String arguments = "{}";
        int cursor = nameEnd;
        while (cursor < content.length() && Character.isWhitespace(content.charAt(cursor))) {
            cursor++;
        }

        if (cursor < content.length() && content.charAt(cursor) == '(') {
            JsonArgument argument = parseJsonArgument(content, cursor + 1);
            if (argument == null) {
                return null;
            }
            arguments = normalizeJson(argument.json());
            cursor = argument.endExclusive();
            if (cursor >= content.length() || content.charAt(cursor) != ')') {
                return null;
            }
            cursor++;
        }

        while (cursor < content.length() && Character.isWhitespace(content.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= content.length() || content.charAt(cursor) != ']') {
            return null;
        }

        String toolCallId;
        if (toolIndex < followingToolCallIds.size()
                && StringUtils.hasText(followingToolCallIds.get(toolIndex))) {
            toolCallId = followingToolCallIds.get(toolIndex);
        } else if (toolIndex == 0 && StringUtils.hasText(entry.toolCallId())) {
            toolCallId = entry.toolCallId();
        } else {
            toolCallId = "%s-tool-%d".formatted(
                    StringUtils.hasText(entry.id()) ? entry.id() : "history",
                    toolIndex);
        }
        AguiToolCall toolCall = new AguiToolCall(toolCallId, new AguiFunctionCall(toolName, arguments));
        return new ToolCallMarker(toolCall, cursor + 1);
    }

    private int findToolNameEnd(String content, int nameStart) {
        int cursor = nameStart;
        while (cursor < content.length()) {
            char c = content.charAt(cursor);
            if (c == '(' || c == ']') {
                return cursor;
            }
            cursor++;
        }
        return -1;
    }

    private JsonArgument parseJsonArgument(String content, int jsonStart) {
        int cursor = jsonStart;
        while (cursor < content.length() && Character.isWhitespace(content.charAt(cursor))) {
            cursor++;
        }
        if (cursor >= content.length() || content.charAt(cursor) != '{') {
            return null;
        }

        boolean inString = false;
        boolean escaping = false;
        int depth = 0;
        for (int i = cursor; i < content.length(); i++) {
            char c = content.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{') {
                depth++;
                continue;
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    return new JsonArgument(content.substring(cursor, i + 1), i + 1);
                }
            }
        }
        return null;
    }

    private String normalizeJson(String json) {
        if (!StringUtils.hasText(json)) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(OBJECT_MAPPER.readTree(json));
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private String stripToolResultPrefix(String content) {
        if (!StringUtils.hasText(content) || !content.startsWith(TOOL_RESULT_PREFIX)) {
            return content;
        }
        int closeIndex = content.indexOf(']');
        if (closeIndex < 0 || closeIndex + 1 >= content.length()) {
            return "";
        }
        return content.substring(closeIndex + 1).stripLeading();
    }

    private String trimTrailingBlankLines(String content) {
        if (content == null) {
            return null;
        }
        return content.replaceFirst("\\s+$", "");
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return "assistant";
        }
        return switch (role.trim().toUpperCase()) {
            case "USER" -> "user";
            case "SYSTEM" -> "system";
            case "TOOL" -> "tool";
            default -> "assistant";
        };
    }

    private LocalDateTime parseTime(String updatedAt) {
        if (!StringUtils.hasText(updatedAt)) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(Instant.parse(updatedAt), ZoneId.systemDefault());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatTimeDisplay(LocalDateTime updatedAt) {
        if (updatedAt == null) {
            return "";
        }

        Duration duration = Duration.between(updatedAt, LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + "分钟前";
        }
        if (hours < 24) {
            return hours + "小时前";
        }
        if (days < 30) {
            return days + "天前";
        }
        long months = days / 30;
        if (months < 12) {
            return months + "个月前";
        }
        return updatedAt.format(GsonConfiguration.dateTimeFormatter);
    }

    private record RestoredAssistantMessage(String content, List<AguiToolCall> toolCalls) {
    }

    private record ToolCallMarker(AguiToolCall toolCall, int endExclusive) {
    }

    private record JsonArgument(String json, int endExclusive) {
    }
}
