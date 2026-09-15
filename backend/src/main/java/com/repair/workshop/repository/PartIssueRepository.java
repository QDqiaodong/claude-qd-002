package com.repair.workshop.repository;

import com.repair.workshop.entity.PartIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartIssueRepository extends JpaRepository<PartIssue, Long> {

    List<PartIssue> findAllByOrderByCreatedAtDesc();

    List<PartIssue> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    List<PartIssue> findByOrderIdAndPartId(Long orderId, Long partId);
}
