package com.elasticpom.core.service.embedding;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.elasticpom.exception.EmbeddingGenerationException;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the ONNX export of sentence-transformers/all-MiniLM-L6-v2 with ONNX Runtime for
 * inference and DJL's standalone HuggingFace tokenizer binding (WordPiece, exact
 * vocab/special-token parity with the Python tokenizer - not reimplemented by hand) for
 * tokenization, then mean-pools the token embeddings (excluding padding, via the attention
 * mask) and L2-normalizes, matching the Python ingestor's embedding behaviour exactly so
 * query vectors land in the same space as the corpus vectors already stored in
 * Elasticsearch's {@code embedPaper} field.
 * <p>
 * Model/tokenizer files are NOT bundled in the jar (~90MB, not committed to git) - they are
 * loaded lazily from {@link EmbeddingModelProperties} on first use and cached as a singleton
 * session/tokenizer for the lifetime of the application context. If the files are missing
 * (e.g. {@code ./gradlew downloadEmbeddingModel} was never run) or inference throws for any
 * reason, every call fails fast with {@link EmbeddingGenerationException} so callers can
 * degrade gracefully (see {@code PaperService}).
 */
@Service
@EnableConfigurationProperties(EmbeddingModelProperties.class)
public class OnnxEmbeddingService implements EmbeddingService {

    private final EmbeddingModelProperties properties;
    private volatile OrtSession session;
    private volatile HuggingFaceTokenizer tokenizer;
    private final Object initLock = new Object();

    public OnnxEmbeddingService(EmbeddingModelProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        try {
            ensureInitialized();
            Encoding encoding = tokenizer.encode(text);
            long[] ids = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();
            long[] typeIds = encoding.getTypeIds();

            long[] shape = {1, ids.length};
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            try (OnnxTensor inputIds = OnnxTensor.createTensor(env, LongBuffer.wrap(ids), shape);
                 OnnxTensor mask = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape);
                 OnnxTensor tokenTypeIds = OnnxTensor.createTensor(env, LongBuffer.wrap(typeIds), shape)) {

                Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
                inputs.put("input_ids", inputIds);
                inputs.put("attention_mask", mask);
                if (session.getInputNames().contains("token_type_ids")) {
                    inputs.put("token_type_ids", tokenTypeIds);
                }

                try (OrtSession.Result result = session.run(inputs)) {
                    float[][][] lastHiddenState = extractLastHiddenState(result);
                    return MeanPooling.pool(lastHiddenState[0], attentionMask);
                }
            }
        } catch (Exception e) {
            throw new EmbeddingGenerationException("Failed to generate embedding for query text", e);
        }
    }

    @SuppressWarnings("unchecked")
    private float[][][] extractLastHiddenState(OrtSession.Result result) throws Exception {
        OnnxValue output = result.get(0);
        return (float[][][]) output.getValue();
    }

    private void ensureInitialized() throws OrtException, IOException {
        if (session != null) {
            return;
        }
        synchronized (initLock) {
            if (session != null) {
                return;
            }
            Path modelPath = Path.of(properties.getPath());
            Path tokenizerPath = Path.of(properties.getTokenizerPath());
            if (!Files.exists(modelPath) || !Files.exists(tokenizerPath)) {
                throw new IOException("Embedding model not found at " + modelPath
                        + " - run './gradlew downloadEmbeddingModel' or set embedding.model.path/embedding.model.tokenizer-path");
            }
            tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);
            OrtEnvironment env = OrtEnvironment.getEnvironment();
            session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions());
        }
    }

    @PreDestroy
    void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (OrtException ignored) {
            // best-effort cleanup on shutdown
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
    }
}
