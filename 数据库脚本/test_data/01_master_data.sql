-- =====================================================
-- 主数据维度测试数据
-- 生成时间: 2026-06-26
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 机构维度 (3级: 总行-分行-支行)
-- ----------------------------
TRUNCATE TABLE dim_org;
INSERT INTO dim_org (id, code, name, parent_id, level, status, sort_order) VALUES
-- 总行
(1, 'HQ', '总行', 0, 1, 1, 1),
-- 分行
(2, 'BJ', '北京分行', 1, 2, 1, 1),
(3, 'SH', '上海分行', 1, 2, 1, 2),
(4, 'GZ', '广州分行', 1, 2, 1, 3),
(5, 'SZ', '深圳分行', 1, 2, 1, 4),
-- 支行-北京
(6, 'BJ01', '朝阳支行', 2, 3, 1, 1),
(7, 'BJ02', '海淀支行', 2, 3, 1, 2),
(8, 'BJ03', '西城支行', 2, 3, 1, 3),
-- 支行-上海
(9, 'SH01', '浦东支行', 3, 3, 1, 1),
(10, 'SH02', '静安支行', 3, 3, 1, 2),
-- 支行-广州
(11, 'GZ01', '天河支行', 4, 3, 1, 1),
(12, 'GZ02', '越秀支行', 4, 3, 1, 2),
-- 支行-深圳
(13, 'SZ01', '南山支行', 5, 3, 1, 1),
(14, 'SZ02', '福田支行', 5, 3, 1, 2);

-- ----------------------------
-- 2. 条线维度
-- ----------------------------
TRUNCATE TABLE dim_biz_line;
INSERT INTO dim_biz_line (id, code, name, parent_id, level, status, sort_order) VALUES
(1, 'RETAIL', '零售条线', 0, 1, 1, 1),
(2, 'CORP', '对公条线', 0, 1, 1, 2),
(3, 'SME', '小微条线', 0, 1, 1, 3),
(4, 'TREASURY', '金融市场条线', 0, 1, 1, 4);

-- ----------------------------
-- 3. 部门维度
-- ----------------------------
TRUNCATE TABLE dim_dept;
INSERT INTO dim_dept (id, code, name, parent_id, level, status, sort_order) VALUES
(1, 'FRONT', '前台部门', 0, 1, 1, 1),
(2, 'MID', '中台部门', 0, 1, 1, 2),
(3, 'BACK', '后台部门', 0, 1, 1, 3),
(4, 'SALES', '销售部', 1, 2, 1, 1),
(5, 'MARKETING', '市场部', 1, 2, 1, 2),
(6, 'RISK', '风控部', 2, 2, 1, 1),
(7, 'FINANCE', '财务部', 2, 2, 1, 2),
(8, 'IT', '科技部', 3, 2, 1, 1),
(9, 'HR', '人力部', 3, 2, 1, 2),
(10, 'ADMIN', '行政部', 3, 2, 1, 3);

-- ----------------------------
-- 4. 产品维度 (含线上/线下标识)
-- ----------------------------
TRUNCATE TABLE dim_product;
INSERT INTO dim_product (id, code, name, parent_id, level, biz_type, channel_type, status, sort_order) VALUES
-- 产品大类
(1, 'LOAN', '贷款产品', 0, 1, 'ASSET', 'ALL', 1, 1),
(2, 'DEPOSIT', '存款产品', 0, 1, 'LIAB', 'ALL', 1, 2),
(3, 'WEALTH', '理财产品', 0, 1, 'OFF_BALANCE', 'ALL', 1, 3),
(4, 'FEE', '手续费产品', 0, 1, 'INCOME', 'ALL', 1, 4),
-- 贷款子类
(5, 'LOAN_PNL', '个人经营贷', 1, 2, 'ASSET', 'OFFLINE', 1, 1),
(6, 'LOAN_PCL', '个人消费贷', 1, 2, 'ASSET', 'ONLINE', 1, 2),
(7, 'LOAN_MORTGAGE', '住房按揭贷', 1, 2, 'ASSET', 'OFFLINE', 1, 3),
(8, 'LOAN_CORP', '对公流动资金贷', 1, 2, 'ASSET', 'OFFLINE', 1, 4),
(9, 'LOAN_SME', '小微普惠贷', 1, 2, 'ASSET', 'ONLINE', 1, 5),
(10, 'LOAN_AUTO', '汽车消费贷', 1, 2, 'ASSET', 'ONLINE', 1, 6),
-- 存款子类
(11, 'DEPOSIT_SA', '活期存款', 2, 2, 'LIAB', 'ONLINE', 1, 1),
(12, 'DEPOSIT_TD', '定期存款', 2, 2, 'LIAB', 'OFFLINE', 1, 2),
(13, 'DEPOSIT_STRUCTURED', '结构性存款', 2, 2, 'LIAB', 'OFFLINE', 1, 3),
-- 理财子类
(14, 'WEALTH_WM', '银行理财', 3, 2, 'OFF_BALANCE', 'ONLINE', 1, 1),
(15, 'WEALTH_FUND', '基金代销', 3, 2, 'OFF_BALANCE', 'ONLINE', 1, 2),
(16, 'WEALTH_INSURANCE', '保险代销', 3, 2, 'OFF_BALANCE', 'OFFLINE', 1, 3),
-- 手续费子类
(17, 'FEE_SETTLE', '结算手续费', 4, 2, 'INCOME', 'ONLINE', 1, 1),
(18, 'FEE_GUARANTEE', '保函手续费', 4, 2, 'INCOME', 'OFFLINE', 1, 2),
(19, 'FEE_TRADE', '贸易融资手续费', 4, 2, 'INCOME', 'OFFLINE', 1, 3);

