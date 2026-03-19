#!/usr/bin/env python3
"""
KQL 查询语句生成器

用法：
    python3 generate_kql.py --request-id <request_id> [选项]

选项：
    --request-id    请求 ID（必需）
    --level         日志级别（ERROR, WARN, INFO, DEBUG）
    --service       服务类型（gse, cmdb）
    --module        服务模块（job-execute, job-manage 等）
    --message       消息关键词（支持通配符）

示例：
    # 基础查询
    python3 generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9"
    
    # 查询错误日志
    python3 generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --level ERROR
    
    # 查询 GSE 调用日志
    python3 generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --service gse
    
    # 查询 CMDB 调用日志
    python3 generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --service cmdb --module job-execute
"""

import argparse
import sys


# 服务日志路径映射（使用通配符形式，适配不同部署环境）
SERVICE_PATHS = {
    'gse': {
        'default': '*gse.log*',
    },
    'cmdb': {
        'default': '*cmdb.log*',
    }
}


def generate_kql(request_id, level=None, service=None, module=None, message=None):
    """生成 KQL 查询语句"""
    conditions = []
    
    # 添加 request_id 条件
    conditions.append(f'request_id: "{request_id}"')
    
    # 添加日志级别条件
    if level:
        conditions.append(f'level: {level.upper()}')
    
    # 添加服务路径条件
    if service:
        if service not in SERVICE_PATHS:
            print(f"❌ 错误：不支持的服务类型 '{service}'", file=sys.stderr)
            print(f"支持的服务类型：{', '.join(SERVICE_PATHS.keys())}", file=sys.stderr)
            sys.exit(1)
        
        # 使用通配符形式匹配所有服务的日志
        path = SERVICE_PATHS[service]['default']
        conditions.append(f'path: "{path}"')
    
    # 添加消息关键词条件
    if message:
        # 如果消息中没有通配符，自动添加
        if '*' not in message:
            message = f'*{message}*'
        conditions.append(f'message: "{message}"')
    
    # 组合所有条件
    kql = ' AND '.join(conditions)
    
    return kql


def print_query_info(kql, level=None, service=None, module=None):
    """打印查询信息"""
    print("\n=== KQL 查询语句 ===")
    print(f"\n{kql}\n")
    
    print("=== 查询说明 ===")
    if level:
        print(f"日志级别: {level.upper()}")
    if service:
        service_name = "GSE（管控平台）" if service == 'gse' else "CMDB（配置平台）"
        print(f"服务类型: {service_name}")
    if module:
        print(f"服务模块: {module}")
    
    print("\n=== 推荐参数 ===")
    
    # 根据查询类型推荐参数
    if level == 'ERROR':
        print("timeRange: 1d（查询最近 1 天）")
        print("size: 50（错误日志通常不多）")
        print("asc: false（最新错误在前）")
    elif service:
        print("timeRange: 1h（外部调用通常在最近时间）")
        print("size: 100（可能有多次调用）")
        print("asc: false（最新调用在前）")
    else:
        print("timeRange: 1d（查询最近 1 天）")
        print("size: 50（适中的返回量）")
        print("asc: false（最新日志在前）")
    
    print()


def main():
    parser = argparse.ArgumentParser(
        description='生成 KQL 查询语句',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__
    )
    
    parser.add_argument('--request-id', required=True, help='请求 ID')
    parser.add_argument('--level', choices=['ERROR', 'WARN', 'INFO', 'DEBUG'], help='日志级别')
    parser.add_argument('--service', choices=['gse', 'cmdb'], help='服务类型')
    parser.add_argument('--module', help='服务模块（job-execute, job-manage 等）')
    parser.add_argument('--message', help='消息关键词')
    
    args = parser.parse_args()
    
    # 生成 KQL
    kql = generate_kql(
        request_id=args.request_id,
        level=args.level,
        service=args.service,
        module=args.module,
        message=args.message
    )
    
    # 打印查询信息
    print_query_info(kql, args.level, args.service, args.module)


if __name__ == '__main__':
    main()
