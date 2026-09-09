package org.quyq.gwsu.security.errcode;


import org.quyq.gwsu.common.core.constants.ErrorCodeConstants;
import org.quyq.gwsu.common.core.domain.ReturnCode;
import org.quyq.gwsu.common.core.exception.errcode.ErrorCodeMeta;

/**
 * 安全模块错误码
 *
 * @author Quyq
 */
@ErrorCodeMeta(moduleCode = ErrorCodeConstants.SECURITY_ERROR_CODE_MODULE, notes = "安全模块错误码")
public enum SecurityErrorCode implements ReturnCode {

    E01001("无效的菜单位置类型"),
    E01002("无效的菜单所属类型"),
    E01003("菜单名称不能为空"),
    E01004("按钮标识不能为空"),
    E01005("功能描述不能为空"),


    E02001("角色不存在"),
    E02002("角色编码已存在"),
    E02003("系统角色不可删除"),
    E02004("角色已禁用"),
    E02005("时效配置无效"),
    E02006("该时效分组已存在，请编辑已有配置"),

    E03001("表名称不能为空"),
    E03002("数据源不能为空"),
    E03003("角色ID不能为空"),
    E03004("所属服务（模块）不能为空") ,

    E04001("所属服务不能为空") ,
    E04002("表名不能为空"),

    E05001("配置键已存在"),
    E05002("系统配置不可删除"),
    E05003("配置不存在"),
    E05004("字典键已存在"),
    E05005("系统字典不可删除"),
    E05006("字典不存在"),
    E05007("字典值已经存在，不可重复添加"),
    E06001("业务名称不能为空"),
    E06002("业务简介不能为空"),
    E06003("详细介绍不能为空"),
    E06004("业务名称已存在"),

    E07007("钉钉Client ID，未配置，初始化失败"),
    E07008("钉钉Client Secret 未配置,初始化失败"),
    E07009("未启用钉钉远程操作功能"),
    E07010("钉钉用户unionId获取失败"),
    E07011("ai输出卡片模板ID不能为空"),
    E07012("远程操作必填配置项不能为空"),
    E07013("会话正在运行中，请稍后尝试"),

    E08001("OAuth应用名称不能为空"),
    E08002("OAuth客户端类型不能为空"),
    E08003("OAuth授权模式不能为空"),
    E08004("OAuth授权范围不能为空"),
    E08005("客户端凭证模式只允许机密服务端应用启用"),
    E08006("授权码模式必须配置重定向URI"),
    E08007("OAuth应用不存在"),
    E08008("只有机密服务端应用允许重置客户端密钥"),
    E08009("OAuth客户端认证方式不能为空"),
    E08010("公共客户端必须启用PKCE"),
    E08011("机密服务端应用必须启用客户端密钥认证方式"),
    E08012("OAuth客户端ID已存在"),
    E08101("Scope编码格式不正确"),
    E08102("Scope编码已存在"),
    E08103("Scope不存在"),
    E08104("Scope名称不能为空"),
    E08105("Scope账号体系不能为空"),
    E08106("Scope编码和账号体系创建后不可修改"),
    E08107("Scope绑定了不存在的接口资源"),
    E08108("客户端只能配置相同账号体系且已启用的Scope"),
    E08109("该Scope编码为系统保留编码"),

    ;

    private final String msg;

    SecurityErrorCode(String msg) {
        this.msg = msg;
    }

    @Override
    public String msg() {
        return msg;
    }
}
