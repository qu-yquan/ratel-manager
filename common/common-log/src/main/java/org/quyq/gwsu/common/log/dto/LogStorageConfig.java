package org.quyq.gwsu.common.log.dto;

/**
 * 日志存储与生命周期系统配置。
 *
 * @param operationLog 操作日志配置
 * @param tableLog 表操作日志配置
 * @param loginLog 登录日志配置
 */
public record LogStorageConfig(
        LogStorage operationLog,
        LogStorage tableLog,
        LogStorage loginLog
) {

    public static final String CONFIG_KEY = "log_storage_config";
}
