package org.quyq.gwsu.security.oauth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.domain.KeyValue;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.security.annotation.LoginAllowAccess;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.quyq.gwsu.common.security.api.oauth.OAuthClientApi;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthClientSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientAuthenticationMethod;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthGrantType;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientSecretVO;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthConsentContextVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthClient;
import org.quyq.gwsu.security.oauth.service.ISecurityOAuthClientService;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OAuth 应用管理。
 *
 * @author Quyq
 */
@RestController
@RequestMapping("oauth/client")
@Tag(name = "OAuth应用管理")
@TableModelPermission({SecurityOAuthClient.class})
@RequiredArgsConstructor
public class SecurityOAuthClientController implements OAuthClientApi {

    private final ISecurityOAuthClientService oauthClientService;

    @Override
    @LoginAllowAccess
    @TableModelPermission
    @Operation(summary = "根据ID查询OAuth应用")
    @GetMapping("/id/{id}")
    public R<OAuthClientInfoVO> getById(@PathVariable String id) {
        return R.ok(oauthClientService.getInfoById(id));
    }

    @Override
    @LoginAllowAccess
    @TableModelPermission
    @Operation(summary = "根据ClientId查询OAuth应用")
    @GetMapping("/clientId/{clientId}")
    public R<OAuthClientInfoVO> getByClientId(@PathVariable String clientId) {
        return R.ok(oauthClientService.getByClientId(clientId));
    }

    @Override
    @LoginAllowAccess
    @TableModelPermission
    @Operation(summary = "获取OAuth应用授权上下文")
    @GetMapping("/consent-context")
    public R<OAuthConsentContextVO> getConsentContext(
            @RequestParam String clientId,
            @RequestParam(required = false) String scope) {
        return R.ok(oauthClientService.getConsentContext(clientId, scope));
    }

    @Override
    @Operation(summary = "获取OAuth应用枚举选项")
    @GetMapping("/enums")
    public R<Map<String, List<KeyValue<String, String>>>> enumOptions() {
        Map<String, List<KeyValue<String, String>>> options = new LinkedHashMap<>();
        options.put("clientTypes", Arrays.stream(OAuthClientType.values())
                .map(item -> new KeyValue<>(item.name(), item.getName()))
                .toList());
        options.put("accountTypes", Arrays.stream(AccountType.values())
                .map(item -> new KeyValue<>(item.name(), item.getMsg()))
                .toList());
        options.put("statuses", Arrays.stream(OAuthClientStatus.values())
                .map(item -> new KeyValue<>(item.name(), item.getName()))
                .toList());
        options.put("grantTypes", Arrays.stream(OAuthGrantType.values())
                .map(item -> new KeyValue<>(item.name(), item.getName()))
                .toList());
        options.put("authenticationMethods", Arrays.stream(OAuthClientAuthenticationMethod.values())
                .map(item -> new KeyValue<>(item.name(), item.getName()))
                .toList());
        return R.ok(options);
    }

    @Operation(summary = "分页查询OAuth应用")
    @PostMapping("/page")
    public R<IPage<OAuthClientInfoVO>> page(@RequestBody OAuthClientQueryDTO query) {
        return R.ok(oauthClientService.pageByCondition(query));
    }

    @Operation(summary = "新增或更新OAuth应用")
    @PostMapping
    public R<OAuthClientSecretVO> saveOrUpdate(@RequestBody OAuthClientSaveDTO dto) {
        return R.ok(oauthClientService.saveOrUpdateClient(dto));
    }

    @Operation(summary = "重置OAuth应用密钥")
    @PostMapping("/{id}/secret/reset")
    public R<OAuthClientSecretVO> resetSecret(@PathVariable String id) {
        return R.ok(oauthClientService.resetSecret(id));
    }

    @Operation(summary = "批量删除OAuth应用")
    @DeleteMapping
    public R<Boolean> remove(@RequestBody List<String> ids) {
        return R.ok(oauthClientService.removeClients(ids));
    }

}
