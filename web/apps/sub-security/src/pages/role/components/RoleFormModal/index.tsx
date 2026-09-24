import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, InputNumber, Switch, Select } from 'antd';
import styles from './index.module.less';
import type { RoleInfo, EnumOption } from '../../types';

const { TextArea } = Input;

interface RoleFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  data?: RoleInfo | null;
  roleTypeOptions: EnumOption[];
  dataScopeOptions: EnumOption[];
  onSave: (data: RoleInfo) => Promise<boolean>;
  onClose: () => void;
  onSuccess: () => void;
}

const RoleFormModal: React.FC<RoleFormModalProps> = ({
  visible,
  mode,
  data,
  roleTypeOptions,
  dataScopeOptions,
  onSave,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (visible) {
      if (data) {
        form.setFieldsValue({
          roleCode: data.roleCode,
          roleName: data.roleName,
          description: data.description,
          sort: data.sort ?? 0,
          roleType: data.roleType,
          dataScope: data.dataScope,
          status: data.status,
        });
      } else {
        form.resetFields();
        form.setFieldsValue({
          sort: 0,
          roleType: 2,
          dataScope: 4,
          status: true,
        });
      }
    }
  }, [visible, data, form]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const reqData: RoleInfo = {
        ...values,
        id: isEdit ? data?.id : undefined,
      };
      const success = await onSave(reqData);
      if (success) {
        onSuccess();
      }
    } catch {
      // 表单校验失败或请求错误
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={isEdit ? "编辑角色" : "新增角色"}
      open={visible}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ "data-ai-approval": "true" }}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      className={styles.formModal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="roleCode"
          label="角色编码"
          rules={[{ required: true, message: "请输入角色编码" }]}
        >
          <Input
            placeholder="请输入角色编码，如 ROLE_ADMIN"
            disabled={isEdit}
          />
        </Form.Item>
        <Form.Item
          name="roleName"
          label="角色名称"
          rules={[{ required: true, message: "请输入角色名称" }]}
        >
          <Input placeholder="请输入角色名称" />
        </Form.Item>
        <Form.Item name="description" label="描述">
          <TextArea
            rows={3}
            placeholder="请输入角色描述"
            showCount
            maxLength={256}
          />
        </Form.Item>
        <Form.Item name="sort" label="排序号">
          <InputNumber min={0} style={{ width: "100%" }} />
        </Form.Item>
        <Form.Item
          name="roleType"
          label="角色类型"
          rules={[{ required: true, message: "请选择角色类型" }]}
        >
          <Select
            options={roleTypeOptions}
            placeholder="请选择角色类型"
            disabled
          />
        </Form.Item>
        <Form.Item
          name="dataScope"
          label="数据范围"
          rules={[{ required: true, message: "请选择数据范围" }]}
        >
          <Select options={dataScopeOptions} placeholder="请选择数据范围" />
        </Form.Item>
        <Form.Item name="status" label="状态" valuePropName="checked">
          <Switch checkedChildren="启用" unCheckedChildren="禁用" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default RoleFormModal;
