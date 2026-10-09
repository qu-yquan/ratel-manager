package org.quyq.gwsu.common.ai.loop;


import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
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
import org.quyq.gwsu.common.ai.loop.domain.ApprovalResult;
import org.quyq.gwsu.common.ai.loop.domain.HumanApprovalInfo;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 统一解析 AgentState 中待确认的审批信息。
 */
public final class AgentApprovalResolver {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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


    public static List<ConfirmResult> buildConfirmResults(Agent agent, String sessionId, String userId, ApprovalResult approvalResult) {
        if (approvalResult == null) {
            return Collections.emptyList();
        }
        String rejectReason = approvalResult.isApproved() ? null : StringUtils.hasText(approvalResult.rejectReason()) ? approvalResult.rejectReason() : "用户拒绝";
        return findPendingApprovalToolCalls(agent, sessionId, userId).stream()
                .map(tool -> new ConfirmResult(approvalResult.isApproved(), tool, null, rejectReason))
                .toList();
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
