package org.quyq.gwsu.security.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.googlecode.aviator.Expression;
import com.googlecode.aviator.exception.CompileExpressionErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.casbin.jcasbin.exception.CasbinMatcherException;
import org.casbin.jcasbin.main.Enforcer;
import org.casbin.jcasbin.model.Model;
import org.casbin.jcasbin.util.Util;
import org.casbin.jcasbin.util.function.CustomFunction;
import org.quyq.gwsu.common.cache.utils.IDGenerationUtils;
import org.quyq.gwsu.common.core.exception.BusinessException;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.abac.domain.ExpressionContext;
import org.quyq.gwsu.security.abac.domain.SecurityAbacPermission;
import org.quyq.gwsu.security.abac.enums.AbacPerType;
import org.quyq.gwsu.security.abac.loading.RoleBindingMenuAbacLoading;
import org.quyq.gwsu.security.abac.mapper.SecurityAbacPermissionMapper;
import org.quyq.gwsu.security.abac.service.PermissionAlterationManager;
import org.quyq.gwsu.security.api.menu.dto.MenuQueryDTO;
import org.quyq.gwsu.security.api.menu.dto.MenuSortDTO;
import org.quyq.gwsu.security.api.menu.enums.MenuOwner;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.quyq.gwsu.security.api.role.enums.CycleType;
import org.quyq.gwsu.security.api.role.enums.ValidType;
import org.quyq.gwsu.security.errcode.SecurityErrorCode;
import org.quyq.gwsu.security.menu.domain.SecurityMenu;
import org.quyq.gwsu.security.menu.mapper.SecurityMenuMapper;
import org.quyq.gwsu.security.menu.service.ISecurityMenuService;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenu;
import org.quyq.gwsu.security.role.domain.SecurityRoleMenuPermission;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuMapper;
import org.quyq.gwsu.security.role.mapper.SecurityRoleMenuPermissionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.quyq.gwsu.security.abac.loading.MenuApiChangeAbacReLoading.*;

