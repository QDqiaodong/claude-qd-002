package com.repair.workshop.service;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Equipment;
import com.repair.workshop.repository.BayRepository;
import com.repair.workshop.repository.EquipmentRepository;
import com.repair.workshop.repository.WorkOrderRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipmentService {

    private final EquipmentRepository equipments;
    private final BayRepository bays;
    private final WorkOrderRepository orders;

    public EquipmentService(EquipmentRepository equipments, BayRepository bays,
                            WorkOrderRepository orders) {
        this.equipments = equipments;
        this.bays = bays;
        this.orders = orders;
    }

    public List<Equipment> list(Long bayId, String status, String keyword) {
        return equipments.findAll().stream()
                .filter(e -> bayId == null || bayId.equals(e.bayId))
                .filter(e -> status == null || status.isEmpty() || status.equals(e.status))
                .filter(e -> keyword == null || keyword.isEmpty()
                        || e.name.contains(keyword) || e.code.contains(keyword))
                .toList();
    }

    @Transactional
    public Equipment create(Equipment input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("设备编号不能为空");
        }
        if (equipments.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的设备用掉了");
        }
        if (input.bayId != null && !bays.existsById(input.bayId)) {
            throw new BizException("归属的工位不存在");
        }
        Equipment saved = new Equipment();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.category = (input.category == null || input.category.isBlank()) ? "拆装" : input.category;
        saved.bayId = input.bayId;
        saved.status = (input.status == null || input.status.isBlank()) ? "可用" : input.status;
        return equipments.save(saved);
    }

    @Transactional
    public Equipment update(Long id, Equipment input) {
        Equipment e = equipments.findById(id).orElseThrow(() -> new BizException("设备不存在"));
        if (input.name != null) {
            e.name = input.name;
        }
        if (input.category != null) {
            e.category = input.category;
        }
        if (input.bayId != null) {
            if (!bays.existsById(input.bayId)) {
                throw new BizException("要挪过去的工位不存在");
            }
            e.bayId = input.bayId;
        }
        if (input.status != null && !input.status.isBlank() && !input.status.equals(e.status)) {
            if (("停用".equals(input.status) || "维修中".equals(input.status))
                    && e.bayId != null
                    && orders.countByBayIdAndStatusNotIn(e.bayId, List.of("已交车", "已取消")) > 0) {
                throw new BizException("这台设备所在工位还有没了结的工单，先把工单处理完再"
                        + ("停用".equals(input.status) ? "停用" : "送修"));
            }
            e.status = input.status;
        }
        return equipments.save(e);
    }
}
