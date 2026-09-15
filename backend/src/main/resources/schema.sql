-- 汽车维修车间：建表 + 种子数据
-- 表结构由 Hibernate 兜底（ddl-auto=update），这里只保证首次启动就有数据

CREATE TABLE IF NOT EXISTS bay (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  kind VARCHAR(16) NOT NULL DEFAULT '举升',
  status VARCHAR(16) NOT NULL DEFAULT '可用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bay_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS equipment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  category VARCHAR(16) NOT NULL DEFAULT '拆装',
  bay_id BIGINT NULL,
  status VARCHAR(16) NOT NULL DEFAULT '可用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_equipment_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS technician (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(32) NOT NULL,
  level_name VARCHAR(16) NOT NULL DEFAULT '初级',
  status VARCHAR(16) NOT NULL DEFAULT '在岗',
  PRIMARY KEY (id),
  UNIQUE KEY uk_technician_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS work_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(32) NOT NULL,
  plate VARCHAR(16) NOT NULL,
  model VARCHAR(64) NULL,
  customer VARCHAR(32) NULL,
  phone VARCHAR(20) NULL,
  kind VARCHAR(16) NOT NULL,
  fault_desc VARCHAR(255) NULL,
  bay_id BIGINT NULL,
  technician_id BIGINT NULL,
  plan_date DATE NULL,
  start_min INT NULL,
  end_min INT NULL,
  status VARCHAR(16) NOT NULL DEFAULT '待派工',
  qc_result VARCHAR(16) NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_work_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS part (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  spec VARCHAR(64) NULL,
  stock INT NOT NULL DEFAULT 0,
  warn_stock INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT '在用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_part_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS part_issue (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  part_id BIGINT NOT NULL,
  qty INT NOT NULL,
  kind VARCHAR(16) NOT NULL,
  operator VARCHAR(32) NULL,
  created_at DATETIME NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO bay (id, code, name, kind, status) VALUES
  (1, 'B-01', '一号举升工位', '举升', '可用'),
  (2, 'B-02', '二号举升工位', '举升', '可用'),
  (3, 'B-03', '地沟工位',     '地沟', '可用'),
  (4, 'B-04', '钣金工位',     '钣金', '可用'),
  (5, 'B-05', '喷漆房',       '喷漆', '停用');

INSERT IGNORE INTO equipment (id, code, name, category, bay_id, status) VALUES
  (1, 'EQ-1001', '双柱举升机',   '举升', 1,    '可用'),
  (2, 'EQ-1002', '四轮定位仪',   '检测', 1,    '可用'),
  (3, 'EQ-1003', '双柱举升机',   '举升', 2,    '可用'),
  (4, 'EQ-1004', '地沟举升台',   '举升', 3,    '可用'),
  (5, 'EQ-1005', '钣金校正架',   '钣金', 4,    '可用'),
  (6, 'EQ-1006', '车身点焊机',   '钣金', 4,    '维修中'),
  (7, 'EQ-1007', '烤漆房风机组', '钣喷', 5,    '停用'),
  (8, 'EQ-1008', '轮胎拆装机',   '拆装', NULL, '可用');

INSERT IGNORE INTO technician (id, code, name, level_name, status) VALUES
  (1, 'T-01', '张建军', '高级', '在岗'),
  (2, 'T-02', '李国伟', '中级', '在岗'),
  (3, 'T-03', '王海涛', '中级', '在岗'),
  (4, 'T-04', '赵小龙', '初级', '在岗'),
  (5, 'T-05', '陈志刚', '高级', '休假'),
  (6, 'T-06', '刘明',   '初级', '离职');

INSERT IGNORE INTO work_order (id, order_no, plate, model, customer, phone, kind, fault_desc, bay_id, technician_id, plan_date, start_min, end_min, status, qc_result, created_at, updated_at) VALUES
  (1, 'WO-0001', '京A8D26', '大众速腾',   '刘女士', '13900000001', '保养', '常规保养，换机油机滤', 1, 1, CURDATE(), 540, 600, '施工中', NULL, NOW(), NOW()),
  (2, 'WO-0002', '京N3F71', '本田雅阁',   '周先生', '13900000002', '维修', '冷车启动抖动，查点火线圈', 2, 2, CURDATE(), 600, 720, '待质检', NULL, NOW(), NOW()),
  (3, 'WO-0003', '京Q5H92', '丰田凯美瑞', '吴女士', '13900000003', '钣金', '右后叶子板凹陷整形', 4, 3, CURDATE(), 540, 660, '施工中', NULL, NOW(), NOW()),
  (4, 'WO-0004', '京E1K48', '日产轩逸',   '郑先生', '13900000004', '喷漆', '前保险杠补漆', NULL, NULL, NULL, NULL, NULL, '待派工', NULL, NOW(), NOW()),
  (5, 'WO-0005', '京B6P29', '别克君威',   '冯先生', '13900000005', '维修', '更换前刹车片', 3, 4, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 600, 660, '已交车', '合格', NOW(), NOW()),
  (6, 'WO-0006', '京C9S35', '现代途胜',   '何先生', '13900000006', '保养', '客户临时有事，取消', NULL, NULL, NULL, NULL, NULL, '已取消', NULL, NOW(), NOW());

INSERT IGNORE INTO part (id, code, name, spec, stock, warn_stock, status) VALUES
  (1, 'PT-1001', '机油滤清器',     '适配大众 1.4T',  40, 10, '在用'),
  (2, 'PT-1002', '空气滤芯',       '通用 260×200',   25,  8, '在用'),
  (3, 'PT-1003', '前刹车片',       '适配别克君威',   32, 10, '在用'),
  (4, 'PT-1004', '火花塞',         '铱金 火花塞',     8, 12, '在用'),
  (5, 'PT-1005', '变速箱油',       '4L 装',           6,  5, '在用'),
  (6, 'PT-1006', '化油器垫片',     '老款车型专用',    2,  0, '停用'),
  (7, 'PT-1007', '蓄电池',         '60Ah',            5,  3, '在用');

INSERT IGNORE INTO part_issue (id, order_id, part_id, qty, kind, operator, created_at) VALUES
  (1, 1, 1, 1, '领用', '张建军', NOW()),
  (2, 1, 2, 1, '领用', '张建军', NOW()),
  (3, 1, 5, 2, '领用', '张建军', NOW()),
  (4, 1, 5, 1, '退料', '张建军', NOW()),
  (5, 5, 3, 2, '领用', '赵小龙', NOW());
