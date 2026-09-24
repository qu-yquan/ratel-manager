import { useState, useCallback, useEffect, useMemo } from 'react';
import type { MenuTreeNode } from '../types';

/** 递归获取所有节点 key */
function getAllKeys(nodes: MenuTreeNode[]): React.Key[] {
  const keys: React.Key[] = [];
  const traverse = (items: MenuTreeNode[]) => {
    for (const item of items) {
      keys.push(item.id);
      if (item.children) {
        traverse(item.children);
      }
    }
  };
  traverse(nodes);
  return keys;
}

/**
 * 菜单树搜索和展开逻辑
 */
export function useMenuTree(treeData: MenuTreeNode[]) {
  const [searchValue, setSearchValue] = useState('');
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>(() => getAllKeys(treeData));

  // 数据变化时同步展开所有节点
  useEffect(() => {
    setExpandedKeys(getAllKeys(treeData));
  }, [treeData]);

  /** 递归搜索过滤 */
  const filterTree = useCallback(
    (nodes: MenuTreeNode[], keyword: string): MenuTreeNode[] => {
      if (!keyword) return nodes;
      return nodes.reduce<MenuTreeNode[]>((result, node) => {
        const children = node.children
          ? filterTree(node.children, keyword)
          : [];
        const match = node.menuName.includes(keyword);
        if (match || children.length > 0) {
          result.push({
            ...node,
            children: children.length > 0 ? children : node.children,
          });
        }
        return result;
      }, []);
    },
    [],
  );

  const filteredTreeData = useMemo(
    () => filterTree(treeData, searchValue),
    [treeData, searchValue, filterTree],
  );

  /** 搜索时自动展开匹配节点的父级 */
  const handleSearch = useCallback(
    (value: string) => {
      setSearchValue(value);
      if (value) {
        setExpandedKeys(getAllKeys(treeData));
      }
    },
    [treeData],
  );

  return {
    searchValue,
    setSearchValue: handleSearch,
    expandedKeys,
    setExpandedKeys,
    filteredTreeData,
  };
}
