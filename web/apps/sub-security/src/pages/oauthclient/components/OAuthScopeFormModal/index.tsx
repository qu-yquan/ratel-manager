import React, {useCallback, useEffect, useMemo, useState} from 'react';
import {Button, Form, Input, Modal, Radio, Select, Table, Tag, Tooltip} from 'antd';
import type {TableProps} from 'antd';
import {DeleteOutlined, SettingOutlined} from '@ant-design/icons';
import type {ApiResourceInfo, EnumOption, OAuthClientStatus, OAuthScopeInfo} from '../../types';
import {getApiResourcePage, getApiResourcesByIds} from '../../services/oauthClient';
import styles from './index.module.less';

interface Props {
  visible: boolean;
  data: OAuthScopeInfo | null;
  accountTypeOptions: EnumOption[];
  statusOptions: EnumOption<OAuthClientStatus>[];
  onClose: () => void;
  onSave: (data: OAuthScopeInfo) => Promise<boolean>;
}

const OAuthScopeFormModal: React.FC<Props> = ({visible, data, accountTypeOptions, statusOptions, onClose, onSave}) => {
  const [form] = Form.useForm();
  const [resources, setResources] = useState<ApiResourceInfo[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<React.Key[]>([]);
  const [selectedResourceMap, setSelectedResourceMap] = useState<Record<string, ApiResourceInfo>>({});
  const [resourceLoading, setResourceLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(8);
  const [total, setTotal] = useState(0);
  const accountType = Form.useWatch('accountType', form) || 'MANAGER';
  const prefix = `${String(accountType).toLowerCase()}.`;

  const loadResources = useCallback(async (nextPage: number, nextSize: number, nextKeyword: string) => {
    setResourceLoading(true);
    try {
      const result = await getApiResourcePage({pageNum: nextPage, pageSize: nextSize, keyword: nextKeyword || undefined});
      setResources(result.records || []);
      setTotal(result.total || 0);
      setPage(result.current || nextPage);
      setPageSize(result.size || nextSize);
    } finally {
      setResourceLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!visible) return;
    const fullCode = data?.scopeCode || '';
    const editPrefix = data ? `${data.accountType.toLowerCase()}.` : prefix;
    form.setFieldsValue({
      accountType: data?.accountType || 'MANAGER',
      scopeSuffix: fullCode.startsWith(editPrefix) ? fullCode.slice(editPrefix.length) : fullCode,
      scopeName: data?.scopeName,
      description: data?.description,
      status: data?.status || 'ENABLED',
    });
    setSelectedKeys(data?.resourceIds || []);
    setSelectedResourceMap({});
    setKeyword('');
    setPage(1);
    setPageSize(8);
    void loadResources(1, 8, '');
    if (data?.resourceIds?.length) {
      void getApiResourcesByIds(data.resourceIds).then(items => {
        setSelectedResourceMap(Object.fromEntries(items.map(item => [item.id, item])));
      });
    }
  }, [data, form, loadResources, visible]);

  const columns = useMemo<TableProps<ApiResourceInfo>['columns']>(() => [
    {title: '服务', dataIndex: 'modulePrefix', width: 110},
    {title: '接口', dataIndex: 'reqPath', ellipsis: true},
    {title: '方法', dataIndex: 'reqMethod', width: 80},
    {title: '说明', dataIndex: 'summary', ellipsis: true},
  ], []);

  const selectedResources = useMemo(() => selectedKeys.map(key => {
    const id = String(key);
    return selectedResourceMap[id] || {
      id,
      modulePrefix: '-',
      tagName: '-',
      reqPath: '接口已失效',
      reqMethod: '-',
      summary: '请解除该绑定并重新选择接口',
    };
  }), [selectedKeys, selectedResourceMap]);

  const handleResourceSelectionChange = (keys: React.Key[], selectedRows: ApiResourceInfo[]) => {
    const selectedIdSet = new Set(keys.map(String));
    setSelectedKeys(keys);
    setSelectedResourceMap(previous => {
      const next = Object.fromEntries(Object.entries(previous).filter(([id]) => selectedIdSet.has(id)));
      selectedRows.forEach(resource => {
        if (selectedIdSet.has(resource.id)) next[resource.id] = resource;
      });
      return next;
    });
  };

  const unbindResource = (id: string) => {
    setSelectedKeys(keys => keys.filter(key => String(key) !== id));
    setSelectedResourceMap(previous => {
      const next = {...previous};
      delete next[id];
      return next;
    });
  };

  const selectedColumns = useMemo<TableProps<ApiResourceInfo>['columns']>(() => [
    {title: '服务', dataIndex: 'modulePrefix', width: 90, ellipsis: true},
    {title: '接口', dataIndex: 'reqPath', ellipsis: true},
    {title: '方法', dataIndex: 'reqMethod', width: 70},
    {title: '字段范围', width: 90, render: () => <Tag>全部字段</Tag>},
    {title: '操作', width: 92, render: (_, record) => (
      <div className={styles.resourceActions}>
        <Tooltip title="字段配置入口已预留">
          <span><Button type="text" size="small" disabled icon={<SettingOutlined/>}/></span>
        </Tooltip>
        <Tooltip title="解除绑定">
          <Button type="text" danger size="small" icon={<DeleteOutlined/>} onClick={() => unbindResource(record.id)}/>
        </Tooltip>
      </div>
    )},
  ], []);

  const handleOk = async () => {
    const values = await form.validateFields();
    setSaving(true);
    try {
      const success = await onSave({
        ...data,
        ...values,
        scopeCode: `${values.accountType.toLowerCase()}.${values.scopeSuffix.trim().toLowerCase()}`,
        resourceIds: selectedKeys as string[],
      });
      if (success) onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal title={data ? '编辑 Scope' : '新增 Scope'} open={visible} width={1200}
           confirmLoading={saving} onCancel={onClose} onOk={() => void handleOk()} destroyOnHidden>
      <Form form={form} layout="vertical">
        <Form.Item name="accountType" label="账号体系" rules={[{required: true}]}>
          <Radio.Group className={styles.accountSystemGroup} disabled={Boolean(data?.id)}>
            {accountTypeOptions.map(option => (
              <Radio.Button key={option.value} value={option.value}>{option.label}</Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>
        <Form.Item name="scopeSuffix" label="Scope 编码" rules={[
          {required: true, message: '请输入Scope编码'},
          {pattern: /^[a-z][a-z0-9]*(?:[._:-][a-z0-9]+)*$/, message: '仅支持小写字母、数字及 . _ : - 分段'},
        ]}>
          <Input addonBefore={prefix} disabled={Boolean(data?.id)} placeholder="例如 user.read"/>
        </Form.Item>
        <Form.Item name="scopeName" label="中文名称" rules={[{required: true, message: '请输入中文名称'}]}>
          <Input maxLength={128}/>
        </Form.Item>
        <Form.Item name="description" label="授权说明" rules={[{required: true, message: '请输入授权界面展示的说明'}]}>
          <Input.TextArea rows={2} maxLength={500} showCount/>
        </Form.Item>
        <Form.Item name="status" label="状态" rules={[{required: true}]}>
          <Select options={statusOptions}/>
        </Form.Item>
      </Form>
      <div className={styles.resourceHeader}>
        <span className={styles.resourceTitle}>绑定接口</span>
        <span className={styles.selectedCount}>已选择 {selectedKeys.length} 个</span>
      </div>
      <div className={styles.bindingGrid}>
        <section className={styles.bindingPane}>
          <div className={styles.paneHeader}>可选接口</div>
          <Input.Search placeholder="搜索接口路径、标签或说明" allowClear value={keyword}
                        onChange={event => setKeyword(event.target.value)}
                        onSearch={value => void loadResources(1, pageSize, value)}/>
          <Table<ApiResourceInfo> rowKey="id" size="small" loading={resourceLoading} columns={columns}
                                  dataSource={resources} scroll={{x: 620, y: 300}}
                                  rowSelection={{selectedRowKeys: selectedKeys, preserveSelectedRowKeys: true,
                                    onChange: handleResourceSelectionChange}}
                                  pagination={{current: page, pageSize, total, showSizeChanger: true, size: 'small',
                                    onChange: (nextPage, nextSize) => void loadResources(nextPage, nextSize, keyword)}}/>
        </section>
        <section className={styles.bindingPane}>
          <div className={styles.paneHeader}>已绑定接口 ({selectedKeys.length})</div>
          <Table<ApiResourceInfo> rowKey="id" size="small" columns={selectedColumns}
                                  dataSource={selectedResources} scroll={{x: 560, y: 300}}
                                  pagination={{pageSize: 8, size: 'small', hideOnSinglePage: true}}/>
        </section>
      </div>
    </Modal>
  );
};

export default OAuthScopeFormModal;
