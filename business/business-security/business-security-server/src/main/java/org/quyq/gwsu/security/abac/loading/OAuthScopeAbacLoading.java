package org.quyq.gwsu.security.abac.loading;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.IAbacAlterationProvider;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiResourceMapper;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScopeResource;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthScopeResourceMapper;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthScopeMapper;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuthScopeAbacLoading implements IAbacAlterationProvider {

    public static final String SCOPE_ID = "scopeId";
    private final SecurityOAuthScopeResourceMapper scopeResourceMapper;
    private final SecurityApiResourceMapper apiResourceMapper;
    private final SecurityOAuthScopeMapper scopeMapper;

    @Override
    public AbacPerType abacType() {
        return AbacPerType.OAUTH_SCOPE;
    }

    @Override
    public String buildExpression(ExpressionContext context) {
        return "contains(r.sub.detail.authorizedScopes, '%s')".formatted(context.getValue());
    }

    @Override
    public void alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        String scopeId = context.getParam(SCOPE_ID);
        var scope = scopeMapper.selectById(scopeId);
        if (scope == null || scope.getStatus() != OAuthClientStatus.ENABLED || Boolean.TRUE.equals(scope.getDeleted())) {
            wrapper.replacePermission(List.of());
            return;
        }
        List<String> resourceIds = scopeResourceMapper.selectList(
                        new LambdaQueryWrapper<SecurityOAuthScopeResource>()
                                .eq(SecurityOAuthScopeResource::getScopeId, scopeId)
                                .eq(SecurityOAuthScopeResource::getDeleted, false))
                .stream().map(SecurityOAuthScopeResource::getApiResourceId).toList();
        if (resourceIds.isEmpty()) {
            wrapper.replacePermission(List.of());
            return;
        }
        List<SecurityAbacPermission> permissions = apiResourceMapper.selectByIds(resourceIds).stream()
                .map(resource -> new SecurityAbacPermission()
                        .setResourceType(resource.getModulePrefix())
                        .setAction(resource.getReqMethod())
                        .setUrlPattern(resource.getReqPath())
                        .setEffect(AbacEffect.PERMIT)
                        .setStatus(true))
                .toList();
        wrapper.replacePermission(permissions);
    }
}
