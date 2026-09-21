package org.quyq.gwsu.kit.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.kit.api.knowledge.enums.KnowledgeDocumentStatus;

import java.time.LocalDateTime;

/**
 * 知识源文档。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName("kit_knowledge_node")
@Schema(description = "知识源文档")
public class KitKnowledgeSourceDocument extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "父目录ID")
    private String parentId;

    @Schema(description = "节点类型：DIRECTORY或DOCUMENT")
    private String nodeType;

    @Schema(description = "目录路径，包含所有祖先目录及所在目录ID")
    private String directoryPath;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "排序值")
    private Integer sortNo;

    @Schema(description = "文件ID")
    private String fileId;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件格式")
    private String fileFormat;

    @Schema(description = "文档处理状态")
    private KnowledgeDocumentStatus documentStatus;

    @Schema(description = "目标Page ID")
    private String targetPageId;

    @Schema(description = "处理信息")
    private String processMessage;

    @Schema(description = "导入图片文件ID JSON")
    private String imageFileIdsJson;

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
}
