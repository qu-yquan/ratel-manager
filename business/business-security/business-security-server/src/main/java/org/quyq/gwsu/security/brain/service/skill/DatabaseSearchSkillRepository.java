package org.quyq.gwsu.security.brain.service.skill;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.harness.agent.skill.LazyResourceCapable;
import io.agentscope.harness.agent.skill.SkillResources;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.BusinessFunctionVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DatabaseSearchSkillRepository implements AgentSkillRepository, LazyResourceCapable {

    public static final String SKILL_NAME = "database_search";

    private final Supplier<List<TableModelTableVO>> allTablesSupplier;

    private final Supplier<List<BusinessFunctionVO>> businessFunctionsSupplier;

    private final Function<String, BusinessFunctionDetailVO> businessDetailSupplier;

    private final Supplier<Map<String, Map<String, FieldPermission>>> permissionSupplier;

    private final BooleanSupplier singleDeploySupplier;

    private final AgentSkillRepositoryInfo repositoryInfo;

    public DatabaseSearchSkillRepository(
            String source ,
            Supplier<List<TableModelTableVO>> allTablesSupplier,
            Supplier<List<BusinessFunctionVO>> businessFunctionsSupplier,
            Function<String, BusinessFunctionDetailVO> businessDetailSupplier,
            Supplier<Map<String, Map<String, FieldPermission>>> permissionSupplier,
            BooleanSupplier singleDeploySupplier) {
        this.allTablesSupplier = Objects.requireNonNull(allTablesSupplier, "allTablesSupplier must not be null");
        this.businessFunctionsSupplier = Objects.requireNonNull(businessFunctionsSupplier, "businessFunctionsSupplier must not be null");
        this.businessDetailSupplier = Objects.requireNonNull(businessDetailSupplier, "businessDetailSupplier must not be null");
        this.permissionSupplier = Objects.requireNonNull(permissionSupplier, "permissionSupplier must not be null");
        this.singleDeploySupplier = Objects.requireNonNull(singleDeploySupplier, "singleDeploySupplier must not be null");
        this.repositoryInfo = new AgentSkillRepositoryInfo("dynamic", source, false);
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
        return repositoryInfo.getLocation();
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
        Snapshot snapshot = snapshot();
        if (snapshot.allTables().isEmpty()) {
            return SkillResources.empty();
        }
        return new DatabaseSearchSkillResources(snapshot);
    }

    private AgentSkill buildSkill() {
        Snapshot snapshot = snapshot();
        if (snapshot.allTables().isEmpty()) {
            return AgentSkill.builder()
                    .name(SKILL_NAME)
                    .source(repositoryInfo.getLocation())
                    .description(skillDescription())
                    .skillContent("系统表模型未初始化，该功能不可用，请回复用户，让其`联系管理员在'业务模型管理'中采集所有表模型`")
                    .build();
        }

        return AgentSkill.builder()
                .name(SKILL_NAME)
                .source(repositoryInfo.getLocation())
                .description(skillDescription())
                .skillContent(buildSkillContent(snapshot))
                .build();
    }

    private String skillDescription() {
        return """
                用于查询系统中实时或持久化的业务数据，包括记录明细、数量统计、聚合分析、状态核验和关联查询；仅允许读取数据，不用于修改数据，也不作为制度、流程或操作说明的事实来源。若问题同时需要业务规则和当前数据，可与 knowledge_search 配合使用。生成 SQL 前必须确认业务规则、表结构、字段权限和数据库厂商，并严格遵守同一数据源内关联、禁止 SELECT *、默认限制 10 条和仅 SELECT 等约束。
                """;
    }

    private Snapshot snapshot() {
        List<TableModelTableVO> tables = Optional.ofNullable(allTablesSupplier.get()).orElseGet(List::of);
        List<BusinessFunctionVO> businesses = Optional.ofNullable(businessFunctionsSupplier.get()).orElseGet(List::of);
        Map<String, Map<String, FieldPermission>> permissions = Optional.ofNullable(permissionSupplier.get()).orElseGet(Map::of);
        boolean isSingle = singleDeploySupplier.getAsBoolean();
        return new Snapshot(tables, businesses, permissions, isSingle);
    }

    private String buildSkillContent(Snapshot snapshot) {
        String deployMode = snapshot.isSingle() ? "单应用部署" : "微服务部署";
        String groupCondition = snapshot.isSingle() ? "同一数据源" : "同一服务同一数据源";
        String businessOverview = buildBusinessOverview(snapshot.businessFunctions());

        return """
                # 数据查询技能
                
                ## 核心工作流程（必须严格遵守）
                
                **第一步：识别业务场景并获取理解**
                - 仔细分析用户问题，判断是否可能属于【可用业务功能】中列出的任一场景。
                - **如果可能属于**：优先阅读对应业务的详细文档。文档可能包含业务规则、典型 SQL、关联表与权限说明，但后续仍需结合表模型概览、元数据工具与实际权限综合判断。
                - **如果不属于任何已有业务功能**：进入通用查询模式，参考【数据库表模型概览】和通用 SQL 规范生成查询。
                
                **第二步：获取必要信息**
                - 生成 SQL 之前，需要按需获取两类信息：
                  - **数据库厂商类型**：调用 `GetDatabaseVendor` 工具，用于确定 SQL 语法。
                  - **表字段详情**：调用 `GetTableDetail` 工具，获取字段信息、字段权限与外键关联。
                - 已经通过业务文档和表模型概览确认的信息，只能当作辅助参考；若与工具返回冲突，以工具返回的真实结构为准。
                
                **第三步：生成 SQL 语句**
                - 严格遵循业务文档中的业务规则（若已查阅），但文档与实际结构冲突时以实际结构为准。
                - 禁止使用 `SELECT *`，必须明确列出字段。
                - 默认加上 10 条数据限制（除非用户明确要求更多），并按数据库厂商使用正确语法。
                
                **第四步：执行 SQL**
                - 如果用户需要执行 SQL 并获取结果，可以调用 `ExecuteSql` 工具。
                - 执行前必须确保 SQL 涉及的所有表与字段，都已经在本技能中确认存在且有权限。
                
                ---
                
                ## 可用业务功能
                
                %s
                
                > 如果用户需求命中上述业务，必须优先查阅对应详细文档，再结合实际表结构和权限做判断。
                
                ---
                
                ## 数据库表模型概览
                
                若用户需求**不涉及**任何已有业务功能，则参考 `reference/table_overview.md` 了解可用表及其所属数据源和权限。
                该概览仅包含表级元信息，不包含字段详情和业务规则。字段详情请通过 `GetTableDetail` 工具获取。
                
                ---
                
                ## 部署模式
                
                当前为**%s**：
                - 单应用部署：按**数据源**分组，同一数据源下的表可以在一条 SQL 中关联查询
                - 微服务部署：按**服务名 + 数据源**分组，同一服务同一数据源下的表才可关联
                
                当前分组条件：**%s** 下的表可以关联查询
                
                ---
                
                ## 重要约束
                
                - 同一条 SQL 中只能关联 **%s** 下的表，不同分组不能跨组查询
                - 如用户需求涉及不同分组，须生成多条 SQL 分别执行
                - 仅允许生成 SELECT 语句，禁止任何修改操作
                - 禁止使用 `SELECT *`
                - 无权限的表和字段不得使用
                - 默认添加 10 条数据限制
                - 优先使用业务文档中的业务规则，但文档与实际结构矛盾时以实际为准
                - 执行 SQL 前必须确保所有表和字段在本技能中已确认存在且有权限，否则 `ExecuteSql` 会失败
                """.formatted(businessOverview, deployMode, groupCondition, groupCondition);
    }

    private String buildBusinessOverview(List<BusinessFunctionVO> businessFunctions) {
        StringBuilder busContent = new StringBuilder();
        busContent.append("| 业务名称 | 简介 | 详细文档 |\n");
        busContent.append("|---------|------|---------|\n");

        if (!CollectionUtils.isEmpty(businessFunctions)) {
            for (BusinessFunctionVO business : businessFunctions) {
                String sanitizedName = sanitizeFileName(business.getName());
                busContent.append("| ").append(business.getName())
                        .append(" | ").append(business.getSummary() != null ? business.getSummary() : "-")
                        .append(" | `reference/").append(sanitizedName).append(".md` |\n");
            }
        } else {
            busContent.append("| - | 暂无业务功能配置 | - |\n");
        }

        return busContent.toString();
    }

    private String buildTableOverviewResource(Snapshot snapshot) {
        Map<String, List<TableModelTableVO>> groupedTables = groupTables(snapshot.allTables(), snapshot.isSingle());
        Map<String, String> tableBusinessMap = buildTableBusinessMap(snapshot);
        Map<String, Boolean> tablePermissionMap = buildTablePermissionMap(snapshot.allTables(), snapshot.permissions());

        StringBuilder tables = new StringBuilder();
        for (Map.Entry<String, List<TableModelTableVO>> entry : groupedTables.entrySet()) {
            if (snapshot.isSingle()) {
                tables.append("## 分组：").append(entry.getKey()).append("（数据源）\n\n");
            } else {
                String[] parts = entry.getKey().split(":", 2);
                String modulePrefix = parts[0];
                String dataSource = parts.length > 1 ? parts[1] : "master";
                tables.append("## 分组：").append(modulePrefix).append(":").append(dataSource).append("（服务:数据源）\n\n");
            }

            tables.append("| 表名 | 表注释 | 所属业务 | 有权限 |\n");
            tables.append("|------|--------|---------|--------|\n");
            for (TableModelTableVO table : entry.getValue()) {
                String key = tableKey(table);
                boolean hasPermission = tablePermissionMap.getOrDefault(key, false);
                String businesses = tableBusinessMap.getOrDefault(table.getTableName(), "-");
                tables.append("| ").append(table.getTableName())
                        .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                        .append(" | ").append(businesses)
                        .append(" | ").append(hasPermission ? "是" : "否")
                        .append(" |\n");
            }
            tables.append("\n");
        }

        return """
                # 数据库表模型概览
                
                > 本文件仅提供表级别概览（表名、注释、所属业务、权限）。
                > 如果用户问题匹配某个业务功能，请优先查阅该业务对应的详细文档。
                > 本概览只用于快速了解可用表，字段详情请调用 `GetTableDetail` 工具。
                
                %s
                
                > 注意：必须属于同一个分组的表，才能在一条 SQL 中关联查询。
                """.formatted(tables);
    }

    private Map<String, String> buildBusinessResources(Snapshot snapshot) {
        Map<String, String> resources = new LinkedHashMap<>();
        Map<String, Boolean> tablePermissionMap = buildTablePermissionMap(snapshot.allTables(), snapshot.permissions());
        for (BusinessFunctionVO business : snapshot.businessFunctions()) {
            BusinessFunctionDetailVO detail = businessDetailSupplier.apply(business.getId());
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(business.getName()).append("\n\n");
            sb.append("# 业务简介\n");
            sb.append(business.getSummary() != null ? business.getSummary() : "-").append("\n\n");

            if (business.getDetail() != null && !business.getDetail().isBlank()) {
                sb.append("# 详细介绍\n\n");
                sb.append(business.getDetail()).append("\n\n");
            }

            if (detail != null && !CollectionUtils.isEmpty(detail.getTables())) {
                sb.append("# 关联表模型\n\n");
                sb.append("| 服务 | 数据源 | 表名 | 表注释 | 当前用户权限 |\n");
                sb.append("|------|--------|------|--------|-------------|\n");
                for (TableModelTableVO table : detail.getTables()) {
                    String key = tableKey(table);
                    boolean hasPermission = tablePermissionMap.getOrDefault(key, false);
                    sb.append("| ").append(table.getModulePrefix() != null ? table.getModulePrefix() : "-")
                            .append(" | ").append(table.getDataSource() != null ? table.getDataSource() : "-")
                            .append(" | ").append(table.getTableName())
                            .append(" | ").append(table.getTableComment() != null ? table.getTableComment() : "-")
                            .append(" | ").append(hasPermission ? "✅ 有权限" : "❌ 无权限")
                            .append(" |\n");
                }
                sb.append("\n");
            }

            resources.put("reference/" + sanitizeFileName(business.getName()) + ".md", sb.toString());
        }
        return resources;
    }

    private Map<String, String> buildTableBusinessMap(Snapshot snapshot) {
        Map<String, String> tableBusinessMap = new LinkedHashMap<>();
        for (BusinessFunctionVO business : snapshot.businessFunctions()) {
            BusinessFunctionDetailVO detail = businessDetailSupplier.apply(business.getId());
            if (detail != null && !CollectionUtils.isEmpty(detail.getTables())) {
                for (TableModelTableVO table : detail.getTables()) {
                    tableBusinessMap.merge(
                            table.getTableName(),
                            business.getName(),
                            (existing, newName) -> existing + ", " + newName
                    );
                }
            }
        }
        return tableBusinessMap;
    }

    private Map<String, List<TableModelTableVO>> groupTables(List<TableModelTableVO> allTables, boolean isSingle) {
        if (isSingle) {
            return allTables.stream()
                    .collect(Collectors.groupingBy(
                            table -> Objects.toString(table.getDataSource(), "master"),
                            LinkedHashMap::new,
                            Collectors.toList()));
        }
        return allTables.stream()
                .collect(Collectors.groupingBy(
                        table -> Objects.toString(table.getModulePrefix(), "-") + ":" + Objects.toString(table.getDataSource(), "master"),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    private Map<String, Boolean> buildTablePermissionMap(
            List<TableModelTableVO> allTables,
            Map<String, Map<String, FieldPermission>> mergedPermissions) {
        Map<String, Boolean> tablePermissionMap = new LinkedHashMap<>();
        for (TableModelTableVO table : allTables) {
            tablePermissionMap.put(tableKey(table), mergedPermissions.containsKey(tableKey(table)));
        }
        return tablePermissionMap;
    }

    private String tableKey(TableModelTableVO table) {
        return Objects.toString(table.getModulePrefix(), "-")
                + ":"
                + Objects.toString(table.getDataSource(), "master")
                + ":"
                + Objects.toString(table.getTableName(), "-");
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private record Snapshot(
            List<TableModelTableVO> allTables,
            List<BusinessFunctionVO> businessFunctions,
            Map<String, Map<String, FieldPermission>> permissions,
            boolean isSingle
    ) {
    }

    private final class DatabaseSearchSkillResources implements SkillResources {

        private final Map<String, String> resources;

        private DatabaseSearchSkillResources(Snapshot snapshot) {
            Map<String, String> files = new LinkedHashMap<>();
            if (!snapshot.allTables().isEmpty()) {
                files.put("reference/table_overview.md", buildTableOverviewResource(snapshot));
                files.putAll(buildBusinessResources(snapshot));
            }
            this.resources = Collections.unmodifiableMap(files);
        }

        @Override
        public Optional<String> read(String path) {
            return Optional.ofNullable(resources.get(path));
        }

        @Override
        public Optional<byte[]> readBinary(String path) {
            return Optional.empty();
        }

        @Override
        public List<String> list() {
            return new ArrayList<>(resources.keySet());
        }
    }
}
