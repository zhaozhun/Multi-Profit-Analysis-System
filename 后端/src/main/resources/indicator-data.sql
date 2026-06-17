-- ============================================
-- 指标库Mock数据
-- ============================================

-- 规模类指标
INSERT INTO indicator_library (code, name, category, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) VALUES
('SCALE_BIZ_AMT', '业务规模', 'SCALE', '万元', 2, 'sum(biz_amount)', 'biz_ledger', 'biz_amount', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL","MANAGER","CUSTOMER"]', '全部业务金额汇总', 1),
('SCALE_LOAN_BAL', '贷款余额', 'SCALE', '万元', 2, 'sum(biz_amount) where product_type=贷款', 'biz_ledger', 'biz_amount', '["MONTH"]', '["ORG","PRODUCT","CUSTOMER"]', '贷款类产品业务金额汇总', 2),
('SCALE_DEPOSIT_BAL', '存款余额', 'SCALE', '万元', 2, 'sum(biz_amount) where product_type=存款', 'biz_ledger', 'biz_amount', '["MONTH"]', '["ORG","PRODUCT","CUSTOMER"]', '存款类产品业务金额汇总', 3),
('SCALE_CUSTOMER_CNT', '客户数', 'SCALE', '个', 0, 'count(distinct customer_name)', 'biz_ledger', 'customer_name', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT","CHANNEL"]', '去重客户数量', 4),
('SCALE_BIZ_CNT', '业务笔数', 'SCALE', '笔', 0, 'count(biz_id)', 'biz_ledger', 'biz_id', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL","MANAGER"]', '业务交易笔数', 5),
('SCALE_AVG_BAL', '日均余额', 'SCALE', '万元', 2, 'biz_amount/自然日天数', 'calc', 'biz_amount', '["DAY"]', '["ORG","PRODUCT"]', '业务金额日均值', 6);

-- 收入类指标
INSERT INTO indicator_library (code, name, category, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) VALUES
('REV_TOTAL', '业务总收入', 'REV', '万元', 2, 'sum(revenue)', 'biz_ledger', 'revenue', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL","MANAGER","CUSTOMER"]', '全部业务收入汇总', 1),
('REV_INTEREST', '利息收入', 'REV', '万元', 2, 'sum(interest_income)', 'biz_ledger', 'interest_income', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT","CUSTOMER"]', '利息收入汇总', 2),
('REV_FEE', '手续费收入', 'REV', '万元', 2, 'sum(fee_income)', 'biz_ledger', 'fee_income', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '手续费收入汇总', 3),
('REV_NON_INTEREST', '非息收入', 'REV', '万元', 2, 'sum(non_interest_income)', 'biz_ledger', 'non_interest_income', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '非利息收入汇总', 4),
('REV_YIELD', '综合收益率', 'REV', '%', 4, 'revenue/biz_amount*100', 'calc', 'revenue,biz_amount', '["MONTH"]', '["ORG","PRODUCT"]', '收入与业务规模的比率', 5),
('REV_INTEREST_RATIO', '利息收入占比', 'REV', '%', 2, 'interest_income/revenue*100', 'calc', 'interest_income,revenue', '["MONTH"]', '["ORG","PRODUCT"]', '利息收入占总收入比重', 6),
('REV_FEE_RATIO', '手续费收入占比', 'REV', '%', 2, 'fee_income/revenue*100', 'calc', 'fee_income,revenue', '["MONTH"]', '["ORG","PRODUCT"]', '手续费收入占总收入比重', 7),
('REV_NON_INT_RATIO', '非息收入占比', 'REV', '%', 2, 'non_interest_income/revenue*100', 'calc', 'non_interest_income,revenue', '["MONTH"]', '["ORG","PRODUCT"]', '非息收入占总收入比重', 8),
('REV_PER_CUST', '户均收入', 'REV', '万元', 2, 'revenue/customer_cnt', 'calc', 'revenue', '["MONTH"]', '["ORG","CUSTOMER"]', '每客户平均收入', 9),
('REV_GROWTH_RATE', '收入增长率', 'REV', '%', 2, '(当期-上期)/上期*100', 'calc', 'revenue', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '收入同比/环比增长率', 10);

-- 成本类指标
INSERT INTO indicator_library (code, name, category, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) VALUES
('COST_FTP', 'FTP资金成本', 'COST', '万元', 2, 'sum(ftp_cost)', 'biz_ledger', 'ftp_cost', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL","MANAGER","CUSTOMER"]', 'FTP资金转移成本', 1),
('COST_RISK', '风险成本', 'COST', '万元', 2, 'sum(risk_cost)', 'biz_ledger', 'risk_cost', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT","CUSTOMER"]', '风险计提成本', 2),
('COST_OP', '运营成本', 'COST', '万元', 2, 'sum(op_cost)', 'biz_ledger', 'op_cost', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL"]', '运营费用分摊', 3),
('COST_TOTAL', '总成本', 'COST', '万元', 2, 'ftp_cost+risk_cost+op_cost', 'calc', 'ftp_cost,risk_cost,op_cost', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '三项成本合计', 4),
('COST_FTP_RATE', 'FTP成本率', 'COST', '%', 4, 'ftp_cost/revenue*100', 'calc', 'ftp_cost,revenue', '["MONTH"]', '["ORG","PRODUCT"]', 'FTP成本占收入比', 5),
('COST_RISK_RATE', '风险成本率', 'COST', '%', 4, 'risk_cost/revenue*100', 'calc', 'risk_cost,revenue', '["MONTH"]', '["ORG","PRODUCT"]', '风险成本占收入比', 6),
('COST_OP_RATE', '运营成本率', 'COST', '%', 4, 'op_cost/revenue*100', 'calc', 'op_cost,revenue', '["MONTH"]', '["ORG","PRODUCT"]', '运营成本占收入比', 7),
('COST_INCOME_RATIO', '成本收入比', 'COST', '%', 2, 'total_cost/revenue*100', 'calc', 'total_cost,revenue', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '总成本占收入比', 8),
('COST_FTP_SPREAD', 'FTP利差', 'COST', '%', 4, '(revenue-ftp_cost)/biz_amount*100', 'calc', 'revenue,ftp_cost,biz_amount', '["MONTH"]', '["ORG","PRODUCT"]', '收入与FTP成本差额占规模比', 9),
('COST_PER_BIZ', '笔均成本', 'COST', '万元', 2, 'total_cost/biz_cnt', 'calc', 'total_cost', '["MONTH"]', '["ORG","PRODUCT"]', '每笔业务平均成本', 10);

-- 利润类指标
INSERT INTO indicator_library (code, name, category, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) VALUES
('PROFIT_NET', '净利润', 'PROFIT', '万元', 2, 'sum(net_profit)', 'biz_ledger', 'net_profit', '["DAY","MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","DEPT","PRODUCT","CHANNEL","MANAGER","CUSTOMER"]', '收入减去全部成本后的利润', 1),
('PROFIT_GROSS', '毛利润', 'PROFIT', '万元', 2, 'revenue-ftp_cost', 'calc', 'revenue,ftp_cost', '["MONTH","QUARTER","YEAR"]', '["ORG","PRODUCT"]', '收入减FTP成本', 2),
('PROFIT_MARGIN', '利润率', 'PROFIT', '%', 2, 'net_profit/revenue*100', 'calc', 'net_profit,revenue', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '净利润占收入比', 3),
('PROFIT_PER_CUST', '户均利润', 'PROFIT', '万元', 2, 'net_profit/customer_cnt', 'calc', 'net_profit', '["MONTH"]', '["ORG","CUSTOMER"]', '每客户平均利润', 4),
('PROFIT_PER_BIZ', '笔均利润', 'PROFIT', '万元', 2, 'net_profit/biz_cnt', 'calc', 'net_profit', '["MONTH"]', '["ORG","PRODUCT"]', '每笔业务平均利润', 5),
('PROFIT_BUDGET_RATE', '预算完成率', 'PROFIT', '%', 2, 'net_profit/budget_target*100', 'calc', 'net_profit', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE"]', '实际利润与预算目标比', 6),
('PROFIT_YOY', '利润同比增长率', 'PROFIT', '%', 2, '(当期-去年同期)/去年同期*100', 'calc', 'net_profit', '["MONTH","QUARTER","YEAR"]', '["ORG","BIZ_LINE","PRODUCT"]', '利润同比变化', 7),
('PROFIT_MOM', '利润环比增长率', 'PROFIT', '%', 2, '(当期-上期)/上期*100', 'calc', 'net_profit', '["MONTH"]', '["ORG","BIZ_LINE","PRODUCT"]', '利润环比变化', 8);

-- 效率类指标
INSERT INTO indicator_library (code, name, category, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, description, sort_order) VALUES
('EFF_ROE', '净资产收益率', 'EFF', '%', 2, 'net_profit/equity*100', 'external', 'net_profit', '["QUARTER","YEAR"]', '["ORG"]', '净利润与净资产比', 1),
('EFF_ROA', '资产利润率', 'EFF', '%', 4, 'net_profit/total_asset*100', 'external', 'net_profit', '["MONTH","QUARTER"]', '["ORG"]', '净利润与总资产比', 2),
('EFF_RAROC', '风险调整资本回报', 'EFF', '%', 2, 'net_profit/economic_capital*100', 'external', 'net_profit', '["QUARTER","YEAR"]', '["ORG","BIZ_LINE"]', '风险调整后的资本回报率', 3),
('EFF_PROFIT_PER_PERSON', '人均创利', 'EFF', '万元', 2, 'net_profit/headcount', 'external', 'net_profit', '["MONTH","QUARTER"]', '["ORG","BIZ_LINE","DEPT"]', '每人平均创造利润', 4),
('EFF_PROFIT_PER_ORG', '机构均利润', 'EFF', '万元', 2, 'net_profit/org_count', 'calc', 'net_profit', '["MONTH","QUARTER"]', '["ORG"]', '每个机构平均利润', 5),
('EFF_REVENUE_PER_PERSON', '人均收入', 'EFF', '万元', 2, 'revenue/headcount', 'external', 'revenue', '["MONTH"]', '["ORG","BIZ_LINE"]', '每人平均创造收入', 6),
('EFF_COST_PER_PERSON', '人均成本', 'EFF', '万元', 2, 'total_cost/headcount', 'external', 'total_cost', '["MONTH"]', '["ORG","BIZ_LINE"]', '每人平均分摊成本', 7),
('EFF_ASSET_TURNOVER', '资产周转率', 'EFF', '次', 4, 'revenue/total_asset', 'external', 'revenue', '["MONTH","QUARTER"]', '["ORG"]', '收入与总资产比', 8);

-- ============================================
-- 客户主数据
-- ============================================
INSERT INTO customer_master (customer_code, customer_name, customer_type, industry, region, credit_rating, manager_id, manager_name, org_id, org_name) VALUES
('CUST001', '华为技术有限公司', 'CORP', '信息技术', '深圳', 'AAA', 44, '张明', 7, '朝阳支行'),
('CUST002', '腾讯科技有限公司', 'CORP', '互联网', '深圳', 'AAA', 45, '李华', 8, '海淀支行'),
('CUST003', '阿里巴巴集团', 'CORP', '电子商务', '杭州', 'AAA', 46, '王芳', 15, '西湖支行'),
('CUST004', '京东商城有限公司', 'CORP', '电商物流', '北京', 'AA+', 44, '张明', 7, '朝阳支行'),
('CUST005', '比亚迪股份有限公司', 'CORP', '新能源', '深圳', 'AA+', 47, '赵强', 12, '南山支行'),
('CUST006', '宁德时代新能源', 'CORP', '新能源', '福建', 'AA+', 46, '王芳', 10, '浦东支行'),
('CUST007', '万科地产有限公司', 'CORP', '房地产', '深圳', 'AA', 47, '赵强', 12, '南山支行'),
('CUST008', '碧桂园控股', 'CORP', '房地产', '广州', 'AA', 48, '刘洋', 14, '天河支行'),
('CUST009', '张伟个人客户', 'PERSON', '个人', '北京', 'A', 45, '李华', 8, '海淀支行'),
('CUST010', '李娜个人客户', 'PERSON', '个人', '上海', 'A', 46, '王芳', 10, '浦东支行'),
('CUST011', '王磊个人客户', 'PERSON', '个人', '深圳', 'A', 47, '赵强', 12, '南山支行'),
('CUST012', '小微科技有限公司', 'CORP', '科技', '北京', 'A', 44, '张明', 9, '西城支行'),
('CUST013', '创新材料有限公司', 'CORP', '制造业', '上海', 'A', 46, '王芳', 11, '浦西支行'),
('CUST014', '绿色能源有限公司', 'CORP', '新能源', '杭州', 'AA', 45, '李华', 15, '西湖支行'),
('CUST015', '智慧物流有限公司', 'CORP', '物流', '广州', 'A', 48, '刘洋', 14, '天河支行');

-- 自定义报表预设模板
INSERT INTO custom_report_template (name, row_dims, col_metrics, filter_config, sort_config, is_system) VALUES
('分行产品盈利透视', '["ORG","PRODUCT"]', '["REV_TOTAL","COST_FTP","COST_RISK","COST_OP","PROFIT_NET"]', '{"level":"分行"}', '{"field":"PROFIT_NET","order":"desc"}', 1),
('条线机构盈利矩阵', '["BIZ_LINE","ORG"]', '["REV_TOTAL","PROFIT_NET","PROFIT_MARGIN","COST_INCOME_RATIO"]', '{}', '{"field":"PROFIT_NET","order":"desc"}', 1),
('客户经理业绩排名', '["MANAGER"]', '["REV_TOTAL","PROFIT_NET","SCALE_CUSTOMER_CNT","EFF_PROFIT_PER_PERSON"]', '{}', '{"field":"PROFIT_NET","order":"desc"}', 1),
('产品损益明细表', '["PRODUCT"]', '["SCALE_BIZ_AMT","REV_TOTAL","REV_INTEREST","REV_FEE","COST_FTP","COST_RISK","COST_OP","PROFIT_NET","PROFIT_MARGIN"]', '{}', '{"field":"REV_TOTAL","order":"desc"}', 1);
