package org.quyq.gwsu.common.security.api.oauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "OAuth应用授权上下文")
public class OAuthConsentContextVO {
    private String clientId;
    private String clientName;
    private List<OAuthScopeVO> scopes;
    private boolean canAuthorize;
}
