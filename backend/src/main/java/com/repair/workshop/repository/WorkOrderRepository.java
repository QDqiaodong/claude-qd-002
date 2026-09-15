package com.repair.workshop.repository;

import com.repair.workshop.entity.WorkOrder;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByOrderNo(String orderNo);

    List<WorkOrder> findAllByOrderByUpdatedAtDesc();

    List<WorkOrder> findByPlanDateOrderByStartMinAsc(LocalDate planDate);

    List<WorkOrder> findByBayIdAndPlanDateAndStatusNot(Long bayId, LocalDate planDate, String status);

    List<WorkOrder> findByTechnicianIdAndPlanDateAndStatusNot(Long technicianId, LocalDate planDate, String status);

    /** 还没了结的工单（已交车 / 已取消 之外的） */
    long countByBayIdAndStatusNotIn(Long bayId, Collection<String> statuses);

    long countByTechnicianIdAndStatusNotIn(Long technicianId, Collection<String> statuses);
}
