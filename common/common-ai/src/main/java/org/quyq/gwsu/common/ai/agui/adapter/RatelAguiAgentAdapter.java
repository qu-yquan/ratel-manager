package org.quyq.gwsu.common.ai.agui.adapter;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.adapter.strategy.AgentEventConverterRegistry;
import io.agentscope.core.agui.adapter.strategy.AguiStreamContext;
import io.agentscope.core.agui.converter.AguiMessageConverter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.harness.agent.HarnessAgent;
import org.quyq.gwsu.common.ai.agui.converter.CustomAguiMessageConverter;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AgentScope 官方 AG-UI 适配器的多模态扩展。
 *
 * <p>事件转换、interrupt/resume 和生命周期协议全部复用官方实现；这里只替换输入消息转换，
 * 补齐官方 2.0.4 尚未支持的 {@code DocumentInputContent -> DataBlock} 能力。</p>
 */
public final class RatelAguiAgentAdapter extends AguiAgentAdapter {

    private final Agent agent;
    private final AguiAdapterConfig config;
    private final CustomAguiMessageConverter messageConverter = new CustomAguiMessageConverter();
    private final AguiMessageConverter officialMessageConverter =
            new AguiMessageConverter();
    private final AgentEventConverterRegistry converterRegistry;

    public RatelAguiAgentAdapter(Agent agent, AguiAdapterConfig config) {
        super(agent, config);
        this.agent = Objects.requireNonNull(agent, "agent cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.converterRegistry = new AgentEventConverterRegistry(
                config.getEventConverters(),
                config.getEventEnrichers(),
                config.isEmitSubagentEventsAsNative());
    }

    @Override
    public Flux<AguiEvent> run(RunAgentInput input, RuntimeContext runtimeContext) {
        AtomicBoolean runStartedSeen = new AtomicBoolean(false);
        return Flux.<AguiEvent>defer(() -> {
            RuntimeContext effectiveContext = buildRuntimeContext(input, runtimeContext);
            List<Msg> messages = new ArrayList<>(messageConverter.toMsgList(input.getMessages()));
            messages.addAll(toResumeMessages(input, effectiveContext, messages));

            Flux<AgentEvent> agentEvents = streamEvents(messages, effectiveContext);
            AguiStreamContext streamContext = new AguiStreamContext(
                    input.getThreadId(), input.getRunId(), config, input);

            return agentEvents
                    .concatMapIterable(event -> converterRegistry.convert(event, streamContext))
                    .concatWith(Flux.defer(() -> Flux.fromIterable(
                            converterRegistry.enrich(null, streamContext.finishPendingEvents(), streamContext))))
                    .onErrorResume(error -> Flux.concat(
                            Flux.fromIterable(converterRegistry.enrich(
                            null, streamContext.finishPendingEvents(), streamContext)),
                            Flux.error(error)));
        }).doOnNext(event -> {
            if (event instanceof AguiEvent.RunStarted) {
                runStartedSeen.set(true);
            }
        }).onErrorResume(error -> Flux.fromIterable(
                errorEvents(input, error, !runStartedSeen.get())));
    }

    private List<AguiEvent> errorEvents(
            RunAgentInput input, Throwable error, boolean includeRunStarted) {
        String message = error.getMessage() != null
                ? error.getMessage()
                : error.getClass().getSimpleName();
        List<AguiEvent> events = new ArrayList<>();
        if (includeRunStarted) {
            events.add(new AguiEvent.RunStarted(
                    input.getThreadId(), input.getRunId(), null, input));
        }
        events.add(new AguiEvent.RunError(
                input.getThreadId(),
                input.getRunId(),
                message,
                error instanceof IllegalArgumentException ? "INVALID_INPUT_ERROR" : "INTERNAL_ERROR",
                System.currentTimeMillis(),
                null));
        if (config.isEmitRunFinishedAfterError()) {
            events.add(new AguiEvent.RunFinished(input.getThreadId(), input.getRunId()));
        }
        return List.copyOf(events);
    }

