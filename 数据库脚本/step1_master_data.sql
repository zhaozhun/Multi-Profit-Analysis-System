-- ============================================
-- Step 1: 主数据插入
-- 内容：维度主数据（6类） + 指标配置数据
-- 说明：INSERT IGNORE 保证幂等，重复执行不报错
-- ============================================

USE multi_profit;

-- ============================================
-- 一、维度主数据
-- ============================================

-- 1.1 部门主数据（7个部门 × 3级 = 40条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('DEPT01', '科创金融部', 'DEPT', 0, 1, 1, NULL),
('DEPT02', '产业链金融部', 'DEPT', 0, 1, 2, NULL),
('DEPT03', '消费金融部', 'DEPT', 0, 1, 3, NULL),
('DEPT04', '普惠金融部', 'DEPT', 0, 1, 4, NULL),
('DEPT05', '财富金融部', 'DEPT', 0, 1, 5, NULL),
('DEPT06', '支付金融部', 'DEPT', 0, 1, 6, NULL),
('DEPT07', '金融市场部', 'DEPT', 0, 1, 7, NULL),
('DEPT0101', '科技创新处', 'DEPT', 1, 2, 1, NULL),
('DEPT0102', '投贷联动处', 'DEPT', 1, 2, 2, NULL),
('DEPT0201', '供应链金融处', 'DEPT', 2, 2, 1, NULL),
('DEPT0202', '产业链平台处', 'DEPT', 2, 2, 2, NULL),
('DEPT0301', '信用卡中心', 'DEPT', 3, 2, 1, NULL),
('DEPT0302', '消费信贷处', 'DEPT', 3, 2, 2, NULL),
('DEPT0401', '小微企业处', 'DEPT', 4, 2, 1, NULL),
('DEPT0402', '三农金融处', 'DEPT', 4, 2, 2, NULL),
('DEPT0501', '私人银行处', 'DEPT', 5, 2, 1, NULL),
('DEPT0502', '理财业务处', 'DEPT', 5, 2, 2, NULL),
('DEPT0601', '线上支付处', 'DEPT', 6, 2, 1, NULL),
('DEPT0602', '线下支付处', 'DEPT', 6, 2, 2, NULL),
('DEPT0701', '同业业务处', 'DEPT', 7, 2, 1, NULL),
('DEPT0702', '投资交易处', 'DEPT', 7, 2, 2, NULL),
('DEPT010101', '高新企业组', 'DEPT', 8, 3, 1, NULL),
('DEPT010102', '专精特新组', 'DEPT', 8, 3, 2, NULL),
('DEPT010201', '投贷联动组', 'DEPT', 9, 3, 1, NULL),
('DEPT020101', '核心企业组', 'DEPT', 10, 3, 1, NULL),
('DEPT020102', '上下游企业组', 'DEPT', 10, 3, 2, NULL),
('DEPT020201', '平台运营组', 'DEPT', 11, 3, 1, NULL),
('DEPT030101', '发卡组', 'DEPT', 12, 3, 1, NULL),
('DEPT030102', '收单组', 'DEPT', 12, 3, 2, NULL),
('DEPT030201', '消费贷组', 'DEPT', 13, 3, 1, NULL),
('DEPT040101', '首贷组', 'DEPT', 14, 3, 1, NULL),
('DEPT040102', '续贷组', 'DEPT', 14, 3, 2, NULL),
('DEPT040201', '三农组', 'DEPT', 15, 3, 1, NULL),
('DEPT050101', '高净值组', 'DEPT', 16, 3, 1, NULL),
('DEPT050102', '家族信托组', 'DEPT', 16, 3, 2, NULL),
('DEPT050201', '理财销售组', 'DEPT', 17, 3, 1, NULL),
('DEPT060101', '移动支付组', 'DEPT', 18, 3, 1, NULL),
('DEPT060102', '网银支付组', 'DEPT', 18, 3, 2, NULL),
('DEPT060201', 'POS业务组', 'DEPT', 19, 3, 1, NULL),
('DEPT070101', '同业拆借组', 'DEPT', 20, 3, 1, NULL),
('DEPT070102', '同业存单组', 'DEPT', 20, 3, 2, NULL),
('DEPT070201', '债券投资组', 'DEPT', 21, 3, 1, NULL),
('DEPT070202', '基金投资组', 'DEPT', 21, 3, 2, NULL);

