#!/usr/bin/env python3
"""
多维盈利分析系统 - Mock数据生成器V3（星型模型 + 维度依赖）
支持贷款/存款利润拆分，3级主数据结构，ID外键关联

ID映射说明（基于master-data-3level.sql插入顺序）：
- DEPT: ID 1-49 (7+14+28)
- ORG: ID 50-68 (1+5+9)
- PRODUCT: ID 69-102 (6+18+18)
- BIZ_LINE: ID 103-124 (3+6+12)
- CHANNEL: ID 125-140 (2+4+8)
- MANAGER: ID 141-167 (5+9+12)
"""

import random
import csv
import os
from datetime import datetime, timedelta

# ============================================
# 主数据定义（与dimension_master实际ID对应）
# ============================================

# 机构3级（id, code, name, parent_id）
# ID范围：99-107（基于实际dimension_master表）
ORGS_L3 = [
    (99, 'ORG0010101', '朝阳支行', 94),   # 北京分行
    (100, 'ORG0010102', '海淀支行', 94),  # 北京分行
    (101, 'ORG0010103', '西城支行', 94),  # 北京分行
    (102, 'ORG0010201', '浦东支行', 95),  # 上海分行
    (103, 'ORG0010202', '浦西支行', 95),  # 上海分行
    (104, 'ORG0010301', '南山支行', 96),  # 深圳分行
    (105, 'ORG0010302', '福田支行', 96),  # 深圳分行
    (106, 'ORG0010401', '天河支行', 97),  # 广州分行
    (107, 'ORG0010501', '西湖支行', 98),  # 杭州分行
]

# 产品3级（id, code, name, product_type, 条线id, 部门id）
# ID范围：126-141（基于实际dimension_master表）
PRODUCTS = [
    # 贷款产品 - 公司贷款
    {'id': 126, 'code': 'PROD010101', 'name': '流动资金贷款', 'type': 'LOAN', 'biz_line_id': 145, 'dept_id': 57},
    {'id': 127, 'code': 'PROD010102', 'name': '银承汇票', 'type': 'LOAN', 'biz_line_id': 145, 'dept_id': 57},
    {'id': 128, 'code': 'PROD010201', 'name': '固定资产贷款', 'type': 'LOAN', 'biz_line_id': 146, 'dept_id': 58},
    {'id': 129, 'code': 'PROD010202', 'name': '项目贷款', 'type': 'LOAN', 'biz_line_id': 146, 'dept_id': 59},
    # 贷款产品 - 个人贷款
    {'id': 130, 'code': 'PROD020101', 'name': '首套房贷款', 'type': 'LOAN', 'biz_line_id': 147, 'dept_id': 62},
    {'id': 131, 'code': 'PROD020102', 'name': '二套房贷款', 'type': 'LOAN', 'biz_line_id': 147, 'dept_id': 62},
    {'id': 132, 'code': 'PROD020201', 'name': '个人消费贷款', 'type': 'LOAN', 'biz_line_id': 147, 'dept_id': 62},
    {'id': 133, 'code': 'PROD020202', 'name': '个人经营贷款', 'type': 'LOAN', 'biz_line_id': 146, 'dept_id': 63},
    # 存款产品 - 公司存款
    {'id': 134, 'code': 'PROD030101', 'name': '协定存款', 'type': 'DEPOSIT', 'biz_line_id': 145, 'dept_id': 57},
    {'id': 135, 'code': 'PROD030102', 'name': '通知存款', 'type': 'DEPOSIT', 'biz_line_id': 145, 'dept_id': 57},
    {'id': 136, 'code': 'PROD030201', 'name': '整存整取', 'type': 'DEPOSIT', 'biz_line_id': 146, 'dept_id': 59},
    {'id': 137, 'code': 'PROD030202', 'name': '大额存单', 'type': 'DEPOSIT', 'biz_line_id': 146, 'dept_id': 59},
    # 存款产品 - 个人存款
    {'id': 138, 'code': 'PROD040101', 'name': '借记卡存款', 'type': 'DEPOSIT', 'biz_line_id': 148, 'dept_id': 65},
    {'id': 139, 'code': 'PROD040102', 'name': '活期储蓄', 'type': 'DEPOSIT', 'biz_line_id': 148, 'dept_id': 66},
    {'id': 140, 'code': 'PROD040201', 'name': '零存整取', 'type': 'DEPOSIT', 'biz_line_id': 148, 'dept_id': 66},
    {'id': 141, 'code': 'PROD040202', 'name': '整存零取', 'type': 'DEPOSIT', 'biz_line_id': 148, 'dept_id': 66},
]

