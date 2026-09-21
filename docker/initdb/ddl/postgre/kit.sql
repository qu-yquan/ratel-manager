-- =============================================
-- 文件管理相关表结构
-- 数据库：PostgreSQL
-- =============================================

-- =============================================
-- 表名：kit_file_meta_info
-- 说明：文件元信息表
-- =============================================
CREATE TABLE kit_file_meta_info
(
    file_meta_id       VARCHAR(24)  PRIMARY KEY,
    unique_id          VARCHAR(64)           DEFAULT NULL,
    upload_service_type VARCHAR(20)          DEFAULT NULL,
    file_size          VARCHAR(50)           DEFAULT NULL,
    media_type         VARCHAR(100)          DEFAULT NULL,
    file_group         VARCHAR(64)           DEFAULT NULL,
    file_url           VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_meta_info IS '文件元信息表';
COMMENT ON COLUMN kit_file_meta_info.file_meta_id IS '元文件ID';
COMMENT ON COLUMN kit_file_meta_info.unique_id IS '文件md5唯一值';
COMMENT ON COLUMN kit_file_meta_info.upload_service_type IS '上传服务类型：MINIO-OSS-COS-LOCAL';
COMMENT ON COLUMN kit_file_meta_info.file_size IS '文件大小';
COMMENT ON COLUMN kit_file_meta_info.media_type IS '文件媒体类型';
COMMENT ON COLUMN kit_file_meta_info.file_group IS '文件组';
COMMENT ON COLUMN kit_file_meta_info.file_url IS '文件路径';
COMMENT ON COLUMN kit_file_meta_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_meta_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_meta_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_meta_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_meta_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_meta_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_meta_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_meta_info.delete_time IS '删除时间';

-- 索引
CREATE UNIQUE INDEX uk_kit_file_meta_info_unique_id ON kit_file_meta_info (unique_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_meta_info_upload_service_type ON kit_file_meta_info (upload_service_type);

-- =============================================
-- 表名：kit_file_info
-- 说明：文件信息表，类似文件超链接，多个链接引用同一个文件元信息
-- =============================================
CREATE TABLE kit_file_info
(
    file_id            VARCHAR(24)  PRIMARY KEY,
    file_meta_id       VARCHAR(24)           DEFAULT NULL,
    file_name          VARCHAR(200)          DEFAULT NULL,
    file_size          VARCHAR(50)           DEFAULT NULL,
    file_suffix        VARCHAR(20)           DEFAULT NULL,
    disposable         INT2         NOT NULL DEFAULT 0,
    expired_time       TIMESTAMP             DEFAULT NULL,
    scope              VARCHAR(20)  NOT NULL DEFAULT 'PROTECTED',
    visitors           VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_info IS '文件信息表';
COMMENT ON COLUMN kit_file_info.file_id IS '文件ID';
COMMENT ON COLUMN kit_file_info.file_meta_id IS '元文件ID';
COMMENT ON COLUMN kit_file_info.file_name IS '文件名';
COMMENT ON COLUMN kit_file_info.file_size IS '文件大小';
COMMENT ON COLUMN kit_file_info.file_suffix IS '文件后缀';
COMMENT ON COLUMN kit_file_info.disposable IS '是否为一次性文件：0-否 1-是';
COMMENT ON COLUMN kit_file_info.expired_time IS '文件过期时间';
COMMENT ON COLUMN kit_file_info.scope IS '文件作用域：PUBLIC-公共 PROTECTED-受保护 PRIVATE-私有';
COMMENT ON COLUMN kit_file_info.visitors IS '作用域为PRIVATE时，允许访问的人员ID，多个逗号分隔';
COMMENT ON COLUMN kit_file_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_info.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_kit_file_info_file_meta_id ON kit_file_info (file_meta_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_info_scope ON kit_file_info (scope);
-- 外键
ALTER TABLE kit_file_info ADD CONSTRAINT file_info_meta_fk FOREIGN KEY (file_meta_id) REFERENCES kit_file_meta_info(file_meta_id) ON DELETE CASCADE ON UPDATE CASCADE;

-- =============================================
-- 表名：kit_file_chunk_info
-- 说明：断点续传分片信息表
-- =============================================
CREATE TABLE kit_file_chunk_info
(
    file_chunk_id      VARCHAR(24)  PRIMARY KEY,
    unique_id          VARCHAR(64)           DEFAULT NULL,
    upload_service_type VARCHAR(20)          DEFAULT NULL,
    file_name          VARCHAR(200)          DEFAULT NULL,
    media_type         VARCHAR(100)          DEFAULT NULL,
    chunk_offset       INT                   DEFAULT NULL,
    chunk_stream_size  INT                   DEFAULT NULL,
    chunk_group        VARCHAR(64)           DEFAULT NULL,
    chunk_url          VARCHAR(500)          DEFAULT NULL,
    expiry             INT                   DEFAULT NULL,
    upload_id          VARCHAR(256)           DEFAULT NULL,
    notes              VARCHAR(500)          DEFAULT NULL,
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
COMMENT ON TABLE kit_file_chunk_info IS '断点续传分片信息表';
COMMENT ON COLUMN kit_file_chunk_info.file_chunk_id IS '文件分片ID';
COMMENT ON COLUMN kit_file_chunk_info.unique_id IS '文件标识（md5唯一值）';
COMMENT ON COLUMN kit_file_chunk_info.upload_service_type IS '上传服务类型：MINIO-OSS-COS-LOCAL';
COMMENT ON COLUMN kit_file_chunk_info.file_name IS '文件名';
COMMENT ON COLUMN kit_file_chunk_info.media_type IS '文件媒体类型';
COMMENT ON COLUMN kit_file_chunk_info.chunk_offset IS 'chunk偏移量';
COMMENT ON COLUMN kit_file_chunk_info.chunk_stream_size IS 'chunk流大小';
COMMENT ON COLUMN kit_file_chunk_info.chunk_group IS 'chunk组';
COMMENT ON COLUMN kit_file_chunk_info.chunk_url IS 'chunk上传路径';
COMMENT ON COLUMN kit_file_chunk_info.expiry IS '到期时长（秒）';
COMMENT ON COLUMN kit_file_chunk_info.upload_id IS '唯一上传ID';
COMMENT ON COLUMN kit_file_chunk_info.notes IS '其他信息，用于扩展';
COMMENT ON COLUMN kit_file_chunk_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_file_chunk_info.create_op IS '创建人';
COMMENT ON COLUMN kit_file_chunk_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_file_chunk_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_file_chunk_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_file_chunk_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_file_chunk_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_file_chunk_info.delete_time IS '删除时间';

-- 索引
CREATE INDEX idx_kit_file_chunk_info_unique_id ON kit_file_chunk_info (unique_id) WHERE deleted = 0;
CREATE INDEX idx_kit_file_chunk_info_upload_id ON kit_file_chunk_info (upload_id);
CREATE INDEX idx_kit_file_chunk_info_chunk_group ON kit_file_chunk_info (chunk_group);

-- =============================================
-- 定时任务相关表结构（基于 xxl-job 改造）
-- 表名前缀：kit_job_
-- =============================================

-- =============================================
-- 表名：kit_job_registry
-- 说明：执行器注册表
-- =============================================
CREATE TABLE kit_job_registry
(
    id              VARCHAR(24)  PRIMARY KEY,
    registry_group  VARCHAR(64)  NOT NULL,
    registry_key    VARCHAR(255) NOT NULL,
    registry_value  VARCHAR(255) NOT NULL,
    tenant_id       VARCHAR(50)           DEFAULT NULL,
    create_op       VARCHAR(50)           DEFAULT NULL,
    create_time     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)           DEFAULT NULL,
    modify_time     TIMESTAMP             DEFAULT NULL,
    deleted         INT2         NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)           DEFAULT NULL,
    delete_time     TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_registry IS '执行器注册表';
COMMENT ON COLUMN kit_job_registry.id IS '主键ID';
COMMENT ON COLUMN kit_job_registry.registry_group IS '执行器AppName（命名空间）';
COMMENT ON COLUMN kit_job_registry.registry_key IS 'Handler名称';
COMMENT ON COLUMN kit_job_registry.registry_value IS '注册值（地址）';
COMMENT ON COLUMN kit_job_registry.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_registry.create_op IS '创建人';
COMMENT ON COLUMN kit_job_registry.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_registry.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_registry.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_registry.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_registry.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_registry.delete_time IS '删除时间';

ALTER TABLE kit_job_registry ADD CONSTRAINT uk_g_k_v UNIQUE (registry_group, registry_key, registry_value);
CREATE UNIQUE INDEX i_g_k_v ON kit_job_registry (registry_group, registry_key, registry_value) WHERE deleted = 0;

-- =============================================
-- 表名：kit_job_info
-- 说明：任务信息表
-- =============================================
CREATE TABLE kit_job_info
(
    id                        VARCHAR(24)  PRIMARY KEY,
    name                      VARCHAR(255) NOT NULL,
    author                    VARCHAR(64)           DEFAULT NULL,
    alarm_email               VARCHAR(255)          DEFAULT NULL,
    schedule_type             VARCHAR(50)  NOT NULL DEFAULT 'NONE',
    schedule_conf             VARCHAR(128)          DEFAULT NULL,
    misfire_strategy          VARCHAR(50)  NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy   VARCHAR(50)           DEFAULT NULL,
    executor_handler          VARCHAR(255)          DEFAULT NULL,
    executor_param            TEXT                  DEFAULT NULL,
    executor_block_strategy   VARCHAR(50)           DEFAULT NULL,
    executor_timeout          INT          NOT NULL DEFAULT 0,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    glue_type                 VARCHAR(50)  NOT NULL,
    glue_source               TEXT                  DEFAULT NULL,
    glue_remark               VARCHAR(128)          DEFAULT NULL,
    glue_updatetime           TIMESTAMP             DEFAULT NULL,
    child_jobid               VARCHAR(255)          DEFAULT NULL,
    trigger_status            INT2         NOT NULL DEFAULT 0,
    trigger_last_time         BIGINT       NOT NULL DEFAULT 0,
    trigger_next_time         BIGINT       NOT NULL DEFAULT 0,
    tenant_id                 VARCHAR(50)           DEFAULT NULL,
    create_op                 VARCHAR(50)           DEFAULT NULL,
    create_time               TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op                 VARCHAR(50)           DEFAULT NULL,
    modify_time               TIMESTAMP             DEFAULT NULL,
    deleted                   INT2         NOT NULL DEFAULT 0,
    delete_op                 VARCHAR(50)           DEFAULT NULL,
    delete_time               TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_info IS '任务信息表';
COMMENT ON COLUMN kit_job_info.id IS '主键ID';
COMMENT ON COLUMN kit_job_info.name IS '任务名称';
COMMENT ON COLUMN kit_job_info.author IS '作者';
COMMENT ON COLUMN kit_job_info.alarm_email IS '报警邮件';
COMMENT ON COLUMN kit_job_info.schedule_type IS '调度类型';
COMMENT ON COLUMN kit_job_info.schedule_conf IS '调度配置，值含义取决于调度类型';
COMMENT ON COLUMN kit_job_info.misfire_strategy IS '调度过期策略';
COMMENT ON COLUMN kit_job_info.executor_route_strategy IS '执行器路由策略';
COMMENT ON COLUMN kit_job_info.executor_handler IS '任务handler';
COMMENT ON COLUMN kit_job_info.executor_param IS '任务参数';
COMMENT ON COLUMN kit_job_info.executor_block_strategy IS '阻塞处理策略';
COMMENT ON COLUMN kit_job_info.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN kit_job_info.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN kit_job_info.glue_type IS 'GLUE类型';
COMMENT ON COLUMN kit_job_info.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN kit_job_info.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN kit_job_info.glue_updatetime IS 'GLUE更新时间';
COMMENT ON COLUMN kit_job_info.child_jobid IS '子任务ID，多个逗号分隔';
COMMENT ON COLUMN kit_job_info.trigger_status IS '调度状态：0-停止，1-运行';
COMMENT ON COLUMN kit_job_info.trigger_last_time IS '上次调度时间';
COMMENT ON COLUMN kit_job_info.trigger_next_time IS '下次调度时间';
COMMENT ON COLUMN kit_job_info.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_info.create_op IS '创建人';
COMMENT ON COLUMN kit_job_info.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_info.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_info.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_info.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_info.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_info.delete_time IS '删除时间';

-- =============================================
-- 表名：kit_job_log_glue
-- 说明：任务GLUE日志表
-- =============================================
CREATE TABLE kit_job_log_glue
(
    id            VARCHAR(24)  PRIMARY KEY,
    job_id        VARCHAR(24)  NOT NULL,
    glue_type     VARCHAR(50)           DEFAULT NULL,
    glue_source   TEXT                  DEFAULT NULL,
    glue_remark   VARCHAR(128) NOT NULL,
    tenant_id     VARCHAR(50)           DEFAULT NULL,
    create_op     VARCHAR(50)           DEFAULT NULL,
    create_time   TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op     VARCHAR(50)           DEFAULT NULL,
    modify_time   TIMESTAMP             DEFAULT NULL,
    deleted       INT2         NOT NULL DEFAULT 0,
    delete_op     VARCHAR(50)           DEFAULT NULL,
    delete_time   TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log_glue IS '任务GLUE日志表';
COMMENT ON COLUMN kit_job_log_glue.id IS '主键ID';
COMMENT ON COLUMN kit_job_log_glue.job_id IS '任务主键ID';
COMMENT ON COLUMN kit_job_log_glue.glue_type IS 'GLUE类型';
COMMENT ON COLUMN kit_job_log_glue.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN kit_job_log_glue.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN kit_job_log_glue.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log_glue.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log_glue.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log_glue.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log_glue.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log_glue.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log_glue.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log_glue.delete_time IS '删除时间';

-- =============================================
-- 表名：kit_job_log
-- 说明：任务执行日志表
-- =============================================
CREATE TABLE kit_job_log
(
    id                        VARCHAR(24)  PRIMARY KEY,
    job_id                    VARCHAR(24)  NOT NULL,
    executor_address          VARCHAR(255)          DEFAULT NULL,
    executor_handler          VARCHAR(255)          DEFAULT NULL,
    executor_param            TEXT                  DEFAULT NULL,
    executor_sharding_param   VARCHAR(20)           DEFAULT NULL,
    executor_fail_retry_count INT          NOT NULL DEFAULT 0,
    trigger_time              TIMESTAMP             DEFAULT NULL,
    trigger_code              INT          NOT NULL,
    trigger_msg               TEXT                  DEFAULT NULL,
    handle_time               TIMESTAMP             DEFAULT NULL,
    handle_code               INT          NOT NULL,
    handle_msg                TEXT                  DEFAULT NULL,
    alarm_status              INT2         NOT NULL DEFAULT 0,
    tenant_id                 VARCHAR(50)           DEFAULT NULL,
    create_op                 VARCHAR(50)           DEFAULT NULL,
    create_time               TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op                 VARCHAR(50)           DEFAULT NULL,
    modify_time               TIMESTAMP             DEFAULT NULL,
    deleted                   INT2         NOT NULL DEFAULT 0,
    delete_op                 VARCHAR(50)           DEFAULT NULL,
    delete_time               TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log IS '任务执行日志表';
COMMENT ON COLUMN kit_job_log.id IS '主键ID';
COMMENT ON COLUMN kit_job_log.job_id IS '任务主键ID';
COMMENT ON COLUMN kit_job_log.executor_address IS '执行器地址，本次执行的地址';
COMMENT ON COLUMN kit_job_log.executor_handler IS '任务handler';
COMMENT ON COLUMN kit_job_log.executor_param IS '任务参数';
COMMENT ON COLUMN kit_job_log.executor_sharding_param IS '任务分片参数，格式如 1/2';
COMMENT ON COLUMN kit_job_log.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN kit_job_log.trigger_time IS '调度-时间';
COMMENT ON COLUMN kit_job_log.trigger_code IS '调度-结果';
COMMENT ON COLUMN kit_job_log.trigger_msg IS '调度-日志';
COMMENT ON COLUMN kit_job_log.handle_time IS '执行-时间';
COMMENT ON COLUMN kit_job_log.handle_code IS '执行-状态';
COMMENT ON COLUMN kit_job_log.handle_msg IS '执行-日志';
COMMENT ON COLUMN kit_job_log.alarm_status IS '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败';
COMMENT ON COLUMN kit_job_log.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log.delete_time IS '删除时间';

CREATE INDEX i_trigger_time ON kit_job_log (trigger_time);
CREATE INDEX i_handle_code ON kit_job_log (handle_code);
CREATE INDEX i_job_id ON kit_job_log (job_id);

-- =============================================
-- 表名：kit_job_log_report
-- 说明：任务日志报表
-- =============================================
CREATE TABLE kit_job_log_report
(
    id              VARCHAR(24)  PRIMARY KEY,
    trigger_day     TIMESTAMP             DEFAULT NULL,
    running_count   INT          NOT NULL DEFAULT 0,
    suc_count       INT          NOT NULL DEFAULT 0,
    fail_count      INT          NOT NULL DEFAULT 0,
    tenant_id       VARCHAR(50)           DEFAULT NULL,
    create_op       VARCHAR(50)           DEFAULT NULL,
    create_time     TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)           DEFAULT NULL,
    modify_time     TIMESTAMP             DEFAULT NULL,
    deleted         INT2         NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)           DEFAULT NULL,
    delete_time     TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_job_log_report IS '任务日志报表';
COMMENT ON COLUMN kit_job_log_report.id IS '主键ID';
COMMENT ON COLUMN kit_job_log_report.trigger_day IS '调度-时间';
COMMENT ON COLUMN kit_job_log_report.running_count IS '运行中-日志数量';
COMMENT ON COLUMN kit_job_log_report.suc_count IS '执行成功-日志数量';
COMMENT ON COLUMN kit_job_log_report.fail_count IS '执行失败-日志数量';
COMMENT ON COLUMN kit_job_log_report.tenant_id IS '租户ID';
COMMENT ON COLUMN kit_job_log_report.create_op IS '创建人';
COMMENT ON COLUMN kit_job_log_report.create_time IS '创建时间';
COMMENT ON COLUMN kit_job_log_report.modify_op IS '修改人';
COMMENT ON COLUMN kit_job_log_report.modify_time IS '修改时间';
COMMENT ON COLUMN kit_job_log_report.deleted IS '删除标识：0-未删除 1-已删除';
COMMENT ON COLUMN kit_job_log_report.delete_op IS '删除人';
COMMENT ON COLUMN kit_job_log_report.delete_time IS '删除时间';

ALTER TABLE kit_job_log_report ADD CONSTRAINT uk_trigger_day UNIQUE (trigger_day);
CREATE UNIQUE INDEX i_trigger_day ON kit_job_log_report (trigger_day) WHERE deleted = 0;

-- =============================================
-- 表名：kit_job_lock
-- 说明：调度锁表
-- =============================================
CREATE TABLE kit_job_lock
(
    lock_name VARCHAR(50) PRIMARY KEY
);

COMMENT ON TABLE kit_job_lock IS '调度锁表';
COMMENT ON COLUMN kit_job_lock.lock_name IS '锁名称';

-- =============================================
-- 知识库相关表结构
-- 表名前缀：kit_knowledge_
-- =============================================

CREATE TABLE kit_knowledge_node
(
    id               VARCHAR(24) PRIMARY KEY,
    parent_id        VARCHAR(24)           DEFAULT NULL,
    node_type        VARCHAR(16)  NOT NULL,
    directory_path   VARCHAR(2000) NOT NULL DEFAULT '',
    name             VARCHAR(200) NOT NULL,
    sort_no          INT          NOT NULL DEFAULT 0,
    file_id          VARCHAR(24)           DEFAULT NULL,
    file_name        VARCHAR(200)          DEFAULT NULL,
    file_size        BIGINT                DEFAULT NULL,
    file_format      VARCHAR(32)           DEFAULT NULL,
    document_status  VARCHAR(32)           DEFAULT NULL,
    target_page_id   VARCHAR(24)           DEFAULT NULL,
    process_message  VARCHAR(1000)         DEFAULT NULL,
    image_file_ids_json TEXT               DEFAULT NULL,
    image_ocr_parsed INT2         NOT NULL DEFAULT 0,
    embedding_completed INT2      NOT NULL DEFAULT 0,
    enabled          INT2         NOT NULL DEFAULT 1,
    parsed_at        TIMESTAMP             DEFAULT NULL,
    processed_at     TIMESTAMP             DEFAULT NULL,
    tenant_id        VARCHAR(50)           DEFAULT NULL,
    create_op        VARCHAR(50)           DEFAULT NULL,
    create_time      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op        VARCHAR(50)           DEFAULT NULL,
    modify_time      TIMESTAMP             DEFAULT NULL,
    deleted          INT2         NOT NULL DEFAULT 0,
    delete_op        VARCHAR(50)           DEFAULT NULL,
    delete_time      TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_node IS '知识目录与文档节点';
COMMENT ON COLUMN kit_knowledge_node.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_node.parent_id IS '父目录ID';
COMMENT ON COLUMN kit_knowledge_node.node_type IS 'DIRECTORY或DOCUMENT';
COMMENT ON COLUMN kit_knowledge_node.directory_path IS '祖先目录ID路径';
COMMENT ON COLUMN kit_knowledge_node.name IS '节点名称';
COMMENT ON COLUMN kit_knowledge_node.file_id IS '文件ID';
COMMENT ON COLUMN kit_knowledge_node.file_name IS '文件名';
COMMENT ON COLUMN kit_knowledge_node.document_status IS '文档处理状态';
COMMENT ON COLUMN kit_knowledge_node.target_page_id IS '目标Page ID';
COMMENT ON COLUMN kit_knowledge_node.process_message IS '处理信息';
COMMENT ON COLUMN kit_knowledge_node.image_file_ids_json IS '导入图片文件ID JSON';
COMMENT ON COLUMN kit_knowledge_node.image_ocr_parsed IS '图片是否已完成 OCR 解析：0-否 1-是';
COMMENT ON COLUMN kit_knowledge_node.embedding_completed IS '是否已完成向量化：0-否 1-是';
COMMENT ON COLUMN kit_knowledge_node.parsed_at IS '文件解析完成时间';
COMMENT ON COLUMN kit_knowledge_node.processed_at IS '处理完成时间';

CREATE INDEX idx_kit_knowledge_node_parent ON kit_knowledge_node (parent_id, node_type, sort_no) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_kit_knowledge_node_sibling ON kit_knowledge_node (COALESCE(parent_id, 'ROOT'), node_type, name) WHERE deleted = 0;
CREATE INDEX idx_kit_knowledge_node_file_id ON kit_knowledge_node (file_id) WHERE deleted = 0;
CREATE INDEX idx_kit_knowledge_node_status ON kit_knowledge_node (document_status);

CREATE TABLE kit_knowledge_source_segment
(
    id                 VARCHAR(24) PRIMARY KEY,
    source_document_id VARCHAR(24)  NOT NULL,
    segment_no         INT          NOT NULL,
    segment_type       VARCHAR(32)  NOT NULL,
    heading_path       VARCHAR(1000)         DEFAULT NULL,
    source_locator     VARCHAR(500)          DEFAULT NULL,
    content            TEXT                  DEFAULT NULL,
    tenant_id          VARCHAR(50)           DEFAULT NULL,
    create_op          VARCHAR(50)           DEFAULT NULL,
    create_time        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)           DEFAULT NULL,
    modify_time        TIMESTAMP             DEFAULT NULL,
    deleted            INT2         NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)           DEFAULT NULL,
    delete_time        TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_source_segment IS '知识源文档解析片段';
COMMENT ON COLUMN kit_knowledge_source_segment.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_source_segment.source_document_id IS '源文档ID';
COMMENT ON COLUMN kit_knowledge_source_segment.segment_no IS '片段顺序号';
COMMENT ON COLUMN kit_knowledge_source_segment.segment_type IS '片段类型';
COMMENT ON COLUMN kit_knowledge_source_segment.heading_path IS '标题路径';
COMMENT ON COLUMN kit_knowledge_source_segment.source_locator IS '来源定位';
COMMENT ON COLUMN kit_knowledge_source_segment.content IS '片段内容';

CREATE UNIQUE INDEX uk_kit_knowledge_source_segment_doc_no ON kit_knowledge_source_segment (source_document_id, segment_no) WHERE deleted = 0;
CREATE INDEX idx_kit_knowledge_source_segment_document ON kit_knowledge_source_segment (source_document_id) WHERE deleted = 0;

CREATE TABLE kit_knowledge_directory_role
(
    id                 VARCHAR(24) PRIMARY KEY,
    directory_id       VARCHAR(24)  NOT NULL,
    role_code          VARCHAR(100) NOT NULL,
    tenant_id          VARCHAR(50) DEFAULT NULL,
    create_op          VARCHAR(50) DEFAULT NULL,
    create_time        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50) DEFAULT NULL,
    modify_time        TIMESTAMP   DEFAULT NULL,
    deleted            INT2        NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50) DEFAULT NULL,
    delete_time        TIMESTAMP   DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_directory_role IS '知识目录角色授权';
COMMENT ON COLUMN kit_knowledge_directory_role.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_directory_role.directory_id IS '目录ID';
COMMENT ON COLUMN kit_knowledge_directory_role.role_code IS '角色编码';

CREATE UNIQUE INDEX uk_kit_knowledge_directory_role ON kit_knowledge_directory_role (directory_id, role_code) WHERE deleted = 0;
CREATE INDEX idx_kit_knowledge_directory_role_role ON kit_knowledge_directory_role (role_code) WHERE deleted = 0;

CREATE TABLE kit_knowledge_document_event
(
    id VARCHAR(24) PRIMARY KEY,
    document_id VARCHAR(24) NOT NULL,
    task_id VARCHAR(24) DEFAULT NULL,
    event_type VARCHAR(40) NOT NULL,
    message VARCHAR(2000) DEFAULT NULL,
    occurred_at TIMESTAMP NOT NULL,
    tenant_id VARCHAR(50) DEFAULT NULL,
    create_op VARCHAR(50) DEFAULT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modify_op VARCHAR(50) DEFAULT NULL,
    modify_time TIMESTAMP DEFAULT NULL,
    deleted INT2 NOT NULL DEFAULT 0,
    delete_op VARCHAR(50) DEFAULT NULL,
    delete_time TIMESTAMP DEFAULT NULL
);
COMMENT ON TABLE kit_knowledge_document_event IS '知识文档事件日志';
CREATE INDEX idx_kit_knowledge_event_doc_time ON kit_knowledge_document_event (document_id, occurred_at) WHERE deleted = 0;

CREATE TABLE kit_knowledge_page
(
    id                 VARCHAR(24) PRIMARY KEY,
    title              VARCHAR(200)          DEFAULT NULL,
    page_status        VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    current_version_id VARCHAR(24)           DEFAULT NULL,
    tenant_id          VARCHAR(50)           DEFAULT NULL,
    create_op          VARCHAR(50)           DEFAULT NULL,
    create_time        TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)           DEFAULT NULL,
    modify_time        TIMESTAMP             DEFAULT NULL,
    deleted            INT2         NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)           DEFAULT NULL,
    delete_time        TIMESTAMP             DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_page IS '知识Page';
COMMENT ON COLUMN kit_knowledge_page.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_page.title IS '标题';
COMMENT ON COLUMN kit_knowledge_page.page_status IS 'Page状态';
COMMENT ON COLUMN kit_knowledge_page.current_version_id IS '当前版本ID';

CREATE INDEX idx_kit_knowledge_page_status ON kit_knowledge_page (page_status);

CREATE TABLE kit_knowledge_page_version
(
    id               VARCHAR(24) PRIMARY KEY,
    page_id          VARCHAR(24) NOT NULL,
    version_no       INT         NOT NULL,
    version_status   VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    markdown_content TEXT                 DEFAULT NULL,
    published_at     TIMESTAMP            DEFAULT NULL,
    tenant_id        VARCHAR(50)          DEFAULT NULL,
    create_op        VARCHAR(50)          DEFAULT NULL,
    create_time      TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op        VARCHAR(50)          DEFAULT NULL,
    modify_time      TIMESTAMP            DEFAULT NULL,
    deleted          INT2        NOT NULL DEFAULT 0,
    delete_op        VARCHAR(50)          DEFAULT NULL,
    delete_time      TIMESTAMP            DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_page_version IS '知识Page版本';
COMMENT ON COLUMN kit_knowledge_page_version.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_page_version.page_id IS 'Page ID';
COMMENT ON COLUMN kit_knowledge_page_version.version_no IS '版本号';
COMMENT ON COLUMN kit_knowledge_page_version.version_status IS '版本状态';
COMMENT ON COLUMN kit_knowledge_page_version.markdown_content IS 'Markdown内容快照';
COMMENT ON COLUMN kit_knowledge_page_version.published_at IS '发布时间';

CREATE UNIQUE INDEX uk_kit_knowledge_page_version_page_no ON kit_knowledge_page_version (page_id, version_no) WHERE deleted = 0;

CREATE TABLE kit_knowledge_page_block
(
    id              VARCHAR(24) PRIMARY KEY,
    page_version_id VARCHAR(24) NOT NULL,
    order_no        INT         NOT NULL,
    block_type      VARCHAR(32) NOT NULL,
    content         TEXT                 DEFAULT NULL,
    tenant_id       VARCHAR(50)          DEFAULT NULL,
    create_op       VARCHAR(50)          DEFAULT NULL,
    create_time     TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op       VARCHAR(50)          DEFAULT NULL,
    modify_time     TIMESTAMP            DEFAULT NULL,
    deleted         INT2        NOT NULL DEFAULT 0,
    delete_op       VARCHAR(50)          DEFAULT NULL,
    delete_time     TIMESTAMP            DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_page_block IS '知识Page Block';
COMMENT ON COLUMN kit_knowledge_page_block.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_page_block.page_version_id IS 'Page版本ID';
COMMENT ON COLUMN kit_knowledge_page_block.order_no IS '排序号';
COMMENT ON COLUMN kit_knowledge_page_block.block_type IS 'Block类型';
COMMENT ON COLUMN kit_knowledge_page_block.content IS 'Block内容';

CREATE UNIQUE INDEX uk_kit_knowledge_page_block_version_order ON kit_knowledge_page_block (page_version_id, order_no) WHERE deleted = 0;

CREATE TABLE kit_knowledge_page_source_ref
(
    id                 VARCHAR(24) PRIMARY KEY,
    page_block_id      VARCHAR(24) NOT NULL,
    source_type        VARCHAR(32) NOT NULL,
    source_document_id VARCHAR(24) NOT NULL,
    source_segment_start_no INT             DEFAULT NULL,
    source_segment_end_no   INT             DEFAULT NULL,
    source_locator     VARCHAR(500) DEFAULT NULL,
    tenant_id          VARCHAR(50)  DEFAULT NULL,
    create_op          VARCHAR(50)  DEFAULT NULL,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)  DEFAULT NULL,
    modify_time        TIMESTAMP    DEFAULT NULL,
    deleted            INT2         NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)  DEFAULT NULL,
    delete_time        TIMESTAMP    DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_page_source_ref IS '知识Page Block来源关系';
COMMENT ON COLUMN kit_knowledge_page_source_ref.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_page_source_ref.page_block_id IS 'Page Block ID';
COMMENT ON COLUMN kit_knowledge_page_source_ref.source_type IS '来源类型';
COMMENT ON COLUMN kit_knowledge_page_source_ref.source_document_id IS '源文档ID';
COMMENT ON COLUMN kit_knowledge_page_source_ref.source_segment_start_no IS '起始片段序号';
COMMENT ON COLUMN kit_knowledge_page_source_ref.source_segment_end_no IS '结束片段序号';
COMMENT ON COLUMN kit_knowledge_page_source_ref.source_locator IS '来源定位';

CREATE UNIQUE INDEX uk_kit_knowledge_page_source_ref_block ON kit_knowledge_page_source_ref (page_block_id) WHERE deleted = 0;
CREATE INDEX idx_kit_knowledge_page_source_ref_document ON kit_knowledge_page_source_ref (source_document_id) WHERE deleted = 0;

CREATE TABLE kit_knowledge_ingest_task
(
    id                 VARCHAR(24) PRIMARY KEY,
    source_document_id VARCHAR(24) NOT NULL,
    task_status        VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    current_stage      VARCHAR(32)          DEFAULT NULL,
    retry_count        INT         NOT NULL DEFAULT 0,
    error_message      VARCHAR(2000)        DEFAULT NULL,
    started_at         TIMESTAMP            DEFAULT NULL,
    finished_at        TIMESTAMP            DEFAULT NULL,
    tenant_id          VARCHAR(50)          DEFAULT NULL,
    create_op          VARCHAR(50)          DEFAULT NULL,
    create_time        TIMESTAMP            DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50)          DEFAULT NULL,
    modify_time        TIMESTAMP            DEFAULT NULL,
    deleted            INT2        NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50)          DEFAULT NULL,
    delete_time        TIMESTAMP            DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_ingest_task IS '知识文档导入任务';
COMMENT ON COLUMN kit_knowledge_ingest_task.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_ingest_task.source_document_id IS '源文档ID';
COMMENT ON COLUMN kit_knowledge_ingest_task.task_status IS '任务状态';
COMMENT ON COLUMN kit_knowledge_ingest_task.current_stage IS '当前处理阶段';
COMMENT ON COLUMN kit_knowledge_ingest_task.retry_count IS '重试次数';
COMMENT ON COLUMN kit_knowledge_ingest_task.error_message IS '错误信息';
COMMENT ON COLUMN kit_knowledge_ingest_task.started_at IS '开始时间';
COMMENT ON COLUMN kit_knowledge_ingest_task.finished_at IS '完成时间';

CREATE UNIQUE INDEX uk_kit_knowledge_ingest_task_active_doc ON kit_knowledge_ingest_task (source_document_id) WHERE deleted = 0 AND task_status IN ('PENDING', 'RUNNING');

CREATE TABLE kit_knowledge_ingest_analysis_checkpoint
(
    id                 VARCHAR(24) PRIMARY KEY,
    ingest_task_id     VARCHAR(24) NOT NULL,
    chunk_no           INT         NOT NULL,
    chunk_content_hash VARCHAR(64) NOT NULL,
    analysis_digest    TEXT,
    checkpoint_status  VARCHAR(32) NOT NULL,
    source_language    VARCHAR(32) DEFAULT NULL,
    tenant_id          VARCHAR(50) DEFAULT NULL,
    create_op          VARCHAR(50) DEFAULT NULL,
    create_time        TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    modify_op          VARCHAR(50) DEFAULT NULL,
    modify_time        TIMESTAMP   DEFAULT NULL,
    deleted            INT2        NOT NULL DEFAULT 0,
    delete_op          VARCHAR(50) DEFAULT NULL,
    delete_time        TIMESTAMP   DEFAULT NULL
);

COMMENT ON TABLE kit_knowledge_ingest_analysis_checkpoint IS '知识文档导入分析检查点';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.id IS '主键ID';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.ingest_task_id IS '导入任务ID';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.chunk_no IS '源文档分析片段序号';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.chunk_content_hash IS '源文档分析片段内容哈希';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.analysis_digest IS '片段分析摘要';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.checkpoint_status IS '检查点状态：PENDING-待处理 RUNNING-处理中 SUCCEEDED-成功 FAILED-失败';
COMMENT ON COLUMN kit_knowledge_ingest_analysis_checkpoint.source_language IS '源文档片段识别语言';

CREATE UNIQUE INDEX uk_kit_knowledge_ingest_analysis_checkpoint_task_chunk
    ON kit_knowledge_ingest_analysis_checkpoint (ingest_task_id, chunk_no) WHERE deleted = 0;

-- ================== 初始数据 ==================

INSERT INTO kit_job_lock (lock_name) VALUES ('schedule_lock');
