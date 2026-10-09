export interface KnowledgeReferenceLocation {
  citationKey: string;
  chunkId?: string | null;
  pageId?: string | null;
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
