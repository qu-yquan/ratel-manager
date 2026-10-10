import {
  CheckCircleOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { Button } from 'antd';
import {
  clearHumanApproval,
  getPendingApproval,
} from '@/services/human-approval';
import {
  clearAskUserQuestionPrompt,
  dispatchAskUserQuestion,
  getPendingAskUserQuestion,
  resolveAskUserQuestion,
} from '@/services/ask-user-question';
import type { AskUserQuestionAnswer } from '@/services/ask-user-question';
import styles from './index.module.less';

/**
 * 无头浏览器提交控制条
 *
 * 当后端（HeadlessBrowserSession）需要提交审批或回答问题时，
 * 通过 page.evaluate() 设置 data-headless-forms-visible 属性来显示此组件，
 * 然后使用 page.locator().click() 点击可见按钮（Playwright 原生 click 会协调事件循环，避免死锁），
 * 点击后自动隐藏。
 *
 * 隐藏的 input 用于接收后端填充的值，按钮负责触发提交逻辑。
 */
export function HeadlessSubmitBar() {
  return (
    <div className={styles.submitBar} data-testid="headless-forms">
      {/* 隐藏输入框 - 接收后端填充的值 */}
      <input
        data-testid="headless-approval-result"
        readOnly
        className={styles.hiddenInput}
      />
      <input
        data-testid="headless-approval-reject-reason"
        readOnly
        className={styles.hiddenInput}
      />
      <input
        data-testid="headless-question-answers"
        readOnly
        className={styles.hiddenInput}
      />
      <input
        data-testid="headless-question-tool-call-id"
        readOnly
        className={styles.hiddenInput}
      />

      {/* 可见按钮区 */}
      <div className={styles.buttonRow}>
        <Button
          type="primary"
          size="small"
          className={styles.submitBtn}
          icon={<SafetyCertificateOutlined />}
          data-testid="headless-approval-submit"
          onClick={async () => {
            const resultEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-approval-result"]',
            );
            const reasonEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-approval-reject-reason"]',
            );
            const result = resultEl?.value;
            if (!result) return;

            try {
              const pending = getPendingApproval();
              if (!pending) {
                throw new Error('当前没有待处理的官方 AG-UI interrupt');
              }
              const approved = result === 'APPROVED';
              const rejectReason = reasonEl?.value.trim() ?? '';
              const payload = approved
                ? { approved: true }
                : {
                    approved: false,
                    ...(rejectReason ? { reason: rejectReason } : {}),
                  };
              for (const interrupt of pending.interrupts) {
                await pending.resolve(payload, interrupt.id);
              }
              clearHumanApproval(pending.interrupt.id);
            } catch (e) {
              console.error('[HeadlessApproval] interrupt恢复失败:', e);
            }

            // 重置表单值并隐藏
            if (resultEl) resultEl.value = '';
            if (reasonEl) reasonEl.value = '';
            document.body.removeAttribute('data-headless-forms-visible');
          }}
        >
          提交审批
        </Button>
        <Button
          type="primary"
          size="small"
          className={styles.submitBtn}
          icon={<CheckCircleOutlined />}
          data-testid="headless-question-submit"
          onClick={async () => {
            const answersEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-question-answers"]',
            );
            const toolCallIdEl = document.querySelector<HTMLInputElement>(
              '[data-testid="headless-question-tool-call-id"]',
            );
            const answersJson = answersEl?.value;
            const toolCallId = toolCallIdEl?.value;
            if (!answersJson || !toolCallId) return;
            try {
              const answers = JSON.parse(answersJson);
              const answer: AskUserQuestionAnswer = {
                answers,
                annotations: {},
              };
              const pendingPrompt = getPendingAskUserQuestion();
              clearAskUserQuestionPrompt();
              try {
                await resolveAskUserQuestion(toolCallId, answer);
              } catch (e) {
                if (pendingPrompt) {
                  dispatchAskUserQuestion(pendingPrompt);
                }
                throw e;
              }
            } catch (e) {
              console.error('[HeadlessQuestion] interrupt恢复失败:', e);
            }

            // 重置表单值并隐藏
            if (answersEl) answersEl.value = '';
            if (toolCallIdEl) toolCallIdEl.value = '';
            document.body.removeAttribute('data-headless-forms-visible');
          }}
        >
          提交问题
        </Button>
      </div>
    </div>
  );
}
