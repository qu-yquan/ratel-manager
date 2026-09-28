package org.quyq.gwsu.common.log.constants;

import org.quyq.gwsu.common.core.constants.CoreConstants;

/**
 * @author Quyq
 * @date 2026/5/21
 * @description 日志相关常量
 */
public interface LogInfoConstants {



    String TRACE_ID = "traceId";

    String SPAN_ID = "spanId";

    /**
     * 上一服务的操作日志标识，用于构建跨服务操作日志父子链路。
     */
    String HEADER_PARENT_LOG_ID = CoreConstants.Headers.PARENT_LOG_ID;
}
