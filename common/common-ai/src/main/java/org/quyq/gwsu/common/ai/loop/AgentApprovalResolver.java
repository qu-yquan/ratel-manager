package org.quyq.gwsu.common.ai.loop;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import org.quyq.gwsu.common.ai.constants.AIConstants;
import org.quyq.gwsu.common.ai.loop.ApprovalStage;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 统一解析 AgentState 中待确认的审批信息。
 */
public final class AgentApprovalResolver {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * AgentScope 2.0.4 在暂停权限确认时写入最后一条 assistant 消息的 metadata key。
     *
     * <p>该值才是 {@code RequireUserConfirmEvent.replyId}，不能使用可能已经滚动到下一轮的
     * {@link AgentState#getReplyId()} 重建 AG-UI interrupt ID。</p>
     */
    private static final String CONFIRM_REQUEST_REPLY_ID_METADATA_KEY =
            "agentscope_confirm_request_reply_id";

    /**
     * AgentScope 的工具挂起结果消息不会进入 AgentState，需要在事件仍可见时保存其真实 ID。
     */
    private static final String SUSPENDED_RESULT_ID_METADATA_KEY =
            "ratel_agui_suspended_result_id";

    private AgentApprovalResolver() {
    }

    public static AgentState resolveAgentState(Agent agent, String sessionId, String userId) {
        if (agent instanceof HarnessAgent harnessAgent) {
            return harnessAgent.getDelegate().getAgentState(userId, sessionId);
        }
        if (agent instanceof ReActAgent reactAgent) {
            return reactAgent.getAgentState(userId, sessionId);
        }
        return RuntimeContext.resolveAgentState(RuntimeContext.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build(), agent);
    }

    public static AgentState resolveAgentState(AgentStateStore agentStateStore, String stateKey, String sessionId, String userId) {
        if (agentStateStore == null || sessionId == null || sessionId.isBlank() || stateKey == null || stateKey.isBlank()) {
            return null;
        }
        return agentStateStore.get(userId, sessionId, stateKey, AgentState.class).orElse(null);
    }

    public static List<ToolUseBlock> findPendingApprovalToolCalls(Agent agent, String sessionId, String userId) {
        return findPendingApprovalToolCalls(resolveAgentState(agent, sessionId, userId));
    }

    public static List<ToolUseBlock> findPendingApprovalToolCalls(AgentState agentState) {
        if (agentState == null || CollectionUtils.isEmpty(agentState.getContext())) {
            return Collections.emptyList();
        }

        Msg lastAssistantMsg = findLastAssistantMsg(agentState);
        if (lastAssistantMsg == null || !lastAssistantMsg.hasContentBlocks(ToolUseBlock.class)) {
            return Collections.emptyList();
        }

        Set<String> completedToolCallIds = new HashSet<>();
        for (Msg contextMsg : agentState.getContext()) {
            for (ToolResultBlock block : contextMsg.getContentBlocks(ToolResultBlock.class)) {
                if (block.getId() != null && !block.getId().isBlank()) {
                    completedToolCallIds.add(block.getId());
                }
            }
        }

        return lastAssistantMsg.getContentBlocks(ToolUseBlock.class).stream()
                .filter(toolUseBlock -> toolUseBlock.getId() != null && !completedToolCallIds.contains(toolUseBlock.getId()))
                .toList();
    }

    /**
     * 获取当前待确认工具调用对应的原始回复 ID。
     *
     * <p>AgentScope 会把暂停事件使用的 replyId 持久化到最后一条 assistant 消息中。
     * {@code AgentState.replyId} 表示智能体后续回复槽位，暂停完成后两者可能不同。</p>
     */
    public static String resolvePendingRequestReplyId(AgentState agentState) {
        Msg lastAssistantMsg = findLastAssistantMsg(agentState);
        if (lastAssistantMsg != null && !CollectionUtils.isEmpty(lastAssistantMsg.getMetadata())) {
            Object persistedReplyId = lastAssistantMsg.getMetadata()
                    .get(CONFIRM_REQUEST_REPLY_ID_METADATA_KEY);
            if (persistedReplyId instanceof String replyId && StringUtils.hasText(replyId)) {
                return replyId;
            }
        }
        return agentState == null ? null : agentState.getReplyId();
    }

    /**
     * 将 {@code AgentResultEvent.result.id} 记录到发起挂起工具调用的 assistant 消息。
     *
     * <p>AgentScope 官方 AG-UI 转换器使用该 ID 构造工具挂起 interrupt，不能使用
     * {@link AgentState#getReplyId()} 代替。</p>
     */
    public static boolean recordSuspendedResultId(AgentState agentState, Msg suspendedResult) {
        if (agentState == null || suspendedResult == null || !StringUtils.hasText(suspendedResult.getId())) {
            return false;
        }
        Set<String> suspendedToolCallIds = suspendedResult.getContentBlocks(ToolResultBlock.class).stream()
                .filter(ToolResultBlock::isSuspended)
                .map(ToolResultBlock::getId)
                .filter(StringUtils::hasText)
                .collect(java.util.stream.Collectors.toSet());
        if (suspendedToolCallIds.isEmpty()) {
            return false;
        }

        List<Msg> context = agentState.contextMutable();
        for (int index = context.size() - 1; index >= 0; index--) {
            Msg message = context.get(index);
            if (message.getRole() != MsgRole.ASSISTANT
                    || message.getContentBlocks(ToolUseBlock.class).stream()
                    .map(ToolUseBlock::getId)
                    .noneMatch(suspendedToolCallIds::contains)) {
                continue;
            }
            Map<String, Object> metadata = new HashMap<>(message.getMetadata());
            metadata.put(SUSPENDED_RESULT_ID_METADATA_KEY, suspendedResult.getId());
            context.set(index, message.withMetadata(Map.copyOf(metadata)));
            return true;
        }
        return false;
    }

    /**
     * 获取官方工具挂起 interrupt 使用的原始 {@code AgentResultEvent.result.id}。
     */
    public static String resolveSuspendedResultId(AgentState agentState) {
        Msg lastAssistantMsg = findLastAssistantMsg(agentState);
        if (lastAssistantMsg == null || CollectionUtils.isEmpty(lastAssistantMsg.getMetadata())) {
            return null;
        }
        Object resultId = lastAssistantMsg.getMetadata().get(SUSPENDED_RESULT_ID_METADATA_KEY);
        return resultId instanceof String id && StringUtils.hasText(id) ? id : null;
    }

    public static HumanApprovalInfo buildReasoningApprovalInfo(List<ToolUseBlock> toolCalls) {
        if (CollectionUtils.isEmpty(toolCalls)) {
            return null;
        }

        List<HumanApprovalInfo.ReasoningStateInfo> reasoningInfos = toolCalls.stream()
                .map(toolCall -> {
                    ApprovalTips approvalTips = resolveApprovalTips(toolCall);
                    if (approvalTips == null) {
                        return null;
                    }
                    return new HumanApprovalInfo.ReasoningStateInfo(approvalTips.tip(), toolCall);
                })
                .filter(Objects::nonNull)
                .toList();
        if (CollectionUtils.isEmpty(reasoningInfos)) {
            return null;
        }
        return new HumanApprovalInfo(ApprovalStage.POST_REASONING, reasoningInfos, null);
    }

    public static ApprovalTips resolveApprovalTips(ToolUseBlock toolCall) {
        if (toolCall == null || CollectionUtils.isEmpty(toolCall.getMetadata())) {
            return null;
        }

        Object value = toolCall.getMetadata().get(AIConstants.MSG_METADATA_APPROVAL_TOOLS_KEY);
        if (value == null) {
            return null;
        }
        if (value instanceof ApprovalTips approvalTips) {
            return approvalTips;
        }
        if (value instanceof String text) {
            if (!StringUtils.hasText(text)) {
                return null;
            }
            try {
                return OBJECT_MAPPER.readValue(text, ApprovalTips.class);
            } catch (Exception ignored) {
                return null;
            }
        }
        try {
            return OBJECT_MAPPER.convertValue(value, ApprovalTips.class);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }


    private static Msg findLastAssistantMsg(AgentState agentState) {
        if (agentState == null || CollectionUtils.isEmpty(agentState.getContext())) {
            return null;
        }
        List<Msg> context = agentState.getContext();
        for (int i = context.size() - 1; i >= 0; i--) {
            Msg msg = context.get(i);
            if (msg.getRole() == MsgRole.ASSISTANT) {
                return msg;
            }
        }
        return null;
    }
}
