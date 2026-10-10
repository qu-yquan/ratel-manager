package org.quyq.gwsu.common.ai.agui.converter;

import static io.agentscope.core.agui.AguiInterruptConstants.INTERRUPT_KIND_PERMISSION_CONFIRM;
import static io.agentscope.core.agui.AguiInterruptConstants.METADATA_AGENTSCOPE_INTERRUPT_KIND;
import static io.agentscope.core.agui.AguiInterruptConstants.METADATA_REPLY_ID;
import static io.agentscope.core.agui.AguiInterruptConstants.METADATA_TOOL_CONTENT;
import static io.agentscope.core.agui.AguiInterruptConstants.METADATA_TOOL_INPUT;
import static io.agentscope.core.agui.AguiInterruptConstants.METADATA_TOOL_NAME;
import static io.agentscope.core.agui.AguiInterruptConstants.TOOL_CALL_INTERRUPT_REASON;

import io.agentscope.core.agui.adapter.strategy.AgentEventConverter;
import io.agentscope.core.agui.adapter.strategy.AguiStreamContext;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.UserConfirmResultEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.util.JsonUtils;
import org.quyq.gwsu.common.ai.loop.AgentApprovalResolver;
import org.quyq.gwsu.common.ai.loop.domain.ApprovalTips;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 AgentScope 权限确认事件转换为 AG-UI 中断，并恢复业务工具提供的审批提示。
 *
 * <p>除提示文案外，中断标识、响应结构和恢复所需元数据均与 AgentScope 官方转换器保持一致。
 */
public final class ApprovalInterruptEventConverter implements AgentEventConverter {

    private static final Map<String, Object> CONFIRM_RESPONSE_SCHEMA =
            Map.of(
                    "type",
                    "object",
                    "properties",
                    Map.of(
                            "approved",
                            Map.of("type", "boolean"),
                            "editedArgs",
                            Map.of(
                                    "type",
                                    "object",
                                    "description",
                                    "Full replacement of the tool args. Not merged."),
                            "reason",
                            Map.of(
                                    "type",
                                    "string",
                                    "description",
                                    "Optional explanation supplied when the tool call is denied.")),
                    "required",
                    List.of("approved"));

    @Override
    public Set<Class<? extends AgentEvent>> eventTypes() {
        return Set.of(RequireUserConfirmEvent.class, UserConfirmResultEvent.class);
    }

    @Override
    public void convert(AgentEvent event, AguiStreamContext context) {
        if (event instanceof UserConfirmResultEvent resultEvent) {
            adoptConfirmedToolCalls(resultEvent, context);
            return;
        }

        RequireUserConfirmEvent confirmEvent = (RequireUserConfirmEvent) event;
        for (ToolUseBlock toolUse : confirmEvent.getToolCalls()) {
            if (isBlank(toolUse.getId())) {
                throw new IllegalStateException(
                        "RequireUserConfirmEvent contains a tool call without a stable id");
            }
            context.addInterrupt(buildInterrupt(confirmEvent.getReplyId(), toolUse));
        }
    }

    private static void adoptConfirmedToolCalls(
            UserConfirmResultEvent resultEvent, AguiStreamContext context) {
        for (ConfirmResult result : resultEvent.getConfirmResults()) {
            if (result.getToolCall() != null) {
                context.adoptToolCall(result.getToolCall().getId());
            }
        }
    }

    /**
     * 根据持久化的待审批工具调用重建官方 AG-UI interrupt。
     *
     * <p>历史消息恢复需要复用与实时事件完全一致的中断结构，避免前端恢复后提交的
     * {@code interruptId}、{@code toolCallId} 或响应结构与原运行不一致。</p>
     */
    public static AguiEvent.Interrupt buildInterrupt(String replyId, ToolUseBlock toolUse) {
        String toolCallId = toolUse.getId();
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (!isBlank(toolUse.getName())) {
            metadata.put(METADATA_TOOL_NAME, toolUse.getName());
        }
        if (toolUse.getInput() != null && !toolUse.getInput().isEmpty()) {
            metadata.put(METADATA_TOOL_INPUT, toolUse.getInput());
        }
        metadata.put(METADATA_TOOL_CONTENT, JsonUtils.resolveToolCallArgsJson(toolUse));
        metadata.put(METADATA_AGENTSCOPE_INTERRUPT_KIND, INTERRUPT_KIND_PERMISSION_CONFIRM);
        if (!isBlank(replyId)) {
            metadata.put(METADATA_REPLY_ID, replyId);
        }

        return new AguiEvent.Interrupt(
                interruptId(replyId, toolCallId),
                TOOL_CALL_INTERRUPT_REASON,
                confirmMessage(toolUse),
                toolCallId,
                CONFIRM_RESPONSE_SCHEMA,
                null,
                Map.copyOf(metadata));
    }

    private static String confirmMessage(ToolUseBlock toolUse) {
        ApprovalTips approvalTips = AgentApprovalResolver.resolveApprovalTips(toolUse);
        if (approvalTips != null && !isBlank(approvalTips.tip())) {
            return approvalTips.tip();
        }

        String name = isBlank(toolUse.getName()) ? "tool" : toolUse.getName();
        return "Tool '" + name + "' requires user confirmation before execution";
    }

    private static String interruptId(String replyId, String toolCallId) {
        return isBlank(replyId) ? toolCallId : replyId + ":" + toolCallId;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
