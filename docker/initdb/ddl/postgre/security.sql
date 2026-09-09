-- =============================================
-- 菜单管理相关表结构
-- 数据库：PostgreSQL
-- =============================================

-- =============================================
-- 表名：security_menu
-- 说明：菜单表
-- =============================================
CREATE TABLE security_menu
(
    id          VARCHAR(24) PRIMARY KEY,
    parent_id   VARCHAR(24)          DEFAULT NULL,
    menu_name   VARCHAR(50) NOT NULL,
    menu_type   SMALLINT    NOT NULL DEFAULT 1,
    sort        INT         NOT NULL DEFAULT 0,
    icon        VARCHAR(100)         DEFAULT NULL,
    path        VARCHAR(200)         DEFAULT NULL,
    visible     SMALLINT    NOT NULL DEFAULT 1,
    status      SMALLINT    NOT NULL DEFAULT 1,
    permission  VARCHAR(500)         DEFAULT NULL,
    position    INT                  DEFAULT NULL,
    owner       INT                  DEFAULT NULL,
    button_key  VARCHAR(100)         DEFAULT NULL,
    description VARCHAR(1024)        DEFAULT NULL,
    tenant_id   VARCHAR(50)          DEFAULT NULL,
    create_op   VARCHAR(50)          DEFAULT NULL,
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)          DEFAULT NULL,
    modify_time TIMESTAMP            DEFAULT NULL,
    deleted     INT2        NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)          DEFAULT NULL,
    delete_time TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_menu IS '菜单表';
COMMENT ON COLUMN security_menu.id IS '主键ID';
COMMENT ON COLUMN security_menu.parent_id IS '父菜单ID，NULL表示顶级菜单';
COMMENT ON COLUMN security_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN security_menu.menu_type IS '菜单类型：1-目录 2-菜单 3-按钮';
COMMENT ON COLUMN security_menu.sort IS '排序号';
COMMENT ON COLUMN security_menu.icon IS '菜单图标';
COMMENT ON COLUMN security_menu.path IS '路由路径';
COMMENT ON COLUMN security_menu.visible IS '是否显示：0-隐藏 1-显示';
COMMENT ON COLUMN security_menu.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN security_menu.permission IS '权限标识';
COMMENT ON COLUMN security_menu.position IS '菜单位置类型：1-侧边栏 2-顶部栏';
COMMENT ON COLUMN security_menu.owner IS '菜单所属类型：1-后端管理 2-移动端APP';
COMMENT ON COLUMN security_menu.button_key IS '按钮标识，格式：菜单ID_标识，用于前端按钮显示控制';
COMMENT ON COLUMN security_menu.description IS '功能描述，用于AI提示词构建';
COMMENT ON COLUMN security_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN security_menu.create_op IS '创建人';
COMMENT ON COLUMN security_menu.create_time IS '创建时间';
COMMENT ON COLUMN security_menu.modify_op IS '修改人';
COMMENT ON COLUMN security_menu.modify_time IS '修改时间';
COMMENT ON COLUMN security_menu.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_menu.delete_op IS '删除人';
COMMENT ON COLUMN security_menu.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_menu_parent_id ON security_menu (parent_id) WHERE deleted = 0;
CREATE INDEX idx_security_menu_sort ON security_menu (sort);
CREATE INDEX idx_security_menu_type ON security_menu (menu_type);

-- =============================================
-- 表名：security_role
-- 说明：角色表
-- =============================================
CREATE TABLE security_role
(
    id          VARCHAR(24) PRIMARY KEY,
    role_name   VARCHAR(50) NOT NULL,
    role_code   VARCHAR(50) NOT NULL,
    sort        INT         NOT NULL DEFAULT 0,
    description VARCHAR(200)         DEFAULT NULL,
    status      SMALLINT    NOT NULL DEFAULT 1,
    role_type   INT2        NOT NULL DEFAULT 2,
    data_scope  INT2        NOT NULL DEFAULT 1,
    tenant_id   VARCHAR(50)          DEFAULT NULL,
    create_op   VARCHAR(50)          DEFAULT NULL,
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)          DEFAULT NULL,
    modify_time TIMESTAMP            DEFAULT NULL,
    deleted     INT2        NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)          DEFAULT NULL,
    delete_time TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role IS '角色表';
COMMENT ON COLUMN security_role.id IS '主键ID';
COMMENT ON COLUMN security_role.role_name IS '角色名称';
COMMENT ON COLUMN security_role.role_code IS '角色编码';
COMMENT ON COLUMN security_role.sort IS '排序号';
COMMENT ON COLUMN security_role.description IS '角色描述';
COMMENT ON COLUMN security_role.status IS '状态：0-禁用 1-正常';
COMMENT ON COLUMN security_role.role_type IS '角色类型：1-系统角色 2-业务角色';
COMMENT ON COLUMN security_role.data_scope IS '数据范围：0-自定义 1-全部数据 2-本部门及以下 3-本部门 4-仅本人';
COMMENT ON COLUMN security_role.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role.create_op IS '创建人';
COMMENT ON COLUMN security_role.create_time IS '创建时间';
COMMENT ON COLUMN security_role.modify_op IS '修改人';
COMMENT ON COLUMN security_role.modify_time IS '修改时间';
COMMENT ON COLUMN security_role.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role.delete_op IS '删除人';
COMMENT ON COLUMN security_role.delete_time IS '删除时间';

-- 唯一索引
CREATE UNIQUE INDEX uk_security_role_code ON security_role (role_code) WHERE deleted = 0;

-- =============================================
-- 表名：security_role_subject
-- 说明：主体角色关联表
-- =============================================
CREATE TABLE security_role_subject
(
    id          VARCHAR(24) PRIMARY KEY,
    subject_id  VARCHAR(24) NOT NULL,
    role_id     VARCHAR(24) NOT NULL,
    tenant_id   VARCHAR(50)          DEFAULT NULL,
    create_op   VARCHAR(50)          DEFAULT NULL,
    create_time TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)          DEFAULT NULL,
    modify_time TIMESTAMP            DEFAULT NULL,
    deleted     INT2        NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)          DEFAULT NULL,
    delete_time TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role_subject IS '主体角色关联表';
COMMENT ON COLUMN security_role_subject.id IS '主键ID';
COMMENT ON COLUMN security_role_subject.subject_id IS '主体ID（用户ID）';
COMMENT ON COLUMN security_role_subject.role_id IS '角色ID';
COMMENT ON COLUMN security_role_subject.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_subject.create_op IS '创建人';
COMMENT ON COLUMN security_role_subject.create_time IS '创建时间';
COMMENT ON COLUMN security_role_subject.modify_op IS '修改人';
COMMENT ON COLUMN security_role_subject.modify_time IS '修改时间';
COMMENT ON COLUMN security_role_subject.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role_subject.delete_op IS '删除人';
COMMENT ON COLUMN security_role_subject.delete_time IS '删除时间';


