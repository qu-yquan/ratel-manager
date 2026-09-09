package org.quyq.gwsu.security.oauth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthScopeVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScope;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScopeResource;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthScopeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("oauth/scope")
@Tag(name = "OAuth Scope管理")
@TableModelPermission({SecurityOAuthScope.class, SecurityOAuthScopeResource.class})
@RequiredArgsConstructor
public class SecurityOAuthScopeController {
    private final ISecurityOAuthScopeService scopeService;

    @PostMapping("page")
    @Operation(summary = "分页查询Scope")
    public R<IPage<OAuthScopeVO>> page(@RequestBody OAuthScopeQueryDTO query) {
        return R.ok(scopeService.pageByCondition(query));
    }

    @GetMapping("{id}")
    public R<OAuthScopeVO> get(@PathVariable String id) {
        return R.ok(scopeService.getInfo(id));
    }

    @GetMapping("options")
    public R<List<OAuthScopeVO>> options(@RequestParam(required = false) AccountType accountType) {
        return R.ok(scopeService.listOptions(accountType, OAuthClientStatus.ENABLED));
    }

    @LoginAllowAccess
    @TableModelPermission
    @PostMapping("descriptions")
    public R<List<OAuthScopeVO>> descriptions(@RequestBody List<String> codes) {
        return R.ok(scopeService.listByCodes(codes));
    }

    @PostMapping
    public R<Boolean> save(@RequestBody OAuthScopeSaveDTO dto) {
        return R.ok(scopeService.saveScope(dto));
    }

    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(scopeService.removeScopes(ids));
    }
}
