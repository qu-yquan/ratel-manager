import type { Interrupt, RunAgentResult } from '@ag-ui/client';

export type ApprovalResolve = (
  payload?: unknown,
  interruptId?: string,
) => Promise<RunAgentResult | void>;

/** CopilotKit 从官方 AG-UI RUN_FINISHED interrupt 中解析出的待审批状态。 */
export interface PendingApproval {
  interrupt: Interrupt;
  interrupts: Interrupt[];
  resolve: ApprovalResolve;
}