# 渠道3级（id, code, name）
# ID范围：169-176（基于实际dimension_master表）
CHANNELS = [
    (169, 'CH010101', '自助银行'),
    (170, 'CH010102', '智能柜台'),
    (171, 'CH010201', '客户经理外拓'),
    (172, 'CH010202', '社区银行'),
    (173, 'CH020101', 'APP渠道'),
    (174, 'CH020102', '小程序渠道'),
    (175, 'CH020201', '个人网银'),
    (176, 'CH020202', '企业网银'),
]

# 客户经理3级（id, code, name, 所属支行org_id, 专长产品大类）
# ID范围：191-202（基于实际dimension_master表）
MANAGERS = [
    (191, 'MGR001', '张明', 99, 'CORP_LOAN'),      # 朝阳支行，专长公司贷款
    (192, 'MGR002', '周伟', 99, 'CORP_DEPOSIT'),    # 朝阳支行，专长公司存款
    (193, 'MGR003', '李华', 100, 'PERSON_LOAN'),    # 海淀支行，专长个人贷款
    (194, 'MGR004', '赵强', 100, 'PERSON_DEPOSIT'), # 海淀支行，专长个人存款
    (195, 'MGR005', '郑磊', 101, 'WEALTH'),         # 西城支行，专长理财
    (196, 'MGR006', '王芳', 102, 'CORP_LOAN'),      # 浦东支行，专长公司贷款
    (197, 'MGR007', '陈静', 102, 'PERSON_LOAN'),    # 浦东支行，专长个人贷款
    (198, 'MGR008', '吴敏', 103, 'INTL'),           # 浦西支行，专长国际业务
    (199, 'MGR009', '孙丽', 104, 'CORP_DEPOSIT'),   # 南山支行，专长公司存款
    (200, 'MGR010', '刘洋', 105, 'PERSON_DEPOSIT'), # 福田支行，专长个人存款
    (201, 'MGR011', '徐磊', 106, 'CORP_LOAN'),      # 天河支行，专长公司贷款
    (202, 'MGR012', '马超', 107, 'WEALTH'),          # 西湖支行，专长理财
]

# 客户
CUSTOMERS_CORP = [
    (1, '华为技术'), (2, '腾讯科技'), (3, '阿里巴巴'), (4, '京东商城'),
    (5, '比亚迪'), (6, '宁德时代'), (7, '万科地产'), (8, '碧桂园'),
    (9, '中芯国际'), (10, '小米科技'), (11, '美团科技'), (12, '字节跳动'),
]

CUSTOMERS_PERSON = [
    (13, '张伟'), (14, '李娜'), (15, '王芳'), (16, '刘洋'),
    (17, '陈静'), (18, '杨磊'), (19, '赵强'), (20, '黄敏'),
    (21, '周杰'), (22, '吴敏'), (23, '郑磊'), (24, '孙丽'),
]

# ============================================
# 维度依赖关系 - 权重矩阵
# ============================================

# 产品大类ID（基于实际dimension_master表）
CATEGORY_IDS = {
    'CORP_LOAN': 108,      # 公司贷款
    'PERSON_LOAN': 109,    # 个人贷款
    'CORP_DEPOSIT': 110,   # 公司存款
    'PERSON_DEPOSIT': 111, # 个人存款
    'WEALTH': 112,         # 理财产品
    'INTL': 113,           # 国际业务
}

