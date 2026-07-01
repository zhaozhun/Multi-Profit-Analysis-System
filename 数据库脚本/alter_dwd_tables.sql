-- 给DWD层表添加派生指标字段
-- 创建时间：2026-06-30

-- 贷款明细表添加字段
ALTER TABLE dwd_loan_detail
ADD COLUMN IF NOT EXISTS loan_profit DECIMAL(18,4) COMMENT '贷款利润=利息收入-FTP成本-风险成本-运营成本' AFTER op_cost,
ADD COLUMN IF NOT EXISTS net_interest_margin DECIMAL(18,4) COMMENT '净利差=利息收入-FTP成本' AFTER loan_profit;

-- 存款明细表添加字段
ALTER TABLE dwd_deposit_detail
ADD COLUMN IF NOT EXISTS deposit_profit DECIMAL(18,4) COMMENT '存款利润=FTP收入-利息支出-运营成本' AFTER op_cost;

SELECT 'DWD层表结构更新完成' AS result;
