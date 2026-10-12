export interface KnowledgeReferenceLocation {
  citationKey: string;
  chunkId?: string | null;
  pageId?: string | null;
  pageVersionId?: string | null;
  pageBlockId?: string | null;
  headingPath?: string | null;
  blockOrder?: number | null;
}

export interface KnowledgeReference {
  documentId: string;
  fileId?: string | null;
  fileName: string;
  fileFormat?: string | null;
  chunkCount: number;
  bestScore?: number | null;
  locations: KnowledgeReferenceLocation[];
}

export interface KnowledgeReferencesPayload {
  messageId: string;
  references: KnowledgeReference[];
}

export interface KnowledgeCitationPreviewBlock {
  id: string;
  orderNo: number;
  blockType: string;
  content: string;
  sourceLocator?: string | null;
}

export interface KnowledgeCitationPreview {
  pageVersionId?: string | null;
  blocks: KnowledgeCitationPreviewBlock[];
}
