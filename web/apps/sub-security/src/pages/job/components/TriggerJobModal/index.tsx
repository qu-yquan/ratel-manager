import React, { useEffect } from 'react';
import { App, Form, Input, Modal, Typography } from 'antd';
import type { JobInfo } from '../../types';
import { deriveJobMode } from '../../utils';

interface TriggerJobModalProps {
  visible: boolean;
  job: JobInfo | null;
  onSubmit: (jobId: string, executorParam?: string) => Promise<boolean>;
  onClose: () => void;
}

interface TriggerJobFormValues {
  executorParam?: string;
}

const TriggerJobModal: React.FC<TriggerJobModalProps> = ({ visible, job, onSubmit, onClose }) => {
  const { message } = App.useApp();
  const [form] = Form.useForm<TriggerJobFormValues>();
  const [submitting, setSubmitting] = React.useState(false);

  useEffect(() => {
    if (visible) {
      form.setFieldsValue({
        executorParam: job?.executorParam ?? '',
      });
    } else {
      form.resetFields();
    }
  }, [visible, job, form]);

  const handleOk = async () => {
    if (!job?.id) {
      return;
    }
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      const success = await onSubmit(job.id, values.executorParam);
      if (success) {
        message.success('触发成功');
        onClose();
      }
    } finally {
      setSubmitting(false);
    }
  };

  const jobMode = job ? deriveJobMode(job) : 'BEAN';

  return (
    <Modal
      title="立即执行"
      open={visible}
      onCancel={onClose}
      onOk={handleOk}
      okText="执行"
      confirmLoading={submitting}
      okButtonProps={{ "data-ai-approval": "true" }}
      destroyOnHidden
    >
      <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
        执行前可按需调整扩展参数。平台URL任务默认回显当前 `executorParam` 配置。
      </Typography.Paragraph>
      <Form form={form} layout="vertical">
        <Form.Item label="任务名称">
          <Input value={job?.name} disabled />
        </Form.Item>
        <Form.Item label="任务类型">
          <Input value={jobMode === "URL" ? "平台URL" : jobMode} disabled />
        </Form.Item>
        <Form.Item name="executorParam" label="扩展参数">
          <Input.TextArea
            rows={8}
            placeholder={
              jobMode === "URL"
                ? "请输入URL任务执行参数(JSON)"
                : "请输入扩展参数"
            }
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default TriggerJobModal;
