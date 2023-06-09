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

package com.tencent.bk.job.analysis.service.remote;

import com.tencent.bk.job.analysis.service.TaskTemplateService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service("jobAnalysisTaskTemplateServiceImpl")
public class TaskTemplateServiceImpl implements TaskTemplateService {
    private final ServiceTaskTemplateResource taskTemplateResource;

    @Autowired
    public TaskTemplateServiceImpl(ServiceTaskTemplateResource taskTemplateResource) {
        this.taskTemplateResource = taskTemplateResource;
    }

    @Override
    public PageData<ServiceTaskTemplateDTO> listPageTaskTemplates(Long appId,
                                                                  BaseSearchCondition baseSearchCondition) {
        val result =
            taskTemplateResource.listPageTaskTemplates(appId,
                baseSearchCondition.getStart(), baseSearchCondition.getLength());
        if (result != null) {
            return result.getData();
        } else {
            throw new RuntimeException("Fail to call job-manage:serviceTaskTemplateResourceClient" +
                ".listPageTaskTemplates, please check");
        }
    }
}
