package com.repair.workshop.controller;

import com.repair.workshop.entity.Part;
import com.repair.workshop.entity.PartIssue;
import com.repair.workshop.service.PartService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PartController {

    private final PartService service;

    public PartController(PartService service) {
        this.service = service;
    }

    @GetMapping("/parts")
    public List<Part> list(@RequestParam(required = false) String keyword,
                           @RequestParam(required = false) String status) {
        return service.list(keyword, status);
    }

    @PostMapping("/parts")
    public Part create(@RequestBody Part input) {
        return service.create(input);
    }

    @PutMapping("/parts/{id}")
    public Part update(@PathVariable Long id, @RequestBody Part input) {
        return service.update(id, input);
    }

    @GetMapping("/issues")
    public List<PartIssue> issues(@RequestParam(required = false) Long orderId) {
        return service.issueList(orderId);
    }

    @PostMapping("/issues")
    public PartIssue issue(@RequestBody PartIssue input) {
        return service.issue(input);
    }
}
