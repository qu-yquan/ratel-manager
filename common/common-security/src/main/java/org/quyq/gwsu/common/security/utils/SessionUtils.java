package org.quyq.gwsu.common.security.utils;


import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.quyq.gwsu.common.security.enums.AccountType;
import org.springframework.util.StringUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author Quyq
 * @date 2026/4/11
 * @description
 */
@RequiredArgsConstructor
public class SessionUtils {

    private final CacheUtils cacheUtils;

    private final SecurityUtils securityUtils;

    private final ObjectMapper mapper;


    private static final String TOKEN_KEY = "token";


    /**
     * 获取当前登录用户的访问者类型
     *
     * @return
     */
    public VisitorType getVisitorType() {
        Optional<VisitorType> value = getValue(SecurityConstants.Session.SESSION_USER_VISITOR_TYPE);

        return value.orElse(null);
    }

    public String getLoginType() {
        Optional<String> value = getValue(SecurityConstants.Session.SESSION_USER_LOGIN_TYPE);
        return value.orElse(null);
    }

    /**
     * 获取当前 Token 对应的认证会话标识。
     */
    public Optional<String> getAuthorizationId() {
        return getValue(SecurityConstants.Session.SESSION_AUTHORIZATION_ID);
    }

    public Optional<AccountType> getAccountType() {
        return getValue(SecurityConstants.Session.SESSION_ACCOUNT_TYPE);
    }

    /**
     * <p>
     * 获取指定数据
     *
     * @param key 键名
     * @return 值的 Optional 包装
     */
    public <V> Optional<V> getValue(String key) {
        return getPayloads()
                .flatMap(this::getTokenSessionValue)
                .map(mapper::readTree)
                .map(session -> session.get(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY))
                .map(map -> map.get(key))
                .map(value -> mapper.convertValue(value, new TypeReference<>() {
                }));
    }

    public <V> void putValue(String key, V value) {
        Optional<JSONObject> payloads = getPayloads();
        Optional<ObjectNode> jsonNodes = payloads
                .flatMap(this::getTokenSessionValue)
                .map(str -> mapper.readValue(str, ObjectNode.class));
        if (jsonNodes.isEmpty()) {
            return;
        }
        ObjectNode target = jsonNodes.get();
        JsonNode dataMapNode = target.get(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY);

        ObjectNode dataMap;
        if (dataMapNode instanceof ObjectNode on) {
            dataMap = on;
        } else {
            dataMap = mapper.createObjectNode();
            target.set(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY, dataMap);
        }

        ObjectNode jsonNode = mapper.valueToTree(value);
        //解决record类型序列化没有@class问题
        if (value.getClass().isRecord()) {
            jsonNode.put("@class", value.getClass().getName());
        }

        dataMap.set(key, jsonNode);


        // 将修改后的 session 数据保存回 Redis
        payloads.ifPresent(payload -> {
            String loginType = payload.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY);
            String token = payload.getStr(TOKEN_KEY);
            String sessionKey = SecurityConstants.Authentication.TOKEN_SPLICING_KEY_TOKEN_SESSION.apply(loginType) + token;
            cacheUtils.withRebel(() -> {
                Long expire = cacheUtils.getExpire(sessionKey, TimeUnit.MICROSECONDS);
                cacheUtils.set(sessionKey, mapper.writeValueAsString(target), expire, TimeUnit.MICROSECONDS);
                return true;
            });

        });

    }

    private Optional<JSONObject> getPayloads() {
        String token = securityUtils.normalizeToken(securityUtils.getToken());
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        return Optional.ofNullable(JWTUtil.parseToken(token).getPayloads())
                .map(v -> v.putOnce(TOKEN_KEY, token));
    }

    /**
     * 获取tokenSession 字符串类型
     *
     * @param payloads
     * @return
     */
    private Optional<String> getTokenSessionValue(JSONObject payloads) {

        return cacheUtils.withRebel(() ->
                Optional.ofNullable(payloads)
                        .map(payload -> {
                            String loginType = payload.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY);
                            String token = payload.getStr(TOKEN_KEY);
                            return cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_TOKEN_SESSION.apply(loginType) + token);
                        })
                        .map(Object::toString));


    }


}
