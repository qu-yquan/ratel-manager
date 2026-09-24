import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Switch, TreeSelect, Select, App } from 'antd';
import styles from './index.module.less';
import { saveOrUpdateMenu, getMenuTree } from '../../services/menu';
import type { MenuTreeNode, MenuSaveRequest } from '../../types';
import IconPicker from '../IconPicker';
import MarkdownEditor from '@/components/MarkdownEditor';

const MENU_TYPE_OPTIONS = [
  { value: 1, label: '目录' },
  { value: 2, label: '菜单' },
];

interface MenuFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  menuType: number;
  owner: number;
  position: number;
  data?: MenuTreeNode | null;
  parentId?: string | null;
  onClose: () => void;
  onSuccess: () => void;
}

const MenuFormModal: React.FC<MenuFormModalProps> = ({
  visible,
  mode,
  menuType,
  owner,
  position,
  data,
  parentId,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<MenuTreeNode[]>([]);
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (visible) {
      getMenuTree(owner, position).then(setTreeData).catch(() => {});
      if (data) {
        form.setFieldsValue({
          menuName: data.menuName,
          menuType: data.menuType,
          parentId: data.parentId === '0' ? null : data.parentId,
          path: data.path,
          microApp: data.microApp,
          icon: data.icon,
          sort: data.sort,
          visible: data.visible,
          status: data.status,
          description: data.description,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          menuType,
          parentId: parentId || null,
          sort: 0,
          visible: true,
          status: true,
        });
      }
    }
  }, [visible, data, form, menuType, parentId, owner, position]);

  const convertToTreeSelectData = (nodes: MenuTreeNode[]):any => {
    return nodes
      .filter((n) => n.menuType === 1)
      .map((node) => ({
        title: node.menuName,
        value: node.id,
        children: node.children ? convertToTreeSelectData(node.children) : undefined,
      }));
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: MenuSaveRequest = {
        ...values,
        id: isEdit ? data?.id : undefined,
        owner,
        position,
      };
      await saveOrUpdateMenu(reqData);
      message.success(isEdit ? '编辑成功' : '新增成功');
      onSuccess();
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  const currentMenuType = Form.useWatch('menuType', form);

  return (
    <Modal
      title={isEdit ? "编辑菜单" : menuType === 1 ? "新增目录" : "新增菜单"}
      open={visible}
      onOk={handleOk}
      onCancel={onClose}
      okButtonProps={{ "data-ai-approval": "true" }}
      confirmLoading={loading}
      className={styles.formModal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="menuName"
          label="菜单名称"
          rules={[{ required: true, message: "请输入菜单名称" }]}
        >
          <Input placeholder="请输入菜单名称" />
        </Form.Item>
        <Form.Item
          name="description"
          label="功能描述"
          rules={[{ required: true, message: "请输入功能描述" }]}
        >
          <MarkdownEditor
            placeholder="描述该菜单的功能，用于AI提示词构建"
            minHeight={150}
          />
        </Form.Item>
        <Form.Item name="menuType" label="菜单类型">
          <Select disabled options={MENU_TYPE_OPTIONS} />
        </Form.Item>
        <Form.Item name="parentId" label="父菜单">
          <TreeSelect
            placeholder="无（顶级）"
            treeData={convertToTreeSelectData(treeData)}
            allowClear
            treeDefaultExpandAll
          />
        </Form.Item>
        {currentMenuType === 2 && (
          <>
            <Form.Item
              name="path"
              label="路由路径"
              rules={[{ required: true, message: "请输入路由路径" }]}
            >
              <Input placeholder="例如：/sub-security/menu" />
            </Form.Item>
          </>
        )}
        {currentMenuType === 1 && (
          <Form.Item name="icon" label="菜单图标">
            <IconPicker />
          </Form.Item>
        )}
        <Form.Item name="sort" label="排序号">
          <InputNumber min={0} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item name="visible" label="是否显示" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item name="status" label="状态" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default MenuFormModal;
