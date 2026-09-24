import React from 'react';
import { Input, Button, Tree, Spin, Space } from 'antd';
import type { TreeProps } from 'antd';
import {
  SearchOutlined,
  PlusOutlined,
  FolderOutlined,
  FileOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import { useMenuTree } from '../../hooks/useMenuTree';
import type { EnumOption, MenuTreeNode } from '../../types';
import {AuthGate} from '@gwsu/core'
import { PERM_ADD } from '../../permissionConstants';

interface MenuTreeProps {
  treeData: MenuTreeNode[];
  loading: boolean;
  selectedKey: string | null;
  positions: EnumOption[];
  currentPosition: number;
  onPositionChange: (position: number) => void;
  onSelect: (menuId: string, menu: MenuTreeNode) => void;
  onCreateDirectory: () => void;
  onCreateMenu: () => void;
  onCreateChild: (parentId: string, parentType: number) => void;
}

const MenuTree: React.FC<MenuTreeProps> = ({
  treeData,
  loading,
  selectedKey,
  positions,
  currentPosition,
  onPositionChange,
  onSelect,
  onCreateDirectory,
  onCreateMenu,
  onCreateChild,
}) => {
  const { searchValue, setSearchValue, expandedKeys, setExpandedKeys, filteredTreeData } =
    useMenuTree(treeData);

  const convertToTreeData = (data: MenuTreeNode[]): TreeProps['treeData'] => {
    return data.map((node) => ({
      key: node.id,
      title: (
        <div className={styles.treeNode}>
          <span className={styles.nodeIcon}>
            {node.menuType === 1 ? <FolderOutlined /> : <FileOutlined />}
          </span>
          <span className={`${styles.nodeName} ${!node.status ? styles.disabledNode : ''}`}>
            {node.menuName}
          </span>
          {node.menuType === 1 && (
            <div className={styles.nodeActions}>
              <Button
                type="text"
                size="small"
                icon={<PlusOutlined />}
                className={styles.actionBtn}
                onClick={(e) => {
                  e.stopPropagation();
                  onCreateChild(node.id, node.menuType);
                }}
              />
            </div>
          )}
        </div>
      ),
      children: node.children ? convertToTreeData(node.children) : undefined,
    }));
  };

  const handleSelect: TreeProps['onSelect'] = (keys) => {
    if (keys.length === 0) return;
    const nodeId = keys[0] as string;
    const findNode = (nodes: MenuTreeNode[], id: string): MenuTreeNode | null => {
      for (const n of nodes) {
        if (n.id === id) return n;
        if (n.children) {
          const found = findNode(n.children, id);
          if (found) return found;
        }
      }
      return null;
    };
    const menu = findNode(treeData, nodeId);
    if (menu) {
      onSelect(nodeId, menu);
    }
  };

  return (
    <div className={styles.menuTreePanel}>
      <div className={styles.header}>
        <span className={styles.title}>菜单管理</span>
      </div>
      <div className={styles.positionBar}>
        {positions.map((p, idx) => (
          <Button
            key={p.code}
            type={currentPosition === p.code ? "primary" : "default"}
            size="small"
            onClick={() => onPositionChange(p.code)}
            style={{
              borderRadius:
                idx === 0
                  ? "6px 0 0 6px"
                  : idx === positions.length - 1
                  ? "0 6px 6px 0"
                  : 0,
            }}
          >
            {p.description}
          </Button>
        ))}
      </div>
      <div className={styles.searchWrapper}>
        <Input
          placeholder="搜索菜单"
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
            blockNode
            treeData={convertToTreeData(filteredTreeData)}
            selectedKeys={selectedKey ? [selectedKey] : []}
            expandedKeys={expandedKeys}
            onExpand={setExpandedKeys}
            onSelect={handleSelect}
          />
        </div>
      </Spin>
      <div className={styles.footer}>
        <Space>
          <AuthGate buttonKey={PERM_ADD}>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={onCreateDirectory}
            >
              新增目录
            </Button>
            <Button icon={<PlusOutlined />} onClick={onCreateMenu}>
              新增菜单
            </Button>
          </AuthGate>
        </Space>
      </div>
    </div>
  );
};

export default MenuTree;
