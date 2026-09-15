package org.quyq.gwsu.log.operation.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.common.core.enums.TerminalType;
import org.quyq.gwsu.common.log.enums.ViewOperationSubject;
import org.quyq.gwsu.common.log.vo.LogOperationVO;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "log_operation", autoResultMap = true)
@Schema(description = "操作日志表")
public class LogOperation extends BaseDO {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "认证会话标识")
    private String authorizationId;

    @Schema(description = "全局日志链路")
    private String tid;

    @Schema(description = "父节点")
    private String parentId;

    @Schema(description = "所属服务前缀")
    private String modulePrefix;

    @Schema(description = "链路来源服务")
    private String fromApp;

    @Schema(description = "api模块名")
    private String apiModule;

    @Schema(description = "来源菜单")
    private String menuId;

    @Schema(description = "界面操作主体")
    private ViewOperationSubject operSubject;

    @Schema(description = "api接口详情注释")
    private String apiDescription;

    @Schema(description = "方法名")
    private String method;

    @Schema(description = "请求路径")
    private String requestUrl;

    @Schema(description = "请求方式")
    private String requestMethod;

    @Schema(description = "请求终端")
    private TerminalType terminal;

    @Schema(description = "请求终端详情")
    private String terminalDetail;

    @Schema(description = "操作人")
    private String operName;

    @Schema(description = "请求参数")
    private String requestParam;

    @Schema(description = "响应数据")
    private String responseData;

    @Schema(description = "错误消息")
    private String errorMsg;

    @Schema(description = "状态：1-成功 0-失败")
    private Integer status;

    @Schema(description = "请求时间")
    private LocalDateTime requestTime;

    @Schema(description = "响应时间")
    private LocalDateTime responseTime;

    @Schema(description = "耗时，ms")
    private Long consumeMill;

    /**
     * DO 转 VO
     */
    public LogOperationVO toVo() {
        LogOperationVO vo = new LogOperationVO();
        vo.setOperId(this.id);
        vo.setAuthorizationId(this.authorizationId);
        vo.setTid(this.tid);
        vo.setParentId(this.parentId);
        vo.setModulePrefix(this.modulePrefix);
        vo.setFromApp(this.fromApp);
        vo.setApiModule(this.apiModule);
        vo.setMenuId(this.menuId);
        vo.setOperSubject(this.operSubject);
        vo.setApiDescription(this.apiDescription);
        vo.setMethod(this.method);
        vo.setRequestUrl(this.requestUrl);
        vo.setRequestMethod(this.requestMethod);
        vo.setTerminal(this.terminal);
        vo.setTerminalDetail(this.terminalDetail);
        vo.setOperName(this.operName);
        vo.setRequestParam(this.requestParam);
        vo.setResponseData(this.responseData);
        vo.setErrorMsg(this.errorMsg);
        vo.setStatus(this.status != null && this.status == 1);
        vo.setRequestTime(this.requestTime);
        vo.setResponseTime(this.responseTime);
        vo.setConsumeMill(this.consumeMill != null ? this.consumeMill : 0L);
        vo.copyBaseProperties(this);
        return vo;
    }

    /**
     * VO 转 DO
     */
    public static LogOperation toDo(LogOperationVO vo) {
        LogOperation entity = new LogOperation();
        entity.setId(vo.getOperId());
        entity.setAuthorizationId(vo.getAuthorizationId());
        entity.setTid(vo.getTid());
        entity.setParentId(vo.getParentId());
        entity.setModulePrefix(vo.getModulePrefix());
        entity.setFromApp(vo.getFromApp());
        entity.setApiModule(vo.getApiModule());
        entity.setMenuId(vo.getMenuId());
        entity.setOperSubject(vo.getOperSubject());
        entity.setApiDescription(vo.getApiDescription());
        entity.setMethod(vo.getMethod());
        entity.setRequestUrl(vo.getRequestUrl());
        entity.setRequestMethod(vo.getRequestMethod());
        entity.setTerminal(vo.getTerminal());
        entity.setTerminalDetail(vo.getTerminalDetail());
        entity.setOperName(vo.getOperName());
        entity.setRequestParam(vo.getRequestParam());
        entity.setResponseData(vo.getResponseData());
        entity.setErrorMsg(vo.getErrorMsg());
        entity.setStatus(Boolean.TRUE.equals(vo.getStatus()) ? 1 : 0);
        entity.setRequestTime(vo.getRequestTime());
        entity.setResponseTime(vo.getResponseTime());
        entity.setConsumeMill(vo.getConsumeMill());
        return entity;
    }
}
