package org.quyq.gwsu.common.log.serverfile.domain;

import java.util.List;

/**
 * 本地日志上下文。
 */
public record LocalLogContextResult(
        long startLine,
        long endLine,
        boolean beginningOfFile,
        boolean endOfFile,
        List<Line> lines
) {

    public record Line(long lineNumber, String text) {
    }
}
