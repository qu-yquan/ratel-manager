package org.quyq.gwsu.common.security.api.oauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OAuth Scope信息")
public class OAuthScopeVO extends BaseVO {
    private String id;
    private String scopeCode;
    private String scopeName;
    private String description;
    private AccountType accountType;
    private OAuthClientStatus status;
    private List<String> resourceIds;
}
