import React, { useMemo } from 'react';
import { FileSearchOutlined } from '@ant-design/icons';
import { CopilotChatAssistantMessage } from '@copilotkit/react-core/v2';
import { Tooltip } from 'antd';
import type {
  KnowledgeReference,
  KnowledgeReferenceLocation,
} from '@/services/knowledge-reference';
import styles from './index.module.less';

interface MarkdownNode {
  type: string;
  value?: string;
  url?: string;
  title?: string | null;
  children?: MarkdownNode[];
}

interface CitationPluginOptions {
  citationKeys: string[];
}

interface CitationTarget {
  reference: KnowledgeReference;
  location: KnowledgeReferenceLocation;
}

type DefaultMarkdownProps = React.ComponentProps<
  typeof CopilotChatAssistantMessage.MarkdownRenderer
>;
type RemarkPlugins = NonNullable<DefaultMarkdownProps['remarkPlugins']>;

interface KnowledgeCitationRendererProps extends DefaultMarkdownProps {
  references: KnowledgeReference[];
  onOpenCitation: (
    reference: KnowledgeReference,
    location: KnowledgeReferenceLocation,
  ) => void;
}

const CITATION_PATTERN = /\[(K\d+)]/g;
const PROTECTED_NODE_TYPES = new Set([
  'code',
  'inlineCode',
  'link',
  'image',
  'definition',
]);
const CITATION_LINK_PREFIX = '#knowledge-citation-';

function transformTextNode(
  node: MarkdownNode,
  citationKeys: Set<string>,
): MarkdownNode[] {
  const value = node.value ?? '';
  const transformed: MarkdownNode[] = [];
  let cursor = 0;
  let match: RegExpExecArray | null;

  CITATION_PATTERN.lastIndex = 0;
  while ((match = CITATION_PATTERN.exec(value)) !== null) {
    const citationKey = match[1];
    if (!citationKeys.has(citationKey)) continue;
    if (match.index > cursor) {
      transformed.push({
        type: 'text',
        value: value.slice(cursor, match.index),
      });
    }
    transformed.push({
      type: 'link',
      url: `${CITATION_LINK_PREFIX}${citationKey}`,
      title: null,
      children: [{ type: 'text', value: match[0] }],
    });
    cursor = match.index + match[0].length;
  }

  if (transformed.length === 0) return [node];
  if (cursor < value.length) {
    transformed.push({ type: 'text', value: value.slice(cursor) });
  }
  return transformed;
}

function transformNode(node: MarkdownNode, citationKeys: Set<string>): void {
  if (!node.children || PROTECTED_NODE_TYPES.has(node.type)) return;
  const transformedChildren: MarkdownNode[] = [];
  node.children.forEach((child) => {
    if (child.type === 'text') {
      transformedChildren.push(...transformTextNode(child, citationKeys));
      return;
    }
    transformNode(child, citationKeys);
    transformedChildren.push(child);
  });
  node.children = transformedChildren;
}

function remarkKnowledgeCitations(options: CitationPluginOptions) {
  const citationKeys = new Set(options.citationKeys);
  return (tree: MarkdownNode) => transformNode(tree, citationKeys);
}

export function KnowledgeCitationRenderer({
  references,
  onOpenCitation,
  components,
  remarkPlugins,
  ...props
}: KnowledgeCitationRendererProps) {
  const citationTargets = useMemo(() => {
    const targets = new Map<string, CitationTarget>();
    references.forEach((reference) => {
      reference.locations.forEach((location) => {
        targets.set(location.citationKey, { reference, location });
      });
    });
    return targets;
  }, [references]);

  const citationKeys = useMemo(
    () => Array.from(citationTargets.keys()),
    [citationTargets],
  );

  const mergedRemarkPlugins = useMemo<RemarkPlugins>(() => {
    const citationPlugin = [
      remarkKnowledgeCitations,
      { citationKeys },
    ] as unknown as RemarkPlugins[number];
    return [...(remarkPlugins ?? []), citationPlugin];
  }, [citationKeys, remarkPlugins]);

  const mergedComponents = useMemo(
    () => ({
      ...components,
      a: ({
        href,
        node: _node,
        children,
        ...anchorProps
      }: React.AnchorHTMLAttributes<HTMLAnchorElement> & {
        node?: unknown;
      }) => {
        const citationKey = href?.startsWith(CITATION_LINK_PREFIX)
          ? href.slice(CITATION_LINK_PREFIX.length)
          : undefined;
        const target = citationKey
          ? citationTargets.get(citationKey)
          : undefined;
        if (!target) {
          return (
            <a href={href} {...anchorProps}>
              {children}
            </a>
          );
        }
        const label = `查看引用：${target.reference.fileName}`;
        return (
          <Tooltip title={label}>
            <button
              type="button"
              className={styles.citationButton}
              aria-label={label}
              onClick={() => onOpenCitation(target.reference, target.location)}
            >
              <FileSearchOutlined aria-hidden="true" />
            </button>
          </Tooltip>
        );
      },
    }),
    [citationTargets, components, onOpenCitation],
  );

  return (
    <CopilotChatAssistantMessage.MarkdownRenderer
      {...props}
      components={mergedComponents}
      remarkPlugins={mergedRemarkPlugins}
    />
  );
}
