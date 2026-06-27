-- =====================================================
-- 可配置费用分摊系统 - 数据库表结构
-- 创建日期: 2026-06-26
-- =====================================================

USE multi_profit;

-- 临时禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------
-- 1. 成本类型配置表
-- -----------------------------------------------------
DROP TABLE IF EXISTS cost_type_config;
CREATE TABLE cost_type_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    cost_type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '成本类型编码',
    cost_type_name VARCHAR(100) NOT NULL COMMENT '成本类型名称',
    parent_code VARCHAR(50) COMMENT '父级编码(支持层级)',
    level INT DEFAULT 1 COMMENT '层级',
    description VARCHAR(500) COMMENT '描述',
    default_algorithm VARCHAR(50) COMMENT '默认分摊算法',
    default_factor VARCHAR(50) COMMENT '默认分摊因子',
    accounting_code VARCHAR(50) COMMENT '会计科目编码',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_code (parent_code),
    INDEX idx_level (level),
    INDEX idx_status (status)
) COMMENT '成本类型配置表';

-- -----------------------------------------------------
-- 2. 分摊因子配置表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_factor_config;
CREATE TABLE allocation_factor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    factor_code VARCHAR(50) NOT NULL UNIQUE COMMENT '因子编码',
    factor_name VARCHAR(100) NOT NULL COMMENT '因子名称',
    factor_type VARCHAR(50) NOT NULL COMMENT '因子类型(VOLUME/REVENUE/HEADCOUNT/AREA/ASSET/CUSTOM)',
    data_source VARCHAR(200) COMMENT '数据来源(表名.字段名或SQL)',
    calc_formula VARCHAR(500) COMMENT '计算公式',
    description VARCHAR(500) COMMENT '因子描述',
    applicable_cost_types JSON COMMENT '适用的成本类型列表',
    unit VARCHAR(20) COMMENT '单位',
    precision_val INT DEFAULT 4 COMMENT '精度(小数位数)',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_factor_type (factor_type),
    INDEX idx_status (status)
) COMMENT '分摊因子配置表';

-- -----------------------------------------------------
-- 3. 分摊算法配置表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_algorithm_config;
CREATE TABLE allocation_algorithm_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    algorithm_code VARCHAR(50) NOT NULL UNIQUE COMMENT '算法编码',
    algorithm_name VARCHAR(100) NOT NULL COMMENT '算法名称',
    algorithm_type VARCHAR(50) NOT NULL COMMENT '算法类型(RATIO/WEIGHTED/STEP/DIRECT/FORMULA)',
    description VARCHAR(500) COMMENT '算法描述',
    implementation_class VARCHAR(200) COMMENT '实现类名(用于插件化)',
    param_definition JSON COMMENT '参数定义(JSON Schema)',
    formula_template VARCHAR(1000) COMMENT '公式模板',
    is_builtin TINYINT(1) DEFAULT 1 COMMENT '是否内置算法',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_algorithm_type (algorithm_type),
    INDEX idx_status (status)
) COMMENT '分摊算法配置表';

-- -----------------------------------------------------
-- 4. 分摊规则配置表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_rule_config;
CREATE TABLE allocation_rule_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_code VARCHAR(50) NOT NULL UNIQUE COMMENT '规则编码',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    cost_type VARCHAR(50) NOT NULL COMMENT '成本类型',
    description VARCHAR(500) COMMENT '规则描述',
    priority INT DEFAULT 100 COMMENT '优先级(数值越小优先级越高)',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '来源维度类型',
    source_dim_code VARCHAR(50) COMMENT '来源维度编码(为空表示全部)',
    target_dim_type VARCHAR(20) NOT NULL COMMENT '目标维度类型',
    target_dim_filter VARCHAR(500) COMMENT '目标维度过滤条件',
    algorithm_code VARCHAR(50) NOT NULL COMMENT '算法编码',
    algorithm_params JSON COMMENT '算法参数(JSON格式)',
    period_type VARCHAR(20) DEFAULT 'MONTHLY' COMMENT '分摊周期类型(MONTHLY/QUARTERLY/YEARLY/ON_DEMAND)',
    auto_execute TINYINT(1) DEFAULT 1 COMMENT '是否自动执行',
    effective_date DATE COMMENT '生效日期',
    expire_date DATE COMMENT '失效日期',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/INACTIVE/DRAFT)',
    version INT DEFAULT 1 COMMENT '版本号',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(50) COMMENT '更新人',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_cost_type (cost_type),
    INDEX idx_status (status),
    INDEX idx_effective_date (effective_date, expire_date),
    INDEX idx_algorithm_code (algorithm_code),
    FOREIGN KEY (cost_type) REFERENCES cost_type_config(cost_type_code),
    FOREIGN KEY (algorithm_code) REFERENCES allocation_algorithm_config(algorithm_code)
) COMMENT '分摊规则配置表';

