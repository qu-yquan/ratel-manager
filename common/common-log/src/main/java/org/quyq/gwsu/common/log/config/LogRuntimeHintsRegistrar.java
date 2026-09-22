package org.quyq.gwsu.common.log.config;

import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.log.dto.LogLifeCycle;
import org.quyq.gwsu.common.log.dto.LogStorage;
import org.quyq.gwsu.common.log.dto.LogStorageConfig;
import org.quyq.gwsu.common.log.enums.SaveMedium;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 日志配置 JSON 反序列化运行时提示。
 */
public class LogRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        hints.reflection().registerType(LogStorageConfig.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(LogStorage.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(LogLifeCycle.class,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS);
        hints.reflection().registerType(SaveMedium.class,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.ACCESS_DECLARED_FIELDS);
    }
}
