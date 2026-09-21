import { get, getFileInfo, post } from '@gwsu/core';
import type { KnowledgeBlockType, KnowledgeDocument, KnowledgeDocumentBlocks, KnowledgeEvent, KnowledgeNode, KnowledgeSearchResult, PageResult } from '../types';

const BASE = '/kit/knowledge';

export async function getDirectoryTree(management = false): Promise<KnowledgeNode[]> {
  const path = management ? '/directory/manage/tree' : '/directory/tree';
  const response = await get<KnowledgeNode[]>(`${BASE}${path}`);
  return response.data ?? [];
}

export async function getChildren(parentId: string | undefined, name: string, pageNum: number, pageSize: number): Promise<PageResult<KnowledgeNode>> {
  const response = await get<PageResult<KnowledgeNode>>(`${BASE}/node/children`, { parentId, name, pageNum, pageSize });
  return response.data;
}

export async function searchKnowledgeDocuments(name: string, pageNum: number, pageSize: number): Promise<PageResult<KnowledgeNode>> {
  const response = await get<PageResult<KnowledgeNode>>(`${BASE}/node/search`, { name, pageNum, pageSize });
  return response.data;
}

export async function saveDirectory(data: { id?: string; parentId?: string; name: string }): Promise<string> {
  const response = await post<string>(`${BASE}/directory/save`, data);
  return response.data;
}

export async function deleteKnowledgeNodes(ids: string[]): Promise<void> {
  await post<void>(`${BASE}/node/delete`, { ids });
}

export async function saveKnowledgeDocument(data: { parentId?: string; fileId: string; fileName: string }): Promise<string> {
  const response = await post<string>(`${BASE}/document/save`, data);
  return response.data;
}

export async function getKnowledgeDocument(id: string): Promise<KnowledgeDocument> {
  const response = await post<KnowledgeDocument>(`${BASE}/document/${id}`, {});
  return response.data;
}

export async function toggleDocument(id: string, enabled: boolean): Promise<void> {
  await post<void>(`${BASE}/document/${enabled ? 'enable' : 'disable'}/${id}`, {});
}

export async function retryKnowledgeTask(taskId: string): Promise<void> {
  await post<void>(`${BASE}/task/retry/${taskId}`, {});
}

export async function getMarkdown(id: string): Promise<string> {
  const response = await get<string>(`${BASE}/document/${id}/markdown`);
  return response.data ?? '';
}

export async function getDocumentBlocks(id: string): Promise<KnowledgeDocumentBlocks> {
  const response = await get<KnowledgeDocumentBlocks>(`${BASE}/document/${id}/blocks`);
  return response.data ?? { blocks: [] };
}

export async function getBlockTypeOptions(): Promise<Array<{ label: string; value: KnowledgeBlockType }>> {
  const response = await get<Array<{ key: KnowledgeBlockType; value: string }>>(`${BASE}/document/block-types`);
  return (response.data ?? []).map((item) => ({ label: item.value, value: item.key }));
}

export async function saveMarkdownBlocks(documentId: string, data: KnowledgeDocumentBlocks): Promise<void> {
  const normalizeImage = (content: string) => content.replace(/!\[([^\]]*)\]\(([^)\s]+)\)/g, (_, altText: string, src: string) => {
    const matched = src.match(/(?:^|\/)kit\/file\/stream\/([^/?#)]+)/);
    return matched?.[1] ? `![${altText}](knowledge_image:fileId=${matched[1]})` : `![${altText}](${src})`;
  });
  await post<void>(`${BASE}/document/markdown`, {
    documentId, pageVersionId: data.pageVersionId,
    blocks: data.blocks.map(({ id, blockType, content }) => ({ id, blockType, content: normalizeImage(content) })),
  });
}

export async function getEvents(id: string): Promise<KnowledgeEvent[]> {
  const response = await get<KnowledgeEvent[]>(`${BASE}/document/${id}/events`);
  return response.data ?? [];
}

export async function debugSearch(data: { keyword: string; size: number; mode: 'DIRECTORY' | 'ROLE'; directoryId?: string; roleCode?: string }): Promise<KnowledgeSearchResult[]> {
  const response = await post<KnowledgeSearchResult[]>(`${BASE}/search/debug`, data);
  return response.data ?? [];
}

export async function getAdjacentKnowledgeChunks(data: {
  pageBlockId: string; direction: 'PREVIOUS' | 'NEXT'; mode: 'DIRECTORY' | 'ROLE';
  directoryId?: string; roleCode?: string;
}): Promise<KnowledgeSearchResult[]> {
  const response = await post<KnowledgeSearchResult[]>(`${BASE}/search/debug/adjacent`, { ...data, offset: 1 });
  return response.data ?? [];
}

export async function getRoleDirectoryIds(roleCode: string): Promise<string[]> {
  const response = await get<string[]>(`${BASE}/role/${encodeURIComponent(roleCode)}/directories`);
  return response.data ?? [];
}

export async function saveRoleDirectoryIds(roleCode: string, directoryIds: string[]): Promise<void> {
  await post<void>(`${BASE}/role/directories`, { roleCode, directoryIds });
}

export async function resolveFileName(fileId: string): Promise<string> {
  const response = await getFileInfo(fileId);
  const info = response.data;
  if (!info?.fileName) return '';
  return info.fileSuffix ? `${info.fileName}.${info.fileSuffix}` : info.fileName;
}
