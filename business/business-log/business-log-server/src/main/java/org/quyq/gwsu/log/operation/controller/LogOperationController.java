package org.quyq.gwsu.log.operation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.log.api.dto.LogOperationQueryDTO;
import org.quyq.gwsu.log.operation.domain.LogOperation;
import org.quyq.gwsu.log.operation.service.ILogOperationService;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志管理控制器
 *
 * @author Quyq
 */
@RestController
@RequestMapping("log/operation")
@Tag(name = "操作日志管理", description = "操作日志管理接口")
@RequiredArgsConstructor
@TableModelPermission({LogOperation.class})
public class LogOperationController {

    private final ILogOperationService logOperationService;

    @Operation(summary = "根据ID查询操作日志")
    @GetMapping("/{id}")
    @LogIgnore
    public R<LogOperationVO> getById(@PathVariable String id) {
        return R.ok(logOperationService.getById(id));
    }

    @Operation(summary = "分页查询操作日志")
    @PostMapping("/page")
    @LogIgnore
    public R<IPage<LogOperationVO>> page(@RequestBody LogOperationQueryDTO query) {
        return R.ok(logOperationService.pageByCondition(query));
    }

    @Operation(summary = "根据链路标识查询树形操作日志")
    @GetMapping("/tid/{tid}")
    @LogIgnore
    public R<LogOperationVO> getTreeByTid(@PathVariable String tid) {
        return R.ok(logOperationService.getTreeByTid(tid));
    }
}
