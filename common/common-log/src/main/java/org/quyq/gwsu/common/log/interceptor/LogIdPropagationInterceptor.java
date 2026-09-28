package org.quyq.gwsu.common.log.interceptor;

import org.quyq.gwsu.common.api.interceptor.ApiClientWebClientFilter;
import org.quyq.gwsu.common.core.interceptor.ApiClientInterceptor;
import org.quyq.gwsu.common.log.constants.LogInfoConstants;
import org.quyq.gwsu.common.log.utils.LogIdUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientRequest;

/**
 * 将当前服务的操作日志标识传递给下游服务。
 */
public class LogIdPropagationInterceptor
        implements ApiClientInterceptor, ApiClientWebClientFilter {

    @Override
    public void intercept(HttpHeaders headers) {
        setParentLogId(headers);
    }

    @Override
    public void filter(ClientRequest.Builder requestBuilder) {
        requestBuilder.headers(this::setParentLogId);
    }

    private void setParentLogId(HttpHeaders headers) {
        headers.remove(LogInfoConstants.HEADER_PARENT_LOG_ID);
        String logId = LogIdUtils.getCurrentLogId();
        if (StringUtils.hasText(logId)) {
            headers.set(LogInfoConstants.HEADER_PARENT_LOG_ID, logId);
        }
    }
}
