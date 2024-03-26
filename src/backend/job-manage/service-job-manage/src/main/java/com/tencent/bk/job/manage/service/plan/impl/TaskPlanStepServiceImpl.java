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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.job.manage.api.common.constants.task.TaskTypeEnum;
import com.tencent.bk.job.manage.dao.ScriptDAO;
import com.tencent.bk.job.manage.dao.TaskApprovalStepDAO;
import com.tencent.bk.job.manage.dao.TaskFileInfoDAO;
import com.tencent.bk.job.manage.dao.TaskFileStepDAO;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.dao.TaskStepDAO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @since 16/10/2019 19:57
 */
@Service("TaskPlanStepServiceImpl")
public class TaskPlanStepServiceImpl extends AbstractTaskStepService {

    @Autowired
    public TaskPlanStepServiceImpl(@Qualifier("TaskPlanStepDAOImpl") TaskStepDAO taskStepDAO,
                                   @Qualifier("TaskPlanScriptStepDAOImpl") TaskScriptStepDAO taskScriptStepDAO,
                                   @Qualifier("TaskPlanFileStepDAOImpl") TaskFileStepDAO taskFileStepDAO,
                                   @Qualifier("TaskPlanApprovalStepDAOImpl") TaskApprovalStepDAO taskApprovalStepDAO,
                                   @Qualifier("TaskPlanFileInfoDAOImpl") TaskFileInfoDAO taskFileInfoDAO,
                                   @Autowired ScriptDAO scriptDAO) {
        this.taskStepDAO = taskStepDAO;
        this.taskScriptStepDAO = taskScriptStepDAO;
        this.taskFileStepDAO = taskFileStepDAO;
        this.taskApprovalStepDAO = taskApprovalStepDAO;
        this.taskFileInfoDAO = taskFileInfoDAO;
        this.scriptDAO = scriptDAO;
        this.taskType = TaskTypeEnum.PLAN;
    }
}
