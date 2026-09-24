import React, { useState, useCallback, useEffect, useRef } from 'react';
import { App } from 'antd';
import { ApartmentOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import DeptTreePanel from './components/DeptTreePanel';
import DeptDetailPanel from './components/DeptDetailPanel';
import DeptFormModal from './components/DeptFormModal';
import { getDeptTree, getDeptTypes, getDeptDetail } from '@/services/dept';
import type { DeptTreeNode, DeptDetail, DeptTypeOption } from './types';

const DeptPage: React.FC = () => {
  const { message } = App.useApp();
  const [treeData, setTreeData] = useState<DeptTreeNode[]>([]);
  const [deptTypes, setDeptTypes] = useState<DeptTypeOption[]>([]);
  const [selectedDept, setSelectedDept] = useState<DeptDetail | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formModalData, setFormModalData] = useState<{
    mode: 'create' | 'edit';
    parentId?: number;
    dept?: DeptDetail;
  }>({ mode: 'create' });

  const [treeWidth, setTreeWidth] = useState(280);
  const isResizing = useRef(false);
  const startX = useRef(0);
  const startWidth = useRef(280);

  const loadTreeData = useCallback(async () => {
    setLoading(true);
    try {
      const [tree, types] = await Promise.all([getDeptTree(), getDeptTypes()]);
      setTreeData(tree);
      setDeptTypes(types);
      // 刷新树数据时同步刷新当前选中部门的详情
      if (selectedNodeId) {
        try {
          const detail = await getDeptDetail(String(selectedNodeId));
          setSelectedDept(detail);
        } catch {
          // 详情获取失败不影响树刷新
        }
      }
    } catch {
      message.error('加载部门树失败');
    } finally {
      setLoading(false);
    }
  }, [selectedNodeId]);

  useEffect(() => {
    void loadTreeData();
  }, [loadTreeData]);

  const handleSelectNode = useCallback((nodeId: number, dept: DeptDetail) => {
    setSelectedNodeId(nodeId);
    setSelectedDept(dept);
  }, []);

  const handleCreateRoot = useCallback(() => {
    setFormModalData({ mode: 'create' });
    setFormModalVisible(true);
  }, []);

  const handleCreateChild = useCallback((parentId: number) => {
    setFormModalData({ mode: 'create', parentId });
    setFormModalVisible(true);
  }, []);

  const handleEdit = useCallback((dept: DeptDetail) => {
    setFormModalData({ mode: 'edit', dept });
    setFormModalVisible(true);
  }, []);

  const handleFormSuccess = useCallback(() => {
    setFormModalVisible(false);
    void loadTreeData();
  }, [loadTreeData]);

  const handleDeleteSuccess = useCallback(() => {
    setSelectedDept(null);
    setSelectedNodeId(null);
    void loadTreeData();
  }, [loadTreeData]);

  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    isResizing.current = true;
    startX.current = e.clientX;
    startWidth.current = treeWidth;
    document.addEventListener('mousemove', handleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  }, [treeWidth]);

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
    <div className={styles.deptPage}>
      <div className={styles.treePanel} style={{ width: treeWidth }}>
        <DeptTreePanel
          treeData={treeData}
          loading={loading}
          selectedKey={selectedNodeId}
          onSelect={handleSelectNode}
          onCreateRoot={handleCreateRoot}
          onCreateChild={handleCreateChild}
        />
      </div>
      <div className={styles.resizeHandle} onMouseDown={handleMouseDown} />
      <div className={styles.detailPanel}>
        {selectedDept ? (
          <DeptDetailPanel
            dept={selectedDept}
            deptTypes={deptTypes}
            onEdit={() => handleEdit(selectedDept)}
            onDeleteSuccess={handleDeleteSuccess}
            onRefresh={loadTreeData}
          />
        ) : (
          <div className={styles.emptyState}>
            <ApartmentOutlined className={styles.icon} />
            <span className={styles.text}>请选择部门查看详情</span>
          </div>
        )}
      </div>
      <DeptFormModal
        visible={formModalVisible}
        mode={formModalData.mode}
        parentId={formModalData.parentId}
        dept={formModalData.dept}
        deptTypes={deptTypes}
        onClose={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
      />
    </div>
  );
};

export default DeptPage;
