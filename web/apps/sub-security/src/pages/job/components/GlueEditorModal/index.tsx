import React, { useState, useEffect, useCallback } from 'react';
import { Modal, Select, Input, Table, Button, Space, message, Descriptions, Spin } from 'antd';
import type { TableProps } from 'antd';
import type { GlueVersion } from '../../types';
import { GLUE_TYPE_OPTIONS } from '../../types';
import { getGlueVersionList, getGlueVersionDetail } from '../../services/job';
import { DEFAULT_GLUE_UPDATE_REMARK, normalizeGlueRemark } from '../../glue';
import GlueCodeEditor from '../GlueCodeEditor';
import styles from './index.module.less';

interface GlueEditorModalProps {
  visible: boolean;
  jobId: string;
  glueType?: string;
  glueSource?: string;
  onSave: (glueType: string, glueSource: string, glueRemark: string) => void;
  onClose: () => void;
}

const GlueEditorModal: React.FC<GlueEditorModalProps> = ({
  visible, jobId, glueType, glueSource, onSave, onClose,
}) => {
  const [currentGlueType, setCurrentGlueType] = useState(glueType ?? '');
  const [source, setSource] = useState(glueSource ?? '');
  const [remark, setRemark] = useState('');
  const [versions, setVersions] = useState<GlueVersion[]>([]);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [previewVersion, setPreviewVersion] = useState<GlueVersion | null>(null);

  useEffect(() => {
    if (visible) {
      setCurrentGlueType(glueType ?? '');
      setSource(glueSource ?? '');
      setRemark(DEFAULT_GLUE_UPDATE_REMARK);
      if (jobId) {
        getGlueVersionList(jobId).then(setVersions);
      }
    }
  }, [visible, jobId, glueType, glueSource]);

  const handleVersionPreview = useCallback(async (id: string) => {
    setPreviewLoading(true);
    try {
      const detail = await getGlueVersionDetail(id);
      if (detail) {
        setPreviewVersion(detail);
        setPreviewOpen(true);
      }
    } finally {
      setPreviewLoading(false);
    }
  }, []);

  const handlePreviewApply = useCallback(() => {
    if (!previewVersion) {
      return;
    }
    setSource(previewVersion.glueSource);
    setCurrentGlueType(previewVersion.glueType);
    setRemark(previewVersion.glueRemark ?? '');
    setPreviewOpen(false);
    setPreviewVersion(null);
    message.info('已回填历史版本代码');
  }, [previewVersion]);

  const versionColumns: TableProps<GlueVersion>['columns'] = [
    { title: '版本时间', dataIndex: 'createTime', width: 170 },
    { title: 'GLUE类型', dataIndex: 'glueType', width: 120 },
    { title: '备注', dataIndex: 'glueRemark', ellipsis: true },
    {
      title: '操作', width: 80,
      render: (_: unknown, record: GlueVersion) => (
        <Button type="link" size="small" onClick={() => handleVersionPreview(record.id)}>
          查看
        </Button>
      ),
    },
  ];

  return (
    <Modal
      title="GLUE 代码编辑"
      open={visible}
      width={900}
      onCancel={onClose}
      onOk={() => onSave(currentGlueType, source, normalizeGlueRemark(remark, DEFAULT_GLUE_UPDATE_REMARK))}
      okText="保存"
      destroyOnHidden
    >
      <Space direction="vertical" className={styles.container} size={16}>
        <div className={styles.headerRow}>
          <Select
            value={currentGlueType}
            onChange={setCurrentGlueType}
            options={[...GLUE_TYPE_OPTIONS]}
            placeholder="请选择GLUE类型"
            className={styles.glueType}
          />
        </div>
        <GlueCodeEditor
          glueType={currentGlueType}
          value={source}
          onChange={setSource}
          height="360px"
          fullscreenTitle="GLUE 代码全屏编辑"
        />
        <Input
          value={remark}
          onChange={(e) => setRemark(e.target.value)}
          placeholder="版本备注"
        />

        {versions.length > 0 && (
          <>
            <div className={styles.versionTitle}>历史版本</div>
            <div className={styles.versionHint}>点击“回填”后会把对应版本的脚本类型、代码和备注恢复到当前编辑区。</div>
            <Table<GlueVersion>
              rowKey="id"
              columns={versionColumns}
              dataSource={versions}
              size="small"
              pagination={false}
              scroll={{ y: 200 }}
            />
          </>
        )}
      </Space>

      <Modal
        title="历史版本代码查看"
        open={previewOpen}
        footer={[
          <Button
            key="cancel"
            onClick={() => {
              setPreviewOpen(false);
              setPreviewVersion(null);
            }}
          >
            取消
          </Button>,
          <Button
            key="apply"
            type="primary"
            onClick={handlePreviewApply}
            data-ai-approval
          >
            回填到当前编辑区
          </Button>,
        ]}
        width={960}
        onCancel={() => {
          setPreviewOpen(false);
          setPreviewVersion(null);
        }}
        destroyOnHidden
      >
        <Spin spinning={previewLoading}>
          {previewVersion && (
            <Space direction="vertical" className={styles.container} size={16}>
              <Descriptions column={1} size="small" bordered>
                <Descriptions.Item label="版本时间">{previewVersion.createTime ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="GLUE类型">{previewVersion.glueType}</Descriptions.Item>
                <Descriptions.Item label="版本备注">{previewVersion.glueRemark || '-'}</Descriptions.Item>
              </Descriptions>
              <GlueCodeEditor
                glueType={previewVersion.glueType}
                value={previewVersion.glueSource}
                height="420px"
                fullscreenTitle="历史版本代码查看"
                readOnly
              />
            </Space>
          )}
        </Spin>
      </Modal>
    </Modal>
  );
};

export default GlueEditorModal;
