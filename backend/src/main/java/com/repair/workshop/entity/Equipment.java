package com.repair.workshop.entity;

import jakarta.persistence.*;

/** 车间设备：双柱举升机、四轮定位仪这类，编号唯一，归属某个工位。 */
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 64)
    public String name;

    /** 举升 / 检测 / 拆装 / 钣喷 */
    @Column(nullable = false, length = 16)
    public String category;

    @Column(name = "bay_id")
    public Long bayId;

    /** 可用 / 停用 / 维修中 */
    @Column(nullable = false, length = 16)
    public String status;
}
