package org.quyq.gwsu.security.brain.service.tool;

import cn.hutool.core.collection.CollUtil;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.lang3.StringUtils;
import org.quyq.gwsu.common.ai.AgentException;
import org.quyq.gwsu.common.api.utils.FeignUtils;
import org.quyq.gwsu.common.core.constants.CoreConstants;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.core.domain.visitor.Visitor;
import org.quyq.gwsu.common.core.utils.DeployUtils;
import org.quyq.gwsu.common.core.utils.SpringUtils;
import org.quyq.gwsu.common.security.domain.FieldPermission;
import org.quyq.gwsu.common.security.domain.Subject;
import org.quyq.gwsu.common.security.domain.vo.SqlQueryVO;
import org.quyq.gwsu.common.security.service.ISQLExecutionService;
import org.quyq.gwsu.common.security.utils.ConfigInfoUtils;
import org.quyq.gwsu.common.security.utils.DictInfoUtils;
import org.quyq.gwsu.common.security.utils.SecurityUtils;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelColumnVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelDetailVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelForeignKeyVO;
import org.quyq.gwsu.security.api.tablemodel.vo.TableModelTableVO;
import org.quyq.gwsu.security.brain.service.skill.DatabaseSearchSkillRepository;
import org.quyq.gwsu.security.role.service.ISecurityRoleTableModelService;
import org.quyq.gwsu.security.tablemodel.service.ISecurityTableModelTableService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Supplier;

