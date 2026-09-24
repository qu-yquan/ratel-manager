import React, { useState, useEffect, useCallback } from 'react';
import { Avatar, Card, Descriptions, Tag, Spin, App, Button } from 'antd';
import {
  MailOutlined,
  PhoneOutlined,
  ApartmentOutlined,
  TeamOutlined,
  ManOutlined,
  WomanOutlined,
  StarOutlined,
  LockOutlined,
  EditOutlined,
} from '@ant-design/icons';
import { useUserStore, AuthGate, fetchCurrentUserInfo } from '@gwsu/core';
import { getCurrentUserRoles, updateCurrentUserProfile } from '@/services/profile';
import type { RoleVO } from '@/services/profile';
import { GENDER_MAP, USER_STATUS_MAP } from '@/pages/user/types';
import { PERM_CHANGE_PASSWORD, PERM_EDIT_PROFILE } from './permissionConstants';
import ChangePasswordModal from './components/ChangePasswordModal';
import EditProfileModal from './components/EditProfileModal';
import styles from './index.module.less';

/** 获取用户名首字母 */
function getInitial(name: string): string {
  if (!name) return 'U';
  return name.charAt(0).toUpperCase();
}

const ProfilePage: React.FC = () => {
  const userInfo = useUserStore((s) => s.userInfo);
  const { message } = App.useApp();

  const [roles, setRoles] = useState<RoleVO[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [passwordModalVisible, setPasswordModalVisible] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);

  const displayName = userInfo?.nickname || userInfo?.username || '用户';
  const avatarUrl = userInfo?.avatar;
  const initial = getInitial(displayName);
  const genderLabel = GENDER_MAP[userInfo?.gender ?? 0] ?? '未知';
  const statusInfo = USER_STATUS_MAP[userInfo?.status ?? 1] ?? { text: '未知', color: '#999' };

  // 加载角色信息
  const loadRoles = useCallback(async () => {
    setRolesLoading(true);
    try {
      const data = await getCurrentUserRoles();
      setRoles(data);
    } catch {
      message.error('加载角色信息失败');
    } finally {
      setRolesLoading(false);
    }
  }, [message]);

  useEffect(() => {
    void loadRoles();
  }, [loadRoles]);

  const refreshCurrentUser = useCallback(async () => {
    const latestUserInfo = await fetchCurrentUserInfo();
    useUserStore.getState().setUserInfo(latestUserInfo);
  }, []);

  const handleProfileSubmit = useCallback(async (values: {
    nickname: string;
    gender: number;
    email?: string;
    phone?: string;
  }) => {
    await updateCurrentUserProfile(values);
    await refreshCurrentUser();
    setEditModalVisible(false);
  }, [refreshCurrentUser]);

  return (
    <div className={styles.profilePage}>
      {/* 用户头像卡片 */}
      <Card className={styles.profileCard} variant="borderless">
        <div className={styles.profileHeader}>
          <Avatar
            size={80}
            src={avatarUrl || undefined}
            style={
              avatarUrl
                ? undefined
                : {
                    background: 'linear-gradient(135deg, var(--primary-color, #1a5fb4), #764ba2)',
                    color: '#fff',
                    fontWeight: 600,
                    fontSize: 32,
                  }
            }
          >
            {avatarUrl ? undefined : initial}
          </Avatar>
          <div className={styles.profileHeaderInfo}>
            <h2 className={styles.profileName}>{displayName}</h2>
            <div className={styles.profileMeta}>
              {userInfo?.email && (
                <span className={styles.profileMetaItem}>
                  <MailOutlined /> {userInfo.email}
                </span>
              )}
              {userInfo?.deptName && (
                <span className={styles.profileMetaItem}>
                  <ApartmentOutlined /> {userInfo.deptName}
                </span>
              )}
            </div>
          </div>
        </div>
      </Card>

      {/* 基本信息 */}
      <Card
        title="基本信息"
        className={styles.sectionCard}
        variant="borderless"
        extra={
          <div className={styles.actionGroup}>
            <AuthGate buttonKey={PERM_EDIT_PROFILE}>
              <Button
                type="link"
                icon={<EditOutlined />}
                onClick={() => setEditModalVisible(true)}
              >
                编辑资料
              </Button>
            </AuthGate>
            <AuthGate buttonKey={PERM_CHANGE_PASSWORD}>
              <Button
                type="link"
                icon={<LockOutlined />}
                onClick={() => setPasswordModalVisible(true)}
              >
                修改密码
              </Button>
            </AuthGate>
          </div>
        }
      >
        <Descriptions column={{ xs: 1, sm: 2 }} colon={false} styles={{ label: { color: 'var(--text-secondary-color)' } }}>
          <Descriptions.Item label="用户名">{userInfo?.username ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="昵称">{userInfo?.nickname ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="性别">
            <span>
              {userInfo?.gender === 1 ? (
                <ManOutlined style={{ color: '#1890ff', marginRight: 4 }} />
              ) : userInfo?.gender === 2 ? (
                <WomanOutlined style={{ color: '#eb2f96', marginRight: 4 }} />
              ) : null}
              {genderLabel}
            </span>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
          </Descriptions.Item>
          <Descriptions.Item label="邮箱">
            {userInfo?.email ? (
              <span>
                <MailOutlined style={{ marginRight: 4, color: 'var(--text-secondary-color)' }} />
                {userInfo.email}
              </span>
            ) : (
              '-'
            )}
          </Descriptions.Item>
          <Descriptions.Item label="手机号">
            {userInfo?.phone ? (
              <span>
                <PhoneOutlined style={{ marginRight: 4, color: 'var(--text-secondary-color)' }} />
                {userInfo.phone}
              </span>
            ) : (
              '-'
            )}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 部门信息 */}
      <Card
        title={
          <span>
            <ApartmentOutlined style={{ marginRight: 8 }} />
            部门信息
          </span>
        }
        className={styles.sectionCard}
        variant="borderless"
      >
        {userInfo?.depts && userInfo.depts.length > 0 ? (
          <div className={styles.deptList}>
            {userInfo.depts.map((dept) => (
              <div key={dept.deptId} className={styles.deptItem}>
                <div className={styles.deptItemMain}>
                  <ApartmentOutlined style={{ color: 'var(--primary-color)', marginRight: 8 }} />
                  <span className={styles.deptName}>{dept.deptName}</span>
                  {dept.isPrimary && (
                    <Tag color="blue" className={styles.primaryTag}>
                      <StarOutlined style={{ marginRight: 2 }} />
                      主部门
                    </Tag>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : userInfo?.deptName ? (
          <Descriptions column={{ xs: 1, sm: 2 }} colon={false} styles={{ label: { color: 'var(--text-secondary-color)' } }}>
            <Descriptions.Item label="所属部门">
              <span>
                <ApartmentOutlined style={{ marginRight: 4, color: 'var(--primary-color)' }} />
                {userInfo.deptName}
              </span>
            </Descriptions.Item>
            <Descriptions.Item label="部门ID">{userInfo.deptId ?? '-'}</Descriptions.Item>
          </Descriptions>
        ) : (
          <div className={styles.emptyText}>暂无部门信息</div>
        )}
      </Card>

      {/* 角色信息 */}
      <Card
        title={
          <span>
            <TeamOutlined style={{ marginRight: 8 }} />
            角色信息
          </span>
        }
        className={styles.sectionCard}
        variant="borderless"
      >
        <Spin spinning={rolesLoading}>
          {roles.length > 0 ? (
            <div className={styles.roleList}>
              {roles.map((role) => (
                <div key={role.id} className={styles.roleItem}>
                  <div className={styles.roleItemMain}>
                    <Tag
                      color={role.status ? 'blue' : 'default'}
                      className={styles.roleTag}
                    >
                      {role.roleName}
                    </Tag>
                    <span className={styles.roleCode}>{role.roleCode}</span>
                  </div>
                  {role.description && (
                    <div className={styles.roleDesc}>{role.description}</div>
                  )}
                </div>
              ))}
            </div>
          ) : (
            <div className={styles.emptyText}>
              {rolesLoading ? '加载中...' : '暂无角色信息'}
            </div>
          )}
        </Spin>
      </Card>

      {/* 修改密码弹窗 */}
      <ChangePasswordModal
        visible={passwordModalVisible}
        onClose={() => setPasswordModalVisible(false)}
        onSuccess={() => setPasswordModalVisible(false)}
      />
      <EditProfileModal
        visible={editModalVisible}
        userInfo={userInfo}
        onClose={() => setEditModalVisible(false)}
        onSubmit={handleProfileSubmit}
      />
    </div>
  );
};

export default ProfilePage;
