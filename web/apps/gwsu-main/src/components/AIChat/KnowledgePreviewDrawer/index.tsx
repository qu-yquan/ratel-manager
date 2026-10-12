import { useEffect, useMemo, useRef, useState } from 'react';
import { CopilotChatAssistantMessage } from '@copilotkit/react-core/v2';
import { Alert, Drawer, Empty, Skeleton, Typography } from 'antd';
import {
  getKnowledgeCitationPreview,
  type KnowledgeCitationPreview,
  type KnowledgeCitationPreviewBlock,
  type KnowledgeReference,
  type KnowledgeReferenceLocation,
} from '@/services/knowledge-reference';
import styles from './index.module.less';

export interface KnowledgePreviewTarget {
  reference: KnowledgeReference;
  location?: KnowledgeReferenceLocation;
}

interface KnowledgePreviewDrawerProps {
  target?: KnowledgePreviewTarget;
  onClose: () => void;
}

function normalizeHeading(value?: string | null): string {
  return (value ?? '')
    .replace(/^#{1,6}\s+/, '')
    .replace(/\s+/g, ' ')
    .trim()
    .toLocaleLowerCase();
}

function resolveTargetBlock(
  blocks: KnowledgeCitationPreviewBlock[],
  location?: KnowledgeReferenceLocation,
): KnowledgeCitationPreviewBlock | undefined {
  if (!location) return undefined;
  if (location.pageBlockId) {
    const exact = blocks.find((block) => block.id === location.pageBlockId);
    if (exact) return exact;
  }
  if (typeof location.blockOrder === 'number') {
    const ordered = blocks.find(
      (block) => block.orderNo === location.blockOrder,
    );
    if (ordered) return ordered;
  }
  const heading = normalizeHeading(location.headingPath);
  if (!heading) return undefined;
  return blocks.find((block) => normalizeHeading(block.content) === heading);
}

export function KnowledgePreviewDrawer({
  target,
  onClose,
}: KnowledgePreviewDrawerProps) {
  const [preview, setPreview] = useState<KnowledgeCitationPreview>();
  const [loading, setLoading] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const cacheRef = useRef(new Map<string, KnowledgeCitationPreview>());
  const blockElementsRef = useRef(new Map<string, HTMLDivElement>());

  const pageVersionId =
    target?.location?.pageVersionId ??
    target?.reference.locations[0]?.pageVersionId;

  useEffect(() => {
    if (!target) {
      setPreview(undefined);
      setLoadFailed(false);
      setLoading(false);
      return;
    }
    let active = true;
    const cacheKey = `${target.reference.documentId}:${
      pageVersionId ?? 'current'
    }`;
    const cached = cacheRef.current.get(cacheKey);
    if (cached) {
      setPreview(cached);
      setLoadFailed(false);
      setLoading(false);
      return () => {
        active = false;
      };
    }

    setLoading(true);
    setLoadFailed(false);
    setPreview(undefined);
    void getKnowledgeCitationPreview(target.reference.documentId, pageVersionId)
      .then((result) => {
        if (!active) return;
        cacheRef.current.set(cacheKey, result);
        setPreview(result);
      })
      .catch(() => {
        if (active) setLoadFailed(true);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [pageVersionId, target]);

  const targetBlock = useMemo(
    () => resolveTargetBlock(preview?.blocks ?? [], target?.location),
    [preview?.blocks, target?.location],
  );
  const shouldHighlight = Boolean(target?.location);
  const locationMissing =
    shouldHighlight && !loading && Boolean(preview) && !targetBlock;

  useEffect(() => {
    if (!targetBlock) return;
    const frame = window.requestAnimationFrame(() => {
      blockElementsRef.current.get(targetBlock.id)?.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
      });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [targetBlock]);

  return (
    <Drawer
      title={target?.reference.fileName ?? '引用文档'}
      open={Boolean(target)}
      placement="right"
      size="large"
      destroyOnHidden
      onClose={onClose}
    >
      {loading && <Skeleton active paragraph={{ rows: 12 }} />}
      {!loading && loadFailed && (
        <Empty description="引用文档加载失败，请稍后重试" />
      )}
      {locationMissing && (
        <Alert
          className={styles.locationAlert}
          type="warning"
          showIcon
          message="文档内容已更新，原引用位置无法精确定位"
        />
      )}
      {!loading && !loadFailed && preview?.blocks.length === 0 && (
        <Empty description="该版本暂无可预览内容" />
      )}
      {!loading && !loadFailed && Boolean(preview?.blocks.length) && (
        <article className={styles.document} aria-label="引用文档内容">
          {preview?.blocks.map((block) => {
            const highlighted = shouldHighlight && block.id === targetBlock?.id;
            return (
              <div
                key={block.id}
                ref={(element) => {
                  if (element) blockElementsRef.current.set(block.id, element);
                  else blockElementsRef.current.delete(block.id);
                }}
                className={`${styles.block} ${
                  highlighted ? styles.highlightedBlock : ''
                }`}
              >
                {highlighted && (
                  <Typography.Text className={styles.hitLabel}>
                    引用位置
                  </Typography.Text>
                )}
                <CopilotChatAssistantMessage.MarkdownRenderer
                  content={block.content}
                  mode="static"
                />
              </div>
            );
          })}
        </article>
      )}
    </Drawer>
  );
}
