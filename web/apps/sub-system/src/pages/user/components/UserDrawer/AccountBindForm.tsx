import React, { useState, useEffect } from 'react';
import { Form, Select, Button, Input } from 'antd';
import type { SysAccountBindDTO, DingTalkAccountOption } from '../../types';
import { getBindableDingTalkAccounts } from '@/services/user';
import styles from './AccountBindSection.module.less';

interface AccountBindFormProps {
  identityType: string;
  onSubmit: (data: SysAccountBindDTO) => Promise<void>;
  onCancel: () => void;
}

const AccountBindForm: React.FC<AccountBindFormProps> = ({ identityType, onSubmit, onCancel }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [dingTalkOptions, setDingTalkOptions] = useState<DingTalkAccountOption[]>([]);
  const [dingTalkLoading, setDingTalkLoading] = useState(false);

  // 钉钉绑定时，加载可绑定的钉钉账号列表
  useEffect(() => {
    if (identityType === 'dingtalk') {
      setDingTalkLoading(true);
      getBindableDingTalkAccounts()
        .then((data) => setDingTalkOptions(data))
        .catch(() => {})
        .finally(() => setDingTalkLoading(false));
    }
  }, [identityType]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (identityType === 'dingtalk') {
        const selectedOption = dingTalkOptions.find((o) => o.id === values.dingTalkAccountId);
        await onSubmit({
          identityType: 'dingtalk',
          identifier: selectedOption?.identifier ?? '',
          originalUserId: selectedOption?.userId,
        });
      } else {
        await onSubmit({
          identityType,
          identifier: values.identifier,
          credential: values.credential,
        });
      }
      // 绑定成功后由父组件关闭表单
    } catch {
      // 验证失败或用户取消确认弹框
    } finally {
      setLoading(false);
    }
  };

  return (
    <Form form={form} layout="vertical" size="small" style={{ marginTop: 8 }}>
      {identityType === 'password' && (
        <>
          <Form.Item
            label="用户名"
            name="identifier"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item
            label="密码"
            name="credential"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password />
          </Form.Item>
        </>
      )}
      {identityType === 'phone' && (
        <Form.Item
          label="手机号"
          name="identifier"
          rules={[{ required: true, message: '请输入手机号' }]}
        >
          <Input placeholder="请输入手机号" />
        </Form.Item>
      )}
      {identityType === 'dingtalk' && (
        <>
          <div className={styles.dingTalkHint}>用户需要先通过钉钉快捷登录后方能展示</div>
          <Form.Item
            label="选择钉钉账号"
            name="dingTalkAccountId"
            rules={[{ required: true, message: '请选择钉钉账号' }]}
          >
            <Select
              placeholder="请选择要绑定的钉钉账号"
              loading={dingTalkLoading}
              options={dingTalkOptions.map((o) => ({
                label: `${o.nickname}（${o.userName}）`,
                value: o.id,
              }))}
              notFoundContent={dingTalkLoading ? '加载中...' : '暂无可绑定的钉钉账号'}
            />
          </Form.Item>
        </>
      )}
      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        <Button size="small" onClick={onCancel}>
          取消
        </Button>
        <Button
          size="small"
          type="primary"
          data-ai-approval
          loading={loading}
          onClick={handleSubmit}
        >
          确认绑定
        </Button>
      </div>
    </Form>
  );
};

export default AccountBindForm;
