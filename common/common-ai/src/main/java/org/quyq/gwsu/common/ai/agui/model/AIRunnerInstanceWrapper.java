package org.quyq.gwsu.common.ai.agui.model;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.agui.encoder.AguiEventEncoder;
import org.quyq.gwsu.common.ai.agui.event.AguiEvent;
import org.quyq.gwsu.common.ai.agui.push.AguiEventPusher;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

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
        boolean headless
) {

    public static final AguiEventEncoder ENCODER = new AguiEventEncoder();

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

}
