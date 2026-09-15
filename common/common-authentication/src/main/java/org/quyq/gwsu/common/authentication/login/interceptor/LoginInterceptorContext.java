package org.quyq.gwsu.common.authentication.login.interceptor;

import lombok.Data;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;

import java.util.Map;

/**
 * 登录拦截器上下文
 *
 * @author Quyq
 */
@Data
public class LoginInterceptorContext<T extends UserInfo> {

    /**
     * 登录请求参数
     */
    private final AbstractLoginDTO loginDTO;

    /**
     * 返回对象（可修改）
     */
    private final LoginVO loginVO;

    /**
     * 主体信息（登录成功后有值）
     */
    private Subject<T> subject;

    /**
     * 本次认证会话标识
     */
    private final String authorizationId;

    private final AccountType accountType;

    private final VisitorType visitorType;

    private final String loginType;

    /**
     * 扩展属性，用于拦截器间传递数据
     */
    private final Map<String, Object> attributes;


    public LoginInterceptorContext(AbstractLoginDTO loginDTO, LoginVO loginVO,
                                   String authorizationId, AccountType accountType,
                                   VisitorType visitorType, String loginType) {
        this.loginDTO = loginDTO;
        this.loginVO = loginVO;
        this.authorizationId = authorizationId;
        this.accountType = accountType;
        this.visitorType = visitorType;
        this.loginType = loginType;
        this.attributes = new java.util.HashMap<>();
    }
}
