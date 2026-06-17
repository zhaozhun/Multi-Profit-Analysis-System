#!/usr/bin/env python3
"""
多维盈利分析系统 - 30万条Mock数据生成器
时间范围：2025-01-01 ~ 2026-06-16
每天每机构约60条记录
"""

import random
import csv
import os
from datetime import datetime, timedelta

# 机构（支行级别）
ORGS = [
    (7, 'ORG0010101', '朝阳支行', 2),
    (8, 'ORG0010102', '海淀支行', 2),
    (9, 'ORG0010103', '西城支行', 2),
    (10, 'ORG0010201', '浦东支行', 3),
    (11, 'ORG0010202', '浦西支行', 3),
    (12, 'ORG0010301', '南山支行', 4),
    (13, 'ORG0010302', '福田支行', 4),
    (14, 'ORG0010401', '天河支行', 5),
    (15, 'ORG0010501', '西湖支行', 6),
]

# 产品（区分存款/贷款）
PRODUCTS = [
    {'id': 33, 'code': 'PROD0101', 'name': '流动资金贷款', 'type': 'LOAN', 'biz_line': 0, 'dept': 0, 'base_rev': 800},
    {'id': 34, 'code': 'PROD0102', 'name': '固定资产贷款', 'type': 'LOAN', 'biz_line': 0, 'dept': 0, 'base_rev': 600},
    {'id': 35, 'code': 'PROD0103', 'name': '银承汇票', 'type': 'LOAN', 'biz_line': 0, 'dept': 0, 'base_rev': 400},
    {'id': 36, 'code': 'PROD0201', 'name': '个人住房贷款', 'type': 'LOAN', 'biz_line': 1, 'dept': 1, 'base_rev': 500},
    {'id': 37, 'code': 'PROD0202', 'name': '个人消费贷款', 'type': 'LOAN', 'biz_line': 1, 'dept': 1, 'base_rev': 300},
    {'id': 38, 'code': 'PROD0203', 'name': '个人经营贷款', 'type': 'LOAN', 'biz_line': 3, 'dept': 0, 'base_rev': 350},
    {'id': 28, 'code': 'PROD03', 'name': '公司存款', 'type': 'DEPOSIT', 'biz_line': 0, 'dept': 0, 'base_amt': 5000},
    {'id': 29, 'code': 'PROD04', 'name': '个人存款', 'type': 'DEPOSIT', 'biz_line': 1, 'dept': 1, 'base_amt': 3000},
]

# 条线
BIZ_LINES = [
    (16, 'BL01', '对公条线'),
    (17, 'BL02', '零售条线'),
    (18, 'BL03', '金融市场条线'),
    (19, 'BL04', '小微条线'),
]

# 部门
DEPTS = [
    (20, 'DEPT01', '公司金融部'),
    (21, 'DEPT02', '零售金融部'),
    (22, 'DEPT03', '金融市场部'),
]

# 渠道
CHANNELS = [
    (39, 'CH01', '柜面渠道'),
    (40, 'CH02', '网上银行'),
    (41, 'CH03', '手机银行'),
    (42, 'CH04', '自助设备'),
]

# 客户经理（每个支行分配2-3个）
MANAGERS = [
    (44, 'MGR001', '张明', 7),   # 朝阳支行
    (45, 'MGR002', '李华', 8),   # 海淀支行
    (46, 'MGR003', '王芳', 10),  # 浦东支行
    (47, 'MGR004', '赵强', 12),  # 南山支行
    (48, 'MGR005', '刘洋', 14),  # 天河支行
    (49, 'MGR006', '陈静', 15),  # 西湖支行
    (50, 'MGR007', '周伟', 9),   # 西城支行
    (51, 'MGR008', '吴敏', 11),  # 浦西支行
    (52, 'MGR009', '郑磊', 13),  # 福田支行
]

# 客户
CUSTOMERS_CORP = [
    '华为技术', '腾讯科技', '阿里巴巴', '京东商城', '比亚迪', '宁德时代',
    '万科地产', '碧桂园', '中芯国际', '小米科技', '美团科技', '字节跳动',
    '中国石油', '中国石化', '中国移动', '工商银行', '建设银行', '农业银行',
    '平安保险', '中国人寿', '招商银行', '兴业银行', '民生银行', '光大银行',
    '中信证券', '国泰君安', '海通证券', '华泰证券', '格力电器', '美的集团',
    '海尔智家', '联想集团', '长城汽车', '吉利汽车', '蔚来汽车', '理想汽车',
    '小鹏汽车', '特斯拉中国', '苹果中国', '微软中国', '亚马逊中国', '谷歌中国',
]

