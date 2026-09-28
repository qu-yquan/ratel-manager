package org.quyq.gwsu.log.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "服务器日志聚合检索结果")
public record ServerLogSearchVO(
        List<Group> groups,
        List<Warning> warnings,
        boolean truncated,
        long elapsedMillis
) {

    public record Group(
            String targetId,
            String serviceName,
            String serviceNote,
            String instanceName,
            LocalDateTime latestTime,
            List<Match> matches
    ) {
    }

    public record Match(
            String fileId,
            String fileName,
            long lineNumber,
            LocalDateTime logTime,
            String preview
    ) {
    }

    public record Warning(String serviceName, String instanceName, String message) {
    }
}
