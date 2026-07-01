# 脚本/数据生成器/init_biz.py
"""存量业务初始化:500笔贷款+500笔存款,从dim_*叶子节点随机组合维度"""
import pymysql
import random
import datetime
import pickle
from config import *

def get_leaf_nodes(cursor, table_name, level=3):
    """从dim_*表取叶子节点"""
    cursor.execute(f"SELECT id, name FROM {table_name} WHERE level = {level} AND status = 1")
    return cursor.fetchall()

def get_all_level_nodes(cursor, table_name):
    """取所有层级节点(作为备选)"""
    cursor.execute(f"SELECT id, name, level FROM {table_name} WHERE status = 1")
    return cursor.fetchall()

def get_customers(cursor):
    """取客户清单"""
    cursor.execute("SELECT id, customer_name FROM customer_master WHERE status = 1")
    return cursor.fetchall()

def generate_loan_biz(cursor, conn, start_idx=1, count=LOAN_COUNT):
    """生成贷款存量业务"""
    orgs = get_all_level_nodes(cursor, "dim_organization")
    depts = get_all_level_nodes(cursor, "dim_dept")
    products = get_all_level_nodes(cursor, "dim_product")
    channels = get_all_level_nodes(cursor, "dim_channel")
    managers = get_all_level_nodes(cursor, "dim_manager")
    bizlines = get_all_level_nodes(cursor, "dim_biz_line")
    customers = get_customers(cursor)

    biz_list = []
    for i in range(start_idx, start_idx + count):
        biz_id = f"L{i:06d}"
        org = random.choice(orgs)
        dept = random.choice(depts)
        prod = random.choice(products)
        ch = random.choice(channels)
        mgr = random.choice(managers)
        bl = random.choice(bizlines)
        cust = random.choice(customers)

        balance = round(random.uniform(LOAN_BALANCE_MIN, LOAN_BALANCE_MAX), 2)
        loan_rate = round(random.uniform(LOAN_RATE_MIN, LOAN_RATE_MAX), 6)
        ftp_rate = round(random.uniform(LOAN_FTP_RATE_MIN, LOAN_FTP_RATE_MAX), 6)

        biz_list.append({
            'biz_id': biz_id,
            'org_id': org[0], 'org_name': org[1],
            'dept_id': dept[0], 'dept_name': dept[1],
            'product_id': prod[0], 'product_name': prod[1],
            'channel_id': ch[0], 'channel_name': ch[1],
            'manager_id': mgr[0], 'manager_name': mgr[1],
            'biz_line_id': bl[0], 'biz_line_name': bl[1],
            'customer_id': cust[0], 'customer_name': cust[1],
            'loan_balance': balance,
            'loan_rate': loan_rate,
            'ftp_rate': ftp_rate,
            'risk_rate': round(random.uniform(LOAN_RISK_RATE_MIN, LOAN_RISK_RATE_MAX), 6),
        })
        if (i - start_idx + 1) % 100 == 0:
            print(f"  Loan {biz_id}: balance={balance:,.0f} rate={loan_rate*100:.2f}% [{i-start_idx+1}/{count}]")

    return biz_list

def generate_deposit_biz(cursor, conn, start_idx=1, count=DEPOSIT_COUNT):
    """生成存款存量业务"""
    orgs = get_all_level_nodes(cursor, "dim_organization")
    depts = get_all_level_nodes(cursor, "dim_dept")
    products = get_all_level_nodes(cursor, "dim_product")
    channels = get_all_level_nodes(cursor, "dim_channel")
    managers = get_all_level_nodes(cursor, "dim_manager")
    bizlines = get_all_level_nodes(cursor, "dim_biz_line")
    customers = get_customers(cursor)

    biz_list = []
    for i in range(start_idx, start_idx + count):
        biz_id = f"D{i:06d}"
        org = random.choice(orgs)
        dept = random.choice(depts)
        prod = random.choice(products)
        ch = random.choice(channels)
        mgr = random.choice(managers)
        bl = random.choice(bizlines)
        cust = random.choice(customers)

        balance = round(random.uniform(DEPOSIT_BALANCE_MIN, DEPOSIT_BALANCE_MAX), 2)
        deposit_rate = round(random.uniform(DEPOSIT_RATE_MIN, DEPOSIT_RATE_MAX), 6)
        ftp_rate = round(random.uniform(DEPOSIT_FTP_RATE_MIN, DEPOSIT_FTP_RATE_MAX), 6)

        biz_list.append({
            'biz_id': biz_id,
            'org_id': org[0], 'org_name': org[1],
            'dept_id': dept[0], 'dept_name': dept[1],
            'product_id': prod[0], 'product_name': prod[1],
            'channel_id': ch[0], 'channel_name': ch[1],
            'manager_id': mgr[0], 'manager_name': mgr[1],
            'biz_line_id': bl[0], 'biz_line_name': bl[1],
            'customer_id': cust[0], 'customer_name': cust[1],
            'deposit_balance': balance,
            'deposit_rate': deposit_rate,
            'ftp_rate': ftp_rate,
        })
        if (i - start_idx + 1) % 100 == 0:
            print(f"  Deposit {biz_id}: balance={balance:,.0f} rate={deposit_rate*100:.2f}% [{i-start_idx+1}/{count}]")

    return biz_list

if __name__ == "__main__":
    conn = pymysql.connect(**DB_CONFIG)
    cursor = conn.cursor()
    try:
        print("=== 初始化贷款存量业务(500笔) ===")
        loan_biz = generate_loan_biz(cursor, conn)
        print(f"生成 {len(loan_biz)} 笔贷款业务")

        print("\n=== 初始化存款存量业务(500笔) ===")
        deposit_biz = generate_deposit_biz(cursor, conn)
        print(f"生成 {len(deposit_biz)} 笔存款业务")

        # 保存为Python pickle供generate_daily.py读取
        with open('biz_cache.pkl', 'wb') as f:
            pickle.dump({'loans': loan_biz, 'deposits': deposit_biz}, f)
        print("\n业务属性已缓存到 biz_cache.pkl")
    finally:
        cursor.close()
        conn.close()
