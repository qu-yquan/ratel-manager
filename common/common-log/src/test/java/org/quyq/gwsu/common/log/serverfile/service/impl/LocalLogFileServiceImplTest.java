package org.quyq.gwsu.common.log.serverfile.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.log.serverfile.config.ServerFileLogProperties;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchRequest;
import org.springframework.core.env.StandardEnvironment;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalLogFileServiceImplTest {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @TempDir
    Path logDirectory;

    @Test
    void shouldSearchPlainAndGzipLogsAndReadContext() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(123_000_000);
        LocalDateTime yesterday = now.minusDays(1);
        Files.writeString(
                logDirectory.resolve("out.log"),
                line(now.minusMinutes(1), "start")
                        + line(now, "[tid-current] current hit " + "x".repeat(9000)),
                StandardCharsets.UTF_8);
        writeGzip(
                logDirectory.resolve("out-" + yesterday.toLocalDate() + ".1.log.gz"),
                line(yesterday, "[tid-archive] archive hit"));

        LocalLogFileServiceImpl service = service(1024 * 1024);
        var plainResult = service.search(new LocalLogSearchRequest(
                "current hit", null, yesterday.minusHours(1), now.plusHours(1), 10));
        var gzipResult = service.search(new LocalLogSearchRequest(
                null, "tid-archive", yesterday.minusHours(1), now.plusHours(1), 1));
        var context = service.getContext(new LocalLogContextRequest("out.log", 1, 10));

        assertEquals(1, plainResult.matches().size());
        assertEquals("out.log", plainResult.matches().getFirst().fileName());
        assertTrue(plainResult.matches().getFirst().preview().length() < 600);
        assertEquals(1, gzipResult.matches().size());
        assertTrue(gzipResult.matches().getFirst().fileName().endsWith(".gz"));
        assertEquals(2, context.lines().size());
        assertTrue(context.lines().getLast().text().length() < 8300);
        assertTrue(context.endOfFile());
    }

    @Test
    void shouldPreferNewestFilesWhenScanLimitIsReachedAndRejectPathTraversal() throws Exception {
        LocalDateTime now = LocalDateTime.now().withNano(123_000_000);
        LocalDate yesterday = now.toLocalDate().minusDays(1);
        Files.writeString(
                logDirectory.resolve("out-" + yesterday + ".1.log"),
                line(now.minusDays(1), "x".repeat(1024)),
                StandardCharsets.UTF_8);
        Files.writeString(
                logDirectory.resolve("out.log"),
                line(now, "newest target"),
                StandardCharsets.UTF_8);

        LocalLogFileServiceImpl service = service(256);
        var result = service.search(new LocalLogSearchRequest(
                "newest target", null, now.minusDays(2), now.plusHours(1), 10));

        assertEquals(1, result.matches().size());
        assertTrue(result.truncated());
        assertThrows(
                BusinessException.class,
                () -> service.getContext(new LocalLogContextRequest("../out.log", 1, 10)));
    }

    @Test
    void shouldReturnEarliestTidMatchAndLatestTenKeywordMatches() throws Exception {
        LocalDateTime start = LocalDateTime.now().withNano(123_000_000).minusMinutes(20);
        StringBuilder content = new StringBuilder();
        content.append(line(start, "[same-tid] first service call"));
        content.append(line(start.plusSeconds(1), "[same-tid] later service call"));
        for (int index = 0; index < 12; index++) {
            content.append(line(start.plusMinutes(index + 2), "keyword hit " + index));
        }
        Files.writeString(logDirectory.resolve("out.log"), content, StandardCharsets.UTF_8);

        LocalLogFileServiceImpl service = service(1024 * 1024);
        var tidResult = service.search(new LocalLogSearchRequest(
                null, "same-tid", start.minusMinutes(1), start.plusHours(1), 1));
        var keywordResult = service.search(new LocalLogSearchRequest(
                "keyword hit", null, start.minusMinutes(1), start.plusHours(1), 10));

        assertEquals(1, tidResult.matches().size());
        assertEquals(start, tidResult.matches().getFirst().logTime());
        assertEquals(10, keywordResult.matches().size());
        assertEquals("keyword hit 2", message(keywordResult.matches().getFirst().preview()));
        assertEquals("keyword hit 11", message(keywordResult.matches().getLast().preview()));
        assertTrue(keywordResult.truncated());
    }

    private LocalLogFileServiceImpl service(long maxScanBytes) {
        ServerFileLogProperties properties = new ServerFileLogProperties(
                true,
                logDirectory.toString(),
                8192,
                100,
                500,
                7,
                maxScanBytes,
                500,
                8192,
                StandardCharsets.UTF_8.name());
        return new LocalLogFileServiceImpl(properties, new StandardEnvironment());
    }

    private String line(LocalDateTime time, String message) {
        return TIME_FORMATTER.format(time) + " INFO " + message + System.lineSeparator();
    }

    private String message(String line) {
        return line.substring(line.indexOf(" INFO ") + 6);
    }

    private void writeGzip(Path file, String content) throws Exception {
        try (var output = new GZIPOutputStream(Files.newOutputStream(file));
             var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {
            writer.write(content);
        }
    }
}
