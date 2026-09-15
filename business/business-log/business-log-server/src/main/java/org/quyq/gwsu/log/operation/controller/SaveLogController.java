package org.quyq.gwsu.log.operation.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.quyq.gwsu.log.operation.service.ILogOperationService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/21
 * @description
 */
@Slf4j
@RestController
@RequestMapping("saveLog")
@Tag(name = "日志记录保存模块", description = "日志记录保存模块")
@RequiredArgsConstructor
public class SaveLogController implements ILogClientApi {

    private final ILogOperationService logOperationService;

    private final ILogLoginService logLoginService;

    @Operation(summary = "保存操作日志")
    @PostMapping("operation")
    @LogIgnore
    @Override
    public R<Boolean> saveOperLog(@RequestBody LogOperationVO vo) {
        log.info("收到操作日志：tid={}, module={}, url={}", vo.getTid(), vo.getModulePrefix(), vo.getRequestUrl());
        return R.ok(logOperationService.saveLog(vo));
    }

    @Operation(summary = "保存认证日志")
    @PostMapping("login")
    @LogIgnore
    @Override
    public R<Boolean> saveLoginLog(@RequestBody LogLoginVO vo) {
        log.info("收到认证日志：authorizationId={}, eventType={}",
                vo.getAuthorizationId(), vo.getEventType());
        return R.ok(logLoginService.saveLog(vo));
    }
}
