package org.quyq.gwsu.common.security.domain;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.JsonAdapter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.deserializer.VisitorDeserializer;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/4
 * @description
 */
@Getter
@Setter
@EqualsAndHashCode
public class Subject<T extends Visitor> {

    @JsonCreator
    public Subject(
            @NonNull @JsonProperty("detail") T info) {
        this.detail = info;
    }


    private TerminalType terminalType;
    /**
     * 角色信息
     */
    private List<String> roles = new ArrayList<>();

    /**
     * 数据权限作用域
     */
    private DataScope dataScope;


    /**
     * 登录的主体 ， 如果是用户登录，则是UserInfo ,如果是客户端登录，则是ClientInfo
     */
    @JsonAdapter(VisitorDeserializer.class)
    private final T detail;

    /**
     * 客户端登录，如果是用户授权 ， 此处为授权用户信息
     * 注意：正常登录，该字段为空
     */
    @JsonAdapter(VisitorDeserializer.class)
    private UserInfo authUser;


    /**
     * 获取用户信息
     *
     * @param <U>
     * @return
     */
    public <U extends UserInfo> Optional<U> userInfo() {

        if (detail instanceof UserInfo) {
            return Optional.of((U) detail);
        }

        return Optional.ofNullable((U) authUser);
    }

    /**
     * 获取客户端信息
     *
     * @param <U>
     * @return
     */
    public <U extends ClientInfo> Optional<U> clientInfo() {

        if (detail instanceof ClientInfo) {
            return Optional.of((U) detail);
        }

        return Optional.empty();
    }

    /**
     * 当前用户是否是管理员
     *
     * @return
     */
    public boolean isAdmin() {
        return !CollectionUtils.isEmpty(roles) && roles.contains(SecurityConstants.Authentication.ROLE_SUPER_ADMIN_FLAG);
    }


}
