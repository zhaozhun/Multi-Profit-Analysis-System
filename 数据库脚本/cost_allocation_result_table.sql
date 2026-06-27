-- 费用分摊结果表
CREATE TABLE IF NOT EXISTS cost_allocation_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(7) NOT NULL COMMENT '账期月份',
    cost_type VARCHAR(50) NOT NULL COMMENT '费用类型',
    source_dim_type VARCHAR(20) NOT NULL COMMENT '来源维度类型',
    source_dim_code VARCHAR(50) NOT NULL COMMENT '来源维度编码',
    target_account_id VARCHAR(30) NOT NULL COMMENT '目标账户ID',
    allocated_amount DECIMAL(18,4) COMMENT '分摊金额',
    allocation_rule_id BIGINT COMMENT '分摊规则ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_period (period),
    INDEX idx_cost_type (cost_type),
    INDEX idx_target_account (target_account_id)
) COMMENT='费用分摊结果表';
