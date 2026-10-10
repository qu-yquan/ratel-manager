import { CopilotKit } from '@copilotkit/react-core';
import { useRenderTool } from '@copilotkit/react-core/v2';
import { useAgent } from '@copilotkit/react-core/v2';
import { useUserStore } from '@gwsu/core';
import type { ReactNode } from 'react';
import { useEffect, useRef, useMemo } from 'react';
import { App } from 'antd';
import { randomUUID } from '@ag-ui/client';
import { dispatchWebTool } from '@/services/web-tool';
import type { WebToolExecutePayload } from '@/services/web-tool';
import {
  dispatchAskUserQuestion,
  normalizeAskUserQuestionPayload,
} from '@/services/ask-user-question';
import {
  dispatchAgentOutput,
  dispatchAgentOutputEnd,
} from '@/services/agent-output';
import type {
  AgentOutputPayload,
  AgentOutputEndPayload,
} from '@/services/agent-output';
import { dispatchKnowledgeReferences } from '@/services/knowledge-reference';
import type { KnowledgeReferencesPayload } from '@/services/knowledge-reference';
import { WebToolConfirmModal } from '@/services/web-tool/components/WebToolConfirmModal';
import { ToolCallItem } from '@/components/AIChat/ToolCallItem';
import { useViewConfigStore } from '@/stores/viewConfig';
// 确保 route-navigation 工具被注册
import '@/services/web-tool/tools/route-navigation';
// 确保 AI 界面操作工具被注册
import '@/services/web-tool/tools/get-page-state';
import '@/services/web-tool/tools/click-element';
import '@/services/web-tool/tools/input-text';
import '@/services/web-tool/tools/select-option';
import '@/services/web-tool/tools/scroll-page';
import '@/services/web-tool/tools/hover-element';
import '@/services/web-tool/tools/attach-uploaded-file';
// 确保 AI 操作模式工具被注册
import '@/services/web-tool/tools/enter-ai-mode';
import '@/services/web-tool/tools/exit-ai-mode';
import { AgentSubscriber } from '@ag-ui/client';
import { useForwardedPropsStore } from '@/stores/forwardedProps';

interface GwsuCopilotKitProviderProps {
  children: ReactNode;
}

export const RAW_ERROR_MESSAGE_NAME = '__raw_error__';

/**
 * 工具调用渲染注册组件
 * 注册通配符(*)渲染器，使所有工具调用在聊天面板中展示
 * 必须在 CopilotKit 内部使用
 */
function ToolCallRendererRegistration() {
  const showToolCalls = useViewConfigStore((s) => s.showToolCalls);
  useRenderTool(
    {
      name: '*',
      render: ({ name, args, status, result }) => {
        if (!showToolCalls) return <></>;
        return (
          <ToolCallItem
            name={name}
            args={args}
            status={status}
            result={result}
          />
        );
      },
    },
    [showToolCalls],
  );
  return null;
}

/**
 * Agent CUSTOM 事件订阅组件
 * 必须在 CopilotKit 内部使用，因为需要 access to agent context
 */
function WebToolEventListener() {
  const { agent } = useAgent({ agentId: 'brain' });
  const subscriptionRef = useRef<ReturnType<typeof agent.subscribe> | null>(
    null,
  );
  const { notification } = App.useApp();

  const showRawError = (msg: string) => {
    notification.error({
      title: '请求错误',
      description: <div>{msg}</div>,
      duration: 3,
    });
  };

  useEffect(() => {
    if (!agent) return;

    // 清理旧的订阅
    if (subscriptionRef.current) {
      subscriptionRef.current.unsubscribe();
    }

    const subscriber: AgentSubscriber = {
      onCustomEvent: ({ event }): void => {
        //web工具调用
        if (event.name === 'TOOL_EXECUTE') {
          dispatchWebTool(event.value as WebToolExecutePayload);
        }
        // AI 输出视图 - 完整 JSONL Patch 行
        else if (event.name === 'AGENT_OUTPUT') {
          dispatchAgentOutput(event.value as AgentOutputPayload);
        }
        // AI 输出视图 - 输出结束
        else if (event.name === 'AGENT_OUTPUT_END') {
          dispatchAgentOutputEnd(event.value as AgentOutputEndPayload);
        } else if (event.name === 'KNOWLEDGE_REFERENCES') {
          dispatchKnowledgeReferences(
            event.value as KnowledgeReferencesPayload,
          );
        }
      },
      onRawEvent: ({ event }): void => {
        const rawEvent = event.rawEvent as Record<string, unknown> | undefined;
        const errorMessage = rawEvent?.error;
        if (typeof errorMessage === 'string' && errorMessage.trim()) {
          showRawError(errorMessage);
          agent.addMessage({
            id: randomUUID(),
            role: 'assistant',
            name: RAW_ERROR_MESSAGE_NAME,
            content: errorMessage,
          });
        }
      },
      onToolCallEndEvent: ({ toolCallName, toolCallArgs, event }): void => {
        if (toolCallName === 'AskUserQuestion') {
          const payload = normalizeAskUserQuestionPayload(
            event.toolCallId,
            toolCallArgs,
          );
          if (payload) {
            dispatchAskUserQuestion(payload);
          }
        }
      },
    };

    const subscription = agent.subscribe(subscriber);
    subscriptionRef.current = subscription;

    return () => {
      subscription.unsubscribe();
      subscriptionRef.current = null;
    };
  }, [agent, notification]);

  return null;
}

/**
 * CopilotKit Provider 封装
 * 使用 HttpAgent 直接连接到后端 AG-UI 接口
 */
export function GwsuCopilotKitProvider({
  children,
}: GwsuCopilotKitProviderProps) {
  // 订阅 forwardedProps store，变化时触发重渲染以更新 properties
  const currentPath = useForwardedPropsStore((s: any) => s.currentPath);
  const operationMode = useForwardedPropsStore((s: any) => s.operationMode);
  const extras = useForwardedPropsStore((s: any) => s.extras);

  // 动态获取请求头
  const getHeaders = (): Record<string, string> => {
    const tokenInfo = useUserStore.getState().getTokenInfo();
    const headers: Record<string, string> = {};
    if (tokenInfo?.token) {
      headers['Authorization'] = `Bearer ${tokenInfo.token}`;
    }
    return headers;
  };

  // 构建 properties，路由或操作模式变化时更新
  const properties = useMemo(
    () => ({
      currentPath,
      operationMode,
      ...extras,
    }),
    [currentPath, operationMode, extras],
  );

  return (
    <CopilotKit
      runtimeUrl="/api/security/brain/run/copilotKit"
      useSingleEndpoint
      headers={getHeaders}
      properties={properties}
      agent="brain"
      enableInspector={false}
    >
      <ToolCallRendererRegistration />
      <WebToolEventListener />
      <WebToolConfirmModal />
      {children}
    </CopilotKit>
  );
}
