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

package com.tencent.bk.job.common.sharding.mysql;

/**
 * 分布式 ID KEY
 */
public class SegmentIdKeys {

    public static final String KEY_JOB_EXECUTE_TASK_INSTANCE = "job_execute.task_instance";
    public static final String KEY_JOB_EXECUTE_STEP_INSTANCE = "job_execute.step_instance";
    public static final String KEY_JOB_EXECUTE_GSE_TASK = "job_execute.gse_task";
    public static final String KEY_JOB_EXECUTE_OPERATION_LOG = "job_execute.operation_log";
    public static final String KEY_JOB_EXECUTE_FILE_SOURCE_TASK_LOG = "job_execute.file_source_task_log";
    public static final String KEY_JOB_EXECUTE_GSE_FILE_EXECUTE_OBJ_TASK = "job_execute.gse_file_execute_obj_task";
    public static final String KEY_JOB_EXECUTE_GSE_SCRIPT_EXECUTE_OBJ_TASK =
        "job_execute.gse_script_execute_obj_task";
    public static final String KEY_JOB_EXECUTE_ROLLING_CONFIG = "job_execute.rolling_config";
    public static final String KEY_JOB_EXECUTE_STEP_INSTANCE_ROLLING_TASK =
        "job_execute.step_instance_rolling_task";
    public static final String KEY_JOB_EXECUTE_STEP_INSTANCE_VARIABLE = "job_execute.step_instance_variable";
    public static final String KEY_JOB_EXECUTE_TASK_INSTANCE_VARIABLE = "job_execute.task_instance_variable";
}
