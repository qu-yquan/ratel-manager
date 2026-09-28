package @@GROUP_ID@@.config;

import org.jspecify.annotations.Nullable;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 注册单体应用使用的 YAML 资源。
 */
public class YamlRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.resources().registerPattern("database.yaml");
        hints.resources().registerPattern("redis.yaml");
        hints.resources().registerPattern("elasticsearch.yaml");
    }
}
