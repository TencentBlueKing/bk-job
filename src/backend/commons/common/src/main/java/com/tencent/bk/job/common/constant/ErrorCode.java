/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.common.constant;

/**
 * 蓝鲸标准化错误码
 * <p>
 * 错误码分类： 文件磁盘系统 59 MySQL系统 52 Redis系统 50 MQ 55 GSE 10 CMDB 11 作业平台 12 PAAS 13
 * </p>
 */
public class ErrorCode {

    // ==================================== 通用 ===================================================//
    /**
     * 成功
     */
    public static final int RESULT_OK = 0;

    // ==================================== 业务级错误 ================================================//
    /*
     * 业务通用-1241xxx
     * 配置服务-1242xxx
     * 作业管理-1243xxx
     * 作业执行-1244xxx
     * 定时作业-1245xxx
     * 日志服务-1246xxx
     * 用户服务-1247xxx
     * 业务网关-1248xxx
     * 备份服务-1249xxx
     * 文件网关-1260xxx
     * 文件代理-1261xxx
     * 文件Worker-1262xxx
     */

    // 业务通用 start
    // 请求参数缺失
    public static final int MISSING_PARAM = 1241001;
    // 请求参数不合法
    public static final int ILLEGAL_PARAM = 1241002;
    // 不支持的操作
    public static final int UNSUPPORTED_OPERATION = 1241003;
    // 请求参数[]缺失
    public static final int MISSING_PARAM_WITH_PARAM_NAME = 1241004;
    // 请求参数[]不合法
    public static final int ILLEGAL_PARAM_WITH_PARAM_NAME = 1241005;
    // 请求参数缺失或不合法
    public static final int MISSING_OR_ILLEGAL_PARAM = 1241006;
    // 请求参数[]缺失或不合法
    public static final int MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME = 1241007;
    // 错误的业务 ID
    public static final int WRONG_APP_ID = 1241008;
    // 请求参数[0]不合法，原因：[1]
    public static final int ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON = 1241010;
    // 尚未支持的功能
    public static final int NOT_SUPPORT_FEATURE = 1241011;

    public static final int ILLEGAL_PARAM_WITH_REASON = 1241012;
    // 该功能暂不支持业务集
    public static final int NOT_SUPPORT_FEATURE_FOR_BIZ_SET = 1241013;
    // IPv6地址不合法：{0}
    public static final int INVALID_IPV6_ADDRESS = 1241014;
    // 业务通用 end

    // 配置服务 start
    // 配置服务 end

