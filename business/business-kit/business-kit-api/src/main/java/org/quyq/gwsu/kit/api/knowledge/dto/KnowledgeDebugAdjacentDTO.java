package org.quyq.gwsu.kit.api.knowledge.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class KnowledgeDebugAdjacentDTO extends KnowledgeChunkAdjacentDTO {
    private String mode;
    private String directoryId;
    private String roleCode;
}
