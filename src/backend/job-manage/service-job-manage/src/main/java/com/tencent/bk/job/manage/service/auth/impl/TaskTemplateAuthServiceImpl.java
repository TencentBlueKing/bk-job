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

package com.tencent.bk.job.manage.service.auth.impl;

import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import com.tencent.bk.job.manage.service.auth.TaskTemplateAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskTemplateAuthServiceImpl implements TaskTemplateAuthService {
    private final WebAuthService authService;

    @Autowired
    public TaskTemplateAuthServiceImpl(WebAuthService authService) {
        this.authService = authService;
    }

    @Override
    public void processTemplatePermission(String username, AppResourceScope appResourceScope,
                                          PageData<TaskTemplateVO> taskTemplateVOPageData) {
        taskTemplateVOPageData
            .setCanCreate(authService.auth(false, username, ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS,
                appResourceScope.getAppId().toString(), null).isPass());
        processTemplatePermission(username, appResourceScope, taskTemplateVOPageData.getData());
    }

    @Override
    public void processTemplatePermission(String username, AppResourceScope appResourceScope,
                                          List<TaskTemplateVO> taskTemplateVOList) {
        boolean canCreate = authService.auth(false, username, ActionId.CREATE_JOB_TEMPLATE, ResourceTypeEnum.BUSINESS
            , appResourceScope.getAppId().toString(), null).isPass();
        List<String> templateIdList = new ArrayList<>();
        taskTemplateVOList.forEach(template -> {
            templateIdList.add(template.getId().toString());
        });
        List<Long> allowedViewTemplate = authService
            .batchAuth(username, ActionId.VIEW_JOB_TEMPLATE, appResourceScope.getAppId(), ResourceTypeEnum.TEMPLATE,
                templateIdList)
            .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        List<Long> allowedEditTemplate = authService
            .batchAuth(username, ActionId.EDIT_JOB_TEMPLATE, appResourceScope.getAppId(), ResourceTypeEnum.TEMPLATE,
                templateIdList)
            .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        List<Long> allowedDeleteTemplate = authService
            .batchAuth(username, ActionId.DELETE_JOB_TEMPLATE, appResourceScope.getAppId(), ResourceTypeEnum.TEMPLATE
                , templateIdList)
            .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        // List<Long> allowedDebugTemplate = authService
        // .batchAuth(username, ActionId.DEBUG_JOB_TEMPLATE, appId, ResourceTypeEnum.TEMPLATE, templateIdList)
        // .parallelStream().map(Long::valueOf).collect(Collectors.toList());

        taskTemplateVOList.forEach(template -> {
            template.setCanView(allowedViewTemplate.contains(template.getId()));
            template.setCanEdit(allowedEditTemplate.contains(template.getId()));
            template.setCanDelete(allowedDeleteTemplate.contains(template.getId()));
            template.setCanDebug(allowedViewTemplate.contains(template.getId()));
            template.setCanClone(canCreate && template.getCanView());
        });
    }
}
