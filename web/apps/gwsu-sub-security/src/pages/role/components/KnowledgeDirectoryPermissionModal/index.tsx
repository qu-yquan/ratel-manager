import React, { useEffect, useMemo, useState } from "react";
import { Alert, Modal, Tabs, Tree, message } from "antd";
import type { TreeDataNode } from "antd";
import {
  getDirectoryTree,
  getRoleDirectoryGrants,
  saveRoleDirectoryGrants,
} from "../../../knowledge/services/knowledge";
import type {
  KnowledgeNode,
  KnowledgePermission,
} from "../../../knowledge/types";
import styles from "./index.module.less";

interface Props {
  open: boolean;
  roleCode?: string;
  roleName?: string;
  onClose: () => void;
}
type Checks = Record<KnowledgePermission, string[]>;
const ROOT = "ROOT";
const PERMISSIONS: KnowledgePermission[] = ["SEARCH", "UPLOAD", "MANAGE"];
const LABELS: Record<KnowledgePermission, string> = {
  SEARCH: "检索",
  UPLOAD: "上传",
  MANAGE: "管理",
};
const emptyChecks = (): Checks => ({ SEARCH: [], UPLOAD: [], MANAGE: [] });

function buildTree(nodes: KnowledgeNode[], parentId?: string): TreeDataNode[] {
  return nodes
    .filter((node) => (node.parentId ?? undefined) === parentId)
    .map((node) => ({
      key: node.id,
      title: node.name,
      children: buildTree(nodes, node.id),
    }));
}

function descendants(id: string, nodes: KnowledgeNode[]): string[] {
  return [
    id,
    ...nodes
      .filter((node) => (node.parentId ?? ROOT) === id)
      .flatMap((node) => descendants(node.id, nodes)),
  ];
}

function expand(ids: string[], nodes: KnowledgeNode[]): string[] {
  return [...new Set(ids.flatMap((id) => descendants(id, nodes)))];
}

function completeParents(ids: string[], nodes: KnowledgeNode[]): string[] {
  const selected = new Set(ids);
  const children = new Map<string, string[]>();
  for (const node of nodes) {
    const parent = node.parentId ?? ROOT;
    children.set(parent, [...(children.get(parent) ?? []), node.id]);
  }
  let changed: boolean;
  do {
    changed = false;
    for (const [parent, childIds] of children) {
      if (
        !selected.has(parent) &&
        childIds.length > 0 &&
        childIds.every((id) => selected.has(id))
      ) {
        selected.add(parent);
        changed = true;
      }
    }
  } while (changed);
  return [...selected];
}

function ancestors(id: string, nodes: KnowledgeNode[]): string[] {
  const parents = new Map(
    nodes.map((node) => [node.id, node.parentId ?? ROOT])
  );
  const result: string[] = [];
  let parent = id === ROOT ? undefined : parents.get(id);
  while (parent) {
    result.push(parent);
    parent = parent === ROOT ? undefined : parents.get(parent);
  }
  return result;
}

function halfChecked(ids: string[], nodes: KnowledgeNode[]): string[] {
  const selected = new Set(ids);
  return [...new Set(ids.flatMap((id) => ancestors(id, nodes)))].filter(
    (id) => !selected.has(id)
  );
}

function compress(ids: string[], nodes: KnowledgeNode[]): string[] {
  const selected = new Set(ids);
  const parents = new Map(
    nodes.map((node) => [node.id, node.parentId ?? ROOT])
  );
  return ids.filter((id) => {
    let parent = id === ROOT ? undefined : parents.get(id);
    while (parent) {
      if (selected.has(parent)) return false;
      parent = parent === ROOT ? undefined : parents.get(parent);
    }
    return true;
  });
}

