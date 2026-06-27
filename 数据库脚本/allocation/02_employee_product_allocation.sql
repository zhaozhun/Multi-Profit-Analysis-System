-- =====================================================
-- 补充表结构：员工维度分摊 + 产品分润
-- 创建日期: 2026-06-26
-- =====================================================

USE multi_profit;

-- -----------------------------------------------------
-- 1. 员工主数据表
-- -----------------------------------------------------
DROP TABLE IF EXISTS employee_master;
CREATE TABLE employee_master (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    employee_code VARCHAR(50) NOT NULL UNIQUE COMMENT '员工编码',
    employee_name VARCHAR(100) NOT NULL COMMENT '员工姓名',
    org_code VARCHAR(50) COMMENT '所属机构编码',
    org_name VARCHAR(100) COMMENT '所属机构名称',
    dept_code VARCHAR(50) COMMENT '所属部门编码',
    dept_name VARCHAR(100) COMMENT '所属部门名称',
    position VARCHAR(100) COMMENT '职位',
    job_level VARCHAR(20) COMMENT '职级',
    entry_date DATE COMMENT '入职日期',
    salary DECIMAL(12,2) COMMENT '月薪',
    work_hours_per_month DECIMAL(6,2) DEFAULT 176 COMMENT '月标准工时',
    workstation_area DECIMAL(8,2) COMMENT '工位面积(平方米)',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_org (org_code),
    INDEX idx_dept (dept_code),
    INDEX idx_status (status)
) COMMENT '员工主数据表';

-- -----------------------------------------------------
-- 2. 员工工时记录表（用于按工时分摊）
-- -----------------------------------------------------
DROP TABLE IF EXISTS employee_work_hours;
CREATE TABLE employee_work_hours (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    employee_code VARCHAR(50) NOT NULL COMMENT '员工编码',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    work_days DECIMAL(4,1) COMMENT '出勤天数',
    work_hours DECIMAL(8,2) COMMENT '实际工时',
    overtime_hours DECIMAL(8,2) DEFAULT 0 COMMENT '加班工时',
    total_hours DECIMAL(8,2) COMMENT '总工时(含加班)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_employee_period (employee_code, period),
    INDEX idx_period (period)
) COMMENT '员工工时记录表';

-- -----------------------------------------------------
-- 3. 产品主数据表
-- -----------------------------------------------------
DROP TABLE IF EXISTS product_master;
CREATE TABLE product_master (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    product_code VARCHAR(50) NOT NULL UNIQUE COMMENT '产品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    product_type VARCHAR(50) COMMENT '产品类型(存款/贷款/理财/中间业务)',
    parent_code VARCHAR(50) COMMENT '父级产品编码',
    level INT DEFAULT 1 COMMENT '层级',
    product_manager VARCHAR(50) COMMENT '产品经理',
    profit_center VARCHAR(50) COMMENT '利润中心',
    product_level VARCHAR(20) COMMENT '产品等级(A/B/C/D)',
    commission_rate DECIMAL(6,4) COMMENT '分润费率',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_type (product_type),
    INDEX idx_parent (parent_code),
    INDEX idx_status (status)
) COMMENT '产品主数据表';

-- -----------------------------------------------------
-- 4. 产品分润规则表
-- -----------------------------------------------------
DROP TABLE IF EXISTS product_commission_rule;
CREATE TABLE product_commission_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    product_type VARCHAR(50) COMMENT '产品类型',
    product_code VARCHAR(50) COMMENT '产品编码(为空表示该类型全部产品)',
    commission_type VARCHAR(50) NOT NULL COMMENT '分润类型(REVENUE_SHARE/PROFIT_SHARE/FIXED_RATE/TIERED)',
    calc_base VARCHAR(50) NOT NULL COMMENT '计算基数(REVENUE/PROFIT/BIZ_AMOUNT/BALANCE)',
    rate DECIMAL(8,6) COMMENT '分润费率',
    min_amount DECIMAL(18,2) COMMENT '最低分润金额',
    max_amount DECIMAL(18,2) COMMENT '最高分润金额',
    tier_config JSON COMMENT '阶梯配置(用于TIERED类型)',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_product_type (product_type),
    INDEX idx_product_code (product_code),
    INDEX idx_status (status)
) COMMENT '产品分润规则表';

