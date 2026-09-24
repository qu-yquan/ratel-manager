import React, { useState, useCallback, useEffect, useRef } from 'react';
import { Tabs } from 'antd';
import { MenuOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import MenuTree from './components/MenuTree';
import MenuDetail from './components/MenuDetail';
import MenuFormModal from './components/MenuFormModal';
import { getMenuOwners, getMenuPositions, getMenuTree } from './services/menu';
import type { EnumOption, MenuTreeNode } from './types';

/** 递归查找树节点 */
const findNodeInTree = (
  nodes: MenuTreeNode[],
  id: string,
): MenuTreeNode | null => {
  for (const node of nodes) {
    if (node.id === id) return node;
    if (node.children) {
      const found = findNodeInTree(node.children, id);
      if (found) return found;
    }
  }
  return null;
};

const MenuPage: React.FC = () => {
  const [owners, setOwners] = useState<EnumOption[]>([]);
  const [positions, setPositions] = useState<EnumOption[]>([]);
  const [currentOwner, setCurrentOwner] = useState<number>(1);
  const [currentPosition, setCurrentPosition] = useState<number>(1);
  const [treeData, setTreeData] = useState<MenuTreeNode[]>([]);
  const [treeLoading, setTreeLoading] = useState(false);
  const [selectedMenu, setSelectedMenu] = useState<MenuTreeNode | null>(null);
  const [selectedMenuId, setSelectedMenuId] = useState<string | null>(null);

  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalData, setFormModalData] = useState<{
    mode: 'create' | 'edit';
    menuType: number;
    parentId?: string | null;
    data?: MenuTreeNode | null;
  }>({ mode: 'create', menuType: 1 });

  const [treeWidth, setTreeWidth] = useState(280);
  const isResizing = useRef(false);
  const startX = useRef(0);
  const startWidth = useRef(280);

  // 加载枚举
  useEffect(() => {
    const loadEnums = async () => {
      try {
        const [ownerList, positionList] = await Promise.all([
          getMenuOwners(),
          getMenuPositions(),
        ]);
        setOwners(ownerList);
        setPositions(positionList);
        if (ownerList.length > 0) setCurrentOwner(ownerList[0].code);
        if (positionList.length > 0) setCurrentPosition(positionList[0].code);
      } catch {
        // request 层已自动提示
      }
    };
    loadEnums();
  }, []);

  // 加载菜单树
  const loadTreeData = useCallback(async () => {
    setTreeLoading(true);
    try {
      const tree = await getMenuTree(currentOwner, currentPosition);
      setTreeData(tree);
      // 刷新树后同步更新已选菜单的详情
      setSelectedMenuId((prevId) => {
        if (prevId) {
          const updated = findNodeInTree(tree, prevId);
          if (updated) {
            setSelectedMenu(updated);
          }
        }
        return prevId;
      });
    } catch {
      // request 层已自动提示
    } finally {
      setTreeLoading(false);
    }
  }, [currentOwner, currentPosition]);

  useEffect(() => {
    loadTreeData().catch(() => {});
  }, [loadTreeData]);

  // 切换 owner/position 时清空选中
  useEffect(() => {
    setSelectedMenu(null);
    setSelectedMenuId(null);
  }, [currentOwner, currentPosition]);

  const handleSelectMenu = useCallback((menuId: string, menu: MenuTreeNode) => {
    setSelectedMenuId(menuId);
    setSelectedMenu(menu);
  }, []);

  const handleCreateDirectory = useCallback(() => {
    // 选中目录 → 父菜单默认为该目录；选中菜单 → 父菜单默认为菜单的父级；未选中 → 顶级
    let defaultParentId: string | null = null;
    if (selectedMenu) {
      defaultParentId = selectedMenu.menuType === 1
        ? selectedMenu.id
        : (selectedMenu.parentId === '0' ? null : selectedMenu.parentId);
    }
    setFormModalData({ mode: 'create', menuType: 1, parentId: defaultParentId });
    setFormModalVisible(true);
  }, [selectedMenu]);

  const handleCreateMenu = useCallback(() => {
    let defaultParentId: string | null = null;
    if (selectedMenu) {
      defaultParentId = selectedMenu.menuType === 1
        ? selectedMenu.id
        : (selectedMenu.parentId === '0' ? null : selectedMenu.parentId);
    }
    setFormModalData({ mode: 'create', menuType: 2, parentId: defaultParentId });
    setFormModalVisible(true);
  }, [selectedMenu]);

  const handleCreateChild = useCallback((parentId: string, _parentType: number) => {
    setFormModalData({
      mode: 'create',
      menuType: 2,
      parentId,
    });
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((menu: MenuTreeNode) => {
    setFormModalData({
      mode: 'edit',
      menuType: menu.menuType,
      data: menu,
    });
    setFormModalVisible(true);
  }, []);

  const handleDeleteSuccess = useCallback(() => {
    setSelectedMenu(null);
    setSelectedMenuId(null);
    loadTreeData().catch(() => {});
  }, [loadTreeData]);

  const handleFormSuccess = useCallback(() => {
    setFormModalVisible(false);
    loadTreeData().catch(() => {});
  }, [loadTreeData]);

  // 拖拽调整宽度
  const handleMouseDown = useCallback(
    (e: React.MouseEvent) => {
      isResizing.current = true;
      startX.current = e.clientX;
      startWidth.current = treeWidth;
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
    },
    [treeWidth],
  );

  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing.current) return;
    const diff = e.clientX - startX.current;
    const newWidth = Math.max(200, Math.min(400, startWidth.current + diff));
    setTreeWidth(newWidth);
  }, []);

  const handleMouseUp = useCallback(() => {
    isResizing.current = false;
    document.removeEventListener('mousemove', handleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
  }, [handleMouseMove]);

  return (
    <div className={styles.menuPage}>
      {/* 顶部：终端 Tab */}
      <div className={styles.topBar}>
        <Tabs
          className={styles.ownerTabs}
          activeKey={String(currentOwner)}
          onChange={(key) => setCurrentOwner(Number(key))}
          items={owners.map((o) => ({
            key: String(o.code),
            label: o.description,
          }))}
        />
      </div>

      {/* 主体区域 */}
      <div className={styles.mainContent}>
        <div className={styles.treePanel} style={{ width: treeWidth }}>
          <MenuTree
            treeData={treeData}
            loading={treeLoading}
            selectedKey={selectedMenuId}
            positions={positions}
            currentPosition={currentPosition}
            onPositionChange={setCurrentPosition}
            onSelect={handleSelectMenu}
            onCreateDirectory={handleCreateDirectory}
            onCreateMenu={handleCreateMenu}
            onCreateChild={handleCreateChild}
          />
        </div>
        <div className={styles.resizeHandle} onMouseDown={handleMouseDown} />
        <div className={styles.detailPanel}>
          {selectedMenu ? (
            <MenuDetail
              menu={selectedMenu}
              owner={currentOwner}
              position={currentPosition}
              onEdit={handleEdit}
              onDeleteSuccess={handleDeleteSuccess}
              onRefresh={loadTreeData}
            />
          ) : (
            <div className={styles.emptyState}>
              <MenuOutlined className={styles.emptyStateIcon} />
              <span className={styles.emptyStateText}>请选择菜单查看详情</span>
            </div>
          )}
        </div>
      </div>

      {/* 菜单新增/编辑弹窗 */}
      <MenuFormModal
        visible={formModalVisible}
        mode={formModalData.mode}
        menuType={formModalData.menuType}
        owner={currentOwner}
        position={currentPosition}
        parentId={formModalData.parentId}
        data={formModalData.data}
        onClose={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
};

export default MenuPage;
