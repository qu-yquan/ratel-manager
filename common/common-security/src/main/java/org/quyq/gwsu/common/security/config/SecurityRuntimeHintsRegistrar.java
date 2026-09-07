package org.quyq.gwsu.common.security.config;

import org.jspecify.annotations.Nullable;
import org.quyq.gwsu.common.core.domain.visitor.ClientInfo;
import org.quyq.gwsu.common.core.domain.visitor.UserInfo;
import org.quyq.gwsu.common.security.api.oauth.vo.OAuthClientInfoVO;
import org.quyq.gwsu.common.security.annotation.TableModelField;
import org.quyq.gwsu.common.security.annotation.TableModelPermission;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Security 模块运行时提示注册器，用于 AOT 编译
 * <p>
 * 注册 VisitorDeserializer 中反射访问的类，以及
 * SQL 数据权限过滤中反射遍历的 JSQLParser Expression 类
 *
 * @author Quyq
 * @date 2026/4/12
 */
public class SecurityRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
        // 注册 UserInfo.DefaultUserInfo 及其继承链的所有字段访问
        registerClassWithInheritance(hints, UserInfo.DefaultUserInfo.class);

        // 注册 ClientInfo.DefaultClientInfo 及其继承链的所有字段访问
        registerClassWithInheritance(hints, ClientInfo.DefaultClientInfo.class);

        // 注册 OAuth 客户端主体及其继承链，支持 Visitor 多态反序列化
        registerClassWithInheritance(hints, OAuthClientInfoVO.class);

        // 注册表模型权限注解相关类的反射提示
        hints.reflection().registerType(TableModelPermission.class, MemberCategory.ACCESS_DECLARED_FIELDS);
        hints.reflection().registerType(TableModelField.class, MemberCategory.ACCESS_DECLARED_FIELDS);

        // 注册 SQL 数据权限过滤中反射遍历的 JSQLParser Expression 类
        // processExpressionByReflection 方法会通过反射访问这些类的 Expression 类型字段
        registerJsqlParserExpressionHints(hints);
    }

    /**
     * 注册类及其所有父类的反射提示
     *
     * @param hints 运行时提示
     * @param clazz 目标类
     */
    private void registerClassWithInheritance(RuntimeHints hints, Class<?> clazz) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            // 注册类的字段访问，使用 ACCESS_DECLARED_FIELDS 替代已弃用的 DECLARED_FIELDS
            hints.reflection()
                    .registerType(currentClass, MemberCategory.ACCESS_DECLARED_FIELDS);
            currentClass = currentClass.getSuperclass();
        }
    }

    /**
     * 注册 JSQLParser Expression 子类的反射提示
     * <p>
     * 数据权限过滤中的 processExpressionByReflection 方法通过反射遍历
     * Expression 子类中类型为 Expression 的字段，以发现嵌套的子查询。
     * 这些类需要在 AOT 编译时注册，否则 Native 镜像中反射访问会失败。
     * <p>
     * 已显式处理的类型（ParenthesedSelect、Function、CaseExpression、BinaryExpression）
     * 不需要注册，因为它们不走反射路径。这里注册的是可能通过反射兜底遍历到的类型。
     */
    private void registerJsqlParserExpressionHints(RuntimeHints hints) {
        // ===== net.sf.jsqlparser.expression 包下的类 =====
        Class<?>[] expressionClasses = {
                // 含 Expression 字段的复杂表达式
                net.sf.jsqlparser.expression.AnalyticExpression.class,
                net.sf.jsqlparser.expression.AnyComparisonExpression.class,
                net.sf.jsqlparser.expression.ArrayConstructor.class,
                net.sf.jsqlparser.expression.ArrayExpression.class,
                net.sf.jsqlparser.expression.CastExpression.class,
                net.sf.jsqlparser.expression.CollateExpression.class,
                net.sf.jsqlparser.expression.ConnectByPriorOperator.class,
                net.sf.jsqlparser.expression.ConnectByRootOperator.class,
                net.sf.jsqlparser.expression.DateTimeLiteralExpression.class,
                net.sf.jsqlparser.expression.ExtractExpression.class,
                net.sf.jsqlparser.expression.FilterOverImpl.class,
                net.sf.jsqlparser.expression.IntervalExpression.class,
                net.sf.jsqlparser.expression.Inverse.class,
                net.sf.jsqlparser.expression.JdbcNamedParameter.class,
                net.sf.jsqlparser.expression.JdbcParameter.class,
                net.sf.jsqlparser.expression.JsonAggregateFunction.class,
                net.sf.jsqlparser.expression.JsonExpression.class,
                net.sf.jsqlparser.expression.JsonFunction.class,
                net.sf.jsqlparser.expression.JsonFunctionExpression.class,
                net.sf.jsqlparser.expression.KeepExpression.class,
                net.sf.jsqlparser.expression.LambdaExpression.class,
                net.sf.jsqlparser.expression.MySQLGroupConcat.class,
                net.sf.jsqlparser.expression.NotExpression.class,
                net.sf.jsqlparser.expression.NumericBind.class,
                net.sf.jsqlparser.expression.OracleHierarchicalExpression.class,
                net.sf.jsqlparser.expression.OracleNamedFunctionParameter.class,
                net.sf.jsqlparser.expression.OverlapsCondition.class,
                net.sf.jsqlparser.expression.RangeExpression.class,
                net.sf.jsqlparser.expression.RowConstructor.class,
                net.sf.jsqlparser.expression.RowGetExpression.class,
                net.sf.jsqlparser.expression.SignedExpression.class,
                net.sf.jsqlparser.expression.StructType.class,
                net.sf.jsqlparser.expression.TimeKeyExpression.class,
                net.sf.jsqlparser.expression.TimezoneExpression.class,
                net.sf.jsqlparser.expression.TranscodingFunction.class,
                net.sf.jsqlparser.expression.TrimFunction.class,
                net.sf.jsqlparser.expression.VariableAssignment.class,
                net.sf.jsqlparser.expression.WhenClause.class,
                net.sf.jsqlparser.expression.XMLSerializeExpr.class,
        };

        for (Class<?> clazz : expressionClasses) {
            hints.reflection().registerType(clazz, MemberCategory.ACCESS_DECLARED_FIELDS);
        }

        // ===== net.sf.jsqlparser.expression.operators.relational 包 =====
        Class<?>[] relationalClasses = {
                net.sf.jsqlparser.expression.operators.relational.Between.class,
                net.sf.jsqlparser.expression.operators.relational.ContainedBy.class,
                net.sf.jsqlparser.expression.operators.relational.Contains.class,
                net.sf.jsqlparser.expression.operators.relational.CosineSimilarity.class,
                net.sf.jsqlparser.expression.operators.relational.DoubleAnd.class,
                net.sf.jsqlparser.expression.operators.relational.ExcludesExpression.class,
                net.sf.jsqlparser.expression.operators.relational.ExistsExpression.class,
                net.sf.jsqlparser.expression.operators.relational.FullTextSearch.class,
                net.sf.jsqlparser.expression.operators.relational.GeometryDistance.class,
                net.sf.jsqlparser.expression.operators.relational.IncludesExpression.class,
                net.sf.jsqlparser.expression.operators.relational.InExpression.class,
                net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression.class,
                net.sf.jsqlparser.expression.operators.relational.IsDistinctExpression.class,
                net.sf.jsqlparser.expression.operators.relational.IsNullExpression.class,
                net.sf.jsqlparser.expression.operators.relational.IsUnknownExpression.class,
                net.sf.jsqlparser.expression.operators.relational.JsonOperator.class,
                net.sf.jsqlparser.expression.operators.relational.MemberOfExpression.class,
                net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList.class,
                net.sf.jsqlparser.expression.operators.relational.RegExpMatchOperator.class,
                net.sf.jsqlparser.expression.operators.relational.SimilarToExpression.class,
        };

        for (Class<?> clazz : relationalClasses) {
            hints.reflection().registerType(clazz, MemberCategory.ACCESS_DECLARED_FIELDS);
        }

        // ===== select 包下含 Expression 字段的类 =====
        Class<?>[] selectClasses = {
                net.sf.jsqlparser.statement.select.ParenthesedFromItem.class,
                net.sf.jsqlparser.statement.select.TableFunction.class,
        };

        for (Class<?> clazz : selectClasses) {
            hints.reflection().registerType(clazz, MemberCategory.ACCESS_DECLARED_FIELDS);
        }
    }
}
