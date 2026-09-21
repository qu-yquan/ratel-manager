package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识目录或文档节点")
public class KnowledgeNodeVO extends BaseVO {
    private String id;
    private String parentId;
    private String nodeType;
    private String name;
    private Integer sortNo;
    private String fileId;
    private String fileName;
    private Long fileSize;
    private String fileFormat;
    private KnowledgeDocumentStatus documentStatus;
    private Boolean enabled;
    private Boolean embeddingCompleted;
    private Boolean imageOcrParsed;
    private LocalDateTime parsedAt;
    private LocalDateTime processedAt;
    private String processMessage;
    private Long documentCount;
    private Boolean canSearch;
    private Boolean canUpload;
    private Boolean canManage;
    private Boolean canEdit;
    private Boolean canDelete;
}
