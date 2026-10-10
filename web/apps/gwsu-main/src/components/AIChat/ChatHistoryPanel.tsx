import { useState, useEffect, useCallback, useRef } from 'react';
import { Modal, Spin } from 'antd';
import {
  ArrowLeftOutlined,
  HistoryOutlined,
  MessageOutlined,
  DeleteOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import {
  getHistorySessions,
  deleteSession,
  type BrainHistorySession,
} from '@/services/brain';
import styles from './history.module.less';

interface ChatHistoryPanelProps {
  /** 切换到聊天视图 */
  onBackToChat: () => void;
  /** 加载会话消息 */
  onLoadSession: (sessionId: string) => void;
}

/**
 * 历史记录面板组件
 */
export function ChatHistoryPanel({
  onBackToChat,
  onLoadSession,
}: ChatHistoryPanelProps) {
  // 历史会话列表
  const [sessions, setSessions] = useState<BrainHistorySession[]>([]);
  // 加载状态
  const [loading, setLoading] = useState(true);
  // 是否有更多数据
  const [hasMore, setHasMore] = useState(true);
  // 当前页码
  const [pageNum, setPageNum] = useState(1);
  // 删除确认弹窗
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deletingSessionId, setDeletingSessionId] = useState<string | null>(
    null,
  );

  // 列表容器引用（用于滚动加载）
  const listRef = useRef<HTMLDivElement>(null);

  // 加载历史会话列表
  const loadSessions = useCallback(
    async (page: number, append: boolean = false) => {
      try {
        setLoading(true);
        const result = await getHistorySessions(page, 20);

        if (append) {
          setSessions((prev) => [...prev, ...result.records]);
        } else {
          setSessions(result.records);
        }

        setHasMore(Boolean(result.hasMore));
        setPageNum(result.nextPageNum ?? page + 1);
      } catch (error) {
        console.error('加载历史会话失败:', error);
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  // 初始加载
  useEffect(() => {
    loadSessions(1);
  }, [loadSessions]);

  // 滚动加载更多
  const handleScroll = useCallback(() => {
    if (!listRef.current || loading || !hasMore) return;

    const { scrollTop, scrollHeight, clientHeight } = listRef.current;
    if (scrollHeight - scrollTop - clientHeight < 100) {
      loadSessions(pageNum, true);
    }
  }, [loading, hasMore, pageNum, loadSessions]);

  // 点击会话项
  const handleSessionClick = useCallback(
    (sessionId: string) => {
      onLoadSession(sessionId);
      onBackToChat();
    },
    [onLoadSession, onBackToChat],
  );

  // 显示删除确认
  const showDeleteConfirm = useCallback(
    (sessionId: string, e: React.MouseEvent) => {
      e.stopPropagation();
      setDeletingSessionId(sessionId);
      setDeleteModalVisible(true);
    },
    [],
  );

  // 确认删除
  const confirmDelete = useCallback(async () => {
    if (!deletingSessionId) return;

    try {
      await deleteSession(deletingSessionId);
      setSessions((prev) =>
        prev.filter((s) => s.sessionId !== deletingSessionId),
      );
    } catch (error) {
      console.error('删除会话失败:', error);
    } finally {
      setDeleteModalVisible(false);
      setDeletingSessionId(null);
    }
  }, [deletingSessionId]);

  return (
    <div className={styles.historyPanel}>
      {/* 头部 */}
      <div className={styles.historyHeader}>
        <button className={styles.backButton} onClick={onBackToChat}>
          <ArrowLeftOutlined />
        </button>
        <span className={styles.historyTitle}>历史记录</span>
      </div>

      {/* 列表区域 */}
      <div className={styles.historyList} ref={listRef} onScroll={handleScroll}>
        {/* 加载状态 */}
        {loading && sessions.length === 0 && (
          <div className={styles.loadingState}>
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className={styles.loadingItem} />
            ))}
          </div>
        )}

        {/* 空状态 */}
        {!loading && sessions.length === 0 && (
          <div className={styles.emptyState}>
            <HistoryOutlined className={styles.emptyIcon} />
            <p className={styles.emptyText}>
              暂无历史记录
              <br />
              开始新的对话吧
            </p>
          </div>
        )}

        {/* 会话列表 */}
        {sessions.map((session) => (
          <div
            key={session.sessionId}
            className={styles.historyItem}
            onClick={() => handleSessionClick(session.sessionId)}
          >
            <div className={styles.historyItemContent}>
              <div className={styles.historyItemTitle}>{session.title}</div>
              <div className={styles.historyItemMeta}>
                <span className={styles.historyItemTime}>
                  <ClockCircleOutlined />
                  {session.timeDisplay}
                </span>
                <span className={styles.historyItemCount}>
                  <MessageOutlined />
                  {session.messageCount} 条
                </span>
              </div>
            </div>
            <button
              className={styles.deleteButton}
              onClick={(e) => showDeleteConfirm(session.sessionId, e)}
            >
              <DeleteOutlined />
            </button>
          </div>
        ))}

        {/* 加载更多 */}
        {loading && sessions.length > 0 && (
          <div className={styles.loadMore}>
            <Spin size="small" />
          </div>
        )}
      </div>

      {/* 删除确认弹窗 */}
      <Modal
        title="删除确认"
        open={deleteModalVisible}
        onOk={confirmDelete}
        onCancel={() => setDeleteModalVisible(false)}
        okText="删除"
        cancelText="取消"
        okButtonProps={{ danger: true }}
      >
        <p>确定要删除这条历史记录吗？删除后无法恢复。</p>
      </Modal>
    </div>
  );
}
