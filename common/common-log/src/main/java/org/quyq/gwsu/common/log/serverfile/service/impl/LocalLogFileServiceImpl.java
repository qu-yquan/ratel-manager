package org.quyq.gwsu.common.log.serverfile.service.impl;

import ch.qos.logback.classic.LoggerContext;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.log.serverfile.config.ServerFileLogProperties;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextResult;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchResult;
import org.quyq.gwsu.common.log.serverfile.service.LocalLogFileService;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * 基于 Java NIO 和 GZIP 流式读取的本地日志检索实现。
 */
public class LocalLogFileServiceImpl implements LocalLogFileService {

    private static final int KEYWORD_MATCH_LIMIT = 10;
    private static final DateTimeFormatter LOG_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Pattern ARCHIVE_FILE_PATTERN = Pattern.compile(
            "^out-(\\d{4}-\\d{2}-\\d{2})\\.(\\d+)\\.log(?:\\.gz)?$");

    private final ServerFileLogProperties properties;
    private final Environment environment;

    public LocalLogFileServiceImpl(ServerFileLogProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @Override
    public LocalLogSearchResult search(LocalLogSearchRequest request) {
        validateSearchRequest(request);
        Path root = resolveRoot(false);
        if (root == null) {
            return new LocalLogSearchResult(List.of(), false, 0);
        }

        boolean tidSearch = StringUtils.hasText(request.tid());
        int limit = tidSearch ? 1 : KEYWORD_MATCH_LIMIT;
        Deque<LocalLogSearchResult.Match> matches = new ArrayDeque<>(limit);
        long scannedBytes = 0;
        boolean truncated = false;

        try {
            List<Path> candidates = listCandidateFiles(root, request.startTime(), request.endTime());
            Deque<Path> selectedFiles = new ArrayDeque<>();
            for (int offset = 0; offset < candidates.size(); offset++) {
                int index = tidSearch ? offset : candidates.size() - 1 - offset;
                Path file = candidates.get(index);
                long fileSize = Files.size(file);
                if (scannedBytes + fileSize > properties.maxScanBytes()) {
                    truncated = true;
                    break;
                }
                scannedBytes += fileSize;
                if (tidSearch) {
                    selectedFiles.addLast(file);
                } else {
                    selectedFiles.addFirst(file);
                }
            }
            for (Path file : selectedFiles) {
                ScanOutcome outcome = scanFile(file, request, limit, matches);
                truncated |= outcome.truncated();
                if (outcome.stop()) {
                    break;
                }
            }
            return new LocalLogSearchResult(List.copyOf(matches), truncated, scannedBytes);
        } catch (IOException e) {
            throw new BusinessException("读取服务器日志失败：" + e.getMessage());
        }
    }

    @Override
    public LocalLogContextResult getContext(LocalLogContextRequest request) {
        if (request == null || !StringUtils.hasText(request.fileId())) {
            throw new BusinessException("日志文件标识不能为空");
        }
        if (request.startLine() < 1) {
            throw new BusinessException("日志起始行必须大于0");
        }
        if (request.lineCount() < 1 || request.lineCount() > properties.maxContextLines()) {
            throw new BusinessException("日志上下文行数超过限制");
        }

        Path file = resolveFile(request.fileId());
        List<LocalLogContextResult.Line> lines = new ArrayList<>(request.lineCount());
        long currentLine = 0;
        boolean eof;
        try (BufferedReader reader = openReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                currentLine++;
                if (currentLine < request.startLine()) {
                    continue;
                }
                if (lines.size() >= request.lineCount()) {
                    break;
                }
                lines.add(new LocalLogContextResult.Line(
                        currentLine,
                        abbreviate(line, properties.maxLineChars())));
            }
            eof = line == null;
        } catch (IOException e) {
            throw new BusinessException("读取日志上下文失败：" + e.getMessage());
        }

        long endLine = lines.isEmpty() ? request.startLine() - 1 : lines.getLast().lineNumber();
        return new LocalLogContextResult(
                request.startLine(),
                endLine,
                request.startLine() == 1,
                eof,
                List.copyOf(lines));
    }

    @Override
    public Resource getDownloadResource(String fileId) {
        return new FileSystemResource(resolveFile(fileId));
    }

    @Override
    public MediaType getMediaType(String fileId) {
        return fileId.endsWith(".gz")
                ? MediaType.parseMediaType("application/gzip")
                : new MediaType("text", "plain", properties.resolvedCharset());
    }

