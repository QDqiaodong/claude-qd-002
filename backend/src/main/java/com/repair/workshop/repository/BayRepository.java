package com.repair.workshop.repository;

import com.repair.workshop.entity.Bay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BayRepository extends JpaRepository<Bay, Long> {
    boolean existsByCode(String code);
}
