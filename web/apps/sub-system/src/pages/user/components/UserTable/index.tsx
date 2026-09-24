import React, { useState } from 'react';
import { Table, Input, Select, Button, Tag, Modal, Dropdown, App } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import styles from './index.module.less';
import type { SysUserVO, SysUserQueryDTO } from '../../types';
import { USER_STATUS_MAP } from '../../types';
import { batchDeleteUsers } from '@/services/user';
import { AuthGate , useAuth } from '@gwsu/core';
import { PERM_ADD, PERM_REMOVE, PERM_CHANGE_PWD, PERM_ROLE } from '../../permissionConstants';

interface UserTableProps {
  users: SysUserVO[];
  total: number;
  loading: boolean;
  current: number;
  pageSize: number;
  deptName?: string;
  onQueryChange: (query: Partial<SysUserQueryDTO>) => void;
  onDetail: (userId: string) => void;
  onEdit: (userId: string) => void;
  onCreate: () => void;
  onResetPassword: (userId: string, nickname: string) => void;
  onAssignRole: (userId: string, nickname: string) => void;
  onRefresh: () => void;
}

const UserTable: React.FC<UserTableProps> = ({
  users,
  total,
  loading,
  current,
  pageSize,
  deptName,
  onQueryChange,
  onDetail,
  onEdit,
  onCreate,
  onResetPassword,
  onAssignRole,
  onRefresh,
}) => {
  const { message } = App.useApp();
  let canChangePwd = useAuth(PERM_CHANGE_PWD);
  let canRole = useAuth(PERM_ROLE);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);


  const handleBatchDelete = () => {
    Modal.confirm({
      title: "确认删除",
      content: `确定删除选中的 ${selectedRowKeys.length} 个用户吗？删除后不可恢复。`,
      okText: "确定",
      cancelText: "取消",
      okButtonProps: { danger: true, "data-ai-approval": "true" },
      onOk: async () => {
        try {
          await batchDeleteUsers(selectedRowKeys as string[]);
          message.success("删除成功");
          setSelectedRowKeys([]);
          onRefresh();
        } catch {}
      },
    });
  };

  let getActionMenuItems = (record: SysUserVO): MenuProps['items'] => {

    let buttons =  [
      {
        key: "edit",
        label: "编辑",
        onClick: () => onEdit(record.userId),
      },

    ];

    if(canRole){
      buttons.push({
        key: "assignRole",
        label: "分配角色",
        onClick: () => onAssignRole(record.userId, record.nickname),
      });
    }

    if (canChangePwd) {
      buttons.push({
        key: "resetPassword",
        label: "修改密码",
        onClick: () => onResetPassword(record.userId, record.nickname),
      });
    }

    return buttons
  };

  const columns = [
    {
      title: '用户名',
      dataIndex: 'userName',
      key: 'userName',
      width: 150,
      render: (text: string, record: SysUserVO) => (
        <a onClick={() => onDetail(record.userId)}>{text}</a>
      ),
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      width: 120,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
      ellipsis: true,
    },
    {
      title: '手机',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      fixed: 'right' as const,
      render: (status: number) => {
        const statusInfo = USER_STATUS_MAP[status] || { text: '未知', color: '#999' };
        return <Tag color={statusInfo.color === '#52c41a' ? 'success' : 'error'}>{statusInfo.text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right' as const,
      render: (_: unknown, record: SysUserVO) => (
        <div className={styles.actionCell}>
          <a onClick={() => onDetail(record.userId)}>详情</a>
          <Dropdown menu={{ items: getActionMenuItems(record) }} trigger={['click']}>
            <a onClick={(e) => e.preventDefault()}>更多</a>
          </Dropdown>
        </div>
      ),
    },
  ];

  return (
    <div className={styles.userTable}>
      <div className={styles.toolbar}>
        <div className={styles.titleArea}>
          <span className={styles.title}>
            {deptName ? `${deptName} 及下级用户` : "全部用户"}
          </span>
          <span className={styles.count}>共 {total} 人</span>
        </div>
        <div className={styles.filterArea}>
          <Input.Search
            placeholder="搜索用户名/昵称/手机"
            allowClear
            style={{ width: 200 }}
            onSearch={(value) => onQueryChange({ keyword: value || undefined })}
          />
          <Select
            placeholder="全部状态"
            allowClear
            style={{ width: 110 }}
            onChange={(value) => onQueryChange({ status: value })}
            options={[
              { label: "启用", value: 1 },
              { label: "禁用", value: 0 },
            ]}
          />
        </div>
      </div>
      <div className={styles.actionBar}>
        <AuthGate buttonKey={PERM_ADD}>
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>
            新增用户
          </Button>
        </AuthGate>
        <AuthGate buttonKey={PERM_REMOVE}>
          <Button
            danger
            icon={<DeleteOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleBatchDelete}
          >
            {selectedRowKeys.length > 0
              ? `删除 (${selectedRowKeys.length})`
              : "删除"}
          </Button>
        </AuthGate>
      </div>
      <Table
        columns={columns}
        dataSource={users}
        rowKey="userId"
        loading={loading}
        rowSelection={{
          selectedRowKeys,
          onChange: setSelectedRowKeys,
        }}
        pagination={{
          current,
          pageSize,
          total,
          showTotal: (t) => `共 ${t} 条`,
          onChange: (page, size) =>
            onQueryChange({ pageNum: page, pageSize: size }),
        }}
        scroll={{ x: 800, y: "calc(100vh - 280px)" }}
      />
    </div>
  );
};

export default UserTable;