-- -----------------------------------------------------
-- 5. 分摊因子权重配置表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_factor_weight;
CREATE TABLE allocation_factor_weight (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    factor_code VARCHAR(50) NOT NULL COMMENT '因子编码',
    weight DECIMAL(5,4) NOT NULL COMMENT '权重(0-1)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_rule_factor (rule_id, factor_code),
    FOREIGN KEY (rule_id) REFERENCES allocation_rule_config(id) ON DELETE CASCADE,
    FOREIGN KEY (factor_code) REFERENCES allocation_factor_config(factor_code)
) COMMENT '分摊因子权重配置表';

-- -----------------------------------------------------
-- 6. 分摊批次表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_batch;
CREATE TABLE allocation_batch (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_no VARCHAR(50) NOT NULL UNIQUE COMMENT '批次号',
    period VARCHAR(10) NOT NULL COMMENT '期间(YYYY-MM)',
    cost_type VARCHAR(50) COMMENT '成本类型(为空表示全部)',
    total_amount DECIMAL(18,2) COMMENT '待分摊总金额',
    allocated_amount DECIMAL(18,2) COMMENT '已分摊金额',
    record_count INT COMMENT '分摊记录数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSING/COMPLETED/FAILED)',
    start_time TIMESTAMP COMMENT '开始时间',
    end_time TIMESTAMP COMMENT '结束时间',
    error_message VARCHAR(1000) COMMENT '错误信息',
    trigger_type VARCHAR(20) COMMENT '触发类型(MANUAL/AUTO/SCHEDULED)',
    triggered_by VARCHAR(50) COMMENT '触发人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_period (period),
    INDEX idx_status (status),
    INDEX idx_cost_type (cost_type)
) COMMENT '分摊批次表';

-- -----------------------------------------------------
-- 7. 分摊明细表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_detail;
CREATE TABLE allocation_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT NOT NULL COMMENT '批次ID',
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '来源维度类型',
    source_dim_code VARCHAR(50) NOT NULL COMMENT '来源维度编码',
    target_dim_type VARCHAR(20) NOT NULL COMMENT '目标维度类型',
    target_dim_code VARCHAR(50) NOT NULL COMMENT '目标维度编码',
    original_amount DECIMAL(18,2) NOT NULL COMMENT '原始金额',
    allocated_amount DECIMAL(18,2) NOT NULL COMMENT '分摊金额',
    allocation_ratio DECIMAL(12,8) NOT NULL COMMENT '分摊比例',
    factor_values JSON COMMENT '因子值(多因子时为JSON)',
    algorithm_code VARCHAR(50) COMMENT '使用的算法编码',
    calc_details JSON COMMENT '计算详情(用于审计)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (batch_id) REFERENCES allocation_batch(id),
    FOREIGN KEY (rule_id) REFERENCES allocation_rule_config(id),
    INDEX idx_period (period),
    INDEX idx_source (source_dim_type, source_dim_code),
    INDEX idx_target (target_dim_type, target_dim_code),
    INDEX idx_batch_id (batch_id)
) COMMENT '分摊明细表';

-- -----------------------------------------------------
-- 8. 因子快照表
-- -----------------------------------------------------
DROP TABLE IF EXISTS allocation_factor_snapshot;
CREATE TABLE allocation_factor_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    batch_id BIGINT COMMENT '批次ID',
    period VARCHAR(10) NOT NULL COMMENT '期间',
    factor_code VARCHAR(50) NOT NULL COMMENT '因子编码',
    dim_type VARCHAR(20) NOT NULL COMMENT '维度类型',
    dim_code VARCHAR(50) NOT NULL COMMENT '维度编码',
    factor_value DECIMAL(18,4) NOT NULL COMMENT '因子值',
    factor_ratio DECIMAL(12,8) COMMENT '因子占比',
    data_source VARCHAR(200) COMMENT '数据来源',
    snapshot_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
    INDEX idx_period_factor (period, factor_code),
    INDEX idx_dim (dim_type, dim_code),
    INDEX idx_batch_id (batch_id)
) COMMENT '因子快照表';

