#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
业务统计数据导出脚本
从job_analysis.statistics表中统计各业务的数据，生成Excel报表

依赖：
- pymysql
- pandas
- openpyxl
"""

import pymysql
import pandas as pd
from datetime import datetime, timedelta
import sys

# 数据库配置
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': 'password',
    'charset': 'utf8mb4'
}

def get_db_connection():
    """获取数据库连接"""
    try:
        conn = pymysql.connect(**DB_CONFIG)
        return conn
    except Exception as e:
        print(f"数据库连接失败: {e}")
        sys.exit(1)

def get_all_apps(conn):
    """获取所有业务信息"""
    query = """
    SELECT app_id, bk_scope_id, app_name 
    FROM job_manage.application
    WHERE is_deleted = 0
    ORDER BY app_id
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query)
        results = cursor.fetchall()
        cursor.close()
        return results
    except Exception as e:
        print(f"查询业务信息失败: {e}")
        return []

def get_script_count(conn, app_id, today):
    """获取脚本数量"""
    query = """
    SELECT SUM(value) 
    FROM job_analysis.statistics 
    WHERE resource='script' 
      AND `date`=%s 
      AND dimension='SCRIPT_TYPE' 
      AND app_id=%s
    LIMIT 1
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query, (today, app_id))
        result = cursor.fetchone()
        cursor.close()
        return int(result[0]) if result and result[0] else 0
    except Exception as e:
        print(f"查询业务{app_id}的脚本数量失败: {e}")
        return 0

def get_template_count(conn, app_id, today):
    """获取模板数量"""
    query = """
    SELECT value 
    FROM job_analysis.statistics 
    WHERE app_id=%s 
      AND resource='global' 
      AND dimension='globalStatisticType' 
      AND dimension_value='TASK_TEMPLATE_COUNT' 
      AND `date`=%s 
    LIMIT 1
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query, (app_id, today))
        result = cursor.fetchone()
        cursor.close()
        return int(result[0]) if result and result[0] else 0
    except Exception as e:
        print(f"查询业务{app_id}的模板数量失败: {e}")
        return 0

def get_plan_count(conn, app_id, today):
    """获取方案数量"""
    query = """
    SELECT value 
    FROM job_analysis.statistics 
    WHERE app_id=%s 
      AND resource='global' 
      AND dimension='globalStatisticType' 
      AND dimension_value='TASK_PLAN_COUNT' 
      AND `date`=%s 
    LIMIT 1
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query, (app_id, today))
        result = cursor.fetchone()
        cursor.close()
        return int(result[0]) if result and result[0] else 0
    except Exception as e:
        print(f"查询业务{app_id}的方案数量失败: {e}")
        return 0

def get_cron_count(conn, app_id, today):
    """获取定时任务数量"""
    query = """
    SELECT SUM(value) 
    FROM job_analysis.statistics 
    WHERE resource='cron' 
      AND `date`=%s 
      AND dimension='CRON_TYPE' 
      AND app_id=%s
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query, (today, app_id))
        result = cursor.fetchone()
        cursor.close()
        return int(result[0]) if result and result[0] else 0
    except Exception as e:
        print(f"查询业务{app_id}的定时任务数量失败: {e}")
        return 0

def get_execute_count_last_year(conn, app_id, today, one_year_ago):
    """获取最近一年执行任务数（今天的值 - 一年前的值）"""
    # 查询今天的执行数量
    query_today = """
    SELECT value 
    FROM job_analysis.statistics 
    WHERE resource='executedTask' 
      AND `date`=%s 
      AND dimension='globalStatisticType' 
      AND dimension_value='EXECUTED_TASK_COUNT' 
      AND app_id=%s
    """
    # 查询一年前的执行数量
    query_year_ago = """
    SELECT value 
    FROM job_analysis.statistics 
    WHERE resource='executedTask' 
      AND `date`=%s 
      AND dimension='globalStatisticType' 
      AND dimension_value='EXECUTED_TASK_COUNT' 
      AND app_id=%s
    """
    try:
        cursor = conn.cursor()
        
        # 查询今天的值
        cursor.execute(query_today, (today, app_id))
        result_today = cursor.fetchone()
        value_today = int(result_today[0]) if result_today and result_today[0] else 0
        
        # 查询一年前的值
        cursor.execute(query_year_ago, (one_year_ago, app_id))
        result_year_ago = cursor.fetchone()
        value_year_ago = int(result_year_ago[0]) if result_year_ago and result_year_ago[0] else 0
        
        cursor.close()

        return value_today - value_year_ago
    except Exception as e:
        print(f"查询业务{app_id}的最近一年执行任务数失败: {e}")
        return 0

def get_account_count(conn, app_id):
    """获取账号数量"""
    query = """
    SELECT COUNT(*) 
    FROM job_manage.account 
    WHERE app_id=%s
    """
    try:
        cursor = conn.cursor()
        cursor.execute(query, (app_id,))
        result = cursor.fetchone()
        cursor.close()
        return int(result[0]) if result and result[0] else 0
    except Exception as e:
        print(f"查询业务{app_id}的账号数量失败: {e}")
        return 0

def generate_statistics_excel():
    """生成业务统计Excel表格"""
    print("开始生成业务统计Excel表格...")

    conn = get_db_connection()
    
    try:
        # 日期
        today = datetime.now().strftime('%Y-%m-%d')
        one_year_ago = (datetime.now() - timedelta(days=365-1)).strftime('%Y-%m-%d')
        
        print(f"统计日期: {today}")
        print(f"一年前日期: {one_year_ago}")
        
        # 获取所有业务
        print("\n正在查询业务列表...")
        apps = get_all_apps(conn)
        
        if not apps:
            print("未查询到业务数据")
            return
        
        print(f"共查询到 {len(apps)} 个业务")
        
        # 准备数据列表
        data_list = []
        
        # 遍历每个业务，获取统计数据
        for index, app in enumerate(apps):
            app_id = app[0]
            bk_scope_id = app[1]
            app_name = app[2]
            
            print(f"\n正在处理业务: {app_id} - {app_name} ({index + 1}/{len(apps)})")
            
            # 获取各项统计数据
            script_count = get_script_count(conn, app_id, today)
            template_count = get_template_count(conn, app_id, today)
            plan_count = get_plan_count(conn, app_id, today)
            cron_count = get_cron_count(conn, app_id, today)
            execute_count = get_execute_count_last_year(conn, app_id, today, one_year_ago)
            account_count = get_account_count(conn, app_id)
            
            data_list.append({
                'bk_scope_id': bk_scope_id,
                'app_name': app_name,
                '脚本': script_count,
                '模板': template_count,
                '方案': plan_count,
                '定时任务': cron_count,
                '一年执行数量': execute_count,
                '账号数': account_count
            })

        result_df = pd.DataFrame(data_list)
        output_file = f'JOB_业务统计_{datetime.now().strftime("%Y%m%d_%H%M%S")}.xlsx'
        print(f"\n正在导出Excel文件: {output_file}")
        result_df.to_excel(output_file, index=False, engine='openpyxl')

        print(f"✅ Excel文件生成成功: {output_file}")
        print(f"共导出 {len(result_df)} 条业务数据")
        
    except Exception as e:
        print(f"生成Excel失败: {e}")
        import traceback
        traceback.print_exc()
    finally:
        conn.close()
        print("\n数据库连接已关闭")

if __name__ == '__main__':
    
    generate_statistics_excel()
