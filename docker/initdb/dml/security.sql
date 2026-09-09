-- =============================================
-- 安全模块初始化数据
-- =============================================

-- 角色初始化
INSERT INTO security_role (id, role_name, role_code, sort, description, status, role_type, data_scope, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('1', '超级管理员', 'super_admin', 1, '拥有所有权限', 1, 1, 1, NULL, NULL, '2026-05-02 01:52:45.688324', 'admin', '2026-05-04 15:44:28.18939', 0, NULL, NULL);
INSERT INTO security_role (id, role_name, role_code, sort, description, status, role_type, data_scope, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2', '通用角色', 'common', 1, '所有用户都拥有该角色', 1, 1, 4, NULL, NULL, '2026-05-02 01:52:45.688324', 'admin', '2026-05-26 10:24:21.803287', 0, NULL, NULL);

-- 权限初始化
INSERT INTO security_abac (id, tenant_id, expression, description, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('1', 'default', 'contains(r.sub.roles , ''super_admin'')', '超级管理员角色表达式', 1, NULL, '2026-04-04 06:30:06.683643', NULL, '2026-04-04 06:30:06.683643', NULL, NULL, 0);
INSERT INTO security_abac (id, tenant_id, expression, description, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('2059099745615486976', 'default', 'contains(r.sub.roles , ''common'')', NULL, 1, 'admin', '2026-05-26 10:30:10.235452', 'admin', '2026-05-26 10:30:10.235452', NULL, NULL, 0);

INSERT INTO security_abac_permission (id, tenant_id, abac_id, resource_type, action, url_pattern, effect, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('1', 'default', '1', '*', '*', '*', 'allow', 1, NULL, '2026-05-02 11:17:56.593787', NULL, '2026-05-02 11:17:56.593787', NULL, NULL, 0);
INSERT INTO security_abac_permission (id, tenant_id, abac_id, resource_type, action, url_pattern, effect, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('2074860014971060225', 'default', '2059099745615486976', 'system', 'PUT', '/manager/password', 'allow', 1, 'admin', '2026-07-08 22:15:51.269159', 'admin', '2026-07-08 22:15:51.269159', NULL, NULL, 0);
INSERT INTO security_abac_permission (id, tenant_id, abac_id, resource_type, action, url_pattern, effect, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('2074860014979448833', 'default', '2059099745615486976', 'system', 'PUT', '/manager/profile', 'allow', 1, 'admin', '2026-07-08 22:15:51.26977', 'admin', '2026-07-08 22:15:51.26977', NULL, NULL, 0);
INSERT INTO security_abac_permission (id, tenant_id, abac_id, resource_type, action, url_pattern, effect, status, create_op, create_time, modify_op, modify_time, delete_op, delete_time, deleted) VALUES ('2074860014992031745', 'default', '2059099745615486976', 'security', 'GET', '/role/rolesByCurrUser', 'allow', 1, 'admin', '2026-07-08 22:15:51.270041', 'admin', '2026-07-08 22:15:51.270041', NULL, NULL, 0);

-- 角色用户关联初始化
INSERT INTO security_role_subject (id, subject_id, role_id, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('1', '1', '1', NULL, NULL, '2026-05-01 01:16:11.414947', NULL, NULL, 0, NULL, NULL);

-- 数据权限
INSERT INTO security_data_resource (id, schema_name, table_name, description, support_self_only, self_only_field, status, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, catalog_name) VALUES ('2076622640277331968', 'public', 'sys_user_dept', NULL, 0, NULL, 1, NULL, 'admin', '2026-07-13 18:59:53.882136', 'kaifa', '2026-07-13 19:54:43.440546', 0, NULL, NULL, 'gwsu');
INSERT INTO security_data_resource_condition (id, data_resource_id, field_name, show_null, user_resource_fields, assert_type, relationship, sort, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2076622640365412352', '2076622640277331968', 'dept_id', 1, 'deptId', 'EQ', 'AND', 1, NULL, 'kaifa', '2026-07-13 19:54:43.452626', 'kaifa', '2026-07-13 19:54:43.452626', 0, NULL, NULL);

-- 系统配置初始化
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2061834119926693888', 'assistant_view_config', '助手展示配置', '{"showThinking":true,"showToolCalls":true,"showHistory":true,"enableDragMode":true}', 4, 1, 'AI 助手界面展示配置', NULL, 'admin', '2026-06-02 23:35:35.874567', 'admin', '2026-06-02 23:51:24.23065', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2060910412299448320', 'model_llm_config', '助手配置', '{"provider":"openai","dashscope":{"apiKey":"","modelName":"qwen-plus","stream":true,"enableThinking":false,"enableSearch":false,"baseUrl":""},"openai":{"apiKey":"","modelName":"deepseek-v4-flash","stream":true,"baseUrl":"https://api.deepseek.com","endpointPath":"/chat/completions"},"gemini":{"apiKey":"","modelName":"gemini-2.0-flash","stream":true,"project":"","location":"us-central1"},"anthropic":{"apiKey":"","modelName":"claude-sonnet-4-5-20250929","stream":true,"baseUrl":""},"generateOptions":{"temperature":0.2,"topP":0.75,"frequencyPenalty":0.5,"presencePenalty":0.5,"additionalBodyParams":{"thinking":{"type":"enabled"},"reasoning":{"effort":"medium"}}}}', 4, 1, 'AI 助手模型提供商及生成参数配置', NULL, 'admin', '2026-05-31 10:25:06.816008', 'admin', '2026-06-02 23:11:03.469723', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2077671528211812352', 'model_embedding_config', '向量化模型配置', '{"enabled":false,"provider":"dashscope","dashscope":{"apiKey":"","modelName":"text-embedding-v4","baseUrl":"","dimensions":1024,"batchSize":16},"openai":{"apiKey":"","modelName":"text-embedding-3-small","baseUrl":"","dimensions":1536,"batchSize":16},"ollama":{"modelName":"nomic-embed-text","baseUrl":"http://localhost:11434","batchSize":16},"zhipuai":{"apiKey":"","modelName":"embedding-3","baseUrl":"","dimensions":2048,"batchSize":16}}', 4, 1, '向量化模型提供商及连接参数配置', NULL, 'admin', '2026-07-16 16:27:48.253733', 'admin', '2026-07-16 16:28:46.129448', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2077671887638499328', 'model_rerank_config', '重排模型配置', '{"enabled":false,"provider":"dashscope","dashscope":{"apiKey":"","modelName":"gte-rerank-v2","baseUrl":"","topN":10,"returnDocuments":true}}', 4, 1, '重排模型提供商及连接参数配置', NULL, 'admin', '2026-07-16 16:29:13.947374', 'admin', '2026-07-16 16:29:13.947374', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2060910012284210100', 'upload_file_extension_config', '文件后缀过滤配置', '{"enabled":false,"disable":"gz,exe,sh"}', 4, 1, '禁止上传的文件后缀配置', NULL, 'admin', '2026-06-07 10:29:14.684068', 'admin', '2026-06-07 18:58:39.410433', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2060910412284210129', 'upload_server_info_config', '上传服务配置', '{"type":"LOCAL","group":null,"local":{"path":""},"minio":{"url":"","accessKey":"","secretKey":""},"oss":{"endpoint":"","accessKey":"","secretKey":""},"cos":{"endpoint":"","accessKey":"","secretKey":"","region":""},"extension":{"disable":null,"enabled":false}}', 4, 1, '文件上传服务类型及连接配置', NULL, 'admin', '2026-06-06 01:47:27.244244', 'admin', '2026-06-07 22:40:29.116529', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2068877431517908992', 'assistant_remote_control_config', '助手远程控制配置', '{"dingTalk":{"aiCardTemplateId":"7f991cfb-9c52-4bac-aad2-5c60116d82cc.schema","clientId":null,"clientSecret":null,"endpoint":"api.dingtalk.com","protocol":"https","regionId":"central"},"type":null}', 4, 1, 'AI助手远程控制(钉钉)配置', NULL, NULL, '2026-06-22 02:05:07.109859', NULL, NULL, 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2069619545202655232', 'basic_url_config', '基础地址配置', '{"viewBaseUrl":"http://127.0.0.1","apiBaseUrl":"http://127.0.0.1/api"}', 4, 1, '项目前后端基础地址配置', NULL, 'admin', '2026-06-24 11:12:05.905821', 'admin', '2026-06-30 16:15:41.655821', 0, NULL, NULL);
INSERT INTO security_config (id, config_key, config_name, config_value, value_type, config_type, description, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2060910412291281761', 'captcha_config', '验证码配置', '{"enabled":true,"expireSeconds":120,"type":"BLOCK_PUZZLE","verificationExpireSeconds":180,"waterMark":"Ratel-Manager"}', 4, 1, '登录验证码默认配置', NULL, 'admin', '2026-09-01 00:00:00', 'admin', '2026-09-01 00:00:00', 0, NULL, NULL);

-- 菜单初始化
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95808001', '94991641', '新增业务功能', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/business-function', NULL, 'admin', '2026-06-08 10:00:00', 'admin', '2026-06-08 10:00:00', 0, NULL, NULL, 1, 1, '94991641_bf_add', '新增AI业务功能配置，包含业务名称、简介、详细介绍及关联表模型');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95808002', '94991641', '删除业务功能', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/business-function', NULL, 'admin', '2026-06-08 10:00:00', 'admin', '2026-06-08 10:00:00', 0, NULL, NULL, 1, 1, '94991641_bf_remove', '批量删除业务功能配置');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95808003', '94991641', '编辑业务功能', 3, 0, NULL, NULL, 1, 1, 'GET:security:/business-function/{id};(main)POST:security:/business-function', NULL, 'admin', '2026-06-08 10:00:00', 'admin', '2026-06-08 10:00:00', 0, NULL, NULL, 1, 1, '94991641_bf_edit', '编辑业务功能配置，包含业务名称、简介、详细介绍及关联表模型');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('134159585', NULL, 'API_KEY', 2, 3, NULL, '/sub-system/apikey', 1, 1, NULL, NULL, 'admin', '2026-07-14 10:19:08.372506', 'admin', '2026-07-14 10:19:08.372506', 0, NULL, NULL, 2, 1, NULL, '当前登录用户创建、查看和删除自己的API_KEY');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95775769', '94991641', '编辑', 3, 0, NULL, NULL, 1, 1, 'POST:security:/tablemodel/updateTableComment;POST:security:/tablemodel/column/updateComment;(main)POST:security:/tablemodel/foreignKey/save;DELETE:security:/tablemodel/foreignKey/delete;POST:security:/tablemodel/column/updateDictKey;POST:security:/dict/page', NULL, 'admin', '2026-05-19 21:32:51.899006', 'admin', '2026-06-09 16:52:46.958109', 0, NULL, NULL, 1, 1, '94991641_edit', '# 功能介绍
编辑表模型注释、字段注释、外键信息
# 界面布局
以抽屉的样式弹出 ，上中下布局 。
- 上面部分展示表的基本信息，只有`表注释`支持更改。
- 中间部分字段列表 ， 只有`字段注释`支持更改
- 下面部分外键信息 ，采集的数据只能修改`备注` ，自定义添加的支持修改所有信息
注释修改方式：点击输入框 -> 输入文本 -> 焦点脱离自动保存
字段枚举值修改方式：下拉框选择支持搜索 ，清空枚举值时需要hover选中指定select,清除按钮会展示。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('22', '1', '部门管理', 2, 1, '', '/sub-system/dept', 1, 1, '(main)GET:system:/dept/tree;GET:system:/dept/types;GET:system:/dept/{id};GET:system:/dept/{id}/children', NULL, NULL, '2026-04-26 13:17:42.661806', 'admin', '2026-06-09 16:56:36.430265', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
构建组织架构体系，为数据权限和用户归属提供部门维度支撑。
- 部门树维护：管理多层级部门结构，支持多种部门类型（公司、分公司、部门、小组、虚拟团队），每种类型有独立图标标识。部门支持启用/禁用，禁用部门在树中灰色显示。
- 多父部门：一个部门可关联多个父部门（一个主父部门+多个额外父部门），支持灵活的矩阵式组织结构。
- 部门下用户管理：查看部门下的用户列表，支持设置主部门和移除用户。
- 组织架构图：全屏可视化展示整体组织架构，支持缩放和拖拽浏览。

# 界面布局
页面为左右分栏：左侧部门树 + 右侧详情面板（可拖拽调整宽度）。
- 部门树面板：搜索框 + 树形列表（按类型显示不同图标，禁用部门灰色）+ 底部新增操作。节点hover可新增子部门。
- 详情面板：三个Tab页——基本信息（名称、类型、状态、排序、主父部门、层级路径、额外父部门）、关系图（G6可视化，展示当前部门与父/子部门关系）、用户列表（用户名、昵称、是否主部门、操作）。
- 新增/编辑部门：弹窗表单，含部门名称、类型、父部门（树形选择）、启用状态、排序。
- 添加父部门：弹窗中选择部门树节点，当前部门和已有父部门不可选。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('5', '2', '角色管理', 2, 0, '', '/sub-security/role', 1, 1, 'GET:security:/role/enums/role-type;(main)POST:security:/role/page;GET:security:/role/enums/data-scope', NULL, NULL, '2026-04-15 09:26:55.897731', 'admin', '2026-06-09 16:56:51.210354', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
管理权限分配的核心载体，通过角色将菜单权限、数据范围、AI查询能力打包授权给用户。
- 角色维护：管理角色的编码、名称、类型（系统/业务）、数据范围、状态等基本信息。系统角色不可禁用，内置角色（super_admin/common）操作受限。
- 菜单权限分配：为角色分配可访问的菜单和按钮。支持时效控制——可设置永久有效、绝对时间范围或周期性生效（按周/按月+每日时段），不同时效组可关联不同菜单集合。
- AI表模型权限：控制角色对AI查询表模型的访问范围，可配置每个表的字段是否允许查询及脱敏策略（支持用户名、身份证、手机号、邮箱、地址等预设策略和自定义脱敏）。
- 用户关联：通过穿梭框为角色分配用户，建立角色与用户的多对多关系。

# 界面布局
页面为单页结构：顶部搜索栏 + 下方角色列表表格。
- 搜索栏：支持按角色名称、角色类型、数据范围、状态筛选。
- 列表表格：展示角色编码、名称、类型、数据范围、状态（支持直接切换）、操作入口。支持多选批量删除。
- 新增/编辑角色：弹窗表单，含角色编码（编辑不可改）、名称、描述、排序、类型、数据范围、状态。
- 菜单权限配置：宽弹窗左右分栏——左侧时效分组列表（新增/删除时效组），右侧菜单树勾选面板（按所属类型Tab切换 + 时效配置表单 + 菜单树勾选，按钮节点以内联Tag展示）。
- AI表模型权限配置：宽弹窗左右分栏——左侧表模型列表（支持搜索和新增），右侧字段配置表格（允许查询开关、是否脱敏开关、脱敏策略选择、自定义脱敏参数）。
- 关联用户：穿梭框弹窗，左侧未关联用户、右侧已关联用户，支持搜索。
- 详情查看：右侧抽屉，展示角色完整信息。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72974723', '2', '数据权限管理', 2, 2, NULL, '/sub-security/dataresource', 1, 1, 'GET:security:/data-resource/enums/assert-type;GET:security:/data-resource/enums/condition-type;GET:system:/basic/dataResourceAttribute;(main)POST:security:/data-resource/page', NULL, 'admin', '2026-04-30 14:23:21.750572', 'admin', '2026-06-09 16:35:19.058938', 0, NULL, NULL, 1, 1, NULL, '## 功能介绍
配置数据表级别的行级权限过滤规则，使不同权限的用户在查询数据时自动受到数据范围约束，实现透明的行级数据权限控制。配置的规则会在运行时通过SQL拦截器自动追加WHERE条件，无需业务代码感知权限过滤逻辑。

- 数据资源配置：维护需要受控的数据表规则。每条规则指定一个库（留空则匹配所有库）和一个表名，并可配置多条字段过滤条件。每条条件定义了：需要过滤的数据库字段名、映射到用户资源中的哪些属性值作为过滤依据、断言方式（等于精确匹配或LIKE模糊匹配）、多条件之间的关联关系（AND/OR）、以及是否在用户无对应资源值时仍显示该数据（显示Null）。支持启用/禁用规则，禁用的规则不参与运行时过滤。
- SELF_ONLY模式：为表配置"仅查看自己数据"的能力。开启后指定表中代表数据创建者的字段名，运行时当用户数据范围为SELF_ONLY时，系统自动追加该字段等于当前用户名的过滤条件，无需额外配置字段条件。
- 规则同步：配置变更后需同步至缓存方可生效，确保运行时拦截器能获取到最新规则。

## 界面布局
页面为单页结构：顶部搜索栏 + 下方数据资源列表表格。
- 搜索栏：支持按表名、库名模糊搜索，以及按启用/禁用状态筛选。
- 列表表格：展示库名（为空显示"全部"标签）、表名、描述、条件数量、启用状态（支持直接切换）、操作入口。支持多选行进行批量删除。
- 新增/编辑：以弹窗形式打开，包含基本信息表单（库名、表名、描述、是否支持SELF_ONLY模式及对应字段、启用状态）和可编辑的条件列表表格。条件表格每行可配置字段名、用户资源字段（多选）、断言类型、关联关系、是否显示Null、排序号，支持动态增删条件行。
- 详情查看：以右侧抽屉打开，上方展示基本信息（库名、表名、描述、SELF_ONLY配置、状态、创建时间），下方展示字段条件只读表格（字段名、用户资源字段、断言类型、关联关系、是否显示Null、排序号）。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('4', '1', '用户管理', 2, 0, NULL, '/sub-system/user', 1, 1, '(main)POST:system:/manager/page;GET:system:/dept/user-count;GET:system:/dept/tree;GET:system:/manager/{id}', NULL, NULL, '2026-04-15 09:26:55.897731', 'admin', '2026-06-09 16:56:18.165346', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
管理用户账号全生命周期，包括身份信息、登录凭证、组织归属和角色授权。
- 用户信息维护：管理用户名、昵称、邮箱、手机、性别等基本信息，支持启用/禁用和批量删除。用户名创建后不可修改。
- 账号绑定：为用户绑定多种登录方式（用户名密码、手机号、微信），支持绑定和解绑操作。
- 部门关联：管理用户所属部门，支持多部门归属，需指定一个主部门。
- 角色分配：为用户分配角色，通过角色间接获得菜单权限和数据权限。

# 界面布局
页面为左右分栏：左侧部门树选择器（显示各部门用户数，点击筛选）+ 右侧用户列表（可拖拽调整宽度）。
- 用户列表：标题区（当前部门+用户总数）+ 筛选区（关键词搜索+状态筛选）+ 操作栏（新增+批量删除）+ 表格（用户名、昵称、邮箱、手机、状态、操作）。支持多选批量删除。
- 新增用户：右侧抽屉，含用户名、昵称、初始密码、所属部门（树形选择）、邮箱、手机、性别。
- 用户详情/编辑：右侧宽抽屉，头部显示头像和状态，三个区块——基本信息（只读/可编辑切换）、账号绑定（三种登录方式的绑定/解绑）、部门关联（已关联部门列表+添加部门树勾选）。
- 分配角色：弹窗形式，展示所有启用角色列表，支持搜索和全选，已分配角色标记显示。
- 修改密码：右侧抽屉，输入新密码和确认密码。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('6', '2', '菜单管理', 2, 1, NULL, '/sub-security/menu', 1, 1, '(main)POST:security:/menu/tree;GET:security:/menu/tree/{owner}/buttons/{menuId};GET:security:/menu/enums/owners;GET:security:/menu/enums/positions', NULL, NULL, '2026-04-15 09:26:55.897731', 'admin', '2026-06-09 16:57:30.41982', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
构建系统的导航结构与权限体系，定义用户可访问的页面和可执行的操作。
- 菜单树管理：维护多终端（按所属类型区分）的菜单层级结构，支持目录和菜单两种节点类型。每个菜单可配置路由路径、图标、排序、可见性和功能描述，功能描述用于AI理解界面能力。菜单支持启用/禁用，禁用后不参与权限分配。
- 接口权限绑定：为菜单关联后端API接口，生成权限标识（格式：`请求方法:模块前缀:接口路径`），支持多接口绑定并指定主接口。菜单下的按钮也支持独立绑定接口权限。
- 按钮权限管理：在菜单下维护操作按钮，每个按钮通过标识后缀自动生成唯一权限标识（格式：`菜单ID_后缀`），用于按钮级权限控制。

# 界面布局
页面为左右分栏：顶部终端切换栏 + 下方左侧菜单树 + 右侧详情面板（可拖拽调整宽度）。
- 菜单树面板：位置切换栏 + 搜索框 + 树形列表（目录/菜单节点，支持搜索过滤）+ 底部新增操作。
- 详情面板：选中菜单后展示三个区域——基本信息（名称、类型、路径、排序、图标、状态、可见性、功能描述）、接口权限（已绑定接口Tag列表）、按钮管理表格（按钮名称、功能描述、按钮标识、接口权限）。
- 新增/编辑菜单：弹窗表单，含菜单名称、功能描述（Markdown编辑器）、类型、父菜单（树形选择）、路由路径、图标选择器、排序、可见性、状态。
- 新增/编辑按钮：弹窗表单，含按钮名称、功能描述（Markdown编辑器）、标识后缀、接口权限选择。
- 接口资源选择器：弹窗形式，支持按模块和关键词搜索接口，选择后生成权限标识，多接口时需指定主接口。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83953705', '5', '新增角色', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/role', NULL, 'admin', '2026-05-02 19:03:33.371084', 'admin', '2026-05-02 19:03:33.371084', 0, NULL, NULL, 1, 1, '5_add', '添加新角色');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83956233', '5', '删除角色', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/role', NULL, 'admin', '2026-05-02 19:08:49.862952', 'admin', '2026-05-02 19:08:49.862952', 0, NULL, NULL, 1, 1, '5_remove', '批量删除角色');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('88007033', '72974723', '删除权限', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/data-resource/delete', NULL, 'admin', '2026-05-08 15:47:59.364827', 'admin', '2026-05-08 15:47:59.364827', 0, NULL, NULL, 1, 1, '72974723_remove', '批量删除数据表模型权限过滤条件');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('88004425', '72974723', '新增权限', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/data-resource', NULL, 'admin', '2026-05-08 15:42:33.426123', 'admin', '2026-05-08 15:42:33.426123', 0, NULL, NULL, 1, 1, '72974723_add', '新增数据表模型权限条件过滤项');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('88005737', '72974723', '编辑权限', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/data-resource', NULL, 'admin', '2026-05-08 15:45:17.979199', 'admin', '2026-05-08 15:45:17.979199', 0, NULL, NULL, 1, 1, '72974723_edit', '编辑数据表模型权限过滤条件');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('88007977', '72974723', '同步权限', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/data-resource/sync', NULL, 'admin', '2026-05-08 15:49:57.312964', 'admin', '2026-05-08 15:49:57.312964', 0, NULL, NULL, 1, 1, '72974723_sync', '将最新的表模型过滤条件权限同步到缓存，同步到缓存才能生效');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('88085073', '5', '关联用户', 3, 0, NULL, NULL, 1, 1, '(main)PUT:security:/role/allocationSubject/{roleId};POST:system:/basic/page/userInfo;GET:security:/role/{roleId}/subjects', NULL, 'admin', '2026-05-08 18:30:34.973888', 'admin', '2026-05-08 18:30:34.973888', 0, NULL, NULL, 1, 1, '5_association_user', '给指定角色批量绑定用户');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('90851377', '5', '字段权限', 3, 0, NULL, NULL, 1, 1, NULL, NULL, 'admin', '2026-05-12 18:33:42.415463', 'admin', '2026-05-12 18:33:42.415463', 0, NULL, NULL, 1, 1, '5_field_permission', '给角色配置字段权限');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82462529', '6', '新增菜单', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/menu', NULL, 'admin', '2026-04-30 15:16:56.228805', 'admin', '2026-05-19 19:27:51.628078', 0, NULL, NULL, 1, 1, '6_add', '新增目录或新增菜单');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83954329', '5', '编辑角色', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/role;PUT:security:/role/status', NULL, 'admin', '2026-05-02 19:04:51.87522', 'admin', '2026-05-12 18:42:17.384576', 0, NULL, NULL, 1, 1, '5_edit', '编辑已存在的角色信息');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('100292272', '99868209', '编辑资料', 3, 0, NULL, NULL, 1, 1, '(main)PUT:system:/manager/profile', NULL, 'admin', '2026-07-08 22:10:00', 'admin', '2026-07-08 22:10:00', 0, NULL, NULL, 2, 1, '99868209_edit_profile', '# 功能介绍
修改当前登录用户的昵称、性别、邮箱和手机号
# 界面布局
弹框表单，编辑个人资料并保存');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('100292273', '99868209', '修改密码', 3, 0, NULL, NULL, 1, 1, '(main)PUT:system:/manager/password', NULL, 'admin', '2026-05-26 10:22:14.205881', 'admin', '2026-05-26 10:22:14.205881', 0, NULL, NULL, 2, 1, '99868209_change_password', '# 功能介绍
修改当前登录用户的密码
# 界面布局
弹框，输入旧密码和新密码');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('94991641', '2', 'AI表模型管理', 2, 3, NULL, '/sub-security/tablemodel', 1, 1, '(main)POST:security:/tablemodel/page;GET:security:/tablemodel/detail;POST:security:/business-function/page', NULL, 'admin', '2026-05-18 18:19:15.25493', 'admin', '2026-06-08 17:31:17.593911', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
增强智能助手的数据库查询能力，使AI更准确理解业务语义并生成正确的SQL查询。
- 表模型管理：维护智能助手可查询的表模型基本信息（表信息、字段信息、外键等）。表和字段的注释描述直接影响AI对表模型的理解程度，进而影响查询数据的准确度。支持从接口权限采集和自定义添加两种方式录入表模型，支持同步更新和修改数据源。
- 业务功能配置：将表模型与业务场景关联，为AI提供业务语义上下文。可配置业务名称、业务简介、详细介绍（支持Markdown，可编写业务规则、状态说明、查询示例等），并关联相关表模型。业务描述能帮助AI理解如"order_status=3代表已退款"这类字段值的业务含义，显著提升查询准确度。

# 界面布局
页面分为两个Tab页：
1.表模型管理：顶部搜索栏 + 下方表模型列表表格。
2.业务功能配置：顶部搜索栏 + 下方业务功能列表表格（展示业务名称、简介、关联表数量）。新增/编辑以右侧抽屉打开，包含：业务名称、简介输入框、Markdown编辑器（编辑/预览/分屏模式）、关联表模型选择器（左侧模块列表 + 右侧表勾选列表）。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82470993', '6', '删除按钮', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/menu', NULL, 'admin', '2026-04-30 15:34:34.213706', 'admin', '2026-04-30 15:34:34.213706', 0, NULL, NULL, 1, 1, '6_remove_button', '删除界面的按钮权限');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82470289', '6', '编辑按钮', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/menu;POST:security:/apiResource/page', NULL, 'admin', '2026-04-30 15:33:06.513462', 'admin', '2026-04-30 15:34:53.899148', 0, NULL, NULL, 1, 1, '6_edit_button', '编辑界面的按钮权限');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72970706', '4', '编辑用户', 3, 0, NULL, NULL, 1, 1, 'POST:system:/manager/{id}/account;DELETE:system:/manager/{id}/account/{accountId};DELETE:system:/user-dept;POST:system:/user-dept;PUT:system:/user-dept/primary;(main)POST:system:/manager;GET:system:/manager/dingtalk/bindable', NULL, 'admin', '2026-04-30 10:52:37.283618', 'admin', '2026-04-30 14:57:40.581134', 0, NULL, NULL, 1, 1, '4_edit', '编辑已有用户信息，包含基本信息、账号绑定/解绑、部门关联');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72970506', '4', '重置密码', 3, 0, NULL, NULL, 1, 1, '(main)PUT:system:/manager/{id}/password', NULL, 'admin', '2026-04-30 11:03:36.107681', 'admin', '2026-04-30 11:03:36.107681', 0, NULL, NULL, 1, 1, '4_change_pwd', '重置用户密码');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83983617', '4', '分配角色', 3, 0, NULL, NULL, 1, 1, '(main)PUT:security:/role/allocationRole/{subjectId};GET:security:/role/list;GET:security:/role/list/{subjectId}', NULL, 'admin', '2026-05-02 20:05:52.505684', 'admin', '2026-05-02 20:05:52.505684', 0, NULL, NULL, 1, 1, '4_role', '给用户分配角色');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83961041', '5', '菜单权限', 3, 0, NULL, NULL, 1, 1, 'GET:security:/role/valid-groups/{roleId};GET:security:/role/menu-tree;GET:security:/menu/enums/owners;GET:security:/menu/enums/positions;(main)POST:security:/role/valid-group', NULL, 'admin', '2026-05-02 19:18:50.830662', 'admin', '2026-05-12 19:16:42.149486', 0, NULL, NULL, 1, 1, '5_menu_permission', '# 功能
给角色配置菜单权限（包含持久权限和临时权限）
# 界面布局
1.左侧展示已添加的时效分组列表 和新增时效分组按钮。
2.右侧展示角色拥有的权限回显在菜单树上（菜单权限和按钮权限）。
3.编辑指定时效分组的权限操作：先点击左侧对应的时效分组，然后再点击`编辑权限`。
4.时效配置类型包括如下：
  - 永久：配置永久权限
  - 绝对时间范围：配置在指定的时间段内拥有权限的临时权限
   绝对时间范围的时间格式：yyyy-MM-dd HH:mm
  - 周期性：配置周期性拥有权限的临时权限 ， 支持按周配置和按月配置
    周期性时间范围配置策略：1.开始时间、结束时间都不配置：全天范围都有权限。2.开始时间不配置 ， 结束时间配置：0点到配置的结束时间之内有权限。3.开始时间配置，结束时间不配置：配置的开始时间到当天23:59:59时间内有权限
    周期性的时间格式：HH:mm
**注意**：权限树中选择父级节点时，子节点不会被选中，必须所有权限都点击，包括菜单和按钮
');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('90851569', '5', 'AI表模型', 3, 0, NULL, NULL, 1, 1, 'GET:security:/roleTableModel/getTableModelPermission/{roleId};(main)POST:security:/roleTableModel;POST:security:/tablemodel/page;POST:security:/business-function/page;GET:security:/business-function/{id}', NULL, 'admin', '2026-05-12 18:34:06.440815', 'admin', '2026-08-04 18:40:23.389559', 0, NULL, NULL, 1, 1, '5_table_model_permission', '# 功能介绍：
给角色配置AI表模型权限 , 拥有该角色的用户会自动拥有对应的表模型权限 ，有对应的表模型权限，智能助手才能查询该表的数据。
功能列表：
- 配置有哪些表的权限。
- 配置指定表中哪些字段的权限。
- 配置哪些字段返回时需要脱敏以及脱敏规则。
# 界面布局
以弹窗的形式展现，布局以左右分栏的形式呈现。
- 左侧为已拥有权限的表名列表 ， 以及新增表模型权限按钮。
- 右侧为对应表的字段权限列表，支持给指定字段配置是否允许展示、是否需要脱敏展示以及脱敏规则能力。
点击左侧表名，右侧展示该表的字段权限信息，修改后点击下面的保存按钮，进行保存。
# 新增
弹框中有新增按钮，会展示二级弹框来用于展示可新增的表模型，支持“按表模型添加”和“按业务功能添加”。
- 按表模型添加：添加指定表模型
- 按业务功能添加：通过‘AI表模型管理’中已经配置好的业务功能批量添加表模型
');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95765193', '94991641', '采集', 3, 0, NULL, NULL, 1, 1, 'POST:security:/tablemodel/uncollectedCount;POST:security:/tablemodel/listUncollected;(main)POST:security:/tablemodel/collect', NULL, 'admin', '2026-05-19 21:10:49.613139', 'admin', '2026-05-19 21:14:50.149945', 0, NULL, NULL, 1, 1, '94991641_collected', '# 功能介绍
从已有的接口资源权限来采集对应关联的表模型数据
# 界面布局
以弹框的形式展现，共有4个步骤
- 第一步：`选择模块`。
- 第二步：`确认采集` ， 会列出该模块未采集的表模型列表。
- 第三步： `采集中` ， 展示采集的进度条。
- 第四步：`完成`');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95769137', '94991641', '自定义添加', 3, 0, NULL, NULL, 1, 1, 'GET:security:/apiResource/getDatasourceList;POST:security:/tablemodel/table/info;(main)POST:security:/tablemodel/customSave', NULL, 'admin', '2026-05-19 21:19:02.050331', 'admin', '2026-05-19 21:19:02.050331', 0, NULL, NULL, 1, 1, '94991641_custom_add', '# 功能介绍
从指定的模块 -> 数据源中选择表来添加`自定义`类型的表模型,该中方式是为了可以让用户自定义添加表模型，增加灵活度。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95771697', '94991641', '删除', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/tablemodel/batchDelete', NULL, 'admin', '2026-05-19 21:24:22.683543', 'admin', '2026-05-19 21:24:22.683543', 0, NULL, NULL, 1, 1, '94991641_remove', '# 功能介绍
批量删除表模型配置
');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95776377', '94991641', '同步', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/tablemodel/sync/{tableModelId}', NULL, 'admin', '2026-05-19 21:34:07.894171', 'admin', '2026-05-19 21:34:07.894171', 0, NULL, NULL, 1, 1, '94991641_sync', '# 功能介绍
从数据库重新同步表模型最新信息
');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('95777617', '94991641', '修改数据源', 3, 0, NULL, NULL, 1, 1, 'POST:security:/apiResource/listByTableModel;GET:security:/apiResource/getDatasourceList;(main)POST:security:/tablemodel/changeDatasource', NULL, 'admin', '2026-05-19 21:36:42.938929', 'admin', '2026-05-19 21:36:42.938929', 0, NULL, NULL, 1, 1, '94991641_change_datasource', '# 功能介绍
请输入...
# 界面布局
暂无描述');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('2', NULL, '安全中心', 1, 2, 'security', '/sub-security', 1, 1, NULL, NULL, NULL, '2026-04-15 09:26:55.893534', NULL, NULL, 0, NULL, NULL, 1, 1, NULL, NULL);
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('99868209', NULL, '个人信息', 2, 0, NULL, '/sub-system/profile', 1, 1, '(main)GET:security:/role/rolesByCurrUser', NULL, 'admin', '2026-05-25 19:38:46.964226', 'admin', '2026-05-26 10:20:56.877785', 0, NULL, NULL, 2, 1, NULL, '展示当前登录用户的基本信息');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83966681', '22', '移除用户', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:system:/user-dept', NULL, 'admin', '2026-05-02 19:30:35.028079', 'admin', '2026-05-02 19:30:35.028079', 0, NULL, NULL, 1, 1, '22_remove_user', '将指定部门的指定用户移除');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82443129', '22', '新增部门', 3, 0, NULL, NULL, 1, 1, '(main)POST:system:/dept', NULL, 'admin', '2026-04-30 14:36:31.760361', 'admin', '2026-04-30 14:36:31.760361', 0, NULL, NULL, 1, 1, '22_add', '增加新部门');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82445033', '22', '删除部门', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:system:/dept/{id}', NULL, 'admin', '2026-04-30 14:40:29.976358', 'admin', '2026-04-30 14:40:29.976358', 0, NULL, NULL, 1, 1, '22_remove', '删除已存在的部门');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82444249', '22', '编辑部门', 3, 0, NULL, NULL, 1, 1, '(main)POST:system:/dept;POST:system:/dept/{id}/parent/{parentId};DELETE:system:/dept/{id}/parent/{parentId}', NULL, 'admin', '2026-04-30 14:38:51.323672', 'admin', '2026-04-30 14:42:44.541107', 0, NULL, NULL, 1, 1, '22_edit', '修改部门信息');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('83968065', '22', '设为主部门', 3, 0, NULL, NULL, 1, 1, '(main)PUT:system:/user-dept/primary', NULL, 'admin', '2026-05-02 19:33:28.757686', 'admin', '2026-05-02 19:33:28.757686', 0, NULL, NULL, 1, 1, '22_primary_dept', '将指定部门设为用户的主部门');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72970702', '4', '新增用户', 3, 0, NULL, NULL, 1, 1, '(main)POST:system:/manager', NULL, 'admin', '2026-04-29 19:58:52.743726', 'admin', '2026-04-30 10:43:31.266172', 0, NULL, NULL, 1, 1, '4_add', '创建新用户');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72970701', '4', '删除用户', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:system:/manager', NULL, 'admin', '2026-04-30 10:43:15.961101', 'admin', '2026-04-30 10:43:15.961101', 0, NULL, NULL, 1, 1, '4_remove', '批量删除选中用户');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('72970704', '4', '启禁用用户', 3, 0, NULL, NULL, 1, 1, '(main)PUT:system:/manager/{id}/status', NULL, 'admin', '2026-04-30 10:59:49.483349', 'admin', '2026-04-30 11:00:09.02006', 0, NULL, NULL, 1, 1, '4_disabled_enable', '禁用/启用用户');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82464801', '6', '删除菜单', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/menu', NULL, 'admin', '2026-04-30 15:21:40.70572', 'admin', '2026-04-30 15:21:40.70572', 0, NULL, NULL, 1, 1, '6_remove', '删除已有菜单');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82463377', '6', '编辑菜单', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/menu;POST:security:/apiResource/page', NULL, 'admin', '2026-04-30 15:18:42.55019', 'admin', '2026-04-30 15:23:49.969804', 0, NULL, NULL, 1, 1, '6_edit', '编辑已有的目录或菜单');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('82466385', '6', '新增按钮', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/menu;POST:security:/apiResource/page', NULL, 'admin', '2026-04-30 15:24:58.750576', 'admin', '2026-04-30 15:25:39.662048', 0, NULL, NULL, 1, 1, '6_add_button', '新增界面的按钮权限');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('1', NULL, '用户中心', 1, 1, 'UserOutlined', '/sub-system', 1, 1, NULL, NULL, NULL, '2026-04-15 09:26:55.893534', 'admin', '2026-04-30 15:28:24.006434', 0, NULL, NULL, 1, 1, NULL, '包含用户相关内容');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('130300497', '129590745', '清理日志', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/job/log/clearLog', NULL, 'admin', '2026-07-08 20:19:22.42503', 'admin', '2026-07-08 20:32:07.748707', 0, NULL, NULL, 1, 1, '129590745_job_clear_log', '# 功能介绍
通过选择指定清理范围来清理定时调度日志');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129590745', '2', '定时调度', 2, 2, NULL, '/sub-security/job', 1, 1, '(main)POST:kit:/job/info/page;GET:kit:/job/info/nextTriggerTime', NULL, 'admin', '2026-07-07 19:40:43.372513', 'admin', '2026-07-08 21:11:26.57838', 0, NULL, NULL, 1, 1, NULL, '配置定时任务');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129591489', '129590745', '编辑', 3, 0, NULL, NULL, 1, 1, 'GET:kit:/job/info/handlerList;POST:security:/apiResource/page;(main)POST:kit:/job/info/updateByDTO', NULL, 'admin', '2026-07-07 19:42:16.859549', 'admin', '2026-07-08 20:55:37.365827', 0, NULL, NULL, 1, 1, '129590745_job_edit', '# 功能介绍
编辑定时调度任务
# 界面布局
弹框形式弹出，通过三个步骤来配置任务信息：
- 公共配置：任务名称、报警邮件、过期策略、路由策略、阻塞策略、超时时间、重试时间
- 任务类型：配置任务模式以及对应模式下的特定信息，下面介绍支持的任务模式：
  （1）平台URL:直接调用平台的相关任务接口，需要配置：所属服务、接口url、请求体数据。
  （2）GLUE: 执行特定脚本，需要配置：GLUE类型（Java,shell,python3,Nodejs,php,powerShell）、任务参数、脚本代码
  （3）BEAN: 调用代码中注册的置顶handler ,需要配置：Handler ,任务参数
- 调度配置：配置任务触发的执行周期和关联触发的子任务。
执行周期支持：固定速率 和 CRON');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129592857', '129590745', '立即执行', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/job/info/trigger', NULL, 'admin', '2026-07-07 19:45:07.704116', 'admin', '2026-07-08 21:02:26.680862', 0, NULL, NULL, 1, 1, '129590745_job_trigger', '# 功能介绍
立即执行一次指定任务
# 界面布局
弹款弹出，可自定义配置扩展参数');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129591321', '129590745', '新增', 3, 0, NULL, NULL, 1, 1, 'GET:kit:/job/info/handlerList;POST:security:/apiResource/page;(main)POST:kit:/job/info/addByDTO', NULL, 'admin', '2026-07-07 19:41:55.325592', 'admin', '2026-07-08 20:55:51.449145', 0, NULL, NULL, 1, 1, '129590745_job_add', '# 功能介绍
新增定时调度任务
# 界面布局
弹框形式弹出，通过三个步骤来配置任务信息：
- 公共配置：任务名称、报警邮件、过期策略、路由策略、阻塞策略、超时时间、重试时间
- 任务类型：配置任务模式以及对应模式下的特定信息，下面介绍支持的任务模式：
  （1）平台URL:直接调用平台的相关任务接口，需要配置：所属服务、接口url、请求体数据。
  （2）GLUE: 执行特定脚本，需要配置：GLUE类型（Java,shell,python3,Nodejs,php,powerShell）、任务参数、脚本代码
  （3）BEAN: 调用代码中注册的置顶handler ,需要配置：Handler ,任务参数
- 调度配置：配置任务触发的执行周期和关联触发的子任务。
执行周期支持：固定速率 和 CRON');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129593138', '129590745', '调度日志', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/job/log/page;GET:kit:/job/info/logContent;GET:kit:/job/log/load', NULL, 'admin', '2026-07-07 19:45:42.147505', 'admin', '2026-07-08 20:39:04.260717', 0, NULL, NULL, 1, 1, '129590745_job_log', '# 功能介绍
查看指定任务的调度日志，展示调度时间、调度结果、执行结果、执行器地址，查看执行日志详情和终止运行中任务
# 界面布局
抽屉弹框形式弹出，分页展示调度日志');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129591642', '129590745', '删除', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/job/info/remove', NULL, 'admin', '2026-07-07 19:42:35.077819', 'admin', '2026-07-08 20:57:11.270665', 0, NULL, NULL, 1, 1, '129590745_job_remove', '# 功能介绍
删除指定定时调度任务');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('129592089', '129590745', '启动/停止', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/job/info/start;POST:kit:/job/info/stop', NULL, 'admin', '2026-07-07 19:43:31.615094', 'admin', '2026-07-08 21:00:25.377924', 0, NULL, NULL, 1, 1, '129590745_job_start', '# 功能介绍
启用和停用指定任务');
-- 知识文档管理
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('138666105', '2', '知识文档管理', 2, 6, NULL, '/sub-security/knowledge', 1, 1, '(main)POST:kit:/knowledge/document/page;POST:kit:/knowledge/page/page;GET:security:/role/list;POST:kit:/knowledge/page/{pageId};POST:kit:/knowledge/search;POST:kit:/knowledge/chunk/adjacent', NULL, 'admin', '2026-07-20 22:47:43.117815', 'admin', '2026-07-27 15:50:52.120414', 0, NULL, NULL, 1, 1, NULL, '# 功能介绍
知识库页面用于统一管理系统中的知识文档、Wiki 页面与检索内容，支持知识内容的沉淀、维护与检索验证。页面面向知识运营和内容维护场景，帮助系统持续积累可用于智能问答与知识召回的结构化内容。

# 界面布局
页面采用 Tab 分区布局，主要分为“文档管理”“Wiki 页面”“知识检索”三个区域。
- 文档管理区域以上方筛选区和下方文档列表为主，用于查看知识文档的基础信息、处理状态、授权范围与启用状态。
- Wiki 页面区域以上方筛选区和下方页面列表为主，用于查看由知识文档沉淀出的页面内容与版本信息，并进入页面详情查看完整内容。
- 知识检索区域以上方检索条件区、中部结果列表和详情内容区组成，用于按关键词查看知识命中结果，并结合上下文内容理解检索效果。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('143294233', '138666105', '删除文档', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/document/delete/{documentId}', NULL, 'admin', '2026-07-27 15:29:40.001163', 'admin', '2026-07-27 15:40:59.838631', 0, NULL, NULL, 1, 1, '138666105_knowledge_remove', '# 功能介绍
删除指定文档，会将已经解析的Wiki Page一起删除');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('143293897', '138666105', '启/禁用文档', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/document/enable/{documentId};POST:kit:/knowledge/document/disable/{documentId}', NULL, 'admin', '2026-07-27 15:28:57.860736', 'admin', '2026-07-27 15:40:30.375485', 0, NULL, NULL, 1, 1, '138666105_knowledge_disabled_enable', '# 功能介绍
启用或禁用文档，禁用的文档将不会被检索到');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('138666729', '138666105', '编辑Wiki', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/page/save', NULL, 'admin', '2026-07-20 22:49:01.616962', 'admin', '2026-07-27 15:39:19.372382', 0, NULL, NULL, 1, 1, '138666105_knowledge_page_edit', '# 功能介绍
编辑Wiki知识文档
# 界面布局
位置在Wiki Page 页面的指定page的`查看`界面内的 `Block 编辑` sheet页中。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('138666611', '138666105', '重新导入', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/task/retry/{taskId}', NULL, 'admin', '2026-07-20 22:48:46.986848', 'admin', '2026-07-27 15:40:07.015317', 0, NULL, NULL, 1, 1, '138666105_knowledge_task_retry', '# 功能介绍
将指定文档重新解析，拆分Page');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('138666498', '138666105', '权限编辑', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/document/role/save', NULL, 'admin', '2026-07-20 22:48:32.736763', 'admin', '2026-07-27 15:42:28.498598', 0, NULL, NULL, 1, 1, '138666105_knowledge_role_edit', '# 功能介绍
给指定文档编辑权限 ，只有分配了指定角色的用户才能检索到对应的文档 ，不分配为公共文档，所有人都可以检索到');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('138666393', '138666105', '上传文档', 3, 0, NULL, NULL, 1, 1, '(main)POST:kit:/knowledge/document/save', NULL, 'admin', '2026-07-20 22:48:19.452958', 'admin', '2026-07-27 15:41:24.523782', 0, NULL, NULL, 1, 1, '138666105_knowledge_upload', '# 功能介绍
将指定文档上传到知识库');

-- OAuth应用管理
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000001', '2', 'OAuth应用管理', 2, 7, NULL, '/sub-security/oauth-client', 1, 1, '(main)POST:security:/oauth/client/page;(main)GET:security:/oauth/client/enums;(main)POST:security:/oauth/scope/page', NULL, 'admin', '2026-09-04 15:45:00', 'admin', '2026-09-04 15:45:00', 0, NULL, NULL, 1, 1, NULL, '维护允许接入OAuth2认证的客户端应用及Scope权限组。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000002', '160000001', '新增OAuth应用', 3, 0, NULL, NULL, 1, 1, 'GET:security:/oauth/scope/options;(main)POST:security:/oauth/client', NULL, 'admin', '2026-09-04 15:45:00', 'admin', '2026-09-04 15:45:00', 0, NULL, NULL, 1, 1, 'oauth_client_add', '新增OAuth客户端应用配置。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000003', '160000001', '编辑OAuth应用', 3, 0, NULL, NULL, 1, 1, 'GET:security:/oauth/client/id/{id};GET:security:/oauth/scope/options;(main)POST:security:/oauth/client', NULL, 'admin', '2026-09-04 15:45:00', 'admin', '2026-09-04 15:45:00', 0, NULL, NULL, 1, 1, 'oauth_client_edit', '编辑OAuth客户端应用配置。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000004', '160000001', '删除OAuth应用', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/oauth/client', NULL, 'admin', '2026-09-04 15:45:00', 'admin', '2026-09-04 15:45:00', 0, NULL, NULL, 1, 1, 'oauth_client_remove', '批量删除OAuth客户端应用配置。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000005', '160000001', '重置OAuth密钥', 3, 0, NULL, NULL, 1, 1, '(main)POST:security:/oauth/client/{id}/secret/reset', NULL, 'admin', '2026-09-04 15:45:00', 'admin', '2026-09-04 15:45:00', 0, NULL, NULL, 1, 1, 'oauth_client_reset_secret', '重置保密客户端的客户端密钥。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000007', '160000001', '新增Scope', 3, 0, NULL, NULL, 1, 1, 'POST:security:/apiResource/page;POST:security:/apiResource/list/by-ids;(main)POST:security:/oauth/scope', NULL, 'admin', '2026-09-08 23:00:00', 'admin', '2026-09-08 23:00:00', 0, NULL, NULL, 1, 1, 'oauth_scope_add', '新增Scope权限组并绑定接口。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000008', '160000001', '编辑Scope', 3, 0, NULL, NULL, 1, 1, 'GET:security:/oauth/scope/{id};POST:security:/apiResource/page;POST:security:/apiResource/list/by-ids;(main)POST:security:/oauth/scope', NULL, 'admin', '2026-09-08 23:00:00', 'admin', '2026-09-08 23:00:00', 0, NULL, NULL, 1, 1, 'oauth_scope_edit', '编辑Scope名称、说明、状态和接口绑定。');
INSERT INTO security_menu (id, parent_id, menu_name, menu_type, sort, icon, path, visible, status, permission, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, position, owner, button_key, description) VALUES ('160000009', '160000001', '删除Scope', 3, 0, NULL, NULL, 1, 1, '(main)DELETE:security:/oauth/scope', NULL, 'admin', '2026-09-08 23:00:00', 'admin', '2026-09-08 23:00:00', 0, NULL, NULL, 1, 1, 'oauth_scope_remove', '删除Scope权限组。');


-- 公共角色"个人中心"绑定
INSERT INTO security_role_menu (id, role_id, menu_id, valid_type, valid_start, valid_end, cycle_type, cycle_value, cycle_start_time, cycle_end_time, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2074860014971060224', '2', '100292273', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', '2026-07-08 22:15:51.261576', 'admin', '2026-07-08 22:15:51.261576', 0, NULL, NULL);
INSERT INTO security_role_menu (id, role_id, menu_id, valid_type, valid_start, valid_end, cycle_type, cycle_value, cycle_start_time, cycle_end_time, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2074860014979448832', '2', '100292272', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', '2026-07-08 22:15:51.264192', 'admin', '2026-07-08 22:15:51.264192', 0, NULL, NULL);
INSERT INTO security_role_menu (id, role_id, menu_id, valid_type, valid_start, valid_end, cycle_type, cycle_value, cycle_start_time, cycle_end_time, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2074860014992031744', '2', '99868209', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', '2026-07-08 22:15:51.266312', 'admin', '2026-07-08 22:15:51.266312', 0, NULL, NULL);
-- 公共角色"API_KEY"绑定
INSERT INTO security_role_menu (id, role_id, menu_id, valid_type, valid_start, valid_end, cycle_type, cycle_value, cycle_start_time, cycle_end_time, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time) VALUES ('2076874974039367680', '2', '134159585', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'admin', '2026-07-14 11:42:34.939892', 'admin', '2026-07-14 11:42:34.939892', 0, NULL, NULL);


INSERT INTO security_role_menu_permission (id, role_menu_id, abac_permission_id, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, api_id) VALUES ('2074860015017197568', '2074860014971060224', '2074860014971060225', NULL, 'admin', '2026-07-08 22:15:51.272835', 'admin', '2026-07-08 22:15:51.272835', 0, NULL, NULL, 'ccc85e9422ba9c46ae1ef7199922e6c5');
INSERT INTO security_role_menu_permission (id, role_menu_id, abac_permission_id, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, api_id) VALUES ('2074860015021391872', '2074860014979448832', '2074860014979448833', NULL, 'admin', '2026-07-08 22:15:51.273461', 'admin', '2026-07-08 22:15:51.273461', 0, NULL, NULL, 'ad359b600ca96121355b9a64df8750f1');
INSERT INTO security_role_menu_permission (id, role_menu_id, abac_permission_id, tenant_id, create_op, create_time, modify_op, modify_time, deleted, delete_op, delete_time, api_id) VALUES ('2074860015021391873', '2074860014992031744', '2074860014992031745', NULL, 'admin', '2026-07-08 22:15:51.273691', 'admin', '2026-07-08 22:15:51.273691', 0, NULL, NULL, '8faec24b53244693328279c72c808e2e');
