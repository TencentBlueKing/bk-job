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

package com.tencent.bk.job.common.statistics.consts;

import java.util.Collections;
import java.util.List;

public class StatisticsConstants {
    // 特殊值
    // 数据开始日期
    public static final String KEY_DATA_START_DATE = "STATISTICS_DATA_START_DATE";
    // 数据更新时间
    public static final String KEY_DATA_UPDATE_TIME = "STATISTICS_DATA_UPDATE_TIME";
    // 存储到DB的日期格式
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    // 某些跨业务的统计量的appId填默认值
    public static final Long DEFAULT_APP_ID = -1L;
    // 查询普通业务时需要排除的全局性质的appId列表
    public static final List<Long> GLOBAL_APP_ID_LIST = Collections.singletonList(DEFAULT_APP_ID);

    // metric name
    public static final String NAME_STATISTICS_TASK_SCHEDULE_POOL_SIZE = "statisticsTask.schedule.pool.size";
    public static final String NAME_STATISTICS_TASK_SCHEDULE_QUEUE_SIZE = "statisticsTask.schedule.queue.size";
    public static final String NAME_STATISTICS_TASK_ARRANGED_TASK_NUM = "statisticsTask.arrangedTask.num";
    public static final String NAME_STATISTICS_TASK_REJECTED_TASK_NUM = "statisticsTask.rejectedTask.num";

    // tag
    public static final String TAG_MODULE = "module";

    // value
    public static final String VALUE_MODULE_STATISTICS_TASK = "statisticsTask";

    // Resource
    // 全局资源
    public static final String RESOURCE_GLOBAL = "global";
    // 业务
    public static final String RESOURCE_APP = "app";
    // 所有业务的主机
    public static final String RESOURCE_HOST_OF_ALL_APP = "hostOfAllApp";
    // 所有业务的账号
    public static final String RESOURCE_ACCOUNT_OF_ALL_APP = "accountOfAllApp";
    // 所有业务一天的任务执行量
    public static final String RESOURCE_ONE_DAY_EXECUTED_TASK_OF_ALL_APP = "oneDayExecutedTaskOfAllApp";
    // 所有业务一天的失败任务量
    public static final String RESOURCE_ONE_DAY_FAILED_TASK_OF_ALL_APP = "oneDayFailedTaskOfAllApp";
    // 所有业务一天的脚本执行量
    public static final String RESOURCE_ONE_DAY_EXECUTED_FAST_SCRIPT_OF_ALL_APP = "oneDayExecutedFastScriptOfAllApp";
    // 所有业务一天的文件分发量
    public static final String RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP = "oneDayExecutedFastFileOfAllApp";
    // 某个业务的主机
    public static final String RESOURCE_HOST = "host";
    // 执行过的任务（含快速脚本、快速文件、作业）
    public static final String RESOURCE_EXECUTED_TASK = "executedTask";
    // 失败的任务（含快速脚本、快速文件、作业）
    public static final String RESOURCE_FAILED_TASK = "failedTask";
    // 执行过的快速执行脚本
    public static final String RESOURCE_EXECUTED_FAST_SCRIPT = "executedFastScript";
    // 执行过的快速分发文件
    public static final String RESOURCE_EXECUTED_FAST_FILE = "executedFastFile";
    // 作业模板步骤
    public static final String RESOURCE_TASK_TEMPLATE_STEP = "taskTemplateStep";
    // 脚本
    public static final String RESOURCE_SCRIPT = "script";
    // 脚本引用信息
    public static final String RESOURCE_SCRIPT_CITE_INFO = "scriptCiteInfo";
    // 脚本版本
    public static final String RESOURCE_SCRIPT_VERSION = "scriptVersion";
    // 定时任务
    public static final String RESOURCE_CRON = "cron";
    // 标签
    public static final String RESOURCE_TAG = "tag";

    // Dimension
    // 维度：全局资源统计类型
    public static final String DIMENSION_GLOBAL_STATISTIC_TYPE = "globalStatisticType";
    // 维度：业务统计类型
    public static final String DIMENSION_APP_STATISTIC_TYPE = "appStatisticType";
    // 查分布使用的维度信息，复用分布指标枚举
    // 维度：主机系统类型
    public static final String DIMENSION_HOST_SYSTEM_TYPE = "HOST_SYSTEM_TYPE";
    // 维度：作业步骤类型
    public static final String DIMENSION_STEP_TYPE = "STEP_TYPE";
    // 维度：脚本类型
    public static final String DIMENSION_SCRIPT_TYPE = "SCRIPT_TYPE";
    // 维度：脚本引用信息
    public static final String DIMENSION_SCRIPT_CITE_INFO_METRIC = "scriptCiteInfoMetric";
    // 维度：脚本版本状态
    public static final String DIMENSION_SCRIPT_VERSION_STATUS = "SCRIPT_VERSION_STATUS";
    // 维度：定时任务状态
    public static final String DIMENSION_CRON_STATUS = "CRON_STATUS";
    // 维度：定时任务类型
    public static final String DIMENSION_CRON_TYPE = "CRON_TYPE";
    // 维度：标签统计类型
    public static final String DIMENSION_TAG_STATISTIC_TYPE = "tagStatisticType";
    // 维度：账号类型
    public static final String DIMENSION_ACCOUNT_TYPE = "ACCOUNT_TYPE";

