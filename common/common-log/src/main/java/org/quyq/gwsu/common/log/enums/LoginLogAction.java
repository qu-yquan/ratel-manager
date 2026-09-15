package org.quyq.gwsu.common.log.enums;

/**
 * 认证日志写入动作，仅用于日志采集端与日志服务之间传递，不持久化。
 */
public enum LoginLogAction {
    LOGIN,
    LOGOUT,
    TOKEN_REFRESH
}
