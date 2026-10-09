import { useSyncExternalStore } from 'react';
import type { KnowledgeReference, KnowledgeReferencesPayload } from './types';

const EMPTY_REFERENCES: KnowledgeReference[] = [];
const referencesByMessageId = new Map<string, KnowledgeReference[]>();
const listeners = new Set<() => void>();

function notify(): void {
  listeners.forEach((listener) => listener());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function dispatchKnowledgeReferences(
  payload: KnowledgeReferencesPayload,
): void {
  if (!payload.messageId || !Array.isArray(payload.references)) return;
  referencesByMessageId.set(payload.messageId, payload.references);
  notify();
}

export function restoreKnowledgeReferences(
  messageId: string,
  references: KnowledgeReference[] | undefined,
): void {
  if (!messageId || !references?.length) return;
  referencesByMessageId.set(messageId, references);
}

export function clearKnowledgeReferences(): void {
  if (referencesByMessageId.size === 0) return;
  referencesByMessageId.clear();
  notify();
}

export function notifyKnowledgeReferencesRestored(): void {
  notify();
}

export function useKnowledgeReferences(
  messageId: string,
): KnowledgeReference[] {
  return useSyncExternalStore(
    subscribe,
    () => referencesByMessageId.get(messageId) ?? EMPTY_REFERENCES,
    () => EMPTY_REFERENCES,
  );
}
