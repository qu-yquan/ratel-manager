import React from 'react';
import { Input, Tree, Spin } from 'antd';
import type { TreeProps } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import type { DeptTreeSelectorProps } from './types';
import { useDeptTree } from '../../hooks/useDeptTree';

const DeptTreeSelector: React.FC<DeptTreeSelectorProps> = ({
  treeData,
  loading = false,
  selectedKey,
  deptUserCount,
  showUserCount = true,
  onSelect,
  title = '部门',
}) => {
  const {
    searchValue,
    setSearchValue,
    expandedKeys,
    setExpandedKeys,
    getDeptIcon,
    filteredTreeData,
  } = useDeptTree(treeData);

  const convertToTreeData = (data: TreeProps['treeData'] extends Array<infer T> ? T[] : any): TreeProps['treeData'] => {
    return data.map((node : any) => ({
      key: node.id,
      title: (
        <div className={styles.treeNode}>
          <span className={styles.nodeIcon} style={{ color: node.enabled ? undefined : '#999' }}>
            {getDeptIcon(node.type)}
          </span>
          <span className={styles.nodeName}>{node.name}</span>
          {showUserCount && deptUserCount && deptUserCount[node.id] != null && (
            <span className={styles.userCount}>({deptUserCount[node.id]}人)</span>
          )}
        </div>
      ),
      children: node.children ? convertToTreeData(node.children) : undefined,
    }));
  };

  const handleSelect: TreeProps['onSelect'] = (keys) => {
    if (keys.length === 0) return;
    const deptId = keys[0] as string;
    onSelect(deptId);
  };

  return (
    <div className={styles.treeSelector}>
      <div className={styles.header}>
        <span className={styles.title}>{title}</span>
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
    </div>
  );
};

export default DeptTreeSelector;
