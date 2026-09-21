package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识文档事件")
public class KnowledgeDocumentEventVO extends BaseVO {
    private String id;
    private String taskId;
    private String eventType;
    private String message;
    private LocalDateTime occurredAt;
}