-- -----------------------------------------------------
-- 5. 产品分润计算结果表
-- -----------------------------------------------------
DROP TABLE IF EXISTS product_commission_detail;
CREATE TABLE product_commission_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    product_code VARCHAR(50) NOT NULL COMMENT '产品编码',
    product_name VARCHAR(100) COMMENT '产品名称',
    rule_code VARCHAR(50) COMMENT '规则编码',
    calc_base VARCHAR(50) COMMENT '计算基数类型',
    base_amount DECIMAL(18,2) COMMENT '基数金额',
    commission_rate DECIMAL(8,6) COMMENT '分润费率',
    commission_amount DECIMAL(18,2) NOT NULL COMMENT '分润金额',
    receiver_type VARCHAR(50) COMMENT '接收方类型(机构/部门/个人)',
    receiver_code VARCHAR(50) COMMENT '接收方编码',
    receiver_name VARCHAR(100) COMMENT '接收方名称',
    status VARCHAR(20) DEFAULT 'CALCULATED' COMMENT '状态(CALCULATED/CONFIRMED/PAID)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_product (product_code),
    INDEX idx_batch (batch_no),
    INDEX idx_receiver (receiver_type, receiver_code)
) COMMENT '产品分润计算结果表';

-- -----------------------------------------------------
-- 6. 员工费用分摊结果表
-- -----------------------------------------------------
DROP TABLE IF EXISTS employee_cost_allocation;
CREATE TABLE employee_cost_allocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    employee_code VARCHAR(50) NOT NULL COMMENT '员工编码',
    employee_name VARCHAR(100) COMMENT '员工姓名',
    org_code VARCHAR(50) COMMENT '所属机构',
    dept_code VARCHAR(50) COMMENT '所属部门',
    cost_type VARCHAR(50) NOT NULL COMMENT '成本类型',
    cost_type_name VARCHAR(100) COMMENT '成本类型名称',
    original_amount DECIMAL(18,2) NOT NULL COMMENT '原始金额(该员工分摊前)',
    allocated_amount DECIMAL(18,2) NOT NULL COMMENT '分摊金额',
    allocation_factor VARCHAR(50) COMMENT '分摊因子',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '因子占比',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_employee (employee_code),
    INDEX idx_org_dept (org_code, dept_code),
    INDEX idx_cost_type (cost_type),
    INDEX idx_batch (batch_no)
) COMMENT '员工费用分摊结果表';

-- -----------------------------------------------------
-- 7. 扩展 allocation_factor_config 表，增加员工和产品相关因子
-- -----------------------------------------------------
INSERT IGNORE INTO allocation_factor_config (factor_code, factor_name, factor_type, data_source, calc_formula, description, applicable_cost_types, unit) VALUES
('EMPLOYEE_COUNT', '员工人数', 'HEADCOUNT', 'employee_master.count', 'dept_employee_count / total_employee_count', '按部门员工人数占比分摊', '["RENT","IT","ADMIN","WELFARE"]', '人'),
('WORK_HOURS', '工时', 'WORK_HOURS', 'employee_work_hours.total_hours', 'employee_hours / total_hours', '按员工实际工时占比分摊', '["OPERATION","SALARY","WELFARE"]', '小时'),
('SALARY', '薪资', 'SALARY', 'employee_master.salary', 'employee_salary / total_salary', '按员工薪资占比分摊', '["WELFARE","INSURANCE","TRAINING"]', '元'),
('WORKSTATION_AREA', '工位面积', 'AREA', 'employee_master.workstation_area', 'employee_area / total_area', '按员工工位面积占比分摊', '["RENT","PROPERTY","UTILITIES"]', '平方米'),
('PRODUCT_REVENUE', '产品收入', 'REVENUE', 'biz_ledger.revenue', 'product_revenue / total_revenue', '按产品收入占比计算分润', '["COMMISSION","CHANNEL_FEE"]', '元'),
('PRODUCT_PROFIT', '产品利润', 'PROFIT', 'biz_ledger.net_profit', 'product_profit / total_profit', '按产品利润占比计算分润', '["COMMISSION","BONUS"]', '元'),
('PRODUCT_BIZ_AMOUNT', '产品业务量', 'VOLUME', 'biz_ledger.biz_amount', 'product_amount / total_amount', '按产品业务量占比计算分润', '["COMMISSION","MARKETING"]', '笔');

