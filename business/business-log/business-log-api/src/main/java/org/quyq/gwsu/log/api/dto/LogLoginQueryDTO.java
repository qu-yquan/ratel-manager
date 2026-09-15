package org.quyq.gwsu.log.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.quyq.gwsu.common.core.domain.BaseDTO;
import org.quyq.gwsu.common.log.enums.LoginEventType;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;

import java.time.LocalDateTime;

/**
 * 认证日志查询条件。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "认证日志查询条件")
public class LogLoginQueryDTO extends BaseDTO {

    private String authorizationId;
    private LoginEventType eventType;
    private AccountType accountType;
    private VisitorType visitorType;
    private String loginType;
    private String clientId;
    private String userId;
    private String userName;
    private String loginAccount;
    private String clientIp;
    private Integer status;
    private LocalDateTime eventTimeStart;
    private LocalDateTime eventTimeEnd;
}
