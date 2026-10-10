package org.quyq.gwsu.headless.controller;


import cn.hutool.core.util.IdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import io.agentscope.core.agui.event.AguiEvent;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.headless.api.HeadlessClientApi;
import org.quyq.gwsu.headless.api.dto.HeadlessDTO;
import org.quyq.gwsu.headless.api.dto.NewChatDTO;
import org.quyq.gwsu.headless.core.HeadlessBrowserManager;
import org.quyq.gwsu.headless.domain.HeadlessCallConfig;
import org.quyq.gwsu.headless.domain.SubjectInfo;
import org.quyq.gwsu.headless.errcode.HeadlessErrorCode;
import org.quyq.gwsu.headless.service.IHeadlessService;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * @author Quyq
 * @date 2026/6/25
 * @description
 */
@RestController
@RequestMapping("headless")
@Tag(name = "无头智能体模块")
@RequiredArgsConstructor
public class HeadlessController implements HeadlessClientApi {

    private final IHeadlessService headlessService;

    private final HeadlessBrowserManager headlessBrowserManager;

    @Override
    @PostMapping(value = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(description = "无头智能体调用")
    public Flux<AguiEvent> stream(@RequestParam String userId ,
                                  @RequestParam(required = false) String sign, @RequestBody HeadlessDTO form) {

        return headlessService.stream(form, HeadlessCallConfig.builder()
                 .sign(sign)
                .userId(userId)
                .threadId(form.threadId())
                .build());

    }


    @Override
    @PostMapping(value = "newChat")
    @Operation(description = "主动建立新会话")
    public R<Void> newThreadId(@RequestBody NewChatDTO form){
        AssertUtils.hasText(form.getUserId() , HeadlessErrorCode.E01005);
        if(!StringUtils.hasText(form.getThreadId())){
            form.setThreadId(IdUtil.fastUUID());
        }

        headlessBrowserManager.newSession(new SubjectInfo(form.getSign(),form.getUserId()), form.getThreadId());
        return R.ok();

    }


}
