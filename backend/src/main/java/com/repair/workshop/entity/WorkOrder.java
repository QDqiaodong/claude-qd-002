package com.repair.workshop.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 维修工单：一台车一次进厂，走 待派工 -> 施工中 -> 待质检 -> 已交车。 */
@Entity
@Table(name = "work_order")
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    public String orderNo;

    @Column(nullable = false, length = 16)
    public String plate;

    @Column(length = 64)
    public String model;

    @Column(length = 32)
    public String customer;

    @Column(length = 20)
    public String phone;

    /** 保养 / 维修 / 钣金 / 喷漆 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(name = "fault_desc", length = 255)
    public String faultDesc;

    @Column(name = "bay_id")
    public Long bayId;

    @Column(name = "technician_id")
    public Long technicianId;

    @Column(name = "plan_date")
    public LocalDate planDate;

    /** 计划开始分钟（从 0:00 起算），用于判断同时段占用 */
    @Column(name = "start_min")
    public Integer startMin;

    @Column(name = "end_min")
    public Integer endMin;

    /** 待派工 / 施工中 / 待质检 / 已交车 / 已取消 */
    @Column(nullable = false, length = 16)
    public String status;

    /** 质检结论：合格 / 返工 */
    @Column(name = "qc_result", length = 16)
    public String qcResult;

    /** 不是表字段：最近一次质检不过的项（如「灯光、路试」），退回施工时工单列表上要看得出来 */
    @Transient
    public String failedItems;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
