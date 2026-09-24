import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Modal, Input, Checkbox, Tag, App, Spin } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import {
  allocateRoles,
  getUserRoleCodes,
  getEnabledRoleList,
} from '@/services/user';
import type { RoleOption } from '@/services/user';

interface AssignRoleModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 用户ID */
  userId: string | null;
  /** 用户昵称 */
  nickname?: string;
  /** 关闭回调 */
  onClose: () => void;
  /** 保存成功回调 */
  onSuccess: () => void;
}

const AssignRoleModal: React.FC<AssignRoleModalProps> = ({
  visible,
  userId,
  nickname,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [allRoles, setAllRoles] = useState<RoleOption[]>([]);
  const [assignedRoleCodes, setAssignedRoleCodes] = useState<string[]>([]);
  const [selectedRoleIds, setSelectedRoleIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [keyword, setKeyword] = useState('');

  // 加载数据
  const loadData = useCallback(async () => {
    if (!userId) return;
    setLoading(true);
    try {
      const [roles, codes] = await Promise.all([
        getEnabledRoleList(),
        getUserRoleCodes(userId),
      ]);
      setAllRoles(roles);
      setAssignedRoleCodes(codes);
      // 根据已分配的角色编码，找出对应的角色ID作为初始选中
      const assignedIds = roles
        .filter((r) => codes.includes(r.roleCode))
        .map((r) => r.id);
      setSelectedRoleIds(assignedIds);
    } catch {
      message.error('加载角色数据失败');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (visible && userId) {
      setKeyword('');
      loadData();
    }
  }, [visible, userId, loadData]);

  // 搜索过滤
  const filteredRoles = useMemo(() => {
    if (!keyword.trim()) return allRoles;
    const lower = keyword.toLowerCase();
    return allRoles.filter(
      (r) =>
        r.roleName.toLowerCase().includes(lower) ||
        r.roleCode.toLowerCase().includes(lower),
    );
  }, [allRoles, keyword]);

  // 勾选/取消
  const handleCheck = (roleId: string, checked: boolean) => {
    setSelectedRoleIds((prev) =>
      checked ? [...prev, roleId] : prev.filter((id) => id !== roleId),
    );
  };

  // 全选/取消全选
  const handleCheckAll = (checked: boolean) => {
    setSelectedRoleIds(checked ? filteredRoles.map((r) => r.id) : []);
  };

  const isAllChecked =
    filteredRoles.length > 0 &&
    filteredRoles.every((r) => selectedRoleIds.includes(r.id));
  const isIndeterminate =
    filteredRoles.some((r) => selectedRoleIds.includes(r.id)) &&
    !isAllChecked;

  // 保存
  const handleSave = async () => {
    if (!userId) return;
    setSaving(true);
    try {
      await allocateRoles(userId, selectedRoleIds);
      message.success('角色分配成功');
      onSuccess();
    } catch {
      // request 层已自动提示
    } finally {
      setSaving(false);
    }
  };

  // 判断某角色是否为已分配（初始状态）
  const isOriginallyAssigned = (role: RoleOption) =>
    assignedRoleCodes.includes(role.roleCode);

  return (
    <Modal
      title={`分配角色 - ${nickname || ""}`}
      open={visible}
      onCancel={onClose}
      okButtonProps={{ "data-ai-approval": "true" }}
      onOk={handleSave}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      width={520}
      destroyOnHidden
      className={styles.assignRoleModal}
    >
      <Spin spinning={loading}>
        <div className={styles.searchBar}>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索角色名称或编码"
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>

        {filteredRoles.length > 0 && (
          <div
            style={{
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
              padding: "0 16px 8px",
            }}
          >
            <Checkbox
              checked={isAllChecked}
              indeterminate={isIndeterminate}
              onChange={(e) => handleCheckAll(e.target.checked)}
            >
              全选
            </Checkbox>
            <span className={styles.footerInfo}>
              已选 {selectedRoleIds.length} / {allRoles.length} 个角色
            </span>
          </div>
        )}

        <div className={styles.roleList}>
          {filteredRoles.length === 0 && !loading && (
            <div className={styles.emptyTip}>
              {keyword ? "未找到匹配的角色" : "暂无可分配的角色"}
            </div>
          )}
          {filteredRoles.map((role) => {
            const checked = selectedRoleIds.includes(role.id);
            const originallyAssigned = isOriginallyAssigned(role);
            return (
              <div
                key={role.id}
                className={styles.roleItem}
                onClick={() => handleCheck(role.id, !checked)}
              >
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <Checkbox checked={checked} />
                  <div className={styles.roleItemInfo}>
                    <span className={styles.roleItemName}>{role.roleName}</span>
                    <span className={styles.roleItemCode}>{role.roleCode}</span>
                  </div>
                </div>
                {originallyAssigned && (
                  <Tag color="blue" style={{ fontSize: 11 }}>
                    已分配
                  </Tag>
                )}
              </div>
            );
          })}
        </div>
      </Spin>
    </Modal>
  );
};

export default AssignRoleModal;