/**
 * 菜单服务实现
 *
 * @author Quyq
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMenuServiceImpl extends ServiceImpl<SecurityMenuMapper, SecurityMenu> implements ISecurityMenuService {

    private final SecurityRoleMenuMapper roleMenuMapper;
    private final SecurityUtils securityUtils;
    private final IDGenerationUtils idGenerationUtils;
    private final Model model;
    private final List<CustomFunction> enforcerFunctions;
    private Enforcer casbinEnforcer = null;
    private final SecurityRoleMenuMapper securityRoleMenuMapper;
    private final SecurityRoleMenuPermissionMapper securityRoleMenuPermissionMapper;
    private final SecurityAbacPermissionMapper securityAbacPermissionMapper;
    private final PermissionAlterationManager permissionAlterationManager;

    private Enforcer getEnforcer(){
        if(Objects.isNull(casbinEnforcer)){
            synchronized (SecurityMenuServiceImpl.class){
                if(Objects.isNull(casbinEnforcer)){
                    List<Enforcer> enforcers = SpringUtils.getBeansOfType(Enforcer.class);
                    if(CollectionUtils.isNotEmpty(enforcers)){
                        casbinEnforcer = enforcers.getFirst();
                    }else {
                        casbinEnforcer = new Enforcer(model);
                        if (CollectionUtils.isNotEmpty(enforcerFunctions)) {
                            enforcerFunctions.forEach(function -> casbinEnforcer.addFunction(function.getName(), function));
                        }
                    }
                }
            }
        }


        return casbinEnforcer;
    }

    @Override
    public MenuVO getById(String id) {
        SecurityMenu menu = super.getById(id);
        return menu != null ? menu.toVo() : null;
    }

    @Override
    public List<MenuVO> listTree(MenuQueryDTO query, MenuOwner owner , boolean showButton) {
        LambdaQueryWrapper<SecurityMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityMenu::getDeleted, false);
        wrapper.eq(SecurityMenu::getOwner, owner);

        if (query != null) {
            if (query.getMenuName() != null && !query.getMenuName().isEmpty()) {
                wrapper.like(SecurityMenu::getMenuName, query.getMenuName());
            }
            if (query.getStatus() != null) {
                wrapper.eq(SecurityMenu::getStatus, query.getStatus());
            }
            if (query.getVisible() != null) {
                wrapper.eq(SecurityMenu::getVisible, query.getVisible());
            }
            if (query.getPosition() != null) {
                wrapper.eq(SecurityMenu::getPosition, query.getPosition());
            }
        }

        if(!showButton){
            wrapper.in(SecurityMenu::getMenuType , 1 ,2);
        }

        wrapper.orderByAsc(SecurityMenu::getSort);

        List<SecurityMenu> menus = list(wrapper);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> listTreeBySubjectId(String subjectId, MenuOwner owner) {
        // 查询用户拥有的菜单
        List<SecurityMenu> menus = baseMapper.selectMenusBySubjectId(subjectId, owner);
        return buildMenuTree(menus);
    }

    @Override
    public List<String> listMenuIdsByRoleId(String roleId, MenuOwner owner) {
        LambdaQueryWrapper<SecurityRoleMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityRoleMenu::getRoleId, roleId);

        // 先获取该角色关联的所有菜单ID
        List<SecurityRoleMenu> roleMenus = roleMenuMapper.selectList(wrapper);
        List<String> menuIds = roleMenus.stream()
                .map(SecurityRoleMenu::getMenuId)
                .collect(Collectors.toList());

        if (menuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 再过滤出指定owner的菜单ID
        LambdaQueryWrapper<SecurityMenu> menuWrapper = new LambdaQueryWrapper<>();
        menuWrapper.in(SecurityMenu::getId, menuIds)
                .eq(SecurityMenu::getOwner, owner)
                .eq(SecurityMenu::getDeleted, false);
        List<SecurityMenu> menus = list(menuWrapper);

        return menus.stream()
                .map(SecurityMenu::getId)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public Boolean saveOrUpdateMenu(SecurityMenu menu) {
        if (StringUtils.hasText(menu.getId())) {
            SecurityMenu oldValue = super.getById(menu.getId());
            boolean flag = super.updateById(menu);
            if(apiPermissionChange(menu.getPermission(), oldValue.getPermission())){
                apiPermissionHandler(menu.getId() , menu.getPermission());
            }
            return flag;
        }
        menu.setId(idGenerationUtils.generateNextIdStr(3));
        return super.save(menu);
    }

    /**
     * 刷新角色和abac绑定的接口映射
     * @param menuId
     * @param permission
     */
    private void apiPermissionHandler(String menuId , String permission){
        List<SecurityRoleMenu> roleMenus = securityRoleMenuMapper
                .selectList(new LambdaQueryWrapper<SecurityRoleMenu>()
                        .eq(SecurityRoleMenu::getMenuId, menuId));
        if(CollectionUtils.isEmpty(roleMenus)){
            return;
        }
        List<SecurityRoleMenuPermission> rmpMapping = securityRoleMenuPermissionMapper
                .selectList(new LambdaQueryWrapper<>(SecurityRoleMenuPermission.class)
                        .in(SecurityRoleMenuPermission::getRoleMenuId,
                                roleMenus.stream().map(SecurityRoleMenu::getId).toList()));
        if(CollectionUtils.isEmpty(rmpMapping)){
            return;
        }

        Map<String, List<SecurityAbacPermission>> abacDatas = securityAbacPermissionMapper.selectList(
                new LambdaQueryWrapper<>(SecurityAbacPermission.class)
                        .in(SecurityAbacPermission::getId, rmpMapping.stream().map(SecurityRoleMenuPermission::getAbacPermissionId).toList())
        ).stream().collect(Collectors.groupingBy(SecurityAbacPermission::getAbacId));

        if(CollectionUtils.isEmpty(abacDatas)){
            return;
        }

        abacDatas.forEach((key, value) -> {
            ExpressionContext context = new ExpressionContext();
            context.setValue(key);
            context.putExtraParam(OLD_ABAC_PERMISSION , value);
            context.putExtraParam(NEW_PERMISSION , permission);

            context.putExtraParam(OLD_ROLE_MENU_PERMISSION ,rmpMapping);
            permissionAlterationManager.alterationUrlPermission(AbacPerType.MENU_API_CHANGE , context);
        });


    }

    private boolean apiPermissionChange(String newPermission , String oldPermission){
        if(Objects.isNull(newPermission) || !StringUtils.hasText(oldPermission)){
            return false;
        }

        Set<String> newPs = Stream.of(newPermission.split(";"))
                .map(String::trim)
                .collect(Collectors.toSet());

        Set<String> oldPs = Stream.of(oldPermission.split(";"))
                .map(String::trim)
                .collect(Collectors.toSet());

        return !newPs.equals(oldPs);


    }

    @Override
    public Boolean removeByIds(List<String> ids) {
        // 检查是否有子菜单
        long childCount = count(new LambdaQueryWrapper<SecurityMenu>()
                .in(SecurityMenu::getParentId, ids)
                .eq(SecurityMenu::getDeleted, false));
        if (childCount > 0) {
            throw new BusinessException(SecurityErrorCode.E01006);
        }
        return removeBatchByIds(ids);
    }

    @Override
    public List<MenuVO> listUserRoutes(MenuOwner owner) {
        // 获取当前登录主体
        var subjectOpt = securityUtils.getSubject();
        if (subjectOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Subject<?> subject = subjectOpt.get();

        // 判断是否为超级管理员
        if (subject.isAdmin()) {
            // 返回所有启用的菜单树
            MenuQueryDTO query = new MenuQueryDTO();
            query.setStatus(true);
            return listTree(query, owner , true);
        }

        // 通过角色编码列表获取角色菜单关联记录（含时效字段）
        List<String> roleCodes = subject.getRoles();
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }

        List<SecurityRoleMenu> roleMenus = baseMapper.selectRoleMenusByRoleCodes(roleCodes, owner);
        if (roleMenus.isEmpty()) {
            return new ArrayList<>();
        }

        // 评估时效表达式，过滤出当前有效的菜单ID
        List<String> validMenuIds = filterValidMenuIds(roleMenus, subject);
        if (validMenuIds.isEmpty()) {
            return new ArrayList<>();
        }

        // 查询有效菜单并构建树
        List<SecurityMenu> menus = listByIds(validMenuIds);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> listTreeByRoleCodes(List<String> roleCodes, MenuOwner owner) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        List<SecurityMenu> menus = baseMapper.selectMenusByRoleCodes(roleCodes, owner);
        return buildMenuTree(menus);
    }

    @Override
    public List<MenuVO> listButtonsByParentId(String parentId, MenuOwner owner) {
        LambdaQueryWrapper<SecurityMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SecurityMenu::getDeleted, false)
                .eq(SecurityMenu::getParentId, parentId)
                .eq(SecurityMenu::getMenuType, 3)
                .eq(SecurityMenu::getOwner, owner)
                .orderByAsc(SecurityMenu::getSort);
        return list(wrapper).stream()
                .map(SecurityMenu::toVo)
                .toList();
    }

    @Override
    public Boolean batchSort(List<MenuSortDTO> sortItems) {
        for (MenuSortDTO item : sortItems) {
            SecurityMenu menu = new SecurityMenu();
            menu.setId(item.getId());
            menu.setParentId(item.getParentId());
            menu.setSort(item.getSort());
            updateById(menu);
        }
        return true;
    }

    /**
     * 根据时效表达式过滤有效的菜单ID
     * 复用 RoleBindingMenuAbacLoading.buildExpression 的逻辑生成表达式，
     * 按 Casbin Enforcer 评估表达式判断时效是否有效
     *
     * @param roleMenus 角色菜单关联记录列表
     * @param subject   当前登录主体
     * @return 当前有效的菜单ID列表（去重）
     */
    private List<String> filterValidMenuIds(List<SecurityRoleMenu> roleMenus, Subject<?> subject) {
        // 构建 Aviator 执行参数
        Map<String, Object> parameters = buildExpressionParameters(subject);

        // 按表达式分组，减少编译和评估次数
        Map<String, List<String>> expressionToMenuIds = new HashMap<>();

        for (SecurityRoleMenu rm : roleMenus) {
            String expression = buildExpressionFromRoleMenu(rm);
            expressionToMenuIds.computeIfAbsent(expression, k -> new ArrayList<>()).add(rm.getMenuId());
        }

        // 评估每个表达式，收集有效菜单ID
        List<String> validMenuIds = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : expressionToMenuIds.entrySet()) {
            if (evaluateExpression(entry.getKey(), parameters)) {
                validMenuIds.addAll(entry.getValue());
            }
        }

        return validMenuIds.stream().distinct().toList();
    }

    /**
     * 构建 Aviator 表达式执行参数
     * 对应 Casbin model 中的 r = sub, res_type, act, url, env
     */
    private Map<String, Object> buildExpressionParameters(Subject<?> subject) {
        Map<String, Object> env = new HashMap<>();
        env.put("datatime", LocalDateTime.now());

        Map<String, Object> parameters = HashMap.newHashMap(8);
        parameters.put("r_sub", subject);
        parameters.put("r_env", env);

        return parameters;
    }

    /**
     * 从角色菜单关联记录构建 ABAC 表达式
     * 复用 RoleBindingMenuAbacLoading.buildExpression 的时效逻辑
     */
    private String buildExpressionFromRoleMenu(SecurityRoleMenu rm) {
        // 查找该角色菜单关联记录对应的角色编码
        // 由于 roleMenus 可能来自多个角色，这里需要通过 role_menu 反查 role_code
        // 但为了避免 N+1 查询，直接用 rm 中的时效信息构建表达式
        // 表达式中的角色部分使用通配符，因为分组已经按角色隔离
        // 实际上只需要判断时效条件是否满足，角色匹配已在 SQL 层完成

        ValidType validType = rm.getValidType();
        if (validType == null || validType == ValidType.PERMANENT) {
            return "true"; // 永久有效，无需时效判断
        }

        if (validType == ValidType.ABSOLUTE) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String validStart = rm.getValidStart() != null ? rm.getValidStart().format(formatter) : "";
            String validEnd = rm.getValidEnd() != null ? rm.getValidEnd().format(formatter) : "";
            return "timeInRange(r.env.datatime, \"%s\", \"%s\")".formatted(validStart, validEnd);
        }

        if (validType == ValidType.CYCLE) {
            CycleType cycleType = rm.getCycleType();
            String cycleValue = rm.getCycleValue() != null ? rm.getCycleValue() : "";
            String cycleStartTime = rm.getCycleStartTime() != null ? rm.getCycleStartTime() : "";
            String cycleEndTime = rm.getCycleEndTime() != null ? rm.getCycleEndTime() : "";

            if (cycleType == CycleType.WEEKLY) {
                return "cycleWeekly(r.env.datatime, \"%s\", \"%s\", \"%s\")".formatted(cycleValue, cycleStartTime, cycleEndTime);
            } else if (cycleType == CycleType.MONTHLY) {
                return "cycleMonthly(r.env.datatime, \"%s\", \"%s\", \"%s\")".formatted(cycleValue, cycleStartTime, cycleEndTime);
            }
        }

        return "true";
    }

    /**
     * 评估 ABAC 表达式
     * 逻辑与 FieldEnforcer.abacMatch 保持一致
     */
    private boolean evaluateExpression(String expression, Map<String, Object> parameters) {
        // 永久有效的情况
        if ("true".equals(expression)) {
            return true;
        }

        try {
            String r = replaceTargets(Util.convertInSyntax(expression));
            Expression exp = getEnforcer().getAviatorEval().compile(Util.md5(r), r, false);
            Object result = exp.execute(parameters);

            if (result instanceof Boolean bool) {
                return bool;
            } else if (result instanceof Double || result instanceof Long) {
                return ((Number) result).floatValue() != 0;
            }

            throw new CasbinMatcherException("matcher result should be Boolean, Double or Long");
        } catch (CompileExpressionErrorException e) {
            log.error("菜单时效表达式编译失败: {}", expression, e);
            return false;
        }
    }

    /**
     * 替换表达式中的点号为下划线，使 Aviator 能正确解析
     * 逻辑与 FieldEnforcer.replaceTargets 保持一致
     */
    private String replaceTargets(String exp) {
        if (exp.startsWith("r") || exp.startsWith("p")) {
            exp = exp.replaceFirst("\\.", "_");
        }
        String reg = "([| =)(&<>,+\\-*/!])((r|p)[0-9]*)\\.";
        exp = exp.replaceAll(reg, "$1$2_");
        return exp;
    }

    /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<SecurityMenu> menus) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, List<MenuVO>> groupByParent = menus.stream()
                .map(SecurityMenu::toVo)
                .collect(Collectors.groupingBy(
                        vo -> vo.getParentId() != null ? vo.getParentId() : SecurityMenu.ROOT_MENU_PARENT_ID
                ));

        List<MenuVO> roots = groupByParent.getOrDefault(SecurityMenu.ROOT_MENU_PARENT_ID, new ArrayList<>());

        roots.forEach(root -> setChildren(root, groupByParent));

        return roots;
    }

    /**
     * 递归设置子菜单
     */
    private void setChildren(MenuVO parent, Map<String, List<MenuVO>> groupByParent) {
        List<MenuVO> children = groupByParent.get(parent.getId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            children.forEach(child -> setChildren(child, groupByParent));
        }
    }
}
