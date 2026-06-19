package com.elasticpom.core.service.embedding;

/**
 * Pure math used to turn a transformer's per-token output into a single sentence vector,
 * matching sentence-transformers' default pooling mode ("mean" pooling) used by
 * all-MiniLM-L6-v2: average the token embeddings, excluding padding positions via the
 * attention mask (NOT a naive average over every row, which would dilute the vector with
 * padding-token noise), then L2-normalize the result.
 */
final class MeanPooling {

    private MeanPooling() {
    }

    /**
     * @param tokenEmbeddings [numTokens][hiddenSize] raw last-hidden-state output
     * @param attentionMask   [numTokens], 1 for a real token, 0 for padding
     * @return the mean-pooled, L2-normalized sentence embedding, length hiddenSize
     */
    static float[] pool(float[][] tokenEmbeddings, long[] attentionMask) {
        if (tokenEmbeddings.length != attentionMask.length) {
            throw new IllegalArgumentException("tokenEmbeddings and attentionMask must have the same length");
        }
        if (tokenEmbeddings.length == 0) {
            throw new IllegalArgumentException("tokenEmbeddings must not be empty");
        }

        int hiddenSize = tokenEmbeddings[0].length;
        double[] sum = new double[hiddenSize];
        long validTokens = 0;

        for (int t = 0; t < tokenEmbeddings.length; t++) {
            if (attentionMask[t] == 0) {
                continue;
            }
            validTokens++;
            float[] row = tokenEmbeddings[t];
            for (int h = 0; h < hiddenSize; h++) {
                sum[h] += row[h];
            }
        }

        if (validTokens == 0) {
            throw new IllegalArgumentException("attentionMask has no non-padding tokens");
        }

        float[] mean = new float[hiddenSize];
        for (int h = 0; h < hiddenSize; h++) {
            mean[h] = (float) (sum[h] / validTokens);
        }

        return l2Normalize(mean);
    }

    static float[] l2Normalize(float[] vector) {
        double sumSquares = 0;
        for (float v : vector) {
            sumSquares += (double) v * v;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm == 0) {
            return vector;
        }
        float[] normalized = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            normalized[i] = (float) (vector[i] / norm);
        }
        return normalized;
    }
}