    // 作业管理 start
    // 脚本不存在
    public static final int SCRIPT_NOT_EXIST = 1243001;
    // 脚本名称已存在
    public static final int SCRIPT_NAME_DUPLICATE = 1243002;
    // 脚本名称不合法
    public static final int SCRIPT_NAME_INVALID = 1243003;
    // 脚本不属于该业务
    public static final int SCRIPT_NOT_IN_APP = 1243004;
    // 已上线脚本不支持删除
    public static final int DELETE_ONLINE_SCRIPT_FAIL = 1243005;
    // 账号别名已存在
    public static final int ACCOUNT_ALIAS_EXIST = 1243007;
    // DB依赖的系统账号不合法
    public static final int DB_SYSTEM_ACCOUNT_IS_INVALID = 1243008;
    // 执行方案不存在
    public static final int TASK_PLAN_NOT_EXIST = 1243009;
    // 上传脚本文件名为空
    public static final int UPLOAD_SCRIPT_FILE_NAME_EMPTY = 1243010;
    // 上传脚本文件扩展名不合法
    public static final int UPLOAD_SCRIPT_EXT_TYPE_ILLEGAL = 1243011;
    // 上传脚本文件脚本内容不合法
    public static final int UPLOAD_SCRIPT_CONTENT_ILLEGAL = 1243012;
    // 默认通知策略未配置，请联系管理员
    public static final int DEFAULT_NOTIFY_POLICIES_NOT_CONFIGED = 1243013;
    // 账号被引用，禁止删除
    public static final int DELETE_REF_ACCOUNT_FORBIDDEN = 1243014;
    // 文件大小不能超过{}
    public static final int UPLOAD_FILE_MAX_SIZE_EXCEEDED = 1243015;
    // 新建 Tag 失败
    public static final int PROCESS_TAG_FAILED = 1243016;
    // 引用脚本未上线或已禁用或已删除
    public static final int SCRIPT_VERSION_ILLEGAL = 1243017;
    // 新建模版失败
    public static final int INSERT_TEMPLATE_FAILED = 1243018;
    // 更新模版失败
    public static final int UPDATE_TEMPLATE_FAILED = 1243019;
    // 保存模版发生未知错误
    public static final int SAVE_TEMPLATE_UNKNOWN_ERROR = 1243020;
    // Tag 已存在
    public static final int TAG_ALREADY_EXIST = 1243021;
    // 错误的变量类型
    public static final int WRONG_VARIABLE_TYPE = 1243022;
    // 新建执行方案失败
    public static final int INSERT_TASK_PLAN_FAILED = 1243023;
    // 更新执行方案失败
    public static final int UPDATE_TASK_PLAN_FAILED = 1243024;
    // 保存执行方案发生未知错误
    public static final int SAVE_TASK_PLAN_UNKNOWN_ERROR = 1243025;
    // 模版不存在
    public static final int TEMPLATE_NOT_EXIST = 1243026;
    // 创建调试模版失败
    public static final int CREATE_DEBUG_PLAN_ERROR = 1243027;
    // 同步执行方案失败
    public static final int SYNC_TASK_PLAN_UNKNOWN_ERROR = 1243028;
    // 不支持的步骤类型
    public static final int WRONG_STEP_TYPE = 1243029;
    // 批量操作失败
    public static final int BATCH_INSERT_FAILED = 1243030;
    // 不支持的任务类型
    public static final int WRONG_TASK_TYPE = 1243031;
    // 新建步骤失败
    public static final int CREATE_STEP_FAILED = 1243032;
    // 更新步骤失败
    public static final int UPDATE_STEP_FAILED = 1243033;
    // 删除步骤失败
    public static final int DELETE_STEP_FAILED = 1243034;
    // 更新文件信息失败
    public static final int UPDATE_FILE_INFO_FAILED = 1243035;
    // 脚本版本号已存在
    public static final int SCRIPT_VERSION_NAME_EXIST = 1243036;
    // 执行方案存在关联的定时任务
    public static final int DELETE_PLAN_FAILED_USING_BY_CRON = 1243037;
    // 模版的执行方案存在关联的定时任务
    public static final int DELETE_TEMPLATE_FAILED_PLAN_USING_BY_CRON = 1243038;
    // 脚本版本ID已存在
    public static final int SCRIPT_VERSION_ID_EXIST = 1243039;
    // 模版已存在
    public static final int TEMPLATE_NAME_EXIST = 1243040;
    // 获取模版编辑锁失败
    public static final int TEMPLATE_LOCK_ACQUIRE_FAILED = 1243041;
    // 执行方案名称重复
    public static final int PLAN_NAME_EXIST = 1243042;
    // 执行方案 ID 重复
    public static final int PLAN_ID_EXIST = 1243043;
    // 模版 ID 已存在
    public static final int TEMPLATE_ID_EXIST = 1243044;
    // 同步脚本失败
    public static final int SYNC_SCRIPT_UNKNOWN_ERROR = 1243045;
    // 批量更新变量值失败
    public static final int BATCH_UPDATE_PLAN_VARIABLE_FAILED = 1243046;
    // 脚本版本不存在
    public static final int SCRIPT_VERSION_NOT_EXIST = 1243047;
    // 凭证不存在
    public static final int CREDENTIAL_NOT_EXIST = 1243048;
    // 业务/业务集不存在
    public static final int APP_NOT_EXIST = 1243049;
    // 文件后缀不允许
    public static final int UPLOAD_FILE_SUFFIX_NOT_ALLOW = 1243050;
    // 资源范围不存在:{0}
    public static final int SCOPE_NOT_EXIST = 1243051;
    // IP（含云区域ID）在CMDB中不存在:{0}
    public static final int IP_NOT_EXIST_IN_CMDB = 1243052;
    // 主机ID在CMDB中不存在:{0}
    public static final int HOST_ID_NOT_EXIST_IN_CMDB = 1243053;
    // 作业管理 end

