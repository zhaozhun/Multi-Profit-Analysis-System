-- ============================================
-- 主数据3级结构完整版（含产品类型）
-- ============================================

USE multi_profit;

-- 清空原有主数据
DELETE FROM dimension_master;
DELETE FROM customer_master;

-- ============================================
-- 部门主数据（7个部门 × 2级 × 2级 = 约40条）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
-- 1级：部门
('DEPT01', '科创金融部', 'DEPT', 0, 1, 1, NULL),
('DEPT02', '产业链金融部', 'DEPT', 0, 1, 2, NULL),
('DEPT03', '消费金融部', 'DEPT', 0, 1, 3, NULL),
('DEPT04', '普惠金融部', 'DEPT', 0, 1, 4, NULL),
('DEPT05', '财富金融部', 'DEPT', 0, 1, 5, NULL),
('DEPT06', '支付金融部', 'DEPT', 0, 1, 6, NULL),
('DEPT07', '金融市场部', 'DEPT', 0, 1, 7, NULL),

-- 2级：处室
('DEPT0101', '科技创新处', 'DEPT', 1, 2, 1, NULL),
('DEPT0102', '投贷联动处', 'DEPT', 1, 2, 2, NULL),
('DEPT0201', '供应链金融处', 'DEPT', 2, 2, 1, NULL),
('DEPT0202', '产业链平台处', 'DEPT', 2, 2, 2, NULL),
('DEPT0301', '信用卡中心', 'DEPT', 3, 2, 1, NULL),
('DEPT0302', '消费信贷处', 'DEPT', 3, 2, 2, NULL),
('DEPT0401', '小微企业处', 'DEPT', 4, 2, 1, NULL),
('DEPT0402', '三农金融处', 'DEPT', 4, 2, 2, NULL),
('DEPT0501', '私人银行处', 'DEPT', 5, 2, 1, NULL),
('DEPT0502', '理财业务处', 'DEPT', 5, 2, 2, NULL),
('DEPT0601', '线上支付处', 'DEPT', 6, 2, 1, NULL),
('DEPT0602', '线下支付处', 'DEPT', 6, 2, 2, NULL),
('DEPT0701', '同业业务处', 'DEPT', 7, 2, 1, NULL),
('DEPT0702', '投资交易处', 'DEPT', 7, 2, 2, NULL),

-- 3级：组
('DEPT010101', '高新企业组', 'DEPT', 8, 3, 1, NULL),
('DEPT010102', '专精特新组', 'DEPT', 8, 3, 2, NULL),
('DEPT010201', '投贷联动组', 'DEPT', 9, 3, 1, NULL),
('DEPT020101', '核心企业组', 'DEPT', 10, 3, 1, NULL),
('DEPT020102', '上下游企业组', 'DEPT', 10, 3, 2, NULL),
('DEPT020201', '平台运营组', 'DEPT', 11, 3, 1, NULL),
('DEPT030101', '发卡组', 'DEPT', 12, 3, 1, NULL),
('DEPT030102', '收单组', 'DEPT', 12, 3, 2, NULL),
('DEPT030201', '消费贷组', 'DEPT', 13, 3, 1, NULL),
('DEPT040101', '首贷组', 'DEPT', 14, 3, 1, NULL),
('DEPT040102', '续贷组', 'DEPT', 14, 3, 2, NULL),
('DEPT040201', '三农组', 'DEPT', 15, 3, 1, NULL),
('DEPT050101', '高净值组', 'DEPT', 16, 3, 1, NULL),
('DEPT050102', '家族信托组', 'DEPT', 16, 3, 2, NULL),
('DEPT050201', '理财销售组', 'DEPT', 17, 3, 1, NULL),
('DEPT060101', '移动支付组', 'DEPT', 18, 3, 1, NULL),
('DEPT060102', '网银支付组', 'DEPT', 18, 3, 2, NULL),
('DEPT060201', 'POS业务组', 'DEPT', 19, 3, 1, NULL),
('DEPT070101', '同业拆借组', 'DEPT', 20, 3, 1, NULL),
('DEPT070102', '同业存单组', 'DEPT', 20, 3, 2, NULL),
('DEPT070201', '债券投资组', 'DEPT', 21, 3, 1, NULL),
('DEPT070202', '基金投资组', 'DEPT', 21, 3, 2, NULL);

