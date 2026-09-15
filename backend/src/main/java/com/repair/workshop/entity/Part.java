package com.repair.workshop.entity;

import jakarta.persistence.*;

/** 配件：编号唯一，带库存下限。 */
@Entity
@Table(name = "part")
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(length = 64)
    public String spec;

    @Column(nullable = false)
    public Integer stock;

    /** 低于这个数就该补货 */
    @Column(name = "warn_stock", nullable = false)
    public Integer warnStock;

    /** 在用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
