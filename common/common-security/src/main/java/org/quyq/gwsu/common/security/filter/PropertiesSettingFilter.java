package org.quyq.gwsu.common.security.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.DataPermissionInfo;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.utils.DataPermissionUtils;
import org.quyq.gwsu.common.security.utils.AuthenticationTokenUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.common.security.utils.SessionUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2024/4/29
 * @description 统一全局过滤器
 */
@Order(-100)
@RequiredArgsConstructor
public class PropertiesSettingFilter implements Filter {

    private final SecurityUtils securityUtils;
    private final SessionUtils sessionUtils;
    private final DataPermissionUtils dataPermissionUtils;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, jakarta.servlet.FilterChain filterChain) throws IOException, ServletException {
        try {
            Map<String, String> headers = ServletUtils.getHeaders((HttpServletRequest) servletRequest);

            Optional<Subject<Visitor>> subjectOpt = securityUtils.getSubject(getToken(headers));


            ServletUtils.LOCAL_HEADERS.set(headers);

            if (subjectOpt.isPresent()) {
                Subject<Visitor> subject = subjectOpt.get();
                //设置用户名到请求头
                putUserName(subject, headers);

                //初始化当前用户数据权限
                Optional<Map<String, List<?>>> dataResource = sessionUtils.getValue(SecurityConstants.Session.SESSION_CURR_DATA_RESOURCE);
                dataPermissionUtils.setDataPermission(new DataPermissionInfo(subject.getDataScope(), dataResource.orElse(Collections.emptyMap())));


            }


            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            //清除当前线程头部信息
            ServletUtils.LOCAL_HEADERS.remove();
            //清楚当前用户数据权限
            dataPermissionUtils.clearDataPermission();
        }
    }


    private void putUserName(Subject<Visitor> subject, Map<String, String> headers) {
        Visitor detail = subject.getDetail();
        if (detail instanceof UserInfo) {
            subject.userInfo().ifPresent(userInfo ->
                    headers.put(CoreConstants.Headers.AUTHORIZATION_USER_NAME, userInfo.getUserName())
            );
        } else {
            subject.clientInfo().ifPresent(clientInfo ->
                    headers.put(CoreConstants.Headers.AUTHORIZATION_USER_NAME, clientInfo.getClientName())
            );
        }

    }


    private String getToken(Map<String, String> headers) {
        return AuthenticationTokenUtils.resolve(headers.get(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY));
    }


}
