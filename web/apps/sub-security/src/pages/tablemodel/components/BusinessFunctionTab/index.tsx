import React, { useState, useCallback, useEffect } from 'react';
import {
  Button,
  Table,
  Input,
  Space,
  Popconfirm,
  Dropdown,
  type MenuProps,
  App,
} from 'antd';
import type { TableProps } from 'antd';
import {
  PlusOutlined,
  SearchOutlined,
  ReloadOutlined,
  EyeOutlined,
  EditOutlined,
  MoreOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import {
  getBusinessFunctionPage,
  batchDeleteBusinessFunctions,
} from '../../services/businessFunction';
import type { BusinessFunctionInfo, BusinessFunctionQuery } from '../../types';
import {
  PERM_BF_ADD,
  PERM_BF_REMOVE,
  PERM_BF_EDIT,
} from '../../permissionConstants';
import { AuthGate, useAuth } from '@gwsu/core';
import BusinessFunctionDrawer from '../BusinessFunctionDrawer';
import BusinessFunctionDetailDrawer from '../BusinessFunctionDetailDrawer';
import styles from './index.module.less';

const BusinessFunctionTab: React.FC = () => {
  const { message } = App.useApp();

  const canEdit = useAuth(PERM_BF_EDIT);

  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<BusinessFunctionInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [searchName, setSearchName] = useState('');
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const [drawerVisible, setDrawerVisible] = useState(false);
  const [editData, setEditData] = useState<BusinessFunctionInfo | null>(null);
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailData, setDetailData] = useState<BusinessFunctionInfo | null>(null);

  const fetchData = useCallback(
    async (page = currentPage, size = pageSize, name = searchName) => {
      setLoading(true);
      try {
        const query: BusinessFunctionQuery = {
          pageNum: page,
          pageSize: size,
          name: name || undefined,
        };
        const result = await getBusinessFunctionPage(query);
        setDataSource(result.records);
        setTotal(result.total);
        setCurrentPage(page);
        setPageSize(size);
      } catch {} finally {
        setLoading(false);
      }
    },
    [currentPage, pageSize, searchName],
  );

  useEffect(() => {
    void fetchData(1, 10, '');
  }, []);

  const handleSearch = useCallback(() => {
    void fetchData(1, pageSize, searchName);
  }, [fetchData, pageSize, searchName]);

  const handleReset = useCallback(() => {
    setSearchName('');
    void fetchData(1, pageSize, '');
  }, [fetchData, pageSize]);

  const handlePageChange = useCallback(
    (page: number, size: number) => {
      void fetchData(page, size, searchName);
    },
    [fetchData, searchName],
  );

  const handleCreate = useCallback(() => {
    setEditData(null);
    setDrawerVisible(true);
  }, []);

  const handleEdit = useCallback((record: BusinessFunctionInfo) => {
    setEditData(record);
    setDrawerVisible(true);
  }, []);

  const handleViewDetail = useCallback((record: BusinessFunctionInfo) => {
    setDetailData(record);
    setDetailVisible(true);
  }, []);

  const handleBatchDelete = useCallback(async () => {
    try {
      await batchDeleteBusinessFunctions(selectedRowKeys as string[]);
      message.success('删除成功');
      setSelectedRowKeys([]);
      void fetchData();
    } catch {}
  }, [selectedRowKeys, fetchData, message]);

  const handleDrawerSuccess = useCallback(() => {
    setDrawerVisible(false);
    setEditData(null);
    void fetchData();
  }, [fetchData]);

  const getMoreMenuItems = (
    record: BusinessFunctionInfo,
  ): NonNullable<MenuProps['items']> => {
    const items: NonNullable<MenuProps['items']> = [];
    if (canEdit) {
      items.push({
        key: 'edit',
        icon: <EditOutlined />,
        label: '编辑',
        onClick: () => handleEdit(record),
      });
    }
    return items;
  };

  const columns: TableProps<BusinessFunctionInfo>['columns'] = [
    Table.SELECTION_COLUMN,
    {
      title: '序号',
      width: 60,
      align: 'center',
      render: (_: unknown, __: BusinessFunctionInfo, index: number) =>
        (currentPage - 1) * pageSize + index + 1,
    },
    {
      title: '业务名称',
      dataIndex: 'name',
      key: 'name',
      width: 180,
    },
    {
      title: '业务简介',
      dataIndex: 'summary',
      key: 'summary',
      width: 300,
      ellipsis: true,
    },
    {
      title: '关联表数量',
      dataIndex: 'tableCount',
      key: 'tableCount',
      width: 100,
      align: 'center',
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
      render: (_: unknown, record: BusinessFunctionInfo) => {
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
    <div className={styles.businessFunctionPage}>
      <div className={styles.searchBar}>
        <div className={styles.searchItem}>
          <span className={styles.searchLabel}>业务名称</span>
          <Input
            placeholder="请输入业务名称"
            allowClear
            style={{ width: 200 }}
            value={searchName}
            onChange={(e) => setSearchName(e.target.value)}
            onPressEnter={handleSearch}
          />
        </div>
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

      <div className={styles.tableWrapper}>
        <div className={styles.tableHeader}>
          <div className={styles.tableTitleArea}>
            <span className={styles.tableTitle}>业务功能列表</span>
            <span className={styles.tableHint}>添加业务功能的描述可以提高助手的准确率</span>
          </div>
          <Space>
            <AuthGate buttonKey={PERM_BF_ADD}>
              <Button
                type="primary"
                icon={<PlusOutlined />}
                onClick={handleCreate}
              >
                新增
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_BF_REMOVE}>
              <Popconfirm
                title="批量删除"
                description={`确定删除选中的 ${selectedRowKeys.length} 条记录？`}
                onConfirm={handleBatchDelete}
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
        <Table<BusinessFunctionInfo>
          rowKey="id"
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          loading={loading}
          dataSource={dataSource}
          columns={columns}
          size="middle"
          pagination={{
            current: currentPage,
            pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (t) => `共 ${t} 条`,
            onChange: handlePageChange,
          }}
          scroll={{ x: 860 }}
        />
      </div>

      <BusinessFunctionDrawer
        visible={drawerVisible}
        editData={editData}
        onClose={() => {
          setDrawerVisible(false);
          setEditData(null);
        }}
        onSuccess={handleDrawerSuccess}
      />

      <BusinessFunctionDetailDrawer
        visible={detailVisible}
        data={detailData}
        onClose={() => {
          setDetailVisible(false);
          setDetailData(null);
        }}
      />
    </div>
  );
};

export default BusinessFunctionTab;
