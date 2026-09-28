package org.quyq.gwsu.common.log.serverfile.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 单个服务实例的本地日志检索条件。
 */
public record LocalLogSearchRequest(
        String content,
        String tid,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime endTime,
        Integer maxMatches
) {
}
