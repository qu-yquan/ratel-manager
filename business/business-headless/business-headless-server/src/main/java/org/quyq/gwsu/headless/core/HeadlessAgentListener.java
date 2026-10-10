package org.quyq.gwsu.headless.core;

import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.headless.core.session.HeadlessPageWrapper;

import java.util.List;

/**
 * 无头浏览器智能体事件监听器
 * 所有回调在 SSE 事件到达时实时触发，无需等待流结束
 * <p>
 * 每个事件方法都携带 {@link HeadlessPageWrapper} 参数，
 * 监听器实现可在事件回调中直接通过 wrapper 对浏览器进行截图、录制等操作。
 */
public interface HeadlessAgentListener {

    // ==================== 通用回调 ====================

    /**
     * 收到任意 SSE 事件（总入口，先于具体回调执行）
     * 可用于日志记录、事件统计等
     */
    default void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {}

    // ==================== 生命周期事件 ====================

    /** RUN_STARTED：智能体开始运行 */
    default void onRunStarted(AguiEvent.RunStarted event, HeadlessPageWrapper wrapper) {}

    /** RUN_FINISHED：智能体运行结束 */
    default void onRunFinished(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {}

    // ==================== 文本消息事件 ====================

    /** TEXT_MESSAGE_START：文本消息开始 */
    default void onTextMessageStart(AguiEvent.TextMessageStart event, HeadlessPageWrapper wrapper) {}

    /**
     * TEXT_MESSAGE_CONTENT：文本消息增量内容
     * @param delta 本次增量文本片段
     */
    default void onTextMessageContent(String delta, HeadlessPageWrapper wrapper) {}

    /** TEXT_MESSAGE_END：文本消息结束 */
    default void onTextMessageEnd(AguiEvent.TextMessageEnd event, HeadlessPageWrapper wrapper) {}

    // ==================== 工具调用事件 ====================

    /** TOOL_CALL_START：工具调用开始 */
    default void onToolCallStart(AguiEvent.ToolCallStart event, HeadlessPageWrapper wrapper) {}

    /** TOOL_CALL_ARGS：工具调用参数增量 */
    default void onToolCallArgs(AguiEvent.ToolCallArgs event, HeadlessPageWrapper wrapper) {}

    /** TOOL_CALL_END：工具调用结束 */
    default void onToolCallEnd(AguiEvent.ToolCallEnd event, HeadlessPageWrapper wrapper) {}

    /** TOOL_CALL_RESULT：工具调用结果 */
    default void onToolCallResult(AguiEvent.ToolCallResult event, HeadlessPageWrapper wrapper) {}

    // ==================== 自定义事件 ====================

    /** RUN_FINISHED interrupt：官方 AG-UI 人工审批请求。 */
    default void onHumanApproval(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {}

    /**
     * AskUserQuestion：智能体提问
     * 通过 TOOL_CALL_START 的 toolCallName=AskUserQuestion 识别
     * 收到此事件后，调用方需通过 HeadlessBrowserManager.userAnswer() 提交回答
     *
     * @param threadId 当前 SSE 会话的线程 ID
     * @param toolCallId 工具调用 ID
     * @param questions 问题数据（从 TOOL_CALL_ARGS 中累积解析）
     */
    default void onAskUserQuestion(String threadId, String toolCallId, List<AskUserQuestionTool.QuestionParam> questions, HeadlessPageWrapper wrapper) {}

    /** TOOL_EXECUTE：前端工具执行请求（CUSTOM 类型） */
    default void onToolExecute(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {}

    /** AGENT_OUTPUT：AI 输出视图（CUSTOM 类型） */
    default void onAgentOutput(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {}

    /** 其他未识别的自定义事件 */
    default void onCustomEvent(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {}

    // ==================== 状态事件 ====================

    /** STATE_SNAPSHOT：完整状态快照 */
    default void onStateSnapshot(AguiEvent.StateSnapshot event, HeadlessPageWrapper wrapper) {}

    /** STATE_DELTA：增量状态变更 */
    default void onStateDelta(AguiEvent.StateDelta event, HeadlessPageWrapper wrapper) {}

    // ==================== 错误处理 ====================

    /** SSE 流发生错误 */
    default void onError(Throwable error, HeadlessPageWrapper wrapper) {}
}
