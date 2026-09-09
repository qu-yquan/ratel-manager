package org.quyq.gwsu.security.abac.enums;


/**
 * @author Quyq
 * @date 2026/4/15
 * @description  权限类型
 */
 public enum AbacPerType {
    /**
     * 角色绑定菜单
     */
   ROLE_BINDING_MENU ,
    /**
     * API资源变更
     */
    API_RESOURCE ,
    /**
     * 菜单与API绑定关系变更
     */
    MENU_API_CHANGE,
    /** OAuth Scope绑定接口。 */
    OAUTH_SCOPE
 ;

}
