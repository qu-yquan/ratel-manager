import { Fragment, isValidElement, type ReactElement } from 'react';
import {
  CopilotChatMessageView,
  CopilotChatAssistantMessage,
  type CopilotChatAssistantMessageProps,
  type CopilotChatMessageViewProps,
} from '@copilotkit/react-core/v2';
import type { AssistantMessage, Message } from '@ag-ui/core';
import { ExecutionSummary } from './ExecutionSummary';
import type { ViewConfig } from './types';
import { RAW_ERROR_MESSAGE_NAME } from '@/providers/CopilotKitProvider';
import styles from './copilot-override.module.less';

const DEFAULT_VIEW_CONFIG: ViewConfig = {
  showThinking: true,
  showToolCalls: true,
  showHistory: true,
  enableDragMode: false,
};

export function createCustomRenderMessage(
  viewConfig: ViewConfig = DEFAULT_VIEW_CONFIG,
) {
  function CustomAssistantMessage(props: CopilotChatAssistantMessageProps) {
    const { message } = props;
    const isRawError =
      (message as AssistantMessage & { name?: string }).name ===
      RAW_ERROR_MESSAGE_NAME;

    if (!isRawError) {
      return (
        <CopilotChatAssistantMessage {...props} toolCallsView={() => null} />
      );
    }

    return (
      <CopilotChatAssistantMessage
        {...props}
        className={styles.rawErrorMessage}
        toolbarVisible={false}
      />
    );
  }

  function CustomMessageView(props: CopilotChatMessageViewProps) {
    return (
      <CopilotChatMessageView
        {...props}
        assistantMessage={CustomAssistantMessage}
        reasoningMessage={false as any}
      >
        {({ messages, messageElements, isRunning, interruptElement }) => {
          const elementsByMessageId = new Map<string, ReactElement>();
          messageElements.forEach((element) => {
            if (isValidElement(element) && element.key !== null) {
              elementsByMessageId.set(String(element.key), element);
            }
          });

          const renderedElements: ReactElement[] = [];
          let index = 0;
          while (index < messages.length) {
            const message = messages[index];
            if (message.role !== 'user') {
              const element = elementsByMessageId.get(message.id);
              if (element) renderedElements.push(element);
              index += 1;
              continue;
            }

            const turnStartIndex = index;
            let turnEndIndex = turnStartIndex + 1;
            while (
              turnEndIndex < messages.length &&
              messages[turnEndIndex].role !== 'user'
            ) {
              turnEndIndex += 1;
            }

            const turnMessages = messages.slice(turnStartIndex, turnEndIndex);
            const isActiveTurn = isRunning && turnEndIndex === messages.length;
            let processSegment: Message[] = [];
            let segmentIndex = 0;

            const appendProcessSegment = (segmentIsRunning = false) => {
              const hasVisibleProcess = processSegment.some(
                (current) =>
                  (viewConfig.showThinking && current.role === 'reasoning') ||
                  (viewConfig.showToolCalls &&
                    current.role === 'assistant' &&
                    !!current.toolCalls?.length),
              );
              if (hasVisibleProcess) {
                renderedElements.push(
                  <ExecutionSummary
                    key={`execution-${message.id}-${segmentIndex}`}
                    messages={processSegment}
                    isRunning={segmentIsRunning}
                    showThinking={viewConfig.showThinking}
                    showToolCalls={viewConfig.showToolCalls}
                  />,
                );
              }
              processSegment = [];
              segmentIndex += 1;
            };

            turnMessages.forEach((current) => {
              if (current.role === 'reasoning' || current.role === 'tool') {
                processSegment.push(current);
                return;
              }

              if (current.role === 'assistant' && current.toolCalls?.length) {
                // 同一条消息同时包含文本与工具时，文本代表新的视觉分界。
                if (current.content?.trim()) {
                  appendProcessSegment();
                  const element = elementsByMessageId.get(current.id);
                  if (element) renderedElements.push(element);
                }
                processSegment.push(current);
                return;
              }

              // 可见文本或活动消息会结束此前的连续执行片段。
              appendProcessSegment();
              const element = elementsByMessageId.get(current.id);
              if (element) renderedElements.push(element);
            });
            appendProcessSegment(isActiveTurn);
            index = turnEndIndex;
          }

          const lastMessage = messages[messages.length - 1];
          return (
            <div
              data-testid="copilot-message-list"
              className={`copilotKitMessages cpk:flex cpk:flex-col ${
                props.className ?? ''
              }`}
            >
              {renderedElements.map((element) => (
                <Fragment key={element.key}>{element}</Fragment>
              ))}
              {interruptElement}
              {isRunning && lastMessage?.role !== 'reasoning' && (
                <div className="cpk:mt-2">
                  <CopilotChatMessageView.Cursor />
                </div>
              )}
            </div>
          );
        }}
      </CopilotChatMessageView>
    );
  }

  // 保留 CopilotChatMessageView 的静态属性（如 Cursor），满足 messageView 类型要求
  CustomMessageView.Cursor = CopilotChatMessageView.Cursor;

  return CustomMessageView;
}
