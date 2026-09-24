import React, { useState } from 'react';
import { Tabs } from 'antd';
import BasicInfoTab from './BasicInfoTab';
import RelationGraphTab from './RelationGraphTab';
import UserListTab from './UserListTab';
import AddParentModal from '../AddParentModal';
import styles from './index.module.less';
import type { DeptDetail, DeptTypeOption } from '../../types';

interface DeptDetailPanelProps {
  dept: DeptDetail;
  deptTypes: DeptTypeOption[];
  onEdit: () => void;
  onDeleteSuccess: () => void;
  onRefresh: () => void;
}

const DeptDetailPanel: React.FC<DeptDetailPanelProps> = ({
  dept,
  deptTypes,
  onEdit,
  onDeleteSuccess,
  onRefresh,
}) => {
  const [activeTab, setActiveTab] = useState('basic');
  const [addParentVisible, setAddParentVisible] = useState(false);
  const [ setAddUserVisible] = useState(false);

  const handleNodeClick = (deptId: number) => {
    console.log('Navigate to dept:', deptId);
  };

  // 当前已存在的所有父部门ID（用于过滤）
  const currentParentIds = [
    ...(dept.parentId ? [dept.parentId] : []),
    ...(dept.extraParents?.map((p) => p.id) ?? []),
  ];

  // @ts-ignore
  const tabItems = [
    {
      key: 'basic',
      label: '基本信息',
      children: (
        <BasicInfoTab
          dept={dept}
          deptTypes={deptTypes}
          onEdit={onEdit}
          onDeleteSuccess={onDeleteSuccess}
          onRefresh={onRefresh}
          onAddParent={() => setAddParentVisible(true)}
        />
      ),
    },
    {
      key: 'relation',
      label: '关系图',
      children: <RelationGraphTab dept={dept} onNodeClick={handleNodeClick} />,
    },
    {
      key: 'users',
      label: '用户列表',
      children: (
        <UserListTab
          deptId={dept.id}
          onAddUser={() => setAddUserVisible(true)}
          onRefresh={onRefresh}
        />
      ),
    },
  ];

  return (
    <div className={styles.detailPanel}>
      <div className={styles.header}>
        <span className={styles.title}>{dept.name}</span>
      </div>
      <div className={styles.content}>
        <Tabs items={tabItems} activeKey={activeTab} onChange={setActiveTab} />
      </div>
      <AddParentModal
        visible={addParentVisible}
        deptId={dept.id}
        currentParentIds={currentParentIds}
        onClose={() => setAddParentVisible(false)}
        onSuccess={() => {
          setAddParentVisible(false);
          onRefresh();
        }}
      />
    </div>
  );
};

export default DeptDetailPanel;
