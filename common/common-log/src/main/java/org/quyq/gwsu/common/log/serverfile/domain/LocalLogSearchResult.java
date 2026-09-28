package org.quyq.gwsu.common.log.serverfile.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 单个服务实例的日志检索结果。
 */
public record LocalLogSearchResult(
        List<Match> matches,
        boolean truncated,
        long scannedBytes
) {

    public record Match(
            String fileId,
            String fileName,
            long lineNumber,
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime logTime,
            String preview
    ) {
    }
}
