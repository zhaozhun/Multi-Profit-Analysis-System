-- =====================================================
-- 维度数据测试数据 (使用dimension_master表)
-- 生成时间: 2026-06-26
-- =====================================================

-- 清空现有数据
DELETE FROM dimension_master WHERE dim_type IN ('ORG', 'BIZ_LINE', 'DEPT', 'PRODUCT', 'CHANNEL', 'MANAGER', 'CUSTOMER');

-- ----------------------------
-- 1. 机构维度 (3级: 总行-分行-支行)
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, status) VALUES
-- 总行
('HQ', '总行', 'ORG', 0, 1, 1, 1),
-- 分行
('BJ', '北京分行', 'ORG', 1, 2, 1, 1),
('SH', '上海分行', 'ORG', 1, 2, 2, 1),
('GZ', '广州分行', 'ORG', 1, 2, 3, 1),
('SZ', '深圳分行', 'ORG', 1, 2, 4, 1),
-- 支行-北京
('BJ01', '朝阳支行', 'ORG', 2, 3, 1, 1),
('BJ02', '海淀支行', 'ORG', 2, 3, 2, 1),
('BJ03', '西城支行', 'ORG', 2, 3, 3, 1),
-- 支行-上海
('SH01', '浦东支行', 'ORG', 3, 3, 1, 1),
('SH02', '静安支行', 'ORG', 3, 3, 2, 1),
-- 支行-广州
('GZ01', '天河支行', 'ORG', 4, 3, 1, 1),
('GZ02', '越秀支行', 'ORG', 4, 3, 2, 1),
-- 支行-深圳
('SZ01', '南山支行', 'ORG', 5, 3, 1, 1),
('SZ02', '福田支行', 'ORG', 5, 3, 2, 1);

-- ----------------------------
-- 2. 条线维度
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, status) VALUES
('RETAIL', '零售条线', 'BIZ_LINE', 0, 1, 1, 1),
('CORP', '对公条线', 'BIZ_LINE', 0, 1, 2, 1),
('SME', '小微条线', 'BIZ_LINE', 0, 1, 3, 1),
('TREASURY', '金融市场条线', 'BIZ_LINE', 0, 1, 4, 1);

-- ----------------------------
-- 3. 部门维度
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, status) VALUES
('FRONT', '前台部门', 'DEPT', 0, 1, 1, 1),
('MID', '中台部门', 'DEPT', 0, 1, 2, 1),
('BACK', '后台部门', 'DEPT', 0, 1, 3, 1),
('SALES', '销售部', 'DEPT', 15, 2, 1, 1),
('MARKETING', '市场部', 'DEPT', 15, 2, 2, 1),
('RISK', '风控部', 'DEPT', 16, 2, 1, 1),
('FINANCE', '财务部', 'DEPT', 16, 2, 2, 1),
('IT', '科技部', 'DEPT', 17, 2, 1, 1),
('HR', '人力部', 'DEPT', 17, 2, 2, 1),
('ADMIN', '行政部', 'DEPT', 17, 2, 3, 1);

-- ----------------------------
-- 4. 产品维度 (含线上/线下标识)
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type, status) VALUES
-- 产品大类
('LOAN', '贷款产品', 'PRODUCT', 0, 1, 1, 'ASSET', 1),
('DEPOSIT', '存款产品', 'PRODUCT', 0, 1, 2, 'LIAB', 1),
('WEALTH', '理财产品', 'PRODUCT', 0, 1, 3, 'OFF_BALANCE', 1),
('FEE', '手续费产品', 'PRODUCT', 0, 1, 4, 'INCOME', 1),
-- 贷款子类
('LOAN_PNL', '个人经营贷', 'PRODUCT', 25, 2, 1, 'ASSET', 1),
('LOAN_PCL', '个人消费贷', 'PRODUCT', 25, 2, 2, 'ASSET', 1),
('LOAN_MORTGAGE', '住房按揭贷', 'PRODUCT', 25, 2, 3, 'ASSET', 1),
('LOAN_CORP', '对公流动资金贷', 'PRODUCT', 25, 2, 4, 'ASSET', 1),
('LOAN_SME', '小微普惠贷', 'PRODUCT', 25, 2, 5, 'ASSET', 1),
('LOAN_AUTO', '汽车消费贷', 'PRODUCT', 25, 2, 6, 'ASSET', 1),
-- 存款子类
('DEPOSIT_SA', '活期存款', 'PRODUCT', 26, 2, 1, 'LIAB', 1),
('DEPOSIT_TD', '定期存款', 'PRODUCT', 26, 2, 2, 'LIAB', 1),
('DEPOSIT_STRUCTURED', '结构性存款', 'PRODUCT', 26, 2, 3, 'LIAB', 1),
-- 理财子类
('WEALTH_WM', '银行理财', 'PRODUCT', 27, 2, 1, 'OFF_BALANCE', 1),
('WEALTH_FUND', '基金代销', 'PRODUCT', 27, 2, 2, 'OFF_BALANCE', 1),
('WEALTH_INSURANCE', '保险代销', 'PRODUCT', 27, 2, 3, 'OFF_BALANCE', 1),
-- 手续费子类
('FEE_SETTLE', '结算手续费', 'PRODUCT', 28, 2, 1, 'INCOME', 1),
('FEE_GUARANTEE', '保函手续费', 'PRODUCT', 28, 2, 2, 'INCOME', 1),
('FEE_TRADE', '贸易融资手续费', 'PRODUCT', 28, 2, 3, 'INCOME', 1);

