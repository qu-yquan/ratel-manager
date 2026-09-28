package org.quyq.gwsu.log.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import org.quyq.gwsu.common.core.domain.KeyValue;

import java.util.List;

@Schema(description = "服务器日志页面选项")
public record ServerLogOptionsVO(
        @Schema(description = "是否为分布式部署") boolean distributed,
        @Schema(description = "可检索的服务列表") List<KeyValue<String, String>> services
) {
}