    // 作业执行 start
    // 主机为空
    public static final int SERVER_EMPTY = 1244001;
    // 主机[{0}]无效，请检查源或目标主机的IPv4或AgentID字段值是否存在于配置平台；另外，主机需跨业务执行请联系作业平台管理员将其添加到主机白名单。
    public static final int HOST_INVALID = 1244002;
    // 账号不存在
    public static final int ACCOUNT_NOT_EXIST = 1244003;
    // 没有该账号的权限
    public static final int ACCOUNT_NO_PERMISSION = 1244004;
    // 启动作业失败
    public static final int STARTUP_TASK_FAIL = 1244005;
    // 作业实例不存在
    public static final int TASK_INSTANCE_NOT_EXIST = 1244006;
    // 作业步骤实例不存在
    public static final int STEP_INSTANCE_NOT_EXIST = 1244007;
    // 作业引用的主机列表变量[{}]不存在
    public static final int TASK_INSTANCE_RELATED_HOST_VAR_NOT_EXIST = 1244008;
    // 作业引用的主机列表变量[{}]的主机列表为空
    public static final int TASK_INSTANCE_RELATED_HOST_VAR_SERVER_EMPTY = 1244009;
    // 导出日志文件失败
    public static final int EXPORT_STEP_EXECUTION_LOG_FAIL = 1244010;
    // 执行方案不合法
    public static final int EXECUTE_TASK_PLAN_ILLEGAL = 1244011;
    // 执行方案不存在
    public static final int EXECUTE_TASK_PLAN_NOT_EXIST = 1244012;
    // 下载执行日志文件失败
    public static final int DOWNLOAD_LOG_FILE_FAIL = 1244013;
    // 脚本已禁用，无法执行
    public static final int SCRIPT_DISABLED_SHOULD_NOT_EXECUTE = 1244014;
    // 任务正在终止中，请不要重复操作
    public static final int TASK_STOPPING_DO_NOT_REPEAT = 1244015;
    // 只有步骤的确认人可以操作
    public static final int NOT_IN_CONFIRM_USER_LIST = 1244016;
    // 获取主机失败
    public static final int OBTAIN_HOST_FAIL = 1244017;
    // 脚本状态[]，不支持执行
    public static final int SCRIPT_NOT_EXECUTABLE_STATUS = 1244018;
    // 高危脚本
    public static final int DANGEROUS_SCRIPT_FORBIDDEN_EXECUTION = 1244019;
    // 作业执行历史查询时间范围必须小于30天
    public static final int TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS = 1244020;
    public static final int FILE_TASKS_EXCEEDS_LIMIT = 1244021;
    public static final int SCRIPT_TASK_TARGET_SERVER_EXCEEDS_LIMIT = 1244022;
    // 本地文件{0}在后台不存在（本地/制品库）
    public static final int LOCAL_FILE_NOT_EXIST_IN_BACKEND = 1244023;
    // 保存文件到本地失败
    public static final int FAIL_TO_SAVE_FILE_TO_LOCAL = 1244024;
    // 任务被丢弃
    public static final int TASK_ABANDONED = 1244025;
    // 非法的滚动策略
    public static final int INVALID_ROLLING_EXPR = 1244026;
    // 滚动批次不能大于{}
    public static final int EXCEED_MAX_ALLOWED_BATCH_SIZE = 1244027;
    // 作业执行 end

    // 定时作业 start
    public static final int CRON_JOB_NOT_EXIST = 1245001;
    public static final int UPDATE_CRON_JOB_FAILED = 1245002;
    public static final int INSERT_CRON_JOB_FAILED = 1245003;
    public static final int CRON_JOB_ALREADY_EXIST = 1245004;
    public static final int ACQUIRE_CRON_JOB_LOCK_FAILED = 1245005;
    public static final int CRON_JOB_TIME_PASSED = 1245006;
    public static final int END_TIME_OR_NOTIFY_TIME_ALREADY_PASSED = 1245007;
    // 定时作业 end

    // 日志服务
    // 获取作业执执行日志失败
    public static final int SAVE_JOB_EXECUTION_LOG_FAIL = 1246002;
    // 保存作业执执行日志失败
    public static final int DELETE_JOB_EXECUTION_LOG_FAIL = 1246003;
    // 删除作业执执行日志失败
    public static final int GET_JOB_EXECUTION_LOG_FAIL = 1246001;