-- ----------------------------
-- 5. 渠道维度 (线上/线下)
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, status) VALUES
('ONLINE', '线上渠道', 'CHANNEL', 0, 1, 1, 1),
('OFFLINE', '线下渠道', 'CHANNEL', 0, 1, 2, 1),
('APP', '手机银行', 'CHANNEL', 43, 2, 1, 1),
('WEB', '网上银行', 'CHANNEL', 43, 2, 2, 1),
('MINI_PROGRAM', '小程序', 'CHANNEL', 43, 2, 3, 1),
('COUNTER', '柜台', 'CHANNEL', 44, 2, 1, 1),
('CLIENT_MANAGER', '客户经理', 'CHANNEL', 44, 2, 2, 1),
('AGENT', '代理渠道', 'CHANNEL', 44, 2, 3, 1);

-- ----------------------------
-- 6. 客户经理维度
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, ext_attrs, status) VALUES
('MGR_BJ01_001', '张三', 'MANAGER', 0, 1, 1, '{"org_code":"BJ01","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_BJ01_002', '李四', 'MANAGER', 0, 1, 2, '{"org_code":"BJ01","dept_code":"SALES","biz_line_code":"CORP"}', 1),
('MGR_BJ02_001', '王五', 'MANAGER', 0, 1, 3, '{"org_code":"BJ02","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_BJ02_002', '赵六', 'MANAGER', 0, 1, 4, '{"org_code":"BJ02","dept_code":"SALES","biz_line_code":"SME"}', 1),
('MGR_SH01_001', '钱七', 'MANAGER', 0, 1, 5, '{"org_code":"SH01","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_SH01_002', '孙八', 'MANAGER', 0, 1, 6, '{"org_code":"SH01","dept_code":"SALES","biz_line_code":"CORP"}', 1),
('MGR_SH02_001', '周九', 'MANAGER', 0, 1, 7, '{"org_code":"SH02","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_GZ01_001', '吴十', 'MANAGER', 0, 1, 8, '{"org_code":"GZ01","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_GZ01_002', '郑十一', 'MANAGER', 0, 1, 9, '{"org_code":"GZ01","dept_code":"SALES","biz_line_code":"SME"}', 1),
('MGR_SZ01_001', '冯十二', 'MANAGER', 0, 1, 10, '{"org_code":"SZ01","dept_code":"SALES","biz_line_code":"RETAIL"}', 1),
('MGR_SZ01_002', '陈十三', 'MANAGER', 0, 1, 11, '{"org_code":"SZ01","dept_code":"SALES","biz_line_code":"CORP"}', 1),
('MGR_SZ02_001', '褚十四', 'MANAGER', 0, 1, 12, '{"org_code":"SZ02","dept_code":"SALES","biz_line_code":"RETAIL"}', 1);

-- ----------------------------
-- 7. 客户维度
-- ----------------------------
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, ext_attrs, status) VALUES
('CUST_001', '王建国', 'CUSTOMER', 0, 1, 1, '{"type":"PERSONAL","org_code":"BJ01","manager_code":"MGR_BJ01_001"}', 1),
('CUST_002', '李秀英', 'CUSTOMER', 0, 1, 2, '{"type":"PERSONAL","org_code":"BJ01","manager_code":"MGR_BJ01_001"}', 1),
('CUST_003', '张明华', 'CUSTOMER', 0, 1, 3, '{"type":"PERSONAL","org_code":"BJ02","manager_code":"MGR_BJ02_001"}', 1),
('CUST_004', '刘洋', 'CUSTOMER', 0, 1, 4, '{"type":"PERSONAL","org_code":"SH01","manager_code":"MGR_SH01_001"}', 1),
('CUST_005', '陈静', 'CUSTOMER', 0, 1, 5, '{"type":"PERSONAL","org_code":"SH02","manager_code":"MGR_SH02_001"}', 1),
('CUST_006', '杨勇', 'CUSTOMER', 0, 1, 6, '{"type":"PERSONAL","org_code":"GZ01","manager_code":"MGR_GZ01_001"}', 1),
('CUST_007', '黄丽', 'CUSTOMER', 0, 1, 7, '{"type":"PERSONAL","org_code":"SZ01","manager_code":"MGR_SZ01_001"}', 1),
('CUST_CORP_001', '北京科技有限公司', 'CUSTOMER', 0, 1, 8, '{"type":"CORP","org_code":"BJ01","manager_code":"MGR_BJ01_002"}', 1),
('CUST_CORP_002', '上海贸易有限公司', 'CUSTOMER', 0, 1, 9, '{"type":"CORP","org_code":"SH01","manager_code":"MGR_SH01_002"}', 1),
('CUST_CORP_003', '广州制造有限公司', 'CUSTOMER', 0, 1, 10, '{"type":"CORP","org_code":"GZ01","manager_code":"MGR_GZ01_002"}', 1),
('CUST_CORP_004', '深圳创新科技公司', 'CUSTOMER', 0, 1, 11, '{"type":"CORP","org_code":"SZ01","manager_code":"MGR_SZ01_002"}', 1),
('CUST_SME_001', '个体工商户-王老板', 'CUSTOMER', 0, 1, 12, '{"type":"SME","org_code":"BJ02","manager_code":"MGR_BJ02_002"}', 1),
('CUST_SME_002', '小微企业-李记餐饮', 'CUSTOMER', 0, 1, 13, '{"type":"SME","org_code":"SZ02","manager_code":"MGR_SZ02_001"}', 1);

SELECT '维度数据插入完成' AS result;
