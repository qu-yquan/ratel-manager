import React, { useMemo, useState } from 'react';
import { App, Button, DatePicker, Form, Input, InputNumber, Modal, Radio } from 'antd';
import styles from './index.module.less';
import { createApiKey } from '../../services/apiKey';
import type { ApiKeyCreateDTO, ApiKeyCreateResult, ApiKeyExpireType } from '../../types';

interface Props {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}

interface FormValues {
  apiKeyName: string;
  expireType: ApiKeyExpireType;
  expireTime?: {
    toISOString: () => string;
    valueOf: () => number;
  };
  expireDays?: number;
  remark?: string;
}

const ApiKeyCreateModal: React.FC<Props> = ({ visible, onClose, onSuccess }) => {
  const { message } = App.useApp();
  const [form] = Form.useForm<FormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState<ApiKeyCreateResult | null>(null);

  const expireType = Form.useWatch('expireType', form) ?? 'FOREVER';
  const destroyOnClose = useMemo(() => !visible, [visible]);

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      const payload: ApiKeyCreateDTO = {
        apiKeyName: values.apiKeyName.trim(),
        expireType: values.expireType,
        remark: values.remark?.trim(),
      };
      if (values.expireType === 'AFTER_DAYS') {
        payload.expireDays = values.expireDays;
      }
      if (values.expireType === 'CUSTOM_DATE' && values.expireTime) {
        payload.expireTime = values.expireTime.toISOString();
      }

      setSubmitting(true);
      try {
        const created = await createApiKey(payload);
        message.success('API_KEY 创建成功');
        form.resetFields();
        onClose();
        await onSuccess();
        setResult(created);
      } finally {
        setSubmitting(false);
      }
    } catch {
      // 表单校验或请求异常由统一提示处理
    }
  };

  const handleCopy = async () => {
    if (!result?.apiKey) {
      return;
    }
    try {
      await navigator.clipboard.writeText(result.apiKey);
      message.success('已复制 API_KEY');
    } catch {
      message.error('复制失败，请手动复制');
    }
  };

  return (
    <>
      <Modal
        title="创建 API_KEY"
        open={visible}
        onCancel={onClose}
        onOk={handleSubmit}
        okText="创建"
        cancelText="取消"
        confirmLoading={submitting}
        destroyOnClose={destroyOnClose}
      >
        <Form<FormValues>
          form={form}
          layout="vertical"
          initialValues={{ expireType: 'FOREVER', expireDays: 30 }}
        >
          <Form.Item
            name="apiKeyName"
            label="名称"
            rules={[
              { required: true, message: '请输入名称' },
              { max: 128, message: '名称长度不能超过 128 个字符' },
            ]}
          >
            <Input placeholder="例如：本地自动化脚本" maxLength={128} />
          </Form.Item>

          <Form.Item name="expireType" label="有效期类型" rules={[{ required: true, message: '请选择有效期类型' }]}>
            <Radio.Group>
              <Radio value="FOREVER">永不过期</Radio>
              <Radio value="AFTER_DAYS">按天数</Radio>
              <Radio value="CUSTOM_DATE">指定时间</Radio>
            </Radio.Group>
          </Form.Item>

          {expireType === 'AFTER_DAYS' && (
            <Form.Item
              name="expireDays"
              label="有效天数"
              rules={[{ required: true, message: '请输入有效天数' }]}
            >
              <InputNumber min={1} precision={0} style={{ width: '100%' }} placeholder="请输入有效天数" />
            </Form.Item>
          )}

          {expireType === 'CUSTOM_DATE' && (
            <Form.Item
              name="expireTime"
              label="过期时间"
              rules={[{ required: true, message: '请选择过期时间' }]}
            >
              <DatePicker
                showTime
                style={{ width: '100%' }}
                placeholder="请选择过期时间"
                disabledDate={(current) => !!current && current.valueOf() < Date.now() - 24 * 60 * 60 * 1000}
              />
            </Form.Item>
          )}

          <Form.Item name="remark" label="备注" rules={[{ max: 512, message: '备注长度不能超过 512 个字符' }]}>
            <Input.TextArea rows={3} maxLength={512} placeholder="选填" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="API_KEY 已创建"
        open={!!result}
        onCancel={() => setResult(null)}
        footer={[
          <Button key="copy" type="primary" onClick={handleCopy}>
            复制 API_KEY
          </Button>,
          <Button key="close" onClick={() => setResult(null)}>
            我已保存
          </Button>,
        ]}
      >
        <div className={styles.resultTip}>该 Key 仅展示一次，请及时保存。</div>
        <div className={styles.apiKeyBlock}>{result?.apiKey}</div>
        <div className={styles.resultActions} />
      </Modal>
    </>
  );
};

export default ApiKeyCreateModal;
