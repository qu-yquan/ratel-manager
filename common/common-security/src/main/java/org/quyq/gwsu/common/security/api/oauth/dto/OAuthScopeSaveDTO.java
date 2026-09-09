package org.quyq.gwsu.common.security.api.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.enums.AccountType;

import java.util.List;

@Data
@Schema(description = "OAuth Scope保存参数")
public class OAuthScopeSaveDTO {
    private String id;
    private String scopeCode;
    private String scopeName;
    private String description;
    private AccountType accountType;
    private OAuthClientStatus status;
    private List<String> resourceIds;
}
