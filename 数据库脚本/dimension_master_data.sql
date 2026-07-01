-- 数据库脚本/dimension_master_data.sql
-- 维度主数据初始数据
-- 创建时间：2026-06-30

-- 1. 插入机构初始数据
INSERT INTO dw_dim_organization (org_code, org_name, parent_id, level) VALUES
('00100001', '总行', 0, 1),
('01200001', '北京分行', 1, 2),
('02200001', '广州分行', 1, 2),
('03200001', '上海分行', 1, 2),
('04200001', '深圳分行', 1, 2),
('05200001', '杭州分行', 1, 2);

-- 2. 插入条线初始数据
INSERT INTO dw_dim_biz_line (line_code, line_name, status) VALUES
('01001', '金融市场条线', 1),
('02001', '对公条线', 1),
('03001', '零售条线', 1);

-- 3. 插入产品初始数据
INSERT INTO dw_dim_product (product_code, product_name, product_type, status) VALUES
('01001', '住房贷款', 'LOAN', 1),
('01002', '个人贷款', 'LOAN', 1),
('01003', '公司贷款', 'LOAN', 1),
('01004', '短期贷款', 'LOAN', 1),
('01005', '中长期贷款', 'LOAN', 1);

-- 4. 插入渠道初始数据
INSERT INTO dw_dim_channel (channel_code, channel_name, status) VALUES
('01001', '线下渠道', 1),
('02001', '线上渠道', 1),
('03001', '网点渠道', 1);

-- 5. 插入客户经理初始数据
INSERT INTO dw_dim_manager (manager_code, manager_name, org_id, status) VALUES
('01001', '北京分行客户经理', 2, 1),
('02001', '广州分行客户经理', 3, 1),
('03001', '上海分行客户经理', 4, 1),
('04001', '深圳分行客户经理', 5, 1),
('05001', '杭州分行客户经理', 6, 1);
