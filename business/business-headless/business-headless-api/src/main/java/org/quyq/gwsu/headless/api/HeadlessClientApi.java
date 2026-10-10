package org.quyq.gwsu.headless.api;


import io.swagger.v3.oas.annotations.Operation;
import org.quyq.gwsu.common.api.annotation.ApiClient;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.NewChatDTO;
import org.quyq.gwsu.headless.api.factory.HeadlessClientApiFactory;
import org.quyq.gwsu.headless.api.loadbalancer.UserIdStickyLoadBalancer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@ApiClient(value = CoreConstants.Server.HEADLESS_NAME, note = "无头智能体API",
        fallbackFactory = HeadlessClientApiFactory.class, loadBalancer = UserIdStickyLoadBalancer.class)
@HttpExchange("headless")
public interface HeadlessClientApi {

    /**
     * 无头智能体调用（SSE 流式响应）
     *
     * @param form
     * @return
     */
    @PostExchange(value = "stream", accept = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<AguiEvent> stream(@RequestParam("userId") String userId,
                           @RequestParam(required = false , value = "sign") String sign, @RequestBody HeadlessDTO form);

    /**
     * 主动建立新会话
     *
     * @param form
     * @return
     */
    @PostExchange(value = "newChat")
    @Operation(description = "")
    R<Void> newThreadId(@RequestBody NewChatDTO form);

}
