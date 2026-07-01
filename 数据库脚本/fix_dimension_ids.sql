-- 数据库脚本/fix_dimension_ids.sql
-- 修正原始数据中的维度ID
-- 创建时间：2026-06-30

-- 1. 修正机构org_id
CREATE TEMPORARY TABLE org_mapping AS
SELECT DISTINCT org_name,
    CASE org_name
        WHEN '北京分行' THEN 2
        WHEN '广州分行' THEN 3
        WHEN '上海分行' THEN 4
        WHEN '深圳分行' THEN 5
        WHEN '杭州分行' THEN 6
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN org_mapping m ON l.org_name = m.org_name
SET l.org_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN org_mapping m ON d.org_name = m.org_name
SET d.org_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE org_mapping;

-- 2. 修正条线biz_line_id
CREATE TEMPORARY TABLE biz_line_mapping AS
SELECT DISTINCT biz_line_name,
    CASE biz_line_name
        WHEN '金融市场条线' THEN 1
        WHEN '对公条线' THEN 2
        WHEN '零售条线' THEN 3
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN biz_line_mapping m ON l.biz_line_name = m.biz_line_name
SET l.biz_line_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN biz_line_mapping m ON d.biz_line_name = m.biz_line_name
SET d.biz_line_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE biz_line_mapping;

-- 3. 修正产品product_id
CREATE TEMPORARY TABLE product_mapping AS
SELECT DISTINCT product_name,
    CASE product_name
        WHEN '住房贷款' THEN 1
        WHEN '个人贷款' THEN 2
        WHEN '公司贷款' THEN 3
        WHEN '短期贷款' THEN 4
        WHEN '中长期贷款' THEN 5
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN product_mapping m ON l.product_name = m.product_name
SET l.product_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN product_mapping m ON d.product_name = m.product_name
SET d.product_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE product_mapping;

-- 4. 修正渠道channel_id
CREATE TEMPORARY TABLE channel_mapping AS
SELECT DISTINCT channel_name,
    CASE channel_name
        WHEN '线下渠道' THEN 1
        WHEN '线上渠道' THEN 2
        WHEN '网点渠道' THEN 3
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN channel_mapping m ON l.channel_name = m.channel_name
SET l.channel_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN channel_mapping m ON d.channel_name = m.channel_name
SET d.channel_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE channel_mapping;

-- 5. 修正客户经理manager_id
CREATE TEMPORARY TABLE manager_mapping AS
SELECT DISTINCT manager_name,
    CASE manager_name
        WHEN '北京分行客户经理' THEN 1
        WHEN '广州分行客户经理' THEN 2
        WHEN '上海分行客户经理' THEN 3
        WHEN '深圳分行客户经理' THEN 4
        WHEN '杭州分行客户经理' THEN 5
    END as new_id
FROM loan_indicator_detail WHERE account_period='2026-06';

UPDATE loan_indicator_detail l JOIN manager_mapping m ON l.manager_name = m.manager_name
SET l.manager_id = m.new_id WHERE l.account_period='2026-06';

UPDATE deposit_indicator_detail d JOIN manager_mapping m ON d.manager_name = m.manager_name
SET d.manager_id = m.new_id WHERE d.account_period='2026-06';

DROP TEMPORARY TABLE manager_mapping;
