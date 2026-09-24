import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, InputNumber, Switch, App } from 'antd';
import { saveDept, getDeptTree } from '@/services/dept';
import type { DeptDetail, DeptTypeOption, DeptTreeNode } from '../../types';

interface DeptFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  parentId?: number;
  dept?: DeptDetail;
  deptTypes: DeptTypeOption[];
  onClose: () => void;
  onSuccess: () => void;
}

const DeptFormModal: React.FC<DeptFormModalProps> = ({
  visible,
  mode,
  parentId,
  dept,
  deptTypes,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [deptTree, setDeptTree] = useState<DeptTreeNode[]>([]);

  useEffect(() => {
    if (visible) {
      getDeptTree().then(setDeptTree).catch(() => {});

      if (mode === 'edit' && dept) {
        form.setFieldsValue({
          name: dept.name,
          type: dept.type,
          parentId: dept.parentId,
          enabled: dept.enabled,
          sort: dept.sort,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          parentId: parentId || undefined,
          enabled: true,
          sort: 0,
        });
      }
    }
  }, [visible, mode, dept, parentId, form]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      const data = {
        ...(mode === 'edit' && dept ? { id: dept.id } : {}),
        name: values.name,
        type: values.type,
        parentId: values.parentId || null,
        enabled: values.enabled,
        sort: values.sort || 0,
      };

      await saveDept(data);
      message.success(mode === 'edit' ? '编辑成功' : '创建成功');
      onSuccess();
    } catch {
      message.error(mode === 'edit' ? '编辑失败' : '创建失败');
    } finally {
      setLoading(false);
    }
  };

  const convertToOptions = (nodes: DeptTreeNode[], level = 0): { value: string; label: string }[] => {
    const options: { value: string; label: string }[] = [];
    nodes.forEach((node) => {
      options.push({
        value: node.id,
        label: `${'\u3000'.repeat(level)}${node.name}`,
      });
      if (node.children) {
        options.push(...convertToOptions(node.children, level + 1));
      }
    });
    return options;
  };

  return (
    <Modal
      title={mode === "edit" ? "编辑部门" : "新增部门"}
      open={visible}
      onCancel={onClose}
      onOk={handleSubmit}
      okButtonProps={{ "data-ai-approval": "true" }}
      confirmLoading={loading}
      destroyOnHidden
      width={500}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="部门名称"
          rules={[{ required: true, message: "请输入部门名称" }]}
        >
          <Input placeholder="请输入部门名称" />
        </Form.Item>

        <Form.Item
          name="type"
          label="部门类型"
          rules={[{ required: true, message: "请选择部门类型" }]}
        >
          <Select placeholder="请选择部门类型">
            {deptTypes.map((t) => (
              <Select.Option key={t.code} value={t.code}>
                {t.name}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item name="parentId" label="父部门">
          <Select
            placeholder="请选择父部门（不选则为根部门）"
            allowClear
            showSearch
          >
            {convertToOptions(deptTree).map((opt) => (
              <Select.Option key={opt.value} value={opt.value}>
                {opt.label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item name="enabled" label="是否启用" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>

        <Form.Item name="sort" label="排序号">
          <InputNumber
            min={0}
            placeholder="数字越小越靠前"
            style={{ width: "100%" }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default DeptFormModal;
