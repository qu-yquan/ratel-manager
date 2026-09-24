import React, { useState } from 'react';
import { App, Form, Modal, Select } from 'antd';

const CLEAR_LOG_OPTIONS = [
  { label: '清理一个月前日志', value: 1 },
  { label: '清理三个月前日志', value: 2 },
  { label: '清理六个月前日志', value: 3 },
  { label: '清理一年前日志', value: 4 },
  { label: '清理1000条以前日志', value: 5 },
  { label: '清理10000条以前日志', value: 6 },
  { label: '清理30000条以前日志', value: 7 },
  { label: '清理100000条以前日志', value: 8 },
  { label: '清理所有日志', value: 9 },
];

interface ClearLogModalProps {
  visible: boolean;
  onSubmit: (type: number) => Promise<boolean>;
  onClose: () => void;
}

interface ClearLogFormValues {
  type?: number;
}

const ClearLogModal: React.FC<ClearLogModalProps> = ({ visible, onSubmit, onClose }) => {
  const { message } = App.useApp();
  const [form] = Form.useForm<ClearLogFormValues>();
  const [submitting, setSubmitting] = useState(false);

  const handleOk = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      const success = await onSubmit(values.type!);
      if (success) {
        message.success('清理成功');
        onClose();
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="清理日志"
      open={visible}
      onCancel={onClose}
      onOk={handleOk}
      okText="确认清理"
      confirmLoading={submitting}
      okButtonProps={{ danger: true, "data-ai-approval": "true" }}
      destroyOnHidden
      afterOpenChange={(open) => {
        if (!open) {
          form.resetFields();
        }
      }}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="type"
          label="清理范围"
          rules={[{ required: true, message: "请选择清理范围" }]}
        >
          <Select placeholder="请选择清理范围" options={CLEAR_LOG_OPTIONS} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default ClearLogModal;