    // 用户服务 start
    // 用户不存在或者未登录
    public static final int USER_NOT_EXIST_OR_NOT_LOGIN_IN = 1247001;
    // 用户服务 end

    // 业务网关 start
    // 业务网关 end

    // 备份服务 start
    // 从制品库获取节点信息失败
    public static final int FAIL_TO_GET_NODE_INFO_FROM_ARTIFACTORY = 1249001;
    // 从制品库下载文件失败
    public static final int FAIL_TO_DOWNLOAD_NODE_FROM_ARTIFACTORY = 1249002;
    // 备份服务 end

    // 文件网关 start
    // 文件源不存在:{0}
    public static final int FILE_SOURCE_NOT_EXIST = 1260001;
    // 接入点响应异常：ListFileNode，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_LIST_FILE_NODE = 1260002;
    // 接入点响应异常：FileAvailable，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_FILE_AVAILABLE = 1260003;
    // 接入点响应异常：ExecuteAction，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_EXECUTE_ACTION = 1260008;
    // 接入点响应异常：DeleteBucketFile，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_DELETE_BUCKET_FILE = 1260009;

    // 文件源别名已存在：{0}
    public static final int FILE_SOURCE_ALIAS_ALREADY_EXISTS = 1260004;
    // 无法匹配到有效接入点，请检查文件源配置
    public static final int CAN_NOT_FIND_AVAILABLE_FILE_WORKER = 1260005;
    // 接入点响应异常：ClearTaskFiles，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_CLEAR_TASK_FILES = 1260006;
    // 接入点响应异常：StartFileSourceDownloadTask，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_START_FILE_SOURCE_DOWNLOAD_TASK = 1260007;
    // 接入点响应异常：StopTasks，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_STOP_TASKS = 1260011;
    // 不可删除文件未清空的Bucket
    public static final int CAN_NOT_DELETE_BUCKET_CONTAINS_FILES = 1260010;
    // 无法根据标识{0}找到文件源
    public static final int FAIL_TO_FIND_FILE_SOURCE_BY_CODE = 1260012;
    // 文件源服务异常
    public static final int FILE_SOURCE_SERVICE_INVALID = 1260013;
    // 通过[{0}]找不到file-worker
    public static final int FILE_WORKER_NOT_FOUND = 1260014;
    // 文件源标识已存在：{0}
    public static final int FILE_SOURCE_CODE_ALREADY_EXISTS = 1260015;
    // 文件源ID与标识至少指定一个
    public static final int ID_AND_CODE_AT_LEAST_ONE = 1260016;
    // 文件源[id={0}]不在业务/业务集下
    public static final int FILE_SOURCE_ID_NOT_IN_BIZ = 1260017;
    // 接入点响应异常，详情：{0}
    public static final int FAIL_TO_REQUEST_FILE_WORKER_WITH_REASON = 1260018;

    // 文件网关 end
    // 文件代理 start
    // 文件代理 end
    // 文件Worker start
    // 第三方文件源响应异常：ListBucket，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_LIST_BUCKET = 1262001;
    // 第三方文件源响应异常：ListObjects，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_LIST_OBJECTS = 1262002;
    // 第三方文件源响应异常：DeleteBucket，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DELETE_BUCKET = 1262003;
    // 第三方文件源响应异常：DeleteObject，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DELETE_OBJECT = 1262004;
    // 第三方文件源响应异常：DownloadGenericFile，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_DOWNLOAD_GENERIC_FILE = 1262005;
    // 第三方文件源响应异常：GetObject，详情：{0}
    public static final int FAIL_TO_REQUEST_THIRD_FILE_SOURCE_GET_OBJECT = 1262006;
    // 文件Worker end

    // 迁移升级
    // 迁移失败，任务: {0}, 详情: {1}
    public static final int MIGRATION_FAIL = 1263001;

    // ==================================== 系统级错误 ================================================//
    // ======== 系统错误-权限错误 ==================//
    // 用户({0})权限不足，请前往权限中心确认并申请补充后重试
    public static final int PERMISSION_DENIED = 1238001;
    // 蓝鲸统一权限错误码，用户({0})权限不足，请前往权限中心确认并申请补充后重试
    public static final int BK_PERMISSION_DENIED = 9900403;

