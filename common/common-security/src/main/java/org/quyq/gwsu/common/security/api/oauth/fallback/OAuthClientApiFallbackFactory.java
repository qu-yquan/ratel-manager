package org.quyq.gwsu.common.security.api.oauth.fallback;

import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientApi;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.api.fallback.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * OAuth 应用配置 API 降级工厂。
 *
 * @author Quyq
 */
@Component
public class OAuthClientApiFallbackFactory implements FallbackFactory<OAuthClientApi> {

    @Override
    public OAuthClientApi create(Throwable cause) {
        return new OAuthClientApi() {
            @Override
            public R<OAuthClientInfoVO> getById(String id) {
                return R.fail("OAuth应用配置服务不可用: " + cause.getMessage());
            }

            @Override
            public R<OAuthClientInfoVO> getByClientId(String clientId) {
                return R.fail("OAuth应用配置服务不可用: " + cause.getMessage());
            }

            @Override
            public R<Map<String, List<KeyValue<String, String>>>> enumOptions() {
                return R.fail("OAuth应用配置服务不可用: " + cause.getMessage());
            }
        };
    }

}
