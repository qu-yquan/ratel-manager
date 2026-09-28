package org.quyq.gwsu.log.api.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "服务器日志上下文")
public record ServerLogContextVO(
        long startLine,
        long endLine,
        boolean beginningOfFile,
        boolean endOfFile,
        List<Line> lines
) {
    public record Line(long lineNumber, String text) {
    }
}
