package org.quyq.gwsu.system.api.manager.vo;


import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * @author Quyq
 * @date 2026/7/2
 * @description 登录配置信息
 */
public record LoginInfoVO(
        @Schema(description = "项目名")
        String projectName,

        @Schema(description = "微应用名与开发环境入口地址映射")
        Map<String, String> microApps
) {
}
