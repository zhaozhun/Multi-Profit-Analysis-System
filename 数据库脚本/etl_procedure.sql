-- 数据库脚本/etl_procedure.sql
-- ETL存储过程（修正版：在DWD层计算派生指标）
-- 创建时间：2026-06-30

DELIMITER //

DROP PROCEDURE IF EXISTS sp_etl_recalculate//

CREATE PROCEDURE sp_etl_recalculate(IN p_period VARCHAR(10))
BEGIN
    -- ============================================================
    -- 1. 清除该账期的DWD数据
    -- ============================================================
    DELETE FROM dwd_loan_detail WHERE account_period = p_period;
    DELETE FROM dwd_deposit_detail WHERE account_period = p_period;

    -- ============================================================
    -- 2. 从ODS层清洗数据到DWD层（贷款）
    --    - 关联维度主数据
    --    - 计算派生指标（贷款利润、净利差）
    -- ============================================================
    INSERT INTO dwd_loan_detail (
        biz_id, account_period, caliber_type,
        org_id, org_name, biz_line_id, biz_line_name,
        product_id, product_name, channel_id, channel_name,
        manager_id, manager_name, customer_id, customer_name,
        loan_balance, loan_monthly_interest, ftp_cost, risk_cost, op_cost,
        loan_profit, net_interest_margin
    )
    SELECT
        l.biz_id, l.account_period, l.caliber_type,
        o.id, l.org_name, bl.id, l.biz_line_name,
        p.id, l.product_name, c.id, l.channel_name,
        m.id, l.manager_name, l.customer_id, l.customer_name,
        l.loan_balance, l.loan_monthly_interest, l.ftp_cost, l.risk_cost,
        COALESCE(ear.op_cost, 0),  -- 从费用分摊结果获取运营成本
        -- 派生指标：贷款利润 = 利息收入 - FTP成本 - 风险成本 - 运营成本
        (l.loan_monthly_interest - l.ftp_cost - l.risk_cost - COALESCE(ear.op_cost, 0)),
        -- 派生指标：净利差 = 利息收入 - FTP成本
        (l.loan_monthly_interest - l.ftp_cost)
    FROM loan_indicator_detail l
    LEFT JOIN dw_dim_organization o ON l.org_name = o.org_name
    LEFT JOIN dw_dim_biz_line bl ON l.biz_line_name = bl.line_name
    LEFT JOIN dw_dim_product p ON l.product_name = p.product_name
    LEFT JOIN dw_dim_channel c ON l.channel_name = c.channel_name
    LEFT JOIN dw_dim_manager m ON l.manager_name = m.manager_name
    LEFT JOIN (
        SELECT biz_id, SUM(allocated_amount) as op_cost
        FROM expense_allocation_result
        WHERE period = p_period
        GROUP BY biz_id
    ) ear ON l.biz_id = ear.biz_id
    WHERE l.account_period = p_period;

    -- ============================================================
    -- 3. 从ODS层清洗数据到DWD层（存款）
    --    - 关联维度主数据
    --    - 计算派生指标（存款利润）
    -- ============================================================
    INSERT INTO dwd_deposit_detail (
        biz_id, account_period, caliber_type,
        org_id, org_name, biz_line_id, biz_line_name,
        product_id, product_name, channel_id, channel_name,
        manager_id, manager_name, customer_id, customer_name,
        deposit_balance, deposit_monthly_interest, ftp_income, op_cost,
        deposit_profit
    )
    SELECT
        d.biz_id, d.account_period, d.caliber_type,
        o.id, d.org_name, bl.id, d.biz_line_name,
        p.id, d.product_name, c.id, d.channel_name,
        m.id, d.manager_name, d.customer_id, d.customer_name,
        d.deposit_balance, d.deposit_monthly_interest, d.ftp_income,
        COALESCE(ear.op_cost, 0),  -- 从费用分摊结果获取运营成本
        -- 派生指标：存款利润 = FTP收入 - 利息支出 - 运营成本
        (d.ftp_income - d.deposit_monthly_interest - COALESCE(ear.op_cost, 0))
    FROM deposit_indicator_detail d
    LEFT JOIN dw_dim_organization o ON d.org_name = o.org_name
    LEFT JOIN dw_dim_biz_line bl ON d.biz_line_name = bl.line_name
    LEFT JOIN dw_dim_product p ON d.product_name = p.product_name
    LEFT JOIN dw_dim_channel c ON d.channel_name = c.channel_name
    LEFT JOIN dw_dim_manager m ON d.manager_name = m.manager_name
    LEFT JOIN (
        SELECT biz_id, SUM(allocated_amount) as op_cost
        FROM expense_allocation_result
        WHERE period = p_period
        GROUP BY biz_id
    ) ear ON d.biz_id = ear.biz_id
    WHERE d.account_period = p_period;

    -- ============================================================
    -- 4. 清除该账期的DWS数据
    -- ============================================================
    DELETE FROM dw_indicator_fact WHERE period = p_period;

    -- ============================================================
    -- 5. 从DWD层按各维度汇总指标到DWS层
    --    注意：利润等派生指标直接SUM，不再重新计算
    -- ============================================================

    -- 5.1 按机构维度汇总贷款指标
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_COST', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(ftp_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'RISK_COST', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(risk_cost) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    -- 贷款利润：直接SUM DWD层已计算的派生指标
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    -- 净利差：直接SUM DWD层已计算的派生指标
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'NET_INTEREST_MARGIN', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(net_interest_margin) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    -- 5.2 按机构维度汇总存款指标
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_INCOME', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(ftp_income) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_INTEREST', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(deposit_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    -- 存款利润：直接SUM DWD层已计算的派生指标
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_PROFIT', p_period, 'MONTH', 'ORG', org_id, org_name,
           SUM(deposit_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY org_id, org_name;

    -- 5.3 按条线维度汇总
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY biz_line_id, biz_line_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY biz_line_id, biz_line_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_PROFIT', p_period, 'MONTH', 'BIZ_LINE', biz_line_id, biz_line_name,
           SUM(deposit_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_deposit_detail WHERE account_period = p_period
    GROUP BY biz_line_id, biz_line_name;

    -- 5.4 按产品维度汇总
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'PRODUCT', product_id, product_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY product_id, product_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'PRODUCT', product_id, product_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY product_id, product_name;

    -- 5.5 按渠道维度汇总
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CHANNEL', channel_id, channel_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY channel_id, channel_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'CHANNEL', channel_id, channel_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY channel_id, channel_name;

    -- 5.6 按客户经理维度汇总
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'MANAGER', manager_id, manager_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY manager_id, manager_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'MANAGER', manager_id, manager_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY manager_id, manager_name;

    -- 5.7 按客户维度汇总
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'CUSTOMER', customer_id, customer_name,
           SUM(loan_monthly_interest) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY customer_id, customer_name;

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'CUSTOMER', customer_id, customer_name,
           SUM(loan_profit) / 10000, 'ASSESS', NOW()
    FROM dwd_loan_detail WHERE account_period = p_period
    GROUP BY customer_id, customer_name;

    -- ============================================================
    -- 6. 计算TOTAL汇总（从ORG维度汇总）
    -- ============================================================
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'INTEREST_INCOME', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'INTEREST_INCOME' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'FTP_COST' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'RISK_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'RISK_COST' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'LOAN_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'LOAN_PROFIT' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'NET_INTEREST_MARGIN', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'NET_INTEREST_MARGIN' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'FTP_INCOME', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'FTP_INCOME' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_INTEREST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'DEPOSIT_INTEREST' AND period = p_period AND dim_type = 'ORG';

    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'DEPOSIT_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(calc_value), 'ASSESS', NOW()
    FROM dw_indicator_fact
    WHERE indicator_code = 'DEPOSIT_PROFIT' AND period = p_period AND dim_type = 'ORG';

    -- 6.2 计算总利润TOTAL = 贷款利润 + 存款利润
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'TOTAL_PROFIT', p_period, 'MONTH', 'TOTAL', 0, '全部',
           (SELECT COALESCE(SUM(calc_value), 0) FROM dw_indicator_fact WHERE indicator_code = 'LOAN_PROFIT' AND period = p_period AND dim_type = 'TOTAL')
           + (SELECT COALESCE(SUM(calc_value), 0) FROM dw_indicator_fact WHERE indicator_code = 'DEPOSIT_PROFIT' AND period = p_period AND dim_type = 'TOTAL'),
           'ASSESS', NOW();

    -- 6.3 计算运营成本TOTAL（从DWD层汇总）
    INSERT IGNORE INTO dw_indicator_fact (indicator_code, period, period_type, dim_type, dim_id, dim_name, calc_value, caliber_type, calc_time)
    SELECT 'OP_COST', p_period, 'MONTH', 'TOTAL', 0, '全部',
           SUM(op_cost) / 10000, 'ASSESS', NOW()
    FROM (
        SELECT op_cost FROM dwd_loan_detail WHERE account_period = p_period
        UNION ALL
        SELECT op_cost FROM dwd_deposit_detail WHERE account_period = p_period
    ) t;

END//

DELIMITER ;
