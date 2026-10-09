package org.quyq.gwsu.security.brain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 知识引用在源文档中的定位信息。
 */
@Data
@Accessors(chain = true)
public class KnowledgeReferenceLocationVO {

    private String citationKey;

    private String chunkId;

    private String pageId;

    private String pageBlockId;

    private String headingPath;

    private Integer blockOrder;
}
