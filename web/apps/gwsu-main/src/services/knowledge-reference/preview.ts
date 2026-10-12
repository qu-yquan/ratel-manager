import { get } from '@gwsu/core';
import type { KnowledgeCitationPreview } from './types';

export async function getKnowledgeCitationPreview(
  documentId: string,
  pageVersionId?: string | null,
): Promise<KnowledgeCitationPreview> {
  const response = await get<KnowledgeCitationPreview>(
    `/kit/knowledge/document/${encodeURIComponent(
      documentId,
    )}/citation-preview`,
    { pageVersionId: pageVersionId || undefined },
  );
  return response.data ?? { blocks: [] };
}
