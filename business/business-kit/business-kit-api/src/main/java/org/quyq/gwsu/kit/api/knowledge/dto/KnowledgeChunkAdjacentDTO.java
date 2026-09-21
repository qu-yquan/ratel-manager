package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeChunkDirection;

import java.util.List;

/**
 * 知识 Chunk 邻近查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识Chunk邻近查询条件")
public class KnowledgeChunkAdjacentDTO extends BaseDTO {

    @Schema(description = "当前Page Block ID")
    private String pageBlockId;

    @Schema(description = "邻近查询方向")
    private KnowledgeChunkDirection direction;

    @Schema(description = "偏移量，1表示上一个或下一个")
    private Integer offset = 1;
}
