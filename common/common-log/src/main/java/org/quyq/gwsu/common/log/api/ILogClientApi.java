package org.quyq.gwsu.common.log.api;


import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.factory.LogClientApiFallbackFactory;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * @author Quyq
 * @date 2026/5/21
 * @description
 */
@ApiClient(value = CoreConstants.Server.LOG_NAME, note = "日志信息保存", fallbackFactory = LogClientApiFallbackFactory.class)
@HttpExchange("/saveLog")
public interface ILogClientApi {

    /**
     * 保存操作日志
     *
     * @param vo
     * @return
     */
    @PostExchange("/operation")
    R<Boolean> saveOperLog(@RequestBody LogOperationVO vo);

    /**
     * 保存认证日志。
     */
    @PostExchange("/login")
    R<Boolean> saveLoginLog(@RequestBody LogLoginVO vo);

}
