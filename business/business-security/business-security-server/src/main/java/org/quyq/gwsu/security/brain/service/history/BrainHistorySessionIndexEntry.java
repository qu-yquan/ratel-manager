package org.quyq.gwsu.security.brain.service.history;

import lombok.Data;
import org.quyq.gwsu.security.brain.vo.BrainHistoryMessageMetadataVO;

import java.util.Map;

/**
 * 历史会话 Redis 索引明细。
 */
@Data
public class BrainHistorySessionIndexEntry {

    private String sessionId;

    private String title;

    private Integer messageCount;

    private String updatedAt;

    private String logPath;

    /**
     * 以消息 ID 为键的扩展元数据，与会话索引明细一并存储。
     */
    private Map<String, BrainHistoryMessageMetadataVO> messageMetadata;

}
