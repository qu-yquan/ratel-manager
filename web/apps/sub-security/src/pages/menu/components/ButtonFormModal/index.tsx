import React, { useState, useEffect } from 'react';
import { Modal, Form, Input, Button, Tag, App } from 'antd';
import { SelectOutlined } from '@ant-design/icons';
import styles from './index.module.less';
import { saveOrUpdateMenu } from '../../services/menu';
import type { MenuTreeNode, MenuSaveRequest } from '../../types';
import ApiResourcePicker from '../ApiResourcePicker';
import MarkdownEditor from '@/components/MarkdownEditor';

interface ButtonFormModalProps {
  visible: boolean;
  mode: 'create' | 'edit';
  owner: number;
  position: number;
  parentMenuId: string;
  data?: MenuTreeNode | null;
  onClose: () => void;
  onSuccess: () => void;
}

const METHOD_COLORS: Record<string, string> = {
  GET: 'green',
  POST: 'blue',
  PUT: 'orange',
  DELETE: 'red',
  PATCH: 'purple',
};

const ButtonFormModal: React.FC<ButtonFormModalProps> = ({
  visible,
  mode,
  owner,
  position,
  parentMenuId,
  data,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [pickerVisible, setPickerVisible] = useState(false);
  const [permission, setPermission] = useState('');
  const isEdit = mode === 'edit';

  useEffect(() => {
    if (visible) {
      if (data) {
        let keySuffix = data.buttonKey || '';
        if (keySuffix.startsWith(parentMenuId + '_')) {
          keySuffix = keySuffix.substring(parentMenuId.length + 1);
        }
        form.setFieldsValue({
          menuName: data.menuName,
          buttonKeySuffix: keySuffix,
          description: data.description,
        });
        setPermission(data.permission || '');
      } else {
        form.resetFields();
        form.setFieldsValue({
          description: '# 功能介绍\n请输入...\n# 界面布局\n暂无描述',
        });
        setPermission('');
      }
    }
  }, [visible, data, form, parentMenuId]);

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      const fullButtonKey = `${parentMenuId}_${values.buttonKeySuffix}`;
      const reqData: MenuSaveRequest = {
        id: isEdit ? data?.id : undefined,
        parentId: parentMenuId,
        menuName: values.menuName,
        menuType: 3,
        buttonKey: fullButtonKey,
        permission: permission || undefined,
        description: values.description,
        owner,
        position,
        sort: 0,
        status: true,
        visible: true,
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

  const handlePermissionConfirm = (perm: string) => {
    setPermission(perm);
  };

  const permissionTags = permission
    ? permission.split(';').filter(Boolean)
    : [];

  /** 渲染权限标识 Tag */
  const renderPermTag = (tag: string, idx: number) => {
    const method = tag.split(':')[0] || '';
    return (
      <Tag key={idx} color={METHOD_COLORS[method] || 'default'}>
        {tag}
      </Tag>
    );
  };

  return (
    <Modal
      title={isEdit ? "编辑按钮" : "新增按钮"}
      open={visible}
      okButtonProps={{ "data-ai-approval": "true" }}
      onOk={handleOk}
      onCancel={onClose}
      confirmLoading={loading}
      className={styles.buttonFormModal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="menuName"
          label="按钮名称"
          rules={[{ required: true, message: "请输入按钮名称" }]}
        >
          <Input placeholder="例如：新增、删除、导出" />
        </Form.Item>
        <Form.Item
          name="description"
          label="功能描述"
          rules={[{ required: true, message: "请输入功能描述" }]}
        >
          <MarkdownEditor
            placeholder="描述该按钮的功能，用于AI提示词构建"
            minHeight={200}
          />
        </Form.Item>
        <Form.Item
          name="buttonKeySuffix"
          label="按钮标识"
          rules={[{ required: true, message: "请输入按钮标识" }]}
        >
          <div className={styles.buttonKeyField}>
            <span className={styles.buttonKeyPrefix}>{parentMenuId}_</span>
            <Form.Item name="buttonKeySuffix" noStyle>
              <Input
                className={styles.buttonKeyInput}
                placeholder="输入标识，例如：add"
              />
            </Form.Item>
          </div>
        </Form.Item>
        <Form.Item label="接口权限">
          <div className={styles.permissionField}>
            <div className={styles.permissionTags}>
              {permissionTags.length > 0 ? (
                permissionTags.map((tag, idx) => renderPermTag(tag, idx))
              ) : (
                <span
                  style={{ color: "var(--text-secondary-color)", fontSize: 13 }}
                >
                  暂未配置接口权限
                </span>
              )}
            </div>
            <Button
              icon={<SelectOutlined />}
              onClick={() => setPickerVisible(true)}
            >
              选择接口
            </Button>
          </div>
        </Form.Item>
      </Form>
      <ApiResourcePicker
        visible={pickerVisible}
        currentPermission={permission}
        onClose={() => setPickerVisible(false)}
        onConfirm={handlePermissionConfirm}
      />
    </Modal>
  );
};

export default ButtonFormModal;
