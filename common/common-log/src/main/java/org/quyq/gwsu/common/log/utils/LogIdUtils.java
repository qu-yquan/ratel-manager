package org.quyq.gwsu.common.log.utils;

import cn.hutool.core.util.IdUtil;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * 操作日志标识上下文工具。
 *
 * <p>同一执行上下文内首次获取时生成雪花 ID，后续调用复用该 ID；新建子线程会继承
 * 父线程当前的日志标识。线程池场景由 LogIdContextThreadLocalAccessor 配合
 * Micrometer Context Propagation 传递。操作日志切面负责初始化和清理该上下文。</p>
 */
public final class LogIdUtils {

    private static final InheritableThreadLocal<String> LOG_ID_HOLDER = new InheritableThreadLocal<>();

    private LogIdUtils() {
    }

    /**
     * 获取当前日志标识，不存在时生成一个新的雪花 ID。
     */
    public static String getLogId() {
        String logId = LOG_ID_HOLDER.get();
        if (StringUtils.hasText(logId)) {
            return logId;
        }
        return renewLogId();
    }

    /**
     * 获取当前日志标识，不自动生成。
     */
    public static @Nullable String getCurrentLogId() {
        return LOG_ID_HOLDER.get();
    }

    /**
     * 生成并设置新的日志标识。
     */
    public static String renewLogId() {
        String logId = IdUtil.getSnowflakeNextIdStr();
        LOG_ID_HOLDER.set(logId);
        return logId;
    }

    /**
     * 设置当前日志标识，供上下文恢复使用。
     */
    public static void setLogId(String logId) {
        if (!StringUtils.hasText(logId)) {
            throw new IllegalArgumentException("日志标识不能为空");
        }
        LOG_ID_HOLDER.set(logId);
    }

    /**
     * 清理当前线程的日志标识。
     */
    public static void clear() {
        LOG_ID_HOLDER.remove();
    }
}
