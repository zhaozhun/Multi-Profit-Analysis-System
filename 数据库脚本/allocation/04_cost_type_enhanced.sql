-- =====================================================
-- 费用类型完善设计（支持层级结构 + 灵活配置）
-- 创建日期: 2026-06-26
-- =====================================================

USE multi_profit;

-- -----------------------------------------------------
-- 1. 费用类型主表（支持3级层级）
-- -----------------------------------------------------
DROP TABLE IF EXISTS cost_type_master;
CREATE TABLE cost_type_master (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cost_code VARCHAR(50) NOT NULL UNIQUE COMMENT '费用编码',
    cost_name VARCHAR(100) NOT NULL COMMENT '费用名称',
    parent_code VARCHAR(50) COMMENT '父级编码(支持3级层级)',
    level INT DEFAULT 1 COMMENT '层级(1-大类,2-中类,3-小类)',
    cost_category VARCHAR(50) NOT NULL COMMENT '费用性质(FIXED-固定/VARIABLE-变动/DIRECT-直接)',
    cost_nature VARCHAR(50) NOT NULL COMMENT '费用归属(OPERATION-运营/MANAGEMENT-管理/SALES-销售/FINANCE-财务)',
    allocation_required TINYINT(1) DEFAULT 1 COMMENT '是否需要分摊(0-否,1-是)',
    allocation_method VARCHAR(50) COMMENT '默认分摊方法',
    allocation_factor VARCHAR(100) COMMENT '默认分摊因子配置(JSON)',
    accounting_code VARCHAR(50) COMMENT '会计科目编码',
    accounting_name VARCHAR(100) COMMENT '会计科目名称',
    budget_control TINYINT(1) DEFAULT 0 COMMENT '是否预算控制(0-否,1-是)',
    description VARCHAR(500) COMMENT '费用描述',
    remark VARCHAR(500) COMMENT '备注',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code),
    INDEX idx_level (level),
    INDEX idx_cost_category (cost_category),
    INDEX idx_cost_nature (cost_nature),
    INDEX idx_status (status)
) COMMENT '费用类型主表';

-- -----------------------------------------------------
-- 2. 费用分摊规则配置表（每种费用独立配置）
-- -----------------------------------------------------
DROP TABLE IF EXISTS cost_allocation_rule_config;
CREATE TABLE cost_allocation_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cost_code VARCHAR(50) NOT NULL COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    allocation_method VARCHAR(50) NOT NULL COMMENT '分摊方法',
    allocation_factor VARCHAR(100) NOT NULL COMMENT '分摊因子',
    factor_source VARCHAR(200) COMMENT '因子数据来源',
    factor_formula VARCHAR(500) COMMENT '因子计算公式',
    target_type VARCHAR(50) NOT NULL COMMENT '分摊目标类型(EMPLOYEE-员工/DEPT-部门/ORG-机构)',
    target_scope VARCHAR(50) DEFAULT 'ALL' COMMENT '分摊范围(ALL-全部/DEPT-指定部门/TEAM-指定团队)',
    target_dept_codes JSON COMMENT '目标部门编码列表',
    weight_config JSON COMMENT '权重配置(多因子时)',
    min_amount DECIMAL(18,2) COMMENT '最低分摊金额',
    max_amount DECIMAL(18,2) COMMENT '最高分摊金额',
    precision_val INT DEFAULT 2 COMMENT '精度(小数位数)',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_cost_code (cost_code),
    INDEX idx_allocation_method (allocation_method),
    INDEX idx_target_type (target_type),
    INDEX idx_status (status)
) COMMENT '费用分摊规则配置表';

