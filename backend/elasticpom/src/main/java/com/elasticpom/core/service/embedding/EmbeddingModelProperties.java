package com.elasticpom.core.service.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Local filesystem locations of the ONNX model + tokenizer files for {@link OnnxEmbeddingService}.
 * Defaults to the Gradle-managed on-demand cache populated by the {@code downloadEmbeddingModel}
 * task (see build.gradle); override via {@code embedding.model.*} properties (e.g. in tests, or to
 * point at a model baked into a container image) instead of fetching from HuggingFace at runtime.
 */
@ConfigurationProperties(prefix = "embedding.model")
public class EmbeddingModelProperties {

    private String path = ".model-cache/all-MiniLM-L6-v2/model.onnx";
    private String tokenizerPath = ".model-cache/all-MiniLM-L6-v2/tokenizer.json";

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTokenizerPath() {
        return tokenizerPath;
    }

    public void setTokenizerPath(String tokenizerPath) {
        this.tokenizerPath = tokenizerPath;
    }
}
