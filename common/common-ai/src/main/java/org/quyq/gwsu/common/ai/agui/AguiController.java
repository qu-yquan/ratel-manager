package org.quyq.gwsu.common.ai.agui;


import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.runtime.AguiRuntimeContextRequest;
import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.model.AguiConnectionSnapshot;
import org.quyq.gwsu.common.ai.agui.model.CopilotKitInfo;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.accessor.HeadersContextThreadLocalAccessor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description
 */
@RequiredArgsConstructor
@Slf4j
public abstract class AguiController implements DisposableBean {

    private final static String AGENT_STOP_EVENT_TOPIC = "AGENT_STOP_EVENT_TOPIC:";

    private final AguiRequestProcessor processor;

    private final WebToolUtils webToolUtils;

    private final SecurityUtils securityUtils;

    private final SessionUtils sessionUtils;

    private final long sseTimeout;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    private final static EmitterWrapperManager CURR_EMITTER = new EmitterWrapperManager();

    private RedisMessageListenerContainer listenerContainer = null;

    public static AIRunnerInstanceWrapper getCurrEmitter(String threadId) {
        return CURR_EMITTER.get(threadId);
    }


    @Override
    public void destroy() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
        }
    }


    /**
     * CopilotKit Single Endpoint 统一入口
     * <p>
     * 根据 method 字段路由到不同的处理逻辑：
     * - info: 返回 runtime 信息（JSON）
     * - agent/connect: 返回 SSE 流
     * - agent/run: 返回 SSE 流
     * - agent/stop: 停止 agent（JSON）
     */
    public ResponseEntity<?> handleCopilotKitRequest(ChatDTO request, String headerAgentId) {
        return switch (request.method()) {
            case "info" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(handleInfo());
            case "agent/connect" -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(handlerAgentConnect(request));
            case "agent/run" -> ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(handleAgentRun(request, headerAgentId));
            case "agent/stop" -> ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(handleAgentStop(request, headerAgentId));
            default -> ResponseEntity.badRequest().body(R.fail("未知的方法：%s".formatted(request.method())));
        };

    }

    /**
     * 处理前端工具执行结果回调
     *
     * @param request 回调请求
     * @return 处理结果
     */
    public R<Void> handleToolCallback(WebToolCallbackRequest request) {
        boolean success = webToolUtils.handleCallback(request.toolCallId(), request.success(), request.result());
        if (!success) {
            return R.fail("工具回调处理失败: " + request.toolCallId());
        }
        return R.ok();
    }

    /**
     * agui格式统一入口
     *
     * @param input
     * @param agentId
     * @return
     */
    public SseEmitter handleAgui(
            RunAgentInput input, String agentId) {
        return handleInternal(input, agentId, null);
    }


    /**
     * 处理 info 请求
     * 返回 runtime 信息和可用的 agents
     */
    protected abstract CopilotKitInfo handleInfo();

    /**
     * 智能体开始执行前的钩子。
     *
     * <p>在 AgentScope 派生运行上下文前调用；需要在整个运行链路中共享的状态应在此初始化。</p>
     */
    protected void beforeRunStarted(RunAgentInput input, String userId, RuntimeContext runtimeContext) {
    }

    /**
     * 智能体运行完成后的钩子。
     */
    protected void afterRunCompleted(RunAgentInput input, String userId, RuntimeContext runtimeContext) {
    }

    /**
     * 恢复指定线程的 AG-UI 连接快照。
     *
     * <p>默认返回空快照；需要历史恢复的业务模块应覆盖该方法。该方法只用于读取，
     * 不应创建会话或修改智能体状态。</p>
     */
    protected AguiConnectionSnapshot restoreConnection(RunAgentInput input, String userId) {
        return AguiConnectionSnapshot.empty();
    }


    /**
     * 获取当前登录的用户ID
     *
     * @return
     */
    private String getCurrUserId() {
        return securityUtils.userInfo().map(UserInfo::getUserId).orElse(null);
    }


    protected SseEmitter handlerAgentConnect(ChatDTO request) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        RunAgentInput body = request.body();
        String userId = getCurrUserId();
        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(request.body(), emitter, false);
        executorService.submit(() -> {
            wrapper.sendEvent(new AguiEvent.RunStarted(
                    body.getThreadId(), body.getRunId(), null, body));
            try {
                AguiConnectionSnapshot snapshot = Objects.requireNonNull(
                        restoreConnection(body, userId), "connection snapshot cannot be null");
                wrapper.sendEvent(new AguiEvent.MessagesSnapshot(
                        body.getThreadId(), body.getRunId(), snapshot.messages()));
                wrapper.sendEvent(new AguiEvent.StateSnapshot(
                        body.getThreadId(), body.getRunId(), snapshot.state()));
                snapshot.customEvents().forEach(wrapper::sendEvent);
                AguiEvent.RunFinishedOutcome outcome = snapshot.interrupts().isEmpty()
                        ? new AguiEvent.RunFinishedSuccessOutcome()
                        : new AguiEvent.RunFinishedInterruptOutcome(snapshot.interrupts());
                wrapper.sendEvent(new AguiEvent.RunFinished(
                        body.getThreadId(), body.getRunId(), null, outcome));
                emitter.complete();
            } catch (Exception exception) {
                log.error("Restore AG-UI connection failed, threadId={}", body.getThreadId(), exception);
                sendErrorAndComplete(wrapper, exception.getMessage());
            }
        });

        return emitter;
    }

    /**
     * 处理 agent/run 和 agent/connect 请求
     * 返回 SSE 流
     */
    protected SseEmitter handleAgentRun(ChatDTO request, String headerAgentId) {
        if (!DeployUtils.isSingle()) {
            initStopListenerContainer();
        }

        RunAgentInput input = request.body();

        // 从 params 中获取 agentId（如果有）
        String pathAgentId = null;
        if (!CollectionUtils.isEmpty(request.params()) && request.params().containsKey("agentId")) {
            pathAgentId = (String) request.params().get("agentId");
        }

        return handleInternal(input, headerAgentId, pathAgentId);
    }

    /**
     * 处理 agent/stop 请求
     */
    protected Map<String, Object> handleAgentStop(ChatDTO request, String headerAgentId) {
        Map<String, Object> p = request.params();
        // 从 params 中获取 agentId 和 threadId
        String agentId = Optional.ofNullable(p.get("agentId")).map(String::valueOf).orElse(headerAgentId);
        String threadId = Optional.ofNullable(p.get("threadId")).map(String::valueOf).orElse(null);
        String userId = getCurrUserId();

        log.info("Agent stop requested: agentId={}, threadId={}", agentId, threadId);
        if (StringUtils.hasText(threadId) && Objects.isNull(getCurrEmitter(threadId))) {
            SpringUtils.getBean(CacheUtils.class)
                    .convertAndSend(getStopEventTopic(), new StopEventInfo(agentId, threadId, userId));
        } else {
            stopAgent(agentId, threadId, userId);
        }


        return Map.of("success", true);
    }

    private void stopAgent(String agentId, String threadId, String userId) {
        if (StringUtils.hasText(threadId)) {
            AIRunnerInstanceWrapper currEmitter = getCurrEmitter(threadId);
            if (Objects.nonNull(currEmitter)) {
                currEmitter.interrupt();
                currEmitter.emitter().complete();
            }

        }
    }

    /**
     * 处理 AG-UI 请求的核心逻辑
     */
    private SseEmitter handleInternal(
            RunAgentInput input, String headerAgentId, String pathAgentId) {
        SseEmitter emitter = new SseEmitter(sseTimeout);
        String threadId = input.getThreadId();
        String runId = input.getRunId();
        String userId = getCurrUserId();


        // 在Servlet线程上提前捕获headers，避免进入虚拟线程后
        // processor.process()内部的enableAutomaticContextPropagation清除ThreadLocal
        Map<String, String> capturedHeaders = ServletUtils.LOCAL_HEADERS.get();
        //传递到reactor中，保证tid正确
        Observation observation = ObservationThreadLocalAccessor.getInstance().getValue();

        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(input, emitter, isHeadless());
        RuntimeContext runtimeContext =
                buildRuntimeContext(threadId, userId, input.getForwardedProps(), wrapper);
        executorService.submit(
                () -> {
                    Disposable subscription;
                    try {
                        beforeRunStarted(input, userId, runtimeContext);

                        // Process request - returns both agent and event stream
                        AguiRuntimeContextRequest<RuntimeContext> request = AguiRuntimeContextRequest
                                .<RuntimeContext>builder()
                                .input(input)
                                .headerAgentId(headerAgentId)
                                .pathAgentId(pathAgentId)
                                .transport(AguiRuntimeContextRequest.Transport.MVC)
                                .nativeRequest(runtimeContext)
                                .build();
                        AguiRequestProcessor.ProcessResult result = processor.process(request);
                        wrapper.bindProcessResult(result);
                        CURR_EMITTER.put(threadId, wrapper);
                        // Set up callbacks for client disconnect handling
                        emitter.onCompletion(
                                () -> {
                                    log.debug("SSE connection completed for run {}", runId);
                                    CURR_EMITTER.remove(threadId);
                                });
                        emitter.onTimeout(
                                () -> {
                                    log.info(
                                            "SSE connection timed out for run {}, interrupting agent",
                                            runId);
                                    CURR_EMITTER.remove(threadId);
                                    result.interrupt(threadId);
                                });
                        emitter.onError(
                                (ex) -> {
                                    log.info(
                                            "SSE connection error for run {}: {}, interrupting agent",
                                            runId,
                                            ex.getMessage());
                                    CURR_EMITTER.remove(threadId);
                                    result.interrupt(threadId);
                                });

                        // Subscribe to event stream
                        subscription =
                                result.events()
                                        .contextCapture()
                                        .contextWrite(Context.of(
                                                HeadersContextThreadLocalAccessor.REACTOR_CONTEXT, capturedHeaders
                                                , ObservationThreadLocalAccessor.KEY, observation
                                        ))
                                        .subscribe(
                                                event -> sendEvent(wrapper, event),
                                                error -> {
                                                    log.error(
                                                            "Error during AG-UI run: {}",
                                                            error.getMessage());
                                                    sendErrorAndComplete(
                                                            wrapper,
                                                            error.getMessage());
                                                },
                                                () -> {
                                                    try {
                                                        afterRunCompleted(input, userId, result.runtimeContext());
                                                    } catch (Exception exception) {
                                                        log.warn("Run completed hook execute failed, threadId={}", threadId, exception);
                                                    }
                                                    try {
                                                        emitter.complete();
                                                    } catch (Exception e) {
                                                        log.debug(
                                                                "Error completing emitter: {}",
                                                                e.getMessage());
                                                    }
                                                });

                    } catch (AguiException.AgentNotFoundException e) {
                        log.error("Agent not found: {}", e.getMessage());
                        sendErrorAndComplete(wrapper, e.getMessage());
                    } catch (Exception e) {
                        log.error("Error processing AG-UI request", e);
                        sendErrorAndComplete(wrapper, e.getMessage());
                    }
                });

        return emitter;
    }

    static RuntimeContext buildRuntimeContext(
            String threadId, String userId, Map<String, Object> forwardedProps, AIRunnerInstanceWrapper wrapper) {
        RuntimeContext.Builder builder = RuntimeContext.builder()
                .sessionId(threadId)
                .userId(userId);
        if (!CollectionUtils.isEmpty(forwardedProps)) {
            builder.put(AIConstants.Param.FORWARDED_PROPS_KEY, forwardedProps);
        }
        builder.put(AIRunnerInstanceWrapper.class, wrapper);
        return builder.build();
    }

    /**
     * 是否是无头浏览形式访问
     *
     * @return
     */
    private boolean isHeadless() {
        String loginType = sessionUtils.getLoginType();
        return "headless".equals(loginType);
    }

    private void sendEvent(AIRunnerInstanceWrapper wrapper, AguiEvent event) {
        wrapper.sendEvent(event);
    }

    private void sendErrorAndComplete(
            AIRunnerInstanceWrapper wrapper, String errorMessage) {
        RunAgentInput param = wrapper.input();
        AguiEvent.RunError errorEvent = new AguiEvent.RunError(
                param.getThreadId(), param.getRunId(), errorMessage, "INTERNAL_ERROR");
        wrapper.sendEvent(errorEvent);
        wrapper.emitter().complete();
    }


    /**
     * 初始化智能体停止事件监听器
     * 为了解决分布式部署时，停止事件发送到其他微服务实例导致停止失败的问题
     */
    private void initStopListenerContainer() {
        if (Objects.nonNull(listenerContainer)) {
            return;
        }
        synchronized (AguiController.class) {
            if (Objects.isNull(listenerContainer)) {
                CacheUtils cacheUtils = SpringUtils.getBean(CacheUtils.class);
                listenerContainer = cacheUtils.addListener(getStopEventTopic(),
                        (message, pattern) -> {
                            Object msg = cacheUtils.getSerializer().deserialize(message.getBody());
                            if (msg instanceof StopEventInfo event) {
                                stopAgent(event.agentId, event.threadId, event.userId);
                            }

                        });
            }
        }
    }

    private String getStopEventTopic() {
        return AGENT_STOP_EVENT_TOPIC + this.getClass().getName();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StopEventInfo {
        private String agentId;
        private String threadId;
        private String userId;

    }


}
