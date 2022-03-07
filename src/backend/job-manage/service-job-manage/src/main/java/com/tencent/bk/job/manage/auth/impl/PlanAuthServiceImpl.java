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

package com.tencent.bk.job.manage.auth.impl;

import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 执行方案相关操作鉴权接口
 */
@Service
public class PlanAuthServiceImpl implements PlanAuthService {
    @Override
    public AuthResult authCreateJobPlan(String username,
                                        AppResourceScope appResourceScope,
                                        Long jobTemplateId,
                                        String jobTemplateName) {
        // TODO
        return AuthResult.pass();
    }

    @Override
    public AuthResult authViewJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        // TODO
        return AuthResult.pass();
    }

    @Override
    public AuthResult authEditJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        // TODO
        return AuthResult.pass();
    }

    @Override
    public AuthResult authDeleteJobPlan(String username,
                                        AppResourceScope appResourceScope,
                                        Long jobTemplateId,
                                        Long jobPlanId,
                                        String jobPlanName) {
        // TODO
        return AuthResult.pass();
    }

    @Override
    public AuthResult authSyncJobPlan(String username,
                                      AppResourceScope appResourceScope,
                                      Long jobTemplateId,
                                      Long jobPlanId,
                                      String jobPlanName) {
        // TODO
        return AuthResult.pass();
    }

    @Override
    public List<Long> batchAuthViewJobPlan(String username,
                                           AppResourceScope appResourceScope,
                                           List<Long> jobTemplateIdList,
                                           List<Long> jobPlanIdList) {
        // TODO
        return jobPlanIdList;
    }

    @Override
    public List<Long> batchAuthEditJobPlan(String username,
                                           AppResourceScope appResourceScope,
                                           List<Long> jobTemplateIdList,
                                           List<Long> jobPlanIdList) {
        // TODO
        return jobPlanIdList;
    }

    @Override
    public List<Long> batchAuthDeleteJobPlan(String username,
                                             AppResourceScope appResourceScope,
                                             List<Long> jobTemplateIdList,
                                             List<Long> jobPlanIdList) {
        // TODO
        return jobPlanIdList;
    }
}