-- 1.2 机构主数据（总行 → 5分行 → 11支行 = 17条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('ORG001', '总行', 'ORG', 0, 1, 1, NULL),
('ORG00101', '北京分行', 'ORG', 22, 2, 1, NULL),
('ORG00102', '上海分行', 'ORG', 22, 2, 2, NULL),
('ORG00103', '深圳分行', 'ORG', 22, 2, 3, NULL),
('ORG00104', '广州分行', 'ORG', 22, 2, 4, NULL),
('ORG00105', '杭州分行', 'ORG', 22, 2, 5, NULL),
('ORG0010101', '朝阳支行', 'ORG', 23, 3, 1, NULL),
('ORG0010102', '海淀支行', 'ORG', 23, 3, 2, NULL),
('ORG0010103', '西城支行', 'ORG', 23, 3, 3, NULL),
('ORG0010201', '浦东支行', 'ORG', 24, 3, 1, NULL),
('ORG0010202', '浦西支行', 'ORG', 24, 3, 2, NULL),
('ORG0010301', '南山支行', 'ORG', 25, 3, 1, NULL),
('ORG0010302', '福田支行', 'ORG', 25, 3, 2, NULL),
('ORG0010401', '天河支行', 'ORG', 26, 3, 1, NULL),
('ORG0010501', '西湖支行', 'ORG', 27, 3, 1, NULL);

-- 1.3 产品主数据（6大类 × 3级 = 51条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('PROD01', '公司贷款', 'PRODUCT', 0, 1, 1, 'LOAN'),
('PROD02', '个人贷款', 'PRODUCT', 0, 1, 2, 'LOAN'),
('PROD03', '公司存款', 'PRODUCT', 0, 1, 3, 'DEPOSIT'),
('PROD04', '个人存款', 'PRODUCT', 0, 1, 4, 'DEPOSIT'),
('PROD05', '理财产品', 'PRODUCT', 0, 1, 5, 'DEPOSIT'),
('PROD06', '国际业务', 'PRODUCT', 0, 1, 6, 'LOAN'),
('PROD0101', '短期贷款', 'PRODUCT', 38, 2, 1, 'LOAN'),
('PROD0102', '中长期贷款', 'PRODUCT', 38, 2, 2, 'LOAN'),
('PROD0201', '住房贷款', 'PRODUCT', 39, 2, 1, 'LOAN'),
('PROD0202', '消费贷款', 'PRODUCT', 39, 2, 2, 'LOAN'),
('PROD0301', '活期存款', 'PRODUCT', 40, 2, 1, 'DEPOSIT'),
('PROD0302', '定期存款', 'PRODUCT', 40, 2, 2, 'DEPOSIT'),
('PROD0401', '活期储蓄', 'PRODUCT', 41, 2, 1, 'DEPOSIT'),
('PROD0402', '定期储蓄', 'PRODUCT', 41, 2, 2, 'DEPOSIT'),
('PROD0501', '净值型理财', 'PRODUCT', 42, 2, 1, 'DEPOSIT'),
('PROD0502', '结构性存款', 'PRODUCT', 42, 2, 2, 'DEPOSIT'),
('PROD0601', '国际结算', 'PRODUCT', 43, 2, 1, 'LOAN'),
('PROD0602', '贸易融资', 'PRODUCT', 43, 2, 2, 'LOAN'),
('PROD010101', '流动资金贷款', 'PRODUCT', 44, 3, 1, 'LOAN'),
('PROD010102', '银承汇票', 'PRODUCT', 44, 3, 2, 'LOAN'),
('PROD010201', '固定资产贷款', 'PRODUCT', 45, 3, 1, 'LOAN'),
('PROD010202', '项目贷款', 'PRODUCT', 45, 3, 2, 'LOAN'),
('PROD020101', '首套房贷款', 'PRODUCT', 46, 3, 1, 'LOAN'),
('PROD020102', '二套房贷款', 'PRODUCT', 46, 3, 2, 'LOAN'),
('PROD020201', '个人消费贷款', 'PRODUCT', 47, 3, 1, 'LOAN'),
('PROD020202', '个人经营贷款', 'PRODUCT', 47, 3, 2, 'LOAN'),
('PROD030101', '协定存款', 'PRODUCT', 48, 3, 1, 'DEPOSIT'),
('PROD030102', '通知存款', 'PRODUCT', 48, 3, 2, 'DEPOSIT'),
('PROD030201', '整存整取', 'PRODUCT', 49, 3, 1, 'DEPOSIT'),
('PROD030202', '大额存单', 'PRODUCT', 49, 3, 2, 'DEPOSIT'),
('PROD040101', '借记卡存款', 'PRODUCT', 50, 3, 1, 'DEPOSIT'),
('PROD040102', '活期储蓄', 'PRODUCT', 50, 3, 2, 'DEPOSIT'),
('PROD040201', '零存整取', 'PRODUCT', 51, 3, 1, 'DEPOSIT'),
('PROD040202', '整存零取', 'PRODUCT', 51, 3, 2, 'DEPOSIT');

