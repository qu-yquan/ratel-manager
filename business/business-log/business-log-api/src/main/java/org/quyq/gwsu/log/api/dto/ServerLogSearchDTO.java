package org.quyq.gwsu.log.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "服务器日志检索条件")
public class ServerLogSearchDTO {

    @Schema(description = "普通文本关键词")
    private String content;

    @Schema(description = "链路标识")
    private String tid;

    @Schema(description = "分布式部署下待检索的服务名")
    private List<String> serviceNames;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    /**
     * 仅用于兼容旧版客户端，实际命中数量由检索模式决定。
     */
    @Deprecated
    @Schema(hidden = true)
    private Integer maxMatches;

}
