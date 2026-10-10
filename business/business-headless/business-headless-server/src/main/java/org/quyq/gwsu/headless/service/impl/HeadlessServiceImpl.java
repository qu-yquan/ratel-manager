package org.quyq.gwsu.headless.service.impl;


import cn.hutool.core.util.IdUtil;
import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.state.Reducer;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.config.properties.universal.BaseProjectInfoProperties;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.HeadlessResourceDTO;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.core.session.HeadlessAccessSession;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.enums.GraphRouteType;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import org.quyq.gwsu.headless.graph.HeadlessAguiEvents;
import org.quyq.gwsu.headless.graph.HeadlessGraphState;
import org.quyq.gwsu.headless.graph.HeadlessGraphStateSerializer;
import org.quyq.gwsu.headless.graph.IntentRecognitionNode;
import org.quyq.gwsu.headless.graph.SendAnswerNode;
import org.quyq.gwsu.headless.graph.SendApprovalNode;
import org.quyq.gwsu.headless.graph.SendChatNode;
import org.quyq.gwsu.headless.service.IHeadlessService;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import org.quyq.gwsu.kit.api.utils.FileUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeadlessServiceImpl implements IHeadlessService, InitializingBean {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final SecurityUtils securityUtils;

    private final CacheUtils cacheUtils;

    private CompiledGraph<HeadlessGraphState> headlessGraph;

    private final String lockKey = "headless:lock:%s";


    @Override
    public Flux<AguiEvent> stream(HeadlessDTO request, HeadlessCallConfig config) {
        AssertUtils.notNull(request, HeadlessErrorCode.E01007);
        validateRequest(request);
        HeadlessDTO normalizedRequest = normalizeRequest(request);
        String query = buildRoutingQuery(normalizedRequest);
        String userId = config.getUserId();
        if (!StringUtils.hasText(userId)) {
            userId = securityUtils.userInfo().map(UserInfo::getUserId).orElse("");
        }
        SubjectInfo subjectInfo = new SubjectInfo(config.getSign(), userId);
        AssertUtils.hasText(userId, HeadlessErrorCode.E01005);

        String threadId = StringUtils.hasText(config.getThreadId()) ? config.getThreadId()
                : Optional.ofNullable(headlessBrowserManager.getAccessSession(subjectInfo))
                .map(HeadlessAccessSession::threadId)
                .orElse(IdUtil.fastUUID());
        String keySign = StringUtils.hasText(config.getSign()) ? config.getSign() + ":" + userId : userId;
        return Flux.defer(() -> streamWithLock(
                query,
                normalizedRequest,
                subjectInfo,
                threadId,
                cacheUtils.getLock(lockKey.formatted(keySign))));
    }

    private Flux<AguiEvent> streamWithLock(String query,
                                           HeadlessDTO request,
                                           SubjectInfo subjectInfo,
                                           String threadId,
                                           RLock lock) {
        try {
            // leaseTime=-1：锁不自动过期，持有到操作完成才释放
            if (!lock.tryLock(0, -1, TimeUnit.SECONDS)) {
                return Flux.just(
                        HeadlessAguiEvents.status(threadId, "", HeadlessAgentStatus.BUSY),
                        HeadlessAguiEvents.raw(threadId, "", "BUSY", "助手正在回答中，请稍后尝试...")
                );
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return Flux.error(error);
        }

        AtomicReference<String> runIdRef = new AtomicReference<>("");
        Flux<AguiEvent> output;
        try {
            HeadlessGraphState finalState = headlessGraph.invoke(GraphInput.args(Map.of(
                            HeadlessConstants.Headless.GRAPH_PARAM_QUERY, query,
                            HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, request,
                            HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, subjectInfo,
                            HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, threadId
                    )), RunnableConfig.empty())
                    .orElseThrow(() -> new IllegalStateException("无头浏览器工作流未返回最终状态"));
            output = outputFlux(finalState);
        } catch (RuntimeException error) {
            unlock(lock);
            return errorEvents(threadId, runIdRef.get(), error);
        }

        return output
                .doOnNext(event -> {
                    if (StringUtils.hasText(event.getRunId())) {
                        runIdRef.set(event.getRunId());
                    }
                })
                .startWith(HeadlessAguiEvents.status(threadId, "", HeadlessAgentStatus.CONNECTION))
                .onErrorResume(error -> errorEvents(threadId, runIdRef.get(), error))
                .doFinally(signal -> unlock(lock));
    }

    @SuppressWarnings("unchecked")
    private Flux<AguiEvent> outputFlux(HeadlessGraphState state) {
        Object output = state.value(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT)
                .orElseGet(Flux::empty);
        if (!(output instanceof Flux<?> flux)) {
            throw new IllegalStateException("无头浏览器工作流输出类型错误: " + output.getClass().getName());
        }
        return (Flux<AguiEvent>) flux;
    }

    private Flux<AguiEvent> errorEvents(String threadId, String runId, Throwable error) {
        log.error("智能体流式处理异常: {}", error.getMessage(), error);
        return Flux.just(
                HeadlessAguiEvents.status(threadId, runId, HeadlessAgentStatus.ERROR),
                HeadlessAguiEvents.raw(threadId, runId, error.getMessage())
        );
    }

    private void unlock(RLock lock) {
        try {
            if (lock.isLocked()) {
                lock.forceUnlock();
            }
        } catch (RuntimeException error) {
            log.warn("释放无头浏览器用户锁失败", error);
        }
    }

    public CompiledGraph<HeadlessGraphState> buildGraph() throws GraphStateException {

        IntentRecognitionNode intentRecognitionNode = new IntentRecognitionNode(agentStateStore, headlessBrowserManager);
        SendChatNode sendChatNode = new SendChatNode(agentStateStore, headlessBrowserManager);
        SendAnswerNode sendAnswerNode = new SendAnswerNode(agentStateStore, headlessBrowserManager);
        SendApprovalNode sendApprovalNode = new SendApprovalNode(agentStateStore, headlessBrowserManager);

        Map<String, Channel<?>> channels = new HashMap<>();
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_QUERY, channelWithoutDefault());
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST, channelWithoutDefault());
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID, channelWithoutDefault());
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID, channelWithoutDefault());
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO, channelWithoutDefault());
        channels.put(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, channelWithoutDefault());

        StateGraph<HeadlessGraphState> graph = new StateGraph<>(channels, new HeadlessGraphStateSerializer())
                .addNode("intentRecognitionNode", AsyncNodeAction.node_async(intentRecognitionNode))
                .addNode("sendChatNode", AsyncNodeAction.node_async(sendChatNode))
                .addNode("sendAnswerNode", AsyncNodeAction.node_async(sendAnswerNode))
                .addNode("sendApprovalNode", AsyncNodeAction.node_async(sendApprovalNode))
                .addEdge(StateGraph.START, "intentRecognitionNode")
                .addConditionalEdges("intentRecognitionNode", AsyncEdgeAction.edge_async(state ->
                                state.<RouterInfo>value(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO)
                                        .map(v -> v.getType().name()).orElse(GraphRouteType.UNKNOWN.name())
                        ),
                        Map.of(
                                "UNKNOWN", StateGraph.END,
                                "CHAT", "sendChatNode",
                                "APPROVAL", "sendApprovalNode",
                                "ANSWER", "sendAnswerNode"
                        )
                )
                .addEdge("sendChatNode", StateGraph.END)
                .addEdge("sendAnswerNode", StateGraph.END)
                .addEdge("sendApprovalNode", StateGraph.END);
        return graph.compile();
    }

    static <T> Channel<T> channelWithoutDefault() {
        return Channels.base((Reducer<T>) null);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        headlessGraph = buildGraph();
    }

    private void validateRequest(HeadlessDTO request) {
        if (request.resources() != null) {
            for (HeadlessResourceDTO resource : request.resources()) {
                AssertUtils.notNull(resource, HeadlessErrorCode.E01008);
                AssertUtils.hasText(resource.fileId(), HeadlessErrorCode.E01009);
            }
        }
        AssertUtils.isTrue(request.hasContent(), HeadlessErrorCode.E01007);
    }

    private HeadlessDTO normalizeRequest(HeadlessDTO request) {
        if (CollectionUtils.isEmpty(request.resources())) {
            return request;
        }
        List<HeadlessResourceDTO> normalizedResources = request.resources().stream()
                .filter(Objects::nonNull)
                .map(this::normalizeResource)
                .toList();
        return new HeadlessDTO(
                request.text(),
                normalizedResources,
                request.threadId(),
                request.outputPanelScreenshot(),
                request.approvalRecording()
        );
    }

    private String buildRoutingQuery(HeadlessDTO request) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(request.text())) {
            parts.add(request.text().trim());
        }
