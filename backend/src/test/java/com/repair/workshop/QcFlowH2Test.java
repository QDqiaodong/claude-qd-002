package com.repair.workshop;

import com.repair.workshop.dto.QcSubmitRequest;
import com.repair.workshop.entity.QcItem;
import com.repair.workshop.entity.WorkOrder;
import com.repair.workshop.repository.QcItemRepository;
import com.repair.workshop.repository.WorkOrderRepository;
import com.repair.workshop.service.WorkOrderService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** H2 内存库里把质检交车的规则真跑一遍，不依赖 MySQL。 */
@SpringBootApplication
public class QcFlowH2Test {

    static int failures = 0;

    static void check(boolean cond, String name) {
        System.out.println((cond ? "PASS  " : "FAIL  ") + name);
        if (!cond) {
            failures++;
        }
    }

    static QcSubmitRequest.ItemResult ir(String item, String result) {
        QcSubmitRequest.ItemResult r = new QcSubmitRequest.ItemResult();
        r.item = item;
        r.result = result;
        return r;
    }

    static List<QcSubmitRequest.ItemResult> all(String result) {
        return new ArrayList<>(List.of(ir("制动", result), ir("灯光", result), ir("路试", result)));
    }

    @Bean
    CommandLineRunner run(WorkOrderService service, WorkOrderRepository orders,
                          QcItemRepository qcItems) {
        return args -> {
            // —— 1. 施工中 → 完工送检：自动建三行空项表 ——
            WorkOrder w1 = orders.save(newOrder("施工中"));
            service.advance(w1.id, "finish", null);
            w1 = orders.findById(w1.id).orElseThrow();
            check("待质检".equals(w1.status), "完工送检后状态=待质检");
            List<QcItem> rows = qcItems.findByOrderIdOrderByIdAsc(w1.id);
            check(rows.size() == 3, "送检即生成制动/灯光/路试三行");
            check(rows.stream().allMatch(r -> r.result == null), "新建项表结论全为空（没检）");

            // —— 2. 空项表不能交车 ——
            WorkOrder w2 = orders.save(newOrder("施工中"));
            service.advance(w2.id, "finish", null);
            boolean blockedEmpty = false;
            try {
                service.submitQc(w2.id, new QcSubmitRequest());
            } catch (Exception e) {
                blockedEmpty = e.getMessage().contains("项表是空的");
            }
            check(blockedEmpty, "空项表交车被拦");

            // —— 3. 缺一项不能交车 ——
            boolean blockedMissing = false;
            try {
                QcSubmitRequest req = new QcSubmitRequest();
                req.items = List.of(ir("制动", "过"), ir("灯光", "过"));
                service.submitQc(w2.id, req);
            } catch (Exception e) {
                blockedMissing = e.getMessage().contains("没填齐");
            }
            check(blockedMissing, "缺路试一项被拦（不能空着项表交车）");

            // —— 4. 有一项没选结论不能交车 ——
            boolean blockedNullResult = false;
            try {
                QcSubmitRequest req = new QcSubmitRequest();
                req.items = List.of(ir("制动", "过"), ir("灯光", null), ir("路试", "过"));
                service.submitQc(w2.id, req);
            } catch (Exception e) {
                blockedNullResult = e.getMessage().contains("还没记结论");
            }
            check(blockedNullResult, "某项结论为空被拦");

            // —— 5. 一项不过 → 退回施工中，记下哪项不过，其余项的结论也留着 ——
            WorkOrder w3 = orders.save(newOrder("施工中"));
            service.advance(w3.id, "finish", null);
            QcSubmitRequest failReq = new QcSubmitRequest();
            failReq.items = all("过");
            failReq.items.get(1).result = "不过"; // 灯光不过
            QcSubmitRequest.ItemResult light = failReq.items.get(1);
            light.remark = "右前大灯不亮";
            WorkOrder back = service.submitQc(w3.id, failReq);
            check("施工中".equals(back.status), "灯光不过 → 退回施工中");
            check(back.qcResult != null && back.qcResult.contains("灯光"), "工单留下不过项=灯光");
            List<QcItem> w3rows = qcItems.findByOrderIdOrderByIdAsc(w3.id);
            check(w3rows.size() == 3 && "不过".equals(w3rows.get(1).result)
                    && "右前大灯不亮".equals(w3rows.get(1).remark), "项表落库：灯光=不过+备注，其余也有记录");

            // —— 6. 列表上带得出「不过项」 ——
            List<WorkOrder> list = service.list(null, null, null);
            WorkOrder listed = list.stream().filter(o -> o.id.equals(w3.id)).findFirst().orElseThrow();
            check("灯光".equals(listed.failedItems), "工单列表 failedItems=灯光");

            // —— 7. 返工后再次送检：项表还在三行、结论清空重来 ——
            service.advance(w3.id, "finish", null);
            List<QcItem> reopened = qcItems.findByOrderIdOrderByIdAsc(w3.id);
            check(reopened.size() == 3 && reopened.stream().allMatch(r -> r.result == null),
                    "二次送检仍是三行且旧结论清空（不留陈旧结果）");

            // —— 8. 三项全过 → 已交车 ——
            QcSubmitRequest passReq = new QcSubmitRequest();
            passReq.items = all("过");
            WorkOrder done = service.submitQc(w3.id, passReq);
            check("已交车".equals(done.status) && "合格".equals(done.qcResult), "三项全过 → 已交车/合格");
            check(qcItems.findByOrderIdOrderByIdAsc(w3.id).stream().allMatch(r -> "过".equals(r.result)),
                    "已交车工单的项表三项都是过");

            // —— 9. 已交车再提交一次 → 被拦（点两次交车只能成一次） ——
            boolean blockedTwice = false;
            try {
                service.submitQc(w3.id, passReq);
            } catch (Exception e) {
                blockedTwice = e.getMessage().contains("不能重复交车");
            }
            check(blockedTwice, "已交车再点交车被拦");

            // —— 10. 旧的不带项表的 advance?action=qc 通道必须失效 ——
            WorkOrder w4 = orders.save(newOrder("施工中"));
            service.advance(w4.id, "finish", null);
            boolean blockedLegacy = false;
            try {
                service.advance(w4.id, "qc", "合格");
            } catch (Exception e) {
                blockedLegacy = e.getMessage().contains("逐项填项表");
            }
            WorkOrder w4after = orders.findById(w4.id).orElseThrow();
            check(blockedLegacy && "待质检".equals(w4after.status),
                    "旧的 advance?qc=合格 通道被堵死，车没有被空表交出去");

            // —— 11. 并发两笔全过提交同一单：只有一笔成功，状态最终已交车 ——
            WorkOrder w5 = orders.save(newOrder("施工中"));
            service.advance(w5.id, "finish", null);
            int n = 8;
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(n);
            ExecutorService pool = Executors.newFixedThreadPool(n);
            AtomicInteger ok = new AtomicInteger();
            AtomicInteger rejected = new AtomicInteger();
            for (int i = 0; i < n; i++) {
                pool.submit(() -> {
                    try {
                        start.await();
                        QcSubmitRequest req = new QcSubmitRequest();
                        req.items = all("过");
                        service.submitQc(w5.id, req);
                        ok.incrementAndGet();
                    } catch (Exception e) {
                        rejected.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            start.countDown();
            doneLatch.await();
            pool.shutdown();
            WorkOrder w5after = orders.findById(w5.id).orElseThrow();
            check(ok.get() == 1 && rejected.get() == n - 1,
                    "8 个并发交车仅 1 笔成功（实际成功 " + ok.get() + "，拒绝 " + rejected.get() + "）");
            check("已交车".equals(w5after.status), "并发后状态确定为已交车");
            check(qcItems.findByOrderIdOrderByIdAsc(w5.id).size() == 3, "并发没有把项表建重（仍是 3 行）");

            // —— 12. 非待质检状态（施工中）直接调提交 → 被拦 ——
            WorkOrder w6 = orders.save(newOrder("施工中"));
            boolean blockedBuilding = false;
            try {
                QcSubmitRequest req = new QcSubmitRequest();
                req.items = all("过");
                service.submitQc(w6.id, req);
            } catch (Exception e) {
                blockedBuilding = e.getMessage().contains("只有待质检");
            }
            check(blockedBuilding, "施工中没送检不能直接交车");

            System.out.println(failures == 0 ? "\nALL TESTS PASSED" : "\n" + failures + " TESTS FAILED");
            System.exit(failures == 0 ? 0 : 1);
        };
    }

    private static WorkOrder newOrder(String status) {
        WorkOrder o = new WorkOrder();
        o.orderNo = "WO-T" + System.nanoTime();
        o.plate = "京T0000";
        o.kind = "保养";
        o.status = status;
        return o;
    }

    public static void main(String[] args) {
        SpringApplication.run(QcFlowH2Test.class, args);
    }
}
