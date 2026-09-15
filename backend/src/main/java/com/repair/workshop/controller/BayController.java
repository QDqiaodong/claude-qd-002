package com.repair.workshop.controller;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Bay;
import com.repair.workshop.repository.BayRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bays")
public class BayController {

    private final BayRepository bays;

    public BayController(BayRepository bays) {
        this.bays = bays;
    }

    @GetMapping
    public List<Bay> list() {
        return bays.findAll();
    }

    @PostMapping
    public Bay create(@RequestBody Bay input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("工位编号不能为空");
        }
        if (bays.existsByCode(input.code)) {
            throw new BizException("工位编号 " + input.code + " 已经存在");
        }
        Bay saved = new Bay();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "举升" : input.kind;
        saved.status = (input.status == null || input.status.isBlank()) ? "可用" : input.status;
        return bays.save(saved);
    }

    @PutMapping("/{id}/status")
    public Bay setStatus(@PathVariable Long id, @RequestParam String status) {
        Bay bay = bays.findById(id).orElseThrow(() -> new BizException("工位不存在"));
        bay.status = status;
        return bays.save(bay);
    }
}