-- -----------------------------------------------------
-- 3. 费用实际发生表（录入每月费用数据）
-- -----------------------------------------------------
DROP TABLE IF EXISTS cost_actual_record;
CREATE TABLE cost_actual_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    cost_code VARCHAR(50) NOT NULL COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_type VARCHAR(50) COMMENT '费用类型(大类)',
    amount DECIMAL(18,2) NOT NULL COMMENT '实际金额',
    dept_code VARCHAR(50) COMMENT '归属部门(直接归属时)',
    org_code VARCHAR(50) COMMENT '归属机构',
    vendor VARCHAR(200) COMMENT '供应商',
    invoice_no VARCHAR(100) COMMENT '发票号',
    occurrence_date DATE COMMENT '发生日期',
    description VARCHAR(500) COMMENT '费用说明',
    attachment_count INT DEFAULT 0 COMMENT '附件数量',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING-待分摊/ALLOCATED-已分摊/CONFIRMED-已确认)',
    created_by VARCHAR(50) COMMENT '录入人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_period (period),
    INDEX idx_cost_code (cost_code),
    INDEX idx_cost_type (cost_type),
    INDEX idx_dept_code (dept_code),
    INDEX idx_status (status)
) COMMENT '费用实际发生表';

-- -----------------------------------------------------
-- 4. 费用分摊结果表
-- -----------------------------------------------------
DROP TABLE IF EXISTS cost_allocation_result;
CREATE TABLE cost_allocation_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    cost_code VARCHAR(50) NOT NULL COMMENT '费用编码',
    cost_name VARCHAR(100) COMMENT '费用名称',
    cost_category VARCHAR(50) COMMENT '费用性质',
    original_amount DECIMAL(18,2) NOT NULL COMMENT '原始金额',
    target_type VARCHAR(50) NOT NULL COMMENT '目标类型(EMPLOYEE/DEPT/ORG)',
    target_code VARCHAR(50) NOT NULL COMMENT '目标编码',
    target_name VARCHAR(100) COMMENT '目标名称',
    dept_code VARCHAR(50) COMMENT '所属部门',
    org_code VARCHAR(50) COMMENT '所属机构',
    allocated_amount DECIMAL(18,2) NOT NULL COMMENT '分摊金额',
    allocation_method VARCHAR(50) COMMENT '分摊方法',
    allocation_factor VARCHAR(50) COMMENT '分摊因子',
    factor_value DECIMAL(18,4) COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '因子占比',
    calc_details JSON COMMENT '计算详情',
    status VARCHAR(20) DEFAULT 'CALCULATED' COMMENT '状态(CALCULATED-已计算/CONFIRMED-已确认)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_batch (batch_no),
    INDEX idx_cost_code (cost_code),
    INDEX idx_target (target_type, target_code),
    INDEX idx_dept_org (dept_code, org_code)
) COMMENT '费用分摊结果表';

-- =====================================================
-- 初始化费用类型数据（3级层级结构）
-- =====================================================

-- 一级：费用大类
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('OPERATION', '运营费用', NULL, 1, 'VARIABLE', 'OPERATION', 1, NULL, NULL, '日常运营相关费用', 1),
('MANAGEMENT', '管理费用', NULL, 1, 'FIXED', 'MANAGEMENT', 1, NULL, NULL, '行政管理相关费用', 2),
('SALES', '销售费用', NULL, 1, 'VARIABLE', 'SALES', 1, NULL, NULL, '市场营销相关费用', 3),
('FINANCE', '财务费用', NULL, 1, 'VARIABLE', 'FINANCE', 1, NULL, NULL, '财务相关费用', 4),
('HR', '人力成本', NULL, 1, 'FIXED', 'OPERATION', 1, NULL, NULL, '人力资源相关费用', 5),
('IT', '信息技术', NULL, 1, 'FIXED', 'OPERATION', 1, NULL, NULL, 'IT系统和技术支持费用', 6);

