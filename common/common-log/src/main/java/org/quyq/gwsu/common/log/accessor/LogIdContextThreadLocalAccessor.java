package org.quyq.gwsu.common.log.accessor;

import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.log.utils.LogIdUtils;

/**
 * 操作日志标识的 Micrometer 上下文传播适配器。
 */
public class LogIdContextThreadLocalAccessor implements ThreadLocalAccessor<String> {

    public static final String KEY = "gwsu-log-id";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public @Nullable String getValue() {
        return LogIdUtils.getCurrentLogId();
    }

    @Override
    public void setValue(String value) {
        LogIdUtils.setLogId(value);
    }

    @Override
    public void setValue() {
        LogIdUtils.clear();
    }

    @Override
    public void restore(@Nullable String previousValue) {
        if (previousValue != null) {
            LogIdUtils.setLogId(previousValue);
        } else {
            LogIdUtils.clear();
        }
    }

    @Override
    public void restore() {
        LogIdUtils.clear();
    }
}
