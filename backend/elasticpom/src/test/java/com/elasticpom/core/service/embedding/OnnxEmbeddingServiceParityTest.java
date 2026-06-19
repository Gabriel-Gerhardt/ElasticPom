package com.elasticpom.core.service.embedding;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-model parity test: feeds a sample query through the actual all-MiniLM-L6-v2 ONNX
 * model and asserts the output is a unit-length 384-dim vector (full numeric parity against
 * the Python sentence-transformers output would additionally require a recorded fixture
 * vector, which is out of scope here since the model can't be fetched in this sandbox).
 * <p>
 * This sandbox has no outbound network access to huggingface.co (verified: DNS/HTTP calls
 * there return "host_not_allowed"), so {@code ./gradlew downloadEmbeddingModel} cannot run
 * here and this test self-skips via {@link Assumptions#assumeTrue} when the model files
 * aren't present at the configured cache path. In an environment where the download task HAS
 * been run (e.g. local dev machine, CI with network egress), this test will actually load
 * and run the real ONNX model and execute for real.
 */
class OnnxEmbeddingServiceParityTest {

    @Test
    void embed_realModel_producesUnitLength384DimVector() {
        EmbeddingModelProperties properties = new EmbeddingModelProperties();
        boolean modelPresent = Files.exists(Path.of(properties.getPath()))
                && Files.exists(Path.of(properties.getTokenizerPath()));
        Assumptions.assumeTrue(modelPresent,
                "all-MiniLM-L6-v2 ONNX model not present at " + properties.getPath()
                        + " - run './gradlew downloadEmbeddingModel' to fetch it (requires network access "
                        + "to huggingface.co, unavailable in this sandbox) before this test will actually run.");

        OnnxEmbeddingService service = new OnnxEmbeddingService(properties);
        float[] vector = service.embed("deep learning for natural language processing");

        assertThat(vector).hasSize(384);
        double normSquared = 0;
        for (float v : vector) {
            normSquared += (double) v * v;
        }
        assertThat(Math.sqrt(normSquared)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-3));
    }
}
