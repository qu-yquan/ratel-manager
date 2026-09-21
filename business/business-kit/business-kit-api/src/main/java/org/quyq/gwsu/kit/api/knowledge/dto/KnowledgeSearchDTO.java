package org.quyq.gwsu.kit.api.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;


/**
 * 知识库检索条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "知识库检索条件")
public class KnowledgeSearchDTO extends BaseDTO {

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "返回数量")
    private Integer size;
}