-- ============================================
-- 机构主数据（3级）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
('ORG001', '总行', 'ORG', 0, 1, 1, NULL),
('ORG00101', '北京分行', 'ORG', 22, 2, 1, NULL),
('ORG00102', '上海分行', 'ORG', 22, 2, 2, NULL),
('ORG00103', '深圳分行', 'ORG', 22, 2, 3, NULL),
('ORG00104', '广州分行', 'ORG', 22, 2, 4, NULL),
('ORG00105', '杭州分行', 'ORG', 22, 2, 5, NULL),
('ORG0010101', '朝阳支行', 'ORG', 23, 3, 1, NULL),
('ORG0010102', '海淀支行', 'ORG', 23, 3, 2, NULL),
('ORG0010103', '西城支行', 'ORG', 23, 3, 3, NULL),
('ORG0010201', '浦东支行', 'ORG', 24, 3, 1, NULL),
('ORG0010202', '浦西支行', 'ORG', 24, 3, 2, NULL),
('ORG0010301', '南山支行', 'ORG', 25, 3, 1, NULL),
('ORG0010302', '福田支行', 'ORG', 25, 3, 2, NULL),
('ORG0010401', '天河支行', 'ORG', 26, 3, 1, NULL),
('ORG0010501', '西湖支行', 'ORG', 27, 3, 1, NULL);

-- ============================================
-- 产品主数据（3级结构，含产品类型）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
-- 1级：产品大类
('PROD01', '公司贷款', 'PRODUCT', 0, 1, 1, 'LOAN'),
('PROD02', '个人贷款', 'PRODUCT', 0, 1, 2, 'LOAN'),
('PROD03', '公司存款', 'PRODUCT', 0, 1, 3, 'DEPOSIT'),
('PROD04', '个人存款', 'PRODUCT', 0, 1, 4, 'DEPOSIT'),
('PROD05', '理财产品', 'PRODUCT', 0, 1, 5, 'DEPOSIT'),
('PROD06', '国际业务', 'PRODUCT', 0, 1, 6, 'LOAN'),

-- 2级：产品中类
('PROD0101', '短期贷款', 'PRODUCT', 38, 2, 1, 'LOAN'),
('PROD0102', '中长期贷款', 'PRODUCT', 38, 2, 2, 'LOAN'),
('PROD0201', '住房贷款', 'PRODUCT', 39, 2, 1, 'LOAN'),
('PROD0202', '消费贷款', 'PRODUCT', 39, 2, 2, 'LOAN'),
('PROD0301', '活期存款', 'PRODUCT', 40, 2, 1, 'DEPOSIT'),
('PROD0302', '定期存款', 'PRODUCT', 40, 2, 2, 'DEPOSIT'),
('PROD0401', '活期储蓄', 'PRODUCT', 41, 2, 1, 'DEPOSIT'),
('PROD0402', '定期储蓄', 'PRODUCT', 41, 2, 2, 'DEPOSIT'),
('PROD0501', '净值型理财', 'PRODUCT', 42, 2, 1, 'DEPOSIT'),
('PROD0502', '结构性存款', 'PRODUCT', 42, 2, 2, 'DEPOSIT'),
('PROD0601', '国际结算', 'PRODUCT', 43, 2, 1, 'LOAN'),
('PROD0602', '贸易融资', 'PRODUCT', 43, 2, 2, 'LOAN'),

