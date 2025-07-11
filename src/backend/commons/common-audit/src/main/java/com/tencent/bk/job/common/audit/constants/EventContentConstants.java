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

package com.tencent.bk.job.common.audit.constants;

import com.tencent.bk.job.common.audit.JobAuditAttributeNames;

import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_ID;
import static com.tencent.bk.audit.constants.AuditAttributeNames.INSTANCE_NAME;
import static com.tencent.bk.job.common.audit.JobAuditAttributeNames.OPERATION;
import static com.tencent.bk.job.common.audit.JobAuditAttributeNames.SCRIPT_NAME;
import static com.tencent.bk.job.common.audit.JobAuditAttributeNames.SCRIPT_VERSION_ID;

/**
 * 审计事件描述
 */
public interface EventContentConstants {
    String INSTANCE_INFO = "[{{" + INSTANCE_NAME + "}}]({{" + INSTANCE_ID + "}})";

    String QUICK_EXECUTE_SCRIPT = "Run a quick script task";
    String QUICK_TRANSFER_FILE = "Run a quick transfer file task";
    String EXECUTE_SCRIPT = "Launch a script [{{" + SCRIPT_NAME
        + "}}]({{" + SCRIPT_VERSION_ID + "}})";
    String EXECUTE_PUBLIC_SCRIPT = "Launch a public script [{{" + SCRIPT_NAME
        + "}}]({{" + SCRIPT_VERSION_ID + "}})";

    String VIEW_JOB_INSTANCE = "View job task " + INSTANCE_INFO;

    String VIEW_CRON_JOB = "View cron task " + INSTANCE_INFO;
    String CREATE_CRON_JOB = "Create cron task " + INSTANCE_INFO;
    String EDIT_CRON_JOB = "Modify cron task " + INSTANCE_INFO;
    String DELETE_CRON_JOB = "Delete cron task " + INSTANCE_INFO;
    String SWITCH_CRON_JOB_STATUS = "{{" + OPERATION + "}} cron task " + INSTANCE_INFO;

    String VIEW_FILE_SOURCE = "View file source " + INSTANCE_INFO;
    String CREATE_FILE_SOURCE = "Create file source " + INSTANCE_INFO;
    String EDIT_FILE_SOURCE = "Modify file source " + INSTANCE_INFO;
    String DELETE_FILE_SOURCE = "Delete file source " + INSTANCE_INFO;
    String SWITCH_FILE_SOURCE_STATUS = "{{" + OPERATION + "}} file source " + INSTANCE_INFO;

    String USE_ACCOUNT = "View account " + INSTANCE_INFO;
    String CREATE_ACCOUNT = "Create account " + INSTANCE_INFO;
    String EDIT_ACCOUNT = "Modify account " + INSTANCE_INFO;
    String DELETE_ACCOUNT = "Delete account " + INSTANCE_INFO;

    String USE_TICKET = "View credential " + INSTANCE_INFO;
    String CREATE_TICKET = "Create credential " + INSTANCE_INFO;
    String EDIT_TICKET = "Modify credential " + INSTANCE_INFO;
    String DELETE_TICKET = "Delete credential " + INSTANCE_INFO;

    String VIEW_SCRIPT = "View script " + INSTANCE_INFO;
    String CREATE_SCRIPT = "Create script " + INSTANCE_INFO;
    String EDIT_SCRIPT = "Modify script " + INSTANCE_INFO;
    String DELETE_SCRIPT = "Delete script " + INSTANCE_INFO;
    String CREATE_SCRIPT_VERSION = "Create a new script version " + INSTANCE_INFO + "({{@VERSION}})";
    String EDIT_SCRIPT_VERSION = "Modify script version " + INSTANCE_INFO + "({{@VERSION}})";
    String DELETE_SCRIPT_VERSION = "Delete script version " + INSTANCE_INFO + "({{@VERSION}})";
    String FORBIDDEN_SCRIPT_VERSION = "Set script version " + INSTANCE_INFO + "({{@VERSION}}) state to [forbidden]";
    String ONLINE_SCRIPT_VERSION = "Set script version " + INSTANCE_INFO + "({{@VERSION}}) state to [online]";

    String CREATE_PUBLIC_SCRIPT = "Create public script " + INSTANCE_INFO;
    String EDIT_PUBLIC_SCRIPT = "Modify public script " + INSTANCE_INFO;
    String DELETE_PUBLIC_SCRIPT = "Delete public script " + INSTANCE_INFO;
    String CREATE_PUBLIC_SCRIPT_VERSION = "Create a new public script version " + INSTANCE_INFO + "({{@VERSION}})";
    String EDIT_PUBLIC_SCRIPT_VERSION = "Modify public script version " + INSTANCE_INFO + "({{@VERSION}})";
    String DELETE_PUBLIC_SCRIPT_VERSION = "Delete public script version " + INSTANCE_INFO + "({{@VERSION}})";
    String FORBIDDEN_PUBLIC_SCRIPT_VERSION = "Set public script version " + INSTANCE_INFO + "({{@VERSION}}) state to " +
        "[forbidden]";
    String ONLINE_PUBLIC_SCRIPT_VERSION = "Set public script version " + INSTANCE_INFO + "({{@VERSION}}) state to " +
        "[online]";

    String CREATE_TAG = "Create tag " + INSTANCE_INFO;
    String EDIT_TAG = "Modify tag " + INSTANCE_INFO;
    String DELETE_TAG = "Delete tag " + INSTANCE_INFO;

    String VIEW_JOB_TEMPLATE = "View job template " + INSTANCE_INFO;
    String CREATE_JOB_TEMPLATE = "Create job template " + INSTANCE_INFO;
    String EDIT_JOB_TEMPLATE = "Modify job template " + INSTANCE_INFO;
    String DELETE_JOB_TEMPLATE = "Delete job template " + INSTANCE_INFO;
    String DEBUG_JOB_TEMPLATE = "Debug job template " + INSTANCE_INFO;

    String CREATE_JOB_PLAN = "Create job plan " + INSTANCE_INFO;
    String VIEW_JOB_PLAN = "View job plan " + INSTANCE_INFO;
    String EDIT_JOB_PLAN = "Modify job plan " + INSTANCE_INFO;
    String DELETE_JOB_PLAN = "Delete job plan " + INSTANCE_INFO;
    String SYNC_JOB_PLAN = "Sync job plan " + INSTANCE_INFO;
    String LAUNCH_JOB_PLAN = "Launch a plan [{{" + JobAuditAttributeNames.PLAN_NAME
        + "}}]({{" + JobAuditAttributeNames.PLAN_ID + "}})";

    String CREATE_HIGH_RISK_DETECT_RULE = "Create a new high-risk detect rule";
    String VIEW_HIGH_RISK_DETECT_RULE = "View high-risk detect rule settings";
    String EDIT_HIGH_RISK_DETECT_RULE = "Modify a new high-risk detect rule";
    String DELETE_HIGH_RISK_DETECT_RULE = "DELETE a new high-risk detect rule";

    String VIEW_GLOBAL_SETTINGS = "View the global settings";
    String EDIT_GLOBAL_SETTINGS = "Modify the global settings";

    String VIEW_ANALYSIS_DASHBOARD = "View the analysis dashboard";

    String VIEW_PLATFORM_SERVICE_STAT = "View the platform service states";


    String VIEW_WHITE_LIST = "View the white list";
    String EDIT_WHITE_LIST = "Modify a white list row";

    String VIEW_HIGH_RISK_DETECT_RECORD = "View the high-risk detect record";

    String EDIT_BUSINESS_NOTIFY_SETTINGS = "Modify business notification settings";


}
