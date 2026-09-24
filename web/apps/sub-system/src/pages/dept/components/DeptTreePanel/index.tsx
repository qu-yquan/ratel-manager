import React from 'react';
import { Input, Button, Tree, App, Spin } from 'antd';
import type { TreeProps } from 'antd';
import {
  SearchOutlined,
  PlusOutlined,
  MoreOutlined,
} from '@ant-design/icons';
// @ts-ignore
import { history } from 'umi';
import styles from './index.module.less';
import { getDeptDetail } from '@/services/dept';
import type { DeptTreeNode, DeptDetail } from '../../types';
import { useDeptTree } from '../../hooks/useDeptTree';
import {AuthGate} from '@gwsu/core'
import { PERM_ADD } from '../../permissionConstants';

interface DeptTreePanelProps {
  treeData: DeptTreeNode[];
  loading: boolean;
  selectedKey: number | null;
  onSelect: (nodeId: string, dept: DeptDetail) => void;
  onCreateRoot: () => void;
  onCreateChild: (parentId: string) => void;
}

const DeptTreePanel: React.FC<DeptTreePanelProps> = ({
  treeData,
  loading,
  selectedKey,
  onSelect,
  onCreateRoot,
  onCreateChild,
}) => {
  const { message } = App.useApp();
  const {
    searchValue,
    setSearchValue,
    expandedKeys,
    setExpandedKeys,
    getDeptIcon,
    filteredTreeData,
  } = useDeptTree(treeData);

  const convertToTreeData = (data: DeptTreeNode[]): TreeProps['treeData'] => {
    return data.map((node) => ({
      key: node.id,
      title: (
        <div className={styles.treeNode}>
          <span
            className={styles.nodeIcon}
            style={{ color: node.enabled ? undefined : "#999" }}
          >
            {getDeptIcon(node.type)}
          </span>
          <span
            className={`${styles.nodeName} ${
              !node.enabled ? styles.disabledNode : ""
            }`}
          >
            {node.name}
          </span>
          <div className={styles.nodeActions}>
            <AuthGate buttonKey={PERM_ADD}>
              <Button
                type="text"
                size="small"
                icon={<PlusOutlined />}
                className={styles.actionBtn}
                onClick={(e) => {
                  e.stopPropagation();
                  onCreateChild(node.id);
                }}
              />
            </AuthGate>

            <Button
              type="text"
              size="small"
              icon={<MoreOutlined />}
              className={styles.actionBtn}
              onClick={(e) => {
                e.stopPropagation();
              }}
            />
          </div>
        </div>
      ),
      children: node.children ? convertToTreeData(node.children) : undefined,
    }));
  };

  const handleSelect: TreeProps['onSelect'] = async (keys) => {
    if (keys.length === 0) return;
    const nodeId = keys[0] as string;
    try {
      const dept = await getDeptDetail(nodeId);
      onSelect(nodeId, dept);
    } catch {
      message.error('获取部门详情失败');
    }
  };

  return (
    <div className={styles.treePanel}>
      <div className={styles.header}>
        <span className={styles.title}>部门管理</span>
        <Button type="link" onClick={() => history.push("/dept/org-chart")}>
          组织架构图
        </Button>
      </div>
      <div className={styles.searchWrapper}>
        <Input
          placeholder="搜索部门"
          prefix={<SearchOutlined />}
          value={searchValue}
          onChange={(e) => setSearchValue(e.target.value)}
          allowClear
        />
      </div>
      <Spin spinning={loading}>
        <div className={styles.treeWrapper}>
          <Tree
            showLine
            treeData={convertToTreeData(filteredTreeData)}
            selectedKeys={selectedKey ? [selectedKey] : []}
            expandedKeys={expandedKeys}
            onExpand={setExpandedKeys}
            onSelect={handleSelect}
          />
        </div>
      </Spin>
      <div style={{ padding: "8px 16px", borderTop: "1px solid #e8e8e8" }}>
        <AuthGate buttonKey="22_add">
          <Button
            type="primary"
            icon={<PlusOutlined />}
            block
            onClick={onCreateRoot}
          >
            新增部门
          </Button>
        </AuthGate>
      </div>
    </div>
  );
};

export default DeptTreePanel;
