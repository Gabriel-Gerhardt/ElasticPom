package com.elasticpom.external.rest;

import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.adapters.dto.request.FilterOptionsRequest;
import com.elasticpom.adapters.dto.request.HybridSearchRequest;
import com.elasticpom.adapters.dto.request.PaperQueryRequest;
import com.elasticpom.adapters.dto.request.SemanticSearchRequest;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.exception.BadRequestException;
import com.elasticpom.adapters.PaperMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/papers")
public class PaperController {
    private final PaperService service;
    private final PaperMapper paperMapper;

    public PaperController(PaperService service, PaperMapper paperMapper) {
        this.service = service;
        this.paperMapper = paperMapper;
    }

    @GetMapping("/most-relevant/")
    public ResponseEntity<List<PaperDto>> getMostRelevantPapers(@RequestParam(name = "page-size") Integer pageSize, @RequestParam("page") Integer page){
        if(pageSize < 10 || pageSize > 50){
            throw new BadRequestException("page size must not exceed 50 and must be at least 10");
        }
        validateElasticPageSize(pageSize, page);
        List<PaperDto> paperList = service.getPapersByDefaultRelevance(pageSize, page).stream().map(paperMapper::toDto).toList();
        return ResponseEntity.ok(paperList);
    }

    @PostMapping("/search-by-query")
    public ResponseEntity<List<PaperDto>> searchPaperByQuery(@RequestBody @Validated PaperQueryRequest request){
        validateElasticPageSize(request.pageSize(), request.page());
        List<PaperDto> paperList = service.getPapersByQuery(request.query(), request.pageSize(), request.page(), request.filters()).stream().map(paperMapper::toDto).toList();
        return ResponseEntity.ok(paperList);
    }

    @PostMapping("/filter-options")
    public ResponseEntity<List<String>> getFilterOptions(@RequestBody @Validated FilterOptionsRequest request) {
        List<String> values = service.getDistinctFilterValues(request.query(), request.filterName(), request.filters());
        return ResponseEntity.ok(values);
    }

    @PostMapping("/semantic-search")
    public ResponseEntity<List<PaperDto>> semanticSearch(@RequestBody @Validated SemanticSearchRequest request) {
        validateElasticPageSize(request.pageSize(), request.page());
        List<PaperDto> papers = service.getPapersBySemanticSearch(
                request.query(), request.pageSize(), request.page()
        ).stream().map(paperMapper::toDto).toList();
        return ResponseEntity.ok(papers);
    }

    @PostMapping("/hybrid-search")
    public ResponseEntity<List<PaperDto>> hybridSearch(@RequestBody @Validated HybridSearchRequest request) {
        validateElasticPageSize(request.pageSize(), request.page());
        List<PaperDto> papers = service.getPapersByHybridSearch(
                request.query(), request.pageSize(), request.page(), request.filters()
        ).stream().map(paperMapper::toDto).toList();
        return ResponseEntity.ok(papers);
    }

    private void validateElasticPageSize(Integer pageSize, Integer page){
        if ((long) page * pageSize >= 10000) {
            throw new BadRequestException("Page too large for Elasticsearch");
        }
    }
}
