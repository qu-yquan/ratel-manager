package org.quyq.gwsu.common.security.api.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.enums.AccountType;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OAuth Scope查询条件")
public class OAuthScopeQueryDTO extends BaseDTO {
    private String keyword;
    private AccountType accountType;
    private OAuthClientStatus status;
}
