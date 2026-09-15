package com.repair.workshop.controller;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Equipment;
import com.repair.workshop.service.EquipmentService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping
    public List<Equipment> list(@RequestParam(required = false) Long bayId,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) String keyword) {
        return service.list(bayId, status, keyword);
    }

    @PostMapping
    public Equipment create(@RequestBody Equipment input) {
        return service.create(input);
    }

    @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @RequestBody Equipment input) {
        return service.update(id, input);
    }
}
