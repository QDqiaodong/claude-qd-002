# 汽车维修车间 · 工单与工位占用管理系统

综合修理厂的车间作业系统：**工位与设备台账**、**维修工单闭环**、**派工与工位占用**、
**配件领用与库存**。

## 技术栈

Spring Boot 3.3（Java 17）+ MySQL 8.0 + Redis 7 + Vue 3 + Element Plus + Vite + nginx，全栈 `docker compose` 一键启动。

## 启动

```bash
./start.sh              # 等价于 docker compose up -d --build
```

| 入口 | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8202/ |
| 后端接口 | http://127.0.0.1:8302/api/ |
| MySQL | 127.0.0.1:3502（库 `repair_workshop`） |
| Redis | 127.0.0.1:6502 |

先起 MySQL / Redis，等 healthcheck 通过再起前后端（compose 里已经串好依赖）。

## 停止

```bash
docker compose down       # 保留数据卷
docker compose down -v    # 连数据卷一起删，下次启动重新灌种子数据
```

## 端口与库名

都在 `.env` 里改，`.env.example` 是同一份模板。容器名统一是
`claude-qd-002-{mysql,redis,backend,frontend}`。

## 业务模块

### 1. 工位与设备台账（`bay` / `equipment`）

工位编号 `B-xx` 全库唯一，类型分举升 / 地沟 / 钣金 / 喷漆，状态 `可用 / 停用`。
设备编号 `EQ-xxxx` 全库唯一，归属到一个工位（也可以暂不归），状态 `可用 / 停用 / 维修中`。
设备要改成停用或维修中时，如果它所在工位还有没了结的工单（已交车 / 已取消 之外的），会被拦住。

- 页面：工位与设备（`/bays`）
- 接口：`GET/POST /api/bays`、`PUT /api/bays/{id}/status`、`GET/POST /api/equipments`、`PUT /api/equipments/{id}`

### 2. 维修工单闭环（`work_order`）

一台车一次进厂开一张单，单号 `WO-xxxx` 自动生成。状态机：
`待派工 → 施工中 → 待质检 → 已交车`；质检结论 `返工` 时退回 `施工中`；`待派工 / 施工中` 可以取消。

- 页面：维修工单（`/orders`）
- 接口：`GET/POST /api/orders`、`PUT /api/orders/{id}/assign`、`POST /api/orders/{id}/advance?action=&qcResult=`

### 3. 派工与工位占用（`work_order.plan_date / start_min / end_min`）

派工时同时定工位、技师和计划时段（当天，按「从 0:00 起算的分钟数」存）。校验：
工位必须可用、技师必须在岗、结束时间晚于开始时间、工位下不能有非可用设备；
同一工位同一时段只能有一张未交车的工单，同一位技师同一时段也只能有一张。

- 页面：派工与占用（`/dispatch`）
- 接口：同上（`assign`）

### 4. 配件领用与库存（`part` / `part_issue`）

配件编号 `PT-xxxx` 唯一，带库存与预警线。领料只能对 `施工中` 的工单做，库存不够要拦住；
退料只能对 `施工中 / 待质检` 的工单做，且退的数量不能超过这张单在这件配件上净领的数量。
停用的配件不能再领。

- 页面：配件领用（`/parts`）
- 接口：`GET/POST /api/parts`、`PUT /api/parts/{id}`、`GET/POST /api/issues`

## 目录

```
backend/src/main/java/com/repair/workshop/
├── config/       CORS 配置
├── controller/   REST 入口
├── dto/          BizException + 统一错误响应
├── entity/       6 张业务表
├── repository/   Spring Data JPA
└── service/      业务规则（编号唯一、时段占用、状态机、库存与退料）
backend/src/main/resources/schema.sql   建表 + 种子数据（挂进 MySQL initdb）
frontend/src/views/                     4 个业务页面
```
