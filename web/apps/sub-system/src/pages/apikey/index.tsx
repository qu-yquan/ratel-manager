import React, { useEffect, useMemo, useState } from 'react';
import { App, Button, Dropdown, Form, Input, Space, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { DeleteOutlined, EyeOutlined, MoreOutlined, PlusOutlined, SearchOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import useApiKey from './hooks/useApiKey';
import ApiKeyCreateModal from './components/ApiKeyCreateModal';
import ApiKeyDetailModal from './components/ApiKeyDetailModal';
import type { ApiKeyInfo } from './types';

function formatDateTime(value?: string) {
  if (!value) {
    return '永不过期';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return '-';
  }
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

const ApiKeyPage: React.FC = () => {
  const { message, modal } = App.useApp();
  const [form] = Form.useForm<{ apiKeyName?: string }>();
  const [createVisible, setCreateVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentId, setCurrentId] = useState<string | null>(null);
  const {
    loading,
    dataSource,
    total,
    currentPage,
    pageSize,
    fetchApiKeyPage,
    ensureInitialized,
    handlePageChange,
    handleCreate,
    handleDelete,
  } = useApiKey();

  useEffect(() => {
    void ensureInitialized();
  }, [ensureInitialized]);

  const columns = useMemo<ColumnsType<ApiKeyInfo>>(() => [
    {
      title: '名称',
      dataIndex: 'apiKeyName',
      key: 'apiKeyName',
      width: 180,
    },
    {
      title: '脱敏 Key',
      dataIndex: 'maskedKey',
      key: 'maskedKey',
      ellipsis: true,
      render: (value: string) => <span className={styles.maskedKey}>{value}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (value: number) => (
        <Tag color={value === 1 ? 'green' : 'default'}>
          {value === 1 ? '启用' : '停用'}
        </Tag>
      ),
    },
    {
      title: '过期时间',
      dataIndex: 'expireTime',
      key: 'expireTime',
      width: 180,
      render: (value?: string) => formatDateTime(value),
    },
    {
      title: '最近使用时间',
      dataIndex: 'lastUsedTime',
      key: 'lastUsedTime',
      width: 180,
      render: (value?: string) => formatDateTime(value),
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
      render: (value?: string) => (value ? formatDateTime(value) : '-'),
    },
    {
      title: '备注',
      dataIndex: 'remark',
      key: 'remark',
      ellipsis: true,
      width: 220,
      render: (value?: string) => value || '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 160,
      render: (_, record) => {
        const moreItems = [
          {
            key: 'delete',
            icon: <DeleteOutlined />,
            label: '删除',
            onClick: () => {
              modal.confirm({
                title: '确认删除该 API_KEY 吗？',
                content: '删除后不可恢复，且后续将无法再使用该凭证。',
                okButtonProps: { danger: true },
                onOk: async () => {
                  try {
                    await handleDelete(record.id);
                    message.success('删除成功');
                  } catch {
                    message.error('删除失败');
                  }
                },
              });
            },
          },
        ];
        return (
          <div className={styles.actionColumn}>
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => {
                setCurrentId(record.id);
                setDetailVisible(true);
              }}
            >
              详情
            </Button>
            <Dropdown menu={{ items: moreItems }} trigger={['click']}>
              <Button type="link" size="small" icon={<MoreOutlined />}>
                更多
              </Button>
            </Dropdown>
          </div>
        );
      },
    },
  ], [handleDelete, message, modal]);

  const handleSearch = async () => {
    try {
      const values = await form.validateFields();
      await fetchApiKeyPage({
        apiKeyName: values.apiKeyName?.trim() ?? '',
        pageNum: 1,
      });
    } catch {
      // ignore
    }
  };

  const handleReset = async () => {
    form.resetFields();
    await fetchApiKeyPage({ apiKeyName: '', pageNum: 1 });
  };

  return (
    <div className={styles.apiKeyPage}>
      <div className={styles.searchBar}>
        <Form form={form} layout="vertical">
          <Form.Item name="apiKeyName" label="名称" style={{ marginBottom: 0 }}>
            <Input placeholder="按名称搜索" allowClear />
          </Form.Item>
        </Form>
        <Space>
          <Button icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </div>

      <div className={styles.tableCard}>
        <div className={styles.tableHeader}>
          <div className={styles.headerTitle}>API_KEY 列表</div>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => setCreateVisible(true)}>
            创建 API_KEY
          </Button>
        </div>
        <Table<ApiKeyInfo>
          rowKey="id"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          pagination={{
            total,
            current: currentPage,
            pageSize,
            showSizeChanger: true,
            onChange: (page, size) => {
              void handlePageChange(page, size);
            },
          }}
          scroll={{ x: 1280 }}
        />
      </div>

      <ApiKeyCreateModal
        visible={createVisible}
        onClose={() => setCreateVisible(false)}
        onSuccess={handleCreate}
      />
      <ApiKeyDetailModal
        visible={detailVisible}
        apiKeyId={currentId}
        onClose={() => {
          setDetailVisible(false);
          setCurrentId(null);
        }}
      />
    </div>
  );
};

export default ApiKeyPage;
