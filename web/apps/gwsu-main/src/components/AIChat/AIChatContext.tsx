import {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  ReactNode,
} from 'react';
import type {
  AIChatPanelMode,
  AIChatPanelPosition,
  AIChatPanelState,
  AIChatViewMode,
  AIChatVisiblePanelMode,
  ViewConfig,
} from './types';
import { fetchConfigsBatch } from '@gwsu/core';
import type { ConfigVO } from '@gwsu/core';
import { useViewConfigStore } from '@/stores/viewConfig';

export const AI_CHAT_PANEL_STATE_STORAGE_KEY = 'gwsu-ai-chat-panel-state';
const VIEW_CONFIG_KEY = 'assistant_view_config';

const DEFAULT_PANEL_STATE: AIChatPanelState = {
  mode: 'fixed',
  lastVisibleMode: 'fixed',
  position: { x: 20, y: 80 },
  width: 420,
  height: 520,
};

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

function isVisiblePanelMode(
  mode: AIChatPanelMode | undefined,
): mode is AIChatVisiblePanelMode {
  return mode === 'fixed' || mode === 'draggable';
}

/**
 * 面板状态上下文值
 * 注意：消息管理已由 CopilotKit 处理，此上下文仅管理面板状态
 */
interface PanelContextValue {
  /** 面板状态 */
  panelState: AIChatPanelState;
  /** 设置面板模式 */
  setPanelMode: (mode: AIChatPanelMode) => void;
  /** 设置面板位置 */
  setPanelPosition: (position: AIChatPanelPosition) => void;
  /** 切换面板显示/隐藏 */
  togglePanel: () => void;
  /** 当前视图模式 */
  viewMode: AIChatViewMode;
  /** 设置视图模式 */
  setViewMode: (mode: AIChatViewMode) => void;
  /** 当前会话ID */
  currentThreadId: string | null;
  /** 设置当前会话ID */
  setCurrentThreadId: (threadId: string | null) => void;
  /** 展示配置 */
  viewConfig: ViewConfig;
}

const PanelContext = createContext<PanelContextValue | undefined>(undefined);

export const usePanelContext = (): PanelContextValue => {
  const context = useContext(PanelContext);
  if (!context) {
    throw new Error('usePanelContext must be used within PanelProvider');
  }
  return context;
};

interface PanelProviderProps {
  children: ReactNode;
}

/**
 * 面板状态管理 Provider
 * 仅管理面板的显示模式、位置、尺寸等 UI 状态
 * 消息管理由 CopilotKit 处理
 */
export const PanelProvider: React.FC<PanelProviderProps> = ({ children }) => {
  const [panelState, setPanelState] = useState<AIChatPanelState>(() => {
    // 从 localStorage 恢复面板状态
    try {
      const saved = localStorage.getItem(AI_CHAT_PANEL_STATE_STORAGE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as Partial<AIChatPanelState>;
        const lastVisibleMode = isVisiblePanelMode(parsed.lastVisibleMode)
          ? parsed.lastVisibleMode
          : isVisiblePanelMode(parsed.mode)
          ? parsed.mode
          : DEFAULT_PANEL_STATE.lastVisibleMode;

        return { ...DEFAULT_PANEL_STATE, ...parsed, lastVisibleMode };
      }
    } catch {
      // ignore
    }
    return DEFAULT_PANEL_STATE;
  });

  // 视图模式状态
  const [viewMode, setViewMode] = useState<AIChatViewMode>('chat');
  // 当前会话ID
  const [currentThreadId, setCurrentThreadId] = useState<string | null>(null);
  // 展示配置（从 store 读取，与 CopilotKitProvider 共享）
  const viewConfig = useViewConfigStore();

  // 加载展示配置并同步到 store
  useEffect(() => {
    fetchConfigsBatch([VIEW_CONFIG_KEY])
      .then((configMap) => {
        const info = configMap[VIEW_CONFIG_KEY] as ConfigVO | undefined;
        if (info?.configValue) {
          try {
            const parsed = JSON.parse(info.configValue) as Partial<ViewConfig>;
            useViewConfigStore
              .getState()
              .setViewConfig({ ...DEFAULT_VIEW_CONFIG, ...parsed });
          } catch {
            // 解析失败使用默认值
          }
        }
      })
      .catch(() => {
        // error handled by request util
      });
  }, []);

  // 保存面板状态到 localStorage
  useEffect(() => {
    localStorage.setItem(
      AI_CHAT_PANEL_STATE_STORAGE_KEY,
      JSON.stringify(panelState),
    );
  }, [panelState]);

  // 设置面板模式
  const setPanelMode = useCallback((mode: AIChatPanelMode) => {
    setPanelState((prev) => ({
      ...prev,
      mode,
      lastVisibleMode:
        mode === 'hidden'
          ? isVisiblePanelMode(prev.mode)
            ? prev.mode
            : prev.lastVisibleMode
          : mode,
    }));
  }, []);

  // 设置面板位置
  const setPanelPosition = useCallback((position: AIChatPanelPosition) => {
    setPanelState((prev) => ({ ...prev, position }));
  }, []);

  // 切换面板显示/隐藏，重新打开时恢复关闭前的可见模式
  const togglePanel = useCallback(() => {
    setPanelState((prev) => {
      if (prev.mode === 'hidden') {
        return { ...prev, mode: prev.lastVisibleMode };
      }

      return { ...prev, mode: 'hidden', lastVisibleMode: prev.mode };
    });
  }, []);

  const contextValue: PanelContextValue = {
    panelState,
    setPanelMode,
    setPanelPosition,
    togglePanel,
    viewMode,
    setViewMode,
    currentThreadId,
    setCurrentThreadId,
    viewConfig,
  };

  return (
    <PanelContext.Provider value={contextValue}>
      {children}
    </PanelContext.Provider>
  );
};

// 保持向后兼容的导出
export const AIChatProvider = PanelProvider;
export const useAIChatContext = usePanelContext;
