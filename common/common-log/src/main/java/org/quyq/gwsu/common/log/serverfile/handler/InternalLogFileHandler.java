package org.quyq.gwsu.common.log.serverfile.handler;

import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchRequest;
import org.quyq.gwsu.common.log.serverfile.service.LocalLogFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.nio.charset.StandardCharsets;

/**
 * 服务实例本地日志函数式处理器，不参与 Controller 接口及操作日志采集。
 */
@RequiredArgsConstructor
public class InternalLogFileHandler {

    private final LocalLogFileService localLogFileService;

    public ServerResponse search(ServerRequest request) throws Exception {
        LocalLogSearchRequest query = request.body(LocalLogSearchRequest.class);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(R.ok(localLogFileService.search(query)));
    }

    public ServerResponse context(ServerRequest request) throws Exception {
        LocalLogContextRequest query = request.body(LocalLogContextRequest.class);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(R.ok(localLogFileService.getContext(query)));
    }

    public ServerResponse download(ServerRequest request) {
        String fileId = request.param("fileId")
                .orElseThrow(() -> new BusinessException("日志文件标识不能为空"));
        Resource resource = localLogFileService.getDownloadResource(fileId);
        String disposition = ContentDisposition.attachment()
                .filename(resource.getFilename(), StandardCharsets.UTF_8)
                .build()
                .toString();
        return ServerResponse.ok()
                .contentType(localLogFileService.getMediaType(fileId))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(resource);
    }
}
