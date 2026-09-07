/*
 * Copyright 2020-2099 sa-token.cc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quyq.gwsu.common.authentication.filter;

import cn.dev33.satoken.servlet.util.SaTokenContextJakartaServletUtil;
import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quyq.gwsu.common.authentication.constants.AuthenticationConstants;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.R;
import org.quyq.gwsu.common.core.exception.BasicException;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.http.MediaType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * SaTokenContext 上下文初始化过滤器 (基于 Jakarta-Servlet)
 *
 * @author click33
 * @since 1.42.0
 */
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ContextFilterForJakartaServlet implements Filter {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
            var req = (HttpServletRequest) request;
            SaTokenContextJakartaServletUtil.setContext(req, (HttpServletResponse) response);
            String accountType = null;
            String token = normalizeToken(RequestAuthenticationTokenResolver.resolve(req));
            if ( StringUtils.hasText(token) && JWTUtil.verify(token,
                    SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8))) {
                accountType = JWTUtil.parseToken(token)
                        .getPayloads().getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY);
            }
            ScopedValue.where(AuthenticationConstants.ACCOUNT_TYPE, Optional.ofNullable(accountType)
                            .map(AccountType::fromString).orElse(null))
                    .call(() -> {
                        chain.doFilter(request, response);
                        return 0;
                    });

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            writeUnauthorizedResponse((HttpServletResponse) response , new BusinessException(e));
        } finally {
            SaTokenContextJakartaServletUtil.clearContext();
        }
    }

    private void writeUnauthorizedResponse(HttpServletResponse response , BasicException exception) {
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        try {
            response.getWriter().write(JSON_MAPPER.writeValueAsString(R.fail(exception)));
            response.getWriter().flush();
        } catch (Exception writeException) {
            log.error("写入未授权响应失败", writeException);
        }
    }


    private String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (token.startsWith(SecurityConstants.Authentication.API_KEY_PREFIX)) {
            return token.substring(SecurityConstants.Authentication.API_KEY_PREFIX.length());
        }
        return token;
    }

}
