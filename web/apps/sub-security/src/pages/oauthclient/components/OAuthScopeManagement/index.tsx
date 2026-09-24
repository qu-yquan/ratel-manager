import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {App, Button, Form, Input, Popconfirm, Select, Space, Table, Tag} from 'antd';
import type {TableProps} from 'antd';
import {DeleteOutlined, EditOutlined, PlusOutlined, ReloadOutlined, SearchOutlined} from '@ant-design/icons';
import {AuthGate} from '@gwsu/core';
import type {EnumOption, OAuthClientStatus, OAuthScopeInfo, OAuthScopeQuery} from '../../types';
import {deleteOAuthScopes, getOAuthScope, getOAuthScopePage, saveOAuthScope} from '../../services/oauthClient';
import {PERM_SCOPE_ADD, PERM_SCOPE_EDIT, PERM_SCOPE_REMOVE} from '../../permissionConstants';
import OAuthScopeFormModal from '../OAuthScopeFormModal';
import styles from './index.module.less';

interface Props {
  accountTypeOptions: EnumOption[];
  statusOptions: EnumOption<OAuthClientStatus>[];
  accountTypeLabels: Map<string, string>;
  statusLabels: Map<string, string>;
}

const OAuthScopeManagement: React.FC<Props> = ({accountTypeOptions, statusOptions, accountTypeLabels, statusLabels}) => {
  const {message} = App.useApp();
  const [form] = Form.useForm<OAuthScopeQuery>();
  const [loading, setLoading] = useState(false);
  const [rows, setRows] = useState<OAuthScopeInfo[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [current, setCurrent] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [total, setTotal] = useState(0);
  const [modalVisible, setModalVisible] = useState(false);
  const [editing, setEditing] = useState<OAuthScopeInfo | null>(null);
  const queryRef = useRef<OAuthScopeQuery>({});

  const loadPage = useCallback(async (query?: OAuthScopeQuery) => {
    if (query) queryRef.current = query;
    setLoading(true);
    try {
      const result = await getOAuthScopePage({
        ...queryRef.current,
        pageNum: query?.pageNum ?? current,
        pageSize: query?.pageSize ?? pageSize,
      });
      setRows(result.records || []);
      setCurrent(result.current || 1);
      setPageSize(result.size || 10);
      setTotal(result.total || 0);
    } finally {
      setLoading(false);
    }
  }, [current, pageSize]);

  useEffect(() => { void loadPage({pageNum: 1}); }, []);

  const handleEdit = async (record: OAuthScopeInfo) => {
    if (!record.id) return;
    setLoading(true);
    try {
      setEditing(await getOAuthScope(record.id));
      setModalVisible(true);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (data: OAuthScopeInfo) => {
    try {
      await saveOAuthScope(data);
      message.success(data.id ? 'Scope编辑成功' : 'Scope新增成功');
      await loadPage();
      return true;
    } catch {
      return false;
    }
  };

  const handleDelete = async () => {
    await deleteOAuthScopes(selectedKeys as string[]);
    message.success('Scope删除成功');
    setSelectedKeys([]);
    await loadPage();
  };

  const columns = useMemo<TableProps<OAuthScopeInfo>['columns']>(() => [
    Table.SELECTION_COLUMN,
    {title: 'Scope编码', dataIndex: 'scopeCode', width: 220},
    {title: '中文名称', dataIndex: 'scopeName', width: 180},
    {title: '授权说明', dataIndex: 'description', ellipsis: true},
    {title: '账号体系', dataIndex: 'accountType', width: 120,
      render: value => accountTypeLabels.get(value) || value},
    {title: '状态', dataIndex: 'status', width: 100,
      render: value => <Tag color={value === 'ENABLED' ? 'green' : 'red'}>{statusLabels.get(value) || value}</Tag>},
    {title: '操作', width: 100, fixed: 'right', render: (_, record) => (
      <AuthGate buttonKey={PERM_SCOPE_EDIT}>
        <Button type="link" size="small" icon={<EditOutlined/>} onClick={() => void handleEdit(record)}>编辑</Button>
      </AuthGate>
    )},
  ], [accountTypeLabels, statusLabels]);

  return (
    <div className={styles.panel}>
      <div className={styles.searchBar}>
        <Form form={form} layout="inline">
          <Form.Item name="keyword"><Input allowClear placeholder="编码、名称或说明"/></Form.Item>
          <Form.Item name="accountType"><Select allowClear placeholder="账号体系" options={accountTypeOptions}/></Form.Item>
          <Form.Item name="status"><Select allowClear placeholder="状态" options={statusOptions}/></Form.Item>
        </Form>
        <div className={styles.searchActions}>
          <Button type="primary" icon={<SearchOutlined/>} onClick={() => void loadPage({...form.getFieldsValue(), pageNum: 1})}>查询</Button>
          <Button icon={<ReloadOutlined/>} onClick={() => { form.resetFields(); void loadPage({pageNum: 1}); }}>重置</Button>
        </div>
      </div>
      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>Scope权限组</span>
          <Space className={styles.toolbar}>
            <AuthGate buttonKey={PERM_SCOPE_ADD}>
              <Button type="primary" icon={<PlusOutlined/>} onClick={() => { setEditing(null); setModalVisible(true); }}>新增</Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_SCOPE_REMOVE}>
              <Popconfirm title="确认删除选中的Scope？" onConfirm={() => void handleDelete()} disabled={!selectedKeys.length}>
                <Button danger icon={<DeleteOutlined/>} disabled={!selectedKeys.length}>删除</Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table rowKey="id" loading={loading} columns={columns} dataSource={rows} scroll={{x: 1000}}
               rowSelection={{selectedRowKeys: selectedKeys, onChange: setSelectedKeys}}
               pagination={{current, pageSize, total, showSizeChanger: true, showTotal: count => `共 ${count} 条`,
                 onChange: (page, size) => void loadPage({...queryRef.current, pageNum: page, pageSize: size})}}/>
      </div>
      <OAuthScopeFormModal visible={modalVisible} data={editing} accountTypeOptions={accountTypeOptions}
                           statusOptions={statusOptions} onClose={() => setModalVisible(false)} onSave={handleSave}/>
    </div>
  );
};

export default OAuthScopeManagement;