# 机构 → 产品大类权重（每个支行对不同产品大类的侧重）
ORG_CATEGORY_WEIGHTS = {
    99:  {'CORP_LOAN': 0.35, 'PERSON_LOAN': 0.15, 'CORP_DEPOSIT': 0.20, 'PERSON_DEPOSIT': 0.10, 'WEALTH': 0.10, 'INTL': 0.10},  # 朝阳
    100: {'CORP_LOAN': 0.30, 'PERSON_LOAN': 0.20, 'CORP_DEPOSIT': 0.15, 'PERSON_DEPOSIT': 0.15, 'WEALTH': 0.10, 'INTL': 0.10},  # 海淀
    101: {'CORP_LOAN': 0.25, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.15, 'PERSON_DEPOSIT': 0.15, 'WEALTH': 0.10, 'INTL': 0.10},  # 西城
    102: {'CORP_LOAN': 0.20, 'PERSON_LOAN': 0.30, 'CORP_DEPOSIT': 0.10, 'PERSON_DEPOSIT': 0.20, 'WEALTH': 0.10, 'INTL': 0.10},  # 浦东
    103: {'CORP_LOAN': 0.20, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.15, 'PERSON_DEPOSIT': 0.20, 'WEALTH': 0.10, 'INTL': 0.10},  # 浦西
    104: {'CORP_LOAN': 0.30, 'PERSON_LOAN': 0.20, 'CORP_DEPOSIT': 0.15, 'PERSON_DEPOSIT': 0.15, 'WEALTH': 0.10, 'INTL': 0.10},  # 南山
    105: {'CORP_LOAN': 0.25, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.15, 'PERSON_DEPOSIT': 0.15, 'WEALTH': 0.10, 'INTL': 0.10},  # 福田
    106: {'CORP_LOAN': 0.20, 'PERSON_LOAN': 0.30, 'CORP_DEPOSIT': 0.10, 'PERSON_DEPOSIT': 0.20, 'WEALTH': 0.10, 'INTL': 0.10},  # 天河
    107: {'CORP_LOAN': 0.15, 'PERSON_LOAN': 0.35, 'CORP_DEPOSIT': 0.10, 'PERSON_DEPOSIT': 0.20, 'WEALTH': 0.10, 'INTL': 0.10},  # 西湖
}

# 客户经理专长权重（60%在专长产品，40%其他）
MGR_CATEGORY_WEIGHTS = {}
for mgr in MANAGERS:
    mgr_id = mgr[0]
    specialty = mgr[4]
    weights = {}
    for cat in CATEGORY_IDS:
        if cat == specialty:
            weights[cat] = 0.60
        else:
            weights[cat] = 0.08  # 0.40 / 5 = 0.08
    MGR_CATEGORY_WEIGHTS[mgr_id] = weights

# 渠道 → 产品大类权重
CHANNEL_CATEGORY_WEIGHTS = {
    169: {'CORP_LOAN': 0.05, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.05, 'PERSON_DEPOSIT': 0.40, 'WEALTH': 0.20, 'INTL': 0.05},  # 自助银行
    170: {'CORP_LOAN': 0.05, 'PERSON_LOAN': 0.15, 'CORP_DEPOSIT': 0.05, 'PERSON_DEPOSIT': 0.35, 'WEALTH': 0.35, 'INTL': 0.05},  # 智能柜台
    171: {'CORP_LOAN': 0.40, 'PERSON_LOAN': 0.10, 'CORP_DEPOSIT': 0.30, 'PERSON_DEPOSIT': 0.05, 'WEALTH': 0.10, 'INTL': 0.05},  # 客户经理外拓
    172: {'CORP_LOAN': 0.10, 'PERSON_LOAN': 0.30, 'CORP_DEPOSIT': 0.05, 'PERSON_DEPOSIT': 0.30, 'WEALTH': 0.20, 'INTL': 0.05},  # 社区银行
    173: {'CORP_LOAN': 0.05, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.05, 'PERSON_DEPOSIT': 0.30, 'WEALTH': 0.30, 'INTL': 0.05},  # APP渠道
    174: {'CORP_LOAN': 0.05, 'PERSON_LOAN': 0.20, 'CORP_DEPOSIT': 0.05, 'PERSON_DEPOSIT': 0.35, 'WEALTH': 0.30, 'INTL': 0.05},  # 小程序渠道
    175: {'CORP_LOAN': 0.05, 'PERSON_LOAN': 0.25, 'CORP_DEPOSIT': 0.10, 'PERSON_DEPOSIT': 0.30, 'WEALTH': 0.25, 'INTL': 0.05},  # 个人网银
    176: {'CORP_LOAN': 0.25, 'PERSON_LOAN': 0.05, 'CORP_DEPOSIT': 0.35, 'PERSON_DEPOSIT': 0.05, 'WEALTH': 0.15, 'INTL': 0.15},  # 企业网银
}

