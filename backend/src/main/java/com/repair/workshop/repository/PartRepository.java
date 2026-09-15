package com.repair.workshop.repository;

import com.repair.workshop.entity.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long> {
    boolean existsByCode(String code);
}
