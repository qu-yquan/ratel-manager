package org.quyq.gwsu.common.log.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseVO;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.log.enums.ViewOperationSubject;

import java.time.LocalDateTime;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description 操作日志VO对象
 */
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
public class LogOperationVO extends BaseVO {

    @Schema(description = "日志标识")
    private String operId;

    @Schema(description = "认证会话标识")
    private String authorizationId;

    /**
     * 全局日志链路
     */
    @Schema(description = "全局日志链路")
    private String tid;

    /**
     * 父节点
     */
    @Schema(description = "父节点")
    private String parentId;

    /**
     * 服务名
     */
    @Schema(description = "所属服务前缀")
    private String modulePrefix;

    /**
     * 链路来源服务
     */
    @Schema(description = "链路来源服务")
    private String fromApp;

    /**
     * api模块名
     */
    @Schema(description = "api模块名")
    private String apiModule;

    /**
     * 来源菜单
     */
    @Schema(description = "来源菜单")
    private String menuId;

    @Schema(description = "界面操作主体")
    private ViewOperationSubject operSubject;

    /**
     * api接口详情注释
     */
    @Schema(description = "api接口详情注释")
    private String apiDescription;

    /**
     * 方法名
     */
    @Schema(description = "方法名")
    private String method;

    /**
     * 请求路径
     */
    @Schema(description = "请求路径")
    private String requestUrl;

    /**
     * 请求方式
     */
    @Schema(description = "请求方式")
    private String requestMethod;

    /**
     * 请求终端
     */
    @Schema(description = "请求终端")
    private TerminalType terminal;

    /**
     * 请求终端详情
     */
    @Schema(description = "请求终端详情")
    private String terminalDetail;

    /**
     * 操作人
     */
    @Schema(description = "操作人")
    private String operName;

    @Schema(description = "authorizationId缺失时供日志服务查询Redis的原始Token", hidden = true)
    @ToString.Exclude
    private String tokenId;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    private String requestParam;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private String responseData;

    /**
     * 错误消息
     */
    @Schema(description = "错误消息")
    private String errorMsg;

    /**
     * 状态：1-成功；0-失败
     */
    @Schema(title = "状态")
    private Boolean status;

    /**
     * 请求时间
     */
    @Schema(description = "请求时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private LocalDateTime requestTime;

    /**
     * 响应时间
     */
    @Schema(description = "响应时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private LocalDateTime responseTime;

    /**
     * 耗时，ms
     */
    @Schema(description = "耗时，ms")
    private long consumeMill;

}
