import { useEffect, useMemo, useRef, useState } from 'react';
import {
  BookOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  LoadingOutlined,
  RightOutlined,
  RobotOutlined,
  ToolOutlined,
} from '@ant-design/icons';
import type { Message, ToolMessage } from '@ag-ui/core';
import styles from './ExecutionSummary.module.less';

interface ExecutionSummaryProps {
  /** 本轮对话中的过程消息 */
  messages: Message[];
  /** 当前轮是否仍在运行 */
  isRunning: boolean;
  /** 是否展示思考内容 */
  showThinking: boolean;
  /** 是否展示工具调用 */
  showToolCalls: boolean;
}

interface ToolExecution {
  id: string;
  kind: 'tool' | 'skill' | 'subagent';
  name: string;
  description?: string;
  args: Record<string, unknown>;
  result?: string;
  error?: string;
  completed: boolean;
}

const SKILL_TOOL_NAME = 'load_skill_through_path';
const SUBAGENT_TOOL_NAME = 'agent_spawn';

function getStringArg(
  args: Record<string, unknown>,
  ...keys: string[]
): string | undefined {
  for (const key of keys) {
    const value = args[key];
    if (typeof value === 'string' && value.trim()) return value.trim();
  }
  return undefined;
}

function parseToolArgs(value: string): Record<string, unknown> {
  if (!value.trim()) return {};
  try {
    const parsed = JSON.parse(value) as unknown;
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
      ? (parsed as Record<string, unknown>)
      : { value: parsed };
  } catch {
    return { value };
  }
}

function formatDuration(seconds: number): string {
  if (seconds < 1) return '1秒';
  if (seconds < 60) return `${Math.round(seconds)}秒`;
  const minutes = Math.floor(seconds / 60);
  const remainingSeconds = Math.round(seconds % 60);
  return remainingSeconds > 0
    ? `${minutes}分${remainingSeconds}秒`
    : `${minutes}分钟`;
}

/**
 * 单轮对话的执行过程摘要。
 * 默认只展示当前阶段，用户主动展开后才呈现思考与工具细节。
 */
