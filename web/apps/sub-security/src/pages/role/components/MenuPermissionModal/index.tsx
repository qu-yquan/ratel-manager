import React, { useState, useCallback, useEffect, useMemo, useRef } from 'react';
import { Modal, Form, App } from 'antd';
import styles from './index.module.less';
import ValidGroupList from './ValidGroupList';
import MenuTreePanel from './MenuTreePanel';
import { getValidGroups, getMenuTree, saveOrUpdateValidGroup, deleteValidGroup, getMenuOwnerOptions, getMenuPositionOptions } from '../../services/role';
import type { ValidGroup, MenuTreeNode, ValidGroupSaveRequest } from '../../types';
import { validGroupToFormValues, formValuesToValidFields } from './ValidConfigForm';

interface MenuPermissionModalProps {
  /** 弹窗是否可见 */
  visible: boolean;
  /** 当前角色 ID */
  roleId: string | null;
  /** 当前角色名称（用于弹窗标题） */
  roleName?: string;
  /** 关闭弹窗 */
  onClose: () => void;
}

/**
 * 菜单权限配置弹窗 - 主组件
 * 左侧时效分组列表，右侧菜单树面板
 */
const MenuPermissionModal: React.FC<MenuPermissionModalProps> = ({
  visible,
  roleId,
  roleName,
  onClose,
}) => {
  const { message } = App.useApp();
  /** 时效分组列表 */
  const [groups, setGroups] = useState<ValidGroup[]>([]);
  /** 当前选中的时效组 ID */
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);
  /** 菜单树全量数据 */
  const [menuTreeData, setMenuTreeData] = useState<MenuTreeNode[]>([]);
  /** 当前 Tab（MenuOwner） */
  const [currentOwner, setCurrentOwner] = useState<number>(1);
  /** 编辑模式 */
  const [editing, setEditing] = useState(false);
  /** 编辑模式下的勾选菜单 ID 列表 */
  const [checkedMenuIds, setCheckedMenuIds] = useState<string[]>([]);
  /** 是否新增模式（区别于编辑已有时效组） */
  const [isCreating, setIsCreating] = useState(false);
  /** 加载状态 */
  const [loading, setLoading] = useState(false);
  /** 保存中 */
  const [, setSaving] = useState(false);

  /** 菜单所属/位置枚举 */
  const [ownerOptions, setOwnerOptions] = useState<{ code: number; description: string }[]>([]);
  const [positionOptions, setPositionOptions] = useState<{ code: number; description: string }[]>([]);

  /** 时效配置表单 */
  const [validForm] = Form.useForm();

  /** 记住编辑前的状态，用于取消恢复 */
  const prevCheckedRef = useRef<string[]>([]);
  const prevGroupRef = useRef<ValidGroup | null>(null);

  /** 获取当前选中的时效组对象 */
  const selectedGroup = useMemo(() => {
    if (!selectedGroupId) return null;
    return groups.find((g) => g.roleMenuId === selectedGroupId) ?? null;
  }, [groups, selectedGroupId]);

  // ========== 数据加载 ==========

  /** 加载时效分组列表 */
  const loadGroups = useCallback(async () => {
    if (!roleId) return;
    try {
      const data = await getValidGroups(roleId);
      setGroups(data);
      // 默认选中第一个
      if (data.length > 0) {
        setSelectedGroupId(data[0].roleMenuId);
      }
    } catch {
      // request 层已自动提示
    }
  }, [roleId]);

  /** 加载菜单树 */
  const loadMenuTree = useCallback(async () => {
    if (!roleId) return;
    setLoading(true);
    try {
      const data = await getMenuTree(roleId);
      setMenuTreeData(data);
    } catch {
      // request 层已自动提示
    } finally {
      setLoading(false);
    }
  }, [roleId]);

  /** 弹窗打开时加载数据 */
  useEffect(() => {
    if (visible && roleId) {
      loadGroups();
      loadMenuTree();
      setEditing(false);
      setIsCreating(false);
      setCurrentOwner(1);
      // 加载菜单枚举
      getMenuOwnerOptions().then(setOwnerOptions);
      getMenuPositionOptions().then(setPositionOptions);
    }
  }, [visible, roleId, loadGroups, loadMenuTree]);

  /** 弹窗关闭时重置状态 */
  useEffect(() => {
    if (!visible) {
      setGroups([]);
      setMenuTreeData([]);
      setSelectedGroupId(null);
      setEditing(false);
      setIsCreating(false);
      setCheckedMenuIds([]);
      validForm.resetFields();
    }
  }, [visible, validForm]);

  // ========== 选中时效组 ==========

  const handleSelectGroup = useCallback(
    (roleMenuId: string) => {
      // 编辑模式下不允许切换
      if (editing) return;
      setSelectedGroupId(roleMenuId);
      setCurrentOwner(1);
    },
    [editing],
  );

  // ========== 编辑模式 ==========

  /** 进入编辑模式（编辑已有时效组） */
  const handleEdit = useCallback(() => {
    if (!selectedGroup) return;
    setEditing(true);
    setIsCreating(false);
    // 保存当前状态用于取消恢复
    prevCheckedRef.current = selectedGroup.menuIds;
    prevGroupRef.current = selectedGroup;
    // 设置勾选状态为当前组的菜单 ID
    setCheckedMenuIds(selectedGroup.menuIds);
    // 设置表单初始值
    validForm.setFieldsValue(validGroupToFormValues(selectedGroup));
  }, [selectedGroup, validForm]);

  /** 保存编辑 */
  const handleSave = useCallback(async () => {
    if (!roleId) return;

    try {
      const formValues = await validForm.validateFields();
      const validFields = formValuesToValidFields(formValues);

      const requestData: ValidGroupSaveRequest = {
        roleMenuId: isCreating ? undefined : selectedGroupId ?? undefined,
        roleId,
        ...validFields,
        menuIds: checkedMenuIds,
      };

      setSaving(true);
      await saveOrUpdateValidGroup(requestData);
      message.success(isCreating ? '新增时效组成功' : '编辑时效组成功');
      setEditing(false);
      setIsCreating(false);
      // 重新加载数据
      await Promise.all([loadGroups(), loadMenuTree()]);
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setSaving(false);
    }
  }, [roleId, isCreating, selectedGroupId, checkedMenuIds, validForm, loadGroups, loadMenuTree]);

  /** 取消编辑 */
  const handleCancelEdit = useCallback(() => {
    setEditing(false);
    setIsCreating(false);
    // 恢复勾选状态
    if (prevGroupRef.current) {
      setCheckedMenuIds(prevCheckedRef.current);
    }
    validForm.resetFields();
  }, [validForm]);

  // ========== 新增时效组 ==========

  const handleAddGroup = useCallback(() => {
    setEditing(true);
    setIsCreating(true);
    // 新增时清空选中组，使用临时 ID 标识
    setSelectedGroupId('__new__');
    setCheckedMenuIds([]);
    // 表单设置默认值
    validForm.resetFields();
    validForm.setFieldsValue({
      validType: 1,
      cycleType: 1,
      cycleValue: [],
    });
  }, [validForm]);

  // ========== 删除时效组 ==========

  const handleDeleteGroup = useCallback(
    async (roleMenuId: string) => {
      try {
        await deleteValidGroup(roleMenuId);
        message.success('删除时效组成功');
        // 如果删除的是当前选中组，清除选中
        if (selectedGroupId === roleMenuId) {
          setSelectedGroupId(null);
        }
        // 重新加载数据
        await Promise.all([loadGroups(), loadMenuTree()]);
      } catch {
        // request 层已自动提示
      }
    },
    [selectedGroupId, loadGroups, loadMenuTree],
  );

  /** 右侧面板中删除当前时效组 */
  const handleDeleteCurrentGroup = useCallback(() => {
    if (selectedGroupId && selectedGroupId !== '__new__') {
      handleDeleteGroup(selectedGroupId);
    }
  }, [selectedGroupId, handleDeleteGroup]);

  // ========== 勾选变更 ==========

  const handleCheckedChange = useCallback((checkedIds: string[]) => {
    setCheckedMenuIds(checkedIds);
  }, []);

  // ========== Tab 切换 ==========

  const handleOwnerChange = useCallback(
    (owner: number) => {
      setCurrentOwner(owner);
      // Tab 切换不清空勾选状态，勾选状态跨 Tab 保持
    },
    [],
  );

  // ========== 渲染 ==========

  /** 在新增模式下，需要模拟一个临时的 selectedGroup 用于右侧面板渲染 */
  const displayGroup = useMemo(() => {
    if (isCreating && selectedGroupId === '__new__') {
      return {
        roleMenuId: '__new__',
        menuId: '',
        validType: 1,
        menuCount: checkedMenuIds.length,
        menuIds: checkedMenuIds,
      } as ValidGroup;
    }
    return selectedGroup;
  }, [isCreating, selectedGroupId, checkedMenuIds, selectedGroup]);

  return (
    <Modal
      title={`菜单权限配置 - ${roleName ?? ''}`}
      open={visible}
      onCancel={editing ? handleCancelEdit : onClose}
      width={1120}
      className={styles.modal}
      footer={null}
      destroyOnHidden
      closable={!editing}
    >
      <div className={styles.container}>
        {/* 左侧 - 时效分组列表 */}
        <ValidGroupList
          groups={groups}
          selectedId={selectedGroupId === '__new__' ? null : selectedGroupId}
          onSelect={handleSelectGroup}
          onAdd={handleAddGroup}
          onDelete={handleDeleteGroup}
          editing={editing}
        />

        {/* 右侧 - 菜单树面板 */}
        <MenuTreePanel
          selectedGroup={displayGroup}
          menuTreeData={menuTreeData}
          currentOwner={currentOwner}
          onOwnerChange={handleOwnerChange}
          checkedMenuIds={checkedMenuIds}
          onCheckedChange={handleCheckedChange}
          editing={editing}
          onEdit={handleEdit}
          onSave={handleSave}
          onCancelEdit={handleCancelEdit}
          onDeleteGroup={handleDeleteCurrentGroup}
          loading={loading}
          validForm={validForm}
          ownerOptions={ownerOptions}
          positionOptions={positionOptions}
        />
      </div>
    </Modal>
  );
};

export default MenuPermissionModal;
