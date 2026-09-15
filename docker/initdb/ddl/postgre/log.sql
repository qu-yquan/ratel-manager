-- =============================================
-- 日志模块 DDL（PostgreSQL）
-- =============================================

-- =============================================
-- 表名：log_operation
-- 说明：操作日志表
-- =============================================
CREATE TABLE log_operation (
    id              VARCHAR(24) PRIMARY KEY,
    authorization_id VARCHAR(64),
    tid             VARCHAR(64),
    parent_id       VARCHAR(24),
    module_prefix   VARCHAR(50),
    from_app        VARCHAR(50),
    api_module      VARCHAR(100),
    menu_id         VARCHAR(24),
    oper_subject    INT2 DEFAULT 0,
    api_description VARCHAR(200),
    method          VARCHAR(100),
    request_url     VARCHAR(500),
    request_method  VARCHAR(10),
    terminal        VARCHAR(20),
    terminal_detail VARCHAR(500),
    oper_name       VARCHAR(50),
    request_param   TEXT,
    response_data   TEXT,
    error_msg       TEXT,
    status          INT2 DEFAULT 1,
    request_time    TIMESTAMP,
    response_time   TIMESTAMP,
    consume_mill    BIGINT DEFAULT 0,
    tenant_id       VARCHAR(50),
    create_op       VARCHAR(50),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50),
    modify_time     TIMESTAMP,
    deleted         INT2 DEFAULT 0,
    delete_op       VARCHAR(50),
    delete_time     TIMESTAMP
);

-- 表和字段注释
COMMENT ON TABLE log_operation IS '操作日志表';
COMMENT ON COLUMN log_operation.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN log_operation.authorization_id IS '认证会话标识';
COMMENT ON COLUMN log_operation.tid IS '全局日志链路';
COMMENT ON COLUMN log_operation.parent_id IS '父节点';
COMMENT ON COLUMN log_operation.module_prefix IS '所属服务前缀';
COMMENT ON COLUMN log_operation.from_app IS '链路来源服务';
COMMENT ON COLUMN log_operation.api_module IS 'api模块名';
COMMENT ON COLUMN log_operation.menu_id IS '来源菜单';
COMMENT ON COLUMN log_operation.oper_subject IS '界面操作主体：0-人类 1-智能助手';
COMMENT ON COLUMN log_operation.api_description IS 'api接口详情注释';
COMMENT ON COLUMN log_operation.method IS '方法名';
COMMENT ON COLUMN log_operation.request_url IS '请求路径';
COMMENT ON COLUMN log_operation.request_method IS '请求方式';
COMMENT ON COLUMN log_operation.terminal IS '请求终端';
COMMENT ON COLUMN log_operation.terminal_detail IS '请求终端详情';
COMMENT ON COLUMN log_operation.oper_name IS '操作人';
COMMENT ON COLUMN log_operation.request_param IS '请求参数';
COMMENT ON COLUMN log_operation.response_data IS '响应数据';
COMMENT ON COLUMN log_operation.error_msg IS '错误消息';
COMMENT ON COLUMN log_operation.status IS '状态：0-失败 1-成功';
COMMENT ON COLUMN log_operation.request_time IS '请求时间';
COMMENT ON COLUMN log_operation.response_time IS '响应时间';
COMMENT ON COLUMN log_operation.consume_mill IS '耗时，ms';
COMMENT ON COLUMN log_operation.deleted IS '删除标识：0-未删除 1-已删除';

-- 索引
CREATE INDEX idx_log_operation_tid ON log_operation(tid) WHERE deleted = 0;
CREATE INDEX idx_log_operation_authorization ON log_operation(authorization_id) WHERE deleted = 0;
CREATE INDEX idx_log_operation_request_time ON log_operation(request_time) WHERE deleted = 0;

-- =============================================
-- 表名：log_login
-- 说明：一次认证会话一条记录，登出或过期时更新会话结束信息
-- =============================================
CREATE TABLE log_login (
    id                 VARCHAR(24) PRIMARY KEY,
    authorization_id   VARCHAR(64) NOT NULL,
    account_type        VARCHAR(30),
    visitor_type        VARCHAR(30),
    login_type          VARCHAR(100),
    grant_type          VARCHAR(100),
    client_id           VARCHAR(100),
    user_id             VARCHAR(24),
    user_name           VARCHAR(100),
    login_account       VARCHAR(100),
    end_type            VARCHAR(20),
    terminal            VARCHAR(20),
    terminal_detail     VARCHAR(500),
    client_ip           VARCHAR(64),
    status              INT2 DEFAULT 1,
    failure_code        VARCHAR(50),
    failure_message     VARCHAR(500),
    login_time          TIMESTAMP NOT NULL,
    end_time            TIMESTAMP,
    tenant_id           VARCHAR(50),
    create_op           VARCHAR(50),
    create_time         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op           VARCHAR(50),
    modify_time         TIMESTAMP,
    deleted             INT2 DEFAULT 0,
    delete_op           VARCHAR(50),
    delete_time         TIMESTAMP
);

COMMENT ON TABLE log_login IS '认证会话日志表，一次认证会话一条记录';
COMMENT ON COLUMN log_login.id IS '主键ID（雪花算法）';
COMMENT ON COLUMN log_login.authorization_id IS '认证会话标识';
COMMENT ON COLUMN log_login.account_type IS '账号类型';
COMMENT ON COLUMN log_login.visitor_type IS '访问者类型';
COMMENT ON COLUMN log_login.login_type IS '登录类型';
COMMENT ON COLUMN log_login.grant_type IS 'OAuth2授权类型';
COMMENT ON COLUMN log_login.client_id IS 'OAuth2客户端标识';
COMMENT ON COLUMN log_login.user_id IS '用户ID';
COMMENT ON COLUMN log_login.user_name IS '用户名';
COMMENT ON COLUMN log_login.login_account IS '登录账号';
COMMENT ON COLUMN log_login.end_type IS '会话结束类型：LOGOUT/EXPIRE';
COMMENT ON COLUMN log_login.terminal IS '终端类型';
COMMENT ON COLUMN log_login.terminal_detail IS '终端详情';
COMMENT ON COLUMN log_login.client_ip IS '客户端IP';
COMMENT ON COLUMN log_login.status IS '状态：0-失败 1-成功';
COMMENT ON COLUMN log_login.failure_code IS '失败错误码';
COMMENT ON COLUMN log_login.failure_message IS '失败原因';
COMMENT ON COLUMN log_login.login_time IS '登录时间';
COMMENT ON COLUMN log_login.end_time IS '会话结束时间，成功登录记录为空表示会话尚未结束';
COMMENT ON COLUMN log_login.tenant_id IS '租户ID';
COMMENT ON COLUMN log_login.create_op IS '创建人';
COMMENT ON COLUMN log_login.create_time IS '创建时间';
COMMENT ON COLUMN log_login.modify_op IS '修改人';
COMMENT ON COLUMN log_login.modify_time IS '修改时间';
COMMENT ON COLUMN log_login.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN log_login.delete_op IS '删除人';
COMMENT ON COLUMN log_login.delete_time IS '删除时间';

CREATE UNIQUE INDEX uk_log_login_authorization ON log_login(authorization_id) WHERE deleted = 0;
CREATE INDEX idx_log_login_user_time ON log_login(user_id, login_time) WHERE deleted = 0;
CREATE INDEX idx_log_login_end_time ON log_login(end_type, end_time) WHERE deleted = 0;
