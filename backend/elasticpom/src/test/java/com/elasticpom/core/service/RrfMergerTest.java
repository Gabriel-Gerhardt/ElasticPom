package com.elasticpom.core.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RrfMergerTest {

    @Test
    void merge_idInBothLists_ranksAboveIdInOnlyOneList() {
        List<String> syntactic = List.of("a", "b", "c");
        List<String> semantic = List.of("b", "d", "e");

        List<String> merged = RrfMerger.merge(syntactic, semantic, 5);

        // "b" appears at rank 2 in both lists, so it should fuse to the top.
        assertThat(merged.get(0)).isEqualTo("b");
        assertThat(merged).containsExactlyInAnyOrder("a", "b", "c", "d", "e");
    }

    @Test
    void merge_respectsLimit() {
        List<String> syntactic = List.of("a", "b", "c");
        List<String> semantic = List.of("d", "e", "f");

        List<String> merged = RrfMerger.merge(syntactic, semantic, 2);

        assertThat(merged).hasSize(2);
    }

    @Test
    void merge_higherRankInSingleListBeatsLowerRankInBoth() {
        // "a" is rank 1 in syntactic only; "z" is rank 1 in both lists - "z" should win.
        List<String> syntactic = List.of("z", "a");
        List<String> semantic = List.of("z", "y");

        List<String> merged = RrfMerger.merge(syntactic, semantic, 4);

        assertThat(merged.get(0)).isEqualTo("z");
    }

    @Test
    void merge_emptyLists_returnsEmpty() {
        assertThat(RrfMerger.merge(List.of(), List.of(), 10)).isEmpty();
    }

    @Test
    void merge_oneEmptyList_returnsOtherListOrderPreserved() {
        List<String> syntactic = List.of("a", "b", "c");
        List<String> merged = RrfMerger.merge(syntactic, List.of(), 10);

        assertThat(merged).containsExactly("a", "b", "c");
    }

    // -------------------------------------------------------------------------
    // Fairness: completely reversed rankings of the same 3 ids must fuse to an
    // order that is NEITHER input list verbatim - proving genuine blending
    // rather than one leg silently dominating - with the winner pinned to the
    // actual RRF formula at k=60 (DEFAULT_K), not just "contains both ids".
    // -------------------------------------------------------------------------

    @Test
    void merge_reversedRankings_mergedOrderMatchesNeitherInputListVerbatim() {
        // docX/docZ are rank-1-in-one-list/rank-3-in-the-other (symmetric, tied), while
        // docY is rank-2-in-both. By convexity of 1/n, a tied extreme split
        // (1/(k+1) + 1/(k+3)) scores HIGHER than the equal-middle split (2/(k+2)), so
        // docX and docZ - not docY - fuse to the top, ahead of where either single
        // input list ranks them relative to the other's leader.
        List<String> listA = List.of("docX", "docY", "docZ");
        List<String> listB = List.of("docZ", "docY", "docX");

        List<String> merged = RrfMerger.merge(listA, listB, 3);

        assertThat(merged).isNotEqualTo(listA);
        assertThat(merged).isNotEqualTo(listB);
        double extremeScore = 1.0 / 61 + 1.0 / 63;
        double middleScore = 1.0 / 62 + 1.0 / 62;
        assertThat(extremeScore).isGreaterThan(middleScore);
        // docX and docZ tie for first (both score "extremeScore"); docY is fused last.
        assertThat(merged.get(2)).isEqualTo("docY");
        assertThat(merged.subList(0, 2)).containsExactlyInAnyOrder("docX", "docZ");
    }

    // -------------------------------------------------------------------------
    // Fairness, formula-level: directly verify the RRF score arithmetic at the
    // standard k=60 the merge() overload defaults to, rather than only
    // asserting on relative order.
    // -------------------------------------------------------------------------

    @Test
    void merge_scoreFormula_matchesReciprocalRankFusionAtK60() {
        // "shared" is rank 1 in both lists: score = 1/(60+1) + 1/(60+1) = 2/61.
        // "onlyA" is rank 2 in list A only: score = 1/(60+2) = 1/62.
        // 2/61 (~0.03279) > 1/62 (~0.01613), so "shared" must rank above "onlyA".
        List<String> listA = List.of("shared", "onlyA");
        List<String> listB = List.of("shared");

        List<String> merged = RrfMerger.merge(listA, listB, 2, 60);

        double sharedScore = 1.0 / 61 + 1.0 / 61;
        double onlyAScore = 1.0 / 62;
        assertThat(sharedScore).isGreaterThan(onlyAScore);
        assertThat(merged).containsExactly("shared", "onlyA");
    }

    // -------------------------------------------------------------------------
    // Fairness: neither leg's full list, taken alone, predicts the merged
    // top-N when ranks genuinely disagree across 5 distinct-ish ids - this
    // guards against a regression where the merge silently degenerates into
    // "return list A" or "return list B".
    // -------------------------------------------------------------------------

    @Test
    void merge_disagreeingRankings_topResultIsNotDeterminedBySingleLeg() {
        List<String> syntactic = List.of("p1", "p2", "p3", "p4", "p5");
        List<String> semantic = List.of("p5", "p4", "p1", "p2", "p3");

        List<String> merged = RrfMerger.merge(syntactic, semantic, 5);

        assertThat(merged).isNotEqualTo(syntactic);
        assertThat(merged).isNotEqualTo(semantic);
        assertThat(merged).containsExactlyInAnyOrderElementsOf(syntactic);
    }
}
