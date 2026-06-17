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
}
