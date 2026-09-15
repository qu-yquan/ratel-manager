package org.quyq.gwsu.common.authentication.login;


import cn.hutool.core.lang.func.LambdaUtil;
import cn.hutool.core.util.ReflectUtil;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.authentication.domain.AbstractLoginDTO;
import org.quyq.gwsu.common.authentication.domain.LoginToken;
import org.quyq.gwsu.common.authentication.domain.LoginVO;
import org.quyq.gwsu.common.authentication.exception.AuthException;
import org.quyq.gwsu.common.authentication.login.domain.WebCallInfo;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/8
 * @description
 */
@RequiredArgsConstructor
public class LoginManager {

    private final List<LoginHandler<AbstractLoginDTO>> loginHandlers;

    private final ObjectMapper json;

    public LoginResult login(@NonNull JsonNode form, @NonNull AccountType accountType) {
        String typeKey = LambdaUtil.getFieldName(AbstractLoginDTO::getType);

        String loginType = AssertUtils.hasText(Optional.ofNullable(form.get(typeKey)).map(JsonNode::asString).orElse(""), CommonErrorCode.E04004);
        AbstractLoginDTO loginForm = json.treeToValue(form, LoginLoadingManager.supportClass(loginType, accountType));
        VisitorType visitorType = Optional.ofNullable(loginForm.getVisitorType()).orElse(VisitorType.USER);
        return login(loginForm, accountType, visitorType);
    }

    public LoginResult login(@NonNull JsonNode form, @NonNull AccountType accountType, @NonNull VisitorType visitorType) {
        String typeKey = LambdaUtil.getFieldName(AbstractLoginDTO::getType);

        String loginType = AssertUtils.hasText(Optional.ofNullable(form.get(typeKey)).map(JsonNode::asString).orElse(""), CommonErrorCode.E04004);
        AbstractLoginDTO loginForm = json.treeToValue(form, LoginLoadingManager.supportClass(loginType, accountType));
        loginForm.setVisitorType(visitorType);
        return login(loginForm, accountType, visitorType);
    }

    /**
     * 认证
     *
     * @param form
     * @param accountType
     * @param visitorType
     */
    public <T extends AbstractLoginDTO> LoginResult login(@NonNull T form, @NonNull AccountType accountType, @NonNull VisitorType visitorType) {

        AssertUtils.hasText(form.getType(), CommonErrorCode.E04004);
        AssertUtils.notNull(form.getTerminal(), CommonErrorCode.E04005);
        form.setVisitorType(visitorType);

        Optional<LoginHandler<AbstractLoginDTO>> target = loginHandlers.stream()
                .filter(handler -> accountType == handler.accountType() && form.getType().equals(handler.loginType()))
                .findFirst();

        if (target.isPresent()) {
            return genTokenInfo(target.get(), form, visitorType);
        }

        throw new AuthException(CommonErrorCode.E04001, "用户类型【%s】中未实现该登录类型：%s".formatted(accountType, form.getType()));
    }

    /**
     * 返回三方平台认证地址及其他信息
     *
     * @param state
     * @param accountType
     * @param loginType
     * @return
     */
    public WebCallInfo webAuthUrl(@NonNull String state, @NonNull AccountType accountType, @NonNull String loginType) {
        Optional<LoginHandler<AbstractLoginDTO>> target = loginHandlers.stream()
                .filter(handler -> accountType == handler.accountType() && loginType.equals(handler.loginType()))
                .findFirst();

        if (target.isPresent()) {
            return target.get().generateWebCallInfo(state);
        }

        throw new AuthException(CommonErrorCode.E04001, "用户类型【%s】认证实现中未实现该登录类型：%s".formatted(accountType, loginType));

    }


    private LoginResult genTokenInfo(LoginHandler<AbstractLoginDTO> handler, AbstractLoginDTO form, VisitorType visitorType) {
        LoginVO tokenInfo = handler.authenticate(form, visitorType);
        if (tokenInfo.isFail() && tokenInfo.isNeedRedirect()) {
            return redirect(Map.of(LambdaUtil.getFieldName(LoginVO::getErrMsg), tokenInfo.getErrMsg(),
                    LambdaUtil.getFieldName(LoginVO::getErrCode), tokenInfo.getErrCode()), tokenInfo.getRedirectUrl());
        }
        return printSuccess(tokenInfo);

    }


    private LoginResult printSuccess(LoginVO content) {
        LoginToken loginToken = content.conversionToken();
        if (content.isNeedRedirect()) {
            return redirect(loginToken, content.getRedirectUrl());
        }

        return new LoginResult(null, R.ok(loginToken));
    }

    private LoginResult redirect(Object content, String redirectUrl) {
        StringBuilder param = new StringBuilder();
        String separator = redirectUrl.contains("?") ? "&" : "?";

        if (content instanceof Map<?, ?> mapV) {
            int i = 0;
            for (Map.Entry<?, ?> entry : mapV.entrySet()) {
                if (entry.getValue() == null) continue;
                param.append(i++ == 0 ? separator : "&");
                param.append(entry.getKey()).append("=").append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            }
        } else {
            boolean first = true;
            Field[] fields = ReflectUtil.getFields(content.getClass());
            for (Field f : fields) {
                Object fieldValue = ReflectUtil.getFieldValue(content, f);
                if (fieldValue == null) continue;

                if (fieldValue instanceof Map<?, ?> mapVal) {
                    if (mapVal.isEmpty()) continue;
                    for (Map.Entry<?, ?> entry : mapVal.entrySet()) {
                        if (entry.getValue() == null) continue;
                        param.append(first ? separator : "&");
                        first = false;
                        param.append(entry.getKey()).append("=").append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
                    }
                } else {
                    param.append(first ? separator : "&");
                    first = false;
                    param.append(f.getName()).append("=").append(URLEncoder.encode(String.valueOf(fieldValue), StandardCharsets.UTF_8));
                }
            }
        }

        return new LoginResult(redirectUrl + param, null);
    }


    public record LoginResult(String redirectUrl,
                              R<LoginToken> token) {

        /**
         * 是否需要重定向
         *
         * @return
         */
        public boolean isRedirect() {
            return StringUtils.hasText(redirectUrl);
        }

    }


}
