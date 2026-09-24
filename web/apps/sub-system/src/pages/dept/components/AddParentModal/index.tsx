import React, { useEffect, useState } from 'react';
import { Modal, Tree, App } from 'antd';
import type { DataNode } from 'antd/es/tree';
import { addParentDept, getDeptTree } from '@/services/dept';
import type { DeptTreeNode } from '../../types';

interface AddParentModalProps {
  visible: boolean;
  deptId: string;
  currentParentIds: string[];
  onClose: () => void;
  onSuccess: () => void;
}

const AddParentModal: React.FC<AddParentModalProps> = ({
  visible,
  deptId,
  currentParentIds,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [treeData, setTreeData] = useState<DeptTreeNode[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [confirmLoading, setConfirmLoading] = useState(false);

  useEffect(() => {
    if (visible) {
      setLoading(true);
      getDeptTree()
        .then((data) => {
          setTreeData(data);
        })
        .catch(() => {
          void message.error('加载部门树失败');
        })
        .finally(() => {
          setLoading(false);
        });
    }
  }, [visible]);

  const handleOk = async () => {
    if (!selectedId) {
      message.warning('请选择要添加的父部门');
      return;
    }
    setConfirmLoading(true);
    try {
      await addParentDept(deptId, selectedId);
      message.success('添加成功');
      onSuccess();
    } catch {
      message.error('添加失败');
    } finally {
      setConfirmLoading(false);
    }
  };

  const convertToTreeNodes = (nodes: DeptTreeNode[]): DataNode[] => {
    return nodes.map((node) => {
      const disabled = node.id === deptId || currentParentIds.includes(node.id);
      return {
        key: node.id,
        title: node.name,
        disabled,
        children: node.children ? convertToTreeNodes(node.children) : undefined,
      };
    });
  };

  const handleSelect = (selectedKeys: React.Key[]) => {
    setSelectedId(selectedKeys.length > 0 ? String(selectedKeys[0]) : null);
  };

  return (
    <Modal
      title="添加父部门"
      open={visible}
      onCancel={onClose}
      onOk={handleOk}
      confirmLoading={confirmLoading}
      width={400}
      okButtonProps={{ "data-ai-approval": "true" }}
      destroyOnHidden
    >
      {loading ? (
        <div style={{ padding: 20, textAlign: "center" }}>加载中...</div>
      ) : (
        <Tree
          treeData={convertToTreeNodes(treeData)}
          onSelect={handleSelect}
          selectedKeys={selectedId ? [selectedId] : []}
          showLine
          style={{ maxHeight: 400, overflowY: "auto" }}
        />
      )}
    </Modal>
  );
};

export default AddParentModal;
