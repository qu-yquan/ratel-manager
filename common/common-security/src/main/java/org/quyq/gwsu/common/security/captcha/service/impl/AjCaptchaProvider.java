package org.quyq.gwsu.common.security.captcha.service.impl;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.core.exception.errcode.CommonErrorCode;
import org.quyq.gwsu.common.core.utils.AssertUtils;
import org.quyq.gwsu.common.security.captcha.domain.*;
import org.quyq.gwsu.common.security.captcha.enums.CaptchaType;
import org.quyq.gwsu.common.security.captcha.properties.CaptchaProperties;
import org.quyq.gwsu.common.security.captcha.service.CaptchaProvider;
import org.quyq.gwsu.common.security.captcha.service.CaptchaServiceManager;
import org.quyq.gwsu.common.security.exception.SecurityException;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AJ-Captcha 验证码实现。
 *
 * @author Quyq
 */
@RequiredArgsConstructor
public class AjCaptchaProvider implements CaptchaProvider {

    private static final String SUCCESS_CODE = "0000";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final CaptchaServiceManager captchaServiceManager;
    private final ObjectMapper objectMapper;

    @Override
    public Set<CaptchaType> supportTypes() {
        return Set.of(CaptchaType.BLOCK_PUZZLE, CaptchaType.CLICK_WORD);
    }

    @Override
    public CaptchaGetResponse get(CaptchaGetRequest request) {
        CaptchaProperties properties = properties();
        CaptchaType type = properties.effectiveType(request.type());

        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(type.getCode());
        captchaVO.setClientUid(request.clientUid());

        ResponseModel response = captchaServiceManager.get(type).get(captchaVO);
        assertSuccess(response, CommonErrorCode.E04013);

        Map<String, Object> data = toMap(response.getRepData());
        String token = stringValue(data.get("token"));
        AssertUtils.hasText(token, CommonErrorCode.E04013);

        String captchaId = buildCaptchaId(type, token);
        Map<String, Object> responseData = new HashMap<>(data);
        responseData.put("captchaId", captchaId);
        responseData.put("captchaType", type.getCode());
        return new CaptchaGetResponse(
                type,
                captchaId,
                properties.effectiveExpireSeconds(),
                properties.effectiveVerificationExpireSeconds(),
                responseData
        );
    }

    @Override
    public CaptchaCheckResponse check(CaptchaCheckRequest request) {
        AssertUtils.hasText(request.captchaId(), CommonErrorCode.E04008);
        AssertUtils.hasText(request.captchaCode(), CommonErrorCode.E04009);
        AssertUtils.hasText(request.pointJson(), CommonErrorCode.E04009);

        CaptchaType type = typeFromCaptchaId(request.captchaId());
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType(type.getCode());
        captchaVO.setToken(tokenFromCaptchaId(request.captchaId()));
        captchaVO.setPointJson(request.pointJson());

        ResponseModel response = captchaServiceManager.get(type).check(captchaVO);
        assertSuccess(response, CommonErrorCode.E04011);

        Map<String, Object> data = toMap(response.getRepData());
        String captchaCode = request.captchaCode();

        return new CaptchaCheckResponse(request.captchaId(), captchaCode, data);
    }

    @Override
    public void verify(CaptchaVerifyRequest request) {
        AssertUtils.hasText(request.captchaId(), CommonErrorCode.E04008);
        AssertUtils.hasText(request.captchaCode(), CommonErrorCode.E04009);

        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaVerification(request.captchaCode());
        ResponseModel response = captchaServiceManager.get(typeFromCaptchaId(request.captchaId())).verification(captchaVO);
        assertSuccess(response, CommonErrorCode.E04011);
    }

    private void assertSuccess(ResponseModel response, CommonErrorCode errorCode) {
        if (response == null || !SUCCESS_CODE.equals(response.getRepCode())) {
            throw new SecurityException(errorCode, response == null ? null : response.getRepMsg());
        }
    }

    private Map<String, Object> toMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        return objectMapper.convertValue(value, MAP_TYPE);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String buildCaptchaId(CaptchaType type, String token) {
        return type.name() + ":" + token;
    }

    private CaptchaType typeFromCaptchaId(String captchaId) {
        int separator = captchaId.indexOf(":");
        if (separator <= 0) {
            throw new SecurityException(CommonErrorCode.E04010);
        }
        return CaptchaType.from(captchaId.substring(0, separator));
    }

    private String tokenFromCaptchaId(String captchaId) {
        int separator = captchaId.indexOf(":");
        if (separator <= 0 || separator == captchaId.length() - 1) {
            throw new SecurityException(CommonErrorCode.E04010);
        }
        return captchaId.substring(separator + 1);
    }

    private CaptchaProperties properties() {
        return ConfigInfoUtils.getByObject(CaptchaProperties.CONFIG_KEY, CaptchaProperties.class);
    }
}