-- 二级：费用中类 - 运营费用
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('RENT', '房租物业', 'OPERATION', 2, 'FIXED', 'OPERATION', 1, 'AREA', '{"factor":"WORKSTATION_AREA","unit":"square_meter"}', '办公场地租金、物业管理费', 1),
('UTILITIES', '水电暖', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"EMPLOYEE_COUNT"}', '水费、电费、暖气费、空调费', 2),
('COLLECTION', '催收费用', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"OVERDUE_LOAN_AMOUNT"}', '逾期贷款催收相关费用', 3),
('DATA_FEE', '数据使用费', 'OPERATION', 2, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '数据查询、征信查询、系统使用费', 4),
('OFFICE_SUPPLIES', '办公用品', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"EMPLOYEE_COUNT"}', '办公文具、耗材、设备', 5),
('TRAVEL', '差旅费用', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '出差交通、住宿、餐饮', 6),
('ENTERTAINMENT', '招待费用', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '客户招待、商务宴请', 7),
('LOGISTICS', '物流费用', 'OPERATION', 2, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"DOCUMENT_COUNT"}', '快递、邮寄、押运', 8);

-- 二级：费用中类 - 管理费用
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('PROPERTY', '物业费用', 'MANAGEMENT', 2, 'FIXED', 'MANAGEMENT', 1, 'AREA', '{"factor":"OFFICE_AREA"}', '物业管理、安保、保洁', 1),
('DEPRECIATION', '折旧费用', 'MANAGEMENT', 2, 'FIXED', 'MANAGEMENT', 1, 'ASSET_VALUE', '{"factor":"ASSET_VALUE"}', '固定资产折旧', 2),
('MAINTENANCE', '维修费用', 'MANAGEMENT', 2, 'VARIABLE', 'MANAGEMENT', 1, 'ASSET_VALUE', '{"factor":"ASSET_VALUE"}', '设备维修、装修维护', 3),
('INSURANCE', '保险费用', 'MANAGEMENT', 2, 'FIXED', 'MANAGEMENT', 1, 'ASSET_VALUE', '{"factor":"ASSET_VALUE"}', '财产保险、责任保险', 4),
('LEGAL', '法务费用', 'MANAGEMENT', 2, 'VARIABLE', 'MANAGEMENT', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '法律顾问、诉讼费用', 5),
('AUDIT', '审计费用', 'MANAGEMENT', 2, 'FIXED', 'MANAGEMENT', 1, 'REVENUE', '{"factor":"TOTAL_REVENUE"}', '审计、评估、咨询', 6);

-- 二级：费用中类 - 销售费用
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('MARKETING', '营销推广', 'SALES', 2, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"BIZ_AMOUNT"}', '广告、推广、活动费用', 1),
('PROMOTION', '促销费用', 'SALES', 2, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"BIZ_AMOUNT"}', '优惠、折扣、赠品', 2),
('COMMISSION', '佣金费用', 'SALES', 2, 'VARIABLE', 'SALES', 1, 'REVENUE', '{"factor":"SALES_REVENUE"}', '销售渠道佣金、代理费', 3),
('EXHIBITION', '展会费用', 'SALES', 2, 'VARIABLE', 'SALES', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '展会参展、会议赞助', 4),
('CUSTOMER_SERVICE', '客服费用', 'SALES', 2, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"CUSTOMER_COUNT"}', '客户服务、投诉处理', 5);

-- 二级：费用中类 - 财务费用
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('INTEREST_EXPENSE', '利息支出', 'FINANCE', 2, 'VARIABLE', 'FINANCE', 0, NULL, NULL, '存款利息、借款利息', 1),
('BANK_FEE', '银行手续费', 'FINANCE', 2, 'VARIABLE', 'FINANCE', 1, 'BIZ_VOLUME', '{"factor":"TRANSACTION_COUNT"}', '转账、汇款、账户管理费', 2),
('EXCHANGE_LOSS', '汇兑损失', 'FINANCE', 2, 'VARIABLE', 'FINANCE', 0, NULL, NULL, '外汇兑换损失', 3),
('BAD_DEBT', '坏账损失', 'FINANCE', 2, 'VARIABLE', 'FINANCE', 0, NULL, NULL, '坏账准备、核销', 4);

