package org.quyq.gwsu.headless.graph;

import cn.hutool.core.util.IdUtil;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.AgentStateStore;
import lombok.extern.slf4j.Slf4j;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.tool.AskUserQuestionTool;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.core.HeadlessAgentListener;
import org.quyq.gwsu.headless.core.session.HeadlessPageWrapper;
import org.quyq.gwsu.kit.api.file.vo.KitFileInfoVO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class HeadlessMessageHandler implements HeadlessAgentListener {

    private static final String OUTPUT_PANEL_SELECTOR = "#ai-output-panel";

    private final Sinks.Many<AguiEvent> sink = Sinks.many().unicast().onBackpressureBuffer();

    private final String userId;

    private final AgentStateStore agentStateStore;

    private final boolean outputPanelScreenshotEnabled;

    private final boolean approvalRecordingEnabled;

    private String currentThreadId;

    private String currentRunId = "";

    private HeadlessAgentStatus status = HeadlessAgentStatus.INITING;

    public HeadlessMessageHandler(String userId, AgentStateStore agentStateStore, String threadId,
                                  boolean outputPanelScreenshotEnabled, boolean approvalRecordingEnabled) {
        this.userId = userId;
        this.agentStateStore = agentStateStore;
        this.currentThreadId = threadId;
        this.outputPanelScreenshotEnabled = outputPanelScreenshotEnabled;
        this.approvalRecordingEnabled = approvalRecordingEnabled;
    }

    @Override
    public void onEvent(AguiEvent event, HeadlessPageWrapper wrapper) {
        syncContext(event.getThreadId(), event.getRunId());
        emitStatusIfNeeded(resolveStatus(event));
        emit(event);
    }

    @Override
    public void onRunStarted(AguiEvent.RunStarted event, HeadlessPageWrapper wrapper) {
        if (approvalRecordingEnabled) {
            wrapper.startRecording();
        }
    }

    @Override
    public void onRunFinished(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {
        if (approvalRecordingEnabled && wrapper.isRecording()) {
            wrapper.discardRecording();
        }
    }

    @Override
    public void onHumanApproval(AguiEvent.RunFinished event, HeadlessPageWrapper wrapper) {
        syncContext(event.threadId(), event.runId());
        AguiEvent.RunFinishedInterruptOutcome outcome =
                (AguiEvent.RunFinishedInterruptOutcome) event.outcome();
        String tip = outcome.interrupts().stream()
                .map(AguiEvent.Interrupt::message)
                .filter(Objects::nonNull)
                .filter(message -> !message.isBlank())
                .findFirst()
                .orElse("危险操作需要您审批，请确认是否继续操作。");

        String tipText = "\n\r" + tip;
        Msg tipMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(tipText)
                .build();
        emitTextContent(event.threadId(), event.runId(), tipText);

        StringBuilder approvalText = new StringBuilder("\n\r危险操作需要您审批，请确认是否继续操作。");
        if (approvalRecordingEnabled) {
            File file = wrapper.stopRecording();
            if (file != null) {
                KitFileInfoVO upload = wrapper.upload(file);
                if (upload != null) {
                    approvalText.append("\n\r以下是操作记录：");
                    emitMediaEvent(event.threadId(), event.runId(), "output_video", upload);
                }
            }
        }
        emitTextContent(event.threadId(), event.runId(), approvalText.toString());
        agentStateStore.save(userId, event.threadId(), IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY, List.of(tipMsg));
    }

    @Override
    public void onAskUserQuestion(String threadId, String toolCallId, List<AskUserQuestionTool.QuestionParam> questions,
                                  HeadlessPageWrapper wrapper) {
        syncContext(threadId, currentRunId);
        StringBuilder sb = new StringBuilder("\n\r# 请您回答以下几个问题：\n\r");
        for (int i = 0; i < questions.size(); i++) {
            AskUserQuestionTool.QuestionParam question = questions.get(i);
            sb.append("## ").append(i + 1).append(".").append(question.question())
                    .append("【").append(Boolean.TRUE.equals(question.multiSelect()) ? "多选" : "单选").append("】\n\r");
            for (int j = 0; j < question.options().size(); j++) {
                AskUserQuestionTool.QuestionOption option = question.options().get(j);
                sb.append("* 选项").append(j + 1).append(". ").append(option.label()).append("(")
                        .append(option.description()).append(")\n\r");
            }
            sb.append("* 选项").append(question.options().size() + 1).append(". 其他，描述您的想法\n\n");
        }

        String questionText = sb.toString();
        Msg msg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent(questionText)
                .build();
        emitTextContent(threadId, currentRunId, questionText);
        agentStateStore.save(userId, threadId, IntentRecognitionNode.HEADLESS_RECOGNITION_NODE_KEY, List.of(msg));
    }

    @Override
    public void onAgentOutput(AguiEvent.Custom event, HeadlessPageWrapper wrapper) {
        if (!"AGENT_OUTPUT_END".equals(event.name()) || !outputPanelScreenshotEnabled) {
            return;
        }

        File file = wrapper.screenshot(OUTPUT_PANEL_SELECTOR);
        if (file == null) {
            log.warn("AI输出区截图失败，未获取到截图文件");
            return;
        }
        KitFileInfoVO fileInfo = wrapper.upload(file);
        emitMediaEvent(event.threadId(), event.runId(), "output_image", fileInfo);
    }

    @Override
    public void onError(Throwable error, HeadlessPageWrapper wrapper) {
        emitFailure(error);
    }

    public Flux<AguiEvent> asFlux() {
        return sink.asFlux();
    }

    public void complete() {
        emitStatusIfNeeded(HeadlessAgentStatus.COMPLETE);
        sink.tryEmitComplete();
    }

    public void error(Throwable error) {
        emitFailure(error);
    }

    private void emitFailure(Throwable error) {
        emitStatusIfNeeded(HeadlessAgentStatus.ERROR);
        emit(HeadlessAguiEvents.raw(currentThreadId, currentRunId, error.getMessage()));
        sink.tryEmitComplete();
    }

    private void emitTextContent(String threadId, String runId, String text) {
        emit(new AguiEvent.TextMessageContent(threadId, runId, IdUtil.fastUUID(), text));
    }

    private void emitMediaEvent(String threadId, String runId, String name, KitFileInfoVO fileInfo) {
        if (fileInfo == null) {
            return;
        }
        String url = buildFileUrl(fileInfo);
        emit(new AguiEvent.Custom(threadId, runId, name, Map.of(
                "fileId", fileInfo.getFileId(),
                "url", url,
                "mimeType", fileInfo.getMediaType()
        )));
    }

    private String buildFileUrl(KitFileInfoVO fileInfo) {
        return "/kit/file/stream/%s".formatted(fileInfo.getFileId());
    }

    private void emitStatusIfNeeded(HeadlessAgentStatus newStatus) {
        if (newStatus == null || newStatus == status) {
            return;
        }
        status = newStatus;
        emit(HeadlessAguiEvents.status(currentThreadId, currentRunId, newStatus));
    }

    private HeadlessAgentStatus resolveStatus(AguiEvent event) {
        if (event instanceof AguiEvent.ReasoningStart
                || event instanceof AguiEvent.ReasoningMessageStart
                || event instanceof AguiEvent.ReasoningMessageContent
                || event instanceof AguiEvent.ReasoningMessageChunk
                || event instanceof AguiEvent.ReasoningMessageEnd
                || event instanceof AguiEvent.ReasoningEnd) {
            return HeadlessAgentStatus.THINKING;
        }
        if (event instanceof AguiEvent.TextMessageStart
                || event instanceof AguiEvent.TextMessageContent
                || event instanceof AguiEvent.TextMessageEnd) {
            return HeadlessAgentStatus.OUTPUTTING;
        }
        if (event instanceof AguiEvent.ToolCallStart
                || event instanceof AguiEvent.ToolCallArgs
                || event instanceof AguiEvent.ToolCallEnd
                || event instanceof AguiEvent.ToolCallResult) {
            return HeadlessAgentStatus.CALLING;
        }
        if (event instanceof AguiEvent.Custom custom) {
            if ("AGENT_OUTPUT".equals(custom.name()) || "AGENT_OUTPUT_END".equals(custom.name())) {
                return HeadlessAgentStatus.SHOWING;
            }
        }
        return null;
    }

    private void syncContext(String threadId, String runId) {
        if (Objects.nonNull(threadId) && !threadId.isBlank()) {
            currentThreadId = threadId;
        }
        if (Objects.nonNull(runId) && !runId.isBlank()) {
            currentRunId = runId;
        }
    }

    private void emit(AguiEvent event) {
        sink.tryEmitNext(event);
    }
}
