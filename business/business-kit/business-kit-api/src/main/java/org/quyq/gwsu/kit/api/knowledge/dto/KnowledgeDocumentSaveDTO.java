package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;


/**
 * 知识源文档保存参数。
 */
@Data
@Schema(description = "知识源文档保存参数")
public class KnowledgeDocumentSaveDTO {

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "所属目录ID，空值表示根目录")
    private String parentId;
}
