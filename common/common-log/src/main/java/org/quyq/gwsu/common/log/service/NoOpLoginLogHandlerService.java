package org.quyq.gwsu.common.log.service;

import org.quyq.gwsu.common.log.vo.LogLoginVO;

/**
 * 认证日志关闭时的空实现。
 */
public class NoOpLoginLogHandlerService implements LoginLogHandlerService {

    @Override
    public void save(LogLoginVO loginLog) {
        // 日志功能已关闭
    }
}
