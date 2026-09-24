import React, { useEffect, useState } from 'react';
import { App, Descriptions, Modal, Spin, Tag } from 'antd';
import styles from './index.module.less';
import { getApiKeyDetail } from '../../services/apiKey';
import type { ApiKeyDetail } from '../../types';

interface Props {
  visible: boolean;
  apiKeyId: string | null;
  onClose: () => void;
}

function formatDateTime(value?: string, fallback = '-') {
  if (!value) {
    return fallback;
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return fallback;
  }
  const pad = (num: number) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

const ApiKeyDetailModal: React.FC<Props> = ({ visible, apiKeyId, onClose }) => {
  const { message } = App.useApp();
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<ApiKeyDetail | null>(null);

  useEffect(() => {
    if (!visible || !apiKeyId) {
      return;
    }
    const loadDetail = async () => {
      setLoading(true);
      try {
        const data = await getApiKeyDetail(apiKeyId);
        setDetail(data);
      } catch {
        message.error('加载 API_KEY 详情失败');
      } finally {
        setLoading(false);
      }
    };
    void loadDetail();
  }, [apiKeyId, message, visible]);

  return (
    <Modal
      title="API_KEY 详情"
      open={visible}
      onCancel={onClose}
      footer={null}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <Descriptions column={1} size="small" colon={false}>
          <Descriptions.Item label="名称">{detail?.apiKeyName ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="脱敏 Key">
            <span className={styles.maskedKey}>{detail?.maskedKey ?? '-'}</span>
          </Descriptions.Item>
          <Descriptions.Item label="状态">
            <Tag color={detail?.status === 1 ? 'green' : 'default'}>
              {detail?.status === 1 ? '启用' : '停用'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="过期时间">{formatDateTime(detail?.expireTime, '永不过期')}</Descriptions.Item>
          <Descriptions.Item label="最近使用时间">{formatDateTime(detail?.lastUsedTime)}</Descriptions.Item>
          <Descriptions.Item label="最近使用 IP">{detail?.lastUsedIp ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{formatDateTime(detail?.createTime)}</Descriptions.Item>
          <Descriptions.Item label="备注">{detail?.remark || '-'}</Descriptions.Item>
        </Descriptions>
      </Spin>
    </Modal>
  );
};

export default ApiKeyDetailModal;
