package org.quyq.gwsu.common.log.serverfile.domain;

/**
 * 本地日志上下文读取条件。
 */
public record LocalLogContextRequest(
        String fileId,
        long startLine,
        int lineCount
) {
}
