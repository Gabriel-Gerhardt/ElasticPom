package com.elasticpom.external.rest;

import com.elasticpom.adapters.FilterMapper;
import com.elasticpom.adapters.FilterMapperImpl;
import com.elasticpom.core.service.FilterService;
import com.elasticpom.external.document.FilterDocument;
import com.elasticpom.external.integration.FilterRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end acceptance test for GET /api/filters, exercising the REAL
 * FilterController -> FilterService -> FilterMapper (generated MapStruct impl)
 * wiring, with only the Mongo repository boundary mocked.
 * <p>
 * This is intentionally different from FilterControllerTest (which mocks
 * FilterService and FilterMapper directly) and FilterServiceTest (which mocks
 * the mapper too). Neither of those exercises the real mapping logic end to
 * end, so a field-name typo in FilterDocument/Filter/FilterDto (e.g. Java
 * "filtername" vs. the Python ingestor's "filtername" key) would not be
 * caught by either. Here we seed FilterDocument instances shaped exactly like
 * the production seed data in backend/ingestor/mongo/filter_data_parser.py's
 * FILTERS constant, and assert the resulting JSON response matches that shape
 * field-for-field.
 */
@WebMvcTest(FilterController.class)
@Import({FilterService.class, FilterMapperImpl.class})
class FilterEndToEndAcceptanceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilterRepository filterRepository;

    // Mirrors backend/ingestor/mongo/filter_data_parser.py FILTERS exactly.
    private static final List<FilterDocument> SEEDED_PRODUCTION_FILTERS = List.of(
            new FilterDocument("subjects.keyword", 1, "option"),
            new FilterDocument("creators.keyword", 2, "option"),
            new FilterDocument("date", 3, "range")
    );

    @Test
    void getFilters_withRealMappingChain_matchesIngestorSeededShapeAndOrder() throws Exception {
        // Repository returns in arbitrary/unsorted order, as Mongo offers no guarantee.
        when(filterRepository.findAll()).thenReturn(List.of(
                SEEDED_PRODUCTION_FILTERS.get(2),
                SEEDED_PRODUCTION_FILTERS.get(0),
                SEEDED_PRODUCTION_FILTERS.get(1)
        ));

        mockMvc.perform(get("/api/filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].filtername").value("subjects.keyword"))
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[0].type").value("option"))
                .andExpect(jsonPath("$[1].filtername").value("creators.keyword"))
                .andExpect(jsonPath("$[1].order").value(2))
                .andExpect(jsonPath("$[1].type").value("option"))
                .andExpect(jsonPath("$[2].filtername").value("date"))
                .andExpect(jsonPath("$[2].order").value(3))
                .andExpect(jsonPath("$[2].type").value("range"));
    }

    @Test
    void getFilters_withRealMappingChain_emptyRepository_returnsEmptyArray() throws Exception {
        when(filterRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getFilters_withRealMappingChain_singleDocument_mapsAllThreeFieldsThroughRealMapStructImpl() throws Exception {
        when(filterRepository.findAll()).thenReturn(List.of(new FilterDocument("date", 3, "range")));

        mockMvc.perform(get("/api/filters").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filtername").value("date"))
                .andExpect(jsonPath("$[0].order").value(3))
                .andExpect(jsonPath("$[0].type").value("range"));
    }
}
