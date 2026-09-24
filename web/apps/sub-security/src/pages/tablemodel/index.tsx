import React, { useEffect, useState, useCallback } from 'react';
import {
  Button,
  Table,
  Tag,
  Form,
  Select,
  Input,
  Space,
  Popconfirm,
  Dropdown,
  Tabs,
  type MenuProps,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  CloudDownloadOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  MoreOutlined,
  SyncOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import { useTableModel } from './hooks/useTableModel';
import CollectModal from './components/CollectModal';
import CustomAddModal from './components/CustomAddModal';
import DetailDrawer from './components/DetailDrawer';
import EditDrawer from './components/EditDrawer';
import ChangeDatasourceModal from './components/ChangeDatasourceModal';
import BusinessFunctionTab from './components/BusinessFunctionTab';
import { SOURCE_TYPE_MAP } from './types';
import type { TableModelInfo } from './types';
import {
  PERM_COLLECTED,
  PERM_CUSTOM_ADD,
  PERM_REMOVE,
  PERM_EDIT,
  PERM_SYNC,
  PERM_CHANGE_DATASOURCE,
} from './permissionConstants';
import { AuthGate, useAuth } from '@gwsu/core';
import styles from './index.module.less';

const TableModelTab: React.FC = () => {
  const {
    loading,
    pageData,
    modules,
    loadModules,
    loadPageData,
    handleSearch,
    handlePageChange,
    handleSync,
    handleBatchDelete,
  } = useTableModel();

  const canEdit = useAuth(PERM_EDIT);
  const canSync = useAuth(PERM_SYNC);
  const canChangeDatasource = useAuth(PERM_CHANGE_DATASOURCE);

  const [searchForm] = Form.useForm();

  const [collectVisible, setCollectVisible] = useState(false);
  const [customAddVisible, setCustomAddVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [editVisible, setEditVisible] = useState(false);
  const [changeDatasourceVisible, setChangeDatasourceVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<TableModelInfo | null>(null);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const onBatchDelete = useCallback(async () => {
    const ids = selectedRowKeys as string[];
    const success = await handleBatchDelete(ids);
    if (success) {
      setSelectedRowKeys([]);
    }
  }, [selectedRowKeys, handleBatchDelete]);

  useEffect(() => {
    loadModules();
    loadPageData();
  }, []);

  const onSearch = useCallback(() => {
    const values = searchForm.getFieldsValue();
    handleSearch(values);
  }, [searchForm, handleSearch]);

  const onReset = useCallback(() => {
    searchForm.resetFields();
    handleSearch({});
  }, [searchForm, handleSearch]);

  const handleViewDetail = useCallback((record: TableModelInfo) => {
    setCurrentRecord(record);
    setDetailVisible(true);
  }, []);

  const handleEdit = useCallback((record: TableModelInfo) => {
    setCurrentRecord(record);
    setEditVisible(true);
  }, []);

  const handleChangeDatasource = useCallback((record: TableModelInfo) => {
    setCurrentRecord(record);
    setChangeDatasourceVisible(true);
  }, []);

  const refreshList = useCallback(() => {
    loadPageData();
  }, [loadPageData]);

  const getMoreMenuItems = (record: TableModelInfo): NonNullable<MenuProps['items']> => {
    const items: NonNullable<MenuProps['items']> = [];
    if (canEdit) {
      items.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    if (canSync) {
      items.push({
        key: 'sync',
        icon: <SyncOutlined />,
        label: '同步',
        onClick: () => handleSync(record),
        'data-ai-approval': true,
      });
    }
    if (canChangeDatasource) {
      items.push({
        key: 'changeDatasource',
        label: '修改数据源',
        onClick: () => handleChangeDatasource(record),
      });
    }
    return items;
  };

  const columns: TableProps<TableModelInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: TableModelInfo, index: number) =>
        (pageData.current - 1) * pageData.size + index + 1,
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      key: 'tableName',
      width: 200,
    },
    {
      title: '所属模块',
      dataIndex: 'modulePrefix',
      key: 'modulePrefix',
      width: 120,
    },
    {
      title: '数据源',
      dataIndex: 'dataSource',
      key: 'dataSource',
      width: 120,
    },
    {
      title: '表注释',
      dataIndex: 'tableComment',
      key: 'tableComment',
      width: 200,
      ellipsis: true,
    },
    {
      title: '来源类型',
      dataIndex: 'sourceType',
      key: 'sourceType',
      width: 100,
      render: (val: number) => {
        const info = SOURCE_TYPE_MAP[val];
        return info ? <Tag color={info.color}>{info.text}</Tag> : '-';
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      key: 'createTime',
      width: 180,
    },
    {
      title: '操作',
      width: 180,
      fixed: 'right',
      render: (_: unknown, record: TableModelInfo) => {
        const moreItems = getMoreMenuItems(record);
        return (
          <div className={styles.actionColumn}>
            <Button
              type="link"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => handleViewDetail(record)}
            >
              详情
            </Button>
            {moreItems.length > 0 && (
              <Dropdown menu={{ items: moreItems }}>
                <Button type="link" size="small" icon={<MoreOutlined />}>
                  更多
                </Button>
              </Dropdown>
            )}
          </div>
        );
      },
    },
  ];

  return (
    <div className={styles.tableModelTab}>
      <div className={styles.searchBar}>
        <Form form={searchForm} layout="inline" component={false}>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>所属模块</span>
            <Form.Item name="modulePrefix" noStyle>
              <Select
                placeholder="请选择模块"
                allowClear
                style={{ width: 160 }}
                options={modules.map((m) => ({ label: m.note || m.prefix, value: m.prefix }))}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>表名</span>
            <Form.Item name="tableName" noStyle>
              <Input
                placeholder="请输入表名"
                allowClear
                style={{ width: 160 }}
                onPressEnter={onSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>数据源</span>
            <Form.Item name="dataSource" noStyle>
              <Input
                placeholder="请输入数据源"
                allowClear
                style={{ width: 160 }}
                onPressEnter={onSearch}
              />
            </Form.Item>
          </div>
          <div className={styles.searchItem}>
            <span className={styles.searchLabel}>来源类型</span>
            <Form.Item name="sourceType" noStyle>
              <Select
                placeholder="全部"
                allowClear
                style={{ width: 120 }}
                options={[
                  { label: '采集', value: 0 },
                  { label: '自定义', value: 1 },
                ]}
              />
            </Form.Item>
          </div>
        </Form>
        <div className={styles.searchActions}>
          <Button type="primary" icon={<SearchOutlined />} onClick={onSearch}>
            查询
          </Button>
          <Button icon={<ReloadOutlined />} onClick={onReset}>
            重置
          </Button>
        </div>
      </div>

      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <span className={styles.tableTitle}>表模型列表</span>
          <Space>
            <AuthGate buttonKey={PERM_COLLECTED}>
              <Button
                type="primary"
                icon={<CloudDownloadOutlined />}
                onClick={() => setCollectVisible(true)}
              >
                采集
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_CUSTOM_ADD}>
              <Button
                icon={<PlusOutlined />}
                onClick={() => setCustomAddVisible(true)}
              >
                自定义添加
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 条记录？删除后将同时清除关联的字段和外键数据。`}
                onConfirm={onBatchDelete}
                disabled={selectedRowKeys.length === 0}
              >
                <Button
                  danger
                  icon={<DeleteOutlined />}
                  data-ai-approval
                  disabled={selectedRowKeys.length === 0}
                >
                  删除
                </Button>
              </Popconfirm>
            </AuthGate>
          </Space>
        </div>
        <Table<TableModelInfo>
          rowKey="id"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          loading={loading}
          dataSource={pageData.records}
          columns={columns}
          size="middle"
          pagination={{
            current: pageData.current,
            pageSize: pageData.size,
            total: pageData.total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
          scroll={{ x: 960 }}
        />
      </div>

      <CollectModal
        visible={collectVisible}
        modules={modules}
        onClose={() => setCollectVisible(false)}
        onSuccess={refreshList}
      />

      <CustomAddModal
        visible={customAddVisible}
        modules={modules}
        onClose={() => setCustomAddVisible(false)}
        onSuccess={refreshList}
      />

      <DetailDrawer
        visible={detailVisible}
        record={currentRecord}
        onClose={() => {
          setDetailVisible(false);
          setCurrentRecord(null);
        }}
      />

      <EditDrawer
        visible={editVisible}
        record={currentRecord}
        onClose={() => {
          setEditVisible(false);
          setCurrentRecord(null);
        }}
        onSuccess={refreshList}
      />

      <ChangeDatasourceModal
        visible={changeDatasourceVisible}
        record={currentRecord}
        modules={modules}
        onClose={() => {
          setChangeDatasourceVisible(false);
          setCurrentRecord(null);
        }}
        onSuccess={refreshList}
      />
    </div>
  );
};

const TableModelPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState('tableModel');

  return (
    <div className={styles.dataResourcePage}>
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'tableModel',
            label: '表模型管理',
            children: <TableModelTab />,
          },
          {
            key: 'businessFunction',
            label: '业务功能配置',
            children: <BusinessFunctionTab />,
          },
        ]}
      />
    </div>
  );
};

export default TableModelPage;
