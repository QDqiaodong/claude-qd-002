package com.repair.workshop.repository;

import com.repair.workshop.entity.QcItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QcItemRepository extends JpaRepository<QcItem, Long> {

    /** 按插入顺序取一张单的项表（制动 → 灯光 → 路试）；并发交车的串行化靠工单行锁 */
    List<QcItem> findByOrderIdOrderByIdAsc(Long orderId);
}
