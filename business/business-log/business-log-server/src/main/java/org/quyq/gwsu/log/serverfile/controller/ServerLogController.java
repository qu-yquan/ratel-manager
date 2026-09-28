package org.quyq.gwsu.log.serverfile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.log.annotation.LogIgnore;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.log.api.dto.ServerLogContextDTO;
import org.quyq.gwsu.log.api.dto.ServerLogSearchDTO;
import org.quyq.gwsu.log.api.vo.ServerLogContextVO;
import org.quyq.gwsu.log.api.vo.ServerLogOptionsVO;
import org.quyq.gwsu.log.api.vo.ServerLogSearchVO;
import org.quyq.gwsu.log.errcode.LogErrorCode;
import org.quyq.gwsu.log.serverfile.service.IServerLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("log/server-log")
@Tag(name = "服务器日志检索")
@RequiredArgsConstructor
@TableModelPermission
public class ServerLogController {

    private final IServerLogService serverLogService;

    @GetMapping("/options")
    @Operation(summary = "获取服务器日志检索选项")
    @LogIgnore
    public R<ServerLogOptionsVO> options() {
        return R.ok(serverLogService.getOptions());
    }

    @PostMapping("/search")
    @Operation(summary = "检索服务器日志")
    @LogIgnore
    public R<ServerLogSearchVO> search(@RequestBody ServerLogSearchDTO query) {
        AssertUtils.notNull(query, LogErrorCode.E01004);
        return R.ok(serverLogService.search(query));
    }

    @PostMapping("/context")
    @Operation(summary = "读取服务器日志上下文")
    @LogIgnore
    public R<ServerLogContextVO> context(@RequestBody ServerLogContextDTO query) {
        AssertUtils.notNull(query, LogErrorCode.E01008);
        AssertUtils.hasText(query.getTargetId(), LogErrorCode.E01006);
        AssertUtils.hasText(query.getFileId(), LogErrorCode.E01007);
        AssertUtils.isTrue(query.getStartLine() != null && query.getStartLine() > 0, LogErrorCode.E01008);
        AssertUtils.isTrue(query.getLineCount() != null && query.getLineCount() > 0, LogErrorCode.E01008);
        return R.ok(serverLogService.getContext(query));
    }

    @GetMapping("/download")
    @Operation(summary = "下载服务器日志文件")
    @LogIgnore
    public void download(
            @RequestParam String targetId,
            @RequestParam String fileId,
            HttpServletResponse response) throws IOException {
        AssertUtils.hasText(targetId, LogErrorCode.E01006);
        AssertUtils.hasText(fileId, LogErrorCode.E01007);
        serverLogService.download(targetId, fileId, response);
    }
}
