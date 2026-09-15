package com.repair.workshop.repository;

import com.repair.workshop.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    boolean existsByCode(String code);
}
