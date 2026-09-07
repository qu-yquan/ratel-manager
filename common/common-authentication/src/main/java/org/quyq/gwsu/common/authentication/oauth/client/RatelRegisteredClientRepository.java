package org.quyq.gwsu.common.authentication.oauth.client;

import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * 基于 Ratel 应用管理的 RegisteredClientRepository。
 *
 * @author Quyq
 */
public class RatelRegisteredClientRepository implements RegisteredClientRepository {

    private final OAuthClientInfoProvider clientInfoProvider;

    private final OAuthRegisteredClientConverter converter;

    public RatelRegisteredClientRepository(
            OAuthClientInfoProvider clientInfoProvider,
            OAuthRegisteredClientConverter converter) {
        this.clientInfoProvider = clientInfoProvider;
        this.converter = converter;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("OAuth应用请在应用管理中维护");
    }

    @Override
    public RegisteredClient findById(String id) {
        return convert(clientInfoProvider.getById(id));
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return convert(clientInfoProvider.getByClientId(clientId));
    }

    private RegisteredClient convert(OAuthClientInfoVO vo) {
        return vo == null ? null : converter.convert(vo);
    }

}
