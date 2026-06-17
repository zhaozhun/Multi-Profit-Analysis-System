---
name: database-schema
description: 数据库表结构、核心字段、数据关系
metadata:
  type: reference
---

## 核心表

### dimension_master（维度主数据）
- 支持3级层次结构（parent_id, level）
- dim_type: ORG(机构), PRODUCT(产品), BIZ_LINE(条线), DEPT(部门), CHANNEL(渠道), MANAGER(客户经理)
- 包含 code, name, parent_id, level, sort_order

### biz_ledger（业务明细台账）
核心事实表，按日统计，包含：
- 维度字段：org_*, product_*, biz_line_*, dept_*, channel_*, manager_*, customer_name
- 金额字段：biz_amount, revenue, interest_income, interest_expense, fee_income, non_interest_income, ftp_cost, risk_cost, op_cost, net_profit
- 口径：caliber_type (BOOK/ASSESS), currency

### profit_formula（利润计算公式）
- name, code, expression, caliber_type, version

### cost_rule（成本分摊规则）
- cost_type, dimension, factor, ratio

### indicator_library（指标库）
- code, name, category, calc_formula, data_source, supported_dims

### indicator_pre_calc（指标预计算结果）
- indicator_code, calc_period, period_value, dim_type, dim_code, calc_value

### alert_rule / alert_record（预警规则和记录）
- alert_type, metric_code, threshold, level

### customer_master（客户主数据）
- customer_code, customer_name, customer_type, industry, region, credit_rating

### custom_report_template（自定义报表模板）
- name, row_dims, col_metrics, filter_config, sort_config

## 数据库连接
- Host: localhost:3306
- Database: multi_profit
- Username: mpuser
- Password: <DB_PASSWORD>