CUSTOMERS_PERSON = [
    '张伟', '李娜', '王芳', '刘洋', '陈静', '杨磊', '赵强', '黄敏',
    '周伟', '吴敏', '徐磊', '孙丽', '马超', '朱婷', '胡伟', '郭静',
    '林洋', '何敏', '罗磊', '梁丽', '宋超', '郑婷', '谢伟', '韩静',
]

# 生成日期范围
START_DATE = datetime(2025, 1, 1)
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
    """获取机构下的客户经理"""
    return [m for m in MANAGERS if m[3] == org_id]

def gen_loan_record(biz_id, date, org, product, manager, customer):
    """生成贷款记录"""
    day_of_year = date.timetuple().tm_yday
    month_factor = 1 + (date.month - 1) * 0.005  # 月度增长
    season_factor = 1.15 if date.month in [3, 6, 9, 12] else 1.0  # 季末上浮
    random_factor = random.uniform(0.7, 1.3)

    interest_income = round(product['base_rev'] * random_factor * month_factor * season_factor, 2)
    ftp_cost = round(interest_income * random.uniform(0.45, 0.58), 2)
    risk_cost = round(interest_income * random.uniform(0.05, 0.15), 2)
    op_cost = round(interest_income * random.uniform(0.06, 0.12), 2)
    net_profit = round(interest_income - ftp_cost - risk_cost - op_cost, 2)
    biz_amount = round(interest_income * random.uniform(8, 15), 2)

    channel = random.choice(CHANNELS)
    bl = BIZ_LINES[product['biz_line']]
    dept = DEPTS[product['dept']]

    return {
        'biz_id': f'BIZ{biz_id:010d}',
        'stat_date': date.strftime('%Y-%m-%d'),
        'account_period': date.strftime('%Y-%m'),
        'org_id': org[0], 'org_code': org[1], 'org_name': org[2],
        'product_id': product['id'], 'product_code': product['code'], 'product_name': product['name'],
        'product_type': 'LOAN',
        'biz_line_id': bl[0], 'biz_line_code': bl[1], 'biz_line_name': bl[2],
        'dept_id': dept[0], 'dept_code': dept[1], 'dept_name': dept[2],
        'channel_id': channel[0], 'channel_code': channel[1], 'channel_name': channel[2],
        'manager_id': manager[0], 'manager_code': manager[1], 'manager_name': manager[2],
        'customer_name': customer,
        'biz_amount': biz_amount,
        'revenue': interest_income,
        'interest_income': interest_income,
        'interest_expense': 0,
        'fee_income': round(interest_income * random.uniform(0.05, 0.15), 2),
        'non_interest_income': round(interest_income * random.uniform(0.02, 0.08), 2),
        'ftp_cost': ftp_cost,
        'risk_cost': risk_cost,
        'op_cost': op_cost,
        'net_profit': net_profit,
        'caliber_type': 'ASSESS',
        'currency': 'CNY',
    }

def gen_deposit_record(biz_id, date, org, product, manager, customer):
    """生成存款记录"""
    random_factor = random.uniform(0.6, 1.4)
    month_factor = 1 + (date.month - 1) * 0.003

    biz_amount = round(product['base_amt'] * random_factor * month_factor, 2)
    ftp_income = round(biz_amount * random.uniform(0.02, 0.03), 2)
    cust_interest = round(biz_amount * random.uniform(0.015, 0.025), 2)
    op_cost = round(biz_amount * random.uniform(0.003, 0.005), 2)
    net_profit = round(ftp_income - cust_interest - op_cost, 2)

    channel = random.choice(CHANNELS)
    bl = BIZ_LINES[product['biz_line']]
    dept = DEPTS[product['dept']]

    return {
        'biz_id': f'BIZ{biz_id:010d}',
        'stat_date': date.strftime('%Y-%m-%d'),
        'account_period': date.strftime('%Y-%m'),
        'org_id': org[0], 'org_code': org[1], 'org_name': org[2],
        'product_id': product['id'], 'product_code': product['code'], 'product_name': product['name'],
        'product_type': 'DEPOSIT',
        'biz_line_id': bl[0], 'biz_line_code': bl[1], 'biz_line_name': bl[2],
        'dept_id': dept[0], 'dept_code': dept[1], 'dept_name': dept[2],
        'channel_id': channel[0], 'channel_code': channel[1], 'channel_name': channel[2],
        'manager_id': manager[0], 'manager_code': manager[1], 'manager_name': manager[2],
        'customer_name': customer,
        'biz_amount': biz_amount,
        'revenue': ftp_income,
        'interest_income': ftp_income,
        'interest_expense': cust_interest,
        'fee_income': 0,
        'non_interest_income': 0,
        'ftp_cost': 0,
        'risk_cost': 0,
        'op_cost': op_cost,
        'net_profit': net_profit,
        'caliber_type': 'ASSESS',
        'currency': 'CNY',
    }