-- -----------------------------------------------------
-- 9. 扩展 biz_ledger 表（使用存储过程检查列是否存在）
-- -----------------------------------------------------
DELIMITER //

CREATE PROCEDURE IF NOT EXISTS add_column_if_not_exists(
    IN p_table_name VARCHAR(100),
    IN p_column_name VARCHAR(100),
    IN p_column_definition VARCHAR(500)
)
BEGIN
    DECLARE column_count INT;
    SELECT COUNT(*) INTO column_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = p_table_name
    AND COLUMN_NAME = p_column_name;

    IF column_count = 0 THEN
        SET @sql = CONCAT('ALTER TABLE ', p_table_name, ' ADD COLUMN ', p_column_name, ' ', p_column_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

-- 调用存储过程添加列
CALL add_column_if_not_exists('biz_ledger', 'allocated_cost', 'DECIMAL(18,2) DEFAULT 0 COMMENT ''分摊成本''');
CALL add_column_if_not_exists('biz_ledger', 'profit_before_allocation', 'DECIMAL(18,2) COMMENT ''分摊前利润''');
CALL add_column_if_not_exists('biz_ledger', 'profit_after_allocation', 'DECIMAL(18,2) COMMENT ''分摊后利润''');
CALL add_column_if_not_exists('biz_ledger', 'last_allocation_batch', 'VARCHAR(50) COMMENT ''最近分摊批次号''');
CALL add_column_if_not_exists('biz_ledger', 'last_allocation_time', 'TIMESTAMP COMMENT ''最近分摊时间''');

-- 删除存储过程
DROP PROCEDURE IF EXISTS add_column_if_not_exists;

-- =====================================================
-- 初始化数据
-- =====================================================

-- 初始化成本类型
INSERT INTO cost_type_config (cost_type_code, cost_type_name, level, description, default_algorithm, default_factor) VALUES
('RENT', '房租物业', 1, '办公场地租金、物业管理费', 'RATIO', 'AREA'),
('SALARY', '人力成本', 1, '员工工资、社保、福利', 'WEIGHTED', 'HEADCOUNT'),
('IT', '信息技术', 1, 'IT系统、设备、维护费用', 'RATIO', 'HEADCOUNT'),
('MARKETING', '营销费用', 1, '广告、推广、活动费用', 'RATIO', 'VOLUME'),
('ADMIN', '行政费用', 1, '办公用品、差旅、招待费', 'RATIO', 'HEADCOUNT'),
('RISK', '风险准备', 1, '风险拨备、减值准备', 'DIRECT', NULL),
('CUSTOM', '自定义', 1, '其他自定义成本类型', 'RATIO', NULL);

-- 初始化分摊因子
INSERT INTO allocation_factor_config (factor_code, factor_name, factor_type, data_source, calc_formula, description, applicable_cost_types, unit) VALUES
('VOLUME', '业务量', 'VOLUME', 'biz_ledger.biz_amount', 'SUM(biz_amount) / TOTAL(biz_amount)', '按业务交易量占比分摊', '["MARKETING","ADMIN"]', '笔'),
('REVENUE', '收入', 'REVENUE', 'biz_ledger.revenue', 'SUM(revenue) / TOTAL(revenue)', '按收入金额占比分摊', '["RENT","SALARY","IT","MARKETING","ADMIN"]', '元'),
('HEADCOUNT', '人员数量', 'HEADCOUNT', 'hr_employee.count', 'dept_count / total_count', '按部门人数占比分摊', '["SALARY","IT","ADMIN"]', '人'),
('AREA', '办公面积', 'AREA', 'office_area.area', 'dept_area / total_area', '按办公面积占比分摊', '["RENT"]', '平方米'),
('ASSET', '资产规模', 'ASSET', 'asset_ledger.balance', 'SUM(balance) / TOTAL(balance)', '按资产规模占比分摊', '["RENT","SALARY","IT"]', '元');

-- 初始化分摊算法
INSERT INTO allocation_algorithm_config (algorithm_code, algorithm_name, algorithm_type, description, is_builtin) VALUES
('RATIO', '比例分摊', 'RATIO', '按单一因子占比分摊，公式：分摊额 = 总额 × (因子值 / 因子总和)', 1),
('WEIGHTED', '加权分摊', 'WEIGHTED', '按多因子加权分摊，公式：分摊额 = 总额 × Σ(因子值 × 权重)', 1),
('STEP', '阶梯分摊', 'STEP', '按层级逐级分摊，适用于层级组织结构', 1),
('DIRECT', '直接归属', 'DIRECT', '直接计入目标维度，无需计算分摊比例', 1),
('FORMULA', '公式分摊', 'FORMULA', '自定义公式分摊，支持复杂业务逻辑', 1);

-- 初始化示例分摊规则
INSERT INTO allocation_rule_config (rule_code, rule_name, cost_type, description, priority, source_dim_type, source_dim_code, target_dim_type, target_dim_filter, algorithm_code, algorithm_params, period_type, auto_execute, effective_date, expire_date, status) VALUES
('RENT_TO_ORG', '房租分摊到机构', 'RENT', '将总公司房租按面积分摊到各分支机构', 10, 'COMPANY', NULL, 'ORG', 'level = 1', 'RATIO', '{"factor_code": "AREA"}', 'MONTHLY', 1, '2026-01-01', '2099-12-31', 'ACTIVE'),
('SALARY_TO_DEPT', '人力成本分摊到部门', 'SALARY', '将人力成本按人数和收入加权分摊到各部门', 20, 'COMPANY', NULL, 'DEPT', 'level <= 2', 'WEIGHTED', '{"factors": [{"code": "HEADCOUNT", "weight": 0.7}, {"code": "REVENUE", "weight": 0.3}]}', 'MONTHLY', 1, '2026-01-01', '2099-12-31', 'ACTIVE'),
('IT_TO_DEPT', 'IT成本分摊到部门', 'IT', '将IT成本按人数分摊到各部门', 30, 'COMPANY', NULL, 'DEPT', NULL, 'RATIO', '{"factor_code": "HEADCOUNT"}', 'MONTHLY', 1, '2026-01-01', '2099-12-31', 'ACTIVE'),
('MARKETING_TO_PRODUCT', '营销费用分摊到产品', 'MARKETING', '将营销费用按业务量分摊到各产品线', 40, 'COMPANY', NULL, 'PRODUCT', NULL, 'RATIO', '{"factor_code": "VOLUME"}', 'MONTHLY', 1, '2026-01-01', '2099-12-31', 'ACTIVE');

-- =====================================================
-- 创建视图：分摊结果汇总
-- =====================================================
CREATE OR REPLACE VIEW v_allocation_summary AS
SELECT
    ab.period,
    ab.cost_type,
    ctc.cost_type_name,
    ab.total_amount,
    ab.allocated_amount,
    ab.record_count,
    ab.status,
    ab.trigger_type,
    ab.created_at
FROM allocation_batch ab
LEFT JOIN cost_type_config ctc ON ab.cost_type = ctc.cost_type_code
ORDER BY ab.period DESC, ab.cost_type;

-- =====================================================
-- 创建视图：分摊明细查询
-- =====================================================
CREATE OR REPLACE VIEW v_allocation_detail AS
SELECT
    ad.id,
    ad.period,
    ad.batch_id,
    arc.rule_code,
    arc.rule_name,
    arc.cost_type,
    ad.source_dim_type,
    ad.source_dim_code,
    ad.target_dim_type,
    ad.target_dim_code,
    ad.original_amount,
    ad.allocated_amount,
    ad.allocation_ratio,
    ad.algorithm_code,
    ad.created_at
FROM allocation_detail ad
JOIN allocation_rule_config arc ON ad.rule_id = arc.id
ORDER BY ad.period DESC, ad.allocated_amount DESC;

-- 重新启用外键检查
SET FOREIGN_KEY_CHECKS = 1;
