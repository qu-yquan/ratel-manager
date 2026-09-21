package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDirectoryPermission;

@Data
@Schema(description = "角色知识目录授权")
public class KnowledgeRoleScopeSaveDTO {
    private String roleCode;
    private List<Grant> grants;

    @Data
    public static class Grant {
        private String directoryId;
        private KnowledgeDirectoryPermission permissionType;
    }
}