-- 3级：产品小类
('PROD010101', '流动资金贷款', 'PRODUCT', 44, 3, 1, 'LOAN'),
('PROD010102', '银承汇票', 'PRODUCT', 44, 3, 2, 'LOAN'),
('PROD010201', '固定资产贷款', 'PRODUCT', 45, 3, 1, 'LOAN'),
('PROD010202', '项目贷款', 'PRODUCT', 45, 3, 2, 'LOAN'),
('PROD020101', '首套房贷款', 'PRODUCT', 46, 3, 1, 'LOAN'),
('PROD020102', '二套房贷款', 'PRODUCT', 46, 3, 2, 'LOAN'),
('PROD020201', '个人消费贷款', 'PRODUCT', 47, 3, 1, 'LOAN'),
('PROD020202', '个人经营贷款', 'PRODUCT', 47, 3, 2, 'LOAN'),
('PROD030101', '协定存款', 'PRODUCT', 48, 3, 1, 'DEPOSIT'),
('PROD030102', '通知存款', 'PRODUCT', 48, 3, 2, 'DEPOSIT'),
('PROD030201', '整存整取', 'PRODUCT', 49, 3, 1, 'DEPOSIT'),
('PROD030202', '大额存单', 'PRODUCT', 49, 3, 2, 'DEPOSIT'),
('PROD040101', '借记卡存款', 'PRODUCT', 50, 3, 1, 'DEPOSIT'),
('PROD040102', '活期储蓄', 'PRODUCT', 50, 3, 2, 'DEPOSIT'),
('PROD040201', '零存整取', 'PRODUCT', 51, 3, 1, 'DEPOSIT'),
('PROD040202', '整存零取', 'PRODUCT', 51, 3, 2, 'DEPOSIT');

-- ============================================
-- 条线主数据（3级结构）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
-- 1级
('BL01', '对公条线', 'BIZ_LINE', 0, 1, 1, NULL),
('BL02', '零售条线', 'BIZ_LINE', 0, 1, 2, NULL),
('BL03', '金融市场条线', 'BIZ_LINE', 0, 1, 3, NULL),

-- 2级
('BL0101', '大客户部', 'BIZ_LINE', 66, 2, 1, NULL),
('BL0102', '中小企业部', 'BIZ_LINE', 66, 2, 2, NULL),
('BL0201', '个人信贷部', 'BIZ_LINE', 67, 2, 1, NULL),
('BL0202', '个人负债部', 'BIZ_LINE', 67, 2, 2, NULL),
('BL0301', '同业部', 'BIZ_LINE', 68, 2, 1, NULL),
('BL0302', '投资部', 'BIZ_LINE', 68, 2, 2, NULL),

-- 3级
('BL010101', '央国企组', 'BIZ_LINE', 69, 3, 1, NULL),
('BL010102', '上市公司组', 'BIZ_LINE', 69, 3, 2, NULL),
('BL010201', '制造业组', 'BIZ_LINE', 70, 3, 1, NULL),
('BL010202', '服务业组', 'BIZ_LINE', 70, 3, 2, NULL),
('BL020101', '房贷组', 'BIZ_LINE', 71, 3, 1, NULL),
('BL020102', '消费贷组', 'BIZ_LINE', 71, 3, 2, NULL),
('BL020201', '储蓄组', 'BIZ_LINE', 72, 3, 1, NULL),
('BL020202', '理财组', 'BIZ_LINE', 72, 3, 2, NULL),
('BL030101', '银行组', 'BIZ_LINE', 73, 3, 1, NULL),
('BL030102', '非银组', 'BIZ_LINE', 73, 3, 2, NULL),
('BL030201', '债券组', 'BIZ_LINE', 74, 3, 1, NULL),
('BL030202', '基金组', 'BIZ_LINE', 74, 3, 2, NULL);

-- ============================================
-- 渠道主数据（3级结构）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
-- 1级
('CH01', '线下渠道', 'CHANNEL', 0, 1, 1, NULL),
('CH02', '线上渠道', 'CHANNEL', 0, 1, 2, NULL),

