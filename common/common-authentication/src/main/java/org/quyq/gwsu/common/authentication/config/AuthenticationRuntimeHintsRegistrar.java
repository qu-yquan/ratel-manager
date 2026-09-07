package org.quyq.gwsu.common.authentication.config;

import org.quyq.gwsu.common.authentication.domain.LoginToken;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.domain.WorkspaceInfo;
import org.quyq.gwsu.common.authentication.oauth.domain.OAuthUserSessionSnapshot;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * 认证模块运行时提示注册器，用于 AOT 编译
 *
 * @author Quyq
 */
public class AuthenticationRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // 注册 LoginToken 类的反射提示（LoginManager.redirect() 中使用反射获取字段）
        hints.reflection()
                .registerType(LoginToken.class, MemberCategory.ACCESS_DECLARED_FIELDS)
                .registerType(LoginVO.class, MemberCategory.ACCESS_DECLARED_FIELDS)
                .registerType(WorkspaceInfo.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.ACCESS_DECLARED_FIELDS)
                .registerType(OAuthUserSessionSnapshot.class, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.ACCESS_DECLARED_FIELDS);
    }
}
