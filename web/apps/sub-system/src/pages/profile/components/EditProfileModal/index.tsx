import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, Select, App } from 'antd';
import type { UserInfo } from '@gwsu/core';
import styles from './index.module.less';

interface EditProfileFormValues {
  nickname: string;
  gender: number;
  email?: string;
  phone?: string;
}

interface EditProfileModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 当前用户信息 */
  userInfo: UserInfo | null;
  /** 关闭回调 */
  onClose: () => void;
  /** 提交回调 */
  onSubmit: (values: EditProfileFormValues) => Promise<void>;
}

const EditProfileModal: React.FC<EditProfileModalProps> = ({
  visible,
  userInfo,
  onClose,
  onSubmit,
}) => {
  const [form] = Form.useForm<EditProfileFormValues>();
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();

  useEffect(() => {
    if (!visible) {
      return;
    }
    form.setFieldsValue({
      nickname: userInfo?.nickname ?? '',
      gender: userInfo?.gender ?? 0,
      email: userInfo?.email ?? undefined,
      phone: userInfo?.phone ?? undefined,
    });
  }, [form, userInfo, visible]);

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await onSubmit({
        nickname: values.nickname.trim(),
        gender: values.gender,
        email: values.email?.trim() || undefined,
        phone: values.phone?.trim() || undefined,
      });
      message.success('个人资料更新成功');
      form.resetFields();
    } catch {
      // 表单校验失败或请求错误，由各自机制处理
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title="编辑个人资料"
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="保存"
      cancelText="取消"
      okButtonProps={{ 'data-ai-approval': 'true' }}
      className={styles.modal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" className={styles.form}>
        <Form.Item
          name="nickname"
          label="昵称"
          rules={[
            { required: true, message: '请输入昵称' },
            { max: 30, message: '昵称长度不能超过30个字符' },
          ]}
        >
          <Input placeholder="请输入昵称" maxLength={30} />
        </Form.Item>
        <Form.Item name="gender" label="性别" rules={[{ required: true, message: '请选择性别' }]}>
          <Select
            placeholder="请选择性别"
            options={[
              { label: '未知', value: 0 },
              { label: '男', value: 1 },
              { label: '女', value: 2 },
            ]}
          />
        </Form.Item>
        <Form.Item
          name="email"
          label="邮箱"
          rules={[{ type: 'email', message: '请输入正确的邮箱格式' }]}
        >
          <Input placeholder="请输入邮箱" maxLength={64} />
        </Form.Item>
        <Form.Item
          name="phone"
          label="手机号"
          rules={[{ pattern: /^1\d{10}$/, message: '请输入正确的手机号格式' }]}
        >
          <Input placeholder="请输入手机号" maxLength={11} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default EditProfileModal;
