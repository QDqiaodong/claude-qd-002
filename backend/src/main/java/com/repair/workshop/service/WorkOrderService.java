package com.repair.workshop.service;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.dto.QcSubmitRequest;
import com.repair.workshop.entity.Bay;
import com.repair.workshop.entity.Equipment;
import com.repair.workshop.entity.QcItem;
import com.repair.workshop.entity.Technician;
import com.repair.workshop.entity.WorkOrder;
import com.repair.workshop.repository.BayRepository;
import com.repair.workshop.repository.EquipmentRepository;
import com.repair.workshop.repository.QcItemRepository;
import com.repair.workshop.repository.TechnicianRepository;
import com.repair.workshop.repository.WorkOrderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkOrderService {

    /** 交车前必过的固定三项，顺序就是项表上的顺序 */
    public static final List<String> QC_ITEMS = List.of("制动", "灯光", "路试");

    private final WorkOrderRepository orders;
    private final BayRepository bays;
    private final TechnicianRepository technicians;
    private final EquipmentRepository equipments;
    private final QcItemRepository qcItems;

    public WorkOrderService(WorkOrderRepository orders, BayRepository bays,
                            TechnicianRepository technicians, EquipmentRepository equipments,
                            QcItemRepository qcItems) {
        this.orders = orders;
        this.bays = bays;
        this.technicians = technicians;
        this.equipments = equipments;
        this.qcItems = qcItems;
    }

    public List<WorkOrder> list(String status, LocalDate date, String keyword) {
        List<WorkOrder> all = (date == null)
                ? orders.findAllByOrderByUpdatedAtDesc()
                : orders.findByPlanDateOrderByStartMinAsc(date);
        List<WorkOrder> filtered = all.stream()
                .filter(o -> status == null || status.isEmpty() || status.equals(o.status))
                .filter(o -> keyword == null || keyword.isEmpty()
                        || o.plate.contains(keyword) || o.orderNo.contains(keyword)
                        || (o.customer != null && o.customer.contains(keyword)))
                .toList();
        for (WorkOrder o : filtered) {
            o.failedItems = joinFailedNames(failedItemsOf(o.id));
        }
        return filtered;
    }

    /** 取一张单项表上判了「不过」的项，按固定项序返回。 */
    private List<String> failedItemsOf(Long orderId) {
        List<String> failed = new ArrayList<>();
        Map<String, String> results = new LinkedHashMap<>();
        for (QcItem qi : qcItems.findByOrderIdOrderByIdAsc(orderId)) {
            results.put(qi.item, qi.result);
        }
        for (String name : QC_ITEMS) {
            if ("不过".equals(results.get(name))) {
                failed.add(name);
            }
        }
        return failed;
    }

    private String joinFailedNames(List<String> names) {
        return names.isEmpty() ? null : String.join("、", names);
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
        WorkOrder order = orders.findWithLockingById(id)
                .orElseThrow(() -> new BizException("工单不存在"));

        if ("finish".equals(action)) {
            if (!"施工中".equals(order.status)) {
                throw new BizException("只有施工中的工单能完工送检，这张现在是 " + order.status);
            }
            order.status = "待质检";
            provisionQcItems(order.id);
        } else if ("qc".equals(action)) {
            // 交车必须带着逐项过 / 不过的项表走 /api/orders/{id}/qc，不再有只选一个总评的捷径
            throw new BizException("质检要逐项填项表：制动、灯光、路试都过了才能交车，有一项不过就退回施工");
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

    /**
     * 保证一张工单有完整的三行项表：
     * 首次送检就建齐；返工后再次送检，把上一轮的过 / 不过清掉重新检，但行还在（留下不过的痕迹）。
     * 对 (order_id, item) 有唯一约束兜底，并发也不会建重。
     */
    private void provisionQcItems(Long orderId) {
        Map<String, QcItem> existing = new LinkedHashMap<>();
        for (QcItem qi : qcItems.findByOrderIdOrderByIdAsc(orderId)) {
            existing.put(qi.item, qi);
        }
        LocalDateTime now = LocalDateTime.now();
        for (String name : QC_ITEMS) {
            QcItem qi = existing.get(name);
            if (qi == null) {
                qi = new QcItem();
                qi.orderId = orderId;
                qi.item = name;
                qi.createdAt = now;
                qi.updatedAt = now;
                qcItems.save(qi);
            } else {
                qi.result = null;
                qi.remark = null;
                qi.updatedAt = now;
                qcItems.save(qi);
            }
        }
    }

    /** 质检员看这张单的项表。待质检的单如果项表缺行（比如旧数据），这里补齐。 */
    @Transactional
    public List<QcItem> qcList(Long id) {
        WorkOrder order = orders.findById(id).orElseThrow(() -> new BizException("工单不存在"));
        if ("待质检".equals(order.status)) {
            provisionQcItems(id);
        }
        return qcItems.findByOrderIdOrderByIdAsc(id);
    }

    /**
     * 按项表交车，整个判定和改状态在一个事务、一把行锁里做完：
     * 网断在半路，要么项表+状态一起成，要么一起不动，绝不会出现状态已交车、项表没齐。
     */
    @Transactional
    public WorkOrder submitQc(Long id, QcSubmitRequest request) {
        WorkOrder order = orders.findWithLockingById(id)
                .orElseThrow(() -> new BizException("工单不存在"));
        if (!"待质检".equals(order.status)) {
            throw new BizException("只有待质检的工单能质检，这张现在是 " + order.status
                    + "，不能重复交车");
        }
        if (request == null || request.items == null || request.items.isEmpty()) {
            throw new BizException("项表是空的：制动、灯光、路试每项都要记过或不过，不能空表交车");
        }

        Map<String, String> inputResults = new LinkedHashMap<>();
        Map<String, String> inputRemarks = new LinkedHashMap<>();
        for (QcSubmitRequest.ItemResult ir : request.items) {
            if (ir == null || ir.item == null || !QC_ITEMS.contains(ir.item)) {
                throw new BizException("项表里有不认识的项，只允许：制动、灯光、路试");
            }
            if (inputResults.containsKey(ir.item)) {
                throw new BizException("项「" + ir.item + "」填了不止一次，一项只能有一个结论");
            }
            if (!"过".equals(ir.result) && !"不过".equals(ir.result)) {
                throw new BizException("项「" + ir.item + "」还没记结论，每项都要选过或不过");
            }
            inputResults.put(ir.item, ir.result);
            inputRemarks.put(ir.item, ir.remark);
        }
        if (inputResults.size() != QC_ITEMS.size()) {
            throw new BizException("项表没填齐：制动、灯光、路试三项缺一不可");
        }

        // 以库里这张单的三行为准落结果，缺行就补（正常情况下完工送检时已经建好）
        provisionQcItems(id);
        List<String> failed = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (QcItem qi : qcItems.findByOrderIdOrderByIdAsc(id)) {
            qi.result = inputResults.get(qi.item);
            qi.remark = inputRemarks.get(qi.item);
            qi.updatedAt = now;
            qcItems.save(qi);
            if ("不过".equals(qi.result)) {
                failed.add(qi.item);
            }
        }

        order.updatedAt = now;
        if (failed.isEmpty()) {
            // 三项全过：待质检 → 已交车，和项表在同一个事务里提交
            order.qcResult = "合格";
            order.status = "已交车";
        } else {
            // 有一项不过：记下是哪几项，退回施工中
            order.qcResult = "返工：" + String.join("、", failed);
            order.status = "施工中";
        }
        return orders.save(order);
    }
}
