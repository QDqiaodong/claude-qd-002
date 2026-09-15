package com.repair.workshop.entity;

import jakarta.persistence.*;

/** 维修工位：编号唯一，设备挂在工位上，工单按工位排时段。 */
@Entity
@Table(name = "bay")
public class Bay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 举升 / 地沟 / 钣金 / 喷漆 */
    @Column(nullable = false, length = 16)
    public String kind;

    /** 可用 / 停用 */
    @Column(nullable = false, length = 16)
    public String status;
}
