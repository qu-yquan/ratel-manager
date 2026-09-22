package org.quyq.gwsu.log.login.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.log.api.dto.LogLoginQueryDTO;
import org.quyq.gwsu.log.login.domain.LogLogin;
import org.quyq.gwsu.log.login.service.ILogLoginService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("log/login")
@Tag(name = "认证日志管理")
@RequiredArgsConstructor
@TableModelPermission(LogLogin.class)
public class LogLoginController {

    private final ILogLoginService logLoginService;

    @GetMapping("/{id}")
    @Operation(summary = "查询认证日志")
    @LogIgnore
    public R<LogLoginVO> getById(@PathVariable String id) {
        return R.ok(logLoginService.getById(id));
    }

    @PostMapping("/page")
    @Operation(summary = "分页查询认证日志")
    @LogIgnore
    public R<IPage<LogLoginVO>> page(@RequestBody LogLoginQueryDTO query) {
        return R.ok(logLoginService.pageByCondition(query));
    }

    @GetMapping("/account-types")
    @Operation(summary = "查询认证日志账号类型")
    @LogIgnore
    public R<List<KeyValue<String, String>>> accountTypes() {
        return R.ok(Arrays.stream(AccountType.values())
                .map(item -> new KeyValue<>(item.name(), item.getMsg()))
                .toList());
    }
}
