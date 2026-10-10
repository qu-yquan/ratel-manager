import {
  CheckOutlined,
  CloseOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
} from '@ant-design/icons';
import type { Interrupt } from '@ag-ui/client';
import { useInterrupt } from '@copilotkit/react-core/v2';
import { Button, Input } from 'antd';
import { useCallback, useEffect, useState } from 'react';
import {
  clearHumanApproval,
  setPendingApproval,
  type ApprovalResolve,
} from '@/services/human-approval';
import styles from './HumanApprovalBar.module.less';

interface ApprovalCardProps {
  interrupt: Interrupt;
  interrupts: Interrupt[];
  resolve: ApprovalResolve;
}

function ApprovalCard({ interrupt, interrupts, resolve }: ApprovalCardProps) {
  const [showRejectReason, setShowRejectReason] = useState(false);
  const [rejectReason, setRejectReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setPendingApproval({ interrupt, interrupts, resolve });
    return () => clearHumanApproval(interrupt.id);
  }, [interrupt, interrupts, resolve]);

  const submit = useCallback(
    async (approved: boolean, reason?: string) => {
      setSubmitting(true);
      try {
        const payload = approved
          ? { approved: true }
          : { approved: false, ...(reason ? { reason } : {}) };
        for (const item of interrupts) {
          await resolve(payload, item.id);
        }
        clearHumanApproval(interrupt.id);
      } catch (error) {
        console.error('[HumanApproval] 提交官方 interrupt 响应失败:', error);
      } finally {
        setSubmitting(false);
      }
    },
    [interrupt.id, interrupts, resolve],
  );

  const metadata = (interrupt.metadata ?? {}) as Record<string, unknown>;
  const toolName = String(metadata.toolName ?? '');
  const toolInput = metadata.toolInput as Record<string, unknown> | undefined;
  const tip = interrupt.message || 'AI 请求执行操作，请确认是否允许';
  const inputSummary = toolInput
    ? Object.entries(toolInput)
        .map(
          ([key, value]) =>
            `${key}=${
              typeof value === 'string' ? value : JSON.stringify(value)
            }`,
        )
        .join(', ')
    : '';

  return (
    <div className={styles.approvalBar} data-testid="human-approval">
      <div className={styles.approvalContent}>
        <SafetyCertificateOutlined className={styles.approvalIcon} />
        <div className={styles.approvalInfo}>
          <div className={styles.approvalTip}>{tip}</div>
          <div className={styles.approvalDetail}>
            {toolName && <span className={styles.toolName}>{toolName}</span>}
            {inputSummary && (
              <span>
                {inputSummary.length > 80
                  ? `${inputSummary.slice(0, 80)}...`
                  : inputSummary}
              </span>
            )}
          </div>
        </div>
        <div className={styles.approvalActions}>
          <Button
            type="primary"
            size="small"
            className={styles.approveBtn}
            icon={<CheckOutlined />}
            loading={submitting}
            onClick={() => void submit(true)}
          >
            批准
          </Button>
          <Button
            danger
            size="small"
            className={styles.rejectBtn}
            icon={<CloseOutlined />}
            disabled={showRejectReason || submitting}
            onClick={() => setShowRejectReason(true)}
          >
            拒绝
          </Button>
        </div>
      </div>

      {showRejectReason && (
        <div className={styles.rejectReasonArea}>
          <Input.TextArea
            className={styles.rejectInput}
            value={rejectReason}
            onChange={(event) => setRejectReason(event.target.value)}
            placeholder="可选：填写拒绝原因..."
            autoSize={{ minRows: 1, maxRows: 3 }}
            onPressEnter={(event) => {
              if (!event.shiftKey) {
                event.preventDefault();
                void submit(false, rejectReason.trim() || undefined);
              }
            }}
          />
          <Button
            type="primary"
            danger
            size="small"
            className={styles.rejectConfirmBtn}
            icon={<SendOutlined />}
            loading={submitting}
            onClick={() => void submit(false, rejectReason.trim() || undefined)}
          >
            提交
          </Button>
          <Button
            size="small"
            className={styles.rejectConfirmBtn}
            disabled={submitting}
            onClick={() => {
              setShowRejectReason(false);
              setRejectReason('');
            }}
          >
            取消
          </Button>
        </div>
      )}
    </div>
  );
}

/** 使用 CopilotKit 官方 interrupt/resume 协议渲染审批卡片。 */
export function HumanApprovalBar() {
  const liveInterruptElement = useInterrupt({
    agentId: 'brain',
    renderInChat: false,
    enabled: (event) => {
      if (event.name !== 'on_interrupt' || !event.value) return false;
      const interrupt = event.value as Interrupt;
      const metadata = (interrupt.metadata ?? {}) as Record<string, unknown>;
      return metadata['agentscope.interruptKind'] === 'permission_confirm';
    },
    render: ({ interrupt, interrupts, resolve }) => {
      if (!interrupt) return <></>;
      return (
        <ApprovalCard
          interrupt={interrupt}
          interrupts={interrupts}
          resolve={resolve}
        />
      );
    },
  });

  return liveInterruptElement;
}
