package org.quyq.gwsu.common.ai.agui;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.micrometer.observation.Observation;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.dto.ChatDTO;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.model.AIRunnerInstanceWrapper;
import org.quyq.gwsu.common.ai.agui.model.CopilotKitInfo;
import org.quyq.gwsu.common.ai.agui.model.RunAgentInput;
import org.quyq.gwsu.common.ai.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.push.AguiEventPusher;
import org.quyq.gwsu.common.ai.agui.utils.WebToolUtils;
import org.quyq.gwsu.common.ai.agui.web.WebToolCallbackRequest;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
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

import java.util.*;
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

    private final List<AguiEventPusher> pushers = new ArrayList<>();


    @Setter
    private AgentStateStore agentStateStore;


    public static AIRunnerInstanceWrapper getCurrEmitter(String threadId) {
        return CURR_EMITTER.get(threadId);
    }

    public void addPusher(AguiEventPusher pusher) {
        pushers.add(pusher);
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
     * 处理审批状态查询
     * 从 Session 中加载 Agent，检查是否处于 STOP_REQUESTED 状态
     *
     * @param threadId 会话ID
     * @return 审批状态信息
     */
    public R<HumanApprovalInfo> handleApprovalStatus(String threadId) {
        if (agentStateStore == null) {
            return R.ok(new HumanApprovalInfo(null, null, null));
        }

        try (ReActAgent agent = ReActAgent.builder()
                .stateStore(agentStateStore)
                .build()) {
            String userId = getCurrUserId();


            List<Msg> context = agent.getAgentState(userId, threadId)
                    .getContext();
            if (CollectionUtils.isEmpty(context)) {
                return R.ok(new HumanApprovalInfo(null, null, null));
            }
            Msg lastMsg = findLastAssistantMsg(context);
            if (lastMsg != null) {
                HumanApprovalInfo approvalInfo = AgentApprovalResolver.buildReasoningApprovalInfo(
                        lastMsg.getContentBlocks(ToolUseBlock.class));
                if (approvalInfo != null) {
                    return R.ok(approvalInfo);
                }
            }

            return R.ok(new HumanApprovalInfo(null, null, null));
        } catch (Exception e) {
            log.error("Failed to query approval status for thread {}: {}", threadId, e.getMessage());
            return R.ok(new HumanApprovalInfo(null, null, null));
        }
    }

    /**
     * 从消息列表中获取最后一条消息
     */
    private Msg findLastAssistantMsg(List<Msg> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg msg = messages.get(i);
            if (msg.getRole() == io.agentscope.core.message.MsgRole.ASSISTANT) {
                return msg;
            }
        }
        return null;
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
     * 智能体运行完成后的钩子。
     */
    protected void afterRunCompleted(RunAgentInput input, String userId, RuntimeContext runtimeContext) {
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
        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(request.body(), emitter, false, pushers);
        executorService.submit(() -> {
            wrapper.sendEvent(new AguiEvent.RunStarted(body.threadId(), body.runId()));
            wrapper.sendEvent(new AguiEvent.RunFinished(body.threadId(), body.runId()));
            emitter.complete();
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
                RuntimeContext context = RuntimeContext.builder()
                        .sessionId(threadId)
                        .userId(userId)
                        .build();
                Agent agent = processor.resolveAgent(agentId, context);
                if(agent instanceof HarnessAgent ha){
                    ha.interrupt(context);
                }else {
                    agent.interrupt();
                }


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
        String threadId = input.threadId();
        String runId = input.runId();
        String userId = getCurrUserId();


        // 在Servlet线程上提前捕获headers，避免进入虚拟线程后
        // processor.process()内部的enableAutomaticContextPropagation清除ThreadLocal
        Map<String, String> capturedHeaders = ServletUtils.LOCAL_HEADERS.get();
        //传递到reactor中，保证tid正确
        Observation observation = ObservationThreadLocalAccessor.getInstance().getValue();

        AIRunnerInstanceWrapper wrapper = new AIRunnerInstanceWrapper(input, emitter, isHeadless(), pushers);
        RuntimeContext runtimeContext =
                buildRuntimeContext(threadId, userId, input.forwardedProps(), wrapper);
        executorService.submit(
                () -> {
                    Disposable subscription;
                    try {

                        // Process request - returns both agent and event stream
                        AguiRequestProcessor.ProcessResult result =
                                processor.process(input, headerAgentId, pathAgentId, runtimeContext);
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
                                    result.agent().interrupt();
                                });
                        emitter.onError(
                                (ex) -> {
                                    log.info(
                                            "SSE connection error for run {}: {}, interrupting agent",
                                            runId,
                                            ex.getMessage());
                                    CURR_EMITTER.remove(threadId);
                                    result.agent().interrupt();
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
                                                        afterRunCompleted(input, userId, runtimeContext);
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
                        log.error("Error processing AG-UI request: {}", e);
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
        AguiEvent.Raw errorEvent = new AguiEvent.Raw(param.threadId(), param.runId(), Map.of("error", errorMessage));
        AguiEvent.RunFinished finishEvent = new AguiEvent.RunFinished(param.threadId(), param.runId());

        wrapper.sendEvent(errorEvent);
        wrapper.sendEvent(finishEvent);

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
