package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识检索调试参数")
public class KnowledgeDebugSearchDTO extends KnowledgeSearchDTO {
    private String mode;
    private String directoryId;
    private String roleCode;
}
