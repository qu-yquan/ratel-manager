package org.quyq.gwsu.common.core.constants;


import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * @author Quyq
 * @date 2026/3/10
 * @description 公共常量
 */
public interface CoreConstants {

    interface Project {
        String COMMON_PACKAGE = "org.quyq.gwsu";
    }


    interface Yaml {

        String PROJECT_CONFIG_PREFIX = "ratel";

        String APPLICATION_NAME = "spring.application.name";

        String DEPLOY_SINGLE = "deploy.single";

    }

    /**
     * 通用端点
     */
    interface EndPoint {

        /**
         * 获取微服务信息列表
         */
        String ENDPOINT_MODULE_INFOS = "/modules/list";

        /**
         * 查询数据库接口 ， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_EXECUTION = "/db/query";

        /**
         * 查询所有数据源列表， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_DATASOURCE = "/db/datasource";

        /**
         * 查询指定数据源中库的表信息， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_TABLES = "/db/tables";

        /**
         * 查询指定表的的列信息， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_COLUMNS = "/db/columns";

        /**
         * 查询指定表的外键， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_FOREIGN_KEY = "/db/foreign_key";

        /**
         * 获取指定数据源的数据名， 该接口只有微服务模式启用
         */
        String ENDPOINT_DB_NAME = "/db/name";

    }

    interface Headers {

        /**
         * 服务调用请求头传递时忽略的内容
         */
        List<String> REQUEST_IGNORE_HEADER = Arrays.asList("content-length", "connection", "origin", "cookie", "accept", "request-origion", "referer", //NOSONAR
                "host", "forwarded", "content-md5", "cache-control", "etag", "server", "accept-encoding", "content-encoding", "transfer-encoding", "content-type");

        /**
         * 认证主体的用户名
         */
        String AUTHORIZATION_USER_NAME = "x-username";

        /**
         * 当前认证会话标识
         */
        String AUTHORIZATION_ID = "x-authorization-id";

        /**
         * 接口来源服务
         */
        String SERVER_FROM_APP = "x-server-from-app";

        /**
         * 接口调用的界面菜单ID
         */
        String VIEW_FROM_PAGE_MENU = "view-menu-id";

        /**
         * 界面操作主体对象：0-人类；1-智能助手
         */
        String VIEW_OPERATION_SUBJECT = "view-operation-subject";

        /**
         * 请求头认证令牌键
         */
        String HTTP_HEADER_TOKEN_KEY = "authorization";

        /**
         * 令牌前缀
         */
        String TOKEN_PREFIX = "Bearer ";
    }


    interface Server {

        /**
         * 安全配置服务
         */
        String SECURITY_NAME = "gwsu-security";

        /**
         *  系统服务
         */
        String SYSTEM_NAME = "gwsu-system";

        /**
         * 日志记录服务
         */
        String LOG_NAME = "gwsu-log";

        /**
         * 公共套件服务
         */
        String KIT_NAME = "gwsu-kit";

        /**
         * 无头智能体服务
         */
        String HEADLESS_NAME = "gwsu-headless";

    }

    interface Agent {

        String BRAIN_AGENT_NAME = "CentralBrain";

    }


    @Getter
    enum Code {
        SUCCESS(200, "操作成功"),
        ERROR(500, "操作失败");

        private final String msg;
        private final int code;

        Code(int code, String msg) {
            this.code = code;
            this.msg = msg;

        }


    }

}
