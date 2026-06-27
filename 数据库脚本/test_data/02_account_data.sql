-- =====================================================
-- 账户数据测试数据 (从数据湖获取)
-- 包含: 账号、客户、产品、客户经理、渠道类型
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 账户主表 (含渠道类型标识)
-- ----------------------------
TRUNCATE TABLE biz_account;
INSERT INTO biz_account (id, account_no, customer_id, product_id, manager_id, org_id, channel_type, channel_id, open_date, status) VALUES
-- 线下贷款账户 (按客户经理规模分摊)
(1, 'LOAN_2025_001', 1, 5, 1, 6, 'OFFLINE', 7, '2025-01-15', 'ACTIVE'),
(2, 'LOAN_2025_002', 2, 7, 1, 6, 'OFFLINE', 7, '2025-02-20', 'ACTIVE'),
(3, 'LOAN_2025_003', 3, 5, 3, 7, 'OFFLINE', 7, '2025-03-10', 'ACTIVE'),
(4, 'LOAN_2025_004', 4, 7, 5, 9, 'OFFLINE', 7, '2025-04-05', 'ACTIVE'),
(5, 'LOAN_2025_005', 5, 5, 7, 10, 'OFFLINE', 7, '2025-05-12', 'ACTIVE'),
(6, 'LOAN_2025_006', 6, 7, 8, 11, 'OFFLINE', 7, '2025-06-18', 'ACTIVE'),
(7, 'LOAN_2025_007', 7, 5, 10, 13, 'OFFLINE', 7, '2025-07-22', 'ACTIVE'),
-- 对公贷款
(8, 'LOAN_CORP_001', 8, 8, 2, 6, 'OFFLINE', 7, '2025-01-20', 'ACTIVE'),
(9, 'LOAN_CORP_002', 9, 8, 6, 9, 'OFFLINE', 7, '2025-03-15', 'ACTIVE'),
(10, 'LOAN_CORP_003', 10, 8, 9, 11, 'OFFLINE', 7, '2025-05-10', 'ACTIVE'),
(11, 'LOAN_CORP_004', 11, 8, 11, 13, 'OFFLINE', 7, '2025-07-01', 'ACTIVE'),
-- 小微贷款
(12, 'LOAN_SME_001', 12, 9, 4, 7, 'OFFLINE', 7, '2025-02-28', 'ACTIVE'),
(13, 'LOAN_SME_002', 13, 9, 12, 14, 'OFFLINE', 7, '2025-04-15', 'ACTIVE'),

-- 线上贷款账户 (按产品分摊规则分摊)
(14, 'LOAN_ONLINE_001', 1, 6, 1, 6, 'ONLINE', 3, '2025-01-10', 'ACTIVE'),
(15, 'LOAN_ONLINE_002', 2, 6, 1, 6, 'ONLINE', 3, '2025-02-15', 'ACTIVE'),
(16, 'LOAN_ONLINE_003', 3, 10, 3, 7, 'ONLINE', 4, '2025-03-20', 'ACTIVE'),
(17, 'LOAN_ONLINE_004', 4, 9, 5, 9, 'ONLINE', 5, '2025-04-25', 'ACTIVE'),
(18, 'LOAN_ONLINE_005', 5, 6, 7, 10, 'ONLINE', 3, '2025-05-30', 'ACTIVE'),
(19, 'LOAN_ONLINE_006', 6, 10, 8, 11, 'ONLINE', 4, '2025-06-05', 'ACTIVE'),
(20, 'LOAN_ONLINE_007', 7, 9, 10, 13, 'ONLINE', 5, '2025-07-10', 'ACTIVE'),

-- 存款账户
(21, 'DEP_SA_001', 1, 11, 1, 6, 'ONLINE', 3, '2025-01-01', 'ACTIVE'),
(22, 'DEP_SA_002', 2, 11, 1, 6, 'ONLINE', 3, '2025-01-01', 'ACTIVE'),
(23, 'DEP_SA_003', 3, 11, 3, 7, 'ONLINE', 4, '2025-01-01', 'ACTIVE'),
(24, 'DEP_TD_001', 4, 12, 5, 9, 'OFFLINE', 6, '2025-01-15', 'ACTIVE'),
(25, 'DEP_TD_002', 5, 12, 7, 10, 'OFFLINE', 6, '2025-02-01', 'ACTIVE'),
(26, 'DEP_TD_003', 8, 13, 2, 6, 'OFFLINE', 6, '2025-01-10', 'ACTIVE'),
(27, 'DEP_TD_004', 9, 13, 6, 9, 'OFFLINE', 6, '2025-02-15', 'ACTIVE'),

-- 理财账户 (线上)
(28, 'WM_001', 1, 14, 1, 6, 'ONLINE', 3, '2025-01-05', 'ACTIVE'),
(29, 'WM_002', 2, 14, 1, 6, 'ONLINE', 3, '2025-02-10', 'ACTIVE'),
(30, 'WM_003', 3, 15, 3, 7, 'ONLINE', 4, '2025-03-15', 'ACTIVE'),
(31, 'WM_004', 4, 15, 5, 9, 'ONLINE', 5, '2025-04-20', 'ACTIVE'),
(32, 'WM_005', 8, 16, 2, 6, 'OFFLINE', 7, '2025-05-01', 'ACTIVE'),
(33, 'WM_006', 9, 16, 6, 9, 'OFFLINE', 7, '2025-06-01', 'ACTIVE'),

-- 手续费账户
(34, 'FEE_001', 8, 17, 2, 6, 'ONLINE', 4, '2025-01-01', 'ACTIVE'),
(35, 'FEE_002', 9, 17, 6, 9, 'ONLINE', 4, '2025-01-01', 'ACTIVE'),
(36, 'FEE_003', 10, 18, 9, 11, 'OFFLINE', 6, '2025-02-01', 'ACTIVE'),
(37, 'FEE_004', 11, 19, 11, 13, 'OFFLINE', 6, '2025-03-01', 'ACTIVE');

SET FOREIGN_KEY_CHECKS = 1;

SELECT '账户数据插入完成' AS result;
