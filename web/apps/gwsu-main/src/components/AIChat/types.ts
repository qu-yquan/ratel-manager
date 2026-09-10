/**
 * AI 聊天消息类型
 */
export interface ChatMessage {
  /** 消息唯一标识 */
  id: string;
  /** 消息内容 */
  content: string;
  /** 消息发送者 */
  role: 'user' | 'assistant' | 'system';
  /** 消息发送时间 */
  timestamp: number;
  /** 是否正在加载中 */
  isLoading?: boolean;
}

/**
 * AI 聊天面板显示模式
 */
export type AIChatPanelMode = 'fixed' | 'draggable' | 'hidden';

/** AI 聊天面板可见时的显示模式 */
export type AIChatVisiblePanelMode = Exclude<AIChatPanelMode, 'hidden'>;

/**
 * AI 聊天面板位置
 */
export interface AIChatPanelPosition {
  x: number;
  y: number;
}

/**
 * AI 聊天面板状态
 */
export interface AIChatPanelState {
  /** 当前显示模式 */
  mode: AIChatPanelMode;
  /** 关闭前最后一次使用的可见模式 */
  lastVisibleMode: AIChatVisiblePanelMode;
  /** 拖拽模式下的位置 */
  position: AIChatPanelPosition;
  /** 面板宽度 */
  width: number;
  /** 面板高度（拖拽模式下） */
  height: number;
}

/**
 * AI 聊天面板上下文值
 */
export interface AIChatContextValue {
  /** 聊天消息列表 */
  messages: ChatMessage[];
  /** 发送消息 */
  sendMessage: (content: string) => void;
  /** 清空消息 */
  clearMessages: () => void;
  /** 面板状态 */
  panelState: AIChatPanelState;
  /** 设置面板模式 */
  setPanelMode: (mode: AIChatPanelMode) => void;
  /** 设置面板位置 */
  setPanelPosition: (position: AIChatPanelPosition) => void;
  /** 切换面板显示/隐藏 */
  togglePanel: () => void;
  /** 是否正在加载 */
  isLoading: boolean;
}

/**
 * AI 聊天视图模式
 */
export type AIChatViewMode = 'chat' | 'history';

/**
 * 历史会话项
 */
export interface HistorySessionItem {
  sessionId: string;
  title: string;
  messageCount: number;
  updatedAt: string;
  timeDisplay: string;
}

/**
 * 扩展后的 AI 聊天面板状态
 */
export interface AIChatPanelStateWithHistory extends AIChatPanelState {
  /** 当前视图模式 */
  viewMode: AIChatViewMode;
  /** 当前会话ID（用于加载历史会话） */
  currentThreadId: string | null;
}

/**
 * 助手展示配置
 */
export interface ViewConfig {
  /** 是否展示思考内容 */
  showThinking: boolean;
  /** 是否展示工具调用 */
  showToolCalls: boolean;
  /** 是否展示历史记录 */
  showHistory: boolean;
  /** 是否启用拖拽模式 */
  enableDragMode: boolean;
}
