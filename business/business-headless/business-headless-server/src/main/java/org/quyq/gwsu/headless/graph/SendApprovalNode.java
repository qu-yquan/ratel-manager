package org.quyq.gwsu.headless.graph;


import io.agentscope.core.state.AgentStateStore;
import lombok.RequiredArgsConstructor;
import org.bsc.langgraph4j.action.NodeAction;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.core.utils.ThreadPoolUtil;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.enums.HeadlessAgentStatus;
import org.quyq.gwsu.headless.constants.HeadlessConstants;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.RouterInfo;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author Quyq
 * @date 2026/6/17
 * @description 审批消息发送节点
 */
@RequiredArgsConstructor
public class SendApprovalNode implements NodeAction<HeadlessGraphState> {

    private final AgentStateStore agentStateStore;

    private final HeadlessBrowserManager headlessBrowserManager;

    private final ExecutorService executorService = ThreadPoolUtil.newVirtualThreadPerTaskExecutor();

    @Override
    public Map<String, Object> apply(HeadlessGraphState state) {

        RouterInfo routerInfo = state.<RouterInfo>value(HeadlessConstants.Headless.GRAPH_PARAM_ROUTE_INFO).orElse(null);
        AssertUtils.notNull(routerInfo, HeadlessErrorCode.E01001);

        RouterInfo.ApprovalInfo approvalInfo = routerInfo.getApprovalInfo();
        AssertUtils.notNull(approvalInfo, HeadlessErrorCode.E01002);

        SubjectInfo userId = state.<SubjectInfo>value(HeadlessConstants.Headless.GRAPH_PARAM_USER_ID).orElseThrow();
        HeadlessDTO request = state.<HeadlessDTO>value(HeadlessConstants.Headless.GRAPH_PARAM_REQUEST).orElseThrow();
        String threadId = state.<String>value(HeadlessConstants.Headless.GRAPH_PARAM_THREAD_ID).orElse("");

        HeadlessMessageHandler handler = new HeadlessMessageHandler(
                userId.userId(),
                agentStateStore,
                threadId,
                request.enableOutputPanelScreenshot(),
                request.enableApprovalRecording()
        );

        //发送审批消息
        executorService.submit(() -> {
            try {
                headlessBrowserManager.approval(
                        userId,
                        approvalInfo.agree(), approvalInfo.refuseReason(),
                        handler
                );
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