const KnowledgeDirectoryPermissionModal: React.FC<Props> = ({
  open,
  roleCode,
  roleName,
  onClose,
}) => {
  const [nodes, setNodes] = useState<KnowledgeNode[]>([]);
  const [checked, setChecked] = useState<Checks>(emptyChecks);
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [saving, setSaving] = useState(false);
  useEffect(() => {
    if (!open || !roleCode) return;
    let active = true;
    setLoading(true);
    setLoadFailed(false);
    setNodes([]);
    setChecked(emptyChecks());
    void Promise.all([getDirectoryTree(true), getRoleDirectoryGrants(roleCode)])
      .then(([tree, grants]) => {
        if (!active) return;
        const next = emptyChecks();
        for (const type of PERMISSIONS)
          next[type] = expand(
            grants
              .filter((grant) => grant.permissionType === type)
              .map((grant) => grant.directoryId),
            tree
          );
        next.UPLOAD = [...new Set([...next.UPLOAD, ...next.MANAGE])];
        next.SEARCH = [...new Set([...next.SEARCH, ...next.UPLOAD])];
        setNodes(tree);
        setChecked(next);
      })
      .catch(() => {
        if (active) {
          setLoadFailed(true);
          message.error("加载知识文档权限失败");
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => {
      active = false;
    };
  }, [open, roleCode]);

  const treeData = useMemo<TreeDataNode[]>(
    () => [
      {
        key: ROOT,
        title: "根目录（含全部下级目录和文档）",
        children: buildTree(nodes),
      },
    ],
    [nodes]
  );

  const updateChecked = (
    type: KnowledgePermission,
    id: string,
    enabled: boolean
  ) => {
    setChecked((previous) => {
      const subtree = new Set(descendants(id, nodes));
      const selected = new Set(previous[type]);
      if (enabled) {
        subtree.forEach((key) => selected.add(key));
      } else {
        subtree.forEach((key) => selected.delete(key));
        ancestors(id, nodes).forEach((key) => selected.delete(key));
      }
      const keys = enabled
        ? completeParents([...selected], nodes)
        : [...selected];
      const next: Checks = { ...previous, [type]: keys };
      if (enabled && type === "MANAGE") {
        next.UPLOAD = [...new Set([...next.UPLOAD, ...keys])];
        next.SEARCH = [...new Set([...next.SEARCH, ...keys])];
      } else if (enabled && type === "UPLOAD") {
        next.SEARCH = [...new Set([...next.SEARCH, ...keys])];
      } else if (!enabled && type === "SEARCH") {
        const remaining = new Set(keys);
        next.UPLOAD = next.UPLOAD.filter((id) => remaining.has(id));
        next.MANAGE = next.MANAGE.filter((id) => remaining.has(id));
      } else if (!enabled && type === "UPLOAD") {
        const remaining = new Set(keys);
        next.MANAGE = next.MANAGE.filter((id) => remaining.has(id));
      }
      return next;
    });
  };

  const save = async () => {
    if (!roleCode) return;
    setSaving(true);
    try {
      await saveRoleDirectoryGrants(
        roleCode,
        PERMISSIONS.flatMap((type) =>
          compress(checked[type], nodes).map((directoryId) => ({
            directoryId,
            permissionType: type,
          }))
        )
      );
      message.success("知识文档权限已保存");
      onClose();
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      title={`${roleName ?? ""} · 知识文档权限`}
      open={open}
      onCancel={onClose}
      onOk={() => void save()}
      confirmLoading={saving}
      okButtonProps={{
        "data-ai-approval": "true",
        disabled: loading || loadFailed,
      }}
      destroyOnHidden
      width={720}
    >
      <Alert
        type="info"
        showIcon
        message="权限说明"
        description={
          <div className={styles.permissionDescriptions}>
            <div>
              <strong>检索权限：</strong>
              可以查看目录，并检索目录及其下级目录中的文档。
            </div>
            <div>
              <strong>上传权限：</strong>
              包含检索权限；可以上传文档，并编辑、删除自己上传的文档。
            </div>
            <div>
              <strong>管理权限：</strong>
              包含上传和检索权限；可以管理目录，以及编辑、删除目录中的全部文档。
            </div>
            <div>
              <strong>勾选规则：</strong>
              勾选目录会同时选中全部子目录；取消任一子目录后，父目录不再授权。
            </div>
          </div>
        }
      />
      <Tabs
        items={PERMISSIONS.map((type) => ({
          key: type,
          label: `${LABELS[type]}权限`,
          children: (
            <div className={styles.treeBox}>
              <Tree
                key={`${type}:${nodes.length}`}
                checkable
                checkStrictly
                disabled={loading || loadFailed}
                defaultExpandAll
                treeData={treeData}
                checkedKeys={{
                  checked: checked[type],
                  halfChecked: halfChecked(checked[type], nodes),
                }}
                onCheck={(_, info) =>
                  updateChecked(type, String(info.node.key), info.checked)
                }
              />
            </div>
          ),
        }))}
      />
    </Modal>
  );
};

export default KnowledgeDirectoryPermissionModal;