    private Flux<AgentEvent> streamEvents(List<Msg> messages, RuntimeContext runtimeContext) {
        Flux<AgentEvent> events;
        if (agent instanceof ReActAgent reActAgent) {
            events = Objects.requireNonNull(
                    reActAgent.streamEvents(messages, runtimeContext), "agent stream is null");
        } else if (agent instanceof HarnessAgent harnessAgent) {
            events = Objects.requireNonNull(
                    harnessAgent.streamEvents(messages, runtimeContext), "agent stream is null");
        } else {
            throw new IllegalStateException(
                    "当前 AG-UI 多模态扩展仅支持 ReActAgent/HarnessAgent，实际类型: " + agent.getClass().getName());
        }
        return events.doOnNext(event -> recordSuspendedResultId(event, runtimeContext));
    }

    private void recordSuspendedResultId(AgentEvent event, RuntimeContext runtimeContext) {
        if (!(event instanceof AgentResultEvent resultEvent)
                || resultEvent.getResult() == null
                || resultEvent.getResult().getGenerateReason() != GenerateReason.TOOL_SUSPENDED
                || resultEvent.getResult().getContentBlocks(ToolUseBlock.class).stream()
                .noneMatch(toolUse -> AIConstants.ToolName.ASK_USER_QUESTION.equals(toolUse.getName()))) {
            return;
        }
        ReActAgent reActAgent = agent instanceof HarnessAgent harnessAgent
                ? harnessAgent.getDelegate()
                : (ReActAgent) agent;
        if (AgentApprovalResolver.recordSuspendedResultId(
                reActAgent.getAgentState(runtimeContext), resultEvent.getResult())) {
            reActAgent.saveAgentState(runtimeContext);
        }
    }

    private List<Msg> toResumeMessages(
            RunAgentInput input, RuntimeContext runtimeContext, List<Msg> requestMessages) {
        if (!input.hasResume()) {
            return List.of();
        }
        RunAgentInput resumeOnly = RunAgentInput.builder()
                .threadId(input.getThreadId())
                .runId(input.getRunId())
                .messages(List.of())
                .resume(input.getResume())
                .build();
        List<Msg> resumeMessages =
                officialMessageConverter.toMsgList(resumeOnly, resumeInterrupts(runtimeContext));
        return removeDuplicateToolResults(requestMessages, resumeMessages);
    }

    /**
     * CopilotKit 官方 {@code useInterrupt.resolve()} 会同时发送 tool 消息和 resume 条目。
     * 多模态消息必须由本适配器单独转换，因此需要在合并官方 resume 转换结果时补回官方转换器的去重语义。
     * 审批恢复生成的是 USER 角色的 ConfirmResult 消息，不包含 ToolResultBlock，不会被过滤。
     */
    static List<Msg> removeDuplicateToolResults(
            List<Msg> requestMessages, List<Msg> resumeMessages) {
        Set<String> toolResultIds = collectToolResultIds(requestMessages);
        List<Msg> result = new ArrayList<>();
        for (Msg message : resumeMessages) {
            Set<String> messageToolResultIds = collectToolResultIds(List.of(message));
            if (!messageToolResultIds.isEmpty()
                    && messageToolResultIds.stream().anyMatch(toolResultIds::contains)) {
                continue;
            }
            result.add(message);
            toolResultIds.addAll(messageToolResultIds);
        }
        return List.copyOf(result);
    }

    private static Set<String> collectToolResultIds(List<Msg> messages) {
        Set<String> result = new HashSet<>();
        for (Msg message : messages) {
            if (message.getContent() == null) {
                continue;
            }
            for (ContentBlock block : message.getContent()) {
                if (block instanceof ToolResultBlock toolResult
                        && toolResult.getId() != null
                        && !toolResult.getId().isBlank()) {
                    result.add(toolResult.getId());
                }
            }
        }
        return result;
    }

    private Map<String, AguiEvent.Interrupt> resumeInterrupts(RuntimeContext runtimeContext) {
        Object value = runtimeContext.get(RUNTIME_CONTEXT_RESUME_INTERRUPTS_KEY);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, AguiEvent.Interrupt> interrupts = new LinkedHashMap<>();
        map.forEach((key, interrupt) -> {
            if (key instanceof String id && interrupt instanceof AguiEvent.Interrupt item) {
                interrupts.put(id, item);
            }
        });
        return Map.copyOf(interrupts);
    }
}
