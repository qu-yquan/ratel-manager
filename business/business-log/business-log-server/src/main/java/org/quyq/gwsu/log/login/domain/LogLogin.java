package org.quyq.gwsu.log.login.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.log.enums.LoginEndType;
import org.quyq.gwsu.common.log.vo.LogLoginVO;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;

import java.time.LocalDateTime;

/**
 * 认证日志实体。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "log_login", autoResultMap = true)
@Schema(description = "认证日志表")
public class LogLogin extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;
    private String authorizationId;
    private AccountType accountType;
    private VisitorType visitorType;
    private String loginType;
    private String grantType;
    private String clientId;
    private String userId;
    private String userName;
    private String loginAccount;
    private LoginEndType endType;
    private TerminalType terminal;
    private String terminalDetail;
    private String clientIp;
    private Integer status;
    private String failureCode;
    private String failureMessage;
    private LocalDateTime loginTime;
    private LocalDateTime endTime;

    public LogLoginVO toVo() {
        LogLoginVO vo = new LogLoginVO()
                .setId(id).setAuthorizationId(authorizationId)
                .setAccountType(accountType).setVisitorType(visitorType).setLoginType(loginType)
                .setGrantType(grantType).setClientId(clientId).setUserId(userId).setUserName(userName)
                .setLoginAccount(loginAccount).setEndType(endType).setTerminal(terminal)
                .setTerminalDetail(terminalDetail).setClientIp(clientIp).setStatus(status != null && status == 1)
                .setFailureCode(failureCode).setFailureMessage(failureMessage)
                .setLoginTime(loginTime).setEndTime(endTime);
        vo.copyBaseProperties(this);
        return vo;
    }

    public static LogLogin toDo(LogLoginVO vo) {
        LogLogin entity = new LogLogin()
                .setId(vo.getId()).setAuthorizationId(vo.getAuthorizationId())
                .setAccountType(vo.getAccountType()).setVisitorType(vo.getVisitorType()).setLoginType(vo.getLoginType())
                .setGrantType(vo.getGrantType()).setClientId(vo.getClientId()).setUserId(vo.getUserId())
                .setUserName(vo.getUserName()).setLoginAccount(vo.getLoginAccount())
                .setEndType(vo.getEndType()).setTerminal(vo.getTerminal())
                .setTerminalDetail(vo.getTerminalDetail()).setClientIp(vo.getClientIp())
                .setStatus(Boolean.TRUE.equals(vo.getStatus()) ? 1 : 0).setFailureCode(vo.getFailureCode())
                .setFailureMessage(vo.getFailureMessage()).setLoginTime(vo.getLoginTime())
                .setEndTime(vo.getEndTime());
        entity.setCreateOp(vo.getCreateOp());
        entity.setCreateTime(vo.getCreateTime());
        entity.setModifyOp(vo.getModifyOp());
        entity.setModifyTime(vo.getModifyTime());
        return entity;
    }
}
