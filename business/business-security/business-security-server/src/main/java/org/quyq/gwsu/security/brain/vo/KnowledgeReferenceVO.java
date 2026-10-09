package org.quyq.gwsu.security.brain.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Assistant 消息引用的知识源文件。
 */
@Data
@Accessors(chain = true)
public class KnowledgeReferenceVO {

    private String documentId;

    private String fileId;

    private String fileName;

    private String fileFormat;

    private Integer chunkCount;

    private Double bestScore;

    private List<KnowledgeReferenceLocationVO> locations;
}
