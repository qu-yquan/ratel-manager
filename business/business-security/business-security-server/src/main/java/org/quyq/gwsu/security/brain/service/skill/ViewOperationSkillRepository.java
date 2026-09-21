package org.quyq.gwsu.security.brain.service.skill;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.harness.agent.skill.LazyResourceCapable;
import io.agentscope.harness.agent.skill.SkillResources;
import org.quyq.gwsu.security.api.menu.vo.MenuVO;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ViewOperationSkillRepository implements AgentSkillRepository, LazyResourceCapable {

    public static final String SKILL_NAME = "system_view_operation";

    public static final String SKILL_DESCRIPTION = "当需要操作用户可视化界面时，加载此技能查看当前用户可访问的界面，并按需读取页面按钮说明";

    private static final String PAGE_RESOURCE_PREFIX = "references/pages/";

    private static final String EMPTY_MENU_CONTENT = "当前用户没有任何菜单权限。";

    private final String source;

    private final Supplier<String> usernameSupplier;

    private final Supplier<List<MenuVO>> menuSupplier;

    private final AgentSkillRepositoryInfo repositoryInfo;

    public ViewOperationSkillRepository(String source, Supplier<String> usernameSupplier, Supplier<List<MenuVO>> menuSupplier) {
        this.source = StringUtils.hasText(source) ? source : "central-brain";
        this.usernameSupplier = usernameSupplier != null ? usernameSupplier : () -> null;
        this.menuSupplier = Objects.requireNonNull(menuSupplier, "menuSupplier must not be null");
        this.repositoryInfo = new AgentSkillRepositoryInfo("dynamic", this.source, false);
    }

    @Override
    public AgentSkill getSkill(String skillId) {
        AgentSkill skill = buildSkill();
        if (Objects.equals(skill.getSkillId(), skillId) || Objects.equals(skill.getName(), skillId)) {
            return skill;
        }
        return null;
    }

    @Override
    public List<String> getAllSkillNames() {
        return List.of(SKILL_NAME);
    }

    @Override
    public List<AgentSkill> getAllSkills() {
        return List.of(buildSkill());
    }

    @Override
    public boolean save(List<AgentSkill> skills, boolean overwrite) {
        return false;
    }

    @Override
    public boolean delete(String skillId) {
        return false;
    }

    @Override
    public boolean skillExists(String skillId) {
        return getSkill(skillId) != null;
    }

    @Override
    public AgentSkillRepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setWriteable(boolean writeable) {
    }

    @Override
    public boolean isWriteable() {
        return false;
    }

    @Override
    public SkillResources resourcesFor(String skillName, RuntimeContext runtimeContext) {
        if (!Objects.equals(SKILL_NAME, skillName)) {
            return SkillResources.empty();
        }
        return new UserMenuSkillResources(snapshotMenus());
    }

    private AgentSkill buildSkill() {
        List<MenuVO> menus = snapshotMenus();
        String username = usernameSupplier.get();

        return AgentSkill.builder()
                .name(SKILL_NAME)
                .source(source)
                .description(SKILL_DESCRIPTION)
                .skillContent(buildSkillContent(username, menus))
                .build();
    }

    private List<MenuVO> snapshotMenus() {
        return Optional.ofNullable(menuSupplier.get()).orElseGet(List::of);
    }

    private String buildSkillContent(String username, List<MenuVO> menuTree) {
        if (menuTree.isEmpty()) {
            return EMPTY_MENU_CONTENT;
        }

        String menuSections = menuTree.stream()
                .map(menu -> buildMenuSection(menu, 2))
                .collect(Collectors.joining("\n"));

        String currentUser = StringUtils.hasText(username) ? "\n当前登录用户：`" + username + "`\n" : "\n";

        return """
                # 系统操作安全助手 Skill
                
                ## 核心行为准则
                
                ### 准则1：永不编造
                - 路由地址必须使用下方菜单列表中的值，严禁自行编造或推测
                
                ### 准则2：缺失必问
                - 执行新增、编辑前，检查用户是否提供了所有必要字段
                - 如有缺失，逐一询问，不得跳过、不得猜测、不得使用默认值
                
                ### 准则3：操作必确认
                - 任何修改性操作在执行前，必须获得用户的明确确认
                
                
                ## 当前用户
                %s
                ## 可访问界面总览
                
                下列内容只包含界面、目录与导航信息，不包含按钮明细。
                当你准备操作某个具体页面时，必须继续加载该页面对应的说明文件，读取按钮、权限与注意事项。
                如果任务涉及真实界面点击、输入、切换、提交等交互，还必须先调用 `reset_equipped_tools`，加载界面操作所需的工具组；否则只能停留在阅读说明和整理操作方案阶段，不能假装自己已经具备界面操作能力。
                
                %s
                
                ## 字段说明
                
                | 字段 | 含义 |
                |------|------|
                | 路由 | 前端视图层界面跳转地址 |
                | 位置 | 菜单在视图层的展示位置 |
                | 接口权限 | 菜单对应的后端接口权限标识 |
                | 页面说明文件（按钮权限） | 当前页面按钮、权限和操作说明的单独文档，按需加载 |
                
                ## 操作流程
                
                1. 先从界面总览中定位到目标页面，确认用户拥有访问权限。
                2. 如果接下来要操作真实界面，先调用 `reset_equipped_tools` 加载界面操作工具组；只有工具组加载完成后，才可以执行导航、点击、输入、提交等动作。
                3. 使用该页面的路由地址导航，不得编造路由。
                4. 需要执行页面内操作时，先加载该页面的说明文件，再决定点击哪个按钮。
                5. 新增、编辑、删除前，按规范向用户补齐缺失信息并做最终确认。
                
                ## 禁止行为清单
                
                - ❌ 在未加载页面说明文件前，猜测页面上有哪些按钮
                - ❌ 在未调用 `reset_equipped_tools` 加载界面操作工具组前，假装自己可以直接操作页面
                - ❌ 猜测或编造缺失字段的值
                - ❌ 跳过最终确认步骤直接操作
                - ❌ 编造不在菜单列表中的路由地址
                """.formatted(currentUser.strip(), menuSections);
    }

    private String buildMenuSection(MenuVO menu, int headingLevel) {
        if (menu == null || Integer.valueOf(3).equals(menu.getMenuType())) {
            return "";
        }

        String heading = "#".repeat(headingLevel);
        int menuType = Optional.ofNullable(menu.getMenuType()).orElse(0);
        String typeLabel = switch (menuType) {
            case 1 -> "目录";
            case 2 -> "菜单";
            default -> "未知";
        };

        StringBuilder sb = new StringBuilder();
        sb.append(heading).append(" ").append(menu.getMenuName())
                .append(" `").append(typeLabel).append("`\n\n");

        sb.append("| 属性 | 值 |\n|------|----|\n");
        sb.append("| 类型 | ").append(typeLabel).append(" |\n");
        if (menuType == 2 && StringUtils.hasText(menu.getPath())) {
            sb.append("| 路由 | `").append(menu.getPath()).append("` |\n");
            sb.append("| 页面说明文件 | `").append(buildPageResourcePath(menu)).append("` |\n");
        }
        if (menu.getPosition() != null) {
            sb.append("| 位置 | ").append(menu.getPosition().getDescription()).append(" |\n");
        }
        if (StringUtils.hasText(menu.getPermission())) {
            sb.append("| 接口权限 | `").append(menu.getPermission()).append("` |\n");
        }
        sb.append("\n");

        if (StringUtils.hasText(menu.getDescription())) {
            sb.append(formatDescription(menu.getDescription())).append("\n\n");
        }

        List<MenuVO> subMenus = Optional.ofNullable(menu.getChildren())
                .orElseGet(List::of)
                .stream()
                .filter(child -> !Integer.valueOf(3).equals(child.getMenuType()))
                .toList();

        for (MenuVO child : subMenus) {
            sb.append(buildMenuSection(child, headingLevel + 1));
        }

        return sb.toString();
    }

    private String buildPageMarkdown(MenuVO menu) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(menu.getMenuName()).append("\n\n");
        sb.append("## 界面信息\n\n");
        sb.append("| 属性 | 值 |\n|------|----|\n");
        if (StringUtils.hasText(menu.getPath())) {
            sb.append("| 路由 | `").append(menu.getPath()).append("` |\n");
        }
        if (menu.getPosition() != null) {
            sb.append("| 位置 | ").append(menu.getPosition().getDescription()).append(" |\n");
        }
        if (StringUtils.hasText(menu.getPermission())) {
            sb.append("| 接口权限 | `").append(menu.getPermission()).append("` |\n");
        }
        sb.append("| 页面资源路径 | `").append(buildPageResourcePath(menu)).append("` |\n\n");

        if (StringUtils.hasText(menu.getDescription())) {
            sb.append("## 功能说明\n\n");
            sb.append(formatDescription(menu.getDescription())).append("\n\n");
        }

        sb.append("## 操作按钮\n\n");
        List<MenuVO> buttons = Optional.ofNullable(menu.getChildren())
                .orElseGet(List::of)
                .stream()
                .filter(child -> Integer.valueOf(3).equals(child.getMenuType()))
                .toList();

        if (buttons.isEmpty()) {
            sb.append("当前界面没有可执行按钮，或当前用户没有任何按钮权限。\n");
            return sb.toString();
        }

        sb.append("| 按钮 | 标识 | 接口权限 | 说明 |\n");
        sb.append("|------|------|----------|------|\n");
        for (MenuVO button : buttons) {
            String description = StringUtils.hasText(button.getDescription())
                    ? button.getDescription().replace("|", "\\|").replace("\n", " ")
                    : "-";
            String permission = StringUtils.hasText(button.getPermission()) ? "`" + button.getPermission() + "`" : "-";
            String buttonKey = StringUtils.hasText(button.getButtonKey()) ? "`" + button.getButtonKey() + "`" : "-";

            sb.append("| ").append(button.getMenuName()).append(" | ")
                    .append(buttonKey).append(" | ")
                    .append(permission).append(" | ")
                    .append(description).append(" |\n");
        }
        sb.append("\n");
        sb.append("执行该页面操作前，只能使用上表中已经明确列出的按钮与权限信息。\n");
        return sb.toString();
    }

    private String formatDescription(String description) {
        boolean hasMarkdownSyntax = description.contains("#")
                || description.contains("```")
                || description.contains("- ")
                || description.contains("* ")
                || description.contains("| ")
                || description.contains("> ")
                || description.contains("1. ");

        if (hasMarkdownSyntax) {
            return """
                    <details>
                    <summary>功能说明</summary>
                    
                    %s
                    
                    </details>""".formatted(description);
        }

        return "> " + description.lines().map(line -> "> " + line).collect(Collectors.joining("\n"));
    }

    private String buildPageResourcePath(MenuVO menu) {
        String slug = Optional.ofNullable(menu.getPath())
                .filter(StringUtils::hasText)
                .map(path -> path.replace('\\', '/'))
                .map(path -> path.substring(path.lastIndexOf('/') + 1))
                .filter(StringUtils::hasText)
                .orElseGet(() -> Optional.ofNullable(menu.getMenuName()).orElse("page"));

        slug = sanitizeFileSegment(slug);
        String id = sanitizeFileSegment(Optional.ofNullable(menu.getId()).orElse("menu"));
        return PAGE_RESOURCE_PREFIX + slug + "__" + id + ".md";
    }

    private String sanitizeFileSegment(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        return StringUtils.hasText(sanitized) ? sanitized : "page";
    }

    private final class UserMenuSkillResources implements SkillResources {

        private final Map<String, MenuVO> pageResourceMap;

        private UserMenuSkillResources(List<MenuVO> menuTree) {
            Map<String, MenuVO> pages = new LinkedHashMap<>();
            collectPages(menuTree, pages);
            this.pageResourceMap = Collections.unmodifiableMap(pages);
        }

        @Override
        public Optional<String> read(String path) {
            MenuVO page = pageResourceMap.get(path);
            return page == null ? Optional.empty() : Optional.of(buildPageMarkdown(page));
        }

        @Override
        public Optional<byte[]> readBinary(String path) {
            return Optional.empty();
        }

        @Override
        public List<String> list() {
            return new ArrayList<>(pageResourceMap.keySet());
        }

        private void collectPages(List<MenuVO> menuTree, Map<String, MenuVO> pages) {
            for (MenuVO menu : menuTree) {
                if (menu == null) {
                    continue;
                }
                if (Integer.valueOf(2).equals(menu.getMenuType())) {
                    pages.put(buildPageResourcePath(menu), menu);
                }
                collectPages(Optional.ofNullable(menu.getChildren()).orElseGet(List::of), pages);
            }
        }
    }
}
