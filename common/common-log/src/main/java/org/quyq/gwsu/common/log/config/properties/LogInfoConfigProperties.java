package org.quyq.gwsu.common.log.config.properties;


import org.quyq.gwsu.common.log.dto.LogLifeCycle;
import org.quyq.gwsu.common.log.dto.LogStorage;
import org.quyq.gwsu.common.log.enums.SaveMedium;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Quyq
 * @date 2026/5/14
 * @description
 */
@ConfigurationProperties("dtt.log")
public record LogInfoConfigProperties(
        AccessLogProperties accessLog ,
        TableLogProperties tableLog ,
        LogininfoProperties loginLog ,
        Store store
) {

    public LogInfoConfigProperties {
        if(Objects.isNull(accessLog)) {
            accessLog = new AccessLogProperties(true , 2 , new ArrayList<>());
        }
        if(Objects.isNull(tableLog)) {
            tableLog = new TableLogProperties(true ,false , null , null , null ,null , null);
        }
        if(Objects.isNull(loginLog)){
            loginLog = new LogininfoProperties(true);
        }
        if(Objects.isNull(store)){
            store = new Store(
                    //操作日志
                    new LogStorage(SaveMedium.DATABASE ,new LogLifeCycle(60 , 180)) ,
                    //表操作日志
                    new LogStorage(SaveMedium.DATABASE , new LogLifeCycle(60 , 180)) ,
                    // 登录日志
                    new LogStorage(SaveMedium.DATABASE , new LogLifeCycle(360 , 720) )
            );
        }
    }


    /**
     * 访问日志配置
     */
    public record AccessLogProperties(
            /**
             * 启用标识
             */
            boolean enabled,
            /**
             * 日志记录线程数
             */
            Integer recordThreadCount,
            /**
             * 忽略记录日志的url列表
             */
            List<String> ignoreList
    ) {
        public AccessLogProperties {
            if (Objects.isNull(recordThreadCount)) {
                recordThreadCount = 2;
            }
        }
    }


    public record TableLogProperties(
            /**
             * 启用标识
             */
            boolean enabled,
            /**
             * 使用白名单模式。 false - 黑名单模式；true - 白名单模式
             * 白名单模式: 只记录 tableList 配置的表变更日志。
             * 黑名单模式：默认记录所有可记录的表 ， 不记录 tableList 配置的表变更日志。
             * 注意： 只有 表里包含 OPER_LOG_ID 字段才支持记录
             */
            boolean useWriteList,
            /**
             * 配置表名单
             */
            List<String> tableList,
            /**
             * 数据监听topic
             */
            String consumerTopic,

            /**
             * 数据消费组
             */
            String consumerGroup,

            /**
             * 消费者数量
             */
            Integer consumerConcurrency,

            /**
             * 消费者使用数据源
             */
            String consumerDatasource
    ) {
        public TableLogProperties {
            if (!StringUtils.hasText(consumerGroup)) {
                consumerGroup = "tableDataChangeGroup";
            }
            if (Objects.isNull(consumerConcurrency)) {
                consumerConcurrency = 1;
            }
            if (!StringUtils.hasText(consumerDatasource)) {
                consumerDatasource = "master";
            }
        }

    }

    /**
     * 登录日志记录
     * @param enabled
     */
    public record LogininfoProperties(
            boolean enabled

    ){}

    public record Store(
            LogStorage accessLog ,
            LogStorage tableLog ,
            LogStorage loginLog
    ){}

}
