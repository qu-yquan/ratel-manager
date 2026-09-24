import React, { useMemo, useCallback } from 'react';
import {
  Tabs,
  Tree,
  Tag,
  Button,
  Spin,
  Form,
  Popconfirm,
} from 'antd';
import type { TreeProps } from 'antd';
import {
  MenuOutlined,
  EditOutlined,
  SaveOutlined,
  CloseOutlined,
  DeleteOutlined,
  FolderOutlined,
  FileOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import styles from './index.module.less';
import type { MenuTreeNode, ValidGroup } from '../../types';
import { getValidGroupLabel } from './ValidConfigForm';
import ValidConfigForm from './ValidConfigForm';

/** MenuPosition Tag 颜色映射（颜色是前端展示逻辑，不属于后端枚举） */
const POSITION_TAG_COLORS: Record<number, string> = {
  1: 'blue',
  2: 'orange',
};

interface MenuTreePanelProps {
  /** 当前选中的时效组 */
  selectedGroup: ValidGroup | null;
  /** 菜单树数据（按 owner 分类） */
  menuTreeData: MenuTreeNode[];
  /** 当前 Tab (MenuOwner) */
  currentOwner: number;
  /** 切换 Tab */
  onOwnerChange: (owner: number) => void;
  /** 已勾选的菜单 ID 列表（当前编辑中） */
  checkedMenuIds: string[];
  /** 勾选变更 */
  onCheckedChange: (checkedIds: string[]) => void;
  /** 是否编辑模式 */
  editing: boolean;
  /** 进入编辑模式 */
  onEdit: () => void;
  /** 保存编辑 */
  onSave: () => void;
  /** 取消编辑 */
  onCancelEdit: () => void;
  /** 删除当前时效组 */
  onDeleteGroup: () => void;
  /** 加载中 */
  loading: boolean;
  /** 时效配置表单实例 */
  validForm: ReturnType<typeof Form.useForm>[0];
  /** 菜单所属类型枚举（从后端获取） */
  ownerOptions: { code: number; description: string }[];
  /** 菜单位置类型枚举（从后端获取） */
  positionOptions: { code: number; description: string }[];
}

/** 右侧 - 菜单树面板组件 */
const MenuTreePanel: React.FC<MenuTreePanelProps> = ({
  selectedGroup,
  menuTreeData,
  currentOwner,
  onOwnerChange,
  checkedMenuIds,
  onCheckedChange,
  editing,
  onEdit,
  onSave,
  onCancelEdit,
  onDeleteGroup,
  loading,
  validForm,
  ownerOptions,
  positionOptions,
}) => {
  /** 按当前 owner 过滤菜单树 */
  const filteredTreeData = useMemo(() => {
    const filterByOwner = (nodes: MenuTreeNode[]): MenuTreeNode[] => {
      return nodes
        .filter((node) => node.owner === currentOwner || node.owner === undefined)
        .map((node) => ({
          ...node,
          children: node.children ? filterByOwner(node.children) : undefined,
        }))
        .filter((node) => {
          if (node.children && node.children.length === 0) {
            return node.owner === currentOwner || node.owner === undefined;
          }
          return true;
        });
    };
    return filterByOwner(menuTreeData);
  }, [menuTreeData, currentOwner]);

  /** 收集所有已被任何时效组关联的菜单 ID（用于非编辑模式下显示对勾标记） */
  const allBoundMenuIds = useMemo(() => {
    const ids = new Set<string>();
    const collect = (nodes: MenuTreeNode[]) => {
      for (const node of nodes) {
        if (node.boundRoleMenuId) {
          ids.add(node.id);
        }
        if (node.children) {
          collect(node.children);
        }
      }
    };
    collect(menuTreeData);
    return ids;
  }, [menuTreeData]);

  /** 收集当前选中时效组关联的菜单 ID（用于查看模式下回显勾选） */
  const currentGroupMenuIds = useMemo(() => {
    if (!selectedGroup || editing) return new Set<string>();
    return new Set(selectedGroup.menuIds ?? []);
  }, [selectedGroup, editing]);

  /** 构建 子节点ID -> 父节点ID 的映射（基于过滤后的菜单树，包含按钮节点） */
  const parentMap = useMemo(() => {
    const map = new Map<string, string>();
    const buildMap = (nodes: MenuTreeNode[], parentId?: string) => {
      for (const node of nodes) {
        if (parentId) {
          map.set(node.id, parentId);
        }
        if (node.children) {
          buildMap(node.children, node.id);
        }
      }
    };
    buildMap(filteredTreeData);
    return map;
  }, [filteredTreeData]);

  /** 提取每个菜单/目录节点的按钮子节点（menuType === 3），用于内联 Tag 渲染 */
  const buttonMap = useMemo(() => {
    const map = new Map<string, MenuTreeNode[]>();
    const collect = (nodes: MenuTreeNode[]) => {
      for (const node of nodes) {
        if (node.children) {
          const buttons = node.children.filter((c) => c.menuType === 3);
          if (buttons.length > 0) {
            map.set(node.id, buttons);
          }
          collect(node.children);
        }
      }
    };
    collect(filteredTreeData);
    return map;
  }, [filteredTreeData]);

  /** 判断节点是否已被勾选（编辑模式用 checkedMenuIds，查看模式用 currentGroupMenuIds） */
  const isChecked = useCallback(
    (id: string) => {
      if (editing) {
        return checkedMenuIds.includes(id);
      }
      return currentGroupMenuIds.has(id);
    },
    [editing, checkedMenuIds, currentGroupMenuIds],
  );

  /** 处理按钮 Tag 点击：切换勾选状态，并自动补选父节点 */
  const handleButtonTagClick = useCallback(
    (btnId: string, parentId: string) => {
      if (!editing) return;
      const newChecked = new Set(checkedMenuIds);
      if (newChecked.has(btnId)) {
        newChecked.delete(btnId);
      } else {
        newChecked.add(btnId);
        // 补选父节点
        newChecked.add(parentId);
        let ancestor = parentMap.get(parentId);
        while (ancestor) {
          newChecked.add(ancestor);
          ancestor = parentMap.get(ancestor);
        }
      }
      onCheckedChange(Array.from(newChecked));
    },
    [editing, checkedMenuIds, onCheckedChange, parentMap],
  );

  /** 将 MenuTreeNode 转换为 Ant Design Tree 所需的 treeData 格式
   *  按钮节点(menuType === 3)从 children 中剥离，以 Tag 形式内联到父节点 title 中
   *  目录(1)和菜单(2)保留为树节点 */
  const treeData = useMemo<TreeProps['treeData']>(() => {
    const convert = (nodes: MenuTreeNode[]): TreeProps['treeData'] => {
      return nodes
        .filter((node) => node.menuType !== 3)
        .map((node) => {
          const positionEnum = positionOptions.find((p) => p.code === node.position);
          const isBoundByOther = allBoundMenuIds.has(node.id) &&
            (!selectedGroup || !currentGroupMenuIds.has(node.id));
          const buttons = buttonMap.get(node.id) ?? [];

          return {
            key: node.id,
            title: (
              <div className={styles.treeNodeRow}>
                <div className={styles.treeNode}>
                  {isBoundByOther && (
                    <CheckCircleOutlined className={styles.boundIcon} />
                  )}
                  <span>{node.menuType === 1 ? <FolderOutlined /> : <FileOutlined />}</span>
                  <span className={styles.nodeName}>{node.menuName}</span>
                  {positionEnum && (
                    <Tag color={POSITION_TAG_COLORS[node.position ?? 0] ?? 'default'} className={styles.positionTag}>
                      {positionEnum.description}
                    </Tag>
                  )}
                </div>
                {buttons.length > 0 && (
                  <div className={styles.buttonTags}>
                    {buttons.map((btn) => {
                      const checked = isChecked(btn.id);
                      const boundByOther = allBoundMenuIds.has(btn.id) &&
                        (!selectedGroup || !currentGroupMenuIds.has(btn.id));
                      return (
                        <Tag
                          key={btn.id}
                          role="checkbox"
                          aria-checked={checked}
                          className={`${styles.buttonTag} ${checked ? styles.buttonTagChecked : ''}`}
                          color={checked ? 'blue' : undefined}
                          onClick={(e) => {
                            e.stopPropagation();
                            handleButtonTagClick(btn.id, node.id);
                          }}
                          style={{ cursor: editing ? 'pointer' : 'default' }}
                        >
                          {boundByOther && !checked && (
                            <CheckCircleOutlined style={{ fontSize: 10, marginRight: 2, color: '#52c41a' }} />
                          )}
                          {btn.menuName}
                        </Tag>
                      );
                    })}
                  </div>
                )}
              </div>
            ),
            children: node.children ? convert(node.children) : undefined,
          };
        });
    };
    return convert(filteredTreeData);
  }, [filteredTreeData, allBoundMenuIds, currentGroupMenuIds, selectedGroup, positionOptions, buttonMap, isChecked, handleButtonTagClick]);

  /** 收集所有非按钮节点的 ID，用于受控展开 */
  const expandedKeys = useMemo(() => {
    const keys: string[] = [];
    const collect = (nodes: MenuTreeNode[]) => {
      for (const node of nodes) {
        if (node.menuType !== 3) {
          keys.push(node.id);
        }
        if (node.children) {
          collect(node.children);
        }
      }
    };
    collect(filteredTreeData);
    return keys;
  }, [filteredTreeData]);

  /** 查看模式下的勾选 key（仅当前选中时效组的菜单） */
  const displayCheckedKeys = useMemo(() => {
    if (editing || !selectedGroup) return [];
    return selectedGroup.menuIds ?? [];
  }, [editing, selectedGroup]);

  /** 处理勾选变更 - checkStrictly 模式下：
   *  选中父节点不选中子节点，选中子节点自动补选父节点
   *  取消勾选菜单节点时，同步取消其下所有按钮节点的勾选 */
  const handleCheck: TreeProps['onCheck'] = useCallback(
    (checkedKeys: React.Key[] | { checked: React.Key[]; halfChecked: React.Key[] }) => {
      const keys = Array.isArray(checkedKeys) ? checkedKeys : checkedKeys.checked;
      const newChecked = new Set(keys as string[]);
      const prevChecked = new Set(checkedMenuIds);

      // 找出新增勾选的节点
      const addedKeys = keys.filter((k) => !prevChecked.has(k as string)) as string[];

      // 对新增的节点，向上补选所有祖先节点
      for (const key of addedKeys) {
        let current = parentMap.get(key);
        while (current) {
          newChecked.add(current);
          current = parentMap.get(current);
        }
      }

      // 找出被取消勾选的节点，如果是菜单节点则同步取消其下所有按钮
      const removedKeys = checkedMenuIds.filter((k) => !newChecked.has(k));
      for (const key of removedKeys) {
        const buttons = buttonMap.get(key);
        if (buttons) {
          for (const btn of buttons) {
            newChecked.delete(btn.id);
          }
        }
      }

      onCheckedChange(Array.from(newChecked));
    },
    [onCheckedChange, checkedMenuIds, parentMap, buttonMap],
  );

  /** 没有选中时效组时的空状态 */
  if (!selectedGroup) {
    return (
      <div className={styles.rightPanel}>
        <div className={styles.rightEmpty}>
          <MenuOutlined className={styles.rightEmptyIcon} />
          <span className={styles.rightEmptyText}>请选择左侧时效分组查看菜单权限</span>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.rightPanel}>
      {/* 顶部 Tab + 操作按钮 */}
      <div className={styles.rightHeader}>
        <Tabs
          className={styles.ownerTabs}
          activeKey={String(currentOwner)}
          onChange={(key) => onOwnerChange(Number(key))}
          items={ownerOptions.map((opt) => ({
            key: String(opt.code),
            label: opt.description,
          }))}
        />
        <div className={styles.headerActions}>
          {editing ? (
            <>
              <Button
                size="small"
                icon={<SaveOutlined />}
                type="primary"
                data-ai-approval
                onClick={onSave}
              >
                保存
              </Button>
              <Button
                size="small"
                icon={<CloseOutlined />}
                onClick={onCancelEdit}
              >
                取消
              </Button>
            </>
          ) : (
            <>
              <Button size="small" icon={<EditOutlined />} onClick={onEdit}>
                编辑权限
              </Button>
              <Popconfirm
                title="确定删除此时效组？"
                description="删除后关联的菜单将自动释放"
                onConfirm={onDeleteGroup}
                okText="确定"
                cancelText="取消"
              >
                <Button
                  size="small"
                  danger
                  data-ai-approval
                  icon={<DeleteOutlined />}
                >
                  删除此组
                </Button>
              </Popconfirm>
            </>
          )}
        </div>
      </div>

      {/* 时效信息区 */}
      <div className={styles.validInfoSection}>
        <div className={styles.validInfoHeader}>
          <span className={styles.validInfoTitle}>时效配置</span>
        </div>
        {editing ? (
          <ValidConfigForm form={validForm} initialData={selectedGroup} />
        ) : (
          <div className={styles.validInfoContent}>
            <Tag className={styles.validInfoTag} color="blue">
              {getValidGroupLabel(selectedGroup)}
            </Tag>
            <span className={styles.validInfoText}>
              关联 {selectedGroup.menuCount} 个菜单/按钮
            </span>
          </div>
        )}
      </div>

      {/* 菜单树 */}
      <Spin spinning={loading} className={styles.treeSpinWrapper}>
        <div className={styles.treeSection}>
          <Tree
            checkable
            checkStrictly
            checkedKeys={editing ? checkedMenuIds : displayCheckedKeys}
            onCheck={editing ? handleCheck : undefined}
            treeData={treeData}
            expandedKeys={expandedKeys}
            selectable={false}
            blockNode
          />
        </div>
      </Spin>
    </div>
  );
};

export default MenuTreePanel;
