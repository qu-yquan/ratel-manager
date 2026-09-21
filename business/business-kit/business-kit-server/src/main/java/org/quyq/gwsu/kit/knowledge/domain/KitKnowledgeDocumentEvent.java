package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_document_event")
public class KitKnowledgeDocumentEvent extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String documentId;
    private String taskId;
    private String eventType;
    private String message;
    private LocalDateTime occurredAt;
}
