package org.quyq.gwsu.kit.api.knowledge.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeBlockType;

import java.util.List;

@Data
@Accessors(chain = true)
public class KnowledgeDocumentBlocksVO {
    private String pageVersionId;
    private List<Block> blocks;

    @Data
    @Accessors(chain = true)
    public static class Block {
        private String id;
        private Integer orderNo;
        private KnowledgeBlockType blockType;
        private String content;
        private String sourceLocator;
    }
}
