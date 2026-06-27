-- 指标汇总表
CREATE TABLE IF NOT EXISTS indicator_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    period VARCHAR(7) NOT NULL COMMENT '账期月份（2026-01）',
    indicator_code VARCHAR(50) NOT NULL COMMENT '指标编码',
    indicator_type VARCHAR(20) NOT NULL COMMENT '指标类型：ATOMIC/DERIVED',
    business_line VARCHAR(20) NOT NULL COMMENT '业务条线：ASSET/LIABILITY',
    calc_value DECIMAL(18,4) COMMENT '汇总值',
    calc_time DATETIME COMMENT '计算时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    INDEX idx_period (period),
    INDEX idx_indicator (indicator_code),
    INDEX idx_business_line (business_line)
) COMMENT='指标汇总表';
