package org.quyq.gwsu.security.oauth.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientSecretVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthClient;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthClientMapper;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthClientService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

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
        return buildSecretVo(client, plainSecret);
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
        if (OAuthClientType.CONFIDENTIAL != dto.getClientType()) {
            dto.setRequireProofKey(true);
            dto.setClientAuthenticationMethods(List.of(OAuthClientAuthenticationMethod.NONE));
        }
    }

    private void validate(OAuthClientSaveDTO dto) {
        AssertUtils.hasText(dto.getClientName(), SecurityErrorCode.E08001);
        AssertUtils.notNull(dto.getClientType(), SecurityErrorCode.E08002);
        AssertUtils.notEmpty(dto.getAuthorizationGrantTypes(), SecurityErrorCode.E08003);
        AssertUtils.notEmpty(dto.getScopes(), SecurityErrorCode.E08004);
        AssertUtils.notEmpty(dto.getClientAuthenticationMethods(), SecurityErrorCode.E08009);

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

}
