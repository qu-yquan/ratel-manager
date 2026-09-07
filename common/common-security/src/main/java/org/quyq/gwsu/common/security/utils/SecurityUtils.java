package org.quyq.gwsu.common.security.utils;


import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWTException;
import cn.hutool.jwt.JWTUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.cache.utils.CacheUtils;
import org.quyq.gwsu.common.core.config.GsonConfiguration;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.ServletUtils;
import org.quyq.gwsu.common.security.constants.SecurityConstants;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.domain.deserializer.JacksonCompatibleTypeAdapterFactory;
import org.quyq.gwsu.common.security.enums.DataScope;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Quyq
 * @date 2026/4/9
 * @description
 */
@RequiredArgsConstructor
public class SecurityUtils {
    private final CacheUtils cacheUtils;
    private static final Gson gson;

    static {
        gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(LocalDateTime.class, GsonConfiguration.jsonSerializerDateTime)
                .registerTypeAdapter(LocalDate.class, GsonConfiguration.jsonSerializerDate)
                .registerTypeAdapter(LocalDateTime.class, GsonConfiguration.jsonDeserializerDateTime)
                .registerTypeAdapter(LocalDate.class, GsonConfiguration.jsonDeserializerDate)
                .registerTypeAdapter(DataScope.class, (JsonDeserializer<DataScope>) (json, type, context) ->
                        DataScope.of(json.getAsInt()))
                .registerTypeAdapterFactory(new JacksonCompatibleTypeAdapterFactory())
                .create();
    }

    /**
     * 获取当前登录用户的username
     *
     * @return
     */
    public String getUsername() {
        return Optional.ofNullable(ServletUtils.getHeaders())
                .map(h -> h.get(CoreConstants.Headers.AUTHORIZATION_USER_NAME))
                .orElse(null);
    }

    /**
     * 获取Token
     *
     * @return
     */
    public String getToken() {
        return Optional.ofNullable(ServletUtils.getHeaders())
                .map(headers -> headers.get(CoreConstants.Headers.HTTP_HEADER_TOKEN_KEY))
                .map(AuthenticationTokenUtils::resolve)
                .map(this::normalizeToken)
                .filter(this::isValidJwt)
                .orElse(null);
    }


    /**
     * 获取登录的客户端信息
     *
     * @param <U>
     * @return
     */
    public <U extends ClientInfo> Optional<U> clientInfo() {
        return clientInfo(getToken());
    }

    /**
     * 获取登录的客户端信息
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends ClientInfo> Optional<U> clientInfo(String token) {
        return getSubject(token)
                .flatMap(Subject::clientInfo);

    }

    /**
     * 获取登录的用户信息
     *
     * @param <U>
     * @return
     */
    public <U extends UserInfo> Optional<U> userInfo() {
        return userInfo(getToken());
    }

    /**
     * 获取登录的用户信息
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends UserInfo> Optional<U> userInfo(String token) {
        return getSubject(token)
                .flatMap(Subject::userInfo);
    }

    /**
     * 获取当前登录主体
     *
     * @param <U>
     * @return
     */
    public <U extends Visitor> Optional<Subject<U>> getSubject() {
        return getSubject(getToken());
    }


    public <U extends Visitor> Subject<U> checkSubject() {
        return (Subject<U>) getSubject()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.E03001));
    }

    /**
     * 获取当前登录主体
     *
     * @param token
     * @param <U>
     * @return
     */
    public <U extends Visitor> Optional<Subject<U>> getSubject(String token) {
        return parsePayloads(token)
                .flatMap(payloads -> readAccountSession(payloads, normalizeToken(token)))
                .map(session -> session.getAsJsonObject(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY))
                .filter(Objects::nonNull)
                .map(data -> data.get(SecurityConstants.Session.SESSION_SUBJECT_INFO_KEY))
                .filter(Objects::nonNull)
                .map(user -> gson.fromJson(user, new TypeToken<Subject<Visitor>>() {
                }.getType()));

    }

    public Optional<String> loginType(String token) {
        return parsePayloads(token)
                .flatMap(payloads -> readTokenSession(payloads, normalizeToken(token)))
                .map(session -> session.getAsJsonObject(SecurityConstants.Session.SESSION_ROOT_MAP_NAME_KEY))
                .filter(Objects::nonNull)
                .map(data -> data.get(SecurityConstants.Session.SESSION_USER_LOGIN_TYPE))
                .filter(Objects::nonNull)
                .map(loginType -> gson.fromJson(loginType, String.class));
    }

    /**
     * 标准化 token，兼容 API_KEY 前缀。
     *
     * @param token 原始 token
     * @return 标准 JWT token
     */
    public String normalizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        if (token.startsWith(SecurityConstants.Authentication.API_KEY_PREFIX)) {
            return token.substring(SecurityConstants.Authentication.API_KEY_PREFIX.length());
        }
        return token;
    }

    private Optional<JSONObject> parsePayloads(String token) {
        String normalizedToken = normalizeToken(token);
        if (!isValidJwt(normalizedToken)) {
            return Optional.empty();
        }
        return Optional.ofNullable(JWTUtil.parseToken(normalizedToken).getPayloads());
    }

    private boolean isValidJwt(String token) {
        if (!StringUtils.hasText(token) || StringUtils.countOccurrencesOf(token, ".") != 2) {
            return false;
        }
        try {
            return JWTUtil.verify(token, SecurityConstants.JWT.AUTH_JWT_SECRET_KEY.getBytes(StandardCharsets.UTF_8));
        } catch (JWTException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Optional<com.google.gson.JsonObject> readAccountSession(JSONObject payloads, String normalizedToken) {
        return cacheUtils.withRebel(() ->
                Optional.ofNullable(payloads)
                        .map(payload -> payload.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY))
                        .map(loginType -> cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_VALUE.apply(loginType) + normalizedToken))
                        .map(Object::toString)
                        .map(loginId -> cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_SESSION.apply(payloads.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY)) + loginId))
                        .map(Object::toString)
                        .map(JsonParser::parseString)
                        .map(element -> element.getAsJsonObject()));
    }

    private Optional<com.google.gson.JsonObject> readTokenSession(JSONObject payloads, String normalizedToken) {
        return cacheUtils.withRebel(() ->
                Optional.ofNullable(payloads)
                        .map(payload -> payload.getStr(SecurityConstants.JWT.LOGIN_TYPE_KEY))
                        .map(loginType -> cacheUtils.get(SecurityConstants.Authentication.TOKEN_SPLICING_KEY_TOKEN_SESSION.apply(loginType) + normalizedToken))
                        .map(Object::toString)
                        .map(JsonParser::parseString)
                        .map(element -> element.getAsJsonObject()));
    }


}
