import React, { useEffect, useMemo, useState } from "react";
import {
  DocxRenderer,
  MarkdownRenderer,
  PDFRenderer,
  TXTRenderer,
  XLSXRenderer,
} from "@iamjariwala/react-doc-viewer";
import DocViewer from "@iamjariwala/react-doc-viewer";
import type { IConfig } from "@iamjariwala/react-doc-viewer";
import "@iamjariwala/react-doc-viewer/dist/index.css";
import { ReloadOutlined } from "@ant-design/icons";
import { Alert, Button, Empty, Result, Space, Spin, Typography } from "antd";
import { FileDownloadButton, useThemeContext } from "@gwsu/core";
import { getKnowledgeDocumentPreview } from "../../services/knowledge";
import styles from "./index.module.less";

interface DocumentContentPreviewProps {
  fileId?: string;
  fileName?: string;
  fileFormat?: string;
  fileSize?: number;
  detailLoading?: boolean;
}

interface PreviewFormat {
  mimeType: string;
  maxSize: number;
}

const MEBIBYTE = 1024 * 1024;
const DEFAULT_MAX_PREVIEW_SIZE = 100 * MEBIBYTE;
const XLSX_MAX_PREVIEW_SIZE = 5 * MEBIBYTE;

const PREVIEW_FORMATS: Record<string, PreviewFormat> = {
  pdf: { mimeType: "application/pdf", maxSize: DEFAULT_MAX_PREVIEW_SIZE },
  docx: {
    mimeType:
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    maxSize: DEFAULT_MAX_PREVIEW_SIZE,
  },
  xlsx: {
    mimeType:
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    maxSize: XLSX_MAX_PREVIEW_SIZE,
  },
  txt: { mimeType: "text/plain", maxSize: DEFAULT_MAX_PREVIEW_SIZE },
  md: { mimeType: "text/markdown", maxSize: DEFAULT_MAX_PREVIEW_SIZE },
  markdown: { mimeType: "text/markdown", maxSize: DEFAULT_MAX_PREVIEW_SIZE },
};

const LOCAL_RENDERERS = [
  PDFRenderer,
  DocxRenderer,
  XLSXRenderer,
  TXTRenderer,
  MarkdownRenderer,
];

function resolveExtension(fileName?: string, fileFormat?: string): string {
  const nameExtension = fileName?.split(".").pop()?.toLowerCase();
  if (nameExtension && nameExtension !== fileName?.toLowerCase()) {
    return nameExtension;
  }
  return fileFormat?.replace(/^\./, "").toLowerCase() ?? "";
}

const DocumentContentPreview: React.FC<DocumentContentPreviewProps> = ({
  fileId,
  fileName,
  fileFormat,
  fileSize,
  detailLoading = false,
}) => {
  const { currentTheme } = useThemeContext();
  const [objectUrl, setObjectUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const extension = resolveExtension(fileName, fileFormat);
  const previewFormat = PREVIEW_FORMATS[extension];
  const exceedsPreviewLimit =
    previewFormat && fileSize != null && fileSize > previewFormat.maxSize;

  useEffect(() => {
    if (!fileId || !previewFormat || exceedsPreviewLimit) {
      setObjectUrl("");
      setLoading(false);
      setError("");
      return;
    }

    let active = true;
    let nextObjectUrl = "";
    setObjectUrl("");
    setError("");
    setLoading(true);

    void getKnowledgeDocumentPreview(fileId)
      .then((blob) => {
        if (!blob.size) {
          throw new Error("文件内容为空");
        }
        nextObjectUrl = URL.createObjectURL(blob);
        if (active) {
          setObjectUrl(nextObjectUrl);
        } else {
          URL.revokeObjectURL(nextObjectUrl);
        }
      })
      .catch((reason: unknown) => {
        if (active) {
          setError(
            reason instanceof Error ? reason.message : "文件内容加载失败"
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      if (nextObjectUrl) URL.revokeObjectURL(nextObjectUrl);
    };
  }, [exceedsPreviewLimit, fileId, previewFormat, reloadKey]);

  const documents = useMemo(
    () =>
      objectUrl && previewFormat
        ? [
            {
              uri: objectUrl,
              fileName: fileName ?? `文档.${extension}`,
              fileType: previewFormat.mimeType,
            },
          ]
        : [],
    [extension, fileName, objectUrl, previewFormat]
  );
  const viewerConfig = useMemo<IConfig>(
    () => ({
      header: { disableHeader: true },
      docx: { useOfficeOnlineViewer: false },
      pdfZoom: { defaultZoom: 1.3, zoomJump: 0.1 },
      loadingProgress: { enableProgressBar: true },
      password: { enablePasswordPrompt: true },
      search: { enableSearch: true },
      textSelection: { enableTextSelection: true },
      keyboard: { enableKeyboardShortcuts: true },
      themeMode: currentTheme.key === "midnight" ? "dark" : "light",
    }),
    [currentTheme.key]
  );

  const downloadButton = fileId ? (
    <FileDownloadButton fileId={fileId} fileName={fileName}>
      下载原文件
    </FileDownloadButton>
  ) : null;

  if (detailLoading) {
    return (
      <div className={styles.loading} aria-live="polite">
        <Spin size="large" />
        <Typography.Text type="secondary">正在读取文档信息…</Typography.Text>
      </div>
    );
  }

  if (!fileId) {
    return <Empty className={styles.empty} description="暂无可预览的源文件" />;
  }

  if (!previewFormat) {
    return (
      <Result
        status="info"
        title={`暂不支持在线预览 ${
          extension ? extension.toUpperCase() : "该"
        } 格式`}
        subTitle="为保护内部文档，系统不会将文件发送至第三方在线预览服务。你可以下载原文件，或在 Markdown 页签查看解析后的内容。"
        extra={downloadButton}
      />
    );
  }

  if (exceedsPreviewLimit) {
    return (
      <Result
        status="warning"
        title="文件过大，暂不在线加载"
        subTitle={`当前格式的在线预览上限为 ${
          previewFormat.maxSize / MEBIBYTE
        } MB，请下载后查看。`}
        extra={downloadButton}
      />
    );
  }

  if (loading) {
    return (
      <div className={styles.loading} aria-live="polite">
        <Spin size="large" />
        <Typography.Text type="secondary">正在加载文档内容…</Typography.Text>
      </div>
    );
  }

  if (error) {
    return (
      <Result
        status="error"
        title="文档加载失败"
        subTitle={error}
        extra={
          <Space>
            <Button
              icon={<ReloadOutlined aria-hidden="true" />}
              onClick={() => setReloadKey((current) => current + 1)}
            >
              重新加载
            </Button>
            {downloadButton}
          </Space>
        }
      />
    );
  }

  if (!documents.length) return null;

  return (
    <div className={styles.container}>
      <div className={styles.toolbar}>
        <Alert
          type="info"
          showIcon
          message="当前为只读预览，复杂排版可能与原文件存在差异。"
        />
        {downloadButton}
      </div>
      <div
        className={styles.viewer}
        aria-label={`${fileName ?? "文档"}内容预览`}
      >
        <DocViewer
          key={objectUrl}
          documents={documents}
          pluginRenderers={LOCAL_RENDERERS}
          config={viewerConfig}
        />
      </div>
    </div>
  );
};

export default DocumentContentPreview;
