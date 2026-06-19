package com.elasticpom.core.service.embedding;

/**
 * Converts free text into a dense vector embedding compatible with the corpus vectors
 * already stored in Elasticsearch's {@code embedPaper} field (384-dim, L2-normalized,
 * produced by sentence-transformers/all-MiniLM-L6-v2 - see backend/ingestor/embedding
 * for the Python-side ingestion counterpart).
 */
public interface EmbeddingService {

    /**
     * @param text the query text to embed
     * @return a 384-dim, L2-normalized embedding vector
     * @throws com.elasticpom.exception.EmbeddingGenerationException if embedding generation fails
     */
    float[] embed(String text);
}
