package org.quyq.gwsu.security.oauth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientCacheKeys;
import org.quyq.gwsu.common.security.api.oauth.OAuthScopeConstants;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientSecretVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthConsentContextVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthScopeVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthClient;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthClientMapper;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthClientService;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthScopeService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * OAuth 应用配置服务实现。
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
public class SecurityOAuthClientServiceImpl extends ServiceImpl<SecurityOAuthClientMapper, SecurityOAuthClient>
        implements ISecurityOAuthClientService {

    private static final String BCRYPT_PASSWORD_PREFIX = "{bcrypt}";
    private static final long DEFAULT_ACCESS_TOKEN_TTL = 7200L;
    private static final long DEFAULT_REFRESH_TOKEN_TTL = 2592000L;
    private static final long DEFAULT_AUTHORIZATION_CODE_TTL = 300L;
    private static final long DEFAULT_DEVICE_CODE_TTL = 300L;

    private final ISecurityOAuthScopeService scopeService;
    private final CacheUtils cacheUtils;

    @Override
    public OAuthClientInfoVO getInfoById(String id) {
        SecurityOAuthClient client = getById(id);
        return client == null ? null : client.toVo();
    }

    @Override
    public OAuthClientInfoVO getByClientId(String clientId) {
        SecurityOAuthClient client = getOne(new LambdaQueryWrapper<SecurityOAuthClient>()
                .eq(SecurityOAuthClient::getClientId, clientId)
                .eq(SecurityOAuthClient::getDeleted, false));
        return client == null ? null : client.toVo();
    }

    @Override
    public OAuthConsentContextVO getConsentContext(String clientId, String requestedScope) {
        SecurityOAuthClient client = getOne(new LambdaQueryWrapper<SecurityOAuthClient>()
                .eq(SecurityOAuthClient::getClientId, clientId)
                .eq(SecurityOAuthClient::getStatus, OAuthClientStatus.ENABLED)
                .eq(SecurityOAuthClient::getDeleted, false));
        if (client == null) {
            throw new BusinessException(SecurityErrorCode.E08007);
        }

        List<String> configuredCodes = client.toVo().getScopes();
        List<OAuthScopeVO> configuredScopes = scopeService.listByCodes(configuredCodes).stream()
                .filter(scope -> scope.getStatus() == OAuthClientStatus.ENABLED)
                .toList();
        Map<String, OAuthScopeVO> scopeByCode = configuredScopes.stream()
                .collect(Collectors.toMap(OAuthScopeVO::getScopeCode, Function.identity()));
        boolean hasRequestedScope = StringUtils.hasText(requestedScope);
        List<String> effectiveCodes = hasRequestedScope
                ? List.of(requestedScope.trim().split("\\s+"))
                : configuredScopes.stream().map(OAuthScopeVO::getScopeCode).toList();
        AssertUtils.isTrue(!hasRequestedScope || scopeByCode.keySet().containsAll(effectiveCodes),
                SecurityErrorCode.E08108);

        List<OAuthScopeVO> effectiveScopes = effectiveCodes.stream()
                .distinct()
                .map(scopeByCode::get)
                .toList();
        OAuthConsentContextVO context = new OAuthConsentContextVO();
        context.setClientId(client.getClientId());
        context.setClientName(client.getClientName());
        context.setScopes(effectiveScopes);
        context.setCanAuthorize(!effectiveScopes.isEmpty());
        return context;
    }

    @Override
    public IPage<OAuthClientInfoVO> pageByCondition(OAuthClientQueryDTO query) {
        query = query == null ? new OAuthClientQueryDTO() : query;
        LambdaQueryWrapper<SecurityOAuthClient> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(SecurityOAuthClient::getModifyTime);
        Page<SecurityOAuthClient> page = new Page<>(query.getPageNum(), query.getPageSize());
        return page(page, wrapper).convert(SecurityOAuthClient::toVo);
    }

    @Override
    public OAuthClientSecretVO saveOrUpdateClient(OAuthClientSaveDTO dto) {
        normalize(dto);
        validate(dto);

        SecurityOAuthClient entity = SecurityOAuthClient.toDo(dto);
        String plainSecret = null;
        if (!StringUtils.hasText(entity.getId())) {
            usePrimaryKeyAsClientId(entity);
            if (OAuthClientType.CONFIDENTIAL == entity.getClientType()) {
                plainSecret = "cs_" + IdUtil.fastSimpleUUID();
                entity.setClientSecret(encodeClientSecret(plainSecret));
            }
        } else {
            SecurityOAuthClient existing = super.getById(entity.getId());
            if (existing == null) {
                throw new BusinessException(SecurityErrorCode.E08007);
            }
            entity.setClientId(existing.getClientId());
            entity.setClientSecret(existing.getClientSecret());
        }

        saveOrUpdate(entity);
        evict(entity);
        return buildSecretVo(entity, plainSecret);
    }

    @Override
    public OAuthClientSecretVO resetSecret(String id) {
        SecurityOAuthClient client = super.getById(id);
        if (client == null || Boolean.TRUE.equals(client.getDeleted())) {
            throw new BusinessException(SecurityErrorCode.E08007);
        }
        AssertUtils.isTrue(client.isConfidential(), SecurityErrorCode.E08008);

        String plainSecret = "cs_" + IdUtil.fastSimpleUUID();
        client.setClientSecret(encodeClientSecret(plainSecret));
        updateById(client);
        evict(client);
        return buildSecretVo(client, plainSecret);
    }

    @Override
    public Boolean removeClients(List<String> ids) {
        List<SecurityOAuthClient> clients = listByIds(ids);
        boolean removed = removeByIds(ids);
        clients.forEach(this::evict);
        return removed;
    }

    private String encodeClientSecret(String plainSecret) {
        return BCRYPT_PASSWORD_PREFIX + BCrypt.hashpw(plainSecret, BCrypt.gensalt());
    }

    private void normalize(OAuthClientSaveDTO dto) {
        if (dto.getAccountType() == null) {
            dto.setAccountType(AccountType.MANAGER);
        }
        if (dto.getStatus() == null) {
            dto.setStatus(OAuthClientStatus.ENABLED);
        }
        if (dto.getRequireProofKey() == null) {
            dto.setRequireProofKey(true);
        }
        if (dto.getRequireAuthorizationConsent() == null) {
            dto.setRequireAuthorizationConsent(true);
        }
        if (dto.getAccessTokenTtlSeconds() == null || dto.getAccessTokenTtlSeconds() <= 0) {
            dto.setAccessTokenTtlSeconds(DEFAULT_ACCESS_TOKEN_TTL);
        }
        if (dto.getRefreshTokenTtlSeconds() == null || dto.getRefreshTokenTtlSeconds() <= 0) {
            dto.setRefreshTokenTtlSeconds(DEFAULT_REFRESH_TOKEN_TTL);
        }
        if (dto.getAuthorizationCodeTtlSeconds() == null || dto.getAuthorizationCodeTtlSeconds() <= 0) {
            dto.setAuthorizationCodeTtlSeconds(DEFAULT_AUTHORIZATION_CODE_TTL);
        }
        if (dto.getDeviceCodeTtlSeconds() == null || dto.getDeviceCodeTtlSeconds() <= 0) {
            dto.setDeviceCodeTtlSeconds(DEFAULT_DEVICE_CODE_TTL);
        }
        if (dto.getReuseRefreshTokens() == null) {
            dto.setReuseRefreshTokens(false);
        }
        if (dto.getScopes() == null) {
            dto.setScopes(List.of());
        } else {
            dto.setScopes(new LinkedHashSet<>(dto.getScopes()).stream().toList());
        }
        boolean hasUserAuthorizationGrant = dto.getAuthorizationGrantTypes() != null
                && (dto.getAuthorizationGrantTypes().contains(OAuthGrantType.AUTHORIZATION_CODE)
                || dto.getAuthorizationGrantTypes().contains(OAuthGrantType.DEVICE_CODE));
        if (hasUserAuthorizationGrant && dto.getScopes().contains(OAuthScopeConstants.INHERIT_USER_PERMISSIONS)) {
            dto.setRequireAuthorizationConsent(true);
        }
        if (OAuthClientType.CONFIDENTIAL != dto.getClientType()) {
            dto.setRequireProofKey(true);
            dto.setClientAuthenticationMethods(List.of(OAuthClientAuthenticationMethod.NONE));
        }
    }

    private void validate(OAuthClientSaveDTO dto) {
        AssertUtils.hasText(dto.getClientName(), SecurityErrorCode.E08001);
        AssertUtils.notNull(dto.getClientType(), SecurityErrorCode.E08002);
        AssertUtils.notEmpty(dto.getAuthorizationGrantTypes(), SecurityErrorCode.E08003);
        AssertUtils.notEmpty(dto.getClientAuthenticationMethods(), SecurityErrorCode.E08009);
        validateScopes(dto.getAccountType(), dto.getScopes());

        boolean confidential = OAuthClientType.CONFIDENTIAL == dto.getClientType();
        boolean hasClientCredentials = dto.getAuthorizationGrantTypes().contains(OAuthGrantType.CLIENT_CREDENTIALS);
        boolean hasAuthorizationCode = dto.getAuthorizationGrantTypes().contains(OAuthGrantType.AUTHORIZATION_CODE);

        AssertUtils.isTrue(!hasClientCredentials || confidential, SecurityErrorCode.E08005);
        AssertUtils.isTrue(!hasAuthorizationCode || !CollectionUtils.isEmpty(dto.getRedirectUris()), SecurityErrorCode.E08006);
        AssertUtils.isTrue(confidential || Boolean.TRUE.equals(dto.getRequireProofKey()), SecurityErrorCode.E08010);

        if (confidential) {
            boolean secretMethod = dto.getClientAuthenticationMethods().stream()
                    .anyMatch(method -> method == OAuthClientAuthenticationMethod.CLIENT_SECRET_BASIC
                            || method == OAuthClientAuthenticationMethod.CLIENT_SECRET_POST);
            AssertUtils.isTrue(secretMethod, SecurityErrorCode.E08011);
        }
    }

    private void validateScopes(AccountType accountType, List<String> scopes) {
        if (scopes.isEmpty()) {
            return;
        }
        var availableCodes = scopeService.listOptions(accountType, OAuthClientStatus.ENABLED).stream()
                .map(scope -> scope.getScopeCode())
                .collect(java.util.stream.Collectors.toSet());
        AssertUtils.isTrue(availableCodes.containsAll(scopes), SecurityErrorCode.E08108);
    }

    static void usePrimaryKeyAsClientId(SecurityOAuthClient entity) {
        if (!StringUtils.hasText(entity.getId())) {
            entity.setId(IdWorker.getIdStr());
        }
        entity.setClientId(entity.getId());
    }

    private LambdaQueryWrapper<SecurityOAuthClient> buildQueryWrapper(OAuthClientQueryDTO query) {
        LambdaQueryWrapper<SecurityOAuthClient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityOAuthClient::getDeleted, false);
        if (query == null) {
            return wrapper;
        }
        if (StringUtils.hasText(query.getClientId())) {
            wrapper.like(SecurityOAuthClient::getClientId, query.getClientId());
        }
        if (StringUtils.hasText(query.getClientName())) {
            wrapper.like(SecurityOAuthClient::getClientName, query.getClientName());
        }
        if (query.getClientType() != null) {
            wrapper.eq(SecurityOAuthClient::getClientType, query.getClientType());
        }
        if (query.getAccountType() != null) {
            wrapper.eq(SecurityOAuthClient::getAccountType, query.getAccountType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SecurityOAuthClient::getStatus, query.getStatus());
        }
        return wrapper;
    }

    private OAuthClientSecretVO buildSecretVo(SecurityOAuthClient entity, String plainSecret) {
        OAuthClientSecretVO vo = new OAuthClientSecretVO();
        vo.setId(entity.getId());
        vo.setClientId(entity.getClientId());
        vo.setClientSecret(plainSecret);
        return vo;
    }

    private void evict(SecurityOAuthClient client) {
        cacheUtils.withRebel(() -> {
            cacheUtils.delete(OAuthClientCacheKeys.byId(client.getId()));
            cacheUtils.delete(OAuthClientCacheKeys.byClientId(client.getClientId()));
            return true;
        });
    }

}
