import type { PendingApproval } from './types';

const approvalListeners = new Set<(approval: PendingApproval | null) => void>();
let currentPendingApproval: PendingApproval | null = null;

export function setPendingApproval(approval: PendingApproval): void {
  currentPendingApproval = approval;
  approvalListeners.forEach((listener) => listener(approval));
}

export function clearHumanApproval(interruptId?: string): void {
  if (
    interruptId &&
    currentPendingApproval?.interrupt.id !== interruptId
  ) {
    return;
  }
  currentPendingApproval = null;
  approvalListeners.forEach((listener) => listener(null));
}

export function getPendingApproval(): PendingApproval | null {
  return currentPendingApproval;
}

export function onHumanApproval(
  listener: (approval: PendingApproval | null) => void,
): () => void {
  approvalListeners.add(listener);
  listener(currentPendingApproval);
  return () => approvalListeners.delete(listener);
}
