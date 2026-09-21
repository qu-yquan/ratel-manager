import React, { useEffect, useMemo, useState } from 'react';
import { Form, Input, Modal, Tabs, TreeSelect, Typography, message } from 'antd';
import type { TreeSelectProps } from 'antd';
import { FileScope, FileUpload } from '@gwsu/core';
import { resolveFileName, saveDirectory, saveKnowledgeDocument } from '../../services/knowledge';
import type { KnowledgeNode } from '../../types';
import styles from './index.module.less';

const ROOT_ID = 'ROOT';
type CreateTab = 'directory' | 'document';

interface Props {
  open: boolean;
  directories: KnowledgeNode[];
  initialDirectoryId?: string;
  canCreateDirectory: boolean;
  canUpload: boolean;
  onClose: () => void;
  onCreated: () => Promise<void>;
}

function directoryOptions(nodes: KnowledgeNode[], parentId?: string): NonNullable<TreeSelectProps['treeData']> {
  return nodes.filter((node) => (node.parentId ?? undefined) === parentId).map((node) => ({
    title: node.name,
    value: node.id,
    children: directoryOptions(nodes, node.id),
  }));
}

const KnowledgeCreateModal: React.FC<Props> = ({
  open, directories, initialDirectoryId, canCreateDirectory, canUpload, onClose, onCreated,
}) => {
  const [activeTab, setActiveTab] = useState<CreateTab>('directory');
  const [directoryParentId, setDirectoryParentId] = useState(ROOT_ID);
  const [uploadParentId, setUploadParentId] = useState(ROOT_ID);
  const [fileIds, setFileIds] = useState<string[]>([]);
  const [uploading, setUploading] = useState(false);
  const [submittedIds, setSubmittedIds] = useState<string[]>([]);
  const [saving, setSaving] = useState(false);
  const [form] = Form.useForm<{ name: string }>();
  const treeData = useMemo<NonNullable<TreeSelectProps['treeData']>>(() => [{
    title: '根目录', value: ROOT_ID, children: directoryOptions(directories),
  }], [directories]);

  useEffect(() => {
    if (!open) return;
    setActiveTab(canCreateDirectory ? 'directory' : 'document');
    setDirectoryParentId(initialDirectoryId ?? ROOT_ID);
    setUploadParentId(initialDirectoryId ?? ROOT_ID);
    setFileIds([]);
    setUploading(false);
    setSubmittedIds([]);
    form.resetFields();
  }, [open, initialDirectoryId, canCreateDirectory, form]);

  const submit = async () => {
    setSaving(true);
    try {
      if (activeTab === 'directory') {
        const { name } = await form.validateFields();
        await saveDirectory({ parentId: directoryParentId === ROOT_ID ? undefined : directoryParentId, name: name.trim() });
        message.success('目录已创建');
        await onCreated();
        onClose();
        return;
      }

      const pendingIds = fileIds.filter((id) => !submittedIds.includes(id));
      if (uploading) {
        message.warning('请等待所有文件上传完成');
        return;
      }
      if (!pendingIds.length) {
        message.warning('请先选择并等待文件上传完成');
        return;
      }
      const results = await Promise.allSettled(pendingIds.map(async (fileId) => {
        const fileName = await resolveFileName(fileId);
        if (!fileName) throw new Error('无法读取上传文件名');
        await saveKnowledgeDocument({ parentId: uploadParentId === ROOT_ID ? undefined : uploadParentId, fileId, fileName });
        return fileId;
      }));
      const succeeded = results.flatMap((result) => result.status === 'fulfilled' ? [result.value] : []);
      const failed = results.length - succeeded.length;
      setSubmittedIds((current) => [...current, ...succeeded]);
      if (succeeded.length) {
        message.success(`${succeeded.length} 个文档已提交导入`);
        await onCreated();
      }
      if (failed) {
        message.error(`${failed} 个文档提交失败，请检查文件或目录权限后重试`);
      } else {
        onClose();
      }
    } finally {
      setSaving(false);
    }
  };

  return <Modal title="新建" open={open} onCancel={onClose} onOk={() => void submit()}
    okText={activeTab === 'directory' ? '创建目录' : '提交导入'} confirmLoading={saving}
    okButtonProps={{ 'data-ai-approval': 'true', disabled: uploading }} destroyOnHidden>
    <Tabs activeKey={activeTab} onChange={(key) => setActiveTab(key as CreateTab)} items={[
      ...(canCreateDirectory ? [{ key: 'directory', label: '创建目录', children: <Form form={form} layout="vertical">
        <Form.Item label="所属目录"><TreeSelect className={styles.fullWidth} value={directoryParentId} onChange={setDirectoryParentId}
          treeData={treeData} treeDefaultExpandAll showSearch treeNodeFilterProp="title" /></Form.Item>
        <Form.Item name="name" label="目录名称" rules={[{ required: true, whitespace: true, message: '请输入目录名称' }, { max: 200 }]}>
          <Input maxLength={200} placeholder="请输入目录名称" />
        </Form.Item>
      </Form> }] : []),
      ...(canUpload ? [{ key: 'document', label: '上传文档', children: <div>
        <div className={styles.fieldLabel}>所属目录</div>
        <TreeSelect className={styles.fullWidth} value={uploadParentId} onChange={setUploadParentId}
          treeData={treeData} treeDefaultExpandAll showSearch treeNodeFilterProp="title" />
        <div className={styles.uploadField}>
          <div className={styles.fieldLabel}>选择文件</div>
          <FileUpload property={{ scope: FileScope.PROTECTED, categorize: 'knowledge' }} multiple draggable
            maxSize={500 * 1024 * 1024} accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md"
            onChange={setFileIds} onUploadingChange={setUploading} />
          <Typography.Text type="secondary">可一次选择多个文件；文件上传完成后点击提交导入。</Typography.Text>
        </div>
      </div> }] : []),
    ]} />
  </Modal>;
};

export default KnowledgeCreateModal;
