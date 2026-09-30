package org.quyq.gwsu.kit.job.scheduler.trigger;

import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.job.openapi.executor.ExecutorBiz;
import org.quyq.gwsu.common.job.openapi.executor.dto.IdleBeatRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.KillRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogData;
import org.quyq.gwsu.common.job.openapi.executor.dto.LogRequest;
import org.quyq.gwsu.common.job.openapi.executor.dto.TriggerRequest;
import org.quyq.gwsu.common.job.openapi.executor.impl.ExecutorBizImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 本地触发策略（单体模式）
 * <p>
 * 单体部署时，Admin和Executor在同一个JVM，直接本地调用
 * </p>
 */
@Component
@ConditionalOnProperty(name = CoreConstants.Yaml.DEPLOY_SINGLE, havingValue = "true")
public class LocalTriggerStrategy implements TriggerStrategy {

    private final ExecutorBiz executorBiz = new ExecutorBizImpl();

    @Override
    public R<String> trigger(String address, TriggerRequest triggerRequest) {
        return executorBiz.trigger(triggerRequest);
    }

    @Override
    public R<String> beat(String address) {
        return executorBiz.beat();
    }

    @Override
    public R<String> idleBeat(String address, IdleBeatRequest idleBeatRequest) {
        return executorBiz.idleBeat(idleBeatRequest);
    }

    @Override
    public R<String> kill(String address, KillRequest killRequest) {
        return executorBiz.kill(killRequest);
    }

    @Override
    public R<LogData> log(String address, LogRequest logRequest) {
        return executorBiz.log(logRequest);
    }

}
