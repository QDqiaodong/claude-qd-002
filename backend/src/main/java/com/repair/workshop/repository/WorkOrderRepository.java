package com.repair.workshop.repository;

import com.repair.workshop.entity.WorkOrder;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, Long> {

    boolean existsByOrderNo(String orderNo);

    /** 状态流转时给工单行上写锁，保证点两次「交车」只能成一次、质检和完工不会并发穿插 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WorkOrder> findWithLockingById(Long id);

    List<WorkOrder> findAllByOrderByUpdatedAtDesc();

    List<WorkOrder> findByPlanDateOrderByStartMinAsc(LocalDate planDate);

    List<WorkOrder> findByBayIdAndPlanDateAndStatusNot(Long bayId, LocalDate planDate, String status);

    List<WorkOrder> findByTechnicianIdAndPlanDateAndStatusNot(Long technicianId, LocalDate planDate, String status);

    /** 还没了结的工单（已交车 / 已取消 之外的） */
    long countByBayIdAndStatusNotIn(Long bayId, Collection<String> statuses);

    long countByTechnicianIdAndStatusNotIn(Long technicianId, Collection<String> statuses);
}
