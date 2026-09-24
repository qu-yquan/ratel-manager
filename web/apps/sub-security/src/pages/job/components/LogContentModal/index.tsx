import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Modal, Button, Space, Typography, Spin } from 'antd';
import { getLogContent, getLogDetail } from '../../services/job';
import type { LogData } from '../../types';

interface LogContentModalProps {
  visible: boolean;
  logId: string;
  onClose: () => void;
}

const LogContentModal: React.FC<LogContentModalProps> = ({ visible, logId, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [logContent, setLogContent] = useState('');
  const [isEnd, setIsEnd] = useState(false);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const contentRef = useRef<HTMLPreElement>(null);
  const fromLineRef = useRef(1);
  const loadingRef = useRef(false);

  const fetchLog = useCallback(async () => {
    if (!logId || loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const requestFromLineNum = fromLineRef.current;
      const data: LogData | null = await getLogContent(logId, requestFromLineNum);
      if (data) {
        const nextLineNum = data.toLineNum >= requestFromLineNum ? data.toLineNum + 1 : requestFromLineNum;
        const hasNewContent = Boolean(data.logContent && data.toLineNum >= requestFromLineNum);
        if (hasNewContent) {
          setLogContent((prev) => prev + (prev ? '\n' : '') + data.logContent);
          fromLineRef.current = nextLineNum;
        }
        if (data.isEnd) {
          setIsEnd(true);
        } else if (!hasNewContent) {
          const detail = await getLogDetail(logId);
          if (detail?.handleCode !== 0) {
            setIsEnd(true);
          }
        }
      }
    } catch {
      // ignore
    } finally {
      loadingRef.current = false;
      setLoading(false);
    }
  }, [logId]);

  useEffect(() => {
    if (visible && logId) {
      setLogContent('');
      setIsEnd(false);
      fromLineRef.current = 1;
      fetchLog();
    }
    return () => {
      if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
    };
  }, [visible, logId]);

  useEffect(() => {
    if (visible && !isEnd && logId) {
      timerRef.current = setInterval(() => { fetchLog(); }, 3000);
    } else if (timerRef.current) {
      clearInterval(timerRef.current);
      timerRef.current = null;
    }
    return () => {
      if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
    };
  }, [visible, isEnd, logId, fetchLog]);

  useEffect(() => {
    if (contentRef.current) {
      contentRef.current.scrollTop = contentRef.current.scrollHeight;
    }
  }, [logContent]);

  return (
    <Modal
      title="执行日志"
      open={visible}
      onCancel={onClose}
      width={800}
      footer={[
        <Button key="refresh" onClick={() => fetchLog()} loading={loading}>
          刷新
        </Button>,
        <Button key="close" onClick={onClose}>
          关闭
        </Button>,
      ]}
      destroyOnHidden
    >
      <pre
        ref={contentRef}
        style={{
          background: "#1e1e1e",
          color: "#d4d4d4",
          padding: 16,
          borderRadius: 6,
          maxHeight: 500,
          overflow: "auto",
          fontSize: 12,
          fontFamily: "monospace",
          whiteSpace: "pre-wrap",
          wordBreak: "break-all",
        }}
      >
        {logContent || "暂无日志"}
      </pre>
      <div style={{ marginTop: 8, textAlign: "right" }}>
        {isEnd ? (
          <Typography.Text type="success">日志已加载完毕</Typography.Text>
        ) : (
          <Space>
            <Spin size="small" />
            <Typography.Text type="secondary">日志加载中...</Typography.Text>
          </Space>
        )}
      </div>
    </Modal>
  );
};

export default LogContentModal;
