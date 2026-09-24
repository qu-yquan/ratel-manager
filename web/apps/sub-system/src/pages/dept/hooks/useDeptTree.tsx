import React, { useState, useMemo } from 'react';
import {
  BankOutlined,
  ShopOutlined,
  ApartmentOutlined,
  TeamOutlined,
  UsergroupAddOutlined,
} from '@ant-design/icons';
import type { DeptTreeNode } from '../types';

/**
 * 部门树公共逻辑 hook，提取搜索、展开、图标、过滤等共享功能
 */
export function useDeptTree(treeData: DeptTreeNode[]) {
  const [searchValue, setSearchValue] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  // 树数据加载后默认展开所有节点
  useMemo(() => {
    const collectKeys = (nodes: DeptTreeNode[]): React.Key[] => {
      const keys: React.Key[] = [];
      const walk = (items: DeptTreeNode[]) => {
        for (const item of items) {
          if (item.children?.length) {
            keys.push(item.id);
            walk(item.children);
          }
        }
      };
      walk(nodes);
      return keys;
    };
    setExpandedKeys(collectKeys(treeData));
  }, [treeData]);

  const getDeptIcon = (type: number) => {
    const iconMap: Record<number, React.ReactNode> = {
      1: <BankOutlined />,
      2: <ShopOutlined />,
      3: <ApartmentOutlined />,
      4: <TeamOutlined />,
      5: <UsergroupAddOutlined />,
    };
    return iconMap[type] || <ApartmentOutlined />;
  };

  const filterTree = (data: DeptTreeNode[], search: string): DeptTreeNode[] => {
    if (!search) return data;
    return data
      .map((node) => {
        const match = node.name.toLowerCase().includes(search.toLowerCase());
        const children = node.children ? filterTree(node.children, search) : [];
        if (match || children.length > 0) {
          return { ...node, children };
        }
        return null;
      })
      .filter((node): node is DeptTreeNode => node !== null);
  };

  const filteredTreeData = useMemo(
    () => filterTree(treeData, searchValue),
    [treeData, searchValue],
  );

  return {
    searchValue,
    setSearchValue,
    expandedKeys,
    setExpandedKeys,
    getDeptIcon,
    filterTree,
    filteredTreeData,
  };
}
