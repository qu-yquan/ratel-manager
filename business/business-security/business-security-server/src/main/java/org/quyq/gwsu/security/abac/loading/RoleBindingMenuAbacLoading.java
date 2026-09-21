package org.quyq.gwsu.security.abac.loading;


import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.service.IAbacAlterationProvider;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.role.domain.SecurityRole;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/4/30
 * @description 角色绑定菜单时，从菜单中加载接口权限
 * 每个时效组对应一个唯一的ABAC表达式，数据按表达式隔离
 */
@Component
@RequiredArgsConstructor
public class RoleBindingMenuAbacLoading implements IAbacAlterationProvider {

    public static final String ROLE_INFO_KEY = "roleInfo";
    public static final String MENUS_INFO_KEY = "menusInfo";
    /**
     * 时效类型：1-永久 2-绝对时间范围 3-周期性
     */
    public static final String VALID_TYPE_KEY = "validType";
    public static final String VALID_START_KEY = "validStart";
    public static final String VALID_END_KEY = "validEnd";
    public static final String CYCLE_TYPE_KEY = "cycleType";
    public static final String CYCLE_VALUE_KEY = "cycleValue";
    public static final String CYCLE_START_TIME_KEY = "cycleStartTime";
    public static final String CYCLE_END_TIME_KEY = "cycleEndTime";

    private final SecurityRoleMenuMapper roleMenuMapper;
    private final SecurityRoleMenuPermissionMapper roleMenuPermissionMapper;

    @Override
    public AbacPerType abacType() {
        return AbacPerType.ROLE_BINDING_MENU;
    }

    private static final String ENV_DATATIME = "r.env.datatime";

    @Override
    public String buildExpression(ExpressionContext context) {
        String baseExpression = "contains(r.sub.roles , '%s')".formatted(context.getValue());

        // 获取时效配置
        ValidType validType = context.getParam(VALID_TYPE_KEY);
        if (validType == null || validType == ValidType.PERMANENT) {
            return baseExpression; // 永久，不追加
        }

        if (validType == ValidType.ABSOLUTE) {
            // 绝对时间范围
            String validStart = context.getParam(VALID_START_KEY);
            String validEnd = context.getParam(VALID_END_KEY);
            return baseExpression + " && timeInRange(%s, \"%s\", \"%s\")".formatted(ENV_DATATIME, validStart, validEnd);
        }

        if (validType == ValidType.CYCLE) {
            // 周期性
            CycleType cycleType = context.getParam(CYCLE_TYPE_KEY);
            String cycleValue = context.getParam(CYCLE_VALUE_KEY);
            String cycleStartTime = context.getParam(CYCLE_START_TIME_KEY);
            String cycleEndTime = context.getParam(CYCLE_END_TIME_KEY);
            String startTime = cycleStartTime != null ? cycleStartTime : "";
            String endTime = cycleEndTime != null ? cycleEndTime : "";

            if (cycleType == CycleType.WEEKLY) {
                return baseExpression + " && cycleWeekly(%s, \"%s\", \"%s\", \"%s\")".formatted(ENV_DATATIME, cycleValue, startTime, endTime);
            } else if (cycleType == CycleType.MONTHLY) {
                return baseExpression + " && cycleMonthly(%s, \"%s\", \"%s\", \"%s\")".formatted(ENV_DATATIME, cycleValue, startTime, endTime);
            }
        }

        return baseExpression;
    }

    @Override
    public void alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        SecurityRole roleInfo = getRoleInfo(context);
        ValidType validType = context.getParam(VALID_TYPE_KEY);

