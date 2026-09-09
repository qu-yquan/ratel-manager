package org.quyq.gwsu.common.security.api.oauth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientStatus;
import org.quyq.gwsu.common.security.api.oauth.enums.OAuthClientType;
import org.quyq.gwsu.common.security.enums.AccountType;

/**
 * OAuth 应用查询条件。
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "OAuth应用查询条件")
public class OAuthClientQueryDTO extends BaseDTO {

    @Schema(description = "OAuth客户端ID")
    private String clientId;

    @Schema(description = "应用名称")
    private String clientName;

    @Schema(description = "客户端类型")
    private OAuthClientType clientType;

    @Schema(description = "账号类型")
    private AccountType accountType;

    @Schema(description = "状态")
    private OAuthClientStatus status;

}
