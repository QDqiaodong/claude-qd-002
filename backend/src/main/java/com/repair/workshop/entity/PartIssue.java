package com.repair.workshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 配件领用流水：工单施工时领料，多领了退料，两条都记在这里。 */
@Entity
@Table(name = "part_issue")
public class PartIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    @Column(name = "part_id", nullable = false)
    public Long partId;

    @Column(nullable = false)
    public Integer qty;

    /** 领用 / 退料 */
    @Column(nullable = false, length = 16)
    public String kind;

    @Column(length = 32)
    public String operator;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