    // 维度：时间单元
    public static final String DIMENSION_TIME_UNIT = "timeUnit";
    // 维度：任务启动方式
    public static final String DIMENSION_TASK_STARTUP_MODE = "taskStartupMode";
    // 维度：任务类型
    public static final String DIMENSION_TASK_TYPE = "taskType";
    // 维度：执行耗时
    public static final String DIMENSION_TASK_TIME_CONSUMING = "taskTimeConsuming";
    // 维度：步骤运行状态
    public static final String DIMENSION_STEP_RUN_STATUS = "stepRunStatus";
    // 维度：文件传输模式
    public static final String DIMENSION_FILE_TRANSFER_MODE = "fileTransferMode";
    // 维度：源文件类型
    public static final String DIMENSION_FILE_SOURCE_TYPE = "fileSourceType";

    // Resource
    // 维度取值：时间单元-天
    public static final String DIMENSION_VALUE_TIME_UNIT_DAY = "DAY";
    // 维度取值：全局资源统计类型
    public static final String DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX = "";
    // 维度取值：业务统计类型
    public static final String DIMENSION_VALUE_APP_STATISTIC_TYPE_APP_LIST = "APP_LIST";
    public static final String DIMENSION_VALUE_APP_STATISTIC_TYPE_ACTIVE_APP_LIST = "ACTIVE_APP_LIST";
    // 维度取值：账号类型
    public static final String DIMENSION_VALUE_ACCOUNT_TYPE_LINUX = "LINUX";
    public static final String DIMENSION_VALUE_ACCOUNT_TYPE_WINDOWS = "WINDOWS";
    public static final String DIMENSION_VALUE_ACCOUNT_TYPE_DB = "DB";
    // 维度取值：主机系统类型
    public static final String DIMENSION_VALUE_HOST_SYSTEM_TYPE_LINUX = "LINUX";
    public static final String DIMENSION_VALUE_HOST_SYSTEM_TYPE_WINDOWS = "WINDOWS";
    public static final String DIMENSION_VALUE_HOST_SYSTEM_TYPE_AIX = "AIX";
    public static final String DIMENSION_VALUE_HOST_SYSTEM_TYPE_OTHERS = "OTHERS";
    // 维度取值：任务启动方式
    public static final String DIMENSION_VALUE_TASK_STARTUP_MODE_NORMAL = "NORMAL";
    public static final String DIMENSION_VALUE_TASK_STARTUP_MODE_API = "API";
    public static final String DIMENSION_VALUE_TASK_STARTUP_MODE_CRON = "CRON";
    // 维度取值：任务类型
    public static final String DIMENSION_VALUE_TASK_TYPE_FAST_EXECUTE_SCRIPT = "FAST_EXECUTE_SCRIPT";
    public static final String DIMENSION_VALUE_TASK_TYPE_FAST_PUSH_FILE = "FAST_PUSH_FILE";
    public static final String DIMENSION_VALUE_TASK_TYPE_EXECUTE_TASK = "EXECUTE_TASK";
    // 维度取值：执行耗时
    public static final String DIMENSION_VALUE_TASK_TIME_CONSUMING_LESS_THAN_ONE_MIN = "LESS_THAN_ONE_MIN";
    public static final String DIMENSION_VALUE_TASK_TIME_CONSUMING_ONE_MIN_TO_TEN_MIN = "ONE_MIN_TO_TEN_MIN";
    public static final String DIMENSION_VALUE_TASK_TIME_CONSUMING_OVER_TEN_MIN = "OVER_TEN_MIN";
    // 维度取值：步骤运行状态
    public static final String DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS = "SUCCESS";
    public static final String DIMENSION_VALUE_STEP_RUN_STATUS_FAIL = "FAIL";
    public static final String DIMENSION_VALUE_STEP_RUN_STATUS_EXCEPTION = "EXCEPTION";
    // 维度取值前缀：作业模板步骤类型
    public static final String DIMENSION_VALUE_STEP_TYPE_PREFIX = "";
    // 维度取值前缀：脚本类型
    public static final String DIMENSION_VALUE_SCRIPT_TYPE_PREFIX = "";
    // 维度取值：脚本引用信息
    public static final String DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_SCRIPT_COUNT = "SCRIPT_COUNT";
    public static final String DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_COUNT = "CITED_SCRIPT_COUNT";
    public static final String DIMENSION_VALUE_SCRIPT_CITE_INFO_METRIC_CITED_SCRIPT_STEP_COUNT = 
        "CITED_SCRIPT_STEP_COUNT";
    // 维度取值前缀：脚本版本状态
    public static final String DIMENSION_VALUE_SCRIPT_VERSION_STATUS_PREFIX = "";
    // 维度取值：文件传输模式
    public static final String DIMENSION_VALUE_FILE_TRANSFER_MODE_STRICT = "STRICT";
    public static final String DIMENSION_VALUE_FILE_TRANSFER_MODE_FORCE = "FORCE";
    // 维度取值：源文件类型
    public static final String DIMENSION_VALUE_FILE_SOURCE_TYPE_LOCAL = "LOCAL";
    public static final String DIMENSION_VALUE_FILE_SOURCE_TYPE_SERVER = "SERVER";
    // 维度取值：定时任务状态
    public static final String DIMENSION_VALUE_CRON_STATUS_OPEN = "OPEN";
    public static final String DIMENSION_VALUE_CRON_STATUS_CLOSED = "CLOSED";
    // 维度取值：定时任务类型
    public static final String DIMENSION_VALUE_CRON_TYPE_SIMPLE = "SIMPLE";
    public static final String DIMENSION_VALUE_CRON_TYPE_CRON = "CRON";
    // 维度取值：标签统计类型
    public static final String DIMENSION_VALUE_TAG_STATISTIC_TYPE_DISTRIBUTION_MAP = "DISTRIBUTION_MAP";

}
