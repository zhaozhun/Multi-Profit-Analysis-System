-- 维度主数据：机构
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order) VALUES
('ORG001', '总行', 'ORG', 0, 1, 1),
('ORG00101', '北京分行', 'ORG', 1, 2, 1),
('ORG00102', '上海分行', 'ORG', 1, 2, 2),
('ORG00103', '深圳分行', 'ORG', 1, 2, 3),
('ORG00104', '广州分行', 'ORG', 1, 2, 4),
('ORG00105', '杭州分行', 'ORG', 1, 2, 5),
('ORG0010101', '朝阳支行', 'ORG', 2, 3, 1),
('ORG0010102', '海淀支行', 'ORG', 2, 3, 2),
('ORG0010103', '西城支行', 'ORG', 2, 3, 3),
('ORG0010201', '浦东支行', 'ORG', 3, 3, 1),
('ORG0010202', '浦西支行', 'ORG', 3, 3, 2),
('ORG0010301', '南山支行', 'ORG', 4, 3, 1),
('ORG0010302', '福田支行', 'ORG', 4, 3, 2),
('ORG0010401', '天河支行', 'ORG', 5, 3, 1),
('ORG0010501', '西湖支行', 'ORG', 6, 3, 1);

-- 条线
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order) VALUES
('BL01', '对公条线', 'BIZ_LINE', 0, 1, 1),
('BL02', '零售条线', 'BIZ_LINE', 0, 1, 2),
('BL03', '金融市场条线', 'BIZ_LINE', 0, 1, 3),
('BL04', '小微条线', 'BIZ_LINE', 0, 1, 4);

-- 部门
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order) VALUES
('DEPT01', '公司金融部', 'DEPT', 0, 1, 1),
('DEPT02', '零售金融部', 'DEPT', 0, 1, 2),
('DEPT03', '金融市场部', 'DEPT', 0, 1, 3),
('DEPT04', '风险管理部', 'DEPT', 0, 1, 4),
('DEPT05', '运营管理部', 'DEPT', 0, 1, 5),
('DEPT06', '财务管理部', 'DEPT', 0, 1, 6);

-- 产品
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order) VALUES
('PROD01', '公司贷款', 'PRODUCT', 0, 1, 1),
('PROD02', '个人贷款', 'PRODUCT', 0, 1, 2),
('PROD03', '公司存款', 'PRODUCT', 0, 1, 3),
('PROD04', '个人存款', 'PRODUCT', 0, 1, 4),
('PROD05', '理财产品', 'PRODUCT', 0, 1, 5),
('PROD06', '信用卡', 'PRODUCT', 0, 1, 6),
('PROD07', '国际业务', 'PRODUCT', 0, 1, 7),
('PROD0101', '流动资金贷款', 'PRODUCT', 16, 2, 1),
('PROD0102', '固定资产贷款', 'PRODUCT', 16, 2, 2),
('PROD0103', '银承汇票', 'PRODUCT', 16, 2, 3),
('PROD0201', '个人住房贷款', 'PRODUCT', 17, 2, 1),
('PROD0202', '个人消费贷款', 'PRODUCT', 17, 2, 2),
('PROD0203', '个人经营贷款', 'PRODUCT', 17, 2, 3);

-- 渠道
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order) VALUES
('CH01', '柜面渠道', 'CHANNEL', 0, 1, 1),
('CH02', '网上银行', 'CHANNEL', 0, 1, 2),
('CH03', '手机银行', 'CHANNEL', 0, 1, 3),
('CH04', '自助设备', 'CHANNEL', 0, 1, 4),
('CH05', '第三方代理', 'CHANNEL', 0, 1, 5);

-- 客户经理
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, ext_attrs) VALUES
('MGR001', '张明', 'MANAGER', 0, 1, 1, '{"org":"北京朝阳支行","line":"对公"}'),
('MGR002', '李华', 'MANAGER', 0, 1, 2, '{"org":"北京海淀支行","line":"零售"}'),
('MGR003', '王芳', 'MANAGER', 0, 1, 3, '{"org":"上海浦东支行","line":"对公"}'),
('MGR004', '赵强', 'MANAGER', 0, 1, 4, '{"org":"深圳南山支行","line":"零售"}'),
('MGR005', '刘洋', 'MANAGER', 0, 1, 5, '{"org":"广州天河支行","line":"金融市场"}'),
('MGR006', '陈静', 'MANAGER', 0, 1, 6, '{"org":"杭州西湖支行","line":"对公"}');

-- 利润公式
INSERT INTO profit_formula (name, code, expression, caliber_type, version) VALUES
('账面利润公式', 'BOOK_FORMULA', 'REVENUE - FTP_COST - RISK_COST - OP_COST = NET_PROFIT', 'BOOK', '1.0'),
('考核利润公式', 'ASSESS_FORMULA', 'REVENUE - FTP_COST*0.9 - RISK_COST*0.85 - OP_COST = NET_PROFIT', 'ASSESS', '1.0');

-- 预警规则
INSERT INTO alert_rule (name, alert_type, metric_code, threshold, threshold_type, level) VALUES
('净利润环比大幅下降', 'PROFIT', 'NET_PROFIT', 20.00, 'PERCENT', 'CRITICAL'),
('成本月度涨幅超限', 'COST', 'TOTAL_COST', 15.00, 'PERCENT', 'WARNING'),
('新增亏损主体', 'SUBJECT', 'NET_PROFIT', 0, 'ABSOLUTE', 'CRITICAL');