/**
 * 数据库查询工具集
 * 提供给自然语言转SQL智能体使用的工具，包含获取表详情和执行SQL两个工具
 *
 * @author Quyq
 * @date 2026/5/15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSearchTool {

    private final ISecurityTableModelTableService tableModelTableService;

    private final RestClient.Builder restClientBuilder;

    private final ISQLExecutionService sqlExecutionService;

    private final SecurityUtils securityUtils;

    private final ISecurityRoleTableModelService roleTableModelService;

    public Map<String, Map<String, FieldPermission>> getUserTableModelPermission() {
        return roleTableModelService.getUserTableModelPermission();
    }


    /**
     * 获取指定表的详细内容（字段信息），包含当前登录用户对该字段的权限
     *
     * @param tableNames  表名列表
     * @param ds 数据源
     * @return 表详细信息（含字段权限）
     */
    @Tool(name = "GetTableDetail", description = DatabaseSearchSkillRepository.SKILL_NAME + "技能的伴随工具，必须加载该技能才能使用。" +
            """
            获取一个或多个数据表的详细结构信息，包括表注释、字段名、字段类型、长度、是否可空、是否主键、字典枚举值、外键关系，以及当前用户对字段的查询权限。
            在生成 SQL 之前，应先调用此工具确认表结构和可访问字段。
            如果一次查询多个表，所有表必须属于同一个服务(modelPrefix)和同一个数据源(dataSource)；不支持跨服务或跨数据源混合查询。
            """)
    public Mono<String> getTableDetail(
            @ToolParam(name = "modelPrefix", description = "所属模块/服务") String modelPrefix,
            @ToolParam(name = "tableName", description = "表名列表；多个表时必须属于同一服务和同一数据源") List<String> tableNames,
            @ToolParam(name = "dataSource", description = "数据源名称，默认为master") String ds) {
        String dataSource = StringUtils.isBlank(ds) ? "master" : ds;
        return Mono.defer(() ->{
            List<String> normalizedTableNames = Optional.ofNullable(tableNames)
                    .orElseGet(List::of)
                    .stream()
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .toList();
            if (CollectionUtils.isEmpty(normalizedTableNames)) {
                return Mono.just("未提供有效的表名。请传入至少一个表名；如果一次查询多个表，这些表必须属于同一服务和同一数据源。");
            }

            Map<String, TableModelDetailVO> tableDetailMap =
                    tableModelTableService.getTableDetails(modelPrefix, dataSource, normalizedTableNames);

            List<String> missingTableNames = normalizedTableNames.stream()
                    .filter(tableName -> !tableDetailMap.containsKey(tableName))
                    .toList();
            if (tableDetailMap.isEmpty()) {
                return Mono.just("未找到表 " + normalizedTableNames + "（所属服务：" + modelPrefix + "，数据源：" + dataSource + "），请检查表名、所属服务和数据源是否正确。");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("已按同一服务/同一数据源查询表结构。\n");
            sb.append("所属服务: ").append(modelPrefix).append("\n");
            sb.append("数据源: ").append(dataSource).append("\n");
            sb.append("查询表数: ").append(normalizedTableNames.size()).append("\n\n");

            for (String tableName : normalizedTableNames) {
                TableModelDetailVO tableDetail = tableDetailMap.get(tableName);
                if (tableDetail == null) {
                    continue;
                }
                appendTableDetail(sb, modelPrefix, dataSource, tableName, tableDetail);
                sb.append("\n");
            }

            if (!missingTableNames.isEmpty()) {
                sb.append("### 未找到的表\n");
                sb.append("以下表未匹配到表结构信息，请检查表名、所属服务和数据源是否正确: ")
                        .append(String.join(", ", missingTableNames))
                        .append("\n");
            }

            return Mono.just(sb.toString().trim());
        });
    }

    private void appendTableDetail(StringBuilder sb,
                                   String modelPrefix,
                                   String dataSource,
                                   String tableName,
                                   TableModelDetailVO tableDetail) {
        TableModelTableVO tableVO = tableDetail.getTable();
        List<TableModelColumnVO> columns = tableDetail.getColumns();
        List<TableModelForeignKeyVO> foreignKeys = tableDetail.getForeignKeys();
        Map<String, FieldPermission> fieldPermissions = getCurrentUserFieldPermissions(modelPrefix, dataSource, tableName);

        List<String> dictKeys = columns.stream()
                .map(TableModelColumnVO::getDictKey)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
        Map<String, Map<String, String>> dictValueMap = DictInfoUtils.get(dictKeys);

        sb.append("## 表: ").append(tableName).append("（数据源: ").append(dataSource).append("）\n");
        sb.append("表注释: ").append(tableVO.getTableComment() != null ? tableVO.getTableComment() : "无").append("\n");
        sb.append("所属服务: ").append(tableVO.getModulePrefix()).append("\n\n");

        sb.append("### 字段列表\n");
        sb.append("| 字段名 | 类型 | 长度 | 可空 | 主键 | 注释 | 枚举值 | 用户是否有权限 |\n");
        sb.append("|--------|------|------|------|------|------|--------|------------|\n");

        for (TableModelColumnVO column : columns) {
            FieldPermission perm = fieldPermissions.get(column.getColumnName());
            boolean hasPermission = perm == null || perm.show();

            String enumDisplay = "-";
            if (StringUtils.isNotBlank(column.getDictKey())) {
                Map<String, String> values = dictValueMap.get(column.getDictKey());
                if (!CollectionUtils.isEmpty(values)) {
                    enumDisplay = values.entrySet().stream()
                            .map(e -> e.getValue() + "(" + e.getKey() + ")")
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("-");
                }
            }

            sb.append("| ").append(column.getColumnName())
                    .append(" | ").append(column.getColumnType() != null ? column.getColumnType() : "-")
                    .append(" | ").append(column.getColumnLength() != null ? column.getColumnLength() : "-")
                    .append(" | ").append(column.getIsNullable() != null && column.getIsNullable() ? "是" : "否")
                    .append(" | ").append(column.getIsPrimaryKey() != null && column.getIsPrimaryKey() ? "是" : "否")
                    .append(" | ").append(column.getColumnComment() != null ? column.getColumnComment() : "-")
                    .append(" | ").append(enumDisplay)
                    .append(" | ").append(hasPermission ? "是" : "否")
                    .append(" |\n");
        }

        if (!CollectionUtils.isEmpty(foreignKeys)) {
            sb.append("\n### 外键约束\n");
            for (TableModelForeignKeyVO fk : foreignKeys) {
                sb.append("- 约束名: ").append(fk.getConstraintName() != null ? fk.getConstraintName() : "-")
                        .append(", 字段: ").append(fk.getColumnName() != null ? fk.getColumnName() : "-")
                        .append(" -> ").append(fk.getReferencedTableName() != null ? fk.getReferencedTableName() : "-")
                        .append(".").append(fk.getReferencedColumnName() != null ? fk.getReferencedColumnName() : "-");
                if (StringUtils.isNotBlank(fk.getRemark())) {
                    sb.append("  注释：").append(fk.getRemark());
                }
                sb.append("\n");
            }
        }

        List<String> deniedFields = columns.stream()
                .map(TableModelColumnVO::getColumnName)
                .filter(fieldName -> {
                    FieldPermission perm = fieldPermissions.get(fieldName);
                    return perm != null && !perm.show();
                })
                .toList();

        if (!deniedFields.isEmpty()) {
            sb.append("\n> **权限提示**: 以下字段当前用户无查询权限: ").append(String.join(", ", deniedFields))
                    .append("。生成SQL时请勿包含这些字段。\n");
        }
    }


    @Tool(name = "GetDatabaseVendor", description = DatabaseSearchSkillRepository.SKILL_NAME + "技能的伴随工具，必须加载该技能才能使用。" +
            """
            返回当前数据源的数据库厂商名称，如 'MySQL', 'Oracle'
            使用场景：生成SQL之前需要先调用该方法，获取对应的数据库厂商名，根据数据库生成适配的SQL
            """)
    public Mono<String> getDatabaseVendor(
            @ToolParam(name = "modulePrefix", description = "所属服务（模块前缀），如security") String modulePrefix,
            @ToolParam(name = "datasource", description = "数据源") String datasource) {
        return Mono.defer(() ->{
            if (DeployUtils.isSingle()) {
                return Mono.just(sqlExecutionService.getDatabaseName(datasource));
            }
            RestClient restClient = getRestClient(modulePrefix);

            return Mono.just(FeignUtils.data(
                    restClient.get()
                            .uri(CoreConstants.EndPoint.ENDPOINT_DB_NAME + "?datasource=%s".formatted(datasource))
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {
                            })
            ));
        });




    }

    /**
     * 执行SQL查询
     * 入参：所属服务、数据源、SQL语句
     * 步骤：1. 校验SQL中的表和字段当前用户是否都有权限  2. 追加权限过滤条件  3. 执行SQL
     *
     * @param modulePrefix 所属服务（模块前缀）
     * @param ds   数据源名称
     * @param sql          SQL语句（仅限SELECT）
     */
    @Tool(name = "ExecuteSql", description = DatabaseSearchSkillRepository.SKILL_NAME + "技能的伴随工具，必须加载该技能才能使用。" +
            """
            执行只读SQL查询（仅限SELECT语句）。会自动校验当前用户对SQL中涉及的表和字段的权限，并自动追加数据权限过滤条件。不同服务/数据源的表无法在同一条SQL中关联查询，请确保SQL中所有表属于同一服务同一数据源。
            """)
    public Mono<String> executeSql(
            @ToolParam(name = "modulePrefix", description = "所属服务（模块前缀），如security") String modulePrefix,
            @ToolParam(name = "dataSource", description = "数据源名称，默认为master") String ds,
            @ToolParam(name = "sql", description = "要执行的SELECT SQL语句") String sql) {
        String dataSource = StringUtils.isBlank(ds) ? "master" : ds;

        return Mono.defer(() ->{

            Map<String, Map<String, FieldPermission>> userTableModelPermission =
                    roleTableModelService.getUserTableModelPermission();

            // 1. 校验SQL是否为SELECT语句
            String trimmedSql = sql.trim();
            if (!trimmedSql.toUpperCase().startsWith("SELECT")) {
                return Mono.just("错误：仅允许执行SELECT查询语句，禁止执行任何修改操作（INSERT/UPDATE/DELETE等）。");
            }

            // 2. 校验SQL中涉及的表和字段当前用户是否都有权限（同时解析出 表名->列名 映射供脱敏使用）
            SqlParseResult parseResult = parseSqlMeta(userTableModelPermission, modulePrefix, dataSource, sql);
            if (parseResult.error() != null) {
                return Mono.just("权限校验失败: " + parseResult.error());
            }

            // 3. 执行SQL
            SqlQueryVO result = doExecuteSql(modulePrefix, dataSource, sql);
            if (Objects.isNull(result)) {
                return Mono.just("执行失败");
            }
            List<Map<String, Object>> queryResult = result.data();
            String finSql = result.executionSql();
            log.debug("模型执行sql:{}" , finSql);
            List<String> desensitizedFields = Collections.emptyList();

            StringBuilder dataStr = new StringBuilder("查询无数据");

            if (CollUtil.isNotEmpty(queryResult)) {
                Optional<Subject<Visitor>> subject = securityUtils.getSubject();
                //超级管理员跳过脱敏阶段
                if(subject.isEmpty() || !subject.get().isAdmin() ){
                    // 4. 根据字段脱敏配置对结果进行脱敏处理（基于表名->列名映射精确定位字段所属表）
                    desensitizedFields = applyDesensitization(queryResult, userTableModelPermission, modulePrefix, dataSource, parseResult.resultColumnMap());
                }


                dataStr = new StringBuilder();
                // 表头
                List<String> headers = new ArrayList<>(queryResult.getFirst().keySet());
                dataStr.append("| ").append(String.join(" | ", headers)).append(" |\n");
                dataStr.append("| ").append(Collections.nCopies(headers.size(), "---").stream().reduce((a, b) -> a + " | " + b).orElse("")).append(" |\n");
                // 数据行
                for (Map<String, Object> row : queryResult) {
                    dataStr.append("| ");
                    for (String header : headers) {
                        Object val = row.get(header);
                        dataStr.append(val != null ? val.toString() : "NULL").append(" | ");
                    }
                    dataStr.append("\n");
                }
            }

            String msg = """
                ## SQL执行结果
                
                **执行的SQL**:
                ```sql
                %s
                ```
                
                **执行结果**:
                %s
                
                %s
                """.formatted(finSql, dataStr.toString(),
                    desensitizedFields.isEmpty() ? "" : "> **脱敏提示**: 以下字段因权限配置已做脱敏处理: " + String.join(", ", desensitizedFields)
            );

            return Mono.just(msg);

        });



    }


    /**
     * 解析SQL元数据并校验权限
     * <p>
     * 校验逻辑：
     * 1. 解析SQL提取所有表名和别名映射
     * 2. 校验每个表是否属于指定的服务+数据源分组
     * 3. 校验每个表当前用户是否有表级权限（至少一个字段可见）
     * 4. 解析SQL提取所有列引用，校验字段级权限
     * 5. 构建 表名->列名集合 映射供脱敏使用
     *
     * @return 解析结果，包含错误信息和表名->列名映射
     */
    private SqlParseResult parseSqlMeta(Map<String, Map<String, FieldPermission>> userTableModelPermission,
                                        String modulePrefix, String dataSource, String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select select)) {
                return new SqlParseResult("仅支持SELECT语句的权限校验", Map.of(), Map.of());
            }

            // 1. 提取SQL中的表名与别名映射
            Map<String, String> aliasToTable = new LinkedHashMap<>();
            Set<String> tableNames = new LinkedHashSet<>();
            collectTableInfo(select, aliasToTable, tableNames);

            if (tableNames.isEmpty()) {
                return new SqlParseResult(null, Map.of(), Map.of());
            }

            // 校验是否包含 SELECT *
            if (containsSelectAll(select)) {
                return new SqlParseResult("禁止使用 SELECT * 查询，请明确指定需要查询的字段", Map.of(), Map.of());
            }

            // 2. 校验每个表：存在性 + 归属分组 + 表级权限
            for (String tableName : tableNames) {

                String key = modulePrefix + ":" + dataSource + ":" + tableName;
                Map<String, FieldPermission> fieldPerms = userTableModelPermission.get(key);
                if (Objects.isNull(fieldPerms)) {
                    return new SqlParseResult("当前用户无权访问表 [" + tableName + "]", Map.of(), Map.of());
                }
            }

            // 3. 校验字段级权限 + 构建 表名->列名集合 映射
            Set<Column> columns = new LinkedHashSet<>();
            collectColumns(select, columns);

            Map<String, Set<String>> tableColumnMap = new LinkedHashMap<>();
            List<String> deniedFields = new ArrayList<>();

            for (Column col : columns) {
                String colName = col.getColumnName();
                String tableAlias = col.getTable() != null ? col.getTable().getName() : null;
                String resolvedTableName = resolveTableName(tableAlias, aliasToTable, tableNames);
                if (resolvedTableName == null) {
                    continue;
                }

                // 构建表名->列名映射
                tableColumnMap.computeIfAbsent(resolvedTableName, k -> new LinkedHashSet<>()).add(colName);

                // 校验字段权限
                String key = modulePrefix + ":" + dataSource + ":" + resolvedTableName;
                Map<String, FieldPermission> fieldPerms = userTableModelPermission.get(key);
                if (fieldPerms != null && fieldPerms.containsKey(colName)) {
                    FieldPermission perm = fieldPerms.get(colName);
                    if (!perm.show()) {
                        deniedFields.add(resolvedTableName + "." + colName);
                    }
                }
            }

            if (!deniedFields.isEmpty()) {
                return new SqlParseResult("当前用户无权访问以下字段: " + String.join(", ", deniedFields), Map.of(), Map.of());
            }

            // 4. 构建 结果列名 -> 列来源 的映射（供脱敏使用）
            Map<String, ColumnSource> resultColumnMap = buildResultColumnMap(select, aliasToTable, tableNames);

            return new SqlParseResult(null, tableColumnMap, resultColumnMap);
        } catch (JSQLParserException e) {
            log.warn("SQL解析失败，无法校验权限: {}", sql, e);
            return new SqlParseResult("SQL解析失败，无法校验权限", Map.of(), Map.of());
        }
    }

    /**
     * 结果列来源信息，用于脱敏时精确定位字段所属表
     *
     * @param tableName  字段所属的表名
     * @param columnName 字段原始列名
     */
    private record ColumnSource(String tableName, String columnName) {
    }

    /**
     * SQL解析结果
     *
     * @param error           错误信息，null表示校验通过
     * @param tableColumnMap  表名 -> 列名集合 的映射（用于权限校验）
     * @param resultColumnMap 结果列名 -> 列来源 的映射（用于脱敏处理）
     */
    private record SqlParseResult(String error, Map<String, Set<String>> tableColumnMap,
                                  Map<String, ColumnSource> resultColumnMap) {
    }

    // ==================== SQL解析辅助方法 ====================

    /**
     * 从SELECT语句中收集表名和别名映射
     *
     * @param select       SELECT语句
     * @param aliasToTable 别名 -> 真实表名 的映射
     * @param tableNames   收集到的所有真实表名
     */
    private void collectTableInfo(Select select, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (select instanceof PlainSelect plainSelect) {
            collectTableInfoFromPlainSelect(plainSelect, aliasToTable, tableNames);
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                collectTableInfo(body, aliasToTable, tableNames);
            }
        }
    }

    private void collectTableInfoFromPlainSelect(PlainSelect plainSelect, Map<String, String> aliasToTable, Set<String> tableNames) {
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem != null) {
            collectTableInfoFromItem(fromItem, aliasToTable, tableNames);
        }
        List<Join> joins = plainSelect.getJoins();
        if (joins != null) {
            for (Join join : joins) {
                FromItem rightItem = join.getRightItem();
                if (rightItem != null) {
                    collectTableInfoFromItem(rightItem, aliasToTable, tableNames);
                }
            }
        }
    }

    private void collectTableInfoFromItem(FromItem fromItem, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (fromItem instanceof net.sf.jsqlparser.schema.Table table) {
            String tableName = table.getName();
            tableNames.add(tableName);
            if (table.getAlias() != null) {
                aliasToTable.put(table.getAlias().getName(), tableName);
            }
        } else if (fromItem instanceof ParenthesedSelect parenthesedSelect) {
            Select subSelect = parenthesedSelect.getSelect();
            if (subSelect != null) {
                collectTableInfo(subSelect, aliasToTable, tableNames);
            }
        }
    }

    /**
     * 从SELECT语句中收集所有列引用
     */
    private void collectColumns(Select select, Set<Column> columns) {
        if (select instanceof PlainSelect plainSelect) {
            collectColumnsFromPlainSelect(plainSelect, columns);
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                collectColumns(body, columns);
            }
        }
    }

    private void collectColumnsFromPlainSelect(PlainSelect plainSelect, Set<Column> columns) {
        // SELECT 子句
        if (plainSelect.getSelectItems() != null) {
            for (SelectItem<?> item : plainSelect.getSelectItems()) {
                Expression expr = item.getExpression();
                if (expr != null) {
                    collectColumnsFromExpression(expr, columns);
                }
            }
        }

        // WHERE 子句
        if (plainSelect.getWhere() != null) {
            collectColumnsFromExpression(plainSelect.getWhere(), columns);
        }

        // GROUP BY 子句
        if (plainSelect.getGroupBy() != null && plainSelect.getGroupBy().getGroupByExpressionList() != null) {
            for (var expr : plainSelect.getGroupBy().getGroupByExpressionList()) {
                collectColumnsFromExpression((Expression) expr, columns);
            }
        }

        // HAVING 子句
        if (plainSelect.getHaving() != null) {
            collectColumnsFromExpression(plainSelect.getHaving(), columns);
        }

        // ORDER BY 子句
        if (plainSelect.getOrderByElements() != null) {
            for (OrderByElement order : plainSelect.getOrderByElements()) {
                collectColumnsFromExpression(order.getExpression(), columns);
            }
        }

        // JOIN ON 条件
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                Collection<Expression> onExps = join.getOnExpressions();
                if (onExps != null) {
                    for (Expression onExp : onExps) {
                        collectColumnsFromExpression(onExp, columns);
                    }
                }
            }
        }
    }

    /**
     * 递归从表达式中收集所有列引用
     */
    private void collectColumnsFromExpression(Expression expr, Set<Column> columns) {
        if (expr == null) {
            return;
        }

        // 列引用
        if (expr instanceof Column col) {
            columns.add(col);
            return;
        }

        // 二元表达式（AND, OR, 比较运算, 算术运算等）
        if (expr instanceof BinaryExpression binary) {
            collectColumnsFromExpression(binary.getLeftExpression(), columns);
            collectColumnsFromExpression(binary.getRightExpression(), columns);
            return;
        }

        // 函数调用：SUM(col), COUNT(col) 等
        if (expr instanceof Function func) {
            ExpressionList<?> params = func.getParameters();
            if (params != null && !params.isEmpty()) {
                for (var param : params) {
                    collectColumnsFromExpression(param, columns);
                }
            }
            return;
        }


        if (expr instanceof ParenthesedExpressionList<?> paren) {
            if (!paren.isEmpty()) {
                for (var innerExpr : paren) {
                    collectColumnsFromExpression(innerExpr, columns);
                }
            }
            return;
        }

        // CASE WHEN 表达式
        if (expr instanceof CaseExpression caseExpr) {
            if (caseExpr.getSwitchExpression() != null) {
                collectColumnsFromExpression(caseExpr.getSwitchExpression(), columns);
            }
            if (caseExpr.getWhenClauses() != null) {
                for (WhenClause when : caseExpr.getWhenClauses()) {
                    collectColumnsFromExpression(when.getWhenExpression(), columns);
                    collectColumnsFromExpression(when.getThenExpression(), columns);
                }
            }
            if (caseExpr.getElseExpression() != null) {
                collectColumnsFromExpression(caseExpr.getElseExpression(), columns);
            }
            return;
        }

        // BETWEEN 表达式
        if (expr instanceof Between between) {
            collectColumnsFromExpression(between.getLeftExpression(), columns);
            collectColumnsFromExpression(between.getBetweenExpressionStart(), columns);
            collectColumnsFromExpression(between.getBetweenExpressionEnd(), columns);
            return;
        }

        // IN 表达式
        if (expr instanceof InExpression inExpr) {
            collectColumnsFromExpression(inExpr.getLeftExpression(), columns);
            return;
        }

        // IS NULL 表达式
        if (expr instanceof IsNullExpression isNull) {
            collectColumnsFromExpression(isNull.getLeftExpression(), columns);
            return;
        }

        // NOT 表达式
        if (expr instanceof NotExpression not) {
            collectColumnsFromExpression(not.getExpression(), columns);

        }
    }

    /**
     * 解析表别名到真实表名
     *
     * @param alias        表别名，可能为null
     * @param aliasToTable 别名 -> 真实表名 映射
     * @param tableNames   所有表名
     * @return 真实表名，无法解析时返回null
     */
    private String resolveTableName(String alias, Map<String, String> aliasToTable, Set<String> tableNames) {
        if (alias == null || alias.isBlank()) {
            // 无别名修饰，如果SQL中只有一张表则直接返回
            if (tableNames.size() == 1) {
                return tableNames.iterator().next();
            }
            return null;
        }

        // 先查别名映射
        String resolved = aliasToTable.get(alias);
        if (resolved != null) {
            return resolved;
        }

        // 别名可能就是真实表名本身（没有使用AS）
        if (tableNames.contains(alias)) {
            return alias;
        }

        return null;
    }

    /**
     * 检查SELECT语句中是否包含 SELECT * 或 SELECT t.*
     */
    private boolean containsSelectAll(Select select) {
        if (select instanceof PlainSelect plainSelect) {
            List<SelectItem<?>> items = plainSelect.getSelectItems();
            if (items == null) return false;
            for (SelectItem<?> item : items) {
                Expression expr = item.getExpression();
                if (expr instanceof AllColumns || expr instanceof AllTableColumns) {
                    return true;
                }
            }
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                if (containsSelectAll(body)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从SELECT子句构建结果列名到列来源的映射
     * 只处理SELECT子句中的简单列引用，用于脱敏时精确定位字段所属表
     *
     * @param select       SELECT语句
     * @param aliasToTable 别名 -> 真实表名 的映射
     * @param tableNames   所有表名
     * @return 结果列名 -> 列来源 的映射
     */
    private Map<String, ColumnSource> buildResultColumnMap(Select select, Map<String, String> aliasToTable, Set<String> tableNames) {
        Map<String, ColumnSource> result = new LinkedHashMap<>();

        if (select instanceof PlainSelect plainSelect) {
            List<SelectItem<?>> items = plainSelect.getSelectItems();
            if (items == null) return result;

            for (SelectItem<?> item : items) {
                Expression expr = item.getExpression();
                if (expr == null) continue;

                if (expr instanceof Column col) {
                    String colName = col.getColumnName();
                    String tableAlias = col.getTable() != null ? col.getTable().getName() : null;
                    String resolvedTableName = resolveTableName(tableAlias, aliasToTable, tableNames);
                    if (resolvedTableName == null) continue;

                    // 结果列名：有别名用别名，否则用列名
                    String resultColName = item.getAlias() != null ? item.getAlias().getName() : colName;
                    result.put(resultColName, new ColumnSource(resolvedTableName, colName));
                }
            }
        } else if (select instanceof SetOperationList setOp) {
            for (Select body : setOp.getSelects()) {
                result.putAll(buildResultColumnMap(body, aliasToTable, tableNames));
            }
        }

        return result;
    }

    /**
     * 执行SQL的具体实现
     *
     * @param modulePrefix 所属服务
     * @param dataSource   数据源
     * @param sql          最终执行的SQL
     * @return 查询结果列表，每行是一个 字段名->值 的Map
     */
    private SqlQueryVO doExecuteSql(String modulePrefix, String dataSource, String sql) {

        if (DeployUtils.isSingle()) {
            return SpringUtils.getBean(ISQLExecutionService.class).query(dataSource, sql, null);
        }

        RestClient restClient = getRestClient(modulePrefix);

        DistributedSqlQueryVO v = FeignUtils.data(restClient.post()
                .uri(CoreConstants.EndPoint.ENDPOINT_DB_EXECUTION)
                .body(Map.of("datasource", dataSource,
                        "sql", sql))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                }));

        return new SqlQueryVO(v.executionSql, v.transformationData());
    }

    //专门用于解析分布式部署时的结果，使用LinkedHashMap接收
    private record DistributedSqlQueryVO(
            String executionSql,
            List<LinkedHashMap<String, Object>> data
    ) {

        public List<Map<String, Object>> transformationData() {
            if (CollectionUtils.isEmpty(data)) {
                return Collections.emptyList();
            }

            return data.stream()
                    .map(v -> (Map<String, Object>) v)
                    .toList();

        }

    }


    private RestClient getRestClient(String modulePrefix) {
        var serverInfo = DeployUtils.getDistributedServerModuleMapping()
                .get(modulePrefix);
        String serviceName = serverInfo == null ? null : serverInfo.applicationName();
        if (StringUtils.isBlank(serviceName)) {
            throw new AgentException("%s 服务未启动，请联系管理员".formatted(modulePrefix));
        }
        return restClientBuilder.clone()
                .baseUrl("http://%s".formatted(serviceName))
                .build();
    }


    /**
     * 根据字段脱敏配置对查询结果进行脱敏处理
     * 基于SQL解析出的结果列名->列来源映射，精确定位每个结果字段所属的表和原始列名，获取对应的脱敏配置
     *
     * @param queryResult              查询结果
     * @param userTableModelPermission 用户表模型权限
     * @param modulePrefix             所属服务
     * @param dataSource               数据源
     * @param resultColumnMap          SQL解析出的 结果列名 -> 列来源 的映射
     * @return 已脱敏的字段名列表
     */
    private List<String> applyDesensitization(List<Map<String, Object>> queryResult,
                                              Map<String, Map<String, FieldPermission>> userTableModelPermission,
                                              String modulePrefix, String dataSource,
                                              Map<String, ColumnSource> resultColumnMap) {
        if (CollectionUtils.isEmpty(queryResult) || resultColumnMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> desensitizedFields = new ArrayList<>();

        for (Map<String, Object> row : queryResult) {
            for (Map.Entry<String, Object> field : row.entrySet()) {
                String resultColName = field.getKey();
                ColumnSource source = resultColumnMap.get(resultColName);
                if (source == null) {
                    continue;
                }

                // 根据来源表和原始列名精确查找脱敏配置
                String key = modulePrefix + ":" + dataSource + ":" + source.tableName();
                Map<String, FieldPermission> tablePerms = userTableModelPermission.get(key);
                if (tablePerms == null) {
                    continue;
                }

                FieldPermission perm = tablePerms.get(source.columnName());
                if (perm == null || !perm.desensitize()) {
                    continue;
                }

                Object value = field.getValue();
                if (value == null) {
                    continue;
                }

                String desensitized = desensitize(value.toString(), perm);
                field.setValue(desensitized);

                if (!desensitizedFields.contains(resultColName)) {
                    desensitizedFields.add(resultColName);
                }
            }
        }

        return desensitizedFields;
    }

    /**
     * 根据脱敏策略对字符串值进行脱敏
     * 委托给 SensitiveStrategy.getConverter().apply() 处理，CUSTOM 策略调用 customConverter
     *
     * @param value 原始值
     * @param perm  字段权限配置（含脱敏策略）
     * @return 脱敏后的值
     */
    private String desensitize(String value, FieldPermission perm) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (perm.strategy() == org.quyq.gwsu.common.security.annotation.SensitiveStrategy.CUSTOM) {
            return org.quyq.gwsu.common.security.annotation.SensitiveStrategy.customConverter(
                    value,
                    perm.symbol() != null ? perm.symbol() : "*",
                    perm.prefixNoMaskLen() != null ? perm.prefixNoMaskLen() : 0,
                    perm.suffixNoMaskLen() != null ? perm.suffixNoMaskLen() : 0);
        }

        return perm.strategy().getConverter().apply(value);
    }

    /**
     * 获取当前用户对指定表的字段权限
     */
    private Map<String, FieldPermission> getCurrentUserFieldPermissions(
            String modulePrefix, String dataSource, String tableName) {
        Map<String, Map<String, FieldPermission>> mergedPermissions =
                roleTableModelService.getUserTableModelPermission();

        String key = modulePrefix + ":" + dataSource + ":" + tableName;
        return mergedPermissions.getOrDefault(key, Map.of());
    }



}
