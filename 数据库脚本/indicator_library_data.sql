-- 数据库脚本/indicator_library_data.sql
-- 灌入30+指标定义(资产/负债分域新命名)

TRUNCATE TABLE indicator_library;

-- ============================================
-- 一、规模类(SCALE) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_BALANCE', '贷款时点余额', 'SCALE', 'DECIMAL', '万元', 2,
 '期末loan_balance/10000', 'dwd_loan_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 1, 1),
('LOAN_DAVG_BALANCE', '贷款日均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日loan_balance)/10000', 'loan_indicator_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 2, 1),
('LOAN_MAVG_BALANCE', '贷款月均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日loan_balance)/10000', 'loan_indicator_detail', 'loan_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 3, 1);

-- 二、规模类(SCALE) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('DEPOSIT_BALANCE', '存款时点余额', 'SCALE', 'DECIMAL', '万元', 2,
 '期末deposit_balance/10000', 'dwd_deposit_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 10, 1),
('DEPOSIT_DAVG_BALANCE', '存款日均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日deposit_balance)/10000', 'deposit_indicator_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 11, 1),
('DEPOSIT_MAVG_BALANCE', '存款月均余额', 'SCALE', 'DECIMAL', '万元', 2,
 'AVG(每日deposit_balance)/10000', 'deposit_indicator_detail', 'deposit_balance',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 12, 1);

-- ============================================
-- 三、利息收入类(REVENUE) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_DAILY_INTEREST', '当日贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'loan_daily_interest/10000', 'loan_indicator_detail', 'loan_daily_interest',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 20, 1),
('LOAN_MONTHLY_INTEREST', '当月贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(loan_daily_interest)/10000', 'dwd_loan_detail', 'loan_monthly_interest',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 21, 1),
('LOAN_YEARLY_INTEREST', '当年贷款利息收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(loan_monthly_interest)/10000', 'dwd_loan_detail', 'loan_monthly_interest',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 22, 1);

-- 四、利息收入类(REVENUE) - 负债域(FTP收入)
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('FTP_DAILY_INCOME', '当日FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'ftp_income/10000', 'deposit_indicator_detail', 'ftp_income',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 30, 1),
('FTP_MONTHLY_INCOME', '当月FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(ftp_income)/10000', 'dwd_deposit_detail', 'ftp_income',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 31, 1),
('FTP_YEARLY_INCOME', '当年FTP收入', 'REVENUE', 'DECIMAL', '万元', 2,
 'SUM(ftp_income)/10000', 'dwd_deposit_detail', 'ftp_income',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 32, 1);

-- 五、利息支出类(EXPENSE) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('INTEREST_DAILY_EXPENSE', '当日利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'deposit_daily_interest/10000', 'deposit_indicator_detail', 'deposit_daily_interest',
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 40, 1),
('INTEREST_MONTHLY_EXPENSE', '当月利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'SUM(deposit_daily_interest)/10000', 'dwd_deposit_detail', 'deposit_monthly_interest',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 41, 1),
('INTEREST_YEARLY_EXPENSE', '当年利息支出', 'EXPENSE', 'DECIMAL', '万元', 2,
 'SUM(deposit_monthly_interest)/10000', 'dwd_deposit_detail', 'deposit_monthly_interest',
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 42, 1);

-- ============================================
-- 六、成本类(COST) - 资产域
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_FTP_COST', '贷款FTP成本', 'COST', 'DECIMAL', '万元', 2,
 'SUM(ftp_cost)/10000', 'dwd_loan_detail', 'ftp_cost',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 50, 1),
('LOAN_RISK_COST', '贷款风险成本', 'COST', 'DECIMAL', '万元', 2,
 'SUM(risk_cost)/10000', 'dwd_loan_detail', 'risk_cost',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 51, 1),
('LOAN_OP_COST', '贷款运营成本', 'COST', 'DECIMAL', '万元', 2,
 'expense_allocation_result分摊额/10000', 'expense_allocation_result', 'allocated_amount',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 52, 1);

-- 七、成本类(COST) - 负债域
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('DEPOSIT_OP_COST', '存款运营成本', 'COST', 'DECIMAL', '万元', 2,
 'expense_allocation_result分摊额/10000', 'expense_allocation_result', 'allocated_amount',
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 60, 1);

-- ============================================
-- 八、利润类(PROFIT) - 派生指标
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_DAILY_PROFIT', '当日贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 '当日利息-当日FTP-当日风险-0(运营成本当日为0)', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 70, 1),
('LOAN_MONTHLY_PROFIT', '当月贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_MONTHLY_INTEREST - LOAN_FTP_COST - LOAN_RISK_COST - LOAN_OP_COST', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 71, 1),
('LOAN_YEARLY_PROFIT', '当年贷款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_YEARLY_INTEREST - LOAN_FTP_COST(YEAR) - LOAN_RISK_COST(YEAR) - LOAN_OP_COST(YEAR)', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 72, 1),

('DEPOSIT_DAILY_PROFIT', '当日存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_DAILY_INCOME - INTEREST_DAILY_EXPENSE - 0(运营成本当日为0)', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 80, 1),
('DEPOSIT_MONTHLY_PROFIT', '当月存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_MONTHLY_INCOME - INTEREST_MONTHLY_EXPENSE - DEPOSIT_OP_COST', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 81, 1),
('DEPOSIT_YEARLY_PROFIT', '当年存款利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'FTP_YEARLY_INCOME - INTEREST_YEARLY_EXPENSE - DEPOSIT_OP_COST(YEAR)', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 82, 1),

('TOTAL_DAILY_PROFIT', '当日总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_DAILY_PROFIT + DEPOSIT_DAILY_PROFIT', 'derived', NULL,
 'DAY', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 90, 1),
('TOTAL_MONTHLY_PROFIT', '当月总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_MONTHLY_PROFIT + DEPOSIT_MONTHLY_PROFIT', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 91, 1),
('TOTAL_YEARLY_PROFIT', '当年总利润', 'PROFIT', 'DECIMAL', '万元', 2,
 'LOAN_YEARLY_PROFIT + DEPOSIT_YEARLY_PROFIT', 'derived', NULL,
 'YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 92, 1);

-- ============================================
-- 九、率值类(RATIO) - 预计算存DWS
-- ============================================
INSERT INTO indicator_library (code, name, category, data_type, unit, precision_val, calc_formula, data_source, source_field, pre_calc_periods, supported_dims, sort_order, status)
VALUES
('LOAN_RATE', '贷款平均利率', 'RATIO', 'DECIMAL', '%', 4,
 'LOAN_YEARLY_INTEREST / LOAN_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 100, 1),
('LOAN_FTP_RATIO', 'FTP成本占比', 'RATIO', 'DECIMAL', '%', 2,
 'LOAN_FTP_COST / LOAN_MONTHLY_INTEREST * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 101, 1),
('LOAN_RISK_RATIO', '风险成本占比', 'RATIO', 'DECIMAL', '%', 2,
 'LOAN_RISK_COST / LOAN_MONTHLY_INTEREST * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 102, 1),
('DEPOSIT_RATE', '存款付息率', 'RATIO', 'DECIMAL', '%', 4,
 'INTEREST_YEARLY_EXPENSE / DEPOSIT_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 110, 1),
('FTP_SPREAD', 'FTP利差', 'RATIO', 'DECIMAL', '%', 4,
 '(FTP_YEARLY_INCOME - INTEREST_YEARLY_EXPENSE) / DEPOSIT_MAVG_BALANCE * 100', 'derived', NULL,
 'MONTH,YEAR', 'ORG,BIZ_LINE,DEPT,PRODUCT,CHANNEL,MANAGER,CUSTOMER', 111, 1),
('COST_INCOME_RATIO', '成本收入比', 'RATIO', 'DECIMAL', '%', 2,
 '(LOAN_FTP_COST + LOAN_RISK_COST + LOAN_OP_COST + DEPOSIT_OP_COST + INTEREST_MONTHLY_EXPENSE) / (LOAN_MONTHLY_INTEREST + FTP_MONTHLY_INCOME) * 100', 'derived', NULL,
 'MONTH,YEAR', 'TOTAL', 120, 1),
('PROFIT_MARGIN', '利润率', 'RATIO', 'DECIMAL', '%', 2,
 'TOTAL_MONTHLY_PROFIT / (LOAN_MONTHLY_INTEREST + FTP_MONTHLY_INCOME) * 100', 'derived', NULL,
 'MONTH,YEAR', 'TOTAL', 121, 1);
