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

package com.tencent.bk.job.common.util.toggle.feature;

/**
 * 特性ID定义
 */
public interface FeatureIdConstants {
    /**
     * 特性: 任务下发对接 GSE2.0
     */
    String FEATURE_GSE_V2 = "gseV2";
    /**
     * 特性: Agent状态对接 GSE2.0
     */
    String FEATURE_AGENT_STATUS_GSE_V2 = "agentStatusGseV2";
    /**
     * 特性: OpenAPI 兼容bk_biz_id参数
     */
    String FEATURE_BK_BIZ_ID_COMPATIBLE = "bkBizIdCompatible";
    /**
     * 特性-第三方文件源
     */
    String FEATURE_FILE_MANAGE = "fileManage";

    /**
     * 特性-是否支持GSE 获取文件分发任务结果的API协议(2.0版本之前)
     */
    String GSE_FILE_PROTOCOL_BEFORE_V2 = "gseFileProtocolBeforeV2";
    /**
     * 特性: 执行对象
     */
    String FEATURE_EXECUTE_OBJECT = "executeObject";
    /**
     * 特性: 容器执行
     */
    String FEATURE_CONTAINER_EXECUTE = "containerExecution";
    /**
     * 特性: 作业执行增加db 所有表增加 task_instance_id 字段作为分库分表 shard_key
     */
    String DAO_ADD_TASK_INSTANCE_ID = "daoAddTaskInstanceId";
}
