import {
  CopilotChat,
  CopilotChatConfigurationProvider,
  type AttachmentsConfig,
  useCopilotKit,
} from '@copilotkit/react-core/v2';
import { useAgent } from '@copilotkit/react-core/v2';
import '@copilotkit/react-core/v2/styles.css';
import type { InputContent } from '@ag-ui/core';
import { App, Button, Tooltip } from 'antd';
import {
  RobotOutlined,
  CompressOutlined,
  DragOutlined,
  CloseOutlined,
  HistoryOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { useRef, useState, useCallback, useEffect, useMemo } from 'react';
import { createPortal } from 'react-dom';
import Draggable, { DraggableEvent, DraggableData } from 'react-draggable';
import type {
  ButtonHTMLAttributes,
  CSSProperties,
  MouseEventHandler,
} from 'react';
import {
  fetchConfigsBatch,
  FileScope,
  useHeadlessStore,
  useFileUpload,
  useProjectConfigStore,
} from '@gwsu/core';
import type { AIChatPanelMode } from './types';
import { usePanelContext } from './AIChatContext';
import { ChatHistoryPanel } from './ChatHistoryPanel';
import { HumanApprovalBar } from './HumanApprovalBar';
import { AskUserQuestionBar } from './AskUserQuestionBar';
import { AiModeControlBar } from './AiModeControlBar';
import { HeadlessSubmitBar } from './HeadlessSubmitBar';
import { createCustomRenderMessage } from './CustomRenderMessage';
import {
  getSessionMessages,
  getApprovalStatus,
  type BrainMessage,
} from '@/services/brain';
import {
  dispatchHumanApproval,
  clearHumanApproval,
  onHumanApproval,
} from '@/services/human-approval';
import {
  dispatchAskUserQuestion,
  clearAskUserQuestion,
  onAskUserQuestion,
} from '@/services/ask-user-question';
import { clearAgentOutput } from '@/services/agent-output';
import type { ModelLlmConfig } from '../SettingsPanel/ModelConfigTab/types';
import { createDefaultModelLlmConfig } from '../SettingsPanel/ModelConfigTab/types';
import styles from './copilot-override.module.less';

const MODEL_LLM_CONFIG_KEY = 'model_llm_config';

interface HeadlessBridgeResource {
  fileId?: string;
  fileName?: string;
  url?: string;
  mimeType?: string;
}

interface HeadlessBridgePayload {
  text?: string;
  resources?: HeadlessBridgeResource[];
  threadId?: string;
}

declare global {
  interface Window {
    __GWSU_HEADLESS_CHAT__?: {
      send: (payload: HeadlessBridgePayload) => void;
    };
  }
}

interface DirectUploadButtonProps
  extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onClick'> {
  onAddFile?: () => void;
  onClick?: MouseEventHandler<HTMLButtonElement>;
  toolsMenu?: unknown;
}

function DirectUploadButton(props: DirectUploadButtonProps) {
  const {
    className,
    disabled,
    onAddFile,
    onClick,
    toolsMenu: _toolsMenu,
    ...restProps
  } = props;

  return (
    <button
      type="button"
      data-testid="copilot-add-menu-button"
      className={className}
      disabled={disabled}
      onClick={(event) => {
        onClick?.(event);
        if (!disabled) {
          onAddFile?.();
        }
      }}
      {...restProps}
    >
      <PlusOutlined style={{ fontSize: 16 }} />
    </button>
  );
}

function trimTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '');
}

function toAcceptValue(formats?: string[]): string {
  if (!formats?.length) {
    return '*/*';
  }
  return formats.join(',');
}

