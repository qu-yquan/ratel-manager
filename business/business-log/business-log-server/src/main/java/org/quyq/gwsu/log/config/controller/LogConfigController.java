package org.quyq.gwsu.log.config.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.log.enums.SaveMedium;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 日志配置选项接口。
 */
@RestController
@RequestMapping("log/config")
@Tag(name = "日志配置")
@TableModelPermission
public class LogConfigController {

    @GetMapping("/storage-medium-options")
    @Operation(summary = "查询日志存储媒介")
    @LoginAllowAccess
    @LogIgnore
    public R<List<KeyValue<String, String>>> storageMediumOptions() {
        return R.ok(Arrays.stream(SaveMedium.values())
                .map(item -> new KeyValue<>(item.name(), item.getName()))
                .toList());
    }
}
