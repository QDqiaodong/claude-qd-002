package com.repair.workshop.controller;

import com.repair.workshop.entity.WorkOrder;
import com.repair.workshop.service.WorkOrderService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class WorkOrderController {

    private final WorkOrderService service;

    public WorkOrderController(WorkOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<WorkOrder> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword) {
        return service.list(status, date, keyword);
    }

    @PostMapping
    public WorkOrder open(@RequestBody WorkOrder input) {
        return service.open(input);
    }

    @PutMapping("/{id}/assign")
    public WorkOrder assign(@PathVariable Long id, @RequestBody WorkOrder input) {
        return service.assign(id, input);
    }

    @PostMapping("/{id}/advance")
    public WorkOrder advance(@PathVariable Long id,
                             @RequestParam String action,
                             @RequestParam(required = false) String qcResult) {
        return service.advance(id, action, qcResult);
    }
}