-- 二级：费用中类 - 人力成本
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('SALARY', '工资薪金', 'HR', 2, 'FIXED', 'OPERATION', 1, 'SALARY', '{"factor":"EMPLOYEE_SALARY"}', '基本工资、绩效工资、奖金', 1),
('SOCIAL_INSURANCE', '社保公积金', 'HR', 2, 'FIXED', 'OPERATION', 1, 'SALARY', '{"factor":"EMPLOYEE_SALARY"}', '五险一金', 2),
('WELFARE', '福利费用', 'HR', 2, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"EMPLOYEE_COUNT"}', '节日福利、体检、团建', 3),
('TRAINING', '培训费用', 'HR', 2, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"TRAINING_PARTICIPANTS"}', '员工培训、技能提升', 4),
('RECRUITMENT', '招聘费用', 'HR', 2, 'VARIABLE', 'OPERATION', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '招聘渠道、猎头费', 5);

-- 二级：费用中类 - 信息技术
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
('IT_HARDWARE', '硬件设备', 'IT', 2, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"DEVICE_COUNT"}', '电脑、服务器、网络设备', 1),
('IT_SOFTWARE', '软件费用', 'IT', 2, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '软件许可、系统订阅', 2),
('IT_SERVICE', 'IT服务', 'IT', 2, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '运维服务、技术支持', 3),
('IT_SECURITY', '信息安全', 'IT', 2, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '安全防护、等保测评', 4),
('CLOUD_SERVICE', '云服务', 'IT', 2, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '云计算、存储、带宽', 5);

-- 三级：费用小类（示例）
INSERT INTO cost_type_master (cost_code, cost_name, parent_code, level, cost_category, cost_nature, allocation_required, allocation_method, allocation_factor, description, sort_order) VALUES
-- 催收费用细分
('COLLECTION_CALL', '电话催收', 'COLLECTION', 3, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"OVERDUE_LOAN_COUNT"}', '电话催收人工费', 1),
('COLLECTION_VISIT', '外访催收', 'COLLECTION', 3, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"OVERDUE_LOAN_COUNT"}', '上门催收交通、人工费', 2),
('COLLECTION_LEGAL', '法律催收', 'COLLECTION', 3, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"OVERDUE_LOAN_AMOUNT"}', '诉讼、执行费用', 3),
('COLLECTION_OUTSOURCE', '委外催收', 'COLLECTION', 3, 'VARIABLE', 'OPERATION', 1, 'BIZ_VOLUME', '{"factor":"OVERDUE_LOAN_AMOUNT"}', '外包催收服务费', 4),

-- 营销推广细分
('MARKETING_ONLINE', '线上营销', 'MARKETING', 3, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"ONLINE_LEADS"}', '网络广告、SEO、SEM', 1),
('MARKETING_OFFLINE', '线下营销', 'MARKETING', 3, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"OFFLINE_LEADS"}', '地推、传单、户外广告', 2),
('MARKETING_EVENT', '活动营销', 'MARKETING', 3, 'VARIABLE', 'SALES', 1, 'BIZ_VOLUME', '{"factor":"EVENT_PARTICIPANTS"}', '沙龙、讲座、路演', 3),
('MARKETING_MEDIA', '媒体投放', 'MARKETING', 3, 'VARIABLE', 'SALES', 1, 'REVENUE', '{"factor":"SALES_REVENUE"}', '电视、广播、报纸', 4),

-- 培训费用细分
('TRAINING_INTERNAL', '内训费用', 'TRAINING', 3, 'VARIABLE', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"TRAINING_PARTICIPANTS"}', '内部讲师、场地、教材', 1),
('TRAINING_EXTERNAL', '外训费用', 'TRAINING', 3, 'VARIABLE', 'OPERATION', 1, 'DEPT_DIRECT', '{"factor":"DEPT_CODE"}', '外部培训、认证考试', 2),
('TRAINING_ONLINE', '在线学习', 'TRAINING', 3, 'FIXED', 'OPERATION', 1, 'EMPLOYEE_COUNT', '{"factor":"SYSTEM_USER_COUNT"}', '在线课程、学习平台', 3);