-- 1.4 条线主数据（3条线 × 3级 = 18条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('BL01', '对公条线', 'BIZ_LINE', 0, 1, 1, NULL),
('BL02', '零售条线', 'BIZ_LINE', 0, 1, 2, NULL),
('BL03', '金融市场条线', 'BIZ_LINE', 0, 1, 3, NULL),
('BL0101', '大客户部', 'BIZ_LINE', 66, 2, 1, NULL),
('BL0102', '中小企业部', 'BIZ_LINE', 66, 2, 2, NULL),
('BL0201', '个人信贷部', 'BIZ_LINE', 67, 2, 1, NULL),
('BL0202', '个人负债部', 'BIZ_LINE', 67, 2, 2, NULL),
('BL0301', '同业部', 'BIZ_LINE', 68, 2, 1, NULL),
('BL0302', '投资部', 'BIZ_LINE', 68, 2, 2, NULL),
('BL010101', '央国企组', 'BIZ_LINE', 69, 3, 1, NULL),
('BL010102', '上市公司组', 'BIZ_LINE', 69, 3, 2, NULL),
('BL010201', '制造业组', 'BIZ_LINE', 70, 3, 1, NULL),
('BL010202', '服务业组', 'BIZ_LINE', 70, 3, 2, NULL),
('BL020101', '房贷组', 'BIZ_LINE', 71, 3, 1, NULL),
('BL020102', '消费贷组', 'BIZ_LINE', 71, 3, 2, NULL),
('BL020201', '储蓄组', 'BIZ_LINE', 72, 3, 1, NULL),
('BL020202', '理财组', 'BIZ_LINE', 72, 3, 2, NULL),
('BL030101', '银行组', 'BIZ_LINE', 73, 3, 1, NULL),
('BL030102', '非银组', 'BIZ_LINE', 73, 3, 2, NULL),
('BL030201', '债券组', 'BIZ_LINE', 74, 3, 1, NULL),
('BL030202', '基金组', 'BIZ_LINE', 74, 3, 2, NULL);