# ============================================
# 产品基础收益参数
# ============================================

LOAN_BASE_REV = {
    126: 800,   # 流动资金贷款
    127: 400,   # 银承汇票
    128: 600,   # 固定资产贷款
    129: 500,   # 项目贷款
    130: 500,   # 首套房贷款
    131: 400,   # 二套房贷款
    132: 300,   # 个人消费贷款
    133: 350,   # 个人经营贷款
}

DEPOSIT_BASE_AMT = {
    134: 5000,  # 协定存款
    135: 3000,  # 通知存款
    136: 4000,  # 整存整取
    137: 8000,  # 大额存单
    138: 2000,  # 借记卡存款
    139: 1500,  # 活期储蓄
    140: 1000,  # 零存整取
    141: 1200,  # 整存零取
}

# 产品大类 → 具体产品列表
PRODUCTS_BY_CATEGORY = {
    'CORP_LOAN': [p for p in PRODUCTS if p['type'] == 'LOAN' and p['id'] in [126, 127, 128, 129]],
    'PERSON_LOAN': [p for p in PRODUCTS if p['type'] == 'LOAN' and p['id'] in [130, 131, 132, 133]],
    'CORP_DEPOSIT': [p for p in PRODUCTS if p['type'] == 'DEPOSIT' and p['id'] in [134, 135, 136, 137]],
    'PERSON_DEPOSIT': [p for p in PRODUCTS if p['type'] == 'DEPOSIT' and p['id'] in [138, 139, 140, 141]],
    'WEALTH': [p for p in PRODUCTS if p['type'] == 'DEPOSIT' and p['id'] in [138, 139, 140, 141]],
    'INTL': [p for p in PRODUCTS if p['type'] == 'LOAN' and p['id'] in [126, 127, 128, 129]],
}

# 日期范围
START_DATE = datetime(2024, 1, 1)
END_DATE = datetime(2026, 6, 16)

def get_date_range(start, end):
    dates = []
    current = start
    while current <= end:
        dates.append(current)
        current += timedelta(days=1)
    return dates

DATES = get_date_range(START_DATE, END_DATE)

def get_org_managers(org_id):
    """获取某机构的客户经理列表"""
    return [m for m in MANAGERS if m[3] == org_id]

def weighted_choice(weights):
    """按权重随机选择"""
    items = list(weights.keys())
    probs = [weights[k] for k in items]
    total = sum(probs)
    probs = [p / total for p in probs]
    return random.choices(items, weights=probs, k=1)[0]

def select_category(org_id, manager_id, channel_id):
    """三维加权选择产品大类"""
    org_w = ORG_CATEGORY_WEIGHTS.get(org_id, ORG_CATEGORY_WEIGHTS[99])
    mgr_w = MGR_CATEGORY_WEIGHTS.get(manager_id, MGR_CATEGORY_WEIGHTS[191])
    ch_w = CHANNEL_CATEGORY_WEIGHTS.get(channel_id, CHANNEL_CATEGORY_WEIGHTS[169])

    # 合成权重 = 机构×0.4 + 经理×0.35 + 渠道×0.25
    final_w = {}
    for cat in CATEGORY_IDS:
        final_w[cat] = (
            org_w.get(cat, 0.1) * 0.4 +
            mgr_w.get(cat, 0.1) * 0.35 +
            ch_w.get(cat, 0.1) * 0.25
        )

    return weighted_choice(final_w)

def select_channel_for_product(product):
    """根据产品类型选择合适的渠道"""
    if product['type'] == 'LOAN':
        loan_channels = {171: 0.40, 169: 0.10, 170: 0.10, 172: 0.10, 173: 0.10, 174: 0.10, 175: 0.05, 176: 0.05}
    else:
        loan_channels = {169: 0.20, 170: 0.20, 171: 0.05, 172: 0.10, 173: 0.20, 174: 0.15, 175: 0.05, 176: 0.05}

    channel_id = weighted_choice(loan_channels)
    return next(c for c in CHANNELS if c[0] == channel_id)

