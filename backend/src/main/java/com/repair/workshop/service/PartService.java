package com.repair.workshop.service;

import com.repair.workshop.dto.BizException;
import com.repair.workshop.entity.Part;
import com.repair.workshop.entity.PartIssue;
import com.repair.workshop.entity.WorkOrder;
import com.repair.workshop.repository.PartIssueRepository;
import com.repair.workshop.repository.PartRepository;
import com.repair.workshop.repository.WorkOrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartService {

    private final PartRepository parts;
    private final PartIssueRepository issues;
    private final WorkOrderRepository orders;

    public PartService(PartRepository parts, PartIssueRepository issues, WorkOrderRepository orders) {
        this.parts = parts;
        this.issues = issues;
        this.orders = orders;
    }

    public List<Part> list(String keyword, String status) {
        return parts.findAll().stream()
                .filter(p -> status == null || status.isEmpty() || status.equals(p.status))
                .filter(p -> keyword == null || keyword.isEmpty()
                        || p.name.contains(keyword) || p.code.contains(keyword))
                .toList();
    }

    public List<PartIssue> issueList(Long orderId) {
        if (orderId == null) {
            return issues.findAllByOrderByCreatedAtDesc();
        }
        return issues.findByOrderIdOrderByCreatedAtAsc(orderId);
    }

    @Transactional
    public Part create(Part input) {
        if (input.code == null || input.code.isBlank()) {
            throw new BizException("配件编号不能为空");
        }
        if (parts.existsByCode(input.code)) {
            throw new BizException("编号 " + input.code + " 已经被别的配件用掉了");
        }
        Part saved = new Part();
        saved.code = input.code.trim();
        saved.name = input.name;
        saved.spec = input.spec;
        saved.stock = input.stock == null ? 0 : input.stock;
        saved.warnStock = input.warnStock == null ? 0 : input.warnStock;
        saved.status = (input.status == null || input.status.isBlank()) ? "在用" : input.status;
        return parts.save(saved);
    }

    @Transactional
    public Part update(Long id, Part input) {
        Part p = parts.findById(id).orElseThrow(() -> new BizException("配件不存在"));
        if (input.name != null) {
            p.name = input.name;
        }
        if (input.spec != null) {
            p.spec = input.spec;
        }
        if (input.warnStock != null) {
            p.warnStock = input.warnStock;
        }
        if (input.stock != null && input.stock < 0) {
            throw new BizException("库存不能是负数");
        }
        if (input.stock != null) {
            p.stock = input.stock;
        }
        if (input.status != null && !input.status.isBlank()) {
            p.status = input.status;
        }
        return parts.save(p);
    }

    /** 领料 / 退料：库存与已领数量都在这条链上卡。 */
    @Transactional
    public PartIssue issue(PartIssue input) {
        if (input.orderId == null) {
            throw new BizException("请选一张工单");
        }
        if (input.partId == null) {
            throw new BizException("请选一个配件");
        }
        if (input.qty == null || input.qty <= 0) {
            throw new BizException("数量要大于 0");
        }
        WorkOrder order = orders.findById(input.orderId)
                .orElseThrow(() -> new BizException("工单不存在"));
        Part part = parts.findById(input.partId)
                .orElseThrow(() -> new BizException("配件不存在"));

        if (!"在用".equals(part.status)) {
            throw new BizException("配件 " + part.name + " 已经停用，不能再领");
        }
        String kind = "退料".equals(input.kind) ? "退料" : "领用";
        if ("领用".equals(kind)) {
            if (!"施工中".equals(order.status)) {
                throw new BizException("只有施工中的工单能领料，这张现在是 " + order.status);
            }
            if (part.stock < input.qty) {
                throw new BizException("配件 " + part.name + " 库存只剩 "
                        + part.stock + " 个，不够领 " + input.qty + " 个");
            }
            part.stock = part.stock - input.qty;
        } else {
            if (!"施工中".equals(order.status) && !"待质检".equals(order.status)) {
                throw new BizException("这张工单现在是 " + order.status + "，不能退料");
            }
            int taken = takenQty(order.id, part.id);
            if (taken < input.qty) {
                throw new BizException("这张工单在 " + part.name + " 上只领了 "
                        + taken + " 个，退不了 " + input.qty + " 个");
            }
            part.stock = part.stock + input.qty;
        }
        parts.save(part);

        PartIssue saved = new PartIssue();
        saved.orderId = order.id;
        saved.partId = part.id;
        saved.qty = input.qty;
        saved.kind = kind;
        saved.operator = input.operator;
        saved.createdAt = LocalDateTime.now();
        return issues.save(saved);
    }

    private int takenQty(Long orderId, Long partId) {
        int net = 0;
        for (PartIssue i : issues.findByOrderIdAndPartId(orderId, partId)) {
            net += "领用".equals(i.kind) ? i.qty : -i.qty;
        }
        return net;
    }
}