//        if (!CollectionUtils.isEmpty(request.resources())) {
//            String resourceSummary = request.resources().stream()
//                    .filter(Objects::nonNull)
//                    .map(this::describeResource)
//                    .filter(StringUtils::hasText)
//                    .reduce((left, right) -> left + "；" + right)
//                    .orElse("用户附带了资源");
//            parts.add("用户附带了%s个资源：%s".formatted(request.resources().size(), resourceSummary));
//        }
        return String.join("\n", parts);
    }

    private String describeResource(HeadlessResourceDTO resource) {
        if (resource == null || !StringUtils.hasText(resource.fileId())) {
            return null;
        }
        String mimeType = StringUtils.hasText(resource.mimeType()) ? resource.mimeType() : "unknown";
        String fileName = StringUtils.hasText(resource.fileName()) ? resource.fileName() : resource.fileId();
        return "资源(fileName=%s,mimeType=%s)".formatted(fileName, mimeType);
    }

    private HeadlessResourceDTO normalizeResource(HeadlessResourceDTO resource) {
        KitFileInfoVO fileInfo = FileUtils.getFileInfo(resource.fileId());
        AssertUtils.notNull(fileInfo, HeadlessErrorCode.E01010);
        AssertUtils.hasText(fileInfo.getFileId(), HeadlessErrorCode.E01010);

        String mimeType = StringUtils.hasText(fileInfo.getMediaType()) ? fileInfo.getMediaType() : resource.mimeType();
        AssertUtils.hasText(mimeType, HeadlessErrorCode.E01011);

        String fileName = StringUtils.hasText(fileInfo.getFileName()) ? fileInfo.getFileName() : resource.fileName();
        String url = buildAbsoluteFileUrl(fileInfo.getFileId());
        return new HeadlessResourceDTO(fileInfo.getFileId(), url, mimeType, fileName);
    }

    private String buildAbsoluteFileUrl(String fileId) {
        BaseProjectInfoProperties properties = ConfigInfoUtils.getByObject(
                BaseProjectInfoProperties.CONFIG_KEY,
                BaseProjectInfoProperties.class
        );
        AssertUtils.notNull(properties, HeadlessErrorCode.E01010);
        AssertUtils.hasText(properties.apiBaseUrl(), HeadlessErrorCode.E01010);
        return "%s/kit/file/stream/%s".formatted(properties.apiBaseUrl(), fileId);
    }
}
