package org.quyq.gwsu.common.ai.model;

import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.quyq.gwsu.common.ai.config.properties.ModelLlmConfigDTO;
import reactor.core.publisher.Flux;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 稳定的模型委托对象，配置变化时原子替换真实模型。
 * <p>一次 stream 方法调用固定一个真实模型；订阅、重试和流输出期间不会切换模型。
 * 独立的能力查询反映查询时的配置；需要多个操作保持一致时先调用 {@link #resolve()}。
 */
public final class ReloadableModel implements Model {

    private static final JsonMapper CONFIG_MAPPER = JsonMapper.builder().build();

    private final Supplier<ModelLlmConfigDTO> configSupplier;
    private final Function<ModelLlmConfigDTO, Model> modelFactory;
    private volatile Snapshot snapshot;

    ReloadableModel(Supplier<ModelLlmConfigDTO> configSupplier,
                    Function<ModelLlmConfigDTO, Model> modelFactory) {
        this.configSupplier = Objects.requireNonNull(configSupplier);
        this.modelFactory = Objects.requireNonNull(modelFactory);
    }

    /**
     * 获取本次操作使用的真实模型。读取或构建失败直接上抛，不静默回退到旧配置。
     */
    public Model resolve() {
        JsonNode configKey = configKey(configSupplier.get());
        Snapshot current = snapshot;
        if (current != null && current.configKey().equals(configKey)) {
            return current.model();
        }
        synchronized (this) {
            // 等待锁期间配置可能已经更新，必须重新读取，避免旧配置覆盖新快照。
            ModelLlmConfigDTO config = Objects.requireNonNull(configSupplier.get(), "LLM config is required");
            configKey = configKey(config);
            current = snapshot;
            if (current != null && current.configKey().equals(configKey)) {
                return current.model();
            }
            Model model = Objects.requireNonNull(modelFactory.apply(config), "LLM model is required");
            // configKey 是独立的树快照，不持有外部可变 DTO；构建成功才发布。
            snapshot = new Snapshot(configKey, model);
            return model;
        }
    }

    private JsonNode configKey(ModelLlmConfigDTO config) {
        return CONFIG_MAPPER.valueToTree(Objects.requireNonNull(config, "LLM config is required"));
    }

    @Override
    public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
        // 固定在方法调用边界。重订阅同一个 Flux 不会偷偷切换提供商。
        Model model = resolve();
        return Flux.defer(() -> model.stream(messages, tools, options));
    }

    @Override
    public String getModelName() {
        return resolve().getModelName();
    }

    @Override
    public boolean supportsNativeStructuredOutput() {
        return resolve().supportsNativeStructuredOutput();
    }

    @Override
    public boolean supportsNativeStructuredOutputWithTools() {
        return resolve().supportsNativeStructuredOutputWithTools();
    }

    @Override
    public int getContextWindowSize() {
        return resolve().getContextWindowSize();
    }

    private record Snapshot(JsonNode configKey, Model model) {
        @Override
        public String toString() {
            return "ModelSnapshot"; // 配置包含密钥，禁止通过快照字符串泄露。
        }
    }
}
