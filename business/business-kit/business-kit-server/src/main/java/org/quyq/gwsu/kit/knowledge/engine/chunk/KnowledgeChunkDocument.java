package org.quyq.gwsu.kit.knowledge.engine.chunk;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.KnnSimilarity;

import java.time.Instant;
import java.util.List;

/**
 * ES-only 知识 Chunk 文档。
 */
@Document(indexName = "kit_knowledge_chunk", createIndex = false)
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChunkDocument {

    @Id
    @Field(name = "chunk_id", type = FieldType.Keyword)
    private String chunkId;

    @Field(name = "page_id", type = FieldType.Keyword)
    private String pageId;

    @Field(name = "page_version_id", type = FieldType.Keyword)
    private String pageVersionId;

    @Field(name = "page_block_id", type = FieldType.Keyword)
    private String pageBlockId;

    @Field(name = "source_document_id", type = FieldType.Keyword)
    private String sourceDocumentId;

    @Field(name = "directory_path_ids", type = FieldType.Keyword)
    private List<String> directoryPathIds;

    @Field(name = "title", type = FieldType.Text)
    private String title;

    @Field(name = "heading_path", type = FieldType.Text)
    private String headingPath;

    @Field(name = "content", type = FieldType.Text)
    private String content;

    @Field(name = "chunk_order", type = FieldType.Integer)
    private Integer chunkOrder;

    @Field(name = "content_hash", type = FieldType.Keyword)
    private String contentHash;

    @Field(name = "status", type = FieldType.Keyword)
    private String status;

    @Field(name = "version", type = FieldType.Integer)
    private Integer version;

    @Field(name = "indexed_at", type = FieldType.Date)
    private Instant indexedAt;

    @Field(name = "embedding", type = FieldType.Dense_Vector, knnSimilarity = KnnSimilarity.COSINE)
    private float[] embedding;

    @Field(name = "embedding_model", type = FieldType.Keyword)
    private String embeddingModel;
}
