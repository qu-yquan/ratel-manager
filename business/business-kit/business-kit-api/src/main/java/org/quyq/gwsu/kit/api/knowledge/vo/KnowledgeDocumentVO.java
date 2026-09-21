package org.quyq.gwsu.kit.api.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestStage;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeIngestTaskStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识源文档视图。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "知识源文档视图")
public class KnowledgeDocumentVO extends BaseVO {

    @Schema(description = "源文档ID")
    private String id;

    private String parentId;

    private Long fileSize;

    private String fileFormat;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文档处理状态")
    private KnowledgeDocumentStatus documentStatus;

    @Schema(description = "处理信息")
    private String processMessage;

    @Schema(description = "图片是否已完成 OCR 解析")
    private Boolean imageOcrParsed;

    @Schema(description = "是否已完成向量化")
    private Boolean embeddingCompleted;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "文件解析完成时间")
    private LocalDateTime parsedAt;

    @Schema(description = "处理完成时间")
    private LocalDateTime processedAt;

    @Schema(description = "最近导入任务ID")
    private String latestTaskId;

    @Schema(description = "最近导入任务状态")
    private KnowledgeIngestTaskStatus latestTaskStatus;

    @Schema(description = "最近导入任务阶段")
    private KnowledgeIngestStage latestTaskStage;

    @Schema(description = "最近导入任务重试次数")
    private Integer latestTaskRetryCount;

    @Schema(description = "最近导入任务错误信息")
    private String latestTaskErrorMessage;

    @Schema(description = "最近导入任务开始时间")
    private LocalDateTime latestTaskStartedAt;

    @Schema(description = "最近导入任务完成时间")
    private LocalDateTime latestTaskFinishedAt;
}
