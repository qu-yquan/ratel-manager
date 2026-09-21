package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "角色知识目录授权")
public class KnowledgeRoleScopeSaveDTO {
    private String roleCode;
    private List<String> directoryIds;
}
