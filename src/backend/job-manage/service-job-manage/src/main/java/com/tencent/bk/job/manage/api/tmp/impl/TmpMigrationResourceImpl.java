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

package com.tencent.bk.job.manage.api.tmp.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.manage.api.tmp.TmpMigrationResource;
import com.tencent.bk.job.manage.dao.plan.TaskPlanDAO;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.tmp.MigrationPlanBasic;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class TmpMigrationResourceImpl implements TmpMigrationResource {
    private final TaskTemplateService taskTemplateService;
    private final TaskPlanDAO taskPlanDAO;

    @Autowired
    public TmpMigrationResourceImpl(TaskTemplateService taskTemplateService,
                                    TaskPlanDAO taskPlanDAO) {
        this.taskTemplateService = taskTemplateService;
        this.taskPlanDAO = taskPlanDAO;
    }

    @Override
    public ServiceResponse<List<MigrationPlanBasic>> listAppPlanBasicInfo(String username, Long appId) {
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        // Get all
        baseSearchCondition.setLength(Integer.MAX_VALUE);

        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId).baseSearchCondition(baseSearchCondition)
            .build();
        PageData<TaskTemplateInfoDTO> templatePageData = taskTemplateService.listPageTaskTemplates(query);
        if (templatePageData == null || CollectionUtils.isEmpty(templatePageData.getData())) {
            log.info("Template is empty, appId: {}", appId);
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
        }
        Map<Long, String> templateIdNameMap = new HashMap<>();
        templatePageData.getData().forEach(template -> templateIdNameMap.put(template.getId(), template.getName()));

        TaskPlanQueryDTO taskPlanQuery = new TaskPlanQueryDTO();
        taskPlanQuery.setAppId(appId);
        PageData<TaskPlanInfoDTO> pagePlanData = taskPlanDAO.listPageTaskPlans(taskPlanQuery,
            baseSearchCondition, Collections.emptyList());
        if (pagePlanData == null || CollectionUtils.isEmpty(pagePlanData.getData())) {
            log.info("Plan is empty, appId: {}", appId);
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
        }
        List<MigrationPlanBasic> planList = new ArrayList<>(pagePlanData.getData().size());
        pagePlanData.getData().forEach(plan -> {
            MigrationPlanBasic planBasic = new MigrationPlanBasic();
            planBasic.setPlanId(plan.getId());
            planBasic.setPlanName(plan.getName());
            planBasic.setTemplateId(plan.getTemplateId());
            planBasic.setTemplateName(templateIdNameMap.get(plan.getTemplateId()));
            planList.add(planBasic);
        });

        return ServiceResponse.buildSuccessResp(planList);
    }
}
