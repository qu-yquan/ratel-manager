package org.quyq.gwsu.security.oauth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.api.oauth.OAuthScopeConstants;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthScopeVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.loading.OAuthScopeAbacLoading;
import org.quyq.gwsu.security.abac.service.PermissionAlterationManager;
import org.quyq.gwsu.security.apiresource.domain.SecurityApiResource;
import org.quyq.gwsu.security.apiresource.mapper.SecurityApiResourceMapper;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScope;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScopeResource;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthScopeMapper;
import org.quyq.gwsu.security.oauth.mapper.SecurityOAuthScopeResourceMapper;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SecurityOAuthScopeServiceImpl extends ServiceImpl<SecurityOAuthScopeMapper, SecurityOAuthScope>
        implements ISecurityOAuthScopeService {

    private static final Pattern SCOPE_CODE_PATTERN = Pattern.compile("[a-z][a-z0-9]*(?:[._:-][a-z0-9]+)*");
    private final SecurityOAuthScopeResourceMapper scopeResourceMapper;
    private final SecurityApiResourceMapper apiResourceMapper;
    private final PermissionAlterationManager permissionAlterationManager;

    @Override
    public IPage<OAuthScopeVO> pageByCondition(OAuthScopeQueryDTO query) {
        OAuthScopeQueryDTO condition = query == null ? new OAuthScopeQueryDTO() : query;
        var wrapper = new LambdaQueryWrapper<SecurityOAuthScope>()
                .eq(SecurityOAuthScope::getDeleted, false)
                .eq(condition.getAccountType() != null, SecurityOAuthScope::getAccountType, condition.getAccountType())
                .eq(condition.getStatus() != null, SecurityOAuthScope::getStatus, condition.getStatus())
                .and(StringUtils.hasText(condition.getKeyword()), item -> item
                        .like(SecurityOAuthScope::getScopeCode, condition.getKeyword())
                        .or().like(SecurityOAuthScope::getScopeName, condition.getKeyword())
                        .or().like(SecurityOAuthScope::getDescription, condition.getKeyword()))
                .orderByDesc(SecurityOAuthScope::getModifyTime);
        return page(new Page<>(condition.getPageNum(), condition.getPageSize()), wrapper).convert(SecurityOAuthScope::toVo);
    }

    @Override
    public OAuthScopeVO getInfo(String id) {
        SecurityOAuthScope scope = getById(id);
        if (scope == null || Boolean.TRUE.equals(scope.getDeleted())) {
            throw new BusinessException(SecurityErrorCode.E08103);
        }
        OAuthScopeVO vo = scope.toVo();
        vo.setResourceIds(scopeResourceMapper.selectList(new LambdaQueryWrapper<SecurityOAuthScopeResource>()
                        .eq(SecurityOAuthScopeResource::getScopeId, id)
                        .eq(SecurityOAuthScopeResource::getDeleted, false))
                .stream().map(SecurityOAuthScopeResource::getApiResourceId).toList());
        return vo;
    }

    @Override
    public List<OAuthScopeVO> listOptions(AccountType accountType, OAuthClientStatus status) {
        List<OAuthScopeVO> scopes = new ArrayList<>(lambdaQuery().eq(SecurityOAuthScope::getDeleted, false)
                .eq(accountType != null, SecurityOAuthScope::getAccountType, accountType)
                .eq(status != null, SecurityOAuthScope::getStatus, status)
                .orderByAsc(SecurityOAuthScope::getScopeCode).list().stream()
                .map(SecurityOAuthScope::toVo).toList());
        if (status == null || status == OAuthClientStatus.ENABLED) {
            scopes.removeIf(scope -> isInheritPermissionsScope(scope.getScopeCode()));
            scopes.add(inheritPermissionsScope());
        }
        return scopes;
    }

    @Override
    public List<OAuthScopeVO> listByCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        List<OAuthScopeVO> scopes = new ArrayList<>(lambdaQuery().in(SecurityOAuthScope::getScopeCode, new LinkedHashSet<>(codes))
                .eq(SecurityOAuthScope::getDeleted, false).list().stream()
                .map(SecurityOAuthScope::toVo).toList());
        OAuthScopeVO builtin = inheritPermissionsScope();
        if (codes.contains(builtin.getScopeCode())) {
            scopes.removeIf(scope -> builtin.getScopeCode().equals(scope.getScopeCode()));
            scopes.add(builtin);
        }
        return scopes;
    }

    @Override
    @Transactional
    public Boolean saveScope(OAuthScopeSaveDTO dto) {
        AssertUtils.notNull(dto.getAccountType(), SecurityErrorCode.E08105);
        AssertUtils.hasText(dto.getScopeName(), SecurityErrorCode.E08104);
        String code = normalizeCode(dto.getScopeCode(), dto.getAccountType());
        SecurityOAuthScope existing = StringUtils.hasText(dto.getId()) ? getById(dto.getId()) : null;
        if (StringUtils.hasText(dto.getId()) && existing == null) {
            throw new BusinessException(SecurityErrorCode.E08103);
        }
        if (existing != null && (!existing.getScopeCode().equals(code) || existing.getAccountType() != dto.getAccountType())) {
            throw new BusinessException(SecurityErrorCode.E08106);
        }
        long duplicate = lambdaQuery().eq(SecurityOAuthScope::getScopeCode, code)
                .eq(SecurityOAuthScope::getDeleted, false)
                .ne(StringUtils.hasText(dto.getId()), SecurityOAuthScope::getId, dto.getId()).count();
        AssertUtils.isTrue(duplicate == 0, SecurityErrorCode.E08102);

        Set<String> resourceIds = dto.getResourceIds() == null ? Set.of() : new LinkedHashSet<>(dto.getResourceIds());
        if (!resourceIds.isEmpty()) {
            long resourceCount = apiResourceMapper.selectCount(new LambdaQueryWrapper<SecurityApiResource>()
                    .in(SecurityApiResource::getId, resourceIds).eq(SecurityApiResource::getDeleted, false));
            AssertUtils.isTrue(resourceCount == resourceIds.size(), SecurityErrorCode.E08107);
        }

        SecurityOAuthScope scope = existing == null ? new SecurityOAuthScope() : existing;
        scope.setScopeCode(code).setScopeName(dto.getScopeName().trim()).setDescription(dto.getDescription())
                .setAccountType(dto.getAccountType())
                .setStatus(dto.getStatus() == null ? OAuthClientStatus.ENABLED : dto.getStatus());
        saveOrUpdate(scope);
        scopeResourceMapper.delete(new LambdaQueryWrapper<SecurityOAuthScopeResource>()
                .eq(SecurityOAuthScopeResource::getScopeId, scope.getId()));
        resourceIds.forEach(resourceId -> scopeResourceMapper.insert(new SecurityOAuthScopeResource()
                .setScopeId(scope.getId()).setApiResourceId(resourceId)));
        refreshPolicy(scope);
        return true;
    }

    @Override
    @Transactional
    public Boolean removeScopes(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return true;
        }
        List<SecurityOAuthScope> scopes = listByIds(ids);
        for (SecurityOAuthScope scope : scopes) {
            scopeResourceMapper.delete(new LambdaQueryWrapper<SecurityOAuthScopeResource>()
                    .eq(SecurityOAuthScopeResource::getScopeId, scope.getId()));
            refreshPolicy(scope);
        }
        return removeByIds(ids);
    }

    private void refreshPolicy(SecurityOAuthScope scope) {
        ExpressionContext context = new ExpressionContext();
        context.setValue(scope.getScopeCode());
        context.putExtraParam(OAuthScopeAbacLoading.SCOPE_ID, scope.getId());
        permissionAlterationManager.alterationUrlPermission(AbacPerType.OAUTH_SCOPE, context);
    }

    static String normalizeCode(String code, AccountType accountType) {
        AssertUtils.hasText(code, SecurityErrorCode.E08101);
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        AssertUtils.isTrue(!isInheritPermissionsScope(normalized), SecurityErrorCode.E08109);
        String prefix = accountType.name().toLowerCase(Locale.ROOT) + ".";
        if (!normalized.startsWith(prefix)) {
            normalized = prefix + normalized;
        }
        AssertUtils.isTrue(SCOPE_CODE_PATTERN.matcher(normalized).matches(), SecurityErrorCode.E08101);
        return normalized;
    }

    private static OAuthScopeVO inheritPermissionsScope() {
        OAuthScopeVO scope = new OAuthScopeVO();
        scope.setScopeCode(OAuthScopeConstants.INHERIT_USER_PERMISSIONS);
        scope.setScopeName("继承用户所有权限");
        scope.setDescription("允许应用使用您当前账号拥有的全部角色权限。");
        scope.setAccountType(null);
        scope.setStatus(OAuthClientStatus.ENABLED);
        scope.setResourceIds(List.of());
        return scope;
    }

    private static boolean isInheritPermissionsScope(String code) {
        return OAuthScopeConstants.INHERIT_USER_PERMISSIONS.equals(code);
    }
}
