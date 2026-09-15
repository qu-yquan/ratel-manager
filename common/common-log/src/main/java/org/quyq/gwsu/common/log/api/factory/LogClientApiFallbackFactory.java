package org.quyq.gwsu.common.log.api.factory;


import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.log.api.ILogClientApi;
import org.quyq.gwsu.common.log.vo.LogOperationVO;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.springframework.stereotype.Component;

/**
 * @author Quyq
 * @date 2026/5/21
 * @description
 */
@Component
public class LogClientApiFallbackFactory implements FallbackFactory<ILogClientApi> {
    @Override
    public ILogClientApi create(Throwable cause) {
        return new ILogClientApi() {
            @Override
            public R<Boolean> saveOperLog(LogOperationVO vo) {
                return R.fail(cause.getMessage());
            }

            @Override
            public R<Boolean> saveLoginLog(LogLoginVO vo) {
                return R.fail(cause.getMessage());
            }
        };
    }
}
