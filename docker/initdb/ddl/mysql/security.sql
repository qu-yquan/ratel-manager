-- =============================================
-- 菜单管理相关表结构
-- 数据库：MySQL
-- =============================================

-- =============================================
-- 表名：security_menu
-- 说明：菜单表
-- =============================================
CREATE TABLE security_menu
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    parent_id   VARCHAR(24)          DEFAULT NULL COMMENT '父菜单ID，NULL表示顶级菜单',
    menu_name   VARCHAR(50) NOT NULL              COMMENT '菜单名称',
    menu_type   SMALLINT   NOT NULL DEFAULT 1     COMMENT '菜单类型：1-目录 2-菜单 3-按钮',
    sort        INT        NOT NULL DEFAULT 0      COMMENT '排序号',
    icon        VARCHAR(100)         DEFAULT NULL COMMENT '菜单图标',
    path        VARCHAR(200)         DEFAULT NULL COMMENT '路由路径',
    visible     SMALLINT   NOT NULL DEFAULT 1     COMMENT '是否显示：0-隐藏 1-显示',
    status      SMALLINT   NOT NULL DEFAULT 1     COMMENT '状态：0-禁用 1-正常',
    permission  VARCHAR(100)         DEFAULT NULL COMMENT '权限标识',
    position    INT                  DEFAULT NULL COMMENT '菜单位置类型：1-侧边栏 2-顶部栏',
    owner       INT                  DEFAULT NULL COMMENT '菜单所属类型：1-后端管理 2-移动端APP',
    button_key  VARCHAR(100)         DEFAULT NULL COMMENT '按钮标识，格式：菜单ID_标识，用于前端按钮显示控制',
    description VARCHAR(1024)        DEFAULT NULL COMMENT '功能描述，用于AI提示词构建',
    tenant_id   VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '菜单表';

-- 索引
CREATE INDEX idx_security_menu_parent_id ON security_menu (parent_id);
CREATE INDEX idx_security_menu_sort ON security_menu (sort);
CREATE INDEX idx_security_menu_type ON security_menu (menu_type);

-- =============================================
-- 表名：security_role
-- 说明：角色表
-- =============================================
CREATE TABLE security_role
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_name   VARCHAR(50) NOT NULL              COMMENT '角色名称',
    role_code   VARCHAR(50) NOT NULL              COMMENT '角色编码',
    sort        INT        NOT NULL DEFAULT 0      COMMENT '排序号',
    description VARCHAR(200)         DEFAULT NULL COMMENT '角色描述',
    status      SMALLINT   NOT NULL DEFAULT 1     COMMENT '状态：0-禁用 1-正常',
    role_type   SMALLINT   NOT NULL DEFAULT 2     COMMENT '角色类型：1-系统角色 2-业务角色',
    data_scope  SMALLINT   NOT NULL DEFAULT 1     COMMENT '数据范围：0-自定义 1-全部数据 2-本部门及以下 3-本部门 4-仅本人',
    tenant_id   VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '角色表';

-- 唯一索引
CREATE UNIQUE INDEX uk_security_role_code ON security_role (role_code);

-- =============================================
-- 表名：security_role_subject
-- 说明：主体角色关联表
-- =============================================
CREATE TABLE security_role_subject
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    subject_id  VARCHAR(24) NOT NULL              COMMENT '主体ID（用户ID）',
    role_id     VARCHAR(24) NOT NULL              COMMENT '角色ID',
    tenant_id   VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '主体角色关联表';

