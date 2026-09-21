package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "知识目录保存参数")
public class KnowledgeDirectorySaveDTO {
    private String id;
    private String parentId;
    private String name;
}
