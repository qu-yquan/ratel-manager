package org.quyq.gwsu.common.core.domain;

/**
 * 分布式服务注册信息。
 *
 * @param applicationName 注册中心服务名
 * @param note 服务中文说明
 */
public record DistributedServerInfo(String applicationName, String note) {
}
