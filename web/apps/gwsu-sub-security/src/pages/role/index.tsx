import React, { useState, useCallback, useEffect } from "react";
import {
  Button,
  Table,
  Input,
  Select,
  Tag,
  Switch,
  Dropdown,
  Modal,
  Form,
  Space,
  Popconfirm, type MenuProps,
} from "antd";
import type { TableProps } from "antd";
import {
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  MoreOutlined,
  EyeOutlined,
  EditOutlined,
  MenuOutlined,
  LockOutlined,
  TableOutlined,
  UserOutlined,
} from "@ant-design/icons";
import styles from "./index.module.less";
import RoleDetail from "./components/RoleDetail";
import RoleFormModal from "./components/RoleFormModal";
import MenuPermissionModal from "./components/MenuPermissionModal";
import RelatedUserModal from "./components/RelatedUserModal";
import TableModelPermissionModal from "./components/TableModelPermissionModal";
import KnowledgeDirectoryPermissionModal from './components/KnowledgeDirectoryPermissionModal';
import { useRole } from "./hooks/useRole";
import type { RoleInfo, RoleQuery, EnumOption } from "./types";
import { getRoleTypeOptions, getDataScopeOptions } from "./services/role";
import {AuthGate , useAuth, useUserStore} from '@gwsu/core'
import {
  PERM_ADD, PERM_REMOVE, PERM_EDIT,
  PERM_ASSOCIATION_USER, PERM_MENU_PERMISSION,
  PERM_FIELD_PERMISSION, PERM_TABLE_MODEL_PERMISSION, PERM_KNOWLEDGE_DIRECTORY_PERMISSION,
} from './permissionConstants';

const STATUS_OPTIONS = [
  { label: "启用", value: true },
  { label: "禁用", value: false },
];