-- 2级
('CH0101', '网点渠道', 'CHANNEL', 82, 2, 1, NULL),
('CH0102', '外拓渠道', 'CHANNEL', 82, 2, 2, NULL),
('CH0201', '手机银行', 'CHANNEL', 83, 2, 1, NULL),
('CH0202', '网上银行', 'CHANNEL', 83, 2, 2, NULL),

-- 3级
('CH010101', '自助银行', 'CHANNEL', 84, 3, 1, NULL),
('CH010102', '智能柜台', 'CHANNEL', 84, 3, 2, NULL),
('CH010201', '客户经理外拓', 'CHANNEL', 85, 3, 1, NULL),
('CH010202', '社区银行', 'CHANNEL', 85, 3, 2, NULL),
('CH020101', 'APP渠道', 'CHANNEL', 86, 3, 1, NULL),
('CH020102', '小程序渠道', 'CHANNEL', 86, 3, 2, NULL),
('CH020201', '个人网银', 'CHANNEL', 87, 3, 1, NULL),
('CH020202', '企业网银', 'CHANNEL', 87, 3, 2, NULL);

-- ============================================
-- 客户经理主数据（3级：分行→支行→个人）
-- ============================================
INSERT INTO dimension_master (code, name, dim_type, parent_id, level, sort_order, product_type) VALUES
-- 1级：按分行分组
('MGRP01', '北京分行客户经理', 'MANAGER', 0, 1, 1, NULL),
('MGRP02', '上海分行客户经理', 'MANAGER', 0, 1, 2, NULL),
('MGRP03', '深圳分行客户经理', 'MANAGER', 0, 1, 3, NULL),
('MGRP04', '广州分行客户经理', 'MANAGER', 0, 1, 4, NULL),
('MGRP05', '杭州分行客户经理', 'MANAGER', 0, 1, 5, NULL),

-- 2级：按支行分组
('MGRP0101', '朝阳支行客户经理', 'MANAGER', 96, 2, 1, NULL),
('MGRP0102', '海淀支行客户经理', 'MANAGER', 96, 2, 2, NULL),
('MGRP0103', '西城支行客户经理', 'MANAGER', 96, 2, 3, NULL),
('MGRP0201', '浦东支行客户经理', 'MANAGER', 97, 2, 1, NULL),
('MGRP0202', '浦西支行客户经理', 'MANAGER', 97, 2, 2, NULL),
('MGRP0301', '南山支行客户经理', 'MANAGER', 98, 2, 1, NULL),
('MGRP0302', '福田支行客户经理', 'MANAGER', 98, 2, 2, NULL),
('MGRP0401', '天河支行客户经理', 'MANAGER', 99, 2, 1, NULL),
('MGRP0501', '西湖支行客户经理', 'MANAGER', 100, 2, 1, NULL),

-- 3级：个人
('MGR001', '张明', 'MANAGER', 101, 3, 1, NULL),
('MGR002', '周伟', 'MANAGER', 101, 3, 2, NULL),
('MGR003', '李华', 'MANAGER', 102, 3, 1, NULL),
('MGR004', '赵强', 'MANAGER', 102, 3, 2, NULL),
('MGR005', '郑磊', 'MANAGER', 103, 3, 1, NULL),
('MGR006', '王芳', 'MANAGER', 104, 3, 1, NULL),
('MGR007', '陈静', 'MANAGER', 104, 3, 2, NULL),
('MGR008', '吴敏', 'MANAGER', 105, 3, 1, NULL),
('MGR009', '孙丽', 'MANAGER', 106, 3, 1, NULL),
('MGR010', '刘洋', 'MANAGER', 107, 3, 1, NULL),
('MGR011', '徐磊', 'MANAGER', 108, 3, 1, NULL),
('MGR012', '马超', 'MANAGER', 109, 3, 1, NULL);

-- 验证
SELECT dim_type, level, count(*) as cnt
FROM dimension_master
GROUP BY dim_type, level
ORDER BY dim_type, level;

-- 验证产品类型
SELECT product_type, count(*) as cnt
FROM dimension_master
WHERE dim_type = 'PRODUCT'
GROUP BY product_type;