-- 1.5 渠道主数据（2渠道 × 3级 = 12条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('CH01', '线下渠道', 'CHANNEL', 0, 1, 1, NULL),
('CH02', '线上渠道', 'CHANNEL', 0, 1, 2, NULL),
('CH0101', '网点渠道', 'CHANNEL', 82, 2, 1, NULL),
('CH0102', '外拓渠道', 'CHANNEL', 82, 2, 2, NULL),
('CH0201', '手机银行', 'CHANNEL', 83, 2, 1, NULL),
('CH0202', '网上银行', 'CHANNEL', 83, 2, 2, NULL),
('CH010101', '自助银行', 'CHANNEL', 84, 3, 1, NULL),
('CH010102', '智能柜台', 'CHANNEL', 84, 3, 2, NULL),
('CH010201', '客户经理外拓', 'CHANNEL', 85, 3, 1, NULL),
('CH010202', '社区银行', 'CHANNEL', 85, 3, 2, NULL),
('CH020101', 'APP渠道', 'CHANNEL', 86, 3, 1, NULL),
('CH020102', '小程序渠道', 'CHANNEL', 86, 3, 2, NULL),
('CH020201', '个人网银', 'CHANNEL', 87, 3, 1, NULL),
('CH020202', '企业网银', 'CHANNEL', 87, 3, 2, NULL);

-- 1.6 客户经理主数据（5分行 × 3级 = 27条）
INSERT IGNORE INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('MGRP01', '北京分行客户经理', 'MANAGER', 0, 1, 1, NULL),
('MGRP02', '上海分行客户经理', 'MANAGER', 0, 1, 2, NULL),
('MGRP03', '深圳分行客户经理', 'MANAGER', 0, 1, 3, NULL),
('MGRP04', '广州分行客户经理', 'MANAGER', 0, 1, 4, NULL),
('MGRP05', '杭州分行客户经理', 'MANAGER', 0, 1, 5, NULL),
('MGRP0101', '朝阳支行客户经理', 'MANAGER', 96, 2, 1, NULL),
('MGRP0102', '海淀支行客户经理', 'MANAGER', 96, 2, 2, NULL),
('MGRP0103', '西城支行客户经理', 'MANAGER', 96, 2, 3, NULL),
('MGRP0201', '浦东支行客户经理', 'MANAGER', 97, 2, 1, NULL),
('MGRP0202', '浦西支行客户经理', 'MANAGER', 97, 2, 2, NULL),
('MGRP0301', '南山支行客户经理', 'MANAGER', 98, 2, 1, NULL),
('MGRP0302', '福田支行客户经理', 'MANAGER', 98, 2, 2, NULL),
('MGRP0401', '天河支行客户经理', 'MANAGER', 99, 2, 1, NULL),
('MGRP0501', '西湖支行客户经理', 'MANAGER', 100, 2, 1, NULL),
('MGR001', '张明', 'MANAGER', 101, 3, 1, NULL),
('MGR002', '周伟', 'MANAGER', 101, 3, 2, NULL),
('MGR003', '李华', 'MANAGER', 102, 3, 1, NULL),
('MGR004', '赵强', 'MANAGER', 102, 3, 2, NULL),
('MGR005', '郑磊', 'MANAGER', 103, 3, 1, NULL),
('MGR006', '王芳', 'MANAGER', 104, 3, 1, NULL),
('MGR007', '陈静', 'MANAGER', 104, 3, 2, NULL),
('MGR008', '吴敏', 'MANAGER', 105, 3, 1, NULL),
('MGR009', '孙丽', 'MANAGER', 106, 3, 1, NULL),
('MGR010', '刘洋', 'MANAGER', 107, 3, 1, NULL),
('MGR011', '徐磊', 'MANAGER', 108, 3, 1, NULL),
('MGR012', '马超', 'MANAGER', 109, 3, 1, NULL);

-- ============================================
-- 二、指标配置数据
-- ============================================

