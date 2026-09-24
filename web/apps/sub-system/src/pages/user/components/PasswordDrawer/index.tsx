import React from 'react';
import { Drawer, Form, Input, Button, App } from 'antd';
import styles from './index.module.less';
import { resetPassword } from '@/services/user';
import { encryptPassword } from '@gwsu/core';

interface PasswordDrawerProps {
  visible: boolean;
  userId: string | null;
  nickname: string;
  onClose: () => void;
  onSuccess: () => void;
}

const PasswordDrawer: React.FC<PasswordDrawerProps> = ({
  visible,
  userId,
  nickname,
  onClose,
  onSuccess,
}) => {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = React.useState(false);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (!userId) return;
      setLoading(true);
      await resetPassword(userId, encryptPassword(values.newPassword));
      message.success('密码修改成功');
      form.resetFields();
      onSuccess();
    } catch {
      // 表单验证失败
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    form.resetFields();
    onClose();
  };

  return (
    <Drawer
      title={`修改密码 - ${nickname}`}
      open={visible}
      onClose={handleClose}
      size={400}
      footer={
        <div className={styles.footer}>
          <Button onClick={handleClose}>取消</Button>
          <Button
            type="primary"
            data-ai-approval
            loading={loading}
            onClick={handleSubmit}
          >
            确认
          </Button>
        </div>
      }
    >
      <Form form={form} layout="vertical" className={styles.form}>
        <Form.Item
          label="新密码"
          name="newPassword"
          rules={[
            { required: true, message: "请输入新密码" },
            { min: 6, message: "密码至少6位" },
          ]}
        >
          <Input.Password placeholder="请输入新密码，至少6位" />
        </Form.Item>
        <Form.Item
          label="确认密码"
          name="confirmPassword"
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
    </Drawer>
  );
};

export default PasswordDrawer;