function createOneMonthLaterIsoString(): string {
  const expiredAt = new Date();
  expiredAt.setMonth(expiredAt.getMonth() + 1);
  const year = expiredAt.getFullYear();
  const month = String(expiredAt.getMonth() + 1).padStart(2, '0');
  const day = String(expiredAt.getDate()).padStart(2, '0');
  const hours = String(expiredAt.getHours()).padStart(2, '0');
  const minutes = String(expiredAt.getMinutes()).padStart(2, '0');
  const seconds = String(expiredAt.getSeconds()).padStart(2, '0');
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function buildHeadlessInputContent(
  payload: HeadlessBridgePayload,
): InputContent[] {
  const content: InputContent[] = [];
  const text = payload.text?.trim();
  if (text) {
    content.push({
      type: 'text',
      text,
    });
  }
  for (const resource of payload.resources ?? []) {
    if (!resource?.url?.trim()) {
      throw new Error('Headless 资源缺少 url');
    }
    const mimeType = resource.mimeType?.trim();
    if (!mimeType) {
      throw new Error('Headless 资源缺少 mimeType');
    }
    const source = {
      type: 'url' as const,
      value: resource.url.trim(),
      mimeType,
    };
    const metadata = {
      ...(resource.fileId?.trim()
        ? { fileId: resource.fileId.trim(), id: resource.fileId.trim() }
        : {}),
      ...(resource.fileName?.trim()
        ? {
            fileName: resource.fileName.trim(),
            filename: resource.fileName.trim(),
          }
        : {}),
    };
    if (mimeType?.startsWith('image/')) {
      content.push({ type: 'image', source, metadata });
    } else if (mimeType?.startsWith('audio/')) {
      content.push({ type: 'audio', source, metadata });
    } else if (mimeType?.startsWith('video/')) {
      content.push({ type: 'video', source, metadata });
    } else {
      content.push({ type: 'document', source, metadata });
    }
  }
  return content;
}

function parseModelLlmConfig(configValue?: string): ModelLlmConfig {
  const defaults = createDefaultModelLlmConfig();
  if (!configValue) {
    return defaults;
  }

  try {
    const parsed = JSON.parse(configValue) as Partial<ModelLlmConfig>;
    return {
      provider: parsed.provider || defaults.provider,
      supportMultimodal: parsed.supportMultimodal ?? defaults.supportMultimodal,
      multimodalOptions: {
        ...defaults.multimodalOptions,
        ...parsed.multimodalOptions,
      },
      dashscope: { ...defaults.dashscope, ...parsed.dashscope },
      openai: { ...defaults.openai, ...parsed.openai },
      gemini: { ...defaults.gemini, ...parsed.gemini },
      anthropic: { ...defaults.anthropic, ...parsed.anthropic },
      generateOptions: {
        ...defaults.generateOptions,
        ...parsed.generateOptions,
        additionalBodyParams:
          parsed.generateOptions?.additionalBodyParams ?? {},
      },
    };
  } catch {
    return defaults;
  }
}

interface CopilotChatPanelProps {
  /** 自定义类名 */
  className?: string;
  /** 固定模式下的宽度 */
  fixedWidth?: string;
  /** 自定义样式 */
  style?: CSSProperties;
  /** 当前面板模式 */
  mode?: AIChatPanelMode;
  /** 切换到固定模式 */
  onFixed?: () => void;
  /** 切换到拖拽模式 */
  onDraggable?: () => void;
  /** 隐藏面板 */
  onHide?: () => void;
}

/**
 * CopilotChat 组件封装
 * 使用 CopilotKit 的 CopilotChat 组件，支持自定义样式和 header
 * 内部处理拖拽逻辑，避免模式切换时重新初始化
 */
export function CopilotChatPanel({
  className,
  style,
  mode = 'fixed',
  onFixed,
  onDraggable,
  onHide,
}: CopilotChatPanelProps) {
  const {
    viewMode,
    setViewMode,
    panelState,
    setPanelPosition,
    currentThreadId,
    setCurrentThreadId,
    viewConfig,
  } = usePanelContext();
  const { agent } = useAgent({ agentId: 'brain' });
  const { copilotkit } = useCopilotKit();
  const { message } = App.useApp();
  const apiBaseUrl = useProjectConfigStore((s) => s.baseUrlConfig.apiBaseUrl);
  const { upload } = useFileUpload({ scope: FileScope.PUBLIC });
  const [llmConfig, setLlmConfig] = useState<ModelLlmConfig>(
    createDefaultModelLlmConfig(),
  );

  // 根据展示配置动态创建消息渲染组件
  const CustomMessageView = useMemo(
    () => createCustomRenderMessage(viewConfig),
    [viewConfig],
  );

  // CopilotChat 组件容器 ref，用于精确控制其内部输入框
  const copilotChatRef = useRef<HTMLDivElement>(null);
  const selectedAttachmentCountRef = useRef(0);
  const pendingAttachmentCountRef = useRef(0);

  // 防止 Ant Design 弹框/抽屉的 focus trap 阻止面板内输入框获取焦点
  // Ant Design 的 rc-dialog 在打开时会通过 focusin 全局事件将焦点拉回弹框内部
  // 这里在捕获阶段拦截，允许面板内的元素正常获取焦点
  const wrapperRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = wrapperRef.current;
    if (!el) return;
    const handleFocusIn = (e: FocusEvent) => {
      // 如果焦点目标是面板内的元素，阻止后续的 focus trap 处理
      if (el.contains(e.target as Node)) {
        e.stopPropagation();
      }
    };
    // 在捕获阶段拦截，优先于 rc-dialog 的冒泡阶段 focusin 监听
    el.addEventListener('focusin', handleFocusIn, true);
    return () => {
      el.removeEventListener('focusin', handleFocusIn, true);
    };
  }, []);

  // 拖拽相关状态
  const nodeRef = useRef<HTMLDivElement>(null);
  const [isDragging, setIsDragging] = useState(false);
  // 使用 ref 来跟踪当前模式，避免因模式变化导致重新渲染
  const isDraggableMode = mode === 'draggable';

  // 弹框与输入框互斥控制
  const [hasApproval, setHasApproval] = useState(false);
  const [hasAskQuestion, setHasAskQuestion] = useState(false);

  useEffect(() => {
    const unsubApproval = onHumanApproval((payload) =>
      setHasApproval(payload !== null),
    );
    const unsubAskQuestion = onAskUserQuestion((payload) =>
      setHasAskQuestion(payload !== null),
    );
    return () => {
      unsubApproval();
      unsubAskQuestion();
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadModelConfig = async () => {
      try {
        const configMap = await fetchConfigsBatch([MODEL_LLM_CONFIG_KEY]);
        if (cancelled) return;
        setLlmConfig(
          parseModelLlmConfig(configMap[MODEL_LLM_CONFIG_KEY]?.configValue),
        );
      } catch {
        if (!cancelled) {
          setLlmConfig(createDefaultModelLlmConfig());
        }
      }
    };

    void loadModelConfig();

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const chatEl = copilotChatRef.current;
    if (!chatEl) return;

    const syncAttachmentCount = () => {
      selectedAttachmentCountRef.current = chatEl.querySelectorAll(
        '.copilotKitAttachmentQueueItem',
      ).length;
    };

    syncAttachmentCount();

    const observer = new MutationObserver(syncAttachmentCount);
    observer.observe(chatEl, { childList: true, subtree: true });

    return () => {
      observer.disconnect();
    };
  }, []);

  const isInteractionActive = hasApproval || hasAskQuestion;

  const submitHeadlessMessage = useCallback(
    (payload: HeadlessBridgePayload) => {
      const content = buildHeadlessInputContent(payload);
      if (content.length === 0) {
        throw new Error('Headless 消息不能为空');
      }
      const text = payload.text?.trim() ?? '';
      const hasResources = (payload.resources ?? []).some(
        (item) => !!item?.url,
      );
      const normalizedContent = hasResources || !text ? content : text;
      agent.addMessage({
        id: crypto.randomUUID(),
        role: 'user',
        content: normalizedContent,
      } as any);
      void copilotkit.runAgent({ agent }).catch((error) => {
        console.error('[Headless] 发起 Agent 运行失败:', error);
      });
    },
    [agent, copilotkit],
  );

  useEffect(() => {
    const bridge = {
      send: submitHeadlessMessage,
    };
    window.__GWSU_HEADLESS_CHAT__ = bridge;
    return () => {
      if (window.__GWSU_HEADLESS_CHAT__ === bridge) {
        delete window.__GWSU_HEADLESS_CHAT__;
      }
    };
  }, [submitHeadlessMessage]);

  // 互斥控制：弹框活跃时通过 DOM 直接禁用 CopilotChat 输入框
  // 仅在 copilotChatRef 范围内查找，避免误禁用审批栏/问题栏的输入
  useEffect(() => {
    const chatEl = copilotChatRef.current;
    if (!chatEl) return;

    const applyState = (active: boolean) => {
      // 仅查找 CopilotChat 内的 textarea（聊天消息输入框）
      const textarea = chatEl.querySelector('textarea');
      if (textarea) {
        (textarea as HTMLTextAreaElement).disabled = active;
        (textarea as HTMLElement).style.opacity = active ? '0.4' : '';
        (textarea as HTMLElement).style.pointerEvents = active ? 'none' : '';
      }

      // 仅禁用 CopilotChat 内的发送按钮
      chatEl.querySelectorAll('button').forEach((btn) => {
        (btn as HTMLButtonElement).disabled = active;
        (btn as HTMLElement).style.opacity = active ? '0.4' : '';
        (btn as HTMLElement).style.pointerEvents = active ? 'none' : '';
      });
    };

    applyState(isInteractionActive);

    // 当弹框活跃时，监听 DOM 变化以应对 CopilotChat 重新渲染
    let observer: MutationObserver | null = null;
    if (isInteractionActive) {
      observer = new MutationObserver(() => applyState(true));
      observer.observe(chatEl, { childList: true, subtree: true });
    }

    return () => {
      observer?.disconnect();
    };
  }, [isInteractionActive]);

  // 切换到历史视图
  const handleShowHistory = () => {
    setViewMode('history');
  };

  // 返回聊天视图
  const handleBackToChat = () => {
    setViewMode('chat');
  };

  // 新建会话
  const handleNewSession = () => {
    agent.setMessages([]);
    clearHumanApproval();
    clearAskUserQuestion();
    clearAgentOutput();
    selectedAttachmentCountRef.current = 0;
    pendingAttachmentCountRef.current = 0;
    // 清除 headlessStore 中的 threadId
    useHeadlessStore.getState().clearThreadId();
    // 生成新的 threadId，让后端创建新的会话
    const newThreadId = crypto.randomUUID();
    setCurrentThreadId(newThreadId);
    agent.threadId = newThreadId;
  };

  const handleLoadSession = async (sessionId: string) => {
    try {
      // 先清除旧的弹框状态，避免切换会话后旧弹框残留
      clearHumanApproval();
      clearAskUserQuestion();
      clearAgentOutput();
      const messages = await getSessionMessages(sessionId);
      const formattedMessages = messages.map((msg: BrainMessage) => ({
        id: msg.id,
        role: msg.role,
        content: msg.content ?? '',
        ...(msg.toolCalls && msg.toolCalls.length > 0
          ? { toolCalls: msg.toolCalls }
          : {}),
        ...(msg.toolCallId ? { toolCallId: msg.toolCallId } : {}),
        ...(msg.encryptedValue ? { encryptedValue: msg.encryptedValue } : {}),
      }));
      // 所有 agent 状态同步设置，再触发重渲染，确保重渲染时消息已就绪
      agent.setMessages([]);
      agent.threadId = sessionId;
      agent.setMessages(
        formattedMessages as Parameters<typeof agent.setMessages>[0],
      );
      setCurrentThreadId(sessionId);

      // 检查是否需要恢复 AskUserQuestion 弹框
      // 最新一条消息如果是 AskUserQuestion 工具调用且无对应结果，则恢复弹框
      try {
        const lastMsg = messages[messages.length - 1] as
          | BrainMessage
          | undefined;
        if (lastMsg?.role === 'assistant' && lastMsg.toolCalls?.length) {
          const askQuestionToolCall = lastMsg.toolCalls.find(
            (tc) => tc.function?.name === 'AskUserQuestion',
          );
          if (askQuestionToolCall) {
            const args =
              typeof askQuestionToolCall.function?.arguments === 'string'
                ? JSON.parse(askQuestionToolCall.function.arguments)
                : askQuestionToolCall.function?.arguments ??
                  askQuestionToolCall.args ??
                  {};
            dispatchAskUserQuestion({
              toolCallId: askQuestionToolCall.id as string,
              questions: Array.isArray(args.questions)
                ? args.questions.map((q: Record<string, unknown>) => ({
                    question: String(q.question ?? ''),
                    header: String(q.header ?? ''),
                    options: Array.isArray(q.options)
                      ? q.options
                      : q.options
                      ? [q.options]
                      : [],
                    multiSelect: Boolean(q.multiSelect),
                  }))
                : [],
            });
          }
        }
      } catch (e) {
        console.warn('[AskUserQuestion] 历史会话恢复失败:', e);
      }

      // 根据后端实际审批状态决定是否恢复审批弹框
      try {
        const approvalStatus = await getApprovalStatus(sessionId);
        if (approvalStatus.stage) {
          dispatchHumanApproval(approvalStatus as any);
        }
      } catch (e) {
        console.warn('[HumanApproval] 查询审批状态失败:', e);
      }

      // 会话恢复完成，通知后端可以发送消息
      document.body.setAttribute('data-headless-chat-ready', 'true');
    } catch (error) {
      console.error('加载会话消息失败:', error);
      message.error('加载会话消息失败');
      // 即使失败也标记就绪，避免后端一直等待
      document.body.setAttribute('data-headless-chat-ready', 'true');
    }
  };

  // 自动加载 headlessStore 中的历史聊天记录
  const headlessThreadId = useHeadlessStore((s) => s.threadId);
  const hasRestoredRef = useRef(false);
  const handleLoadSessionRef = useRef(handleLoadSession);
  handleLoadSessionRef.current = handleLoadSession;
  useEffect(() => {
    if (hasRestoredRef.current) return;

    if (headlessThreadId) {
      hasRestoredRef.current = true;
      // 延迟等待 CopilotChat 初始化完成后再恢复历史消息
      const timer = setTimeout(() => {
        handleLoadSessionRef.current(headlessThreadId);
      }, 500);
      return () => clearTimeout(timer);
    } else {
      // 无历史会话，直接标记就绪
      document.body.setAttribute('data-headless-chat-ready', 'true');
    }
  }, [headlessThreadId]);

  // 拖拽处理
  const handleDragStart = useCallback(() => {
    setIsDragging(true);
  }, []);

  const handleDragStop = useCallback(
    (_e: DraggableEvent, data: DraggableData) => {
      setIsDragging(false);
      setPanelPosition({ x: data.x, y: data.y });
    },
    [setPanelPosition],
  );

  const attachmentsConfig = useMemo<AttachmentsConfig | undefined>(() => {
    if (!llmConfig.supportMultimodal) {
      return undefined;
    }

    const { maxUploadCount, maxUploadSizeMb, allowedUploadFormats } =
      llmConfig.multimodalOptions;

    return {
      enabled: true,
      accept: toAcceptValue(allowedUploadFormats),
      maxSize: maxUploadSizeMb * 1024 * 1024,
      onUpload: async (file) => {
        const activeCount =
          selectedAttachmentCountRef.current +
          pendingAttachmentCountRef.current;
        if (activeCount >= maxUploadCount) {
          throw new Error(`最多可上传 ${maxUploadCount} 个资源`);
        }

        pendingAttachmentCountRef.current += 1;
        try {
          const fileInfo = await upload(file, {
            property: {
              scope: FileScope.PUBLIC,
              expiredTime: createOneMonthLaterIsoString(),
            },
          });
          selectedAttachmentCountRef.current += 1;
          const resourceUrl = `${trimTrailingSlash(
            apiBaseUrl,
          )}/kit/file/stream/${fileInfo.fileId}`;
          return {
            type: 'url' as const,
            value: resourceUrl,
            mimeType: file.type || fileInfo.mediaType,
            metadata: {
              fileId: fileInfo.fileId,
              fileName: fileInfo.fileName,
              scope: FileScope.PUBLIC,
            },
          };
        } finally {
          pendingAttachmentCountRef.current = Math.max(
            0,
            pendingAttachmentCountRef.current - 1,
          );
        }
      },
      onUploadFailed: (error) => {
        if (error.reason === 'file-too-large') {
          message.warning(error.message);
          return;
        }
        if (error.reason === 'invalid-type') {
          message.warning(error.message);
          return;
        }
        message.error(error.message);
      },
    };
  }, [apiBaseUrl, llmConfig, message, upload]);

  // 渲染聊天内容（不含外层容器）
  const renderChatContent = () => (
    <>
      {/* 自定义 Header - 包含拖动和关闭按钮 */}
      <div
        className={`${styles.chatHeader} ${isDragging ? styles.dragging : ''}`}
      >
        <div className={styles.chatHeaderTitle}>
          <RobotOutlined />
          <span>智能助手</span>
        </div>
        <div className={styles.chatHeaderActions}>
          <Tooltip title="新会话" zIndex={1029}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={handleNewSession}
              icon={<PlusOutlined />}
            />
          </Tooltip>
          {viewConfig.showHistory && (
            <Tooltip title="历史记录" zIndex={1029}>
              <Button
                type="text"
                size="small"
                className={styles.actionButton}
                onClick={handleShowHistory}
                icon={<HistoryOutlined />}
              />
            </Tooltip>
          )}
          {viewConfig.enableDragMode &&
            (isDraggableMode ? (
              <Tooltip title="固定模式" zIndex={1029}>
                <Button
                  type="text"
                  size="small"
                  className={styles.actionButton}
                  onClick={onFixed}
                  icon={<CompressOutlined />}
                />
              </Tooltip>
            ) : (
              <Tooltip title="拖拽模式" zIndex={1029}>
                <Button
                  type="text"
                  size="small"
                  className={styles.actionButton}
                  onClick={onDraggable}
                  icon={<DragOutlined />}
                />
              </Tooltip>
            ))}
          <Tooltip title="收起面板" zIndex={1029}>
            <Button
              type="text"
              size="small"
              className={styles.actionButton}
              onClick={onHide}
              icon={<CloseOutlined />}
            />
          </Tooltip>
        </div>
      </div>
      {/* AI模式终止控制 - 最上方 */}
      <AiModeControlBar />
      {/* 无头浏览器提交控制（默认隐藏，后端控制显隐） */}
      <HeadlessSubmitBar />
      {/* 人工审批卡片 */}
      <HumanApprovalBar />
      {/* AskUserQuestion 选择框 */}
      <AskUserQuestionBar />
      {/* CopilotChat 组件 - 隐藏默认 header */}
      <div ref={copilotChatRef} style={{ display: 'contents' }}>
        <CopilotChatConfigurationProvider
          threadId={currentThreadId ?? undefined}
          hasExplicitThreadId={false}
        >
          <CopilotChat
            key={currentThreadId ?? 'default-thread'}
            agentId="brain"
            labels={{
              welcomeMessageText: '你好，我是你的助手^_^',
              chatInputPlaceholder: '输入消息...',
              chatDisclaimerText: 'AI生成内容，仅供参考',
            }}
            className={styles.copilotChat}
            input={
              llmConfig.supportMultimodal
                ? {
                    addMenuButton: DirectUploadButton,
                  }
                : undefined
            }
            messageView={CustomMessageView}
            attachments={attachmentsConfig}
            onStop={() => {
              agent.abortRun();
            }}
          />
        </CopilotChatConfigurationProvider>
      </div>
    </>
  );

  // 渲染历史面板内容
  const renderHistoryContent = () => (
    <ChatHistoryPanel
      onBackToChat={handleBackToChat}
      onLoadSession={handleLoadSession}
    />
  );

  // 始终渲染 Draggable，让历史视图和聊天视图都在同一个可拖拽容器中
  // 这样可以避免模式切换时组件被销毁重建，同时保持拖拽位置一致
  const isHidden = mode === 'hidden';

  // 使用 createPortal 将面板挂载到 body 最外层，脱离任何父级 stacking context
  // 确保智能助手层级始终高于所有弹框、Modal、Drawer 等 Ant Design 组件
  const panelContent = (
    <Draggable
      nodeRef={nodeRef}
      handle={`.${styles.chatHeader}`}
      cancel={`.${styles.chatHeaderActions}`}
      position={isDraggableMode ? panelState.position : { x: 0, y: 0 }}
      onStart={isDraggableMode ? handleDragStart : undefined}
      onStop={isDraggableMode ? handleDragStop : undefined}
      bounds="body"
      disabled={!isDraggableMode}
    >
      <div
        ref={(el) => {
          (nodeRef as React.MutableRefObject<HTMLDivElement | null>).current =
            el;
          (
            wrapperRef as React.MutableRefObject<HTMLDivElement | null>
          ).current = el;
        }}
        className={`${styles.copilotChatWrapper} ${
          isHidden ? styles.hiddenWrapper : ''
        } ${isDraggableMode ? styles.draggableWrapper : ''} ${
          isDragging ? styles.draggingWrapperActive : ''
        } ${className || ''}`}
        style={
          isDraggableMode
            ? {
                width: panelState.width,
                height: panelState.height,
              }
            : style
        }
      >
        {viewMode === 'history' ? renderHistoryContent() : renderChatContent()}
      </div>
    </Draggable>
  );

  return createPortal(panelContent, document.body);
}
