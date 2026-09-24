import React, { useState, useEffect, useCallback } from 'react';
import { Drawer, Empty, Spin } from 'antd';
import { getBusinessFunctionDetail } from '../../services/businessFunction';
import type { BusinessFunctionInfo, BusinessFunctionDetail } from '../../types';
import styles from './index.module.less';

interface BusinessFunctionDetailDrawerProps {
  visible: boolean;
  data: BusinessFunctionInfo | null;
  onClose: () => void;
}

const BusinessFunctionDetailDrawer: React.FC<BusinessFunctionDetailDrawerProps> = ({
  visible,
  data,
  onClose,
}) => {
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<BusinessFunctionDetail | null>(null);

  useEffect(() => {
    if (visible && data?.id) {
      setLoading(true);
      getBusinessFunctionDetail(data.id)
        .then((res) => {
          setDetail(res);
        })
        .catch(() => {
          setDetail(null);
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setDetail(null);
    }
  }, [visible, data]);

  const renderMarkdownPreview = useCallback((content: string) => {
    const simpleHtml = content
      .replace(/^### (.+)$/gm, '<h3>$1</h3>')
      .replace(/^## (.+)$/gm, '<h2>$1</h2>')
      .replace(/^# (.+)$/gm, '<h1>$1</h1>')
      .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.+?)\*/g, '<em>$1</em>')
      .replace(/`(.+?)`/g, '<code>$1</code>')
      .replace(/^- (.+)$/gm, '<li>$1</li>')
      .replace(/^> (.+)$/gm, '<blockquote>$1</blockquote>')
      .replace(/\n/g, '<br/>');
    return simpleHtml;
  }, []);

  return (
    <Drawer
      title="业务功能详情"
      size={640}
      open={visible}
      onClose={onClose}
      className={styles.detailDrawer}
    >
      {loading ? (
        <div style={{ textAlign: 'center', padding: 40 }}>
          <Spin />
        </div>
      ) : detail ? (
        <>
          <div className={styles.section}>
            <div className={styles.sectionTitle}>基本信息</div>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>业务名称</span>
              <span className={styles.infoValue}>{detail.name}</span>
            </div>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>业务简介</span>
              <span className={styles.infoValue}>{detail.summary}</span>
            </div>
          </div>

          <div className={styles.section}>
            <div className={styles.sectionTitle}>详细介绍</div>
            <div
              className={styles.mdPreview}
              dangerouslySetInnerHTML={{
                __html: renderMarkdownPreview(detail.detail || ''),
              }}
            />
          </div>

          <div className={styles.section}>
            <div className={styles.sectionTitle}>
              关联表模型（{detail.tables?.length || 0}）
            </div>
            {detail.tables && detail.tables.length > 0 ? (
              <div className={styles.tableList}>
                {detail.tables.map((table) => (
                  <div key={table.id} className={styles.tableItem}>
                    <span className={styles.tableName}>{table.tableName}</span>
                    <span className={styles.tableComment}>
                      {table.tableComment || '-'}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <Empty
                description="暂无关联表模型"
                image={Empty.PRESENTED_IMAGE_SIMPLE}
              />
            )}
          </div>
        </>
      ) : (
        <Empty description="暂无数据" />
      )}
    </Drawer>
  );
};

export default BusinessFunctionDetailDrawer;
