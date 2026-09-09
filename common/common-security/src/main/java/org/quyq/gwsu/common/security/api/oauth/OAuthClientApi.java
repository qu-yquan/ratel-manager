package org.quyq.gwsu.common.security.api.oauth;

import org.quyq.gwsu.common.api.annotation.ApiClient;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.api.oauth.fallback.OAuthClientApiFallbackFactory;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthConsentContextVO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * OAuth 应用配置 API。
 *
 * @author Quyq
 */
@ApiClient(value = CoreConstants.Server.SECURITY_NAME, note = "OAuth应用配置", fallbackFactory = OAuthClientApiFallbackFactory.class)
@HttpExchange("/oauth/client")
public interface OAuthClientApi {

    @GetExchange("/id/{id}")
    R<OAuthClientInfoVO> getById(@PathVariable("id") String id);

    @GetExchange("/clientId/{clientId}")
    R<OAuthClientInfoVO> getByClientId(@PathVariable("clientId") String clientId);

    @GetExchange("/consent-context")
    R<OAuthConsentContextVO> getConsentContext(
            @RequestParam("clientId") String clientId,
            @RequestParam(value = "scope", required = false) String scope);

    @GetExchange("/enums")
    R<Map<String, List<KeyValue<String, String>>>> enumOptions();

}
