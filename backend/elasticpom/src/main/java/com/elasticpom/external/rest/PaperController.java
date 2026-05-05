package com.elasticpom.external.rest;

import com.elasticpom.adapters.dto.PaperDto;
import com.elasticpom.core.service.PaperService;
import com.elasticpom.external.mapper.PaperMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public List<PaperDto> getMostRelevantPapers(@RequestParam(name = "page-size") Integer pageSize, @RequestParam("page") Integer page){
        if(pageSize < 10 || pageSize > 50){
            return List.of();
        }
        return service.getPapersByDefaultRelevance(pageSize, page).stream().map(paperMapper::toDto).toList();

    }
}