def gen_data():
    """生成全部数据"""
    rows = []
    biz_id = 1

    loan_products = [p for p in PRODUCTS if p['type'] == 'LOAN']
    deposit_products = [p for p in PRODUCTS if p['type'] == 'DEPOSIT']

    total_dates = len(DATES)
    print(f'生成数据：{DATES[0].strftime("%Y-%m-%d")} ~ {DATES[-1].strftime("%Y-%m-%d")}，共{total_dates}天')

    for day_idx, date in enumerate(DATES):
        if day_idx % 50 == 0:
            print(f'  进度：{day_idx}/{total_dates} 天，已生成 {len(rows)} 条')

        for org in ORGS:
            org_managers = get_org_managers(org[0])
            if not org_managers:
                org_managers = [MANAGERS[0]]  # fallback

            # 贷款业务：每天40-50笔
            for _ in range(random.randint(40, 50)):
                product = random.choice(loan_products)
                manager = random.choice(org_managers)
                customer = random.choice(CUSTOMERS_CORP + CUSTOMERS_PERSON)
                rows.append(gen_loan_record(biz_id, date, org, product, manager, customer))
                biz_id += 1

            # 存款业务：每天15-20笔
            for _ in range(random.randint(15, 20)):
                product = random.choice(deposit_products)
                manager = random.choice(org_managers)
                customer = random.choice(CUSTOMERS_CORP + CUSTOMERS_PERSON)
                rows.append(gen_deposit_record(biz_id, date, org, product, manager, customer))
                biz_id += 1

    return rows

def write_csv(rows, output_path):
    """写入CSV"""
    headers = [
        'biz_id', 'stat_date', 'account_period',
        'org_id', 'org_code', 'org_name',
        'product_id', 'product_code', 'product_name', 'product_type',
        'biz_line_id', 'biz_line_code', 'biz_line_name',
        'dept_id', 'dept_code', 'dept_name',
        'channel_id', 'channel_code', 'channel_name',
        'manager_id', 'manager_code', 'manager_name',
        'customer_name', 'biz_amount',
        'revenue', 'interest_income', 'interest_expense', 'fee_income', 'non_interest_income',
        'ftp_cost', 'risk_cost', 'op_cost', 'net_profit',
        'caliber_type', 'currency',
    ]

    with open(output_path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f, quoting=csv.QUOTE_ALL)
        writer.writerow(headers)
        for row in rows:
            writer.writerow([row.get(h, '') for h in headers])

    print(f'CSV文件已生成：{output_path} ({len(rows)} 条)')

if __name__ == '__main__':
    print('='*50)
    print('多维盈利分析系统 - 30万条Mock数据生成器')
    print('='*50)

    rows = gen_data()

    output_dir = os.path.dirname(os.path.abspath(__file__))
    csv_path = os.path.join(output_dir, 'biz_ledger_mock.csv')

    write_csv(rows, csv_path)

    # 统计
    loan_count = len([r for r in rows if r['product_type'] == 'LOAN'])
    deposit_count = len([r for r in rows if r['product_type'] == 'DEPOSIT'])
    org_count = len(set(r['org_name'] for r in rows))
    date_count = len(set(r['stat_date'] for r in rows))

    print(f'\n统计：')
    print(f'  总记录数：{len(rows):,}')
    print(f'  贷款：{loan_count:,}')
    print(f'  存款：{deposit_count:,}')
    print(f'  机构数：{org_count}')
    print(f'  天数：{date_count}')
    print(f'  日期范围：{DATES[0].strftime("%Y-%m-%d")} ~ {DATES[-1].strftime("%Y-%m-%d")}')
    print('\n完成!')
