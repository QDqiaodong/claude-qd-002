package com.repair.workshop.service;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Bay;
import com.repair.workshop.entity.Equipment;
import com.repair.workshop.entity.Technician;
import com.repair.workshop.entity.WorkOrder;
import com.repair.workshop.repository.BayRepository;
import com.repair.workshop.repository.EquipmentRepository;
import com.repair.workshop.repository.TechnicianRepository;
import com.repair.workshop.repository.WorkOrderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {

    private final WorkOrderRepository orders;
    private final BayRepository bays;
    private final TechnicianRepository technicians;
    private final EquipmentRepository equipments;

    public WorkOrderService(WorkOrderRepository orders, BayRepository bays,
                            TechnicianRepository technicians, EquipmentRepository equipments) {
        this.orders = orders;
        this.bays = bays;
        this.technicians = technicians;
        this.equipments = equipments;
    }

    public List<WorkOrder> list(String status, LocalDate date, String keyword) {
        List<WorkOrder> all = (date == null)
                ? orders.findAllByOrderByUpdatedAtDesc()
                : orders.findByPlanDateOrderByStartMinAsc(date);
        return all.stream()
                .filter(o -> status == null || status.isEmpty() || status.equals(o.status))
                .filter(o -> keyword == null || keyword.isEmpty()
                        || o.plate.contains(keyword) || o.orderNo.contains(keyword)
                        || (o.customer != null && o.customer.contains(keyword)))
                .toList();
    }

    private String nextOrderNo() {
        long n = orders.count() + 1;
        String no;
        do {
            no = "WO-" + String.format("%04d", n++);
        } while (orders.existsByOrderNo(no));
        return no;
    }

    @Transactional
    public WorkOrder open(WorkOrder input) {
        if (input.plate == null || input.plate.isBlank()) {
            throw new BizException("车牌不能为空");
        }
        WorkOrder saved = new WorkOrder();
        saved.orderNo = nextOrderNo();
        saved.plate = input.plate.trim().toUpperCase();
        saved.model = input.model;
        saved.customer = input.customer;
        saved.phone = input.phone;
        saved.kind = (input.kind == null || input.kind.isBlank()) ? "保养" : input.kind;
        saved.faultDesc = input.faultDesc;
        saved.status = "待派工";
        saved.createdAt = LocalDateTime.now();
        saved.updatedAt = saved.createdAt;
        return orders.save(saved);
    }

    /** 派工：定工位、定技师、定计划时段，同时段占用在这里拦。 */
    @Transactional
    public WorkOrder assign(Long id, WorkOrder input) {
        WorkOrder order = orders.findById(id).orElseThrow(() -> new BizException("工单不存在"));
        if (!"待派工".equals(order.status)) {
            throw new BizException("只有待派工的工单能派工，这张现在是 " + order.status);
        }
        if (input.bayId == null) {
            throw new BizException("请选一个工位");
        }
        if (input.technicianId == null) {
            throw new BizException("请选一位技师");
        }
        if (input.planDate == null) {
            throw new BizException("请选计划进场日期");
        }
        if (input.startMin == null || input.endMin == null || input.endMin <= input.startMin) {
            throw new BizException("计划结束时间必须晚于开始时间");
        }

        Bay bay = bays.findById(input.bayId).orElseThrow(() -> new BizException("工位不存在"));
        Technician tech = technicians.findById(input.technicianId)
                .orElseThrow(() -> new BizException("技师不存在"));

        if (!"可用".equals(bay.status)) {
            throw new BizException("工位 " + bay.name + " 已经停用，不能再接车");
        }
        if (!"在岗".equals(tech.status)) {
            throw new BizException("技师 " + tech.name + " 现在不是在岗状态（" + tech.status + "）");
        }
        List<Equipment> bayEquipments = equipments.findAll().stream()
                .filter(e -> bay.id.equals(e.bayId))
                .toList();
        for (Equipment e : bayEquipments) {
            if (!"可用".equals(e.status)) {
                throw new BizException("工位 " + bay.name + " 上的 " + e.name
                        + " 现在是" + e.status + "，先处理这台设备再派工");
            }
        }

        for (WorkOrder other : orders.findByBayIdAndPlanDateAndStatusNot(
                bay.id, input.planDate, "已取消")) {
            if ("已交车".equals(other.status)) {
                continue;
            }
            if (overlap(other, input)) {
                throw new BizException("工位 " + bay.name + " 这个时段已经被工单 "
                        + other.orderNo + " 占了");
            }
        }
        for (WorkOrder other : orders.findByTechnicianIdAndPlanDateAndStatusNot(
                tech.id, input.planDate, "已取消")) {
            if ("已交车".equals(other.status)) {
                continue;
            }
            if (overlap(other, input)) {
                throw new BizException("技师 " + tech.name + " 这个时段已经在做工单 "
                        + other.orderNo + " 了，一个人不能同时上两台车");
            }
        }

        order.bayId = bay.id;
        order.technicianId = tech.id;
        order.planDate = input.planDate;
        order.startMin = input.startMin;
        order.endMin = input.endMin;
        order.status = "施工中";
        order.updatedAt = LocalDateTime.now();
        return orders.save(order);
    }

    private boolean overlap(WorkOrder a, WorkOrder b) {
        if (a.startMin == null || a.endMin == null) {
            return false;
        }
        return a.startMin < b.endMin && b.startMin < a.endMin;
    }

    @Transactional
    public WorkOrder advance(Long id, String action, String qcResult) {
        WorkOrder order = orders.findById(id).orElseThrow(() -> new BizException("工单不存在"));

        if ("finish".equals(action)) {
            if (!"施工中".equals(order.status)) {
                throw new BizException("只有施工中的工单能完工送检，这张现在是 " + order.status);
            }
            order.status = "待质检";
        } else if ("qc".equals(action)) {
            if (!"待质检".equals(order.status)) {
                throw new BizException("只有待质检的工单能质检，这张现在是 " + order.status);
            }
            if ("合格".equals(qcResult)) {
                order.qcResult = "合格";
                order.status = "已交车";
            } else if ("返工".equals(qcResult)) {
                order.qcResult = "返工";
                order.status = "施工中";
            } else {
                throw new BizException("质检结论只能是合格或返工");
            }
        } else if ("cancel".equals(action)) {
            if ("已交车".equals(order.status)) {
                throw new BizException("已经交车的工单不能取消");
            }
            if ("已取消".equals(order.status)) {
                throw new BizException("这张工单已经取消过了");
            }
            order.status = "已取消";
        } else {
            throw new BizException("不认识的动作：" + action);
        }
        order.updatedAt = LocalDateTime.now();
        return orders.save(order);
    }
}
