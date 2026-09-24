import React, { useState } from 'react';
import { Card, Space, Divider, Input, Button, App, Progress, Alert, Tag, Descriptions } from 'antd';
import { UploadOutlined, DownloadOutlined, DeleteOutlined, FileOutlined } from '@ant-design/icons';
import { FileUpload, FileDownloadButton, useFileUpload, useFileDownload, removeFile, getFileInfo, FileScope } from '@gwsu/core';
import type { KitFileInfoVO } from '@gwsu/core';
import styles from './index.module.less';

const FileTestPage: React.FC = () => {
  const { message: msgApi } = App.useApp();

  const [hookUploadResult, setHookUploadResult] = useState<KitFileInfoVO | null>(null);

  const [downloadFileId, setDownloadFileId] = useState('');
  const [deleteFileId, setDeleteFileId] = useState('');
  const [infoFileId, setInfoFileId] = useState('');
  const [fileInfo, setFileInfo] = useState<KitFileInfoVO | null>(null);
  const [infoLoading, setInfoLoading] = useState(false);

  const [componentFileIds, setComponentFileIds] = useState<string[]>([]);

  const { upload: hookUpload, progress: uploadProgress, abort: abortUpload } = useFileUpload({ scope: FileScope.PROTECTED });
  const { download: hookDownload, progress: downloadProgress, abort: abortDownload } = useFileDownload();

  const handleHookUpload = async () => {
    const input = document.createElement('input');
    input.type = 'file';
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      try {
        const result = await hookUpload(file);
        setHookUploadResult(result);
        msgApi.success(`上传成功: ${result.fileName} (ID: ${result.fileId})`);
      } catch {
        msgApi.error('上传失败');
      }
    };
    input.click();
  };

  const handleHookDownload = async () => {
    if (!downloadFileId.trim()) {
      msgApi.warning('请输入文件ID');
      return;
    }
    try {
      await hookDownload(downloadFileId.trim());
      msgApi.success('下载完成');
    } catch {
      msgApi.error('下载失败');
    }
  };

  const handleDelete = async () => {
    if (!deleteFileId.trim()) {
      msgApi.warning('请输入文件ID');
      return;
    }
    try {
      await removeFile(deleteFileId.trim());
      msgApi.success('删除成功');
      setDeleteFileId('');
    } catch {
      msgApi.error('删除失败');
    }
  };

  const handleGetInfo = async () => {
    if (!infoFileId.trim()) {
      msgApi.warning('请输入文件ID');
      return;
    }
    setInfoLoading(true);
    try {
      const res = await getFileInfo(infoFileId.trim());
      setFileInfo(res.data);
    } catch {
      msgApi.error('获取文件信息失败');
      setFileInfo(null);
    } finally {
      setInfoLoading(false);
    }
  };

  return (
    <div className={styles.fileTestPage}>
      <h2 className={styles.pageTitle}>文件上传下载功能测试</h2>

      <Card title="1. 组件方式 - FileUpload 上传" className={styles.section}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert type="info" message="使用 FileUpload 组件，支持自动分片上传、拖拽上传、进度展示" />
          <div>
            <h4>普通上传</h4>
            <FileUpload
              property={{ scope: FileScope.PROTECTED, categorize: 'test' }}
              multiple
              maxCount={5}
              maxSize={500 * 1024 * 1024}
              onChange={(ids) => {
                setComponentFileIds(ids);
                msgApi.info(`当前已上传文件ID: ${ids.join(', ')}`);
              }}
            />
          </div>
          <Divider />
          <div>
            <h4>拖拽上传</h4>
            <FileUpload
              property={{ scope: FileScope.PROTECTED, categorize: 'test-drag' }}
              draggable
              maxSize={500 * 1024 * 1024}
              onChange={(ids) => msgApi.info(`拖拽上传完成: ${ids.join(', ')}`)}
            />
          </div>
          {componentFileIds.length > 0 && (
            <div>
              <Tag color="blue">已上传文件ID:</Tag>
              {componentFileIds.map((id) => (
                <Tag key={id} color="green">{id}</Tag>
              ))}
            </div>
          )}
        </Space>
      </Card>

      <Card title="2. Hook 方式 - useFileUpload / useFileDownload" className={styles.section}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert type="info" message="使用 Hook 编程式调用，适合自定义 UI 场景" />

          <div className={styles.hookRow}>
            <Button type="primary" icon={<UploadOutlined />} onClick={handleHookUpload}>
              Hook 上传文件
            </Button>
            <Button danger onClick={abortUpload} disabled={!uploadProgress || uploadProgress.status !== 'uploading'}>
              取消上传
            </Button>
          </div>

          {uploadProgress && (
            <div className={styles.progressBox}>
              <div>文件: {uploadProgress.fileName}</div>
              <Progress percent={uploadProgress.percent} status={uploadProgress.status === 'error' ? 'exception' : uploadProgress.status === 'success' ? 'success' : 'active'} />
              {uploadProgress.totalChunks > 1 && (
                <div style={{ fontSize: 12, color: '#999' }}>
                  分片: {uploadProgress.uploadedChunks}/{uploadProgress.totalChunks}
                </div>
              )}
              <Tag color={uploadProgress.status === 'success' ? 'green' : uploadProgress.status === 'error' ? 'red' : 'blue'}>
                {uploadProgress.status}
              </Tag>
            </div>
          )}

          {hookUploadResult && (
            <Descriptions size="small" bordered column={1}>
              <Descriptions.Item label="文件ID">{hookUploadResult.fileId}</Descriptions.Item>
              <Descriptions.Item label="文件名">{hookUploadResult.fileName}</Descriptions.Item>
              <Descriptions.Item label="大小">{hookUploadResult.fileSize}</Descriptions.Item>
              <Descriptions.Item label="后缀">{hookUploadResult.fileSuffix}</Descriptions.Item>
              <Descriptions.Item label="MD5">{hookUploadResult.uniqueId}</Descriptions.Item>
            </Descriptions>
          )}

          <Divider />

          <div className={styles.hookRow}>
            <Input
              placeholder="输入文件ID"
              value={downloadFileId}
              onChange={(e) => setDownloadFileId(e.target.value)}
              style={{ width: 300 }}
            />
            <Button type="primary" icon={<DownloadOutlined />} onClick={handleHookDownload}>
              Hook 下载
            </Button>
            <Button danger onClick={abortDownload} disabled={!downloadProgress || downloadProgress.status !== 'downloading'}>
              取消下载
            </Button>
          </div>

          {downloadProgress && (
            <div className={styles.progressBox}>
              <div>文件: {downloadProgress.fileName}</div>
              <Progress percent={downloadProgress.percent} status={downloadProgress.status === 'error' ? 'exception' : downloadProgress.status === 'success' ? 'success' : 'active'} />
              {downloadProgress.totalChunks > 1 && (
                <div style={{ fontSize: 12, color: '#999' }}>
                  分片: {downloadProgress.downloadedChunks}/{downloadProgress.totalChunks}
                </div>
              )}
              <Tag color={downloadProgress.status === 'success' ? 'green' : downloadProgress.status === 'error' ? 'red' : 'blue'}>
                {downloadProgress.status}
              </Tag>
            </div>
          )}
        </Space>
      </Card>

      <Card title="3. FileDownloadButton 组件" className={styles.section}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <Alert type="info" message="使用 FileDownloadButton 组件，自动判断单点/分片下载" />
          <Space>
            <FileDownloadButton fileId="test-file-id-1" fileName="测试文件1.pdf">
              下载测试文件1
            </FileDownloadButton>
            <FileDownloadButton fileId="test-file-id-2" fileName="测试文件2.docx" disabled>
              下载测试文件2（禁用）
            </FileDownloadButton>
          </Space>
        </Space>
      </Card>

      <Card title="4. 文件管理（查询/删除）" className={styles.section}>
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div className={styles.hookRow}>
            <Input
              placeholder="输入文件ID查询信息"
              value={infoFileId}
              onChange={(e) => setInfoFileId(e.target.value)}
              style={{ width: 300 }}
            />
            <Button type="primary" icon={<FileOutlined />} onClick={handleGetInfo} loading={infoLoading}>
              查询文件信息
            </Button>
          </div>

          {fileInfo && (
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="文件ID">{fileInfo.fileId}</Descriptions.Item>
              <Descriptions.Item label="文件名">{fileInfo.fileName}</Descriptions.Item>
              <Descriptions.Item label="大小">{fileInfo.fileSize}</Descriptions.Item>
              <Descriptions.Item label="后缀">{fileInfo.fileSuffix}</Descriptions.Item>
              <Descriptions.Item label="MD5">{fileInfo.uniqueId}</Descriptions.Item>
              <Descriptions.Item label="作用域">{fileInfo.scope}</Descriptions.Item>
              <Descriptions.Item label="存储类型">{fileInfo.uploadServiceType}</Descriptions.Item>
              <Descriptions.Item label="媒体类型">{fileInfo.mediaType}</Descriptions.Item>
              <Descriptions.Item label="一次性">{fileInfo.disposable ? '是' : '否'}</Descriptions.Item>
              <Descriptions.Item label="文件URL">{fileInfo.fileUrl}</Descriptions.Item>
            </Descriptions>
          )}

          <Divider />

          <div className={styles.hookRow}>
            <Input
              placeholder="输入文件ID删除"
              value={deleteFileId}
              onChange={(e) => setDeleteFileId(e.target.value)}
              style={{ width: 300 }}
            />
            <Button danger icon={<DeleteOutlined />} onClick={handleDelete} data-ai-approval>
              删除文件
            </Button>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default FileTestPage;
