package org.quyq.gwsu.common.ai.agui.model;


import lombok.extern.slf4j.Slf4j;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import org.quyq.gwsu.common.ai.agui.encoder.AguiProtocolEventEncoder;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Quyq
 * @date 2026/5/6
 * @description
 */
@Slf4j
public record AIRunnerInstanceWrapper(
        RunAgentInput input ,
        SseEmitter emitter ,
        //是否是无头浏览器访问
        boolean headless,
        AtomicReference<AguiRequestProcessor.ProcessResult> processResult
) {

    public AIRunnerInstanceWrapper(RunAgentInput input, SseEmitter emitter, boolean headless) {
        this(input, emitter, headless, new AtomicReference<>());
    }

    public static final AguiProtocolEventEncoder ENCODER = new AguiProtocolEventEncoder();

    /**
     * 发送事件
     * @param event
     */
    public  void sendEvent( AguiEvent event) {
        try {
            String jsonData = ENCODER.encodeToJson(event);
            emitter.send(SseEmitter.event().data(jsonData, MediaType.APPLICATION_JSON));

        } catch (IOException e) {
            log.debug("Failed to send SSE event: {}", e.getMessage());
        }
    }

    public void bindProcessResult(AguiRequestProcessor.ProcessResult result) {
        processResult.set(result);
    }

    public void interrupt() {
        AguiRequestProcessor.ProcessResult result = processResult.get();
        if (result != null) {
            result.interrupt(input.getThreadId());
        }
    }

}
