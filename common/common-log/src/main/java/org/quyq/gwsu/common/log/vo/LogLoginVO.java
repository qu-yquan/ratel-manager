package org.quyq.gwsu.common.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.log.enums.LoginEventType;
import org.quyq.gwsu.common.log.enums.LoginSessionStatus;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;

import java.time.LocalDateTime;

/**
 * 认证日志 VO。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@Schema(description = "认证日志")
public class LogLoginVO extends BaseVO {

    @Schema(description = "日志标识")
    private String id;

    @Schema(description = "认证会话标识")
    private String authorizationId;

    @Schema(description = "事件类型")
    private LoginEventType eventType;

    @Schema(description = "账号类型")
    private AccountType accountType;

    @Schema(description = "访问者类型")
    private VisitorType visitorType;

    @Schema(description = "登录类型")
    private String loginType;

    @Schema(description = "OAuth2 授权类型")
    private String grantType;

    @Schema(description = "OAuth2 客户端标识")
    private String clientId;

    @Schema(description = "用户标识")
    private String userId;

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "登录账号")
    private String loginAccount;

    @Schema(description = "Token HMAC 指纹")
    private String tokenFingerprint;

    @Schema(description = "Token HMAC 密钥版本")
    private String tokenKeyVersion;

    @Schema(description = "会话状态")
    private LoginSessionStatus sessionStatus;

    @Schema(description = "终端")
    private TerminalType terminal;

    @Schema(description = "终端详情")
    private String terminalDetail;

    @Schema(description = "客户端IP")
    private String clientIp;

    @Schema(description = "是否成功")
    private Boolean status;

    @Schema(description = "失败错误码")
    private String failureCode;

    @Schema(description = "失败原因")
    private String failureMessage;

    @Schema(description = "事件时间")
    private LocalDateTime eventTime;
}
