package org.quyq.gwsu.common.security.enums;


import lombok.Getter;

/**
 * @author Quyq
 * @date 2026/4/7
 * @description 登录的账户类型，当平台有多账号系统时用于扩展
 */
@Getter
public enum AccountType {

    MANAGER("管理端"),

    USER("官网端");

    private final String msg;

     AccountType(String msg) {
        this.msg = msg;
    }

    public static AccountType fromString(String type) {
         return AccountType.valueOf(type.toUpperCase());
    }
}
