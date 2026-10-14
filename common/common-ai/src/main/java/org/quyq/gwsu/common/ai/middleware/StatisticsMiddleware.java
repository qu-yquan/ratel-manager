package org.quyq.gwsu.common.ai.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.ai.model.ReloadableModel;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Quyq
 * @date 2026/7/10
 * @description 统计当前智能体的模型调用与工具调用
 */
@Slf4j
public class StatisticsMiddleware implements MiddlewareBase {

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext runtimeContext,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> chain) {
        // 统计与实际请求共用本次解析的真实模型，避免配置切换导致名称与请求不一致。
        if (input.model() instanceof ReloadableModel model) {
            input = new ModelCallInput(input.messages(), input.tools(), input.options(), model.resolve());
        }
        StatisticsState state = getOrCreateState(agent, runtimeContext);
        InvocationRecord record = state.start(InvocationType.MODEL, resolveModelName(input));
        return chain.apply(input)
                .doFinally(signalType -> state.finish(record, LocalDateTime.now()));
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext runtimeContext,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> chain) {
        StatisticsState state = getOrCreateState(agent, runtimeContext);
        InvocationRecord record = state.start(InvocationType.TOOL, resolveToolNames(input));
        return chain.apply(input)
                .doFinally(signalType -> state.finish(record, LocalDateTime.now()));
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext runtimeContext,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> chain) {
        StatisticsState state = getOrCreateState(agent, runtimeContext);
        return chain.apply(input)
                .doFinally(signalType -> {
                    log.debug(buildCountLog(state));
                    runtimeContext.put(StatisticsState.class, null);
                });
    }

    private StatisticsState getOrCreateState(Agent agent, RuntimeContext runtimeContext) {
        StatisticsState state = runtimeContext.get(StatisticsState.class);
        if (state != null) {
            return state;
        }
        StatisticsState newState = new StatisticsState(agent.getName());
        runtimeContext.put(StatisticsState.class, newState);
        return newState;
    }

    private String resolveModelName(ModelCallInput input) {
        if (input == null || input.model() == null || !StringUtils.hasText(input.model().getModelName())) {
            return "未知模型";
        }
        return input.model().getModelName();
    }

    private String resolveToolNames(ActingInput input) {
        if (input == null || input.toolCalls() == null || input.toolCalls().isEmpty()) {
            return "未知工具";
        }
        String toolNames = input.toolCalls().stream()
                .map(ToolUseBlock::getName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(","));
        return StringUtils.hasText(toolNames) ? toolNames : "未知工具";
    }

    static String buildCountLog(StatisticsState state) {
        if (state == null || state.records().isEmpty()) {
            return "\n智能体运行完成，没有模型或工具调用记录。";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        List<InvocationRecord> records = state.records().stream()
                .sorted(Comparator.comparing(InvocationRecord::start).thenComparingInt(InvocationRecord::seq))
                .toList();

        long modelCount = records.stream().filter(record -> record.type() == InvocationType.MODEL).count();
        long toolCount = records.stream().filter(record -> record.type() == InvocationType.TOOL).count();

        StringBuilder sb = new StringBuilder();
        sb.append("\n智能体运行完成，智能体 [")
                .append(state.agentName())
                .append("] 共记录 ")
                .append(records.size())
                .append(" 次调用（模型 ")
                .append(modelCount)
                .append(" 次，工具 ")
                .append(toolCount)
                .append(" 次），按调用时间排序如下：\n");
        sb.append("| 序号 | 智能体 | 类型 | 名称 | 开始时间 | 耗时 |\n");
        sb.append("|------|--------|------|------|--------------------------|-----------|\n");
        for (InvocationRecord record : records) {
            sb.append("| ")
                    .append(record.seq())
                    .append(" | ")
                    .append(record.agentName())
                    .append(" | ")
                    .append(record.type().displayName())
                    .append(" | ")
                    .append(record.name())
                    .append(" | ")
                    .append(record.start().format(formatter))
                    .append(" | ")
                    .append(formatSmartPerfect(record.durationMs()))
                    .append(" |\n");
        }
        return sb.toString();
    }

    static String formatSmartPerfect(long timeConsuming) {
        long millis = Math.abs(timeConsuming);

        if (millis == 0) {
            return "0秒";
        }
        if (millis < 1000) {
            return millis + "毫秒";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        long remainSeconds = seconds % 60;
        long remainMinutes = minutes % 60;
        long remainHours = hours % 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("天");
        }
        if (remainHours > 0) {
            sb.append(remainHours).append("小时");
        }
        if (remainMinutes > 0) {
            sb.append(remainMinutes).append("分");
        }
        if (remainSeconds > 0) {
            sb.append(remainSeconds).append("秒");
        }
        if (sb.isEmpty()) {
            sb.append(remainSeconds).append("秒");
        }
        return sb.toString();
    }

    enum InvocationType {
        MODEL("模型"),
        TOOL("工具");

        private final String displayName;

        InvocationType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    static final class StatisticsState {
        private final String agentName;
        private final AtomicInteger sequence = new AtomicInteger(0);
        private final List<InvocationRecord> records = new CopyOnWriteArrayList<>();

        StatisticsState(String agentName) {
            this.agentName = StringUtils.hasText(agentName) ? agentName : "未知智能体";
        }

        InvocationRecord start(InvocationType type, String name) {
            InvocationRecord record = new InvocationRecord(
                    sequence.incrementAndGet(),
                    agentName,
                    type,
                    StringUtils.hasText(name) ? name : "未知",
                    LocalDateTime.now(),
                    0L);
            records.add(record);
            return record;
        }

        void finish(InvocationRecord record, LocalDateTime endTime) {
            record.setDurationMs(Duration.between(record.start(), endTime).toMillis());
        }

        void addRecord(InvocationRecord record) {
            records.add(record);
        }

        String agentName() {
            return agentName;
        }

        List<InvocationRecord> records() {
            return records;
        }
    }

    static final class InvocationRecord {
        private final int seq;
        private final String agentName;
        private final InvocationType type;
        private final String name;
        private final LocalDateTime start;
        private volatile long durationMs;

        InvocationRecord(int seq,
                         String agentName,
                         InvocationType type,
                         String name,
                         LocalDateTime start,
                         long durationMs) {
            this.seq = seq;
            this.agentName = agentName;
            this.type = type;
            this.name = name;
            this.start = start;
            this.durationMs = durationMs;
        }

        int seq() {
            return seq;
        }

        String agentName() {
            return agentName;
        }

        InvocationType type() {
            return type;
        }

        String name() {
            return name;
        }

        LocalDateTime start() {
            return start;
        }

        long durationMs() {
            return durationMs;
        }

        void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
    }
}
