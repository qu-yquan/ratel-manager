package org.quyq.gwsu.log.serverfile.service.impl;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.DistributedServerInfo;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogContextResult;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchRequest;
import org.quyq.gwsu.common.log.serverfile.domain.LocalLogSearchResult;
import org.quyq.gwsu.common.log.serverfile.service.LocalLogFileService;
import org.quyq.gwsu.log.api.dto.ServerLogContextDTO;
import org.quyq.gwsu.log.api.dto.ServerLogSearchDTO;
import org.quyq.gwsu.log.api.vo.ServerLogContextVO;
import org.quyq.gwsu.log.api.vo.ServerLogOptionsVO;
import org.quyq.gwsu.log.api.vo.ServerLogSearchVO;
import org.quyq.gwsu.log.errcode.LogErrorCode;
import org.quyq.gwsu.log.serverfile.service.IServerLogService;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class ServerLogServiceImpl implements IServerLogService {

    private static final int KEYWORD_RESULT_LIMIT = 10;
    private static final String INTERNAL_BASE_PATH = "/internal/log-files";
    private static final String LOCAL_TARGET_ID = "local";

    private final LocalLogFileService localLogFileService;
    private final ExecutorService searchExecutor = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public ServerLogOptionsVO getOptions() {
        if (DeployUtils.isSingle()) {
            return new ServerLogOptionsVO(false, List.of());
        }
        List<KeyValue<String, String>> services = DeployUtils.getDistributedServerModuleMapping()
                .entrySet()
                .stream()
                .filter(entry -> !"log".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(
                        java.util.stream.Collectors.toMap(
                                DistributedServerInfo::applicationName,
                                DistributedServerInfo::note,
                                (left, right) -> left))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> new KeyValue<>(entry.getKey(), entry.getValue()))
                .toList();
        return new ServerLogOptionsVO(true, services);
    }

    @Override
    public ServerLogSearchVO search(ServerLogSearchDTO query) {
        long startNanos = System.nanoTime();
        LocalLogSearchRequest localRequest = toLocalRequest(query);
        if (DeployUtils.isSingle()) {
            LocalLogSearchResult result = localLogFileService.search(localRequest);
            List<ServerLogSearchVO.Group> rawGroups = result.matches().isEmpty()
                    ? List.of()
                    : List.of(toGroup(localTarget(), result));
            List<ServerLogSearchVO.Group> groups = arrangeGroups(rawGroups, query.getTid());
            return new ServerLogSearchVO(
                    groups,
                    List.of(),
                    result.truncated(),
                    elapsedMillis(startNanos));
        }

        List<Target> targets = listDistributedTargets(query.getServiceNames());
        List<CompletableFuture<SearchOutcome>> futures = targets.stream()
                .map(target -> CompletableFuture.supplyAsync(
                        () -> searchTarget(target, localRequest), searchExecutor))
                .toList();

        List<ServerLogSearchVO.Group> groups = new ArrayList<>();
        List<ServerLogSearchVO.Warning> warnings = new ArrayList<>();
        boolean truncated = false;
        for (CompletableFuture<SearchOutcome> future : futures) {
            SearchOutcome outcome = future.join();
            if (outcome.group() != null) {
                groups.add(outcome.group());
            }
            if (outcome.warning() != null) {
                warnings.add(outcome.warning());
            }
            truncated |= outcome.truncated();
        }
        int originalMatchCount = groups.stream()
                .mapToInt(group -> group.matches().size())
                .sum();
        boolean tidSearch = StringUtils.hasText(query.getTid());
        groups = arrangeGroups(groups, query.getTid());
        truncated |= !tidSearch && originalMatchCount > KEYWORD_RESULT_LIMIT;
        return new ServerLogSearchVO(
                List.copyOf(groups),
                List.copyOf(warnings),
                truncated,
                elapsedMillis(startNanos));
    }

    static List<ServerLogSearchVO.Group> arrangeGroups(
            List<ServerLogSearchVO.Group> groups,
            String tid) {
        return StringUtils.hasText(tid)
                ? arrangeTidGroups(groups)
                : arrangeKeywordGroups(groups);
    }

    private static List<ServerLogSearchVO.Group> arrangeKeywordGroups(
            List<ServerLogSearchVO.Group> groups) {
        Map<String, ServerLogSearchVO.Group> groupByTarget = groups.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ServerLogSearchVO.Group::targetId,
                        group -> group,
                        (left, right) -> left));
        Map<String, List<ServerLogSearchVO.Match>> selectedMatches = groups.stream()
                .flatMap(group -> group.matches().stream().map(match -> new GroupMatch(group, match)))
                .sorted(Comparator.comparing(
                        item -> item.match().logTime(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(KEYWORD_RESULT_LIMIT)
                .collect(java.util.stream.Collectors.groupingBy(
                        item -> item.group().targetId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.mapping(
                                GroupMatch::match,
                                java.util.stream.Collectors.toList())));
        return selectedMatches.entrySet().stream()
                .map(entry -> {
                    ServerLogSearchVO.Group group = groupByTarget.get(entry.getKey());
                    List<ServerLogSearchVO.Match> matches = List.copyOf(entry.getValue());
                    return new ServerLogSearchVO.Group(
                            group.targetId(),
                            group.serviceName(),
                            group.serviceNote(),
                            group.instanceName(),
                            matches.getFirst().logTime(),
                            matches);
                })
                .toList();
    }

    private static List<ServerLogSearchVO.Group> arrangeTidGroups(
            List<ServerLogSearchVO.Group> groups) {
        Map<String, GroupMatch> earliestByService = new LinkedHashMap<>();
        groups.stream()
                .flatMap(group -> group.matches().stream().map(match -> new GroupMatch(group, match)))
                .sorted(Comparator.comparing(
                        item -> item.match().logTime(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> earliestByService.putIfAbsent(item.group().serviceName(), item));
        return earliestByService.values().stream()
                .map(item -> new ServerLogSearchVO.Group(
                        item.group().targetId(),
                        item.group().serviceName(),
                        item.group().serviceNote(),
                        item.group().instanceName(),
                        item.match().logTime(),
                        List.of(item.match())))
                .toList();
    }

    @Override
    public ServerLogContextVO getContext(ServerLogContextDTO query) {
        Target target = resolveTarget(query.getTargetId());
        LocalLogContextRequest request = new LocalLogContextRequest(
                query.getFileId(),
                query.getStartLine(),
                query.getLineCount());
        LocalLogContextResult result = target.local()
                ? localLogFileService.getContext(request)
                : post(target, INTERNAL_BASE_PATH + "/context", request,
                        new ParameterizedTypeReference<R<LocalLogContextResult>>() {
                        });
        return new ServerLogContextVO(
                result.startLine(),
                result.endLine(),
                result.beginningOfFile(),
                result.endOfFile(),
                result.lines().stream()
                        .map(line -> new ServerLogContextVO.Line(line.lineNumber(), line.text()))
                        .toList());
    }

    @Override
    public void download(String targetId, String fileId, HttpServletResponse response) throws IOException {
        Target target = resolveTarget(targetId);
        if (target.local()) {
            var resource = localLogFileService.getDownloadResource(fileId);
            response.setContentType(localLogFileService.getMediaType(fileId).toString());
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename*=UTF-8''" + URLEncoder.encode(
                            Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8));
            try (InputStream input = resource.getInputStream()) {
                input.transferTo(response.getOutputStream());
            }
            return;
        }

        String encodedFileId = URLEncoder.encode(fileId, StandardCharsets.UTF_8);
        URI uri = URI.create(target.uri() + INTERNAL_BASE_PATH + "/download?fileId=" + encodedFileId);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();
        try {
            HttpResponse<InputStream> remote = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofInputStream());
            if (remote.statusCode() >= 400) {
                throw new BusinessException("远程日志文件下载失败，状态码：" + remote.statusCode());
            }
            remote.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                    .ifPresent(response::setContentType);
            remote.headers().firstValue(HttpHeaders.CONTENT_DISPOSITION)
                    .ifPresent(value -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, value));
            try (InputStream input = remote.body()) {
                input.transferTo(response.getOutputStream());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("服务器日志下载被中断");
        }
    }

    @PreDestroy
    public void destroy() {
        searchExecutor.shutdownNow();
    }

    private LocalLogSearchRequest toLocalRequest(ServerLogSearchDTO query) {
        boolean hasContent = StringUtils.hasText(query.getContent());
        boolean hasTid = StringUtils.hasText(query.getTid());
        if (hasContent == hasTid) {
            throw new BusinessException(LogErrorCode.E01004);
        }
        LocalDateTime end = query.getEndTime() == null ? LocalDateTime.now() : query.getEndTime();
        LocalDateTime start = query.getStartTime() == null
                ? end.toLocalDate().atStartOfDay()
                : query.getStartTime();
        if (start.isAfter(end)) {
            throw new BusinessException(LogErrorCode.E01005);
        }
        return new LocalLogSearchRequest(
                query.getContent(),
                query.getTid(),
                start,
                end,
                hasTid ? 1 : KEYWORD_RESULT_LIMIT);
    }

    private SearchOutcome searchTarget(Target target, LocalLogSearchRequest request) {
        try {
            LocalLogSearchResult result = post(
                    target,
                    INTERNAL_BASE_PATH + "/search",
                    request,
                    new ParameterizedTypeReference<>() {
                    });
            ServerLogSearchVO.Group group = result.matches().isEmpty()
                    ? null
                    : toGroup(target, result);
            return new SearchOutcome(group, null, result.truncated());
        } catch (Exception e) {

            return new SearchOutcome(
                    null,
                    new ServerLogSearchVO.Warning(
                            target.serviceName(), target.instanceName(), e.getMessage()),
                    false);
        }
    }

    private ServerLogSearchVO.Group toGroup(Target target, LocalLogSearchResult result) {
        List<ServerLogSearchVO.Match> matches = result.matches().stream()
                .map(match -> new ServerLogSearchVO.Match(
                        match.fileId(),
                        match.fileName(),
                        match.lineNumber(),
                        match.logTime(),
                        match.preview()))
                .sorted(Comparator.comparing(
                        ServerLogSearchVO.Match::logTime,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        LocalDateTime latest = matches.stream()
                .map(ServerLogSearchVO.Match::logTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new ServerLogSearchVO.Group(
                target.targetId(),
                target.serviceName(),
                target.serviceNote(),
                target.instanceName(),
                latest,
                matches);
    }

    private List<Target> listDistributedTargets(List<String> selectedServices) {
        Map<String, DistributedServerInfo> mapping = DeployUtils.getDistributedServerModuleMapping();
        Set<String> selected = CollectionUtils.isEmpty(selectedServices)
                ? Set.of()
                : new HashSet<>(selectedServices);
        DiscoveryClient discoveryClient = SpringUtils.getBean(DiscoveryClient.class);
        List<Target> targets = new ArrayList<>();
        mapping.entrySet().stream()
                .filter(entry -> !"log".equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .distinct()
                .filter(info -> selected.isEmpty() || selected.contains(info.applicationName()))
                .forEach(info -> {
                    List<ServiceInstance> instances = discoveryClient.getInstances(info.applicationName());
                    for (int i = 0; i < instances.size(); i++) {
                        ServiceInstance instance = instances.get(i);
                        targets.add(new Target(
                                targetId(instance),
                                info.applicationName(),
                                info.note(),
                                "实例 " + (i + 1),
                                instance.getUri().toString(),
                                false));
                    }
                });
        return targets;
    }

    private Target resolveTarget(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            throw new BusinessException(LogErrorCode.E01006);
        }
        if (DeployUtils.isSingle()) {
            if (!LOCAL_TARGET_ID.equals(targetId)) {
                throw new BusinessException(LogErrorCode.E01006);
            }
            return localTarget();
        }
        return listDistributedTargets(List.of()).stream()
                .filter(target -> target.targetId().equals(targetId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(LogErrorCode.E01006));
    }

    private Target localTarget() {
        return new Target(
                LOCAL_TARGET_ID,
                "gwsu-application",
                "单体应用",
                "本机实例",
                null,
                true);
    }

    private String targetId(ServiceInstance instance) {
        String raw = instance.getServiceId() + ":" + instance.getHost() + ":" + instance.getPort();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256算法不可用", e);
        }
    }

    private <T> T post(
            Target target,
            String path,
            Object body,
            ParameterizedTypeReference<R<T>> responseType) {
        R<T> response = RestClient.create()
                .post()
                .uri(target.uri() + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(responseType);
        if (response == null || !response.isSuccess() || response.data() == null) {
            throw new BusinessException(response == null ? "远程日志接口无响应" : response.msg());
        }
        return response.data();
    }

    private long elapsedMillis(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
    }

    private record Target(
            String targetId,
            String serviceName,
            String serviceNote,
            String instanceName,
            String uri,
            boolean local
    ) {
    }

    private record SearchOutcome(
            ServerLogSearchVO.Group group,
            ServerLogSearchVO.Warning warning,
            boolean truncated
    ) {
    }

    private record GroupMatch(
            ServerLogSearchVO.Group group,
            ServerLogSearchVO.Match match
    ) {
    }
}
