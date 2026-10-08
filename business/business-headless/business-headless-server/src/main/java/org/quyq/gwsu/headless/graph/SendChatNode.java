package org.quyq.gwsu.headless.graph;


import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.action.NodeAction;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 普通消息发送节点
 */
@RequiredArgsConstructor
public class SendChatNode implements NodeAction<HeadlessGraphState> {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(HeadlessGraphState state) {

        HeadlessDTO request = state.<HeadlessDTO>value(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST).orElseThrow();
        SubjectInfo userId = state.<SubjectInfo>value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID).orElseThrow();
        String threadId = state.<String>value(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                userId.userId(),
                agentStateStore,
                threadId,
                request.enableOutputPanelScreenshot(),
                request.enableApprovalRecording()
        );

        executorService.submit(() -> {
            try {
                headlessBrowserManager.sendMessage(userId, request, handler);
                handler.complete();
            } catch (Exception e) {
                handler.error(e);
            }
        });


        Flux<AguiEvent> output = handler.asFlux()
                .startWith(HeadlessAguiEvents.status(
                        threadId, "", HeadlessAgentStatus.INITING));
        return Map.of(HeadlessConstants.Headless.GRAPH_PARAM_OUTPUT, output);
    }
}