        // 查找当前角色在此时效组下的所有 role_menu 记录
        // 时效组数据按表达式隔离，先删除旧数据再插入新数据
        List<SecurityRoleMenu> existingRoleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleInfo.getId())
                        .eq(SecurityRoleMenu::getValidType, validType != null ? validType : ValidType.PERMANENT));

        // 进一步按时效参数精确匹配（同一个角色可能有多个相同时效类型的组）
        List<SecurityRoleMenu> matchedRoleMenus = existingRoleMenus.stream()
                .filter(rm -> matchesValidGroup(rm, context))
                .toList();

        List<String> matchedRoleMenuIds = matchedRoleMenus.stream()
                .map(SecurityRoleMenu::getId).toList();

        // 删除旧关联数据：role_menu_permission -> role_menu
        if (!matchedRoleMenuIds.isEmpty()) {
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, matchedRoleMenuIds));
        }
        matchedRoleMenus.forEach(rm -> roleMenuMapper.deleteById(rm.getId()));

        // 删除当前表达式下的abac权限（wrapper按表达式隔离，只删对应时效组的）
        wrapper.removeAll();

        List<SecurityMenu> menuInfos = getMenuInfos(context);
        if (CollectionUtils.isEmpty(menuInfos)) {
            return;
        }

        List<SecurityAbacPermission> allPermissions = new ArrayList<>();
        List<SecurityRoleMenuPermission> allRoleMenuPermissions = new ArrayList<>();

        for (SecurityMenu menu : menuInfos) {
            // 构建带时效的角色-菜单关联
            AbacAndMenuRole result = buildAbac(roleInfo.getId(), menu, context);

            // 保存角色菜单关联
            roleMenuMapper.insert(result.roleMenu());

            // 收集abac权限
            if (!CollectionUtils.isEmpty(result.permissions())) {
                allPermissions.addAll(result.permissions());
            }

            // 收集角色菜单权限关联
            if (!CollectionUtils.isEmpty(result.roleMenuPermissions())) {
                allRoleMenuPermissions.addAll(result.roleMenuPermissions());
            }
        }

        // 保存abac权限（通过wrapper写入security_abac_permission表）
        wrapper.addPermissions(allPermissions);

        // 保存角色菜单权限关联
        roleMenuPermissionMapper.insert(allRoleMenuPermissions);
    }

    /**
     * 判断 roleMenu 记录是否与当前 context 的时效配置匹配
     */
    private boolean matchesValidGroup(SecurityRoleMenu rm, ExpressionContext context) {
        ValidType validType = context.getParam(VALID_TYPE_KEY);
        if (validType == null || validType == ValidType.PERMANENT) {
            return rm.getValidType() == ValidType.PERMANENT;
        }
        if (validType == ValidType.ABSOLUTE) {
            if (rm.getValidType() != ValidType.ABSOLUTE) return false;
            DateTimeFormatter dateTFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String validStart = context.getParam(VALID_START_KEY);
            String validEnd = context.getParam(VALID_END_KEY);
            return Objects.equals(rm.getValidStart() != null ? rm.getValidStart().format(dateTFormatter) : "", validStart != null ? validStart : "")
                    && Objects.equals(rm.getValidEnd() != null ? rm.getValidEnd().format(dateTFormatter) : "", validEnd != null ? validEnd : "");
        }
        if (validType == ValidType.CYCLE) {
            if (rm.getValidType() != ValidType.CYCLE) return false;
            CycleType cycleType = context.getParam(CYCLE_TYPE_KEY);
            String cycleValue = context.getParam(CYCLE_VALUE_KEY);
            String cycleStartTime = context.getParam(CYCLE_START_TIME_KEY);
            String cycleEndTime = context.getParam(CYCLE_END_TIME_KEY);
            return rm.getCycleType() == cycleType
                    && Objects.equals(rm.getCycleValue(), cycleValue)
                    && Objects.equals(rm.getCycleStartTime(), cycleStartTime)
                    && Objects.equals(rm.getCycleEndTime(), cycleEndTime);
        }
        return false;
    }

    private AbacAndMenuRole buildAbac(String roleId, SecurityMenu menu, ExpressionContext context) {
        ValidType validType = context.getParam(VALID_TYPE_KEY);

        // 创建 SecurityRoleMenu（一个菜单一条记录，含时效）
        SecurityRoleMenu roleMenu = new SecurityRoleMenu()
                .setId(IdUtil.getSnowflakeNextIdStr())
                .setRoleId(roleId)
                .setMenuId(menu.getId())
                .setValidType(validType != null ? validType : ValidType.PERMANENT);

        // 设置时效字段
        if (validType == ValidType.ABSOLUTE) {
            String validStart = context.getParam(VALID_START_KEY);
            String validEnd = context.getParam(VALID_END_KEY);
            if (validStart != null) {
                roleMenu.setValidStart(java.time.LocalDateTime.parse(validStart,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
            if (validEnd != null) {
                roleMenu.setValidEnd(java.time.LocalDateTime.parse(validEnd,
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            }
        } else if (validType == ValidType.CYCLE) {
            CycleType cycleType = context.getParam(CYCLE_TYPE_KEY);
            String cycleValue = context.getParam(CYCLE_VALUE_KEY);
            String cycleStartTime = context.getParam(CYCLE_START_TIME_KEY);
            String cycleEndTime = context.getParam(CYCLE_END_TIME_KEY);
            roleMenu.setCycleType(cycleType);
            roleMenu.setCycleValue(cycleValue);
            roleMenu.setCycleStartTime(cycleStartTime);
            roleMenu.setCycleEndTime(cycleEndTime);
        }

        String permission = menu.getPermission();
        if (!StringUtils.hasText(permission)) {
            return new AbacAndMenuRole(roleMenu, Collections.emptyList(), Collections.emptyList());
        }

        String[] urlPermissions = permission.split(";");

        List<SecurityAbacPermission> permissions = new ArrayList<>();
        List<SecurityRoleMenuPermission> roleMenuPermissions = new ArrayList<>();

        for (String urlPermission : urlPermissions) {
            String p = urlPermission.trim();
            String[] tmp = p.split(":");

            SecurityAbacPermission abacP = new SecurityAbacPermission()
                    .setId(IdUtil.getSnowflakeNextIdStr())
                    .setEffect(AbacEffect.PERMIT)
                    .setAction(tmp[0])
                    .setResourceType(tmp[1])
                    .setUrlPattern(tmp[2])
                    .setStatus(true);

            permissions.add(abacP);

            SecurityRoleMenuPermission rmp = new SecurityRoleMenuPermission()
                    .setRoleMenuId(roleMenu.getId())
                    .setAbacPermissionId(abacP.getId())
                    .setApiId(ApiEndpointInfo.genId(abacP.getResourceType() ,abacP.getAction() , abacP.getUrlPattern()));
                    ;
            roleMenuPermissions.add(rmp);
        }

        return new AbacAndMenuRole(roleMenu, permissions, roleMenuPermissions);
    }


    /**
     * 删除指定角色下某个时效组的所有数据
     * 包括：security_role_menu_permission -> security_role_menu -> security_abac_permission（通过wrapper）
     *
     * @param context 上下文，包含角色信息和时效配置
     * @param wrapper ABAC权限操作包装器
     */
    public void removeValidGroup(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        SecurityRole roleInfo = getRoleInfo(context);
        ValidType validType = context.getParam(VALID_TYPE_KEY);

        // 查找匹配此时效组的 role_menu 记录
        List<SecurityRoleMenu> allRoleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleInfo.getId()));

        List<SecurityRoleMenu> matchedRoleMenus = allRoleMenus.stream()
                .filter(rm -> matchesValidGroup(rm, context))
                .toList();

        List<String> matchedRoleMenuIds = matchedRoleMenus.stream()
                .map(SecurityRoleMenu::getId).toList();

        if (!matchedRoleMenuIds.isEmpty()) {
            // 删除 role_menu_permission 关联
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, matchedRoleMenuIds));
        }

        // 删除 role_menu 记录
        matchedRoleMenus.forEach(rm -> roleMenuMapper.deleteById(rm.getId()));

        // 删除当前表达式下的abac权限
        wrapper.removeAll();
    }

    /**
     * 删除角色下所有时效组的数据（角色删除时使用）
     *
     * @param roleId 角色ID
     */
    public void removeAllByRoleId(String roleId) {
        List<SecurityRoleMenu> allRoleMenus = roleMenuMapper.selectList(
                new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getRoleId, roleId));

        List<String> allRoleMenuIds = allRoleMenus.stream()
                .map(SecurityRoleMenu::getId).toList();

        if (!allRoleMenuIds.isEmpty()) {
            roleMenuPermissionMapper.delete(new LambdaQueryWrapper<SecurityRoleMenuPermission>()
                    .in(SecurityRoleMenuPermission::getRoleMenuId, allRoleMenuIds));
        }

        roleMenuMapper.delete(new LambdaQueryWrapper<SecurityRoleMenu>()
                .eq(SecurityRoleMenu::getRoleId, roleId));
    }

    /**
     * 获取菜单信息列表
     */
    public List<SecurityMenu> getMenuInfos(ExpressionContext context) {
        return context.getParam(MENUS_INFO_KEY);
    }

    /**
     * 获取角色信息
     */
    public SecurityRole getRoleInfo(ExpressionContext context) {
        return context.getParam(ROLE_INFO_KEY);
    }


    protected record AbacAndMenuRole(SecurityRoleMenu roleMenu,
                                     List<SecurityAbacPermission> permissions,
                                     List<SecurityRoleMenuPermission> roleMenuPermissions) {
    }


}
