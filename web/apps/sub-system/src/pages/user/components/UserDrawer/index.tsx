import React, { useState, useCallback } from 'react';
import { Drawer, Tag, Button, App } from 'antd';
import styles from './index.module.less';
import BasicInfoSection from './BasicInfoSection';
import AccountBindSection from './AccountBindSection';
import DeptAssignSection from './DeptAssignSection';
import type { SysUserDetailVO } from '../../types';
import { USER_STATUS_MAP } from '../../types';
import type { DeptTreeNode } from '../../../dept/types';
import { getUserDetail, updateUserStatus } from '@/services/user';
import { AuthGate } from "@gwsu/core";
import { PERM_DISABLED_ENABLE } from '../../permissionConstants';

interface UserDrawerProps {
  visible: boolean;
  userId: string | null;
  treeData: DeptTreeNode[];
  onClose: () => void;
  /** 模式：detail=只读详情，edit=可编辑 */
  mode?: 'detail' | 'edit';
}

const UserDrawer: React.FC<UserDrawerProps> = ({ visible, userId, treeData, onClose, mode = 'detail' }) => {
  const { message } = App.useApp();
  const isReadOnly = mode === 'detail';
  const [user, setUser] = useState<SysUserDetailVO | null>(null);
  const [loading, setLoading] = useState(false);

  const loadDetail = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const detail = await getUserDetail(userId);
      setUser(detail);
    } catch {
      message.error('获取用户详情失败');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  React.useEffect(() => {
    if (visible && userId) {
      loadDetail();
    } else {
      setUser(null);
    }
  }, [visible, userId, loadDetail]);

  const handleStatusToggle = async () => {
    if (!user) return;
    const newStatus = user.status === 1 ? 0 : 1;
    await updateUserStatus(user.userId, newStatus);
    message.success(newStatus === 1 ? '已启用' : '已禁用');
    loadDetail();
  };

  if (!user) return null;

  const statusInfo = USER_STATUS_MAP[user.status] || { text: '未知', color: '#999' };
  const primaryDept = user.depts?.find((d) => d.isPrimary);

  return (
    <Drawer
      title="用户详情"
      open={visible}
      onClose={onClose}
      size={640}
      loading={loading}
      footer={
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
          <Button onClick={onClose}>关闭</Button>
          {!isReadOnly && (
            <AuthGate buttonKey={PERM_DISABLED_ENABLE}>
              <Button
                data-ai-approval
                danger={user.status === 1}
                type={user.status === 0 ? "primary" : "default"}
                onClick={handleStatusToggle}
              >
                {user.status === 1 ? "禁用用户" : "启用用户"}
              </Button>
            </AuthGate>
          )}
        </div>
      }
    >
      <div className={styles.userDrawer}>
        <div className={styles.drawerHeader}>
          <div className={styles.avatar}>{user.nickname?.charAt(0) || "?"}</div>
          <div className={styles.info}>
            <div className={styles.name}>
              {user.nickname}{" "}
              <span style={{ fontSize: 12, color: "#999", fontWeight: 400 }}>
                / {user.userName}
              </span>
            </div>
            <div className={styles.tags}>
              <Tag color={statusInfo.color === "#52c41a" ? "success" : "error"}>
                {statusInfo.text}
              </Tag>
              {primaryDept && <Tag color="blue">{primaryDept.deptName}</Tag>}
            </div>
          </div>
        </div>

        <div className={styles.section}>
          <BasicInfoSection
            user={user}
            onRefresh={loadDetail}
            readOnly={isReadOnly}
          />
        </div>

        <div className={styles.section}>
          <AccountBindSection
            userId={user.userId}
            accounts={user.accounts || []}
            onRefresh={loadDetail}
            readOnly={isReadOnly}
          />
        </div>

        <div className={styles.section}>
          <DeptAssignSection
            userId={user.userId}
            depts={user.depts || []}
            treeData={treeData}
            onRefresh={loadDetail}
            readOnly={isReadOnly}
          />
        </div>
      </div>
    </Drawer>
  );
};

export default UserDrawer;
