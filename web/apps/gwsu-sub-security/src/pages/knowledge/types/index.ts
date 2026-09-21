export type NodeType = 'DIRECTORY' | 'DOCUMENT';
export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'FAILED';

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export interface KnowledgeNode {
  id: string;
  parentId?: string | null;
  nodeType: NodeType;
  name: string;
  documentCount?: number;
  fileId?: string;
  fileName?: string;
  fileSize?: number;
  fileFormat?: string;
  documentStatus?: DocumentStatus;
  enabled?: boolean;
  embeddingCompleted?: boolean;
  imageOcrParsed?: boolean;
  parsedAt?: string;
  processedAt?: string;
  processMessage?: string;
  createTime?: string;
  modifyTime?: string;
}

export interface KnowledgeDocument extends KnowledgeNode {
  latestTaskId?: string;
  latestTaskStatus?: string;
}

export interface KnowledgeEvent {
  id: string;
  taskId?: string;
  eventType: string;
  message?: string;
  occurredAt: string;
  createOp?: string;
}

export type KnowledgeBlockType = 'HEADING' | 'PARAGRAPH' | 'LIST' | 'TABLE' | 'CODE' | 'QUOTE';

export interface KnowledgeDocumentBlock {
  id: string;
  orderNo: number;
  blockType: KnowledgeBlockType;
  content: string;
  sourceLocator?: string;
}

export interface KnowledgeDocumentBlocks {
  pageVersionId?: string;
  blocks: KnowledgeDocumentBlock[];
}

export interface KnowledgeSearchResult {
  chunkId?: string;
  pageId?: string;
  pageBlockId?: string;
  blockOrder?: number;
  chunkOrder?: number;
  sourceDocumentId: string;
  title?: string;
  headingPath?: string;
  content: string;
  score?: number;
}
