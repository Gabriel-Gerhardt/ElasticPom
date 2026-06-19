package com.elasticpom.core.service.embedding;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit tests for the mean-pooling-with-attention-mask + L2-normalization math used by
 * {@link OnnxEmbeddingService}, with no dependency on ONNX Runtime or the tokenizer - this is
 * the "one runnable check" for the embedding math, and it must pass regardless of whether the
 * real all-MiniLM-L6-v2 ONNX model can be downloaded in a given environment (see
 * OnnxEmbeddingServiceParityTest for that, sandbox-dependent, check).
 * <p>
 * MeanPooling is package-private by design (an internal implementation detail of the
 * embedding package), so tests reach it via reflection rather than widening its visibility
 * just for testing.
 */
class MeanPoolingTest {

    private static float[] pool(float[][] tokenEmbeddings, long[] attentionMask) throws Exception {
        Method m = MeanPooling.class.getDeclaredMethod("pool", float[][].class, long[].class);
        m.setAccessible(true);
        return (float[]) m.invoke(null, tokenEmbeddings, attentionMask);
    }

    private static float[] l2Normalize(float[] vector) throws Exception {
        Method m = MeanPooling.class.getDeclaredMethod("l2Normalize", float[].class);
        m.setAccessible(true);
        return (float[]) m.invoke(null, vector);
    }

    @Test
    void pool_excludesPaddingTokensFromTheAverage() throws Exception {
        // 3 tokens: two real, one padding. A naive average over all 3 rows would be
        // diluted toward the padding row's huge values; mean-pooling with the attention
        // mask must ignore it entirely.
        float[][] tokenEmbeddings = {
                {1f, 1f},
                {3f, 3f},
                {1000f, 1000f} // padding - attentionMask says ignore
        };
        long[] attentionMask = {1, 1, 0};

        float[] result = pool(tokenEmbeddings, attentionMask);

        // mean of (1,1) and (3,3) = (2,2), L2-normalized = (1/sqrt(2), 1/sqrt(2))
        float expected = (float) (1.0 / Math.sqrt(2));
        assertThat(result[0]).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-5f));
        assertThat(result[1]).isCloseTo(expected, org.assertj.core.data.Offset.offset(1e-5f));
    }

    @Test
    void pool_resultIsL2Normalized() throws Exception {
        float[][] tokenEmbeddings = {{3f, 4f}, {3f, 4f}};
        long[] attentionMask = {1, 1};

        float[] result = pool(tokenEmbeddings, attentionMask);

        double norm = Math.sqrt(result[0] * result[0] + result[1] * result[1]);
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }

    @Test
    void pool_mismatchedLengths_throws() {
        float[][] tokenEmbeddings = {{1f}, {2f}};
        long[] attentionMask = {1};

        assertThatThrownBy(() -> pool(tokenEmbeddings, attentionMask))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pool_allPaddingMask_throws() {
        float[][] tokenEmbeddings = {{1f, 1f}};
        long[] attentionMask = {0};

        assertThatThrownBy(() -> pool(tokenEmbeddings, attentionMask))
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void l2Normalize_zeroVector_returnedUnchangedRatherThanDividingByZero() throws Exception {
        float[] result = l2Normalize(new float[]{0f, 0f, 0f});

        assertThat(result).containsExactly(0f, 0f, 0f);
    }
}
