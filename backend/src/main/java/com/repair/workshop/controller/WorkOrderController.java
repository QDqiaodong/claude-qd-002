package com.repair.workshop.controller;

import com.repair.workshop.dto.QcSubmitRequest;
import com.repair.workshop.entity.QcItem;
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

    /** 这张车的质检项表（制动 / 灯光 / 路试） */
    @GetMapping("/{id}/qc-items")
    public List<QcItem> qcItems(@PathVariable Long id) {
        return service.qcList(id);
    }

    /** 按项表质检：三项全过才交车，有一项不过退回施工，缺项不收 */
    @PostMapping("/{id}/qc")
    public WorkOrder submitQc(@PathVariable Long id, @RequestBody QcSubmitRequest request) {
        return service.submitQc(id, request);
    }
}
