package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;

import java.util.List;

@Data
@Schema(description = "知识文档Markdown保存参数")
public class KnowledgeMarkdownSaveDTO {
    private String documentId;
    private String pageVersionId;
    private List<Block> blocks;

    @Data
    public static class Block {
        private String id;
        private KnowledgeBlockType blockType;
        private String content;
    }
}
