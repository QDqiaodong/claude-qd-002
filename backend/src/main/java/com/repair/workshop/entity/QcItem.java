package com.repair.workshop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 质检项表：每张待质检工单固定三行（制动 / 灯光 / 路试），逐项记过或不过。 */
@Entity
@Table(name = "qc_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_qc_order_item",
                columnNames = {"order_id", "item"}))
public class QcItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "order_id", nullable = false)
    public Long orderId;

    /** 制动 / 灯光 / 路试 */
    @Column(nullable = false, length = 16)
    public String item;

    /** 过 / 不过；没检之前是 null，空项表不许交车 */
    @Column(length = 8)
    public String result;

    /** 不过时质检员留的说明，方便返工的人看 */
    @Column(length = 255)
    public String remark;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();
}