-- -----------------------------------------------------
-- 8. 初始化产品分润规则示例
-- -----------------------------------------------------
INSERT INTO product_commission_rule (rule_code, rule_name, product_type, product_code, commission_type, calc_base, rate, status) VALUES
('LOAN_COMMISSION', '贷款产品分润', 'LOAN', NULL, 'REVENUE_SHARE', 'REVENUE', 0.15, 'ACTIVE'),
('DEPOSIT_COMMISSION', '存款产品分润', 'DEPOSIT', NULL, 'PROFIT_SHARE', 'PROFIT', 0.10, 'ACTIVE'),
('WEALTH_COMMISSION', '理财产品分润', 'WEALTH', NULL, 'REVENUE_SHARE', 'REVENUE', 0.20, 'ACTIVE'),
('FEE_COMMISSION', '中间业务分润', 'FEE', NULL, 'REVENUE_SHARE', 'REVENUE', 0.25, 'ACTIVE');

-- -----------------------------------------------------
-- 9. 初始化员工示例数据
-- -----------------------------------------------------
INSERT INTO employee_master (employee_code, employee_name, org_code, org_name, dept_code, dept_name, position, job_level, entry_date, salary, workstation_area, status) VALUES
('EMP001', '张三', 'ORG001', '总行', 'DEPT001', '公司业务部', '客户经理', 'P6', '2020-01-15', 15000.00, 6.0, 'ACTIVE'),
('EMP002', '李四', 'ORG001', '总行', 'DEPT001', '公司业务部', '高级客户经理', 'P7', '2018-06-20', 20000.00, 8.0, 'ACTIVE'),
('EMP003', '王五', 'ORG001', '总行', 'DEPT002', '个人业务部', '客户经理', 'P6', '2021-03-10', 14000.00, 6.0, 'ACTIVE'),
('EMP004', '赵六', 'ORG002', '华东分行', 'DEPT003', '风险管理部', '风险经理', 'P7', '2019-08-01', 18000.00, 7.0, 'ACTIVE'),
('EMP005', '钱七', 'ORG002', '华东分行', 'DEPT004', '运营部', '运营主管', 'P8', '2017-05-15', 25000.00, 10.0, 'ACTIVE');

-- -----------------------------------------------------
-- 10. 初始化员工工时示例数据
-- -----------------------------------------------------
INSERT INTO employee_work_hours (employee_code, period, work_days, work_hours, overtime_hours, total_hours) VALUES
('EMP001', '2026-05', 22.0, 176.0, 10.0, 186.0),
('EMP002', '2026-05', 22.0, 176.0, 15.0, 191.0),
('EMP003', '2026-05', 21.5, 172.0, 5.0, 177.0),
('EMP004', '2026-05', 22.0, 176.0, 8.0, 184.0),
('EMP005', '2026-05', 22.0, 176.0, 20.0, 196.0);

-- =====================================================
-- 创建视图：员工费用分摊汇总
-- =====================================================
CREATE OR REPLACE VIEW v_employee_cost_summary AS
SELECT
    eca.period,
    eca.employee_code,
    eca.employee_name,
    eca.org_code,
    eca.dept_code,
    SUM(eca.allocated_amount) AS total_allocated,
    GROUP_CONCAT(DISTINCT eca.cost_type) AS cost_types
FROM employee_cost_allocation eca
LEFT JOIN employee_master em ON eca.employee_code = em.employee_code
GROUP BY eca.period, eca.employee_code, eca.employee_name, eca.org_code, eca.dept_code;

-- =====================================================
-- 创建视图：产品分润汇总
-- =====================================================
CREATE OR REPLACE VIEW v_product_commission_summary AS
SELECT
    pcd.period,
    pcd.product_code,
    pcd.product_name,
    pm.product_type,
    SUM(pcd.commission_amount) AS total_commission,
    SUM(pcd.base_amount) AS total_base_amount,
    GROUP_CONCAT(DISTINCT pcd.rule_code) AS rules
FROM product_commission_detail pcd
LEFT JOIN product_master pm ON pcd.product_code = pm.product_code
GROUP BY pcd.period, pcd.product_code, pcd.product_name, pm.product_type;