-- ----------------------------
-- 5. 渠道维度 (线上/线下)
-- ----------------------------
TRUNCATE TABLE dim_channel;
INSERT INTO dim_channel (id, code, name, parent_id, level, channel_type, status, sort_order) VALUES
(1, 'ONLINE', '线上渠道', 0, 1, 'ONLINE', 1, 1),
(2, 'OFFLINE', '线下渠道', 0, 1, 'OFFLINE', 1, 2),
(3, 'APP', '手机银行', 1, 2, 'ONLINE', 1, 1),
(4, 'WEB', '网上银行', 1, 2, 'ONLINE', 1, 2),
(5, 'MINI_PROGRAM', '小程序', 1, 2, 'ONLINE', 1, 3),
(6, 'COUNTER', '柜台', 2, 2, 'OFFLINE', 1, 1),
(7, 'CLIENT_MANAGER', '客户经理', 2, 2, 'OFFLINE', 1, 2),
(8, 'AGENT', '代理渠道', 2, 2, 'OFFLINE', 1, 3);

-- ----------------------------
-- 6. 客户经理维度
-- ----------------------------
TRUNCATE TABLE dim_manager;
INSERT INTO dim_manager (id, code, name, org_id, dept_id, biz_line_id, level, status, sort_order) VALUES
-- 北京分行客户经理
(1, 'MGR_BJ01_001', '张三', 6, 4, 1, 1, 1, 1),
(2, 'MGR_BJ01_002', '李四', 6, 4, 2, 1, 1, 2),
(3, 'MGR_BJ02_001', '王五', 7, 4, 1, 1, 1, 1),
(4, 'MGR_BJ02_002', '赵六', 7, 4, 3, 1, 1, 2),
-- 上海分行客户经理
(5, 'MGR_SH01_001', '钱七', 9, 4, 1, 1, 1, 1),
(6, 'MGR_SH01_002', '孙八', 9, 4, 2, 1, 1, 2),
(7, 'MGR_SH02_001', '周九', 10, 4, 1, 1, 1, 1),
-- 广州分行客户经理
(8, 'MGR_GZ01_001', '吴十', 11, 4, 1, 1, 1, 1),
(9, 'MGR_GZ01_002', '郑十一', 11, 4, 3, 1, 1, 2),
-- 深圳分行客户经理
(10, 'MGR_SZ01_001', '冯十二', 13, 4, 1, 1, 1, 1),
(11, 'MGR_SZ01_002', '陈十三', 13, 4, 2, 1, 1, 2),
(12, 'MGR_SZ02_001', '褚十四', 14, 4, 1, 1, 1, 1);

-- ----------------------------
-- 7. 客户维度
-- ----------------------------
TRUNCATE TABLE dim_customer;
INSERT INTO dim_customer (id, code, name, customer_type, org_id, manager_id, status, sort_order) VALUES
-- 个人客户
(1, 'CUST_001', '王建国', 'PERSONAL', 6, 1, 1, 1),
(2, 'CUST_002', '李秀英', 'PERSONAL', 6, 1, 1, 2),
(3, 'CUST_003', '张明华', 'PERSONAL', 7, 3, 1, 1),
(4, 'CUST_004', '刘洋', 'PERSONAL', 9, 5, 1, 1),
(5, 'CUST_005', '陈静', 'PERSONAL', 10, 7, 1, 1),
(6, 'CUST_006', '杨勇', 'PERSONAL', 11, 8, 1, 1),
(7, 'CUST_007', '黄丽', 'PERSONAL', 13, 10, 1, 1),
-- 企业客户
(8, 'CUST_CORP_001', '北京科技有限公司', 'CORP', 6, 2, 1, 1),
(9, 'CUST_CORP_002', '上海贸易有限公司', 'CORP', 9, 6, 1, 1),
(10, 'CUST_CORP_003', '广州制造有限公司', 'CORP', 11, 9, 1, 1),
(11, 'CUST_CORP_004', '深圳创新科技公司', 'CORP', 13, 11, 1, 1),
(12, 'CUST_SME_001', '个体工商户-王老板', 'SME', 7, 4, 1, 1),
(13, 'CUST_SME_002', '小微企业-李记餐饮', 'SME', 14, 12, 1, 1);

SET FOREIGN_KEY_CHECKS = 1;

SELECT '主数据维度测试数据插入完成' AS result;
