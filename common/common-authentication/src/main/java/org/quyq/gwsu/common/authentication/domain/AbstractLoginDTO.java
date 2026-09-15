package org.quyq.gwsu.common.authentication.domain;


import lombok.Data;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.security.enums.VisitorType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description
 */
@Data
public abstract class AbstractLoginDTO {

    /**
     * 登录类型
     */
    private String type;


    /**
     * 登录终端类型
     */
    private TerminalType terminal;

    /**
     * 访问者类型，不传时默认为平台用户
     */
    private VisitorType visitorType = VisitorType.USER;

    /**
     * 验证码ID
     */
    private String captchaId;

    /**
     * 验证码Code
     */
    private String captchaCode;

    /**
     * 额外扩展参数
     */
    @JsonDeserialize(as = LinkedMultiValueMap.class)
    private MultiValueMap<String, String> extraParam;

}
