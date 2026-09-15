package org.quyq.gwsu.common.log.service;

import org.quyq.gwsu.common.log.vo.LogLoginVO;

/**
 * 认证日志提交服务。
 */
public interface LoginLogHandlerService {

    void save(LogLoginVO loginLog);
}