    private void validateSearchRequest(LocalLogSearchRequest request) {
        if (request == null) {
            throw new BusinessException("日志检索条件不能为空");
        }
        boolean hasContent = StringUtils.hasText(request.content());
        boolean hasTid = StringUtils.hasText(request.tid());
        if (hasContent == hasTid) {
            throw new BusinessException("关键词或TID必须且只能填写一项");
        }
        LocalDateTime start = request.startTime();
        LocalDateTime end = request.endTime();
        if (start != null && end != null && start.isAfter(end)) {
            throw new BusinessException(CommonErrorCode.E06001);
        }
        if (start != null && end != null
                && ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) >= properties.maxRangeDays()) {
            throw new BusinessException(CommonErrorCode.E06002);
        }
    }

    private ScanOutcome scanFile(
            Path file,
            LocalLogSearchRequest request,
            int limit,
            Deque<LocalLogSearchResult.Match> matches) throws IOException {
        boolean tidSearch = StringUtils.hasText(request.tid());
        boolean truncated = false;
        try (BufferedReader reader = openReader(file)) {
            String line;
            long lineNumber = 0;
            LocalDateTime currentTime = null;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                LocalDateTime parsedTime = parseLogTime(line);
                if (parsedTime != null) {
                    currentTime = parsedTime;
                    if (request.endTime() != null && currentTime.isAfter(request.endTime())) {
                        break;
                    }
                }
                if (!withinRange(currentTime, request.startTime(), request.endTime())) {
                    continue;
                }
                boolean match = StringUtils.hasText(request.tid())
                        ? line.contains("[" + request.tid().trim() + "]")
                        : line.contains(request.content());
                if (!match) {
                    continue;
                }
                LocalLogSearchResult.Match matchedLog = new LocalLogSearchResult.Match(
                        file.getFileName().toString(),
                        file.getFileName().toString(),
                        lineNumber,
                        currentTime,
                        abbreviate(line, properties.maxPreviewChars()));
                if (tidSearch) {
                    matches.addLast(matchedLog);
                    return new ScanOutcome(true, false);
                }
                if (matches.size() == limit) {
                    matches.removeFirst();
                    truncated = true;
                }
                matches.addLast(matchedLog);
            }
        }
        return new ScanOutcome(false, truncated);
    }

    private boolean withinRange(LocalDateTime time, LocalDateTime start, LocalDateTime end) {
        if (time == null) {
            return start == null && end == null;
        }
        return (start == null || !time.isBefore(start)) && (end == null || !time.isAfter(end));
    }

    private LocalDateTime parseLogTime(String line) {
        if (line.length() < 23) {
            return null;
        }
        try {
            return LocalDateTime.parse(line.substring(0, 23), LOG_TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String abbreviate(String value, int maxChars) {
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, maxChars) + " …[本行内容过长，已截断]";
    }

    private List<Path> listCandidateFiles(Path root, LocalDateTime start, LocalDateTime end) throws IOException {
        LocalDate startDate = start == null ? LocalDate.now() : start.toLocalDate();
        LocalDate endDate = end == null ? LocalDate.now() : end.toLocalDate();
        try (var files = Files.list(root)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> isCandidate(path.getFileName().toString(), startDate, endDate))
                    .sorted(Comparator.comparing(this::fileOrder))
                    .toList();
        }
    }

    private boolean isCandidate(String filename, LocalDate startDate, LocalDate endDate) {
        if ("out.log".equals(filename)) {
            LocalDate today = LocalDate.now();
            return !today.isBefore(startDate) && !today.isAfter(endDate);
        }
        Matcher matcher = ARCHIVE_FILE_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            return false;
        }
        LocalDate date = LocalDate.parse(matcher.group(1));
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private String fileOrder(Path path) {
        String filename = path.getFileName().toString();
        if ("out.log".equals(filename)) {
            return LocalDate.now() + ".999999";
        }
        Matcher matcher = ARCHIVE_FILE_PATTERN.matcher(filename);
        return matcher.matches()
                ? matcher.group(1) + "." + String.format("%06d", Integer.parseInt(matcher.group(2)))
                : filename;
    }

    private BufferedReader openReader(Path file) throws IOException {
        InputStream input = Files.newInputStream(file);
        if (file.getFileName().toString().endsWith(".gz")) {
            input = new GZIPInputStream(input, properties.readBufferSize());
        }
        return new BufferedReader(
                new InputStreamReader(input, properties.resolvedCharset()),
                properties.readBufferSize());
    }

    private record ScanOutcome(boolean stop, boolean truncated) {
    }

    private Path resolveFile(String fileId) {
        if (!StringUtils.hasText(fileId) || fileId.contains("/") || fileId.contains("\\")) {
            throw new BusinessException("非法日志文件标识");
        }
        Path root = Objects.requireNonNull(resolveRoot(true));
        Path candidate = root.resolve(fileId).normalize();
        try {
            if (!candidate.startsWith(root)
                    || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(candidate)) {
                throw new BusinessException("日志文件不存在或不可访问");
            }
            Path realFile = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realFile.startsWith(root)) {
                throw new BusinessException("日志文件路径越界");
            }
            return realFile;
        } catch (IOException e) {
            throw new BusinessException("日志文件不存在或不可访问：" + e.getMessage());
        }
    }

    private Path resolveRoot(boolean required) {
        String configuredPath = properties.path();
        if (!StringUtils.hasText(configuredPath)
                && LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {
            configuredPath = context.getProperty("log.path");
        }
        if (!StringUtils.hasText(configuredPath)) {
            configuredPath = "logs/" + environment.getProperty("spring.application.name", "application");
        }
        Path root = Path.of(configuredPath);
        if (!root.isAbsolute()) {
            root = Path.of(System.getProperty("user.dir")).resolve(root);
        }
        try {
            if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                if (required) {
                    throw new BusinessException("日志目录不存在");
                }
                return null;
            }
            return root.toRealPath(LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            if (required) {
                throw new BusinessException("日志目录不可访问：" + e.getMessage());
            }
            return null;
        }
    }
}