-- 索引
CREATE UNIQUE INDEX uk_security_role_subject ON security_role_subject (subject_id, role_id);
CREATE INDEX idx_security_role_subject_id ON security_role_subject (role_id);
-- 外键
ALTER TABLE security_role_subject ADD CONSTRAINT role_subject_fk FOREIGN KEY (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_role_menu
-- 说明：角色菜单关联表
-- =============================================
CREATE TABLE security_role_menu
(
    id               VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_id          VARCHAR(24) NOT NULL              COMMENT '角色ID',
    menu_id          VARCHAR(24) NOT NULL              COMMENT '菜单ID',
    valid_type       SMALLINT   NOT NULL DEFAULT 1     COMMENT '时效类型：1-永久 2-绝对时间范围 3-周期性',
    valid_start      DATETIME             DEFAULT NULL COMMENT '绝对时间-开始时间',
    valid_end        DATETIME             DEFAULT NULL COMMENT '绝对时间-结束时间',
    cycle_type       SMALLINT             DEFAULT NULL COMMENT '周期类型：1-按周 2-按月',
    cycle_value      VARCHAR(100)         DEFAULT NULL COMMENT '周期值：按周存1,2,3,4,5 按月存1,15',
    cycle_start_time VARCHAR(10)          DEFAULT NULL COMMENT '周期-每日开始时间',
    cycle_end_time   VARCHAR(10)          DEFAULT NULL COMMENT '周期-每日结束时间',
    tenant_id        VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op        VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op        VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time      DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted          SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op        VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time      DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '角色菜单关联表';

-- 索引
CREATE INDEX idx_security_role_menu_id ON security_role_menu (menu_id);
-- 外键
ALTER TABLE security_role_menu ADD CONSTRAINT role_menu_role_menu_id_fk FOREIGN KEY (menu_id) REFERENCES security_menu(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE security_role_menu ADD CONSTRAINT role_menu_roleid_fk FOREIGN KEY (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_abac
-- 说明：ABAC表达式表
-- =============================================
CREATE TABLE security_abac
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    expression  VARCHAR(500) NOT NULL             COMMENT '表达式内容',
    description VARCHAR(200)          DEFAULT NULL COMMENT '表达式描述',
    status      SMALLINT    NOT NULL DEFAULT 1    COMMENT '启用状态：0-禁用 1-启用',
    tenant_id   VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT    NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT 'ABAC表达式表';

-- 索引
CREATE INDEX idx_security_abac_expression ON security_abac (expression);

-- =============================================
-- 表名：security_abac_permission
-- 说明：ABAC接口权限关联表
-- =============================================
CREATE TABLE security_abac_permission
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    abac_id       VARCHAR(24)  NOT NULL             COMMENT '表达式ID',
    resource_type VARCHAR(50)  NOT NULL             COMMENT '资源类型',
    action        VARCHAR(50)  NOT NULL             COMMENT '操作',
    url_pattern   VARCHAR(500) NOT NULL             COMMENT 'URL模式',
    effect        VARCHAR(20)  NOT NULL DEFAULT 'allow' COMMENT '效果：allow-允许 deny-拒绝',
    status        SMALLINT     NOT NULL DEFAULT 1   COMMENT '状态：0-禁用 1-启用',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT 'ABAC接口权限关联表';

-- 索引
CREATE INDEX idx_security_abac_permission_abac_id ON security_abac_permission (abac_id);
CREATE INDEX idx_security_abac_permission_resource_type ON security_abac_permission (resource_type);
CREATE INDEX idx_security_abac_permission_url_pattern ON security_abac_permission (url_pattern);
-- 外键
ALTER TABLE security_abac_permission ADD CONSTRAINT abac_permission_fk FOREIGN KEY (abac_id) REFERENCES security_abac(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_abac_field
-- 说明：ABAC字段权限关联表
-- =============================================
CREATE TABLE security_abac_field
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    abac_id       VARCHAR(24)  NOT NULL             COMMENT '表达式ID',
    resource_type VARCHAR(50)  NOT NULL             COMMENT '资源类型',
    action        VARCHAR(50)  NOT NULL             COMMENT '操作',
    url_pattern   VARCHAR(500) NOT NULL             COMMENT 'URL',
    field_mode    VARCHAR(20)  NOT NULL DEFAULT 'allow' COMMENT '字段模式：allow-允许 deny-拒绝',
    fields        TEXT                  DEFAULT NULL COMMENT '字段列表，JSON数组格式',
    status        SMALLINT     NOT NULL DEFAULT 1   COMMENT '状态：0-禁用 1-启用',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT 'ABAC字段权限关联表';

-- 索引
CREATE INDEX idx_security_abac_field_abac_id ON security_abac_field (abac_id);
CREATE INDEX idx_security_abac_field_resource_type ON security_abac_field (resource_type);
CREATE INDEX idx_security_abac_field_url_pattern ON security_abac_field (url_pattern);
-- 外键
ALTER TABLE security_abac_field ADD CONSTRAINT abac_field_fk FOREIGN KEY (abac_id) REFERENCES security_abac(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_resource
-- 说明：接口资源表
-- =============================================
CREATE TABLE security_api_resource
(
    id                 VARCHAR(64) PRIMARY KEY COMMENT '主键ID',
    module_prefix      VARCHAR(50)           DEFAULT ''   COMMENT '模块前缀',
    tag_name           VARCHAR(100)          DEFAULT ''   COMMENT 'Tag标签名称',
    req_path           VARCHAR(500) NOT NULL              COMMENT '接口地址',
    req_method         VARCHAR(10)  NOT NULL              COMMENT '请求方式(GET/POST/PUT/DELETE等)',
    summary            VARCHAR(200)          DEFAULT ''   COMMENT '接口摘要',
    request_class      VARCHAR(500)          DEFAULT ''   COMMENT '请求参数类型全限定名',
    response_class     VARCHAR(500)          DEFAULT ''   COMMENT '响应类型全限定名',
    class_name         VARCHAR(200)          DEFAULT NULL COMMENT '类名',
    method_name        VARCHAR(64)           DEFAULT NULL COMMENT '方法名',
    login_allow_access SMALLINT              DEFAULT 0    COMMENT '登录后允许访问',
    tenant_id          VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '接口资源表';

-- 索引
CREATE INDEX idx_security_api_resource_module_prefix ON security_api_resource (module_prefix);
CREATE INDEX idx_security_api_resource_req_path_method ON security_api_resource (req_path, req_method);
CREATE INDEX idx_security_api_resource_tag_name ON security_api_resource (tag_name);

-- =============================================
-- 表名：security_data_resource
-- 说明：数据资源配置主表
-- =============================================
CREATE TABLE security_data_resource
(
    id                VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    catalog_name      VARCHAR(100)          DEFAULT NULL COMMENT 'Catalog 名称，为空时匹配所有 Catalog',
    schema_name       VARCHAR(100)          DEFAULT NULL COMMENT '数据库/Schema 名称，为空时匹配所有数据库或 Schema',
    table_name        VARCHAR(100) NOT NULL             COMMENT '表名',
    description       VARCHAR(200)          DEFAULT NULL COMMENT '规则描述',
    support_self_only SMALLINT     NOT NULL DEFAULT 0   COMMENT '是否支持SELF_ONLY过滤：0-不支持 1-支持',
    self_only_field   VARCHAR(100)          DEFAULT NULL COMMENT 'SELF_ONLY过滤时使用的字段名，即目标表中记录创建人的字段',
    status            SMALLINT     NOT NULL DEFAULT 1   COMMENT '启用状态：0-禁用 1-启用',
    tenant_id         VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op         VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time       DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op         VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time       DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted           SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op         VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time       DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '数据资源配置主表';

-- 索引
CREATE INDEX idx_security_data_resource_table_name ON security_data_resource (table_name);
CREATE INDEX idx_security_data_resource_catalog_name ON security_data_resource (catalog_name);
CREATE INDEX idx_security_data_resource_schema_name ON security_data_resource (schema_name);

-- =============================================
-- 表名：security_data_resource_condition
-- 说明：数据资源字段条件配置表
-- =============================================
CREATE TABLE security_data_resource_condition
(
    id                   VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    data_resource_id     VARCHAR(24)  NOT NULL             COMMENT '数据资源配置ID',
    field_name           VARCHAR(100) NOT NULL             COMMENT '字段名',
    show_null            SMALLINT     NOT NULL DEFAULT 0   COMMENT '显示过滤字段为null的数据：0-不显示 1-显示',
    user_resource_fields VARCHAR(500)          DEFAULT NULL COMMENT '关联的用户数据资源字段，多个用逗号分隔',
    assert_type          VARCHAR(20)  NOT NULL DEFAULT 'EQ' COMMENT '断言类型：EQ-等于 LIKE-模糊匹配',
    relationship         VARCHAR(10)  NOT NULL DEFAULT 'AND' COMMENT '与上一个条件的关联关系：AND-与 OR-或',
    sort                 INT          NOT NULL DEFAULT 0    COMMENT '排序号',
    tenant_id            VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op            VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time          DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op            VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time          DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted              SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op            VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time          DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '数据资源字段条件配置表';

-- 索引
CREATE INDEX idx_security_data_resource_condition_data_resource_id ON security_data_resource_condition (data_resource_id);
-- 外键
ALTER TABLE security_data_resource_condition ADD CONSTRAINT data_resource_condition_fk FOREIGN KEY (data_resource_id) REFERENCES security_data_resource(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 表名：security_role_menu_permission
-- 说明：角色菜单权限关联表
-- =============================================
CREATE TABLE security_role_menu_permission
(
    id                 VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_menu_id       VARCHAR(24) NOT NULL              COMMENT '角色菜单关联ID，关联security_role_menu表',
    abac_permission_id VARCHAR(24) NOT NULL              COMMENT 'ABAC接口权限ID，关联security_abac_permission表',
    api_id             VARCHAR(64) NOT NULL              COMMENT '接口资源ID，关联security_api_resource表',
    tenant_id          VARCHAR(50)          DEFAULT NULL COMMENT '租户ID',
    create_op          VARCHAR(50)          DEFAULT NULL COMMENT '创建人',
    create_time        DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op          VARCHAR(50)          DEFAULT NULL COMMENT '修改人',
    modify_time        DATETIME             DEFAULT NULL COMMENT '修改时间',
    deleted            SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op          VARCHAR(50)          DEFAULT NULL COMMENT '删除人',
    delete_time        DATETIME             DEFAULT NULL COMMENT '删除时间'
) COMMENT '角色菜单权限关联表';

-- 索引
CREATE INDEX idx_security_role_menu_permission_role_menu_id ON security_role_menu_permission (role_menu_id);
CREATE INDEX idx_security_role_menu_permission_abac_permission_id ON security_role_menu_permission (abac_permission_id);
-- 外键
ALTER TABLE security_role_menu_permission ADD CONSTRAINT role_menu_permission_fk FOREIGN KEY (role_menu_id) REFERENCES security_role_menu(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_table_model
-- 说明：接口-表模型绑定表（注解采集，启动时覆盖）
-- =============================================
CREATE TABLE security_api_table_model
(
    id            VARCHAR(64) PRIMARY KEY COMMENT '主键ID，MD5(module_prefix + datasource + table_name + api_id)',
    api_id        VARCHAR(64)  NOT NULL             COMMENT '接口资源ID，关联security_api_resource.id',
    module_prefix VARCHAR(50)  NOT NULL DEFAULT ''   COMMENT '模块前缀（服务标识）',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    table_name    VARCHAR(100) NOT NULL              COMMENT '表模型名称',
    field_config  TEXT                  DEFAULT NULL COMMENT '字段配置JSON，仅记录注解标识的字段配置',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '接口-表模型绑定表（注解采集，启动时覆盖）';

-- 索引
CREATE INDEX idx_security_api_table_model_api_id ON security_api_table_model (api_id);
CREATE INDEX idx_security_api_table_model_table_name ON security_api_table_model (table_name);
CREATE INDEX idx_security_api_table_model_module_prefix ON security_api_table_model (module_prefix);
CREATE INDEX idx_security_api_table_model_module_datasource_table ON security_api_table_model (module_prefix, datasource, table_name);
-- 外键
ALTER TABLE security_api_table_model ADD CONSTRAINT table_model_fk FOREIGN KEY (api_id) REFERENCES security_api_resource(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_table_model_config
-- 说明：表模型手动配置表（持久化，启动不覆盖）
-- =============================================
CREATE TABLE security_api_table_model_config
(
    id             VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    table_model_id VARCHAR(64)           DEFAULT NULL COMMENT '关联security_api_table_model的ID',
    datasource     VARCHAR(50) NOT NULL              COMMENT '数据源名称',
    tenant_id      VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op      VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op      VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time    DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted        SMALLINT   NOT NULL DEFAULT 0     COMMENT '删除标识：0-未删除 1-已删除',
    delete_op      VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time    DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '表模型手动配置表（持久化，启动不覆盖）';

-- 索引
CREATE INDEX idx_security_api_table_model_config_table_model_id ON security_api_table_model_config (table_model_id);
-- 外键
ALTER TABLE security_api_table_model_config ADD CONSTRAINT table_model_config_fk FOREIGN KEY (table_model_id) REFERENCES security_api_table_model(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_role_table_model
-- 说明：角色表模型权限配置表
-- =============================================
CREATE TABLE security_role_table_model
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    role_id       VARCHAR(24)  NOT NULL             COMMENT '角色ID',
    module_prefix VARCHAR(50)  NOT NULL DEFAULT ''   COMMENT '模块前缀（服务标识）',
    table_name    VARCHAR(100) NOT NULL              COMMENT '表模型名称',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master' COMMENT '数据源名称',
    field_config  TEXT                  DEFAULT NULL COMMENT '字段限制配置JSON，仅存储限制性配置',
    enabled       TINYINT      NOT NULL DEFAULT 1    COMMENT '是否启用：0-禁用 1-启用，仅对接口关联的表模型有效',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '角色表模型权限配置表';

-- 索引
CREATE INDEX idx_security_role_table_model_role_id ON security_role_table_model (role_id);
CREATE INDEX idx_security_role_table_model_table_name ON security_role_table_model (table_name);
CREATE UNIQUE INDEX uk_security_role_table_model ON security_role_table_model (role_id, module_prefix, datasource, table_name);
-- 外键
ALTER TABLE security_role_table_model ADD CONSTRAINT role_table_model_roleid_fk FOREIGN KEY (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_tablemodel_tables
-- 说明：表基本信息
-- =============================================
CREATE TABLE security_tablemodel_tables
(
    id            VARCHAR(64) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    table_name    VARCHAR(128) NOT NULL             COMMENT '表名',
    module_prefix VARCHAR(64)           DEFAULT NULL COMMENT '模块前缀',
    data_source   VARCHAR(64)  NOT NULL             COMMENT '数据源',
    table_comment TEXT                  DEFAULT NULL COMMENT '表注释',
    source_type   SMALLINT     NOT NULL DEFAULT 0   COMMENT '来源类型：0-采集 1-自定义添加',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '表基本信息';

CREATE UNIQUE INDEX uk_security_tablemodel_tables_table_datasource ON security_tablemodel_tables (table_name, data_source);

-- =============================================
-- 表名：security_tablemodel_columns
-- 说明：字段详细信息
-- =============================================
CREATE TABLE security_tablemodel_columns
(
    id               VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    table_id         VARCHAR(64)  NOT NULL             COMMENT '关联表ID',
    column_name      VARCHAR(128) NOT NULL             COMMENT '字段名',
    column_type      VARCHAR(64)  NOT NULL             COMMENT '字段类型',
    column_length    INT                   DEFAULT NULL COMMENT '字段长度',
    column_scale     INT                   DEFAULT NULL COMMENT '字段精度',
    is_nullable      SMALLINT     NOT NULL DEFAULT 1   COMMENT '是否可空：0-否 1-是',
    is_primary_key   SMALLINT     NOT NULL DEFAULT 0   COMMENT '是否主键：0-否 1-是',
    pk_position      INT                   DEFAULT 0    COMMENT '主键位置',
    default_value    TEXT                  DEFAULT NULL COMMENT '默认值',
    column_comment   TEXT                  DEFAULT NULL COMMENT '字段注释',
    ordinal_position INT                   DEFAULT 0    COMMENT '字段顺序',
    field_config     VARCHAR(512)          DEFAULT NULL COMMENT '字段AI权限配置',
    dict_key         VARCHAR(100)          DEFAULT NULL COMMENT '字典键（绑定枚举值）',
    tenant_id        VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op        VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op        VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time      DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted          SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op        VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time      DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '字段详细信息';

CREATE UNIQUE INDEX uk_security_tablemodel_columns_table_column ON security_tablemodel_columns (table_id, column_name);
CREATE INDEX idx_security_tablemodel_columns_table_id ON security_tablemodel_columns (table_id);
-- 外键
ALTER TABLE security_tablemodel_columns ADD CONSTRAINT tablemodel_column_fk FOREIGN KEY (table_id) REFERENCES security_tablemodel_tables(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_tablemodel_foreign_keys
-- 说明：外键约束信息（单列外键）
-- =============================================
CREATE TABLE security_tablemodel_foreign_keys
(
    id                     VARCHAR(24) PRIMARY KEY COMMENT '主键ID（雪花算法）',
    constraint_name        VARCHAR(128) NOT NULL             COMMENT '约束名称',
    table_id               VARCHAR(64)  NOT NULL             COMMENT '所属表ID',
    column_name            VARCHAR(128) NOT NULL             COMMENT '字段名',
    referenced_table_name  VARCHAR(128) NOT NULL             COMMENT '引用表名',
    referenced_column_name VARCHAR(128) NOT NULL             COMMENT '引用字段名',
    update_rule            VARCHAR(16)           DEFAULT 'RESTRICT' COMMENT '更新规则',
    delete_rule            VARCHAR(16)           DEFAULT 'RESTRICT' COMMENT '删除规则',
    data_type              SMALLINT     NOT NULL DEFAULT 0   COMMENT '数据类型：0-采集 1-自定义添加',
    remark                 VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    tenant_id              VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op              VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time            DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op              VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time            DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted                SMALLINT     NOT NULL DEFAULT 0   COMMENT '删除标识：0-未删除 1-已删除',
    delete_op              VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time            DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '外键约束信息（单列外键）';

CREATE UNIQUE INDEX uk_security_tablemodel_foreign_keys_table_constraint ON security_tablemodel_foreign_keys (table_id, constraint_name);
CREATE INDEX idx_security_tablemodel_foreign_keys_referenced_table_name ON security_tablemodel_foreign_keys (referenced_table_name);
-- 外键
ALTER TABLE security_tablemodel_foreign_keys ADD CONSTRAINT tablemodel_foreign_fk FOREIGN KEY (table_id) REFERENCES security_tablemodel_tables(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 变更：security_tablemodel_columns 增加 field_config 字段
-- 说明：字段权限配置，JSON格式存储 FieldPermission 对象
-- =============================================
-- 注意：建表时已包含 field_config 字段，此 ALTER 仅作变更记录保留
-- ALTER TABLE security_tablemodel_columns ADD COLUMN field_config TEXT DEFAULT NULL COMMENT '字段权限配置JSON，格式为 FieldPermission 对象';

-- =============================================
-- 表名：security_config
-- 说明：配置表
-- =============================================
CREATE TABLE security_config
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    config_key    VARCHAR(100) NOT NULL              COMMENT '配置键',
    config_name   VARCHAR(100) NOT NULL              COMMENT '配置名称',
    config_value  TEXT                  DEFAULT NULL COMMENT '配置值，JSON字符串或基本类型值',
    value_type    SMALLINT     NOT NULL DEFAULT 1    COMMENT '值类型：1-STR 2-NUMBER 3-BOOL 4-JSON',
    config_type   SMALLINT     NOT NULL DEFAULT 2    COMMENT '配置类型：1-系统 2-自定义',
    description   VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '配置表';

CREATE UNIQUE INDEX uk_security_config_key ON security_config (config_key);
CREATE INDEX idx_security_config_type ON security_config (config_type);

-- =============================================
-- 表名：security_dict
-- 说明：字典表
-- =============================================
CREATE TABLE security_dict
(
    id            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    dict_key      VARCHAR(100) NOT NULL              COMMENT '字典键',
    dict_name     VARCHAR(100) NOT NULL              COMMENT '字典名称',
    dict_type     SMALLINT     NOT NULL DEFAULT 2    COMMENT '字典类型：1-系统 2-自定义',
    description   VARCHAR(500)          DEFAULT NULL COMMENT '描述',
    tenant_id     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted       SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '字典表';

CREATE UNIQUE INDEX uk_security_dict_key ON security_dict (dict_key);
CREATE INDEX idx_security_dict_type ON security_dict (dict_type);

-- =============================================
-- 表名：security_dict_value
-- 说明：字典值表
-- =============================================
CREATE TABLE security_dict_value
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    dict_key     VARCHAR(24)  NOT NULL             COMMENT '关联字典key',
    dict_label  VARCHAR(256) NOT NULL              COMMENT '字典标签',
    dict_value  VARCHAR(500) NOT NULL              COMMENT '字典值',
    sort        INT          NOT NULL DEFAULT 0    COMMENT '排序序号',
    tenant_id   VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT '字典值表';

CREATE INDEX idx_security_dict_value_dict_id ON security_dict_value (dict_key);


-- =============================================
-- 表名：security_business_function
-- 说明：AI业务功能配置表
-- =============================================
CREATE TABLE security_business_function
(
    id          VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    name        VARCHAR(128) NOT NULL              COMMENT '业务名称',
    summary     VARCHAR(512) NOT NULL              COMMENT '业务简介',
    detail      LONGTEXT     NOT NULL              COMMENT '详细介绍（Markdown格式）',
    sort_order  INT          NOT NULL DEFAULT 0    COMMENT '排序号',
    tenant_id   VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op   VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op   VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted     SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op   VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT 'AI业务功能配置表';

CREATE UNIQUE INDEX uk_security_business_function_name ON security_business_function (name);

-- =============================================
-- 表名：security_business_function_table
-- 说明：业务功能与表模型关联表
-- =============================================
CREATE TABLE security_business_function_table
(
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    business_id     VARCHAR(24)  NOT NULL              COMMENT '业务功能ID',
    table_model_id  VARCHAR(64)  NOT NULL              COMMENT '表模型ID',
    sort_order      INT          NOT NULL DEFAULT 0    COMMENT '排序号',
    tenant_id       VARCHAR(50)            DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50)            DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME               DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50)            DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME               DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT      NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op       VARCHAR(50)            DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME               DEFAULT NULL COMMENT '删除时间'
) COMMENT '业务功能与表模型关联表';

CREATE UNIQUE INDEX uk_security_business_function_table_bt ON security_business_function_table (business_id, table_model_id);
CREATE INDEX idx_security_business_function_table_business ON security_business_function_table (business_id);
CREATE INDEX idx_security_business_function_table_table ON security_business_function_table (table_model_id);

-- =============================================
-- 表名：security_oauth_client
-- 说明：OAuth应用配置表
-- =============================================
CREATE TABLE security_oauth_client
(
    id                            VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    client_id                     VARCHAR(128) NOT NULL              COMMENT 'OAuth客户端ID',
    client_secret                 VARCHAR(128)          DEFAULT NULL COMMENT 'OAuth客户端密钥密文',
    client_name                   VARCHAR(128) NOT NULL              COMMENT '应用名称',
    client_type                   VARCHAR(32)  NOT NULL              COMMENT '客户端类型',
    account_type                  VARCHAR(32)  NOT NULL DEFAULT 'MANAGER' COMMENT '账号类型',
    status                        VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    client_authentication_methods TEXT         NOT NULL              COMMENT '客户端认证方式，逗号分隔',
    authorization_grant_types     TEXT         NOT NULL              COMMENT '授权模式，逗号分隔',
    redirect_uris                 TEXT                   DEFAULT NULL COMMENT '重定向URI白名单，逗号分隔',
    post_logout_redirect_uris     TEXT                   DEFAULT NULL COMMENT '退出后重定向URI白名单，逗号分隔',
    scopes                        TEXT         NOT NULL              COMMENT '授权范围，逗号分隔',
    require_proof_key             SMALLINT     NOT NULL DEFAULT 1    COMMENT '是否要求PKCE：0-否 1-是',
    require_authorization_consent SMALLINT     NOT NULL DEFAULT 1    COMMENT '是否要求授权确认：0-否 1-是',
    access_token_ttl_seconds      BIGINT       NOT NULL DEFAULT 7200 COMMENT 'Access Token有效期秒',
    refresh_token_ttl_seconds     BIGINT       NOT NULL DEFAULT 2592000 COMMENT 'Refresh Token有效期秒',
    authorization_code_ttl_seconds BIGINT      NOT NULL DEFAULT 300  COMMENT '授权码有效期秒',
    device_code_ttl_seconds       BIGINT       NOT NULL DEFAULT 300  COMMENT '设备码有效期秒',
    reuse_refresh_tokens          SMALLINT     NOT NULL DEFAULT 0    COMMENT '是否复用Refresh Token：0-否 1-是',
    remark                        VARCHAR(500)          DEFAULT NULL COMMENT '备注',
    tenant_id                     VARCHAR(50)           DEFAULT NULL COMMENT '租户ID',
    create_op                     VARCHAR(50)           DEFAULT NULL COMMENT '创建人',
    create_time                   DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op                     VARCHAR(50)           DEFAULT NULL COMMENT '修改人',
    modify_time                   DATETIME              DEFAULT NULL COMMENT '修改时间',
    deleted                       SMALLINT     NOT NULL DEFAULT 0    COMMENT '删除标识：0-未删除 1-已删除',
    delete_op                     VARCHAR(50)           DEFAULT NULL COMMENT '删除人',
    delete_time                   DATETIME              DEFAULT NULL COMMENT '删除时间'
) COMMENT 'OAuth应用配置表';

CREATE UNIQUE INDEX uk_security_oauth_client_client_id ON security_oauth_client (client_id);
CREATE INDEX idx_security_oauth_client_name ON security_oauth_client (client_name);
CREATE INDEX idx_security_oauth_client_status ON security_oauth_client (status);

-- =============================================
-- 表名：security_oauth_scope
-- 说明：OAuth Scope权限组
-- =============================================
CREATE TABLE security_oauth_scope
(
    id             VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    scope_code     VARCHAR(128) NOT NULL COMMENT 'Scope完整编码',
    scope_name     VARCHAR(128) NOT NULL COMMENT 'Scope中文名称',
    description    VARCHAR(500) DEFAULT NULL COMMENT '授权页中文描述',
    account_type   VARCHAR(32)  NOT NULL COMMENT '账号体系',
    status         VARCHAR(32)  NOT NULL DEFAULT 'ENABLED' COMMENT '状态',
    tenant_id      VARCHAR(50)  DEFAULT NULL COMMENT '租户ID',
    create_op      VARCHAR(50)  DEFAULT NULL COMMENT '创建人',
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op      VARCHAR(50)  DEFAULT NULL COMMENT '修改人',
    modify_time    DATETIME     DEFAULT NULL COMMENT '修改时间',
    deleted        SMALLINT     NOT NULL DEFAULT 0 COMMENT '删除标识',
    delete_op      VARCHAR(50)  DEFAULT NULL COMMENT '删除人',
    delete_time    DATETIME     DEFAULT NULL COMMENT '删除时间'
) COMMENT 'OAuth Scope权限组';

CREATE UNIQUE INDEX uk_security_oauth_scope_code ON security_oauth_scope (scope_code);
CREATE INDEX idx_security_oauth_scope_account_status ON security_oauth_scope (account_type, status);

CREATE TABLE security_oauth_scope_resource
(
    id              VARCHAR(24) PRIMARY KEY COMMENT '主键ID',
    scope_id        VARCHAR(24) NOT NULL COMMENT 'Scope ID',
    api_resource_id VARCHAR(64) NOT NULL COMMENT '接口资源ID',
    tenant_id       VARCHAR(50) DEFAULT NULL COMMENT '租户ID',
    create_op       VARCHAR(50) DEFAULT NULL COMMENT '创建人',
    create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_op       VARCHAR(50) DEFAULT NULL COMMENT '修改人',
    modify_time     DATETIME    DEFAULT NULL COMMENT '修改时间',
    deleted         SMALLINT    NOT NULL DEFAULT 0 COMMENT '删除标识',
    delete_op       VARCHAR(50) DEFAULT NULL COMMENT '删除人',
    delete_time     DATETIME    DEFAULT NULL COMMENT '删除时间',
    CONSTRAINT fk_oauth_scope_resource_scope FOREIGN KEY (scope_id) REFERENCES security_oauth_scope (id) ON DELETE CASCADE
) COMMENT 'OAuth Scope与接口资源关联表';

CREATE UNIQUE INDEX uk_oauth_scope_resource ON security_oauth_scope_resource (scope_id, api_resource_id);
CREATE INDEX idx_oauth_scope_resource_api ON security_oauth_scope_resource (api_resource_id);