export function ExecutionSummary({
  messages,
  isRunning,
  showThinking,
  showToolCalls,
}: ExecutionSummaryProps) {
  const [expanded, setExpanded] = useState(false);
  const startTimeRef = useRef(Date.now());
  const hasRunRef = useRef(isRunning);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  const reasoningContents = useMemo(
    () =>
      showThinking
        ? messages
            .filter((message) => message.role === 'reasoning')
            .map((message) => message.content.trim())
            .filter(Boolean)
        : [],
    [messages, showThinking],
  );

  const toolExecutions = useMemo<ToolExecution[]>(() => {
    if (!showToolCalls) return [];

    const results = new Map<string, ToolMessage>();
    messages.forEach((message) => {
      if (message.role === 'tool') {
        results.set(message.toolCallId, message);
      }
    });

    return messages.flatMap((message) => {
      if (message.role !== 'assistant' || !message.toolCalls?.length) return [];
      return message.toolCalls.map((toolCall) => {
        const result = results.get(toolCall.id);
        const args = parseToolArgs(toolCall.function.arguments);
        const isSkill = toolCall.function.name === SKILL_TOOL_NAME;
        const isSubagent = toolCall.function.name === SUBAGENT_TOOL_NAME;
        const subagentName = getStringArg(
          args,
          'name',
          'agentName',
          'taskName',
          'task_name',
        );
        return {
          id: toolCall.id,
          kind: isSkill ? 'skill' : isSubagent ? 'subagent' : 'tool',
          name: isSkill
            ? '加载SKILL.md'
            : isSubagent
            ? subagentName || '启动子智能体'
            : toolCall.function.name,
          description: isSkill
            ? getStringArg(args, 'path')
            : isSubagent
            ? getStringArg(args, 'task', 'prompt', 'description')
            : undefined,
          args,
          result: result?.content,
          error: result?.error,
          completed: !!result,
        };
      });
    });
  }, [messages, showToolCalls]);

  const standardTools = toolExecutions.filter((tool) => tool.kind === 'tool');
  const skillLoads = toolExecutions.filter((tool) => tool.kind === 'skill');
  const subagents = toolExecutions.filter((tool) => tool.kind === 'subagent');

  const pendingTool = toolExecutions.find((tool) => !tool.completed);
  const hasError = toolExecutions.some(
    (tool) => !!tool.error || tool.result?.includes('Error:'),
  );
  const hasDetails = reasoningContents.length > 0 || toolExecutions.length > 0;

  useEffect(() => {
    if (!isRunning) {
      if (hasRunRef.current) {
        setElapsedSeconds((Date.now() - startTimeRef.current) / 1000);
      }
      return;
    }

    hasRunRef.current = true;
    const timer = window.setInterval(() => {
      setElapsedSeconds((Date.now() - startTimeRef.current) / 1000);
    }, 500);
    return () => window.clearInterval(timer);
  }, [isRunning]);

  const statusLabel = isRunning
    ? pendingTool
      ? `调用中 · ${pendingTool.name}`
      : '思考中'
    : pendingTool
    ? '等待继续'
    : hasError
    ? '部分调用失败'
    : '已完成';
  const durationLabel =
    elapsedSeconds > 0 ? formatDuration(elapsedSeconds) : '';
  const countLabel = [
    standardTools.length > 0 ? `${standardTools.length} 个工具` : '',
    skillLoads.length > 0 ? `${skillLoads.length} 个技能` : '',
    subagents.length > 0 ? `${subagents.length} 个子智能体` : '',
  ]
    .filter(Boolean)
    .join(' · ');

  const renderExecutionGroup = (
    title: string,
    executions: ToolExecution[],
    icon: 'tool' | 'skill' | 'subagent',
  ) => {
    if (executions.length === 0) return null;

    return (
      <div className={styles.detailSection}>
        <div className={styles.detailTitle}>{title}</div>
        <div className={styles.toolList}>
          {executions.map((tool) => {
            const toolHasError =
              !!tool.error || tool.result?.includes('Error:');
            return (
              <details key={tool.id} className={styles.toolItem}>
                <summary className={styles.toolHeader}>
                  {icon === 'skill' ? (
                    <BookOutlined
                      className={styles.skillIcon}
                      aria-hidden="true"
                    />
                  ) : icon === 'subagent' ? (
                    <RobotOutlined
                      className={styles.subagentIcon}
                      aria-hidden="true"
                    />
                  ) : (
                    <ToolOutlined aria-hidden="true" />
                  )}
                  <span className={styles.toolIdentity}>
                    <span className={styles.toolName}>{tool.name}</span>
                    {tool.description && (
                      <span className={styles.toolDescription}>
                        {tool.description}
                      </span>
                    )}
                  </span>
                  <span
                    className={`${styles.toolStatus} ${
                      toolHasError ? styles.toolStatusError : ''
                    }`}
                  >
                    {tool.completed
                      ? toolHasError
                        ? '失败'
                        : '完成'
                      : '调用中'}
                  </span>
                </summary>
                <div className={styles.toolDetail}>
                  {Object.keys(tool.args).length > 0 && (
                    <pre className={styles.codeBlock}>
                      {JSON.stringify(tool.args, null, 2)}
                    </pre>
                  )}
                  {(tool.result || tool.error) && (
                    <pre
                      className={`${styles.codeBlock} ${
                        toolHasError ? styles.errorBlock : ''
                      }`}
                    >
                      {tool.error || tool.result}
                    </pre>
                  )}
                </div>
              </details>
            );
          })}
        </div>
      </div>
    );
  };

  return (
    <section
      className={`${styles.summary} ${isRunning ? styles.running : ''}`}
      aria-label="智能助手执行过程"
    >
      <button
        type="button"
        className={styles.summaryButton}
        disabled={!hasDetails}
        aria-expanded={hasDetails ? expanded : undefined}
        onClick={() => setExpanded((current) => !current)}
      >
        <span className={styles.statusIcon} aria-hidden="true">
          {isRunning ? (
            <LoadingOutlined spin />
          ) : hasError ? (
            <CloseCircleOutlined />
          ) : (
            <CheckCircleOutlined />
          )}
        </span>
        <span className={styles.statusLabel} aria-live="polite">
          {statusLabel}
        </span>
        {durationLabel && (
          <span className={styles.duration}>用时 {durationLabel}</span>
        )}
        {countLabel && <span className={styles.count}>{countLabel}</span>}
        {hasDetails && (
          <RightOutlined
            className={`${styles.chevron} ${
              expanded ? styles.chevronExpanded : ''
            }`}
            aria-hidden="true"
          />
        )}
      </button>

      <div
        className={`${styles.details} ${
          expanded ? styles.detailsExpanded : ''
        }`}
      >
        <div className={styles.detailsInner}>
          {reasoningContents.length > 0 && (
            <div className={styles.detailSection}>
              <div className={styles.detailTitle}>思考过程</div>
              {reasoningContents.map((content, index) => (
                <p key={index} className={styles.reasoningContent}>
                  {content}
                </p>
              ))}
            </div>
          )}

          {renderExecutionGroup('工具调用', standardTools, 'tool')}
          {renderExecutionGroup('技能加载', skillLoads, 'skill')}
          {renderExecutionGroup('子智能体', subagents, 'subagent')}
        </div>
      </div>
    </section>
  );
}
