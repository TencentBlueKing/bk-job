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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiJobTemplateV4Resource;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiJobTemplateV4ResourceImpl implements OpenApiJobTemplateV4Resource {

    private final TaskTemplateService templateService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public OpenApiJobTemplateV4ResourceImpl(TaskTemplateService templateService,
                                            AppScopeMappingService appScopeMappingService) {
        this.templateService = templateService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_JOB_TEMPLATE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_job_template_detail"})
    public EsbV4Response<OpenApiV4JobTemplateDetailDTO> getJobTemplateDetail(String username,
                                                                             String appCode,
                                                                             String scopeType,
                                                                             String scopeId,
                                                                             Long jobTemplateId) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        User user = JobContextUtil.getUser();
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplate(user, appId, jobTemplateId);
        OpenApiV4JobTemplateDetailDTO data = OpenApiV4JobTemplateConverter.toDetailDTO(
            templateInfo, appScopeMappingService
        );
        return EsbV4Response.success(data);
    }
}
