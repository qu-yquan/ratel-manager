import React, { useState, useCallback, useEffect } from 'react';
import {
  Button,
  Table,
  Input,
  Select,
  Tag,
  Switch,
  Dropdown,
  Form,
  Space,
  Popconfirm,
  type MenuProps,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  DeleteOutlined,
  SearchOutlined,
  ReloadOutlined,
  SyncOutlined,
  EyeOutlined,
  EditOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import DataResourceDetail from './components/DataResourceDetail';
import DataResourceFormModal from './components/DataResourceFormModal';
import { useDataResource } from './hooks/useDataResource';
import type {
  DataResourceInfo,
  DataResourceQuery,
  StringEnumOption,
  ResourceAttribute,
} from './types';
import {
  getAssertTypeOptions,
  getConditionTypeOptions,
  getResourceAttributes,
} from './services/dataResource';
import {AuthGate, useAuth} from '@gwsu/core'
import { PERM_ADD, PERM_REMOVE, PERM_SYNC, PERM_EDIT } from './permissionConstants';

const STATUS_OPTIONS = [
  { label: '启用', value: true },
  { label: '禁用', value: false },
];

const DataResourcePage: React.FC = () => {
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchDataResourcePage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleSync,
  } = useDataResource();

  const canEdit = useAuth(PERM_EDIT);

  const [searchForm] = Form.useForm<DataResourceQuery>();

  // 枚举和资源属性
  const [assertTypeOptions, setAssertTypeOptions] = useState<
    StringEnumOption[]
  >([]);
  const [conditionTypeOptions, setConditionTypeOptions] = useState<
    StringEnumOption[]
  >([]);
  const [resourceAttributes, setResourceAttributes] = useState<
    ResourceAttribute[]
  >([]);

  useEffect(() => {
    getAssertTypeOptions().then(setAssertTypeOptions);
    getConditionTypeOptions().then(setConditionTypeOptions);
    getResourceAttributes().then(setResourceAttributes);
  }, []);

  // 详情抽屉
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState<DataResourceInfo | null>(null);

  // 新增/编辑弹窗
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalMode, setFormModalMode] = useState<'create' | 'edit'>(
    'create',
  );
  const [formModalData, setFormModalData] = useState<DataResourceInfo | null>(
    null,
  );

  // 表格选中行
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  /** 初始化加载 */
  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  /** 搜索 */
  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchDataResourcePage({ ...values, pageNum: 1 });
  }, [searchForm, fetchDataResourcePage]);

  /** 重置搜索 */
  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchDataResourcePage({ pageNum: 1 });
  }, [searchForm, fetchDataResourcePage]);

  /** 查看详情 */
  const handleViewDetail = useCallback((record: DataResourceInfo) => {
    setDetailData(record);
    setDetailVisible(true);
  }, []);

  /** 新增 */
  const handleCreate = useCallback(() => {
    setFormModalMode('create');
    setFormModalData(null);
    setFormModalVisible(true);
  }, []);

  /** 编辑 */
  const handleEdit = useCallback((record: DataResourceInfo) => {
    setFormModalMode('edit');
    setFormModalData(record);
    setFormModalVisible(true);
  }, []);

  /** 保存（由弹窗调用） */
  const handleSave = useCallback(
    async (data: DataResourceInfo): Promise<boolean> => {
      return handleSaveOrUpdate(data);
    },
    [handleSaveOrUpdate],
  );

  /** 状态切换 */
  const handleStatusChange = useCallback(
    async (record: DataResourceInfo, checked: boolean) => {
      await handleSaveOrUpdate({ ...record, status: checked });
    },
    [handleSaveOrUpdate],
  );


  /** 批量删除 */
  const handleBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    const success = await handleDelete(ids);
    if (success) {
      setSelectedRowKeys([]);
    }
  }, [selectedRowKeys, handleDelete]);

  /** 获取更多下拉菜单项 */
  const getButtonItem = (record: DataResourceInfo): NonNullable<MenuProps['items']> => {
    const buttons = [];
    if (canEdit) {
      buttons.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    return buttons;
  };

  /** 表格列定义 */
  const columns: TableProps<DataResourceInfo>["columns"] = [
    Table.SELECTION_COLUMN,
    {
      title: "序号",
      width: 60,
      align: "center",
      render: (_: unknown, __: DataResourceInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: "Catalog",
      dataIndex: "catalogName",
      width: 140,
      render: (val: string) => val || <Tag>全部</Tag>,
    },
    {
      title: "库名/Schema",
      dataIndex: "schemaName",
      width: 160,
      render: (val: string) => val || <Tag>全部</Tag>,
    },
    {
      title: "表名",
      dataIndex: "tableName",
      width: 180,
      render: (val: string) => <code>{val}</code>,
    },
    {
      title: "描述",
      dataIndex: "description",
      width: 200,
      ellipsis: true,
    },
    {
      title: "条件数",
      width: 80,
      align: "center",
      render: (_: unknown, record: DataResourceInfo) =>
        record.conditions?.length ?? 0,
    },
    {
      title: "状态",
      dataIndex: "status",
      width: 120,
      fixed: "right",
      render: (val: boolean, record: DataResourceInfo) => (
        <Space>
          <AuthGate buttonKey={PERM_EDIT}>
            <Switch
              size="small"
              data-ai-approval
              checked={val}
              onChange={(checked) => handleStatusChange(record, checked)}
            />
          </AuthGate>

          <Tag color={val ? "green" : "red"}>{val ? "启用" : "禁用"}</Tag>
        </Space>
      ),
    },
    {
      title: "操作",
      width: 200,
      fixed: "right",
      render: (_: unknown, record: DataResourceInfo) => (
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
            menu={{ items: getButtonItem(record) }}
            disabled={getButtonItem(record).length === 0}
          >
            <Button
              type="link"
              size="small"
              icon={<MoreOutlined />}
              disabled={getButtonItem(record).length === 0}
            >
              更多
            </Button>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.dataResourcePage}>
      {/* 搜索栏 */}
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>表名</span>
            <Form.Item name="tableName" noStyle>
              <Input
                placeholder="请输入表名"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>Catalog</span>
            <Form.Item name="catalogName" noStyle>
              <Input
                placeholder="请输入 Catalog"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>库名/Schema</span>
            <Form.Item name="schemaName" noStyle>
              <Input
                placeholder="请输入库名或 Schema"
                allowClear
                style={{ width: 180 }}
                onPressEnter={handleSearch}
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
          <span className={styles.tableTitle}>数据资源列表</span>
          <Space>
            <AuthGate buttonKey={PERM_ADD}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
                onConfirm={handleBatchDelete}
                okText="确定"
                cancelText="取消"
                disabled={selectedRowKeys.length === 0}
              >
                <Button
                  danger
                  data-ai-approval
                  icon={<DeleteOutlined />}
                  disabled={selectedRowKeys.length === 0}
                >
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
            <AuthGate buttonKey={PERM_SYNC}>
              <Popconfirm
                title="同步到 Redis"
                description="确定将数据资源规则同步到 Redis？"
                onConfirm={handleSync}
                okText="确定"
                cancelText="取消"
              >
                <Button icon={<SyncOutlined />}>同步</Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<DataResourceInfo>
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

      {/* 详情抽屉 */}
      <DataResourceDetail
        visible={detailVisible}
        data={detailData}
        assertTypeOptions={assertTypeOptions}
        conditionTypeOptions={conditionTypeOptions}
        resourceAttributes={resourceAttributes}
        onClose={() => setDetailVisible(false)}
      />

      {/* 新增/编辑弹窗 */}
      <DataResourceFormModal
        visible={formModalVisible}
        mode={formModalMode}
        data={formModalData}
        assertTypeOptions={assertTypeOptions}
        conditionTypeOptions={conditionTypeOptions}
        resourceAttributes={resourceAttributes}
        onSave={handleSave}
        onClose={() => setFormModalVisible(false)}
        onSuccess={() => setFormModalVisible(false)}
      />
    </div>
  );
};

export default DataResourcePage;
