package com.repair.workshop.repository;

import com.repair.workshop.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {
    boolean existsByCode(String code);
}