-- =====================================================
-- 初始化费用分摊规则配置
-- =====================================================
INSERT INTO cost_allocation_rule_config (cost_code, cost_name, allocation_method, allocation_factor, factor_source, target_type, target_scope, status) VALUES
('RENT', '房租物业', 'AREA', 'WORKSTATION_AREA', 'employee_master.workstation_area', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('UTILITIES', '水电暖', 'EMPLOYEE_COUNT', 'EMPLOYEE_COUNT', 'employee_master.id', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('COLLECTION', '催收费用', 'BIZ_VOLUME', 'OVERDUE_LOAN_AMOUNT', 'biz_ledger.overdue_amount', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('DATA_FEE', '数据使用费', 'EMPLOYEE_COUNT', 'SYSTEM_USER_COUNT', 'system_user.id', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('OFFICE_SUPPLIES', '办公用品', 'EMPLOYEE_COUNT', 'EMPLOYEE_COUNT', 'employee_master.id', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('TRAVEL', '差旅费用', 'DEPT_DIRECT', 'DEPT_CODE', 'cost_actual_record.dept_code', 'EMPLOYEE', 'DEPT', 'ACTIVE'),
('ENTERTAINMENT', '招待费用', 'DEPT_DIRECT', 'DEPT_CODE', 'cost_actual_record.dept_code', 'EMPLOYEE', 'DEPT', 'ACTIVE'),
('PROPERTY', '物业费用', 'AREA', 'OFFICE_AREA', 'office_area.area', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('SALARY', '工资薪金', 'SALARY', 'EMPLOYEE_SALARY', 'employee_master.salary', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('SOCIAL_INSURANCE', '社保公积金', 'SALARY', 'EMPLOYEE_SALARY', 'employee_master.salary', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('WELFARE', '福利费用', 'EMPLOYEE_COUNT', 'EMPLOYEE_COUNT', 'employee_master.id', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('TRAINING', '培训费用', 'EMPLOYEE_COUNT', 'TRAINING_PARTICIPANTS', 'training_record.participants', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('IT_HARDWARE', '硬件设备', 'EMPLOYEE_COUNT', 'DEVICE_COUNT', 'it_asset.device_count', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('IT_SOFTWARE', '软件费用', 'EMPLOYEE_COUNT', 'SYSTEM_USER_COUNT', 'system_user.id', 'EMPLOYEE', 'ALL', 'ACTIVE'),
('MARKETING', '营销推广', 'BIZ_VOLUME', 'BIZ_AMOUNT', 'biz_ledger.biz_amount', 'EMPLOYEE', 'ALL', 'ACTIVE');

-- =====================================================
-- 初始化费用实际发生数据（2026-06月）
-- =====================================================
INSERT INTO cost_actual_record (period, cost_code, cost_name, cost_type, amount, dept_code, status) VALUES
-- 运营费用
('2026-06', 'RENT', '房租物业', 'OPERATION', 150000.00, NULL, 'PENDING'),
('2026-06', 'UTILITIES', '水电暖', 'OPERATION', 28000.00, NULL, 'PENDING'),
('2026-06', 'COLLECTION', '催收费用', 'OPERATION', 18000.00, NULL, 'PENDING'),
('2026-06', 'DATA_FEE', '数据使用费', 'OPERATION', 22000.00, NULL, 'PENDING'),
('2026-06', 'OFFICE_SUPPLIES', '办公用品', 'OPERATION', 8000.00, NULL, 'PENDING'),
('2026-06', 'TRAVEL', '差旅费用', 'OPERATION', 15000.00, NULL, 'PENDING'),
('2026-06', 'ENTERTAINMENT', '招待费用', 'OPERATION', 12000.00, NULL, 'PENDING'),
('2026-06', 'LOGISTICS', '物流费用', 'OPERATION', 5000.00, NULL, 'PENDING'),

-- 管理费用
('2026-06', 'PROPERTY', '物业费用', 'MANAGEMENT', 35000.00, NULL, 'PENDING'),
('2026-06', 'DEPRECIATION', '折旧费用', 'MANAGEMENT', 45000.00, NULL, 'PENDING'),
('2026-06', 'MAINTENANCE', '维修费用', 'MANAGEMENT', 12000.00, NULL, 'PENDING'),
('2026-06', 'INSURANCE', '保险费用', 'MANAGEMENT', 8000.00, NULL, 'PENDING'),
('2026-06', 'LEGAL', '法务费用', 'MANAGEMENT', 15000.00, NULL, 'PENDING'),
('2026-06', 'AUDIT', '审计费用', 'MANAGEMENT', 20000.00, NULL, 'PENDING'),

-- 销售费用
('2026-06', 'MARKETING', '营销推广', 'SALES', 50000.00, NULL, 'PENDING'),
('2026-06', 'PROMOTION', '促销费用', 'SALES', 25000.00, NULL, 'PENDING'),
('2026-06', 'COMMISSION', '佣金费用', 'SALES', 35000.00, NULL, 'PENDING'),
('2026-06', 'EXHIBITION', '展会费用', 'SALES', 18000.00, NULL, 'PENDING'),
('2026-06', 'CUSTOMER_SERVICE', '客服费用', 'SALES', 12000.00, NULL, 'PENDING'),

-- 人力成本
('2026-06', 'SALARY', '工资薪金', 'HR', 500000.00, NULL, 'PENDING'),
('2026-06', 'SOCIAL_INSURANCE', '社保公积金', 'HR', 125000.00, NULL, 'PENDING'),
('2026-06', 'WELFARE', '福利费用', 'HR', 30000.00, NULL, 'PENDING'),
('2026-06', 'TRAINING', '培训费用', 'HR', 15000.00, NULL, 'PENDING'),
('2026-06', 'RECRUITMENT', '招聘费用', 'HR', 10000.00, NULL, 'PENDING'),

-- 信息技术
('2026-06', 'IT_HARDWARE', '硬件设备', 'IT', 25000.00, NULL, 'PENDING'),
('2026-06', 'IT_SOFTWARE', '软件费用', 'IT', 35000.00, NULL, 'PENDING'),
('2026-06', 'IT_SERVICE', 'IT服务', 'IT', 18000.00, NULL, 'PENDING'),
('2026-06', 'IT_SECURITY', '信息安全', 'IT', 12000.00, NULL, 'PENDING'),
('2026-06', 'CLOUD_SERVICE', '云服务', 'IT', 15000.00, NULL, 'PENDING');

-- =====================================================
-- 创建视图：费用类型层级视图
-- =====================================================
CREATE OR REPLACE VIEW v_cost_type_hierarchy AS
SELECT
    l1.cost_code AS level1_code,
    l1.cost_name AS level1_name,
    l2.cost_code AS level2_code,
    l2.cost_name AS level2_name,
    l3.cost_code AS level3_code,
    l3.cost_name AS level3_name,
    COALESCE(l3.cost_category, l2.cost_category, l1.cost_category) AS cost_category,
    COALESCE(l3.cost_nature, l2.cost_nature, l1.cost_nature) AS cost_nature,
    COALESCE(l3.allocation_required, l2.allocation_required, l1.allocation_required) AS allocation_required,
    COALESCE(l3.allocation_method, l2.allocation_method, l1.allocation_method) AS allocation_method
FROM cost_type_master l1
LEFT JOIN cost_type_master l2 ON l2.parent_code = l1.cost_code AND l2.level = 2
LEFT JOIN cost_type_master l3 ON l3.parent_code = l2.cost_code AND l3.level = 3
WHERE l1.level = 1 AND l1.status = 'ACTIVE';

-- =====================================================
-- 创建视图：费用汇总统计
-- =====================================================
CREATE OR REPLACE VIEW v_cost_summary AS
SELECT
    car.period,
    ctm.cost_code,
    ctm.cost_name,
    ctm.level,
    ctm.cost_category,
    ctm.cost_nature,
    car.amount,
    car.status
FROM cost_actual_record car
JOIN cost_type_master ctm ON car.cost_code = ctm.cost_code
WHERE ctm.status = 'ACTIVE'
ORDER BY ctm.sort_order, car.period DESC;
