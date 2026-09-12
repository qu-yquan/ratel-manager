package org.quyq.gwsu.security.brain.service.history;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 历史会话 Redis 索引服务。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BrainHistorySessionIndexService {

    private static final int TITLE_MAX_LENGTH = 50;
    private static final String CONDENSED_SUMMARY_PREFIX = "You are in the middle of a conversation that has been summarized.";
    private static final String CONDENSED_HISTORY_MARKER = "The full conversation history has been saved to ";
    private static final String CONDENSED_SUMMARY_MARKER = "A condensed summary follows:";
    private static final String SUMMARY_OPEN_TAG = "<summary>";
    private static final String SUMMARY_CLOSE_TAG = "</summary>";
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
            .build();

    private final BrainHistoryBaseStoreRepository baseStoreRepository;
    private final BrainHistorySessionIndexRepository sessionIndexRepository;

    public void refreshSessionIndex(String sessionId, String userId) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(userId)) {
            return;
        }

        FileSessionLog sessionLog = readSessionLog(userId, sessionId);
        if (sessionLog == null || !StringUtils.hasText(sessionLog.content())) {
            return;
        }

        List<StoredMessageEntry> messages = parseMessages(sessionLog.content());
        BrainHistorySessionIndexEntry entry = new BrainHistorySessionIndexEntry();
        entry.setSessionId(sessionId);
        entry.setTitle(extractTitle(messages));
        entry.setMessageCount(messages.size());
        entry.setUpdatedAt(resolveUpdatedAt(sessionLog));
        entry.setLogPath(sessionLog.path());
        sessionIndexRepository.save(userId, entry);
    }

    public void deleteSessionIndex(String sessionId, String userId) {
        if (!StringUtils.hasText(sessionId) || !StringUtils.hasText(userId)) {
            return;
        }
        sessionIndexRepository.delete(userId, sessionId);
    }

    public List<StoredMessageEntry> loadMessages(String userId, BrainHistorySessionIndexEntry entry) {
        if (entry == null) {
            return List.of();
        }
        String content = null;
        if (StringUtils.hasText(entry.getLogPath())) {
            content = baseStoreRepository.readSessionLog(userId, entry.getLogPath());
        }
        if (!StringUtils.hasText(content)) {
            content = baseStoreRepository.readSessionLogBySessionId(userId, entry.getSessionId());
        }
        if (!StringUtils.hasText(content)) {
            return List.of();
        }
        return parseMessages(content);
    }

    private FileSessionLog readSessionLog(String userId, String sessionId) {
        var fileInfo = baseStoreRepository.findSessionLog(userId, sessionId).orElse(null);
        if (fileInfo != null && StringUtils.hasText(fileInfo.path())) {
            String content = baseStoreRepository.readSessionLog(userId, fileInfo.path());
            if (StringUtils.hasText(content)) {
                return new FileSessionLog(fileInfo.path(), fileInfo.modifiedAt(), content);
            }
        }

        String directPath = baseStoreRepository.buildSessionLogPath(userId, sessionId);
        String directContent = baseStoreRepository.readSessionLog(userId, directPath);
        if (!StringUtils.hasText(directContent)) {
            return null;
        }
        return new FileSessionLog(directPath, Instant.now().toString(), directContent);
    }

    private String resolveUpdatedAt(FileSessionLog sessionLog) {
        if (sessionLog == null || !StringUtils.hasText(sessionLog.modifiedAt())) {
            return Instant.now().toString();
        }
        return sessionLog.modifiedAt();
    }

    private List<StoredMessageEntry> parseMessages(String rawLogContent) {
        if (!StringUtils.hasText(rawLogContent)) {
            return List.of();
        }
        List<StoredMessageEntry> result = new ArrayList<>();
        for (String line : rawLogContent.split("\\R")) {
            if (!StringUtils.hasText(line)) {
                continue;
            }
            try {
                result.add(OBJECT_MAPPER.readValue(line, StoredMessageEntry.class));
            } catch (Exception exception) {
                log.debug("解析历史消息日志失败，已跳过一条记录", exception);
            }
        }
        return result;
    }

    private String extractTitle(List<StoredMessageEntry> messages) {
        for (StoredMessageEntry message : messages) {
            if (message == null || !"USER".equalsIgnoreCase(message.role())) {
                continue;
            }
            if (CharSequenceUtil.isBlank(message.content())) {
                continue;
            }
            if (isCondensedSummaryPrompt(message)) {
                continue;
            }
            String content = message.content().trim();
            return content.length() > TITLE_MAX_LENGTH
                    ? content.substring(0, TITLE_MAX_LENGTH) + "..."
                    : content;
        }
        return "新对话";
    }

    public boolean isCondensedSummaryPrompt(StoredMessageEntry message) {
        if (message == null || !"USER".equalsIgnoreCase(message.role())) {
            return false;
        }
        if (CharSequenceUtil.isBlank(message.content())) {
            return false;
        }
        String content = message.content().trim();
        return content.startsWith(CONDENSED_SUMMARY_PREFIX)
                && content.contains(CONDENSED_HISTORY_MARKER)
                && content.contains(CONDENSED_SUMMARY_MARKER)
                && content.contains(SUMMARY_OPEN_TAG)
                && content.contains(SUMMARY_CLOSE_TAG);
    }

    public record StoredMessageEntry(
            String type,
            String id,
            String parentId,
            String timestamp,
            String role,
            String content,
            String toolCallId,
            String name,
            Map<String, Object> input,
            String output) {
    }

    private record FileSessionLog(String path, String modifiedAt, String content) {
    }
}
