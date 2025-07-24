/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.api.v3.constants;

/**
 * V3 版本 API URL
 */
public interface APIV3Urls {
    String CREATE_SCRIPT = "/api/job/v3/job-manage/create_script";
    String CREATE_PUBLIC_SCRIPT = "/api/job/v3/job-manage/create_public_script";
    String GET_SCRIPT_LIST = "/api/job/v3/job-manage/get_script_list";
    String UPDATE_SCRIPT_BASIC = "/api/job/v3/job-manage/update_script_basic";
    String UPDATE_PUBLIC_SCRIPT_BASIC = "/api/job/v3/job-manage/update_public_script_basic";
    String DELETE_SCRIPT = "/api/job/v3/job-manage/delete_script";
    String DELETE_PUBLIC_SCRIPT = "/api/job/v3/job-manage/delete_public_script";
    String CREATE_SCRIPT_VERSION = "/api/job/v3/job-manage/create_script_version";
    String CREATE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/create_public_script_version";
    String GET_SCRIPT_VERSION_LIST = "/api/job/v3/job-manage/get_script_version_list";
    String GET_PUBLIC_SCRIPT_VERSION_LIST = "/api/job/v3/job-manage/get_public_script_version_list";
    String GET_SCRIPT_VERSION_DETAIL = "/api/job/v3/job-manage/get_script_version_detail";
    String UPDATE_SCRIPT_VERSION = "/api/job/v3/job-manage/update_script_version";
    String UPDATE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/update_public_script_version";
    String DELETE_SCRIPT_VERSION = "/api/job/v3/job-manage/delete_script_version";
    String DELETE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/delete_public_script_version";
    String PUBLISH_SCRIPT_VERSION = "/api/job/v3/job-manage/publish_script_version";
    String PUBLISH_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/publish_public_script_version";
    String DISABLE_SCRIPT_VERSION = "/api/job/v3/job-manage/disable_script_version";
    String DISABLE_PUBLIC_SCRIPT_VERSION = "/api/job/v3/job-manage/disable_public_script_version";

    String CREATE_DANGEROUS_RULE = "/api/job/v3/job-manage/create_dangerous_rule";
    String DELETE_DANGEROUS_RULE = "/api/job/v3/job-manage/delete_dangerous_rule";
    String GET_DANGEROUS_RULE_LIST = "/api/job/v3/job-manage/get_dangerous_rule_list";
    String UPDATE_DANGEROUS_RULE = "/api/job/v3/job-manage/update_dangerous_rule";
    String ENABLE_DANGEROUS_RULE = "/api/job/v3/job-manage/enable_dangerous_rule";
    String DISABLE_DANGEROUS_RULE = "/api/job/v3/job-manage/disable_dangerous_rule";
    String CHECK_SCRIPT = "/api/job/v3/job-manage/check_script";

    String QUERY_AGENT_INFO = "/api/job/v3/job-manage/query_agent_info";

    String FAST_EXECUTE_SCRIPT = "/api/job/v3/job-execute/fast_execute_script";
    String FAST_TRANSFER_FILE = "/api/job/v3/job-execute/fast_transfer_file";
    String EXECUTE_JOB_PLAN = "/api/job/v3/job-execute/execute_job_plan";
    String GET_STEP_INSTANCE_DETAIL = "/api/job/v3/job-execute/get_step_instance_detail";
    String GET_STEP_INSTANCE_STATUS = "/api/job/v3/job-execute/get_step_instance_status";

    String SAVE_CRON = "/api/job/v3/job-crontab/save_cron";
    String GET_CRON_LIST = "/api/job/v3/job-crontab/get_cron_list";
    String UPDATE_CRON_STATUS = "/api/job/v3/job-crontab/update_cron_status";
    String GET_CRON_DETAIL = "/api/job/v3/job-crontab/get_cron_detail";
    String DELETE_CRON = "/api/job/v3/job-crontab/delete_cron";


    String GET_JOB_PLAN_LIST = "/api/job/v3/job-manage/get_job_plan_list";

    String CREATE_FILE_SOURCE = "/api/job/v3/job-file-gateway/create_file_source";
    String UPDATE_FILE_SOURCE = "/api/job/v3/job-file-gateway/update_file_source";
    String GET_FILE_SOURCE_DETAIL = "/api/job/v3/job-file-gateway/get_file_source_detail";

    String CREATE_CREDENTIAL = "/api/job/v3/job-manage/create_credential";
    String UPDATE_CREDENTIAL = "/api/job/v3/job-manage/update_credential";
    String GET_CREDENTIAL_DETAIL = "/api/job/v3/job-manage/get_credential_detail";
}
