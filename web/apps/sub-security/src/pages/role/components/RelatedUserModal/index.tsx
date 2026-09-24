import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Modal, Transfer, Spin, message } from 'antd';
import type { TransferProps } from 'antd';
import styles from './index.module.less';
import {
  getSubjectIdsByRoleId,
  allocateSubjectsToRole,
  getUserPage,
} from '../../services/role';
import type { UserInfoItem } from '../../types';

interface RelatedUserModalProps {
  visible: boolean;
  roleId: string | null;
  roleName: string;
  onClose: () => void;
}

const RelatedUserModal: React.FC<RelatedUserModalProps> = ({
  visible,
  roleId,
  roleName,
  onClose,
}) => {
  const [allUsers, setAllUsers] = useState<UserInfoItem[]>([]);
  const [targetKeys, setTargetKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  /** 加载数据 */
  const loadData = useCallback(async () => {
    if (!roleId) return;
    setLoading(true);
    try {
      const [users, subjectIds] = await Promise.all([
        getUserPage({ pageNum: 1, pageSize: 9999 }),
        getSubjectIdsByRoleId(roleId),
      ]);
      setAllUsers(users?.records ?? []);
      setTargetKeys(subjectIds);
    } catch {
      message.error('加载数据失败');
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  useEffect(() => {
    if (visible && roleId) {
      loadData();
    }
  }, [visible, roleId, loadData]);

  /** 穿梭框数据源 */
  const transferDataSource = useMemo(() => {
    return allUsers.map((user) => ({
      key: user.userId,
      title: user.nickname || user.userName,
    }));
  }, [allUsers]);

  /** 穿梭框变更 */
  const handleChange: TransferProps['onChange'] = (nextTargetKeys) => {
    setTargetKeys(nextTargetKeys as string[]);
  };

  /** 穿梭框搜索过滤 */
  const filterOption = (
    inputValue: string,
    option: { key?: string; title?: string },
  ) => (option.title ?? '').toLowerCase().includes(inputValue.toLowerCase());

  /** 保存 */
  const handleSave = async () => {
    if (!roleId) return;
    setSaving(true);
    try {
      await allocateSubjectsToRole(roleId, targetKeys);
      message.success('关联用户保存成功');
      onClose();
    } catch {
      // request 层已自动提示
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={`关联用户 - ${roleName}`}
      open={visible}
      onCancel={onClose}
      onOk={handleSave}
      confirmLoading={saving}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ "data-ai-approval": "true" }}
      width={720}
      destroyOnHidden
      className={styles.relatedUserModal}
    >
      <Spin spinning={loading}>
        <div className={styles.transferWrapper}>
          <Transfer
            dataSource={transferDataSource}
            targetKeys={targetKeys}
            onChange={handleChange}
            filterOption={filterOption}
            showSearch
            titles={["未关联", "已关联"]}
            listStyle={{ width: 300, height: 400 }}
            oneWay={false}
            render={(item) => item.title ?? ""}
          />
        </div>
      </Spin>
    </Modal>
  );
};

export default RelatedUserModal;
