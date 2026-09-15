package org.quyq.gwsu.common.log.security;

/**
 * Token 安全指纹。
 */
public record TokenFingerprint(String value, String keyVersion) {
}
