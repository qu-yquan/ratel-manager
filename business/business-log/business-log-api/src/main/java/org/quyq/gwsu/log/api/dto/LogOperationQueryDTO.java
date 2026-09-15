package org.quyq.gwsu.log.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;

import java.time.LocalDateTime;

/**
 * 操作日志查询条件
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "操作日志查询条件")
public class LogOperationQueryDTO extends BaseDTO {

    @Schema(description = "认证会话标识")
    private String authorizationId;

    @Schema(description = "全局日志链路")
    private String tid;

    @Schema(description = "所属服务前缀")
    private String modulePrefix;

    @Schema(description = "操作人")
    private String operName;

    @Schema(description = "请求路径")
    private String requestUrl;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "状态：1-成功 0-失败")
    private Integer status;

    @Schema(description = "请求开始时间")
    private LocalDateTime requestTimeStart;

    @Schema(description = "请求结束时间")
    private LocalDateTime requestTimeEnd;
}
