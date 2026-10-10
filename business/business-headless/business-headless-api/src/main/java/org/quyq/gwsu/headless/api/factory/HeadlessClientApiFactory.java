package org.quyq.gwsu.headless.api.factory;


import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.NewChatDTO;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@Slf4j
public class HeadlessClientApiFactory implements FallbackFactory<HeadlessClientApi> {
    @Override
    public HeadlessClientApi create(Throwable cause) {
        log.error("", cause);
        return new HeadlessClientApi() {
            @Override
            public Flux<AguiEvent> stream(String userId , String sign, HeadlessDTO form) {
                return Flux.error(cause);
            }

            @Override
            public R<Void> newThreadId(NewChatDTO form) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
