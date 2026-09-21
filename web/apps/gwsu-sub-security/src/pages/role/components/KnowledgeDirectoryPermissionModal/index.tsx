import React, { useEffect, useMemo, useState } from 'react';
import { Alert, Modal, Tree, message } from 'antd';
import type { TreeDataNode } from 'antd';
import { getDirectoryTree, getRoleDirectoryIds, saveRoleDirectoryIds } from '../../../knowledge/services/knowledge';
import type { KnowledgeNode } from '../../../knowledge/types';
import styles from './index.module.less';

interface Props { open: boolean; roleCode?: string; roleName?: string; onClose: () => void }

function buildTree(nodes: KnowledgeNode[], parentId?: string): TreeDataNode[] {
  return nodes.filter((node) => (node.parentId ?? undefined) === parentId).map((node) => ({
    key: node.id, title: node.name, children: buildTree(nodes, node.id),
  }));
}

const ROOT_DIRECTORY_ID = 'ROOT';

const KnowledgeDirectoryPermissionModal: React.FC<Props> = ({ open, roleCode, roleName, onClose }) => {
  const [nodes, setNodes] = useState<KnowledgeNode[]>([]);
  const [checked, setChecked] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    if (!open || !roleCode) return;
    void Promise.all([getDirectoryTree(true), getRoleDirectoryIds(roleCode)]).then(([tree, ids]) => {
      setNodes(tree); setChecked(ids);
    });
  }, [open, roleCode]);
  const treeData = useMemo<TreeDataNode[]>(() => [{
    key: ROOT_DIRECTORY_ID,
    title: '根目录（含所有下级目录和文档）',
    children: buildTree(nodes),
  }], [nodes]);

  const save = async () => {
    if (!roleCode) return;
    setSaving(true);
    try {
      await saveRoleDirectoryIds(roleCode, checked);
      message.success('知识目录权限已保存');
      onClose();
    } finally { setSaving(false); }
  };

  return <Modal title={`${roleName ?? ''} · 知识目录权限`} open={open} onCancel={onClose} onOk={() => void save()}
    confirmLoading={saving} okButtonProps={{ 'data-ai-approval': 'true' }} destroyOnHidden>
    <Alert type="info" showIcon message="勾选目录后，该角色可访问目录及所有下级目录和文档；未勾选的内容默认不可见。" />
    <div className={styles.treeBox}><Tree key={nodes.length} checkable checkStrictly defaultExpandAll treeData={treeData} checkedKeys={checked}
      onCheck={(keys) => setChecked((Array.isArray(keys) ? keys : keys.checked).map(String))} /></div>
  </Modal>;
};

export default KnowledgeDirectoryPermissionModal;
