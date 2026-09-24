import React, { useState, useEffect } from 'react';
import { Form, Input, Select, Button, App } from 'antd';
import { GENDER_MAP } from '../../types';
import type { SysUserDetailVO } from '../../types';
import { saveOrUpdateUser } from '@/services/user';
import { AuthGate } from "@gwsu/core";
import { PERM_EDIT } from '../../permissionConstants';

interface BasicInfoSectionProps {
  user: SysUserDetailVO;
  onRefresh: () => void;
  readOnly?: boolean;
}

const BasicInfoSection: React.FC<BasicInfoSectionProps> = ({ user, onRefresh, readOnly = false }) => {
  const { message } = App.useApp();
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  // 进入编辑模式时设置表单初始值
  // 确保 Form 组件已挂载后再调用 setFieldsValue，避免 Ant Design 告警
  useEffect(() => {
    if (editing) {
      form.setFieldsValue({
        nickname: user.nickname,
        email: user.email,
        phone: user.phone,
        gender: user.gender,
      });
    }
  }, [editing]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleEdit = () => {
    setEditing(true);
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);
      await saveOrUpdateUser({ userId: user.userId, ...values });
      message.success('保存成功');
      setEditing(false);
      onRefresh();
    } catch {
      // 表单验证失败
    } finally {
      setSaving(false);
    }
  };

  if (editing) {
    return (
      <Form form={form} layout="vertical" size="small">
        <div
          style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}
        >
          <Form.Item
            label="昵称"
            name="nickname"
            rules={[{ required: true, message: "请输入昵称" }]}
          >
            <Input />
          </Form.Item>
          <Form.Item label="性别" name="gender">
            <Select
              options={[
                { label: "未知", value: 0 },
                { label: "男", value: 1 },
                { label: "女", value: 2 },
              ]}
            />
          </Form.Item>
          <Form.Item label="邮箱" name="email">
            <Input />
          </Form.Item>
          <Form.Item label="手机" name="phone">
            <Input />
          </Form.Item>
        </div>
        <div style={{ display: "flex", justifyContent: "flex-end", gap: 8 }}>
          <Button size="small" onClick={() => setEditing(false)}>
            取消
          </Button>
          <Button
            size="small"
            type="primary"
            data-ai-approval
            loading={saving}
            onClick={handleSave}
          >
            保存
          </Button>
        </div>
      </Form>
    );
  }

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 10,
        }}
      >
        <span style={{ fontSize: 13, fontWeight: 600 }}>基本信息</span>
        {!readOnly && (
          <AuthGate buttonKey={PERM_EDIT}>
            <a onClick={handleEdit}>编辑</a>
          </AuthGate>
        )}
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: 10,
          fontSize: 12,
        }}
      >
        <div
          style={{
            background: "#fafafa",
            padding: "8px 12px",
            borderRadius: 4,
          }}
        >
          <div style={{ color: "#999", marginBottom: 2, fontSize: 10 }}>
            昵称
          </div>
          <div>{user.nickname}</div>
        </div>
        <div
          style={{
            background: "#fafafa",
            padding: "8px 12px",
            borderRadius: 4,
          }}
        >
          <div style={{ color: "#999", marginBottom: 2, fontSize: 10 }}>
            性别
          </div>
          <div>{GENDER_MAP[user.gender] || "未知"}</div>
        </div>
        <div
          style={{
            background: "#fafafa",
            padding: "8px 12px",
            borderRadius: 4,
          }}
        >
          <div style={{ color: "#999", marginBottom: 2, fontSize: 10 }}>
            邮箱
          </div>
          <div>{user.email || "-"}</div>
        </div>
        <div
          style={{
            background: "#fafafa",
            padding: "8px 12px",
            borderRadius: 4,
          }}
        >
          <div style={{ color: "#999", marginBottom: 2, fontSize: 10 }}>
            手机
          </div>
          <div>{user.phone || "-"}</div>
        </div>
        <div
          style={{
            background: "#fafafa",
            padding: "8px 12px",
            borderRadius: 4,
          }}
        >
          <div style={{ color: "#999", marginBottom: 2, fontSize: 10 }}>
            最后登录
          </div>
          <div>{user.lastLoginTime || "-"}</div>
        </div>
      </div>
    </div>
  );
};

export default BasicInfoSection;
