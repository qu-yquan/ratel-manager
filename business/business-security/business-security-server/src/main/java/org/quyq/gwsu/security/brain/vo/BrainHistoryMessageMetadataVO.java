package org.quyq.gwsu.security.brain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 历史消息的扩展元数据。
 */
@Data
@Accessors(chain = true)
public class BrainHistoryMessageMetadataVO {

    private List<KnowledgeReferenceVO> knowledgeReferences;
}
