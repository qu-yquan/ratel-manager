import React from 'react';
import { Button, Popconfirm } from 'antd';
import { PlusOutlined, DeleteOutlined, ClockCircleOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import type { ValidGroup } from '../../types';
import { getValidGroupLabel } from './ValidConfigForm';

interface ValidGroupListProps {
  /** 时效分组列表 */
  groups: ValidGroup[];
  /** 当前选中的时效组 ID（roleMenuId） */
  selectedId: string | null;
  /** 选中某组 */
  onSelect: (roleMenuId: string) => void;
  /** 新增时效组 */
  onAdd: () => void;
  /** 删除时效组 */
  onDelete: (roleMenuId: string) => void;
  /** 是否处于编辑模式（编辑中不可删除） */
  editing: boolean;
}

/** 左侧 - 时效分组列表组件 */
const ValidGroupList: React.FC<ValidGroupListProps> = ({
  groups,
  selectedId,
  onSelect,
  onAdd,
  onDelete,
  editing,
}) => {
  return (
    <div className={styles.leftPanel}>
      {/* 头部标题 + 新增按钮 */}
      <div className={styles.leftHeader}>
        <span className={styles.leftTitle}>时效分组</span>
        <Button
          type="primary"
          size="small"
          icon={<PlusOutlined />}
          onClick={onAdd}
          disabled={editing}
        >
          新增时效组
        </Button>
      </div>

      {/* 分组列表 */}
      <div className={styles.groupList}>
        {groups.length === 0 ? (
          <div className={styles.leftEmpty}>
            <ClockCircleOutlined className={styles.leftEmptyIcon} />
            <span className={styles.leftEmptyText}>暂无时效分组</span>
            <span className={styles.leftEmptyText}>点击上方按钮新增</span>
          </div>
        ) : (
          groups.map((group) => {
            const isActive = selectedId === group.roleMenuId;
            return (
              <div
                key={group.roleMenuId}
                className={`${styles.groupItem} ${isActive ? styles.groupItemActive : ''}`}
                onClick={() => onSelect(group.roleMenuId)}
              >
                <div className={styles.groupInfo}>
                  <div className={styles.groupLabel} title={getValidGroupLabel(group)}>
                    {getValidGroupLabel(group)}
                  </div>
                  <div className={styles.groupCount}>
                    {group.menuCount} 个菜单/按钮
                  </div>
                </div>
                <Popconfirm
                  title="确定删除此时效组？"
                  description="删除后关联的菜单将自动释放"
                  onConfirm={(e) => {
                    e?.stopPropagation();
                    onDelete(group.roleMenuId);
                  }}
                  onCancel={(e) => e?.stopPropagation()}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button
                    type="text"
                    size="small"
                    danger
                    icon={<DeleteOutlined />}
                    className={styles.groupDeleteBtn}
                    onClick={(e) => e.stopPropagation()}
                    disabled={editing}
                  />
                </Popconfirm>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default ValidGroupList;
