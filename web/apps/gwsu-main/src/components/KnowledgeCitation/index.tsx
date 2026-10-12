import {
  createContext,
  Fragment,
  useContext,
  useMemo,
  type ReactNode,
} from 'react';
import { FileSearchOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import type {
  KnowledgeReference,
  KnowledgeReferenceLocation,
} from '@/services/knowledge-reference';
import styles from './index.module.less';

export interface KnowledgeCitationTarget {
  reference: KnowledgeReference;
  location: KnowledgeReferenceLocation;
}

interface KnowledgeCitationContextValue {
  references: KnowledgeReference[];
  onOpenCitation: (target: KnowledgeCitationTarget) => void;
}

const KnowledgeCitationContext =
  createContext<KnowledgeCitationContextValue | null>(null);

export function getKnowledgeCitationTargets(
  references: KnowledgeReference[],
): Map<string, KnowledgeCitationTarget> {
  const targets = new Map<string, KnowledgeCitationTarget>();
  references.forEach((reference) => {
    if (!reference.documentId) return;
    reference.locations.forEach((location) => {
      if (location.citationKey) {
        targets.set(location.citationKey, { reference, location });
      }
    });
  });
  return targets;
}

export function KnowledgeCitationProvider({
  references,
  onOpenCitation,
  children,
}: KnowledgeCitationContextValue & { children: ReactNode }) {
  const value = useMemo(
    () => ({ references, onOpenCitation }),
    [onOpenCitation, references],
  );
  return (
    <KnowledgeCitationContext.Provider value={value}>
      {children}
    </KnowledgeCitationContext.Provider>
  );
}

export function KnowledgeCitationButton({
  target,
  onOpenCitation,
}: {
  target: KnowledgeCitationTarget;
  onOpenCitation: (target: KnowledgeCitationTarget) => void;
}) {
  const label = `查看引用：${target.reference.fileName}`;
  return (
    <Tooltip title={label}>
      <button
        type="button"
        className={styles.citationButton}
        aria-label={label}
        onClick={() => onOpenCitation(target)}
      >
        <FileSearchOutlined aria-hidden="true" />
      </button>
    </Tooltip>
  );
}

/** 仅将当前引用数据中存在对应文档定位的标识替换为图标。 */
export function KnowledgeCitationText({ text }: { text: string }) {
  const context = useContext(KnowledgeCitationContext);
  const targets = useMemo(
    () => getKnowledgeCitationTargets(context?.references ?? []),
    [context?.references],
  );
  if (!context || !text) return text;

  const pattern = /\[(K\d+)]/g;
  const nodes: ReactNode[] = [];
  let cursor = 0;
  let match: RegExpExecArray | null;
  while ((match = pattern.exec(text)) !== null) {
    const target = targets.get(match[1]);
    if (!target) continue;
    if (match.index > cursor) {
      nodes.push(text.slice(cursor, match.index));
    }
    nodes.push(
      <KnowledgeCitationButton
        key={`${match.index}-${match[1]}`}
        target={target}
        onOpenCitation={context.onOpenCitation}
      />,
    );
    cursor = match.index + match[0].length;
  }
  if (nodes.length === 0) return text;
  if (cursor < text.length) nodes.push(text.slice(cursor));
  return <Fragment>{nodes}</Fragment>;
}
