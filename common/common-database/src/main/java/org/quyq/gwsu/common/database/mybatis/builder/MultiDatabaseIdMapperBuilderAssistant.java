package org.quyq.gwsu.common.database.mybatis.builder;


import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.keygen.KeyGenerator;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.LanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.springframework.util.StringUtils;

/**
 * @author Quyq
 * @date 2026/3/19
 * @description
 */
public class MultiDatabaseIdMapperBuilderAssistant extends MapperBuilderAssistant {
    public MultiDatabaseIdMapperBuilderAssistant(Configuration configuration, String resource) {
        super(configuration, resource);
    }


    @Override
    public MappedStatement addMappedStatement(String id, SqlSource sqlSource, StatementType statementType, SqlCommandType sqlCommandType, Integer fetchSize, Integer timeout, String parameterMap, Class<?> parameterType, String resultMap, Class<?> resultType, ResultSetType resultSetType, boolean flushCache, boolean useCache, boolean resultOrdered, KeyGenerator keyGenerator, String keyProperty, String keyColumn, String databaseId, LanguageDriver lang, String resultSets, boolean dirtySelect) {
        if (StringUtils.hasText(databaseId)) {
            id = "%s_%s".formatted(id, databaseId);
        }
        String statementId = applyCurrentNamespace(id, false);
        if (configuration.hasStatement(statementId, false)) {
            return null;
        }
        return super.addMappedStatement(id, sqlSource, statementType, sqlCommandType, fetchSize, timeout, parameterMap, parameterType, resultMap, resultType, resultSetType, flushCache, useCache, resultOrdered, keyGenerator, keyProperty, keyColumn, databaseId, lang, resultSets, dirtySelect);
    }
}