-- 索引
CREATE UNIQUE INDEX uk_security_role_subject ON security_role_subject (subject_id, role_id);
CREATE INDEX idx_security_role_subject_id ON security_role_subject (role_id);
-- 外键
ALTER TABLE security_role_subject add CONSTRAINT role_subject_fk FOREIGN key (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_role_menu
-- 说明：角色菜单关联表
-- =============================================
CREATE TABLE security_role_menu
(
    id               VARCHAR(24) PRIMARY KEY,
    role_id          VARCHAR(24) NOT NULL,
    menu_id          VARCHAR(24) NOT NULL,
    valid_type       INT2        NOT NULL DEFAULT 1,
    valid_start      TIMESTAMP            DEFAULT NULL,
    valid_end        TIMESTAMP            DEFAULT NULL,
    cycle_type       INT2                 DEFAULT NULL,
    cycle_value      VARCHAR(100)         DEFAULT NULL,
    cycle_start_time VARCHAR(10)          DEFAULT NULL,
    cycle_end_time   VARCHAR(10)          DEFAULT NULL,
    tenant_id        VARCHAR(50)          DEFAULT NULL,
    create_op        VARCHAR(50)          DEFAULT NULL,
    create_time      TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op        VARCHAR(50)          DEFAULT NULL,
    modify_time      TIMESTAMP            DEFAULT NULL,
    deleted          INT2        NOT NULL DEFAULT 0,
    delete_op        VARCHAR(50)          DEFAULT NULL,
    delete_time      TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN security_role_menu.id IS '主键ID';
COMMENT ON COLUMN security_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN security_role_menu.menu_id IS '菜单ID';
COMMENT ON COLUMN security_role_menu.valid_type IS '时效类型：1-永久 2-绝对时间范围 3-周期性';
COMMENT ON COLUMN security_role_menu.valid_start IS '绝对时间-开始时间';
COMMENT ON COLUMN security_role_menu.valid_end IS '绝对时间-结束时间';
COMMENT ON COLUMN security_role_menu.cycle_type IS '周期类型：1-按周 2-按月';
COMMENT ON COLUMN security_role_menu.cycle_value IS '周期值：按周存1,2,3,4,5 按月存1,15';
COMMENT ON COLUMN security_role_menu.cycle_start_time IS '周期-每日开始时间';
COMMENT ON COLUMN security_role_menu.cycle_end_time IS '周期-每日结束时间';
COMMENT ON COLUMN security_role_menu.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_menu.create_op IS '创建人';
COMMENT ON COLUMN security_role_menu.create_time IS '创建时间';
COMMENT ON COLUMN security_role_menu.modify_op IS '修改人';
COMMENT ON COLUMN security_role_menu.modify_time IS '修改时间';
COMMENT ON COLUMN security_role_menu.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role_menu.delete_op IS '删除人';
COMMENT ON COLUMN security_role_menu.delete_time IS '删除时间';


-- 索引
CREATE INDEX idx_security_role_menu_id ON security_role_menu (menu_id);
-- 外键
ALTER TABLE security_role_menu add CONSTRAINT role_menu_role_menu_id_fk FOREIGN key (menu_id) REFERENCES security_menu(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE security_role_menu add CONSTRAINT role_menu_roleid_fk FOREIGN key (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_abac
-- 说明：ABAC表达式表
-- =============================================
CREATE TABLE security_abac
(
    id          VARCHAR(24) PRIMARY KEY,
    expression  VARCHAR(500) NOT NULL,
    description VARCHAR(200)          DEFAULT NULL,
    status      INT2         NOT NULL DEFAULT 1,
    tenant_id   VARCHAR(50)           DEFAULT NULL,
    create_op   VARCHAR(50)           DEFAULT NULL,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)           DEFAULT NULL,
    modify_time TIMESTAMP             DEFAULT NULL,
    deleted     INT2         NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)           DEFAULT NULL,
    delete_time TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac IS 'ABAC表达式表';
COMMENT ON COLUMN security_abac.id IS '主键ID';
COMMENT ON COLUMN security_abac.expression IS '表达式内容';
COMMENT ON COLUMN security_abac.description IS '表达式描述';
COMMENT ON COLUMN security_abac.status IS '启用状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac.create_op IS '创建人';
COMMENT ON COLUMN security_abac.create_time IS '创建时间';
COMMENT ON COLUMN security_abac.modify_op IS '修改人';
COMMENT ON COLUMN security_abac.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac.delete_op IS '删除人';
COMMENT ON COLUMN security_abac.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_expression ON security_abac (expression);

-- =============================================
-- 表名：security_abac_permission
-- 说明：ABAC接口权限关联表
-- =============================================
CREATE TABLE security_abac_permission
(
    id            VARCHAR(24) PRIMARY KEY,
    abac_id       VARCHAR(24)  NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    url_pattern   VARCHAR(500) NOT NULL,
    effect        VARCHAR(20)  NOT NULL DEFAULT 'allow',
    status        INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac_permission IS 'ABAC接口权限关联表';
COMMENT ON COLUMN security_abac_permission.id IS '主键ID';
COMMENT ON COLUMN security_abac_permission.abac_id IS '表达式ID';
COMMENT ON COLUMN security_abac_permission.resource_type IS '资源类型';
COMMENT ON COLUMN security_abac_permission.action IS '操作';
COMMENT ON COLUMN security_abac_permission.url_pattern IS 'URL模式';
COMMENT ON COLUMN security_abac_permission.effect IS '效果：allow-允许 deny-拒绝';
COMMENT ON COLUMN security_abac_permission.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac_permission.create_op IS '创建人';
COMMENT ON COLUMN security_abac_permission.create_time IS '创建时间';
COMMENT ON COLUMN security_abac_permission.modify_op IS '修改人';
COMMENT ON COLUMN security_abac_permission.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac_permission.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac_permission.delete_op IS '删除人';
COMMENT ON COLUMN security_abac_permission.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_permission_abac_id ON security_abac_permission (abac_id);
CREATE INDEX idx_security_abac_permission_resource_type ON security_abac_permission (resource_type);
CREATE INDEX idx_security_abac_permission_url_pattern ON security_abac_permission (url_pattern);
-- 外键
alter table security_abac_permission add CONSTRAINT abac_permission_fk FOREIGN key (abac_id) REFERENCES security_abac(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_abac_field
-- 说明：ABAC字段权限关联表
-- =============================================
CREATE TABLE security_abac_field
(
    id            VARCHAR(24) PRIMARY KEY,
    abac_id       VARCHAR(24)  NOT NULL,
    resource_type VARCHAR(50)  NOT NULL,
    action        VARCHAR(50)  NOT NULL,
    url_pattern   VARCHAR(500) NOT NULL,
    field_mode    VARCHAR(20)  NOT NULL DEFAULT 'allow',
    fields        TEXT                  DEFAULT NULL,
    status        INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_abac_field IS 'ABAC字段权限关联表';
COMMENT ON COLUMN security_abac_field.id IS '主键ID';
COMMENT ON COLUMN security_abac_field.abac_id IS '表达式ID';
COMMENT ON COLUMN security_abac_field.resource_type IS '资源类型';
COMMENT ON COLUMN security_abac_field.action IS '操作';
COMMENT ON COLUMN security_abac_field.url_pattern IS 'URL';
COMMENT ON COLUMN security_abac_field.field_mode IS '字段模式：allow-允许 deny-拒绝';
COMMENT ON COLUMN security_abac_field.fields IS '字段列表，JSON数组格式';
COMMENT ON COLUMN security_abac_field.status IS '状态：0-禁用 1-启用';
COMMENT ON COLUMN security_abac_field.tenant_id IS '租户ID';
COMMENT ON COLUMN security_abac_field.create_op IS '创建人';
COMMENT ON COLUMN security_abac_field.create_time IS '创建时间';
COMMENT ON COLUMN security_abac_field.modify_op IS '修改人';
COMMENT ON COLUMN security_abac_field.modify_time IS '修改时间';
COMMENT ON COLUMN security_abac_field.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_abac_field.delete_op IS '删除人';
COMMENT ON COLUMN security_abac_field.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_abac_field_abac_id ON security_abac_field (abac_id);
CREATE INDEX idx_security_abac_field_resource_type ON security_abac_field (resource_type);
CREATE INDEX idx_security_abac_field_url_pattern ON security_abac_field (url_pattern);
-- 外键
alter table security_abac_field add CONSTRAINT abac_field_fk FOREIGN key (abac_id) REFERENCES security_abac(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_resource
-- 说明：接口资源表
-- =============================================
CREATE TABLE security_api_resource
(
    id                 VARCHAR(64) PRIMARY KEY,
    module_prefix      VARCHAR(50)           DEFAULT '',
    tag_name           VARCHAR(100)          DEFAULT '',
    req_path           VARCHAR(500) NOT NULL,
    req_method         VARCHAR(10)  NOT NULL,
    summary            VARCHAR(200)          DEFAULT '',
    request_class      VARCHAR(500)          DEFAULT '',
    response_class     VARCHAR(500)          DEFAULT '',
    class_name         VARCHAR(200)          DEFAULT NULL,
    method_name        VARCHAR(64)           DEFAULT NULL,
    login_allow_access INT2                  DEFAULT 0,
    tenant_id          VARCHAR(50)           DEFAULT NULL,
    create_op          VARCHAR(50)           DEFAULT NULL,
    create_time        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)           DEFAULT NULL,
    modify_time        TIMESTAMP             DEFAULT NULL,
    deleted            INT2         NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)           DEFAULT NULL,
    delete_time        TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_api_resource IS '接口资源表';
COMMENT ON COLUMN security_api_resource.id IS '主键ID';
COMMENT ON COLUMN security_api_resource.module_prefix IS '模块前缀';
COMMENT ON COLUMN security_api_resource.tag_name IS 'Tag标签名称';
COMMENT ON COLUMN security_api_resource.req_path IS '接口地址';
COMMENT ON COLUMN security_api_resource.req_method IS '请求方式(GET/POST/PUT/DELETE等)';
COMMENT ON COLUMN security_api_resource.summary IS '接口摘要';
COMMENT ON COLUMN security_api_resource.request_class IS '请求参数类型全限定名';
COMMENT ON COLUMN security_api_resource.response_class IS '响应类型全限定名';
COMMENT ON COLUMN security_api_resource.class_name IS '类名';
COMMENT ON COLUMN security_api_resource.method_name IS '方法名';
COMMENT ON COLUMN security_api_resource.login_allow_access IS '登录后允许访问';
COMMENT ON COLUMN security_api_resource.tenant_id IS '租户ID';
COMMENT ON COLUMN security_api_resource.create_op IS '创建人';
COMMENT ON COLUMN security_api_resource.create_time IS '创建时间';
COMMENT ON COLUMN security_api_resource.modify_op IS '修改人';
COMMENT ON COLUMN security_api_resource.modify_time IS '修改时间';
COMMENT ON COLUMN security_api_resource.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_api_resource.delete_op IS '删除人';
COMMENT ON COLUMN security_api_resource.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_api_resource_module_prefix ON security_api_resource (module_prefix) WHERE deleted = 0;
CREATE INDEX idx_security_api_resource_req_path_method ON security_api_resource (req_path, req_method) WHERE deleted = 0;
CREATE INDEX idx_security_api_resource_tag_name ON security_api_resource (tag_name) WHERE deleted = 0;

-- =============================================
-- 表名：security_data_resource
-- 说明：数据资源配置主表
-- =============================================
CREATE TABLE security_data_resource
(
    id                VARCHAR(24) PRIMARY KEY,
    catalog_name      VARCHAR(100)          DEFAULT NULL,
    schema_name       VARCHAR(100)          DEFAULT NULL,
    table_name        VARCHAR(100) NOT NULL,
    description       VARCHAR(200)          DEFAULT NULL,
    support_self_only INT2         NOT NULL DEFAULT 0,
    self_only_field   VARCHAR(100)          DEFAULT NULL,
    status            INT2         NOT NULL DEFAULT 1,
    tenant_id         VARCHAR(50)           DEFAULT NULL,
    create_op         VARCHAR(50)           DEFAULT NULL,
    create_time       TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op         VARCHAR(50)           DEFAULT NULL,
    modify_time       TIMESTAMP             DEFAULT NULL,
    deleted           INT2         NOT NULL DEFAULT 0,
    delete_op         VARCHAR(50)           DEFAULT NULL,
    delete_time       TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_data_resource IS '数据资源配置主表';
COMMENT ON COLUMN security_data_resource.id IS '主键ID';
COMMENT ON COLUMN security_data_resource.catalog_name IS 'Catalog 名称，为空时匹配所有 Catalog';
COMMENT ON COLUMN security_data_resource.schema_name IS '数据库/Schema 名称，为空时匹配所有数据库或 Schema';
COMMENT ON COLUMN security_data_resource.table_name IS '表名';
COMMENT ON COLUMN security_data_resource.description IS '规则描述';
COMMENT ON COLUMN security_data_resource.support_self_only IS '是否支持SELF_ONLY过滤：0-不支持 1-支持';
COMMENT ON COLUMN security_data_resource.self_only_field IS 'SELF_ONLY过滤时使用的字段名，即目标表中记录创建人的字段';
COMMENT ON COLUMN security_data_resource.status IS '启用状态：0-禁用 1-启用';
COMMENT ON COLUMN security_data_resource.tenant_id IS '租户ID';
COMMENT ON COLUMN security_data_resource.create_op IS '创建人';
COMMENT ON COLUMN security_data_resource.create_time IS '创建时间';
COMMENT ON COLUMN security_data_resource.modify_op IS '修改人';
COMMENT ON COLUMN security_data_resource.modify_time IS '修改时间';
COMMENT ON COLUMN security_data_resource.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_data_resource.delete_op IS '删除人';
COMMENT ON COLUMN security_data_resource.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_data_resource_table_name ON security_data_resource (table_name) WHERE deleted = 0;
CREATE INDEX idx_security_data_resource_catalog_name ON security_data_resource (catalog_name) WHERE deleted = 0;
CREATE INDEX idx_security_data_resource_schema_name ON security_data_resource (schema_name) WHERE deleted = 0;

-- =============================================
-- 表名：security_data_resource_condition
-- 说明：数据资源字段条件配置表
-- =============================================
CREATE TABLE security_data_resource_condition
(
    id                   VARCHAR(24) PRIMARY KEY,
    data_resource_id     VARCHAR(24)  NOT NULL,
    field_name           VARCHAR(100) NOT NULL,
    show_null            INT2         NOT NULL DEFAULT 0,
    user_resource_fields VARCHAR(500)          DEFAULT NULL,
    assert_type          VARCHAR(20)  NOT NULL DEFAULT 'EQ',
    relationship         VARCHAR(10)  NOT NULL DEFAULT 'AND',
    sort                 INT          NOT NULL DEFAULT 0,
    tenant_id            VARCHAR(50)           DEFAULT NULL,
    create_op            VARCHAR(50)           DEFAULT NULL,
    create_time          TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op            VARCHAR(50)           DEFAULT NULL,
    modify_time          TIMESTAMP             DEFAULT NULL,
    deleted              INT2         NOT NULL DEFAULT 0,
    delete_op            VARCHAR(50)           DEFAULT NULL,
    delete_time          TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_data_resource_condition IS '数据资源字段条件配置表';
COMMENT ON COLUMN security_data_resource_condition.id IS '主键ID';
COMMENT ON COLUMN security_data_resource_condition.data_resource_id IS '数据资源配置ID';
COMMENT ON COLUMN security_data_resource_condition.field_name IS '字段名';
COMMENT ON COLUMN security_data_resource_condition.show_null IS '显示过滤字段为null的数据：0-不显示 1-显示';
COMMENT ON COLUMN security_data_resource_condition.user_resource_fields IS '关联的用户数据资源字段，多个用逗号分隔';
COMMENT ON COLUMN security_data_resource_condition.assert_type IS '断言类型：EQ-等于 LIKE-模糊匹配';
COMMENT ON COLUMN security_data_resource_condition.relationship IS '与上一个条件的关联关系：AND-与 OR-或';
COMMENT ON COLUMN security_data_resource_condition.sort IS '排序号';
COMMENT ON COLUMN security_data_resource_condition.tenant_id IS '租户ID';
COMMENT ON COLUMN security_data_resource_condition.create_op IS '创建人';
COMMENT ON COLUMN security_data_resource_condition.create_time IS '创建时间';
COMMENT ON COLUMN security_data_resource_condition.modify_op IS '修改人';
COMMENT ON COLUMN security_data_resource_condition.modify_time IS '修改时间';
COMMENT ON COLUMN security_data_resource_condition.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_data_resource_condition.delete_op IS '删除人';
COMMENT ON COLUMN security_data_resource_condition.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_data_resource_condition_data_resource_id ON security_data_resource_condition (data_resource_id);
-- 外键
ALTER TABLE security_data_resource_condition add CONSTRAINT data_resource_condition_fk FOREIGN key (data_resource_id) REFERENCES security_data_resource(id) ON DELETE CASCADE ON UPDATE CASCADE;


-- =============================================
-- 表名：security_role_menu_permission
-- 说明：角色菜单权限关联表
-- =============================================
CREATE TABLE security_role_menu_permission
(
    id                 VARCHAR(24) PRIMARY KEY,
    role_menu_id       VARCHAR(24) NOT NULL,
    abac_permission_id VARCHAR(24) NOT NULL,
    api_id             VARCHAR(64) NOT NULL,
    tenant_id          VARCHAR(50)          DEFAULT NULL,
    create_op          VARCHAR(50)          DEFAULT NULL,
    create_time        TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)          DEFAULT NULL,
    modify_time        TIMESTAMP            DEFAULT NULL,
    deleted            INT2        NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)          DEFAULT NULL,
    delete_time        TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role_menu_permission IS '角色菜单权限关联表';
COMMENT ON COLUMN security_role_menu_permission.id IS '主键ID';
COMMENT ON COLUMN security_role_menu_permission.role_menu_id IS '角色菜单关联ID，关联security_role_menu表';
COMMENT ON COLUMN security_role_menu_permission.abac_permission_id IS 'ABAC接口权限ID，关联security_abac_permission表';
COMMENT ON COLUMN security_role_menu_permission.api_id IS '接口资源ID，关联security_api_resource表';
COMMENT ON COLUMN security_role_menu_permission.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_menu_permission.create_op IS '创建人';
COMMENT ON COLUMN security_role_menu_permission.create_time IS '创建时间';
COMMENT ON COLUMN security_role_menu_permission.modify_op IS '修改人';
COMMENT ON COLUMN security_role_menu_permission.modify_time IS '修改时间';
COMMENT ON COLUMN security_role_menu_permission.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role_menu_permission.delete_op IS '删除人';
COMMENT ON COLUMN security_role_menu_permission.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_role_menu_permission_role_menu_id ON security_role_menu_permission (role_menu_id);
CREATE INDEX idx_security_role_menu_permission_abac_permission_id ON security_role_menu_permission (abac_permission_id);
-- 外键
ALTER TABLE security_role_menu_permission add CONSTRAINT role_menu_permission_fk FOREIGN key (role_menu_id) REFERENCES security_role_menu(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_table_model
-- 说明：接口-表模型绑定表（注解采集，启动时覆盖）
-- =============================================
CREATE TABLE security_api_table_model
(
    id            VARCHAR(64) PRIMARY KEY,
    api_id        VARCHAR(64)  NOT NULL,
    module_prefix VARCHAR(50)  NOT NULL DEFAULT '',
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master',
    table_name    VARCHAR(100) NOT NULL,
    field_config  TEXT                  DEFAULT NULL,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_api_table_model IS '接口-表模型绑定表（注解采集，启动时覆盖）';
COMMENT ON COLUMN security_api_table_model.id IS '主键ID，MD5(module_prefix + datasource + table_name + api_id)';
COMMENT ON COLUMN security_api_table_model.api_id IS '接口资源ID，关联security_api_resource.id';
COMMENT ON COLUMN security_api_table_model.module_prefix IS '模块前缀（服务标识）';
COMMENT ON COLUMN security_api_table_model.datasource IS '数据源名称';
COMMENT ON COLUMN security_api_table_model.table_name IS '表模型名称';
COMMENT ON COLUMN security_api_table_model.field_config IS '字段配置JSON，仅记录注解标识的字段配置';
COMMENT ON COLUMN security_api_table_model.tenant_id IS '租户ID';
COMMENT ON COLUMN security_api_table_model.create_op IS '创建人';
COMMENT ON COLUMN security_api_table_model.create_time IS '创建时间';
COMMENT ON COLUMN security_api_table_model.modify_op IS '修改人';
COMMENT ON COLUMN security_api_table_model.modify_time IS '修改时间';
COMMENT ON COLUMN security_api_table_model.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_api_table_model.delete_op IS '删除人';
COMMENT ON COLUMN security_api_table_model.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_api_table_model_api_id ON security_api_table_model (api_id);
CREATE INDEX idx_security_api_table_model_table_name ON security_api_table_model (table_name);
CREATE INDEX idx_security_api_table_model_module_prefix ON security_api_table_model (module_prefix);
CREATE INDEX idx_security_api_table_model_module_datasource_table ON security_api_table_model (module_prefix, datasource, table_name);
-- 外键
alter table security_api_table_model add CONSTRAINT table_model_fk FOREIGN key (api_id) REFERENCES security_api_resource(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_api_table_model_config
-- 说明：表模型手动配置表（持久化，启动不覆盖）
-- =============================================
CREATE TABLE security_api_table_model_config
(
    id             VARCHAR(24) PRIMARY KEY,
    table_model_id VARCHAR(64)          DEFAULT NULL,
    datasource     VARCHAR(50) NOT NULL,
    tenant_id      VARCHAR(50)          DEFAULT NULL,
    create_op      VARCHAR(50)          DEFAULT NULL,
    create_time    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op      VARCHAR(50)          DEFAULT NULL,
    modify_time    TIMESTAMP            DEFAULT NULL,
    deleted        INT2        NOT NULL DEFAULT 0,
    delete_op      VARCHAR(50)          DEFAULT NULL,
    delete_time    TIMESTAMP            DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_api_table_model_config IS '表模型手动配置表（持久化，启动不覆盖）';
COMMENT ON COLUMN security_api_table_model_config.id IS '主键ID';
COMMENT ON COLUMN security_api_table_model_config.table_model_id IS '关联security_api_table_model的ID';
COMMENT ON COLUMN security_api_table_model_config.datasource IS '数据源名称';
COMMENT ON COLUMN security_api_table_model_config.tenant_id IS '租户ID';
COMMENT ON COLUMN security_api_table_model_config.create_op IS '创建人';
COMMENT ON COLUMN security_api_table_model_config.create_time IS '创建时间';
COMMENT ON COLUMN security_api_table_model_config.modify_op IS '修改人';
COMMENT ON COLUMN security_api_table_model_config.modify_time IS '修改时间';
COMMENT ON COLUMN security_api_table_model_config.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_api_table_model_config.delete_op IS '删除人';
COMMENT ON COLUMN security_api_table_model_config.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_api_table_model_config_table_model_id ON security_api_table_model_config (table_model_id);
-- 外键
alter table security_api_table_model_config add CONSTRAINT table_model_config_fk FOREIGN key (table_model_id) REFERENCES security_api_table_model(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_role_table_model
-- 说明：角色表模型权限配置表
-- =============================================
CREATE TABLE security_role_table_model
(
    id            VARCHAR(24) PRIMARY KEY,
    role_id       VARCHAR(24)  NOT NULL,
    module_prefix VARCHAR(50)  NOT NULL DEFAULT '',
    table_name    VARCHAR(100) NOT NULL,
    datasource    VARCHAR(50)  NOT NULL DEFAULT 'master',
    field_config  TEXT                  DEFAULT NULL,
    enabled       INT2         NOT NULL DEFAULT 1,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_role_table_model IS '角色表模型权限配置表';
COMMENT ON COLUMN security_role_table_model.id IS '主键ID';
COMMENT ON COLUMN security_role_table_model.role_id IS '角色ID';
COMMENT ON COLUMN security_role_table_model.module_prefix IS '模块前缀（服务标识）';
COMMENT ON COLUMN security_role_table_model.table_name IS '表模型名称';
COMMENT ON COLUMN security_role_table_model.datasource IS '数据源名称';
COMMENT ON COLUMN security_role_table_model.field_config IS '字段限制配置JSON，仅存储限制性配置';
COMMENT ON COLUMN security_role_table_model.enabled IS '是否启用：0-禁用 1-启用，仅对接口关联的表模型有效';
COMMENT ON COLUMN security_role_table_model.tenant_id IS '租户ID';
COMMENT ON COLUMN security_role_table_model.create_op IS '创建人';
COMMENT ON COLUMN security_role_table_model.create_time IS '创建时间';
COMMENT ON COLUMN security_role_table_model.modify_op IS '修改人';
COMMENT ON COLUMN security_role_table_model.modify_time IS '修改时间';
COMMENT ON COLUMN security_role_table_model.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_role_table_model.delete_op IS '删除人';
COMMENT ON COLUMN security_role_table_model.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_role_table_model_role_id ON security_role_table_model (role_id);
CREATE INDEX idx_security_role_table_model_table_name ON security_role_table_model (table_name);
CREATE UNIQUE INDEX uk_security_role_table_model ON security_role_table_model (role_id, module_prefix, datasource, table_name);
-- 外键
ALTER TABLE security_role_table_model add CONSTRAINT role_table_model_roleid_fk FOREIGN key (role_id) REFERENCES security_role(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_tablemodel_tables
-- 说明：表基本信息
-- =============================================
CREATE TABLE security_tablemodel_tables
(
    id            VARCHAR(64) PRIMARY KEY,
    table_name    VARCHAR(128) NOT NULL,
    module_prefix VARCHAR(64)           DEFAULT NULL,
    data_source   VARCHAR(64)  NOT NULL,
    table_comment TEXT                  DEFAULT NULL,
    source_type   INT2         NOT NULL DEFAULT 0,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE security_tablemodel_tables IS '表基本信息';
COMMENT ON COLUMN security_tablemodel_tables.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN security_tablemodel_tables.table_name IS '表名';
COMMENT ON COLUMN security_tablemodel_tables.module_prefix IS '模块前缀';
COMMENT ON COLUMN security_tablemodel_tables.data_source IS '数据源';
COMMENT ON COLUMN security_tablemodel_tables.table_comment IS '表注释';
COMMENT ON COLUMN security_tablemodel_tables.source_type IS '来源类型：0-采集 1-自定义添加';
COMMENT ON COLUMN security_tablemodel_tables.tenant_id IS '租户ID';
COMMENT ON COLUMN security_tablemodel_tables.create_op IS '创建人';
COMMENT ON COLUMN security_tablemodel_tables.create_time IS '创建时间';
COMMENT ON COLUMN security_tablemodel_tables.modify_op IS '修改人';
COMMENT ON COLUMN security_tablemodel_tables.modify_time IS '修改时间';
COMMENT ON COLUMN security_tablemodel_tables.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_tablemodel_tables.delete_op IS '删除人';
COMMENT ON COLUMN security_tablemodel_tables.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_tablemodel_tables_table_datasource ON security_tablemodel_tables (table_name, data_source);

-- =============================================
-- 表名：security_tablemodel_columns
-- 说明：字段详细信息
-- =============================================
CREATE TABLE security_tablemodel_columns
(
    id               VARCHAR(24) PRIMARY KEY,
    table_id         VARCHAR(64)  NOT NULL,
    column_name      VARCHAR(128) NOT NULL,
    column_type      VARCHAR(64)  NOT NULL,
    column_length    INT                   DEFAULT NULL,
    column_scale     INT                   DEFAULT NULL,
    is_nullable      INT2         NOT NULL DEFAULT 1,
    is_primary_key   INT2         NOT NULL DEFAULT 0,
    pk_position      INT                   DEFAULT 0,
    default_value    TEXT                  DEFAULT NULL,
    column_comment   TEXT                  DEFAULT NULL,
    ordinal_position INT                   DEFAULT 0,
    field_config     VARCHAR(512)          DEFAULT NULL,
    dict_key         VARCHAR(100)          DEFAULT NULL,
    tenant_id        VARCHAR(50)           DEFAULT NULL,
    create_op        VARCHAR(50)           DEFAULT NULL,
    create_time      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op        VARCHAR(50)           DEFAULT NULL,
    modify_time      TIMESTAMP             DEFAULT NULL,
    deleted          INT2         NOT NULL DEFAULT 0,
    delete_op        VARCHAR(50)           DEFAULT NULL,
    delete_time      TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE security_tablemodel_columns IS '字段详细信息';
COMMENT ON COLUMN security_tablemodel_columns.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN security_tablemodel_columns.table_id IS '关联表ID';
COMMENT ON COLUMN security_tablemodel_columns.column_name IS '字段名';
COMMENT ON COLUMN security_tablemodel_columns.column_type IS '字段类型';
COMMENT ON COLUMN security_tablemodel_columns.column_length IS '字段长度';
COMMENT ON COLUMN security_tablemodel_columns.column_scale IS '字段精度';
COMMENT ON COLUMN security_tablemodel_columns.is_nullable IS '是否可空：0-否 1-是';
COMMENT ON COLUMN security_tablemodel_columns.is_primary_key IS '是否主键：0-否 1-是';
COMMENT ON COLUMN security_tablemodel_columns.pk_position IS '主键位置';
COMMENT ON COLUMN security_tablemodel_columns.default_value IS '默认值';
COMMENT ON COLUMN security_tablemodel_columns.column_comment IS '字段注释';
COMMENT ON COLUMN security_tablemodel_columns.ordinal_position IS '字段顺序';
COMMENT ON COLUMN security_tablemodel_columns.field_config IS '字段AI权限配置';
COMMENT ON COLUMN security_tablemodel_columns.dict_key IS '字典键（绑定枚举值）';
COMMENT ON COLUMN security_tablemodel_columns.tenant_id IS '租户ID';
COMMENT ON COLUMN security_tablemodel_columns.create_op IS '创建人';
COMMENT ON COLUMN security_tablemodel_columns.create_time IS '创建时间';
COMMENT ON COLUMN security_tablemodel_columns.modify_op IS '修改人';
COMMENT ON COLUMN security_tablemodel_columns.modify_time IS '修改时间';
COMMENT ON COLUMN security_tablemodel_columns.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_tablemodel_columns.delete_op IS '删除人';
COMMENT ON COLUMN security_tablemodel_columns.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_tablemodel_columns_table_column ON security_tablemodel_columns (table_id, column_name);
CREATE INDEX idx_security_tablemodel_columns_table_id ON security_tablemodel_columns (table_id);
-- 外键
ALTER TABLE security_tablemodel_columns add CONSTRAINT tablemodel_column_fk FOREIGN key (table_id) REFERENCES security_tablemodel_tables(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_tablemodel_foreign_keys
-- 说明：外键约束信息（单列外键）
-- =============================================
CREATE TABLE security_tablemodel_foreign_keys
(
    id                     VARCHAR(24) PRIMARY KEY,
    constraint_name        VARCHAR(128) NOT NULL,
    table_id               VARCHAR(64)  NOT NULL,
    column_name            VARCHAR(128) NOT NULL,
    referenced_table_name  VARCHAR(128) NOT NULL,
    referenced_column_name VARCHAR(128) NOT NULL,
    update_rule            VARCHAR(16)           DEFAULT 'RESTRICT',
    delete_rule            VARCHAR(16)           DEFAULT 'RESTRICT',
    data_type              INT2         NOT NULL DEFAULT 0,
    remark                 VARCHAR(500)          DEFAULT NULL,
    tenant_id              VARCHAR(50)           DEFAULT NULL,
    create_op              VARCHAR(50)           DEFAULT NULL,
    create_time            TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op              VARCHAR(50)           DEFAULT NULL,
    modify_time            TIMESTAMP             DEFAULT NULL,
    deleted                INT2         NOT NULL DEFAULT 0,
    delete_op              VARCHAR(50)           DEFAULT NULL,
    delete_time            TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE security_tablemodel_foreign_keys IS '外键约束信息（单列外键）';
COMMENT ON COLUMN security_tablemodel_foreign_keys.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN security_tablemodel_foreign_keys.constraint_name IS '约束名称';
COMMENT ON COLUMN security_tablemodel_foreign_keys.table_id IS '所属表ID';
COMMENT ON COLUMN security_tablemodel_foreign_keys.column_name IS '字段名';
COMMENT ON COLUMN security_tablemodel_foreign_keys.referenced_table_name IS '引用表名';
COMMENT ON COLUMN security_tablemodel_foreign_keys.referenced_column_name IS '引用字段名';
COMMENT ON COLUMN security_tablemodel_foreign_keys.update_rule IS '更新规则';
COMMENT ON COLUMN security_tablemodel_foreign_keys.delete_rule IS '删除规则';
COMMENT ON COLUMN security_tablemodel_foreign_keys.data_type IS '数据类型：0-采集 1-自定义添加';
COMMENT ON COLUMN security_tablemodel_foreign_keys.remark IS '备注';
COMMENT ON COLUMN security_tablemodel_foreign_keys.tenant_id IS '租户ID';
COMMENT ON COLUMN security_tablemodel_foreign_keys.create_op IS '创建人';
COMMENT ON COLUMN security_tablemodel_foreign_keys.create_time IS '创建时间';
COMMENT ON COLUMN security_tablemodel_foreign_keys.modify_op IS '修改人';
COMMENT ON COLUMN security_tablemodel_foreign_keys.modify_time IS '修改时间';
COMMENT ON COLUMN security_tablemodel_foreign_keys.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_tablemodel_foreign_keys.delete_op IS '删除人';
COMMENT ON COLUMN security_tablemodel_foreign_keys.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_tablemodel_foreign_keys_table_constraint ON security_tablemodel_foreign_keys (table_id, constraint_name);
CREATE INDEX idx_security_tablemodel_foreign_keys_referenced_table_name ON security_tablemodel_foreign_keys (referenced_table_name);
-- 外键
ALTER TABLE security_tablemodel_foreign_keys add CONSTRAINT tablemodel_foreign_fk FOREIGN key (table_id) REFERENCES security_tablemodel_tables(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：security_config
-- 说明：配置表
-- =============================================
CREATE TABLE security_config
(
    id            VARCHAR(24) PRIMARY KEY,
    config_key    VARCHAR(100) NOT NULL,
    config_name   VARCHAR(100) NOT NULL,
    config_value  TEXT                  DEFAULT NULL,
    value_type    INT2         NOT NULL DEFAULT 1,
    config_type   INT2         NOT NULL DEFAULT 2,
    description   VARCHAR(500)          DEFAULT NULL,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_config IS '配置表';
COMMENT ON COLUMN security_config.id IS '主键ID';
COMMENT ON COLUMN security_config.config_key IS '配置键';
COMMENT ON COLUMN security_config.config_name IS '配置名称';
COMMENT ON COLUMN security_config.config_value IS '配置值，JSON字符串或基本类型值';
COMMENT ON COLUMN security_config.value_type IS '值类型：1-STR 2-NUMBER 3-BOOL 4-JSON';
COMMENT ON COLUMN security_config.config_type IS '配置类型：1-系统 2-自定义';
COMMENT ON COLUMN security_config.description IS '描述';
COMMENT ON COLUMN security_config.tenant_id IS '租户ID';
COMMENT ON COLUMN security_config.create_op IS '创建人';
COMMENT ON COLUMN security_config.create_time IS '创建时间';
COMMENT ON COLUMN security_config.modify_op IS '修改人';
COMMENT ON COLUMN security_config.modify_time IS '修改时间';
COMMENT ON COLUMN security_config.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_config.delete_op IS '删除人';
COMMENT ON COLUMN security_config.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX uk_security_config_key ON security_config (config_key);
CREATE INDEX idx_security_config_type ON security_config (config_type);

-- =============================================
-- 表名：security_dict
-- 说明：字典表
-- =============================================
CREATE TABLE security_dict
(
    id            VARCHAR(24) PRIMARY KEY,
    dict_key      VARCHAR(100) NOT NULL,
    dict_name     VARCHAR(100) NOT NULL,
    dict_type     INT2         NOT NULL DEFAULT 2,
    description   VARCHAR(500)          DEFAULT NULL,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_dict IS '字典表';
COMMENT ON COLUMN security_dict.id IS '主键ID';
COMMENT ON COLUMN security_dict.dict_key IS '字典键';
COMMENT ON COLUMN security_dict.dict_name IS '字典名称';
COMMENT ON COLUMN security_dict.dict_type IS '字典类型：1-系统 2-自定义';
COMMENT ON COLUMN security_dict.description IS '描述';
COMMENT ON COLUMN security_dict.tenant_id IS '租户ID';
COMMENT ON COLUMN security_dict.create_op IS '创建人';
COMMENT ON COLUMN security_dict.create_time IS '创建时间';
COMMENT ON COLUMN security_dict.modify_op IS '修改人';
COMMENT ON COLUMN security_dict.modify_time IS '修改时间';
COMMENT ON COLUMN security_dict.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_dict.delete_op IS '删除人';
COMMENT ON COLUMN security_dict.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX uk_security_dict_key ON security_dict (dict_key);
CREATE INDEX idx_security_dict_type ON security_dict (dict_type);

-- =============================================
-- 表名：security_dict_value
-- 说明：字典值表
-- =============================================
CREATE TABLE security_dict_value
(
    id          VARCHAR(24) PRIMARY KEY,
    dict_key     VARCHAR(24)  NOT NULL,
    dict_label  VARCHAR(256) NOT NULL ,
    dict_value  VARCHAR(500) NOT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    tenant_id   VARCHAR(50)           DEFAULT NULL,
    create_op   VARCHAR(50)           DEFAULT NULL,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)           DEFAULT NULL,
    modify_time TIMESTAMP             DEFAULT NULL,
    deleted     INT2         NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)           DEFAULT NULL,
    delete_time TIMESTAMP             DEFAULT NULL
);

-- 表和字段注释
COMMENT ON TABLE security_dict_value IS '字典值表';
COMMENT ON COLUMN security_dict_value.id IS '主键ID';
COMMENT ON COLUMN security_dict_value.dict_key IS '关联字典KEY';
COMMENT ON COLUMN security_dict_value.dict_label IS '字典标签';
COMMENT ON COLUMN security_dict_value.dict_value IS '字典值';
COMMENT ON COLUMN security_dict_value.sort IS '排序序号';
COMMENT ON COLUMN security_dict_value.tenant_id IS '租户ID';
COMMENT ON COLUMN security_dict_value.create_op IS '创建人';
COMMENT ON COLUMN security_dict_value.create_time IS '创建时间';
COMMENT ON COLUMN security_dict_value.modify_op IS '修改人';
COMMENT ON COLUMN security_dict_value.modify_time IS '修改时间';
COMMENT ON COLUMN security_dict_value.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_dict_value.delete_op IS '删除人';
COMMENT ON COLUMN security_dict_value.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_security_dict_value_dict_id ON security_dict_value (dict_key);



-- =============================================
-- 表名：security_business_function
-- 说明：AI业务功能配置表
-- =============================================
CREATE TABLE security_business_function
(
    id          VARCHAR(24) PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    summary     VARCHAR(512) NOT NULL,
    detail      TEXT         NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    tenant_id   VARCHAR(50)           DEFAULT NULL,
    create_op   VARCHAR(50)           DEFAULT NULL,
    create_time TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op   VARCHAR(50)           DEFAULT NULL,
    modify_time TIMESTAMP             DEFAULT NULL,
    deleted     INT2         NOT NULL DEFAULT 0,
    delete_op   VARCHAR(50)           DEFAULT NULL,
    delete_time TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE security_business_function IS 'AI业务功能配置表';
COMMENT ON COLUMN security_business_function.id IS '主键ID';
COMMENT ON COLUMN security_business_function.name IS '业务名称';
COMMENT ON COLUMN security_business_function.summary IS '业务简介';
COMMENT ON COLUMN security_business_function.detail IS '详细介绍（Markdown格式）';
COMMENT ON COLUMN security_business_function.sort_order IS '排序号';
COMMENT ON COLUMN security_business_function.tenant_id IS '租户ID';
COMMENT ON COLUMN security_business_function.create_op IS '创建人';
COMMENT ON COLUMN security_business_function.create_time IS '创建时间';
COMMENT ON COLUMN security_business_function.modify_op IS '修改人';
COMMENT ON COLUMN security_business_function.modify_time IS '修改时间';
COMMENT ON COLUMN security_business_function.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_business_function.delete_op IS '删除人';
COMMENT ON COLUMN security_business_function.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_business_function_name ON security_business_function (name);

-- =============================================
-- 表名：security_business_function_table
-- 说明：业务功能与表模型关联表
-- =============================================
CREATE TABLE security_business_function_table
(
    id              VARCHAR(24) PRIMARY KEY,
    business_id     VARCHAR(24)  NOT NULL,
    table_model_id  VARCHAR(64)  NOT NULL,
    sort_order      INT          NOT NULL DEFAULT 0,
    tenant_id       VARCHAR(50)            DEFAULT NULL,
    create_op       VARCHAR(50)            DEFAULT NULL,
    create_time     TIMESTAMP               DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)            DEFAULT NULL,
    modify_time     TIMESTAMP               DEFAULT NULL,
    deleted         INT2          NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)            DEFAULT NULL,
    delete_time     TIMESTAMP               DEFAULT NULL
);

COMMENT ON TABLE security_business_function_table IS '业务功能与表模型关联表';
COMMENT ON COLUMN security_business_function_table.id IS '主键ID';
COMMENT ON COLUMN security_business_function_table.business_id IS '业务功能ID';
COMMENT ON COLUMN security_business_function_table.table_model_id IS '表模型ID';
COMMENT ON COLUMN security_business_function_table.sort_order IS '排序号';
COMMENT ON COLUMN security_business_function_table.tenant_id IS '租户ID';
COMMENT ON COLUMN security_business_function_table.create_op IS '创建人';
COMMENT ON COLUMN security_business_function_table.create_time IS '创建时间';
COMMENT ON COLUMN security_business_function_table.modify_op IS '修改人';
COMMENT ON COLUMN security_business_function_table.modify_time IS '修改时间';
COMMENT ON COLUMN security_business_function_table.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_business_function_table.delete_op IS '删除人';
COMMENT ON COLUMN security_business_function_table.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_business_function_table_bt ON security_business_function_table (business_id, table_model_id);
CREATE INDEX idx_security_business_function_table_business ON security_business_function_table (business_id);
CREATE INDEX idx_security_business_function_table_table ON security_business_function_table (table_model_id);

-- =============================================
-- 表名：security_oauth_client
-- 说明：OAuth应用配置表
-- =============================================
CREATE TABLE security_oauth_client
(
    id                             VARCHAR(24) PRIMARY KEY,
    client_id                      VARCHAR(128) NOT NULL,
    client_secret                  VARCHAR(128)          DEFAULT NULL,
    client_name                    VARCHAR(128) NOT NULL,
    client_type                    VARCHAR(32)  NOT NULL,
    account_type                   VARCHAR(32)  NOT NULL DEFAULT 'MANAGER',
    status                         VARCHAR(32)  NOT NULL DEFAULT 'ENABLED',
    client_authentication_methods  TEXT         NOT NULL,
    authorization_grant_types      TEXT         NOT NULL,
    redirect_uris                  TEXT                   DEFAULT NULL,
    post_logout_redirect_uris      TEXT                   DEFAULT NULL,
    scopes                         TEXT         NOT NULL,
    require_proof_key              INT2         NOT NULL DEFAULT 1,
    require_authorization_consent  INT2         NOT NULL DEFAULT 1,
    access_token_ttl_seconds       BIGINT       NOT NULL DEFAULT 7200,
    refresh_token_ttl_seconds      BIGINT       NOT NULL DEFAULT 2592000,
    authorization_code_ttl_seconds BIGINT       NOT NULL DEFAULT 300,
    device_code_ttl_seconds        BIGINT       NOT NULL DEFAULT 300,
    reuse_refresh_tokens           INT2         NOT NULL DEFAULT 0,
    remark                         VARCHAR(500)          DEFAULT NULL,
    tenant_id                      VARCHAR(50)           DEFAULT NULL,
    create_op                      VARCHAR(50)           DEFAULT NULL,
    create_time                    TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op                      VARCHAR(50)           DEFAULT NULL,
    modify_time                    TIMESTAMP             DEFAULT NULL,
    deleted                        INT2         NOT NULL DEFAULT 0,
    delete_op                      VARCHAR(50)           DEFAULT NULL,
    delete_time                    TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE security_oauth_client IS 'OAuth应用配置表';
COMMENT ON COLUMN security_oauth_client.id IS '主键ID';
COMMENT ON COLUMN security_oauth_client.client_id IS 'OAuth客户端ID';
COMMENT ON COLUMN security_oauth_client.client_secret IS 'OAuth客户端密钥密文';
COMMENT ON COLUMN security_oauth_client.client_name IS '应用名称';
COMMENT ON COLUMN security_oauth_client.client_type IS '客户端类型';
COMMENT ON COLUMN security_oauth_client.account_type IS '账号类型';
COMMENT ON COLUMN security_oauth_client.status IS '状态';
COMMENT ON COLUMN security_oauth_client.client_authentication_methods IS '客户端认证方式，逗号分隔';
COMMENT ON COLUMN security_oauth_client.authorization_grant_types IS '授权模式，逗号分隔';
COMMENT ON COLUMN security_oauth_client.redirect_uris IS '重定向URI白名单，逗号分隔';
COMMENT ON COLUMN security_oauth_client.post_logout_redirect_uris IS '退出后重定向URI白名单，逗号分隔';
COMMENT ON COLUMN security_oauth_client.scopes IS '授权范围，逗号分隔';
COMMENT ON COLUMN security_oauth_client.require_proof_key IS '是否要求PKCE：0-否 1-是';
COMMENT ON COLUMN security_oauth_client.require_authorization_consent IS '是否要求授权确认：0-否 1-是';
COMMENT ON COLUMN security_oauth_client.access_token_ttl_seconds IS 'Access Token有效期秒';
COMMENT ON COLUMN security_oauth_client.refresh_token_ttl_seconds IS 'Refresh Token有效期秒';
COMMENT ON COLUMN security_oauth_client.authorization_code_ttl_seconds IS '授权码有效期秒';
COMMENT ON COLUMN security_oauth_client.device_code_ttl_seconds IS '设备码有效期秒';
COMMENT ON COLUMN security_oauth_client.reuse_refresh_tokens IS '是否复用Refresh Token：0-否 1-是';
COMMENT ON COLUMN security_oauth_client.remark IS '备注';
COMMENT ON COLUMN security_oauth_client.tenant_id IS '租户ID';
COMMENT ON COLUMN security_oauth_client.create_op IS '创建人';
COMMENT ON COLUMN security_oauth_client.create_time IS '创建时间';
COMMENT ON COLUMN security_oauth_client.modify_op IS '修改人';
COMMENT ON COLUMN security_oauth_client.modify_time IS '修改时间';
COMMENT ON COLUMN security_oauth_client.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN security_oauth_client.delete_op IS '删除人';
COMMENT ON COLUMN security_oauth_client.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_security_oauth_client_client_id ON security_oauth_client (client_id) WHERE deleted = 0;
CREATE INDEX idx_security_oauth_client_name ON security_oauth_client (client_name) WHERE deleted = 0;
CREATE INDEX idx_security_oauth_client_status ON security_oauth_client (status);

CREATE TABLE security_oauth_scope
(
    id           VARCHAR(24) PRIMARY KEY,
    scope_code   VARCHAR(128) NOT NULL,
    scope_name   VARCHAR(128) NOT NULL,
    description  VARCHAR(500) DEFAULT NULL,
    account_type VARCHAR(32) NOT NULL,
    status       VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    tenant_id    VARCHAR(50) DEFAULT NULL,
    create_op    VARCHAR(50) DEFAULT NULL,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op    VARCHAR(50) DEFAULT NULL,
    modify_time  TIMESTAMP DEFAULT NULL,
    deleted      INT2 NOT NULL DEFAULT 0,
    delete_op    VARCHAR(50) DEFAULT NULL,
    delete_time  TIMESTAMP DEFAULT NULL
);

COMMENT ON TABLE security_oauth_scope IS 'OAuth Scope权限组';
COMMENT ON COLUMN security_oauth_scope.scope_code IS 'Scope完整编码';
COMMENT ON COLUMN security_oauth_scope.scope_name IS 'Scope中文名称';
COMMENT ON COLUMN security_oauth_scope.description IS '授权页中文描述';
CREATE UNIQUE INDEX uk_security_oauth_scope_code ON security_oauth_scope (scope_code) WHERE deleted = 0;
CREATE INDEX idx_security_oauth_scope_account_status ON security_oauth_scope (account_type, status) WHERE deleted = 0;

CREATE TABLE security_oauth_scope_resource
(
    id              VARCHAR(24) PRIMARY KEY,
    scope_id        VARCHAR(24) NOT NULL,
    api_resource_id VARCHAR(64) NOT NULL,
    tenant_id       VARCHAR(50) DEFAULT NULL,
    create_op       VARCHAR(50) DEFAULT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50) DEFAULT NULL,
    modify_time     TIMESTAMP DEFAULT NULL,
    deleted         INT2 NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50) DEFAULT NULL,
    delete_time     TIMESTAMP DEFAULT NULL,
    CONSTRAINT fk_oauth_scope_resource_scope FOREIGN KEY (scope_id) REFERENCES security_oauth_scope (id) ON DELETE CASCADE
);

COMMENT ON TABLE security_oauth_scope_resource IS 'OAuth Scope与接口资源关联表';
CREATE UNIQUE INDEX uk_oauth_scope_resource ON security_oauth_scope_resource (scope_id, api_resource_id);
CREATE INDEX idx_oauth_scope_resource_api ON security_oauth_scope_resource (api_resource_id);
