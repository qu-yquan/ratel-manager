package org.quyq.gwsu.security.oauth.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeQueryDTO;
import org.quyq.gwsu.common.security.api.oauth.dto.OAuthScopeSaveDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthScopeVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.security.oauth.domain.SecurityOAuthScope;

import java.util.List;

public interface ISecurityOAuthScopeService extends IService<SecurityOAuthScope> {
    IPage<OAuthScopeVO> pageByCondition(OAuthScopeQueryDTO query);
    OAuthScopeVO getInfo(String id);
    List<OAuthScopeVO> listOptions(AccountType accountType, OAuthClientStatus status);
    List<OAuthScopeVO> listByCodes(List<String> codes);
    Boolean saveScope(OAuthScopeSaveDTO dto);
    Boolean removeScopes(List<String> ids);
}
