package com.repair.workshop.entity;

import jakarta.persistence.*;

/** 车间技师：工号唯一，等级决定能接哪类活。 */
@Entity
@Table(name = "technician")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 32)
    public String code;

    @Column(nullable = false, length = 32)
    public String name;

    /** 初级 / 中级 / 高级 */
    @Column(name = "level_name", nullable = false, length = 16)
    public String level;

    /** 在岗 / 休假 / 离职 */
    @Column(nullable = false, length = 16)
    public String status;
}
