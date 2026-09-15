package org.quyq.gwsu.common.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.log.enums.LoginEndType;
import org.quyq.gwsu.common.log.enums.LoginLogAction;
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

    @Schema(description = "认证日志写入动作", hidden = true)
    private LoginLogAction action;

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

    @Schema(description = "内部日志保存使用的原始Token", hidden = true)
    @ToString.Exclude
    private String token;

    @Schema(description = "会话结束类型")
    private LoginEndType endType;

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

    @Schema(description = "登录时间")
    private LocalDateTime loginTime;

    @Schema(description = "会话结束时间")
    private LocalDateTime endTime;
}
