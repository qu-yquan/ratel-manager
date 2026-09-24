import React, { useState, useEffect } from 'react';
import { Table, Button, Tag, Popconfirm, App, Space, Spin } from 'antd';
import { DeleteOutlined, StarOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { getDeptUsers, removeUserDept, setPrimaryDept } from '@/services/dept';
import type { UserDeptDetail } from '../../types';
import {AuthGate} from '@gwsu/core'
import { PERM_PRIMARY_DEPT, PERM_REMOVE_USER } from '../../permissionConstants';

interface UserListTabProps {
  deptId: string;
  onAddUser: () => void ;
  onRefresh: () => void;
}

const UserListTab: React.FC<UserListTabProps> = ({ deptId, onRefresh }) => {
  const { message } = App.useApp();
  const [users, setUsers] = useState<UserDeptDetail[]>([]);
  const [loading, setLoading] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const data = await getDeptUsers(deptId);
      setUsers(data);
    } catch {
      message.error('加载用户列表失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadUsers();
  }, [deptId]);

  const handleRemove = async (userId: string) => {
    try {
      await removeUserDept({ userId, deptIds: [deptId] });
      message.success('移除成功');
      void loadUsers();
      onRefresh();
    } catch {
      message.error('移除失败');
    }
  };

  const handleSetPrimary = async (userId: string) => {
    try {
      await setPrimaryDept({ userId, deptId });
      message.success('设置主部门成功');
      void loadUsers();
    } catch {
      message.error('设置主部门失败');
    }
  };

  const columns: ColumnsType<UserDeptDetail> = [
    {
      title: "用户名",
      dataIndex: "username",
      key: "username",
    },
    {
      title: "昵称",
      dataIndex: "nickname",
      key: "nickname",
    },
    {
      title: "是否主部门",
      dataIndex: "isPrimary",
      key: "isPrimary",
      render: (isPrimary: boolean) =>
        isPrimary ? <Tag color="blue">主部门</Tag> : <Tag>普通</Tag>,
    },
    {
      title: "操作",
      key: "action",
      width: 180,
      render: (_, record) => (
        <Space size="small">
          {!record.isPrimary && (
            <AuthGate buttonKey={PERM_PRIMARY_DEPT}>
              <Button
                type="link"
                size="small"
                icon={<StarOutlined />}
                data-ai-approval
                onClick={() => handleSetPrimary(record.userId)}
              >
                设为主部门
              </Button>
            </AuthGate>
          )}
          <AuthGate buttonKey={PERM_REMOVE_USER}>
            <Popconfirm
              title="确定要移除该用户吗？"
              onConfirm={() => handleRemove(record.userId)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                data-ai-approval
                icon={<DeleteOutlined />}
              >
                移除
              </Button>
            </Popconfirm>
          </AuthGate>
        </Space>
      ),
    },
  ];

  return (
    <Spin spinning={loading}>

      <Table
        columns={columns}
        dataSource={users}
        rowKey="userId"
        pagination={false}
        size="small"
      />
    </Spin>
  );
};

export default UserListTab;