def gen_loan_record(biz_id, date, org, product, manager, channel, customer):
    """生成贷款记录"""
    random_factor = random.uniform(0.7, 1.3)
    month_factor = 1 + (date.month - 1) * 0.005
    season_factor = 1.15 if date.month in [3, 6, 9, 12] else 1.0

    base_rev = LOAN_BASE_REV.get(product['id'], 500)
    loan_revenue = round(base_rev * random_factor * month_factor * season_factor, 2)
    loan_ftp_cost = round(loan_revenue * random.uniform(0.45, 0.58), 2)
    loan_risk_cost = round(loan_revenue * random.uniform(0.05, 0.15), 2)
    loan_op_cost = round(loan_revenue * random.uniform(0.06, 0.12), 2)
    loan_profit = round(loan_revenue - loan_ftp_cost - loan_risk_cost - loan_op_cost, 2)
    biz_amount = round(loan_revenue * random.uniform(8, 15), 2)

    return {
        'biz_id': f'BIZ{biz_id:010d}',
        'stat_date': date.strftime('%Y-%m-%d'),
        'account_period': date.strftime('%Y-%m'),
        'org_id': org[0],
        'product_id': product['id'],
        'biz_line_id': product['biz_line_id'],
        'dept_id': product['dept_id'],
        'channel_id': channel[0],
        'manager_id': manager[0],
        'customer_id': customer[0],
        'product_type': 'LOAN',
        'biz_amount': biz_amount,
        'revenue': loan_revenue,
        'interest_income': loan_revenue,
        'interest_expense': 0,
        'fee_income': round(loan_revenue * random.uniform(0.05, 0.15), 2),
        'non_interest_income': round(loan_revenue * random.uniform(0.02, 0.08), 2),
        'ftp_cost': loan_ftp_cost,
        'risk_cost': loan_risk_cost,
        'op_cost': loan_op_cost,
        'net_profit': loan_profit,
        'loan_revenue': loan_revenue,
        'loan_ftp_cost': loan_ftp_cost,
        'loan_risk_cost': loan_risk_cost,
        'loan_op_cost': loan_op_cost,
        'loan_profit': loan_profit,
        'deposit_revenue': 0,
        'deposit_interest': 0,
        'deposit_op_cost': 0,
        'deposit_profit': 0,
        'caliber_type': 'ASSESS',
        'currency': 'CNY',
    }

def gen_deposit_record(biz_id, date, org, product, manager, channel, customer):
    """生成存款记录"""
    random_factor = random.uniform(0.6, 1.4)
    month_factor = 1 + (date.month - 1) * 0.003

    base_amt = DEPOSIT_BASE_AMT.get(product['id'], 2000)
    biz_amount = round(base_amt * random_factor * month_factor, 2)
    deposit_revenue = round(biz_amount * random.uniform(0.02, 0.03), 2)
    deposit_interest = round(biz_amount * random.uniform(0.015, 0.025), 2)
    deposit_op_cost = round(biz_amount * random.uniform(0.003, 0.005), 2)
    deposit_profit = round(deposit_revenue - deposit_interest - deposit_op_cost, 2)

    return {
        'biz_id': f'BIZ{biz_id:010d}',
        'stat_date': date.strftime('%Y-%m-%d'),
        'account_period': date.strftime('%Y-%m'),
        'org_id': org[0],
        'product_id': product['id'],
        'biz_line_id': product['biz_line_id'],
        'dept_id': product['dept_id'],
        'channel_id': channel[0],
        'manager_id': manager[0],
        'customer_id': customer[0],
        'product_type': 'DEPOSIT',
        'biz_amount': biz_amount,
        'revenue': deposit_revenue,
        'interest_income': 0,
        'interest_expense': deposit_interest,
        'fee_income': 0,
        'non_interest_income': 0,
        'ftp_cost': 0,
        'risk_cost': 0,
        'op_cost': deposit_op_cost,
        'net_profit': deposit_profit,
        'loan_revenue': 0,
        'loan_ftp_cost': 0,
        'loan_risk_cost': 0,
        'loan_op_cost': 0,
        'loan_profit': 0,
        'deposit_revenue': deposit_revenue,
        'deposit_interest': deposit_interest,
        'deposit_op_cost': deposit_op_cost,
        'deposit_profit': deposit_profit,
        'caliber_type': 'ASSESS',
        'currency': 'CNY',
    }