-- 2.1 原子指标（资产5条 + 负债4条）
INSERT IGNORE INTO atomic_indicator (code, name, business_line, source_table, source_field, filter_condition, detail_table, detail_dimension, detail_display_fields, detail_group_by, unit, precision_val, description, sort_order) VALUES
('ASSET_BALANCE', '资产余额', 'ASSET', 'biz_ledger', 'biz_amount', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '资产本金余额', 1),
('ASSET_INTEREST', '资产利息收入', 'ASSET', 'biz_ledger', 'interest_income', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '资产对客利息收入', 2),
('ASSET_FTP', '资产FTP成本', 'ASSET', 'biz_ledger', 'ftp_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","ftp_cost","product_name"]', 'stat_date', '万元', 2, '资产FTP资金成本', 3),
('ASSET_RISK', '资产风险成本', 'ASSET', 'biz_ledger', 'risk_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","risk_cost","product_name"]', 'stat_date', '万元', 2, '资产风险计提成本', 4),
('ASSET_OP', '资产运营成本', 'ASSET', 'biz_ledger', 'op_cost', 'product_type=LOAN', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '资产运营成本', 5),
('LIABILITY_BALANCE', '负债余额', 'LIABILITY', 'biz_ledger', 'biz_amount', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","biz_amount","product_name"]', 'stat_date', '万元', 2, '负债本金余额', 1),
('LIABILITY_FTP', '负债FTP收入', 'LIABILITY', 'biz_ledger', 'interest_income', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_income","product_name"]', 'stat_date', '万元', 2, '负债FTP资金收入', 2),
('LIABILITY_INTEREST', '负债利息支出', 'LIABILITY', 'biz_ledger', 'interest_expense', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","interest_expense","product_name"]', 'stat_date', '万元', 2, '负债对客利息支出', 3),
('LIABILITY_OP', '负债运营成本', 'LIABILITY', 'biz_ledger', 'op_cost', 'product_type=DEPOSIT', 'biz_ledger', 'product_id', '["biz_id","customer_name","op_cost","product_name","expense_type"]', 'stat_date', '万元', 2, '负债运营成本', 4);

-- 2.2 派生指标（资产5条 + 负债4条）
INSERT IGNORE INTO derived_indicator (code, name, business_line, calc_formula, formula_vars, unit, precision_val, description, sort_order) VALUES
('ASSET_PROFIT', '资产利润', 'ASSET', 'ASSET_INTEREST - ASSET_FTP - ASSET_RISK - ASSET_OP', '["ASSET_INTEREST","ASSET_FTP","ASSET_RISK","ASSET_OP"]', '万元', 2, '资产净利润', 1),
('ASSET_NET_INTEREST', '资产净利息收入', 'ASSET', 'ASSET_INTEREST - ASSET_FTP', '["ASSET_INTEREST","ASSET_FTP"]', '万元', 2, '扣除FTP后的利息收入', 2),
('ASSET_COST_RATIO', '资产成本收入比', 'ASSET', '(ASSET_FTP + ASSET_RISK + ASSET_OP) / ASSET_INTEREST', '["ASSET_FTP","ASSET_RISK","ASSET_OP","ASSET_INTEREST"]', '%', 2, '成本占收入比', 3),
('ASSET_RISK_RATIO', '资产风险成本率', 'ASSET', 'ASSET_RISK / ASSET_INTEREST', '["ASSET_RISK","ASSET_INTEREST"]', '%', 2, '风险成本占比', 4),
('ASSET_FTP_SPREAD', '资产FTP利差', 'ASSET', '(ASSET_INTEREST - ASSET_FTP) / ASSET_BALANCE', '["ASSET_INTEREST","ASSET_FTP","ASSET_BALANCE"]', '%', 2, '净息差', 5),
('LIABILITY_PROFIT', '负债利润', 'LIABILITY', 'LIABILITY_FTP - LIABILITY_INTEREST - LIABILITY_OP', '["LIABILITY_FTP","LIABILITY_INTEREST","LIABILITY_OP"]', '万元', 2, '负债净利润', 1),
('LIABILITY_NET_INTEREST', '负债净利息收入', 'LIABILITY', 'LIABILITY_FTP - LIABILITY_INTEREST', '["LIABILITY_FTP","LIABILITY_INTEREST"]', '万元', 2, '扣除利息支出后的收入', 2),
('LIABILITY_COST_RATIO', '负债成本收入比', 'LIABILITY', '(LIABILITY_INTEREST + LIABILITY_OP) / LIABILITY_FTP', '["LIABILITY_INTEREST","LIABILITY_OP","LIABILITY_FTP"]', '%', 2, '成本占收入比', 3),
('LIABILITY_FTP_SPREAD', '负债FTP利差', 'LIABILITY', '(LIABILITY_FTP - LIABILITY_INTEREST) / LIABILITY_BALANCE', '["LIABILITY_FTP","LIABILITY_INTEREST","LIABILITY_BALANCE"]', '%', 2, '净息差', 4);

-- 2.3 统计口径（20条）
INSERT IGNORE INTO indicator_stat_config (indicator_code, stat_type, calc_formula, description) VALUES
('ASSET_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_BALANCE) / DAY(LAST_DAY(日期))', '资产月日均余额'),
('ASSET_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(ASSET_BALANCE) / DAYOFYEAR(日期)', '资产年日均余额'),
('ASSET_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_INTEREST) / DAY(LAST_DAY(日期))', '资产月日均利息收入'),
('ASSET_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(ASSET_INTEREST) / DAYOFYEAR(日期)', '资产年日均利息收入'),
('ASSET_FTP', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_FTP) / DAY(LAST_DAY(日期))', '资产月日均FTP成本'),
('ASSET_FTP', 'YEARLY_DAILY_AVG', 'SUM(ASSET_FTP) / DAYOFYEAR(日期)', '资产年日均FTP成本'),
('ASSET_RISK', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_RISK) / DAY(LAST_DAY(日期))', '资产月日均风险成本'),
('ASSET_RISK', 'YEARLY_DAILY_AVG', 'SUM(ASSET_RISK) / DAYOFYEAR(日期)', '资产年日均风险成本'),
('ASSET_OP', 'MONTHLY_DAILY_AVG', 'SUM(ASSET_OP) / DAY(LAST_DAY(日期))', '资产月日均运营成本'),
('ASSET_OP', 'YEARLY_DAILY_AVG', 'SUM(ASSET_OP) / DAYOFYEAR(日期)', '资产年日均运营成本'),
('LIABILITY_BALANCE', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_BALANCE) / DAY(LAST_DAY(日期))', '负债月日均余额'),
('LIABILITY_BALANCE', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_BALANCE) / DAYOFYEAR(日期)', '负债年日均余额'),
('LIABILITY_FTP', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_FTP) / DAY(LAST_DAY(日期))', '负债月日均FTP收入'),
('LIABILITY_FTP', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_FTP) / DAYOFYEAR(日期)', '负债年日均FTP收入'),
('LIABILITY_INTEREST', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_INTEREST) / DAY(LAST_DAY(日期))', '负债月日均利息支出'),
('LIABILITY_INTEREST', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_INTEREST) / DAYOFYEAR(日期)', '负债年日均利息支出'),
('LIABILITY_OP', 'MONTHLY_DAILY_AVG', 'SUM(LIABILITY_OP) / DAY(LAST_DAY(日期))', '负债月日均运营成本'),
('LIABILITY_OP', 'YEARLY_DAILY_AVG', 'SUM(LIABILITY_OP) / DAYOFYEAR(日期)', '负债年日均运营成本');

-- 2.4 指标定义（22条）
INSERT IGNORE INTO indicator_definition (indicator_code, indicator_name, indicator_type, business_line, calc_formula, data_source, unit, precision_val, supported_dims, description, sort_order) VALUES
('LOAN_BALANCE', '在贷余额', 'SCALE', 'ASSET', 'SUM(loan_balance)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款本金余额', 1),
('DEPOSIT_BALANCE', '存款余额', 'SCALE', 'LIABILITY', 'SUM(deposit_balance)', 'deposit_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款本金余额', 2),
('LOAN_COUNT', '贷款笔数', 'SCALE', 'ASSET', 'COUNT(biz_id)', 'loan_indicator_detail', '笔', 0, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款业务笔数', 3),
('DEPOSIT_COUNT', '存款笔数', 'SCALE', 'LIABILITY', 'COUNT(biz_id)', 'deposit_indicator_detail', '笔', 0, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款业务笔数', 4),
('INTEREST_INCOME', '利息收入', 'REVENUE', 'ASSET', 'SUM(loan_monthly_interest)', 'loan_indicator_detail', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款利息收入', 5),
('FTP_INCOME', 'FTP收入', 'REVENUE', 'LIABILITY', 'SUM(ftp_income)', 'deposit_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款FTP收入', 6),
('NET_INTEREST_INCOME', '净利息收入', 'REVENUE', 'ALL', '利息收入-FTP成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '扣除FTP后的利息收入', 7),
('FTP_COST', 'FTP成本', 'COST', 'ASSET', 'SUM(ftp_cost)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款FTP资金成本', 8),
('RISK_COST', '风险成本', 'COST', 'ASSET', 'SUM(risk_cost)', 'loan_indicator_detail', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款风险计提成本', 9),
('OP_COST', '运营成本', 'COST', 'ALL', 'SUM(op_cost)', 'biz_profit_summary', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '运营成本（含分摊）', 10),
('TOTAL_COST', '总成本', 'COST', 'ALL', 'FTP成本+风险成本+运营成本', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '各项成本之和', 11),
('COST_INCOME_RATIO', '成本收入比', 'EFFICIENCY', 'ALL', '总成本/总收入', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '成本占收入比', 12),
('RISK_COST_RATIO', '风险成本率', 'EFFICIENCY', 'ASSET', '风险成本/利息收入', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '风险成本占比', 13),
('FTP_SPREAD', 'FTP利差', 'EFFICIENCY', 'ASSET', '(利息收入-FTP成本)/余额', '计算', '%', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '净息差', 14),
('LOAN_PROFIT', '贷款利润', 'PROFIT', 'ASSET', '利息收入-FTP成本-风险成本-运营成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '贷款业务利润', 15),
('DEPOSIT_PROFIT', '存款利润', 'PROFIT', 'LIABILITY', 'FTP收入-利息支出-运营成本', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER,CUSTOMER', '存款业务利润', 16),
('TOTAL_PROFIT', '总利润', 'PROFIT', 'ALL', '贷款利润+存款利润', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '总业务利润', 17),
('PROFIT_PER_MANAGER', '人均利润', 'PROFIT', 'ALL', '总利润/客户经理人数', '计算', '万元', 4, 'ORG,BIZ_LINE', '客户经理人均利润', 18),
('LOAN_BALANCE_MONTHLY_AVG', '贷款余额月日均', 'DAILY_AVG', 'ASSET', 'SUM(loan_balance)/当月天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '贷款余额月日均', 19),
('LOAN_BALANCE_YEARLY_AVG', '贷款余额年日均', 'DAILY_AVG', 'ASSET', 'SUM(loan_balance)/当年天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '贷款余额年日均', 20),
('DEPOSIT_BALANCE_MONTHLY_AVG', '存款余额月日均', 'DAILY_AVG', 'LIABILITY', 'SUM(deposit_balance)/当月天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '存款余额月日均', 21),
('DEPOSIT_BALANCE_YEARLY_AVG', '存款余额年日均', 'DAILY_AVG', 'LIABILITY', 'SUM(deposit_balance)/当年天数', '计算', '万元', 2, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '存款余额年日均', 22),
('INTEREST_MONTHLY_AVG', '利息收入月日均', 'DAILY_AVG', 'ASSET', 'SUM(利息)/当月天数', '计算', '万元', 4, 'ORG,BIZ_LINE,PRODUCT,CHANNEL,MANAGER', '利息收入月日均', 23);

-- 验证
SELECT '维度主数据' AS category, dim_type, COUNT(*) AS cnt
FROM dimension_master GROUP BY dim_type
UNION ALL
SELECT '原子指标', 'ALL', COUNT(*) FROM atomic_indicator
UNION ALL
SELECT '派生指标', 'ALL', COUNT(*) FROM derived_indicator
UNION ALL
SELECT '统计口径', 'ALL', COUNT(*) FROM indicator_stat_config
UNION ALL
SELECT '指标定义', 'ALL', COUNT(*) FROM indicator_definition;

SELECT 'Step 1 完成：主数据插入成功' AS result;
