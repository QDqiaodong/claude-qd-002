package com.repair.workshop.controller;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Technician;
import com.repair.workshop.repository.TechnicianRepository;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianRepository technicians;

    public TechnicianController(TechnicianRepository technicians) {
        this.technicians = technicians;
    }

    @GetMapping
    public List<Technician> list() {
        return technicians.findAll();
    }

    @PostMapping
    public Technician create(@RequestBody Technician input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("工号不能为空");
        }
        if (technicians.existsByCode(input.code)) {
            throw new BizException("工号 " + input.code + " 已经存在");
        }
        Technician saved = new Technician();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.level = (input.level == null || input.level.isBlank()) ? "初级" : input.level;
        saved.status = (input.status == null || input.status.isBlank()) ? "在岗" : input.status;
        return technicians.save(saved);
    }

    @PutMapping("/{id}/status")
    public Technician setStatus(@PathVariable Long id, @RequestParam String status) {
        Technician t = technicians.findById(id)
                .orElseThrow(() -> new BizException("技师不存在"));
        t.status = status;
        return technicians.save(t);
    }
}
