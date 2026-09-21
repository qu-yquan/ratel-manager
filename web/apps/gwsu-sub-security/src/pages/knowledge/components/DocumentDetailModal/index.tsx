import React, { useEffect, useState } from 'react';
import { Alert, Button, Descriptions, Empty, Input, Modal, Select, Space, Tabs, Timeline, Typography, message } from 'antd';
import { EditOutlined, SaveOutlined } from '@ant-design/icons';
import { useAuth, useUserStore } from '@gwsu/core';
import MarkdownPreview from '../../../../components/MarkdownPreview';
import { getBlockTypeOptions, getDocumentBlocks, getEvents, getKnowledgeDocument, getMarkdown, saveMarkdownBlocks } from '../../services/knowledge';
import { PERM_KNOWLEDGE_UPLOAD } from '../../permissionConstants';
import type { KnowledgeBlockType, KnowledgeDocument, KnowledgeDocumentBlocks, KnowledgeEvent } from '../../types';
import styles from './index.module.less';

interface Props {
  documentId?: string;
  onClose: () => void;
  onUpdated: () => void;
}

const EVENT_LABELS: Record<string, string> = {
  UPLOADED: '加入知识库', PARSE_SUCCEEDED: '文件解析成功', INGEST_SUCCEEDED: '导入成功',
  INGEST_FAILED: '导入失败', RETRY_SUBMITTED: '重新导入', MARKDOWN_EDITED: 'Markdown 编辑',
  DOCUMENT_ENABLED: '文档启用', DOCUMENT_DISABLED: '文档禁用',
};
const DocumentDetailModal: React.FC<Props> = ({ documentId, onClose, onUpdated }) => {
  const isAdmin = useUserStore((state) => state.userInfo?.admin === true || state.userInfo?.roles?.includes('super_admin') === true);
  const canEdit = useAuth(PERM_KNOWLEDGE_UPLOAD) || isAdmin;
  const [document, setDocument] = useState<KnowledgeDocument>();
  const [markdown, setMarkdown] = useState('');
  const [blockData, setBlockData] = useState<KnowledgeDocumentBlocks>({ blocks: [] });
  const [draftBlocks, setDraftBlocks] = useState<KnowledgeDocumentBlocks>({ blocks: [] });
  const [blockTypes, setBlockTypes] = useState<Array<{ label: string; value: KnowledgeBlockType }>>([]);
  const [events, setEvents] = useState<KnowledgeEvent[]>([]);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!documentId) return;
    let active = true;
    setEditing(false);
    setDocument(undefined);
    setMarkdown('');
    setBlockData({ blocks: [] });
    setDraftBlocks({ blocks: [] });
    setEvents([]);
    void Promise.allSettled([getKnowledgeDocument(documentId), getMarkdown(documentId), getEvents(documentId), getDocumentBlocks(documentId), getBlockTypeOptions()])
      .then(([detail, content, logs, blocks, types]) => {
        if (!active) return;
        if (detail.status === 'fulfilled') setDocument(detail.value);
        if (content.status === 'fulfilled') setMarkdown(content.value);
        if (logs.status === 'fulfilled') setEvents(logs.value);
        if (blocks.status === 'fulfilled') setBlockData(blocks.value);
        if (types.status === 'fulfilled') setBlockTypes(types.value);
      });
    return () => { active = false; };
  }, [documentId]);

  useEffect(() => {
    if (!documentId || (document?.documentStatus !== 'UPLOADED' && document?.documentStatus !== 'PROCESSING')) return;
    let active = true;
    let timer: number;
    const poll = async () => {
      try {
        const detail = await getKnowledgeDocument(documentId);
        if (!active) return;
        const [logs, content, blocks] = await Promise.allSettled([
          getEvents(documentId),
          detail.documentStatus === 'PROCESSED' ? getMarkdown(documentId) : Promise.resolve(undefined),
          detail.documentStatus === 'PROCESSED' ? getDocumentBlocks(documentId) : Promise.resolve(undefined),
        ]);
        if (!active) return;
        if (logs.status === 'fulfilled') setEvents(logs.value);
        if (content.status === 'fulfilled' && content.value !== undefined) {
          setMarkdown(content.value);
        }
        if (blocks.status === 'fulfilled' && blocks.value !== undefined) setBlockData(blocks.value);
        setDocument(detail);
        if (detail.documentStatus !== 'UPLOADED' && detail.documentStatus !== 'PROCESSING') return;
      } catch {
        // 状态查询失败时继续下一轮，避免一次网络波动使轮询停止。
      }
      if (active) timer = window.setTimeout(() => { void poll(); }, 3000);
    };
    timer = window.setTimeout(() => { void poll(); }, 3000);
    return () => { active = false; window.clearTimeout(timer); };
  }, [documentId, document?.documentStatus]);

  const handleSave = async () => {
    if (!documentId) return;
    setSaving(true);
    try {
      await saveMarkdownBlocks(documentId, draftBlocks);
      const [content, blocks] = await Promise.all([getMarkdown(documentId), getDocumentBlocks(documentId)]);
      setMarkdown(content);
      setBlockData(blocks);
      setEditing(false);
      setEvents(await getEvents(documentId));
      message.success('Markdown 已保存并重建检索索引');
      onUpdated();
    } finally { setSaving(false); }
  };

  return <Modal title={document?.fileName ?? '文档详情'} open={!!documentId} onCancel={onClose} footer={null}
    width={1000} destroyOnHidden>
    <Tabs items={[
      { key: 'content', label: '内容', children: <Empty description="原文档预览暂未开放" className={styles.empty} /> },
      { key: 'detail', label: '详情', children: <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="文件名">{document?.fileName ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="文件大小">{document?.fileSize != null ? `${(document.fileSize / 1024 / 1024).toFixed(2)} MB` : '-'}</Descriptions.Item>
        <Descriptions.Item label="文件格式">{document?.fileFormat ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="上传时间">{document?.createTime ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="解析时间">{document?.parsedAt ?? '-'}</Descriptions.Item>
        <Descriptions.Item label="是否向量化">{document?.embeddingCompleted ? '是' : '否'}</Descriptions.Item>
        <Descriptions.Item label="图片 OCR">{document?.imageOcrParsed ? '是' : '否'}</Descriptions.Item>
        <Descriptions.Item label="状态">{document?.documentStatus ?? '-'}</Descriptions.Item>
        {document?.documentStatus === 'FAILED' && document.processMessage &&
          <Descriptions.Item label="失败原因" span={2}>{document.processMessage}</Descriptions.Item>}
      </Descriptions> },
      { key: 'markdown', label: 'Markdown', children: <div className={styles.markdownPanel}>
        <div className={styles.actions}><Space>
          {editing ? <>
            <Button onClick={() => setEditing(false)}>取消</Button>
            <Button type="primary" icon={<SaveOutlined />} loading={saving} data-ai-approval onClick={() => void handleSave()}>保存文档</Button>
          </> : canEdit && blockData.blocks.length > 0 && <Button icon={<EditOutlined />} onClick={() => {
            setDraftBlocks({ pageVersionId: blockData.pageVersionId, blocks: blockData.blocks.map((block) => ({ ...block })) });
            setEditing(true);
          }}>编辑</Button>}
        </Space></div>
        {editing ? <><Alert type="info" showIcon className={styles.blockNotice} message="按 Block 编辑已有内容和类型，保存后将重建检索索引。" />
          <div className={styles.blockList}>{draftBlocks.blocks.map((block, index) => <div key={block.id} className={styles.blockCard}>
            <div className={styles.blockHeader}><Typography.Text strong>Block #{index + 1}</Typography.Text>
              <Select className={styles.blockTypeSelect} value={block.blockType} options={blockTypes}
                onChange={(value) => setDraftBlocks((current) => ({ ...current, blocks: current.blocks.map((item) =>
                  item.id === block.id ? { ...item, blockType: value } : item) }))} /></div>
            {block.sourceLocator && <Typography.Text type="secondary">sourceLocator：{block.sourceLocator}</Typography.Text>}
            <Input.TextArea value={block.content} autoSize={{ minRows: 3, maxRows: 12 }} aria-label={`Block ${index + 1} 内容`}
              onChange={(event) => setDraftBlocks((current) => ({ ...current, blocks: current.blocks.map((item) =>
                item.id === block.id ? { ...item, content: event.target.value } : item) }))} />
          </div>)}</div></> : markdown ?
          <MarkdownPreview content={markdown} className={styles.preview} /> : <Empty description="暂无 Markdown 内容" />}
      </div> },
      { key: 'events', label: '日志', children: events.length ? <Timeline className={styles.timeline}
        items={events.map((event) => ({ color: event.eventType.includes('FAILED') ? 'red' : 'blue',
          children: <div><Typography.Text strong>{EVENT_LABELS[event.eventType] ?? event.eventType}</Typography.Text>
            <div><Typography.Text type="secondary">{event.occurredAt} · {event.createOp ?? '系统'}</Typography.Text></div>
            {event.message && <div>{event.message}</div>}
          </div> }))} /> : <Empty description="暂无日志" /> },
    ]} />
  </Modal>;
};

export default DocumentDetailModal;
