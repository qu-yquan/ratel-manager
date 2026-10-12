import { RobotOutlined } from '@ant-design/icons';
import { useEffect, useState, useRef, useCallback } from 'react';
import { Renderer, JSONUIProvider } from '@json-render/react';
import { createSpecStreamCompiler } from '@json-render/core';
import type { Spec } from '@json-render/core';
import { registry } from './registry';
import {
  onAgentOutput,
  onAgentOutputEnd,
  onAgentOutputReset,
} from '@/services/agent-output';
import { useOperationTabStore } from '@/stores/operationTab';
import styles from './index.module.less';

/**
 * AI 输出面板组件
 * 使用 json-render Renderer 流式渲染 AI 输出的可视化内容
 *
 * 流程：
 * 1. AGENT_OUTPUT 事件携带完整 JSONL Patch 行 → createSpecStreamCompiler.push() 逐行应用 → Renderer 流式渲染
 * 2. AGENT_OUTPUT_END 事件 → 隐藏"生成中"状态
 */
const AiOutputPanel: React.FC = () => {
  const [spec, setSpec] = useState<Spec | null>(null);
  const [isStreaming, setIsStreaming] = useState(false);
  const [hasContent, setHasContent] = useState(false);
  const compilerRef = useRef<ReturnType<
    typeof createSpecStreamCompiler<Spec>
  > | null>(null);
  // 标记是否已收到过 AGENT_OUTPUT_END，用于判断新一轮输出是否需要重置编译器
  const hasEndedRef = useRef(false);

  // 确保编译器初始化
  if (!compilerRef.current) {
    compilerRef.current = createSpecStreamCompiler<Spec>();
  }

  const handleClear = useCallback(() => {
    compilerRef.current = createSpecStreamCompiler<Spec>();
    hasEndedRef.current = false;
    setSpec(null);
    setHasContent(false);
    setIsStreaming(false);
  }, []);

  useEffect(() => {
    // 监听 AGENT_OUTPUT：后端已按行缓冲，每个事件是完整的 JSONL Patch 行
    const unsubOutput = onAgentOutput(({ text }) => {
      // 收到AI输出事件，自动切换到AI输出Tab，让用户能看到输出内容
      useOperationTabStore.getState().switchToAiOutput();

      // 上一轮输出已结束，当前是新一轮 OutputViewAgent 调用，需要重置编译器
      if (hasEndedRef.current) {
        compilerRef.current = createSpecStreamCompiler<Spec>();
        setSpec(null);
        setHasContent(false);
        hasEndedRef.current = false;
      }

      setIsStreaming(true);

      try {
        const compiler = compilerRef.current;
        if (!compiler) return;
        const { result, newPatches } = compiler.push(text + '\n');
        if (
          newPatches.length > 0 &&
          result &&
          result.root &&
          Object.keys(result.elements || {}).length > 0
        ) {
          setSpec({ ...result });
          setHasContent(true);
        }
      } catch (e) {
        // patch 解析失败，静默忽略
      }
    });

    // 监听 AGENT_OUTPUT_END：输出结束，标记状态供下一轮判断
    const unsubOutputEnd = onAgentOutputEnd(() => {
      setIsStreaming(false);
      hasEndedRef.current = true;
    });

    const unsubOutputReset = onAgentOutputReset(() => {
      handleClear();
    });

    return () => {
      unsubOutput();
      unsubOutputEnd();
      unsubOutputReset();
    };
  }, [handleClear]);

  return (
    <div id="ai-output-panel" className={styles.aiOutputPanel}>
      {hasContent && spec && (
        <JSONUIProvider registry={registry}>
          <Renderer spec={spec} registry={registry} />
        </JSONUIProvider>
      )}
      {isStreaming && (
        <div className={styles.loadingIndicator}>
          <span className={styles.loadingDot} />
          生成中...
        </div>
      )}
      {!hasContent && !isStreaming && (
        <div className={styles.emptyState}>
          <RobotOutlined className={styles.emptyIcon} />
          <div className={styles.emptyTitle}>AI 输出区</div>
          <div className={styles.emptySubtitle}>
            智能助手的输出结果将在此展示
          </div>
        </div>
      )}
    </div>
  );
};

export default AiOutputPanel;