    // ========= 系统错误-请求 ====================//
    // 内部服务异常
    public static final int INTERNAL_ERROR = 1240002;
    // 错误的请求
    public static final int BAD_REQUEST = 1240003;
    // Cookie过期或者不存在
    public static final int COOKIE_ILLEGAL = 1240004;
    // 服务不可用
    public static final int SERVICE_UNAVAILABLE = 1240001;
    // 服务认证失败
    public static final int SERVICE_AUTH_FAIL = 1240005;
    // 配置异常：{0}
    public static final int INVALID_CONFIG = 1240006;

    // ========= 系统错误-API通用 ==================//
    // IP:{}无访问权限
    public static final int API_IP_NO_ACCESS = 1239001;
    // 用户无访问权限
    public static final int API_USER_NO_ACCESS = 1239002;
    // 不支持的API接口
    public static final int API_UNSUPPORTED = 1239003;
    // 缺少请求报文或报文不合法
    public static final int API_PARAM_NULL_OR_ILLEGAL = 1239004;
    // 异步调用的方法超时了
    public static final int API_INVOKE_TIMEOUT = 1239005;
    // 调用方法出错了
    public static final int API_INVOKE_ERROR = 1239006;
    // API接口已经过期废弃
    public static final int API_DEPRECATED = 1239007;
    // API服务过载，拒绝服务请求
    public static final int API_OVER_LOAD = 1239008;
    // API回调其他接口失败
    public static final int API_CALLBACK_FAIL = 1239009;
    // Api.cert文件不可用
    public static final int API_CERT_ERROR = 1239010;

    // ======= 系统错误-公共组件错误 =======//
    // Redis服务不可用，连接不上 - IP不存在或者配置错误
    public static final int REDIS_CONNECT_FAIL = 1250001;
    // Redis服务内存满或者其他问题 - 内存不足够
    public static final int REDIS_DATA_EXCEPTION = 1250002;
    // NFS存储 不可用
    public static final int NFS_ERROR = 1259001;
    // DB 不可用
    public static final int DB_ERROR = 1252001;
    // MQ 不可用
    public static final int MQ_ERROR = 1255001;

    // ======= 系统错误-平台服务错误 =======//
    // LICENSE 不可用
    public static final int LICENSE_ERROR = 1210101;
    // GSE 不可用
    public static final int GSE_ERROR = 1210001;
    // GSE数据异常：{0}
    public static final int GSE_API_DATA_ERROR = 1210002;

    // CMDB错误
    // CMDB服务状态不可达 - 地址配置错误或者地址无法正确解析
    public static final int CMDB_UNREACHABLE_SERVER = 1211001;
    // CMDB接口返回数据结构异常- 一般是被网关防火墙重定向返回统一登录页面
    public static final int CMDB_API_DATA_ERROR = 1211002;
    // 根据动态分组ID查找主机失败，动态分组ID：{0}，原因：{1}，请确认指定的动态分组在业务下是否存在
    public static final int FAIL_TO_FIND_HOST_BY_DYNAMIC_GROUP = 1211003;
    // 根据业务ID查找动态分组失败，业务ID：{0}，原因：{1}，请确认指定的业务是否存在动态分组
    public static final int FAIL_TO_FIND_DYNAMIC_GROUP_BY_BIZ = 1211004;

    // PaaS异常
    // CMSI接口访问异常
    public static final int CMSI_API_ACCESS_ERROR = 1213001;
    // 用户管理接口访问异常
    public static final int USER_MANAGE_API_ACCESS_ERROR = 1213002;
    // 调用CMSI接口获取通知渠道数据异常
    public static final int CMSI_MSG_CHANNEL_DATA_ERROR = 1213003;
    // 调用CMSI接口发送通知失败，错误码：{0}，错误信息：{1}
    public static final int CMSI_FAIL_TO_SEND_MSG = 1213004;

    // 制品库异常
    // Artifactory接口返回数据结构异常
    public static final int ARTIFACTORY_API_DATA_ERROR = 1214001;
    // 制品库中找不到节点:{0}，请到制品库核实
    public static final int CAN_NOT_FIND_NODE_IN_ARTIFACTORY = 1214002;

    // IAM接口数据异常- 一般是被网关防火墙重定向返回统一登录页面
    public static final int IAM_API_DATA_ERROR = 1215001;

    // 第三方API请求错误
    public static final int API_ERROR = 1216001;

}
