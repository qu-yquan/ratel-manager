package org.quyq.gwsu.kit.job.scheduler.trigger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.springframework.http.MediaType;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Type;
import java.time.Duration;

/**
 * 远程触发策略（分布式模式）
 * <p>
 * 分布式部署时，Admin通过HTTP调用Executor的端点
 * </p>
 */
@Component
@ConditionalOnProperty(name = CoreConstants.Yaml.DEPLOY_SINGLE, havingValue = "false", matchIfMissing = true)
public class RemoteTriggerStrategy implements TriggerStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RemoteTriggerStrategy.class);
    private static final Gson GSON = new Gson();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public RemoteTriggerStrategy() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    @Override
    public R<String> trigger(String address, TriggerRequest triggerRequest) {
        return post(address + "job-executor/trigger", triggerRequest, new TypeToken<R<String>>() {}.getType());
    }

    @Override
    public R<String> beat(String address) {
        return post(address + "job-executor/beat", null, new TypeToken<R<String>>() {}.getType());
    }

    @Override
    public R<String> idleBeat(String address, IdleBeatRequest idleBeatRequest) {
        return post(address + "job-executor/idleBeat", idleBeatRequest, new TypeToken<R<String>>() {}.getType());
    }

    @Override
    public R<String> kill(String address, KillRequest killRequest) {
        return post(address + "job-executor/kill", killRequest, new TypeToken<R<String>>() {}.getType());
    }

    @Override
    public R<LogData> log(String address, LogRequest logRequest) {
        return post(address + "job-executor/log", logRequest, new TypeToken<R<LogData>>() {}.getType());
    }

    private <T> T post(String url, Object body, Type responseType) {
        try {
            String jsonBody = body != null ? GSON.toJson(body) : "";
            String response = webClient.post()
                    .uri(url)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null) {
                logger.warn(">>>>>>>>>>> 远程调用返回空响应，url={}", url);
                return responseType.equals(new TypeToken<R<String>>() {}.getType())
                        ? (T) R.fail("远程调用返回空响应") : null;
            }
            return GSON.fromJson(response, responseType);
        } catch (Exception e) {
            logger.error(">>>>>>>>>>> 远程调用失败，url={}, error={}", url, e.getMessage(), e);
            return responseType.equals(new TypeToken<R<String>>() {}.getType())
                    ? (T) R.fail("远程调用失败：" + e.getMessage()) : null;
        }
    }

}