def gen_data():
    """生成全部数据（100万条目标）"""
    rows = []
    biz_id = 1

    total_dates = len(DATES)
    print(f'生成数据：{DATES[0].strftime("%Y-%m-%d")} ~ {DATES[-1].strftime("%Y-%m-%d")}，共{total_dates}天')
    print(f'目标：{total_dates}天 × 9机构 × 120笔/天 ≈ {total_dates * 9 * 120:,}条')

    for day_idx, date in enumerate(DATES):
        if day_idx % 100 == 0:
            print(f'  进度：{day_idx}/{total_dates} 天，已生成 {len(rows):,} 条')

        for org in ORGS_L3:
            org_managers = get_org_managers(org[0])
            if not org_managers:
                org_managers = [MANAGERS[0]]

            # 贷款业务：每天80笔
            for _ in range(80):
                manager = random.choice(org_managers)
                category = select_category(org[0], manager[0], 169)
                products_in_cat = PRODUCTS_BY_CATEGORY.get(category, PRODUCTS[:8])
                product = random.choice(products_in_cat)
                channel = select_channel_for_product(product)
                customer = random.choice(CUSTOMERS_CORP if category in ['CORP_LOAN', 'CORP_DEPOSIT'] else CUSTOMERS_PERSON)
                rows.append(gen_loan_record(biz_id, date, org, product, manager, channel, customer))
                biz_id += 1

            # 存款业务：每天40笔
            for _ in range(40):
                manager = random.choice(org_managers)
                category = select_category(org[0], manager[0], 169)
                products_in_cat = PRODUCTS_BY_CATEGORY.get(category, PRODUCTS[8:])
                product = random.choice(products_in_cat)
                channel = select_channel_for_product(product)
                customer = random.choice(CUSTOMERS_CORP if category in ['CORP_LOAN', 'CORP_DEPOSIT'] else CUSTOMERS_PERSON)
                rows.append(gen_deposit_record(biz_id, date, org, product, manager, channel, customer))
                biz_id += 1

    return rows

def write_csv(rows, output_path):
    """写入CSV"""
    headers = [
        'biz_id', 'stat_date', 'account_period',
        'org_id', 'product_id', 'biz_line_id', 'dept_id',
        'channel_id', 'manager_id', 'customer_id', 'product_type',
        'biz_amount',
        'revenue', 'interest_income', 'interest_expense', 'fee_income', 'non_interest_income',
        'ftp_cost', 'risk_cost', 'op_cost', 'net_profit',
        'loan_revenue', 'loan_ftp_cost', 'loan_risk_cost', 'loan_op_cost', 'loan_profit',
        'deposit_revenue', 'deposit_interest', 'deposit_op_cost', 'deposit_profit',
        'caliber_type', 'currency',
    ]

    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f, quoting=csv.QUOTE_ALL)
        writer.writerow(headers)
        for row in rows:
            writer.writerow([row.get(h, '') for h in headers])

    print(f'CSV文件已生成：{output_path} ({len(rows):,} 条)')

if __name__ == '__main__':
    print('='*50)
    print('多维盈利分析系统 - Mock数据生成器V3（星型模型）')
    print('='*50)

    rows = gen_data()

    output_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(output_dir, 'biz_ledger_mock.csv')

    write_csv(rows, csv_path)

    loan_count = len([r for r in rows if r['product_type'] == 'LOAN'])
    deposit_count = len([r for r in rows if r['product_type'] == 'DEPOSIT'])

    print(f'\n统计：')
    print(f'  总记录数：{len(rows):,}')
    print(f'  贷款：{loan_count:,}')
    print(f'  存款：{deposit_count:,}')
    print(f'  机构数：{len(ORGS_L3)}')
    print(f'  天数：{len(DATES)}')

    # 验证维度依赖
    print(f'\n维度依赖验证：')
    from collections import Counter
    org_product_dist = Counter()
    for r in rows[:10000]:
        org_product_dist[(r['org_id'], r['product_type'])] += 1
    print(f'  机构-产品类型分布（前10000条）：')
    for (org_id, ptype), cnt in sorted(org_product_dist.items()):
        org_name = next(o[2] for o in ORGS_L3 if o[0] == org_id)
        print(f'    {org_name} - {ptype}: {cnt}')

    print('\n完成!')
