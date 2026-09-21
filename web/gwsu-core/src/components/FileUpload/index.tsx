import React, { useCallback, useEffect, useRef } from 'react';
import { Upload, Progress, message } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadProps, RcFile } from 'antd/es/upload';
import { uploadFile } from '../../utils/fileUpload';
import type { FileProperty, FileUploadOptions, ChunkUploadProgress, KitFileInfoVO } from '../../types';
import { registerAiUploadTarget } from './aiUploadRegistry';

const { Dragger } = Upload;

export interface FileUploadProps {
  property?: FileProperty;
  multiple?: boolean;
  maxCount?: number;
  accept?: string;
  maxSize?: number;
  disabled?: boolean;
  listType?: 'text' | 'picture' | 'picture-card';
  draggable?: boolean;
  value?: string[];
  onChange?: (fileIds: string[]) => void;
  onUploadingChange?: (uploading: boolean) => void;
  uploadOptions?: FileUploadOptions;
}

interface FileItem {
  uid: string;
  name: string;
  status: 'uploading' | 'done' | 'error';
  percent: number;
  fileId?: string;
}

function createUploadId(): string {
  const timePart = Date.now().toString(36);
  const randomPart = Math.random().toString(36).slice(2, 4);
  return `u${timePart}${randomPart}`;
}

export const FileUpload: React.FC<FileUploadProps> = ({
  property,
  multiple = false,
  maxCount,
  accept,
  maxSize,
  disabled = false,
  listType = 'text',
  draggable = false,
  value,
  onChange,
  onUploadingChange,
  uploadOptions,
}) => {
  const [fileList, setFileList] = React.useState<FileItem[]>([]);
  const [progressMap, setProgressMap] = React.useState<Record<string, ChunkUploadProgress>>({});
  const uploadIdRef = useRef<string>(createUploadId());

  useEffect(() => {
    onUploadingChange?.(fileList.some((item) => item.status === 'uploading'));
  }, [fileList, onUploadingChange]);

  const getMaxAllowed = useCallback(() => {
    if (!multiple) {
      return 1;
    }
    return maxCount ?? Number.POSITIVE_INFINITY;
  }, [maxCount, multiple]);

  const normalizeFileList = useCallback(
    (items: FileItem[]) => {
      const maxAllowed = getMaxAllowed();
      if (!Number.isFinite(maxAllowed) || items.length <= maxAllowed) {
        return items;
      }
      return items.slice(items.length - maxAllowed);
    },
    [getMaxAllowed],
  );

  const collectFileIds = useCallback((items: FileItem[]) => items.filter((item) => item.fileId).map((item) => item.fileId!), []);

  const triggerChange = useCallback(
    (ids: string[]) => {
      onChange?.(ids);
    },
    [onChange],
  );

  const attachFileInfo = useCallback(
    (fileInfo: KitFileInfoVO) => {
      setFileList((prev) => {
        const nextItem: FileItem = {
          uid: fileInfo.fileId,
          name: fileInfo.fileName,
          status: 'done',
          percent: 100,
          fileId: fileInfo.fileId,
        };
        const withoutSameFile = prev.filter((item) => item.fileId !== fileInfo.fileId);
        const next = normalizeFileList([...withoutSameFile, nextItem]);
        triggerChange(collectFileIds(next));
        return next;
      });
    },
    [collectFileIds, normalizeFileList, triggerChange],
  );

  useEffect(() => registerAiUploadTarget({
    uploadId: uploadIdRef.current,
    property,
    attachFile: attachFileInfo,
  }), [attachFileInfo, property]);

  const customUpload = useCallback(
    async (options: any) => {
      const { file, onSuccess, onError, onProgress } = options;
      const uid = file.uid;

      try {
        const result = await uploadFile(file, {
          property,
          ...uploadOptions,
          onProgress: (p: ChunkUploadProgress) => {
            setProgressMap((prev) => ({ ...prev, [uid]: p }));
            onProgress?.({ percent: p.percent }, new XMLHttpRequest());
          },
        });

        setFileList((prev) =>
          {
            const next = normalizeFileList(
              prev.map((f) => (f.uid === uid ? { ...f, status: 'done' as const, percent: 100, fileId: result.fileId } : f)),
            );
            triggerChange(collectFileIds(next));
            return next;
          },
        );

        onSuccess?.(result, new XMLHttpRequest());
      } catch (error) {
        setFileList((prev) =>
          prev.map((f) => (f.uid === uid ? { ...f, status: 'error' as const } : f)),
        );
        onError?.(error as Error);
        message.error(`文件 ${file.name} 上传失败: ${(error as Error).message}`);
      }
    },
    [collectFileIds, normalizeFileList, property, triggerChange, uploadOptions],
  );

  const handleBeforeUpload = useCallback(
    (file: RcFile) => {
      if (maxSize && file.size > maxSize) {
        message.error(`文件 ${file.name} 超过大小限制 ${Math.round(maxSize / 1024 / 1024)}MB`);
        return false;
      }
      setFileList((prev) => normalizeFileList([
        ...prev,
        { uid: file.uid, name: file.name, status: 'uploading', percent: 0 },
      ]));
      return true;
    },
    [maxSize, normalizeFileList],
  );

  const handleRemove = useCallback(
    (file: any) => {
      setFileList((prev) => {
        const next = prev.filter((f) => f.uid !== file.uid);
        triggerChange(collectFileIds(next));
        return next;
      });
    },
    [collectFileIds, triggerChange],
  );

  const uploadProps: UploadProps = {
    multiple,
    maxCount,
    accept,
    disabled,
    listType,
    customRequest: customUpload,
    beforeUpload: handleBeforeUpload,
    onRemove: handleRemove,
    fileList: fileList.map((f) => ({
      uid: f.uid,
      name: f.name,
      status: f.status,
      percent: f.percent,
    })),
    itemRender: (originNode, file) => {
      const p = progressMap[file.uid];
      return (
        <div>
          {originNode}
          {p && p.status === 'uploading' && p.totalChunks > 1 && (
            <div style={{ marginTop: 4 }}>
              <Progress percent={p.percent} size="small" />
              <div style={{ fontSize: 12, color: '#999', marginTop: 2 }}>
                {p.uploadedChunks}/{p.totalChunks} 分片已上传
              </div>
            </div>
          )}
        </div>
      );
    },
  };

  if (draggable) {
    return (
      <div data-ai-upload-id={uploadIdRef.current}>
        <Dragger {...uploadProps}>
          <p className="ant-upload-drag-icon">
            <InboxOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          {maxSize && (
            <p className="ant-upload-hint">单个文件不超过 {Math.round(maxSize / 1024 / 1024)}MB</p>
          )}
        </Dragger>
      </div>
    );
  }

  return (
    <div data-ai-upload-id={uploadIdRef.current}>
      <Upload {...uploadProps}>{fileList.length >= (maxCount ?? Infinity) ? null : <a>上传文件</a>}</Upload>
    </div>
  );
};
