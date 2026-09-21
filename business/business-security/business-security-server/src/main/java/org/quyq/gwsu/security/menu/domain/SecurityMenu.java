package org.quyq.gwsu.security.menu.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.quyq.gwsu.common.core.domain.BaseDO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.enums.MenuPosition;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;

/**
 * 菜单表
 *
 * @author Quyq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@TableName(value = "security_menu", autoResultMap = true)
@Schema(description = "菜单表")
public class SecurityMenu extends BaseDO {

    public static final String ROOT_MENU_PARENT_ID = "0";

    @TableId(type = IdType.INPUT)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "父菜单ID，NULL表示顶级菜单")
    private String parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：1-目录 2-菜单 3-按钮")
    private Integer menuType;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "路由路径")
    private String path;

    @Schema(description = "是否显示")
    private Boolean visible;

    @Schema(description = "状态：true-正常 false-禁用")
    private Boolean status;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "按钮标识，格式：菜单ID_标识")
    private String buttonKey;

    @Schema(description = "功能描述，用于AI提示词构建")
    private String description;

    @Schema(description = "菜单位置类型")
    private MenuPosition position;

    @Schema(description = "菜单所属类型")
    private MenuOwner owner;

    /**
     * DO 转 VO
     *
     * @return MenuVO
     */
    public MenuVO toVo() {
        MenuVO vo = new MenuVO();
        vo.setId(this.id);
        vo.setParentId(this.parentId);
        vo.setMenuName(this.menuName);
        vo.setMenuType(this.menuType);
        vo.setSort(this.sort);
        vo.setIcon(this.icon);
        vo.setPath(this.path);
        vo.setVisible(this.visible);
        vo.setStatus(this.status);
        vo.setPermission(this.permission);
        vo.setButtonKey(this.buttonKey);
        vo.setDescription(this.description);
        vo.setPosition(this.position);
        vo.setOwner(this.owner);
        vo.copyBaseProperties(this);
        return vo;
    }

}
