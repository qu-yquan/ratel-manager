package org.quyq.gwsu.security.abac.loading;


import lombok.RequiredArgsConstructor;
import org.quyq.gwsu.common.security.domain.ApiEndpointInfo;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacMapper;
import org.quyq.gwsu.security.abac.service.IAbacAlterationProvider;
import org.quyq.gwsu.security.abac.service.impl.AbacPermissionUrlWrapper;
import org.quyq.gwsu.security.api.abac.enums.AbacEffect;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Quyq
 * @date 2026/5/19
 * @description
 */
@Component
@RequiredArgsConstructor
public class MenuApiChangeAbacReLoading implements IAbacAlterationProvider {

    public static final String OLD_ABAC_PERMISSION = "oldAbacPermission";
    public static final String NEW_PERMISSION = "newPermission";
    public static final String OLD_ROLE_MENU_PERMISSION = "oldRoleMenuPermission";

    private final SecurityAbacMapper securityAbacMapper;
    private final SecurityRoleMenuPermissionMapper securityRoleMenuPermissionMapper;


    @Override
    public void alterationUrlPermission(ExpressionContext context, AbacPermissionUrlWrapper wrapper) {
        String permissionStr = context.getParam(NEW_PERMISSION);

        List<String> oldAbacPermissionIds = ((List<SecurityAbacPermission>) context.getParam(OLD_ABAC_PERMISSION)).stream().map(SecurityAbacPermission::getId).toList();

        wrapper.removeByIds(oldAbacPermissionIds);
        List<String> perArr = Stream.of(permissionStr.split(";"))
                .map(String::trim)
                .toList();


        List<SecurityRoleMenuPermission> oldRmpMap = context.getParam(OLD_ROLE_MENU_PERMISSION);

        //需要删除的rmp
        List<SecurityRoleMenuPermission> needRemove = oldRmpMap.stream()
                .filter(v -> oldAbacPermissionIds.contains(v.getAbacPermissionId())).toList();
        Set<String> roleMenuIds = needRemove.stream().map(SecurityRoleMenuPermission::getRoleMenuId).collect(Collectors.toSet());

        //删除旧关联
        securityRoleMenuPermissionMapper.deleteByIds(needRemove.stream().map(SecurityRoleMenuPermission::getId).collect(Collectors.toSet()));

        //重新赋值
        roleMenuIds.forEach(roleMenuId -> {
            List<SecurityAbacPermission> permissions = perArr.stream().map(v -> {
                String[] tmp = v.split(":");
                SecurityAbacPermission p = new SecurityAbacPermission();
                p.setAction(tmp[0])
                        .setResourceType(tmp[1])
                        .setUrlPattern(tmp[2])
                        .setEffect(AbacEffect.PERMIT)
                        .setStatus(true);
                return p;
            }).toList();
            wrapper.addPermissions(permissions);


            securityRoleMenuPermissionMapper.insert(
                    permissions.stream().map(v -> {
                        SecurityRoleMenuPermission p = new SecurityRoleMenuPermission();
                        p.setRoleMenuId(roleMenuId);
                        p.setAbacPermissionId(v.getId());
                        p.setApiId(ApiEndpointInfo.genId(v.getResourceType(), v.getAction(), v.getUrlPattern()));
                        return p;
                    }).toList()
            );
        });


    }

    @Override
    public AbacPerType abacType() {
        return AbacPerType.MENU_API_CHANGE;
    }

    @Override
    public String buildExpression(ExpressionContext context) {
        return securityAbacMapper.selectById(context.getValue()).getExpression();
    }

}
