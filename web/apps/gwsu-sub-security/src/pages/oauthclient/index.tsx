import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {
  Button,
  Dropdown,
  Form,
  Input,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  type MenuProps,
} from 'antd';
import type {TableProps} from 'antd';
import {
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  KeyOutlined,
  MoreOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons';
import {AuthGate, useAuth} from '@gwsu/core';
import OAuthClientDetailDrawer from './components/OAuthClientDetailDrawer';
import OAuthClientFormModal from './components/OAuthClientFormModal';
import {toEnumOptions, toLabelMap} from './constants';
import {useOAuthClient} from './hooks/useOAuthClient';
import {PERM_ADD, PERM_EDIT, PERM_REMOVE, PERM_RESET_SECRET} from './permissionConstants';
import type {OAuthClientInfo, OAuthClientQuery} from './types';
import styles from './index.module.less';

const OAuthClientPage: React.FC = () => {
  const {
    loading,
    enumOptions,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchPage,
    ensureInitialized,
    handlePageChange,
    handleSaveOrUpdate,
    handleDelete,
    handleResetSecret,
  } = useOAuthClient();

  const canEdit = useAuth(PERM_EDIT);
  const canResetSecret = useAuth(PERM_RESET_SECRET);
  const [searchForm] = Form.useForm<OAuthClientQuery>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState<OAuthClientInfo | null>(null);
  const [formVisible, setFormVisible] = useState(false);
  const [formMode, setFormMode] = useState<'create' | 'edit'>('create');
  const [formData, setFormData] = useState<OAuthClientInfo | null>(null);
  const clientTypeOptions = useMemo(() => toEnumOptions(enumOptions.clientTypes), [enumOptions.clientTypes]);
  const accountTypeOptions = useMemo(() => toEnumOptions(enumOptions.accountTypes), [enumOptions.accountTypes]);
  const statusOptions = useMemo(() => toEnumOptions(enumOptions.statuses), [enumOptions.statuses]);
  const grantTypeOptions = useMemo(() => toEnumOptions(enumOptions.grantTypes), [enumOptions.grantTypes]);
  const authenticationMethodOptions = useMemo(
    () => toEnumOptions(enumOptions.authenticationMethods),
    [enumOptions.authenticationMethods],
  );
  const clientTypeLabels = useMemo(() => toLabelMap(enumOptions.clientTypes), [enumOptions.clientTypes]);
  const accountTypeLabels = useMemo(() => toLabelMap(enumOptions.accountTypes), [enumOptions.accountTypes]);
  const statusLabels = useMemo(() => toLabelMap(enumOptions.statuses), [enumOptions.statuses]);
  const grantTypeLabels = useMemo(() => toLabelMap(enumOptions.grantTypes), [enumOptions.grantTypes]);
  const authenticationMethodLabels = useMemo(
    () => toLabelMap(enumOptions.authenticationMethods),
    [enumOptions.authenticationMethods],
  );

  useEffect(() => {
    ensureInitialized();
  }, [ensureInitialized]);

  const handleSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    fetchPage({...values, pageNum: 1});
  }, [fetchPage, searchForm]);

  const handleReset = useCallback(() => {
    searchForm.resetFields();
    fetchPage({pageNum: 1});
  }, [fetchPage, searchForm]);

  const handleCreate = useCallback(() => {
    setFormMode('create');
    setFormData(null);
    setFormVisible(true);
  }, []);

  const handleEdit = useCallback((record: OAuthClientInfo) => {
    setFormMode('edit');
    setFormData(record);
    setFormVisible(true);
  }, []);

  const handleViewDetail = useCallback((record: OAuthClientInfo) => {
    setDetailData(record);
    setDetailVisible(true);
  }, []);

  const handleStatusChange = useCallback(async (record: OAuthClientInfo, checked: boolean) => {
    await handleSaveOrUpdate({...record, status: checked ? 'ENABLED' : 'DISABLED'});
  }, [handleSaveOrUpdate]);

  const handleBatchDelete = useCallback(async () => {
    const success = await handleDelete(selectedRowKeys as string[]);
    if (success) {
      setSelectedRowKeys([]);
    }
  }, [handleDelete, selectedRowKeys]);

  const getMoreItems = (record: OAuthClientInfo): NonNullable<MenuProps['items']> => {
    const items: NonNullable<MenuProps['items']> = [];
    if (canEdit) {
      items.push({
        key: 'edit',
        icon: <EditOutlined/>,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    if (canResetSecret && record.id && record.clientType === 'CONFIDENTIAL') {
      items.push({
        key: 'resetSecret',
        icon: <KeyOutlined/>,
        label: '重置密钥',
        onClick: () => handleResetSecret(record.id!),
        'data-ai-approval': true,
      });
    }
    return items;
  };

  const columns: TableProps<OAuthClientInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: OAuthClientInfo, index: number) => (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: '客户端ID',
      dataIndex: 'clientId',
      width: 190,
      ellipsis: true,
    },
    {
      title: '应用名称',
      dataIndex: 'clientName',
      width: 160,
      ellipsis: true,
    },
    {
      title: '客户端类型',
      dataIndex: 'clientType',
      width: 120,
      render: (value: OAuthClientInfo['clientType']) => clientTypeLabels.get(value) || value,
    },
    {
      title: '账号类型',
      dataIndex: 'accountType',
      width: 110,
      render: (value: OAuthClientInfo['accountType']) => accountTypeLabels.get(value || '') || value || '-',
    },
    {
      title: '授权模式',
      dataIndex: 'authorizationGrantTypes',
      width: 260,
      render: (values: OAuthClientInfo['authorizationGrantTypes']) => (
        <div className={styles.tagList}>
          {(values || []).map((value) => <Tag key={value}>{grantTypeLabels.get(value) || value}</Tag>)}
        </div>
      ),
    },
    {
      title: 'Scope',
      dataIndex: 'scopes',
      width: 180,
      render: (values: string[]) => (
        <div className={styles.tagList}>
          {(values || []).map((value) => <Tag key={value}>{value}</Tag>)}
        </div>
      ),
    },
    {
      title: 'PKCE',
      dataIndex: 'requireProofKey',
      width: 90,
      render: (value: boolean) => <Tag color={value ? 'green' : 'default'}>{value ? '启用' : '关闭'}</Tag>,
    },
    {
      title: '授权确认',
      dataIndex: 'requireAuthorizationConsent',
      width: 100,
      render: (value: boolean) => <Tag color={value ? 'blue' : 'default'}>{value ? '启用' : '关闭'}</Tag>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 130,
      fixed: 'right',
      render: (value: OAuthClientInfo['status'], record: OAuthClientInfo) => (
        <Space>
          <AuthGate buttonKey={PERM_EDIT}>
            <Switch
              size="small"
              data-ai-approval
              checked={value === 'ENABLED'}
              onChange={(checked) => handleStatusChange(record, checked)}
            />
          </AuthGate>
          <Tag color={value === 'ENABLED' ? 'green' : 'red'}>{statusLabels.get(value) || value}</Tag>
        </Space>
      ),
    },
    {
      title: '操作',
      width: 190,
      fixed: 'right',
      render: (_: unknown, record: OAuthClientInfo) => {
        const moreItems = getMoreItems(record);
        return (
          <div className={styles.actionColumn}>
            <Button type="link" size="small" icon={<EyeOutlined/>} onClick={() => handleViewDetail(record)}>
              详情
            </Button>
            <Dropdown menu={{items: moreItems}} disabled={moreItems.length === 0}>
              <Button type="link" size="small" icon={<MoreOutlined/>}>
                更多
              </Button>
            </Dropdown>
          </div>
        );
      },
    },
  ];

  return (
    <div className={styles.oauthClientPage}>
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>客户端ID</span>
            <Form.Item name="clientId" noStyle>
              <Input placeholder="请输入客户端ID" allowClear style={{width: 180}} onPressEnter={handleSearch}/>
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>应用名称</span>
            <Form.Item name="clientName" noStyle>
              <Input placeholder="请输入应用名称" allowClear style={{width: 180}} onPressEnter={handleSearch}/>
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>客户端类型</span>
            <Form.Item name="clientType" noStyle>
              <Select placeholder="请选择" allowClear style={{width: 140}} options={clientTypeOptions}/>
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>状态</span>
            <Form.Item name="status" noStyle>
              <Select placeholder="请选择" allowClear style={{width: 120}} options={statusOptions}/>
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button icon={<SearchOutlined/>} type="primary" onClick={handleSearch}>查询</Button>
          <Button icon={<ReloadOutlined/>} onClick={handleReset}>重置</Button>
        </div>
      </div>

      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>OAuth应用列表</span>
          <div className={styles.toolbar}>
            <AuthGate buttonKey={PERM_ADD}>
              <Button type="primary" icon={<PlusOutlined/>} onClick={handleCreate}>新增</Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_REMOVE}>
              <Popconfirm
                title="确认删除选中的OAuth应用？"
                okButtonProps={{'data-ai-approval': 'true'}}
                onConfirm={handleBatchDelete}
                disabled={selectedRowKeys.length === 0}
              >
                <Button danger icon={<DeleteOutlined/>} disabled={selectedRowKeys.length === 0}>删除</Button>
              </Popconfirm>
            </AuthGate>
          </div>
        </div>
        <Table
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          scroll={{x: 1610}}
          rowSelection={{selectedRowKeys, onChange: setSelectedRowKeys}}
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showTotal: (count) => `共 ${count} 条`,
            onChange: handlePageChange,
          }}
        />
      </div>

      <OAuthClientDetailDrawer
        visible={detailVisible}
        data={detailData}
        clientTypeLabels={clientTypeLabels}
        accountTypeLabels={accountTypeLabels}
        statusLabels={statusLabels}
        grantTypeLabels={grantTypeLabels}
        authenticationMethodLabels={authenticationMethodLabels}
        onClose={() => setDetailVisible(false)}
      />
      <OAuthClientFormModal
        visible={formVisible}
        mode={formMode}
        data={formData}
        clientTypeOptions={clientTypeOptions}
        accountTypeOptions={accountTypeOptions}
        statusOptions={statusOptions}
        grantTypeOptions={grantTypeOptions}
        authenticationMethodOptions={authenticationMethodOptions}
        onSave={handleSaveOrUpdate}
        onClose={() => setFormVisible(false)}
        onSuccess={() => setFormVisible(false)}
      />
    </div>
  );
};

export default OAuthClientPage;
