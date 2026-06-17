package com.elasticpom.core.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reciprocal Rank Fusion: merges two independently ranked id lists into one ranking
 * using only each id's rank (not its raw score), since BM25 and kNN similarity scores
 * are not on comparable scales.
 * <p>
 * score(id) = 1/(k + rank_in_listA) + 1/(k + rank_in_listB), rank is 1-based, missing
 * from a list contributes 0 for that list. Higher score wins.
 * <p>
 * Ceiling: this is an O(n) merge of two small (page-sized) lists held in memory, fine for
 * single-page fusion. It does not support fusing more than two ranked lists or multi-page
 * re-ranking; extend the loop over an arbitrary number of ranked lists if that's ever needed.
 */
final class RrfMerger {

    /** Standard RRF smoothing constant, as used in the original RRF paper. */
    private static final int DEFAULT_K = 60;

    private RrfMerger() {
    }

    static List<String> merge(List<String> listA, List<String> listB, int limit) {
        return merge(listA, listB, limit, DEFAULT_K);
    }

    static List<String> merge(List<String> listA, List<String> listB, int limit, int k) {
        Map<String, Double> scores = new LinkedHashMap<>();
        addRankScores(scores, listA, k);
        addRankScores(scores, listB, k);

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static void addRankScores(Map<String, Double> scores, List<String> ids, int k) {
        for (int i = 0; i < ids.size(); i++) {
            double rankScore = 1.0 / (k + i + 1);
            scores.merge(ids.get(i), rankScore, Double::sum);
        }
    }
}