const RolePage: React.FC = () => {
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchRolePage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleToggleStatus,
  } = useRole();

  const canEdit = useAuth(PERM_EDIT);
  const canAssociationUser = useAuth(PERM_ASSOCIATION_USER);
  const canMenuPermission = useAuth(PERM_MENU_PERMISSION);
  const canFieldPermission = useAuth(PERM_FIELD_PERMISSION);
  const canTableModelPermission = useAuth(PERM_TABLE_MODEL_PERMISSION);
  const isAdmin = useUserStore((state) => state.userInfo?.admin === true || state.userInfo?.roles?.includes('super_admin') === true);
  const canKnowledgeDirectoryPermission = useAuth(PERM_KNOWLEDGE_DIRECTORY_PERMISSION) || isAdmin;

  const [searchForm] = Form.useForm<RoleQuery>();

  // 从后端加载枚举选项
  const [roleTypeOptions, setRoleTypeOptions] = useState<EnumOption[]>([]);
  const [dataScopeOptions, setDataScopeOptions] = useState<EnumOption[]>([]);

  useEffect(() => {
    getRoleTypeOptions().then(setRoleTypeOptions);
    getDataScopeOptions().then(setDataScopeOptions);
  }, []);

  // 详情抽屉
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailRole, setDetailRole] = useState<RoleInfo | null>(null);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<"create" | "edit">(
    "create"
  );
  const [formModalData, setFormModalData] = useState<RoleInfo | null>(null);

  // 菜单权限弹窗
  const [menuPermVisible, setMenuPermVisible] = useState(false);
  const [menuPermRoleId, setMenuPermRoleId] = useState<string | null>(null);
  const [menuPermRoleName, setMenuPermRoleName] = useState<string>("");

  // 关联用户弹窗
  const [relatedUserVisible, setRelatedUserVisible] = useState(false);
  const [relatedUserRoleId, setRelatedUserRoleId] = useState<string | null>(null);
  const [relatedUserRoleName, setRelatedUserRoleName] = useState<string>("");

  // 表模型权限弹窗
  const [tableModelPermVisible, setTableModelPermVisible] = useState(false);
  const [tableModelPermRoleId, setTableModelPermRoleId] = useState<string | null>(null);
  const [tableModelPermRoleName, setTableModelPermRoleName] = useState<string>("");
  const [knowledgeRole, setKnowledgeRole] = useState<RoleInfo | null>(null);

  // 表格选中行
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /** 初始化加载 */
  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  /** 搜索 */
  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchRolePage({ ...values, pageNum: 1 });
  }, [searchForm, fetchRolePage]);

  /** 重置搜索 */
  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchRolePage({ pageNum: 1 });
  }, [searchForm, fetchRolePage]);

  /** 查看详情 */
  const handleViewDetail = useCallback((role: RoleInfo) => {
    setDetailRole(role);
    setDetailVisible(true);
  }, []);

  /** 新增角色 */
  const handleCreate = useCallback(() => {
    setFormModalMode("create");
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑角色 */
  const handleEdit = useCallback((role: RoleInfo) => {
    setFormModalMode("edit");
    setFormModalData(role);
    setFormModalVisible(true);
  }, []);

  /** 保存角色（由弹窗调用） */
  const handleSave = useCallback(
    async (data: RoleInfo): Promise<boolean> => {
      return handleSaveOrUpdate(data);
    },
    [handleSaveOrUpdate]
  );

  /** 状态切换 */
  const handleStatusChange = useCallback(
    async (role: RoleInfo, checked: boolean) => {
      await handleToggleStatus(role.id!, checked);
    },
    [handleToggleStatus]
  );

  /** 占位功能提示 */
  const handlePlaceholder = useCallback((title: string) => {
    Modal.warning({
      title,
      content: "功能开发中，敬请期待",
    });
  }, []);

  /** 菜单权限配置 */
  const handleMenuPermission = useCallback((role: RoleInfo) => {
    setMenuPermRoleId(role.id ?? null);
    setMenuPermRoleName(role.roleName);
    setMenuPermVisible(true);
  }, []);

  /** 关联用户 */
  const handleRelatedUser = useCallback((role: RoleInfo) => {
    setRelatedUserRoleId(role.id ?? null);
    setRelatedUserRoleName(role.roleName);
    setRelatedUserVisible(true);
  }, []);

  /** 表模型权限配置 */
  const handleTableModelPermission = useCallback((role: RoleInfo) => {
    setTableModelPermRoleId(role.id ?? null);
    setTableModelPermRoleName(role.roleName);
    setTableModelPermVisible(true);
  }, []);

  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    const success = await handleDelete(ids);
    if (success) {
      setSelectedRowKeys([]);
    }
  }, [selectedRowKeys, handleDelete]);

  /** 获取数据范围的文字描述 */
  const getDataScopeLabel = (value: number): string => {
    return dataScopeOptions.find((o) => o.value === value)?.label ?? "未知";
  };

  /**
   * 获取菜单列表
   * @param record
   */
  const getButtonItem = (record: RoleInfo): MenuProps["items"] => {
    const isCommon = record.roleCode === "common";
    let buttons = [];
    if(canEdit && !isCommon){
      buttons.push({
        key: "edit",
        icon: <EditOutlined />,
        label: "编辑",
        onClick: () => handleEdit(record),
      });
    }
    if(canAssociationUser && !isCommon){
      buttons.push({
        key: "relatedUser",
        icon: <UserOutlined />,
        label: "关联用户",
        onClick: () => handleRelatedUser(record),
      });
    }

    if(canMenuPermission){
      buttons.push({
        key: "menuPermission",
        icon: <MenuOutlined />,
        label: "菜单权限",
        onClick: () => handleMenuPermission(record),
      });
    }
    if(canFieldPermission){
      buttons.push({
        key: "fieldPermission",
        icon: <LockOutlined />,
        label: "字段权限",
        onClick: () => handlePlaceholder("字段权限"),
      });
    }
    if(canTableModelPermission){
      buttons.push({
        key: "tablePermission",
        icon: <TableOutlined />,
        label: "AI表模型",
        onClick: () => handleTableModelPermission(record),
      });
    }

    if (canKnowledgeDirectoryPermission) {
      buttons.push({
        key: 'knowledgeDirectoryPermission',
        icon: <LockOutlined />,
        label: '知识目录权限',
        onClick: () => setKnowledgeRole(record),
      });
    }

    return buttons
  };

  /** 表格列定义 */
  const columns: TableProps<RoleInfo>["columns"] = [
    Table.SELECTION_COLUMN,
    {
      title: "序号",
      width: 60,
      align: "center",
      render: (_: unknown, __: RoleInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: "角色编码",
      dataIndex: "roleCode",
      width: 160,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: "角色名称",
      dataIndex: "roleName",
      width: 160,
    },
    {
      title: "角色类型",
      dataIndex: "roleType",
      width: 120,
      render: (val: number) => (
        <Tag color={val === 1 ? "blue" : "orange"}>
          {roleTypeOptions.find((o) => o.value === val)?.label ?? "未知"}
        </Tag>
      ),
    },
    {
      title: "数据范围",
      dataIndex: "dataScope",
      width: 140,
      render: (val: number) => getDataScopeLabel(val),
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 120,
      render: (val: boolean, record: RoleInfo) => {
        const isSystemRole = record.roleType === 1;
        return (
          <Space>
            {!isSystemRole && (
              <AuthGate buttonKey={PERM_EDIT}>
                <Switch
                  size="small"
                  checked={val}
                  data-ai-approval
                  onChange={(checked) => handleStatusChange(record, checked)}
                />
              </AuthGate>
            )}
            <Tag color={val ? "green" : "red"}>{val ? "启用" : "禁用"}</Tag>
          </Space>
        );
      },
    },
    {
      title: "操作",
      width: 200,
      fixed: "right",
      render: (_: unknown, record: RoleInfo) => (
        <div className={styles.actionColumn}>
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Dropdown
            menu={{
              items: getButtonItem(record),
            }}
            disabled={record.roleCode === "super_admin"}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={record.roleCode === "super_admin"}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.rolePage}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>角色名称</span>
            <Form.Item name="roleName" noStyle>
              <Input
                placeholder="请输入角色名称"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>角色类型</span>
            <Form.Item name="roleType" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 140 }}
                options={roleTypeOptions}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>数据范围</span>
            <Form.Item name="dataScope" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 140 }}
                options={dataScopeOptions}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="status" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 120 }}
                options={STATUS_OPTIONS}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button
            type="primary"
            icon={<SearchOutlined />}
            onClick={handleSearch}
          >
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
        </div>
      </div>

      {/* 表格区域 */}
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>角色列表</span>
          <Space>
            <AuthGate buttonKey={PERM_ADD}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增角色
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 个角色？`}
                onConfirm={handleBatchDelete}
                okText="确定"
                cancelText="取消"
                disabled={selectedRowKeys.length === 0}
              >
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                  data-ai-approval
                >
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<RoleInfo>
          rowKey="id"
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          columns={columns}
          dataSource={dataSource}
          loading={loading}
          size="middle"
          scroll={{ x: 960 }}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
        />
      </div>

      {/* 角色详情抽屉 */}
      <RoleDetail
        visible={detailVisible}
        role={detailRole}
        roleTypeOptions={roleTypeOptions}
        dataScopeOptions={dataScopeOptions}
        onClose={() => setDetailVisible(false)}
      />

      {/* 新增/编辑角色弹窗 */}
      <RoleFormModal
        visible={formModalVisible}
        mode={formModalMode}
        data={formModalData}
        roleTypeOptions={roleTypeOptions}
        dataScopeOptions={dataScopeOptions}
        onSave={handleSave}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />

      {/* 菜单权限配置弹窗 */}
      <MenuPermissionModal
        visible={menuPermVisible}
        roleId={menuPermRoleId}
        roleName={menuPermRoleName}
        onClose={() => setMenuPermVisible(false)}
      />

      {/* 关联用户弹窗 */}
      <RelatedUserModal
        visible={relatedUserVisible}
        roleId={relatedUserRoleId}
        roleName={relatedUserRoleName}
        onClose={() => setRelatedUserVisible(false)}
      />

      {/* 表模型权限配置弹窗 */}
      <TableModelPermissionModal
        visible={tableModelPermVisible}
        roleId={tableModelPermRoleId}
        roleName={tableModelPermRoleName}
        onClose={() => setTableModelPermVisible(false)}
      />
      <KnowledgeDirectoryPermissionModal open={!!knowledgeRole} roleCode={knowledgeRole?.roleCode}
        roleName={knowledgeRole?.roleName} onClose={() => setKnowledgeRole(null)} />
    </div>
  );
};

export default RolePage;
