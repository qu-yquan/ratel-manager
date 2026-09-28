package org.quyq.gwsu.common.log.serverfile.config;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 服务器本地日志检索配置。
 */
@ConfigurationProperties(CoreConstants.Yaml.PROJECT_CONFIG_PREFIX + ".log.server-file")
public record ServerFileLogProperties(
        Boolean enabled,
        String path,
        Integer readBufferSize,
        Integer maxMatches,
        Integer maxContextLines,
        Integer maxRangeDays,
        Long maxScanBytes,
        Integer maxPreviewChars,
        Integer maxLineChars,
        String charset
) {

    public ServerFileLogProperties {
        enabled = enabled == null || enabled;
        readBufferSize = readBufferSize == null || readBufferSize < 8192 ? 65536 : readBufferSize;
        maxMatches = maxMatches == null || maxMatches < 1 ? 100 : maxMatches;
        maxContextLines = maxContextLines == null || maxContextLines < 1 ? 500 : maxContextLines;
        maxRangeDays = maxRangeDays == null || maxRangeDays < 1 ? 7 : maxRangeDays;
        maxScanBytes = maxScanBytes == null || maxScanBytes < 1 ? 512L * 1024 * 1024 : maxScanBytes;
        maxPreviewChars = maxPreviewChars == null || maxPreviewChars < 100 ? 500 : maxPreviewChars;
        maxLineChars = maxLineChars == null || maxLineChars < 1000 ? 8192 : maxLineChars;
        charset = charset == null || charset.isBlank() ? StandardCharsets.UTF_8.name() : charset;
    }

    public Charset resolvedCharset() {
        return Charset.forName(charset);
    }
}
