# 脚本/数据生成器/generate_daily.py
"""每日数据生成:基于存量业务属性,生成每日时点余额/利息/FTP/风险"""
import pymysql
import random
import pickle
import datetime
from config import *

def generate_daily_data(start_date_str, end_date_str):
    """生成指定日期范围的日级数据"""
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()

    # 加载存量业务属性
    with open('biz_cache.pkl', 'rb') as f:
        cache = pickle.load(f)
    loans = cache['loans']
    deposits = cache['deposits']

    start_date = datetime.date.fromisoformat(start_date_str)
    end_date = datetime.date.fromisoformat(end_date_str)
    delta = (end_date - start_date).days

    # 初始化每笔业务的昨日余额(首次使用初始余额)
    loan_balances = {b['biz_id']: b['loan_balance'] for b in loans}
    deposit_balances = {b['biz_id']: b['deposit_balance'] for b in deposits}

    loan_sql = """INSERT INTO loan_indicator_detail
        (biz_id, stat_date, account_period, caliber_type,
         org_id, org_name, dept_id, dept_name, product_id, product_name,
         channel_id, channel_name, manager_id, manager_name,
         biz_line_id, biz_line_name, customer_id, customer_name,
         loan_balance, loan_rate, loan_interest_calc_type,
         loan_daily_interest, loan_monthly_interest, loan_cumulative_interest,
         ftp_rate, ftp_cost, risk_cost, op_cost)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                %s,%s,%s,%s,%s,%s,%s,%s,%s,%s)"""

    deposit_sql = """INSERT INTO deposit_indicator_detail
        (biz_id, stat_date, account_period, caliber_type,
         org_id, org_name, dept_id, dept_name, product_id, product_name,
         channel_id, channel_name, manager_id, manager_name,
         biz_line_id, biz_line_name, customer_id, customer_name,
         deposit_balance, deposit_rate, deposit_interest_calc_type,
         deposit_daily_interest, deposit_monthly_interest, deposit_cumulative_interest,
         ftp_rate, ftp_income, op_cost)
        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,
                %s,%s,%s,%s,%s,%s,%s,%s,%s)"""

    loan_batch = []
    deposit_batch = []
    # 月度累计利息缓存(按月+按biz_id)
    monthly_loan_interest = {}
    monthly_deposit_interest = {}
    cumulative_loan_interest = {b['biz_id']: 0 for b in loans}
    cumulative_deposit_interest = {b['biz_id']: 0 for b in deposits}
    # 每月FTP和风险累计
    monthly_loan_ftp = {}
    monthly_loan_risk = {}
    monthly_deposit_ftp = {}

    batch_size = 500  # 每500行一批提交
    total_loan_rows = 0
    total_deposit_rows = 0

    for day_offset in range(delta + 1):
        current_date = start_date + datetime.timedelta(days=day_offset)
        account_period = current_date.strftime("%Y-%m")

        # 月初重置月度累计
        if current_date.day == 1:
            monthly_loan_interest = {}
            monthly_deposit_interest = {}
            monthly_loan_ftp = {}
            monthly_loan_risk = {}
            monthly_deposit_ftp = {}

        # === 贷款业务 ===
        for biz in loans:
            bid = biz['biz_id']
            # 日波动
            prev_balance = loan_balances[bid]
            daily_change = random.uniform(-DAILY_VOLATILITY, DAILY_VOLATILITY)
            new_balance = prev_balance * (1 + daily_change)
            new_balance = max(new_balance, 1000)  # 不低于1000
            loan_balances[bid] = new_balance

            # 当日利息 = 余额 × 利率 / 365
            daily_interest = round(new_balance * biz['loan_rate'] / 365, 4)
            # 当日FTP = 余额 × FTP利率 / 365
            daily_ftp = round(new_balance * biz['ftp_rate'] / 365, 4)
            # 当日风险 = 余额 × 风险成本率 / 365
            daily_risk = round(new_balance * biz['risk_rate'] / 365, 4)

            # 月度累计
            month_key = f"{bid}_{account_period}"
            monthly_loan_interest[month_key] = monthly_loan_interest.get(month_key, 0) + daily_interest
            monthly_loan_ftp[month_key] = monthly_loan_ftp.get(month_key, 0) + daily_ftp
            monthly_loan_risk[month_key] = monthly_loan_risk.get(month_key, 0) + daily_risk
            cumulative_loan_interest[bid] += daily_interest

            loan_batch.append((
                bid, current_date, account_period, 'ASSESS',
                biz['org_id'], biz['org_name'], biz['dept_id'], biz['dept_name'],
                biz['product_id'], biz['product_name'],
                biz['channel_id'], biz['channel_name'],
                biz['manager_id'], biz['manager_name'],
                biz['biz_line_id'], biz['biz_line_name'],
                biz['customer_id'], biz['customer_name'],
                new_balance, biz['loan_rate'], 'DAILY_ACCUMULATED',
                daily_interest, monthly_loan_interest[month_key], cumulative_loan_interest[bid],
                biz['ftp_rate'], daily_ftp, daily_risk, 0  # op_cost=0(日级)
            ))

        # === 存款业务 ===
        for biz in deposits:
            bid = biz['biz_id']
            prev_balance = deposit_balances[bid]
            daily_change = random.uniform(-DAILY_VOLATILITY, DAILY_VOLATILITY)
            new_balance = prev_balance * (1 + daily_change)
            new_balance = max(new_balance, 1000)
            deposit_balances[bid] = new_balance

            # 当日FTP收入 = 余额 × FTP利率 / 365
            daily_ftp_income = round(new_balance * biz['ftp_rate'] / 365, 4)
            # 当日利息支出 = 余额 × 存款利率 / 365
            daily_interest_expense = round(new_balance * biz['deposit_rate'] / 365, 4)

            month_key = f"{bid}_{account_period}"
            monthly_deposit_interest[month_key] = monthly_deposit_interest.get(month_key, 0) + daily_interest_expense
            monthly_deposit_ftp[month_key] = monthly_deposit_ftp.get(month_key, 0) + daily_ftp_income
            cumulative_deposit_interest[bid] += daily_interest_expense

            deposit_batch.append((
                bid, current_date, account_period, 'ASSESS',
                biz['org_id'], biz['org_name'], biz['dept_id'], biz['dept_name'],
                biz['product_id'], biz['product_name'],
                biz['channel_id'], biz['channel_name'],
                biz['manager_id'], biz['manager_name'],
                biz['biz_line_id'], biz['biz_line_name'],
                biz['customer_id'], biz['customer_name'],
                new_balance, biz['deposit_rate'], 'DAILY_ACCUMULATED',
                daily_interest_expense, monthly_deposit_interest[month_key], cumulative_deposit_interest[bid],
                biz['ftp_rate'], daily_ftp_income, 0  # op_cost=0(日级)
            ))

        # 每10天提交一次批量插入(500笔×2×10天=10000行)
        if len(loan_batch) >= batch_size * 10:
            cursor.executemany(loan_sql, loan_batch)
            cursor.executemany(deposit_sql, deposit_batch)
            conn.commit()
            total_loan_rows += len(loan_batch)
            total_deposit_rows += len(deposit_batch)
            loan_batch.clear()
            deposit_batch.clear()
            print(f"  {current_date}: {total_loan_rows} loans + {total_deposit_rows} deposits committed")

    # 提交剩余
    if loan_batch:
        cursor.executemany(loan_sql, loan_batch)
        cursor.executemany(deposit_sql, deposit_batch)
        conn.commit()
        total_loan_rows += len(loan_batch)
        total_deposit_rows += len(deposit_batch)

    print(f"\n完成: {total_loan_rows} 条贷款 + {total_deposit_rows} 条存款 (共{total_loan_rows+total_deposit_rows}行)")
    cursor.close()
    conn.close()

if __name__ == "__main__":
    import sys
    start = sys.argv[1] if len(sys.argv) > 1 else START_DATE
    end = sys.argv[2] if len(sys.argv) > 2 else END_DATE
    print(f"生成日级数据: {start} ~ {end}")
    generate_daily_data(start, end)
