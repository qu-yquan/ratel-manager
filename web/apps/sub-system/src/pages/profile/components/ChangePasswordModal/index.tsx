import React, { useState } from 'react';
import { Modal, Form, Input, App } from 'antd';
import { encryptPassword } from '@gwsu/core';
import { changePassword } from '@/services/profile';
import styles from './index.module.less';

interface ChangePasswordModalProps {
  /** 是否显示 */
  visible: boolean;
  /** 关闭回调 */
  onClose: () => void;
  /** 修改成功回调 */
  onSuccess: () => void;
}

const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({ visible, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();

  const handleOk = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);
      await changePassword({
        oldPassword: encryptPassword(values.oldPassword),
        newPassword: encryptPassword(values.newPassword),
      });
      message.success('密码修改成功');
      form.resetFields();
      onSuccess();
    } catch {
      // 表单校验失败或请求错误，由各自机制处理
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Modal
      title="修改密码"
      open={visible}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="确认修改"
      cancelText="取消"
      okButtonProps={{ "data-ai-approval": "true" }}
      className={styles.modal}
      destroyOnHidden
    >
      <Form form={form} layout="vertical" className={styles.form}>
        <Form.Item
          name="oldPassword"
          label="旧密码"
          rules={[{ required: true, message: "请输入旧密码" }]}
        >
          <Input.Password placeholder="请输入旧密码" />
        </Form.Item>
        <Form.Item
          name="newPassword"
          label="新密码"
          rules={[
            { required: true, message: "请输入新密码" },
            { min: 6, message: "密码长度不能少于6位" },
          ]}
        >
          <Input.Password placeholder="请输入新密码" />
        </Form.Item>
        <Form.Item
          name="confirmPassword"
          label="确认新密码"
          dependencies={["newPassword"]}
          rules={[
            { required: true, message: "请确认新密码" },
            ({ getFieldValue }) => ({
              validator(_, value) {
                if (!value || getFieldValue("newPassword") === value) {
                  return Promise.resolve();
                }
                return Promise.reject(new Error("两次输入的密码不一致"));
              },
            }),
          ]}
        >
          <Input.Password placeholder="请再次输入新密码" />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ChangePasswordModal;
