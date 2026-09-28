package org.quyq.gwsu.log.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "服务器日志上下文查询条件")
public class ServerLogContextDTO {

    @Schema(description = "服务实例匿名标识")
    private String targetId;

    @Schema(description = "日志文件标识")
    private String fileId;

    @Schema(description = "开始行")
    private Long startLine;

    @Schema(description = "读取行数")
    private Integer lineCount;
}
