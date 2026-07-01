-- 数据库脚本/cleanup_rebuild.sql
-- 清空ODS/DWD/DWS/分摊结果数据

-- 清空ODS层
TRUNCATE TABLE loan_indicator_detail;
TRUNCATE TABLE deposit_indicator_detail;

-- 清空DWD层
TRUNCATE TABLE dwd_loan_detail;
TRUNCATE TABLE dwd_deposit_detail;

-- 清空DWS层
TRUNCATE TABLE dw_indicator_fact;

-- 清空费用分摊结果(需重跑分摊引擎)
TRUNCATE TABLE expense_allocation_result;

-- 注意:费用原始表(expense_rent/salary/it/marketing/other)保留不动
-- 主数据(customer_master)保留不动
