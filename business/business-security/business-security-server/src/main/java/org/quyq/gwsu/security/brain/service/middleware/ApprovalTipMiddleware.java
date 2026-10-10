package org.quyq.gwsu.security.brain.service.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.Toolkit;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import org.springframework.util.CollectionUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.function.Function;

/**
 * 在 AgentScope v2 权限拦截进入 ASK 前，将审批提示写入工具调用和 assistant 消息 metadata。
 */
@RequiredArgsConstructor
public class ApprovalTipMiddleware implements MiddlewareBase {

    private final ObjectMapper objectMapper;

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext ctx,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return Flux.deferContextual(contextView -> {
            return collectApprovalTips(agent, ctx, input, contextView)
                    .flatMapMany(approvalTips -> {
                        if (approvalTips.isEmpty()) {
                            return next.apply(input);
                        }

                        List<ToolUseBlock> enrichedToolCalls =
                                enrichToolCalls(input.toolCalls(), approvalTips);
                        syncLastAssistantMsg(ctx, enrichedToolCalls);
                        return next.apply(new ActingInput(enrichedToolCalls));
                    });
        });
    }

    private Mono<List<ApprovalTips>> collectApprovalTips(
            Agent agent, RuntimeContext ctx, ActingInput input, ContextView contextView) {
        if (agent == null || input == null || input.toolCalls() == null || input.toolCalls().isEmpty()) {
            return Mono.just(List.of());
        }

        Toolkit toolkit = agent.getToolkit();
        AgentState agentState = RuntimeContext.resolveAgentState(ctx, agent);
        PermissionContextState permissionContext = agentState != null
                ? agentState.getPermissionContext()
                : PermissionContextState.builder().build();

        return Flux.fromIterable(input.toolCalls())
                .concatMap(toolCall -> evaluateApprovalTip(toolkit, permissionContext, toolCall)
                        // The middleware pre-check runs before AgentBase publishes its runtime context.
                        // Pass the current context explicitly so tool-level permission checks see the same state.
                        .contextWrite(reactorContext -> reactorContext
                                .putAll(contextView)
                                .put(AgentBase.RUNTIME_CONTEXT_KEY, ctx)))
                .filter(Objects::nonNull)
                .collectList();
    }

    private Mono<ApprovalTips> evaluateApprovalTip(
            Toolkit toolkit,
            PermissionContextState permissionContext,
            ToolUseBlock toolCall) {
        if (toolkit == null || toolCall == null) {
            return Mono.empty();
        }

        AgentTool agentTool = toolkit.getTool(toolCall.getName());
        if (!(agentTool instanceof ToolBase toolBase)) {
            return Mono.empty();
        }

        return toolBase.checkPermissions(toolCall.getInput(), permissionContext)
                .filter(decision -> decision.getBehavior() == PermissionBehavior.ASK)
                .map(decision -> new ApprovalTips(
                        toolCall.getId(),
                        toolCall.getName(),
                        decision.getMessage(),
                        ApprovalStage.POST_REASONING));
    }

    List<ToolUseBlock> enrichToolCalls(
            List<ToolUseBlock> toolCalls, List<ApprovalTips> approvalTips) {
        if (CollectionUtils.isEmpty(toolCalls) || CollectionUtils.isEmpty(approvalTips)) {
            return toolCalls;
        }

        Map<String, ApprovalTips> tipsByCallId = new LinkedHashMap<>();
        approvalTips.forEach(tip -> tipsByCallId.putIfAbsent(tip.callId(), tip));

        return toolCalls.stream()
                .map(toolCall -> attachApprovalTip(toolCall, tipsByCallId.get(toolCall.getId())))
                .toList();
    }

    private ToolUseBlock attachApprovalTip(ToolUseBlock toolCall, ApprovalTips approvalTips) {
        if (toolCall == null || approvalTips == null) {
            return toolCall;
        }

        Map<String, Object> metadata = new LinkedHashMap<>(toolCall.getMetadata());
        metadata.put(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY, objectMapper.writeValueAsString(approvalTips));
        return new ToolUseBlock(
                toolCall.getId(),
                toolCall.getName(),
                toolCall.getInput(),
                toolCall.getContent(),
                metadata,
                toolCall.getState());
    }

    private void syncLastAssistantMsg(RuntimeContext ctx, List<ToolUseBlock> toolCalls) {
        if (ctx == null || CollectionUtils.isEmpty(toolCalls)) {
            return;
        }

        AgentState agentState = RuntimeContext.resolveAgentState(ctx, null);
        if (agentState == null || agentState.contextMutable().isEmpty()) {
            return;
        }

        List<Msg> messages = agentState.contextMutable();
        Map<String, ToolUseBlock> toolCallsById = new LinkedHashMap<>();
        toolCalls.forEach(toolCall -> toolCallsById.put(toolCall.getId(), toolCall));
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg msg = messages.get(i);
            if (!isApprovalTargetAssistantMsg(msg, toolCallsById.keySet())) {
                continue;
            }

            List<ContentBlock> updatedContent = msg.getContent().stream()
                    .map(contentBlock -> {
                        if (contentBlock instanceof ToolUseBlock toolUseBlock) {
                            ToolUseBlock enrichedToolCall = toolCallsById.get(toolUseBlock.getId());
                            if (enrichedToolCall == null) {
                                return toolUseBlock;
                            }
                            return mergeToolCallMetadata(toolUseBlock, enrichedToolCall);
                        }
                        return contentBlock;
                    })
                    .toList();
            messages.set(i, msg.withContent(updatedContent));
            return;
        }
    }

    private ToolUseBlock mergeToolCallMetadata(ToolUseBlock originalToolCall, ToolUseBlock enrichedToolCall) {
        Map<String, Object> metadata = new LinkedHashMap<>(originalToolCall.getMetadata());
        metadata.putAll(enrichedToolCall.getMetadata());
        return new ToolUseBlock(
                originalToolCall.getId(),
                originalToolCall.getName(),
                originalToolCall.getInput(),
                originalToolCall.getContent(),
                metadata,
                originalToolCall.getState());
    }

    private boolean isApprovalTargetAssistantMsg(Msg msg, Set<String> targetToolCallIds) {
        if (msg == null || msg.getRole() != MsgRole.ASSISTANT || CollectionUtils.isEmpty(targetToolCallIds)) {
            return false;
        }

        return msg.getContentBlocks(ToolUseBlock.class).stream()
                .map(ToolUseBlock::getId)
                .anyMatch(targetToolCallIds::contains);
    }
}
