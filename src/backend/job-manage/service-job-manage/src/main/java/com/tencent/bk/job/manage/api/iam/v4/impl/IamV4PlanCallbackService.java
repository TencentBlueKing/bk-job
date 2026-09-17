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

package com.tencent.bk.job.manage.api.iam.v4.impl;

import com.tencent.bk.job.common.iam.constant.IamV4ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamV4CallbackService;
import com.tencent.bk.job.common.iam.util.IamV4RespUtil;
import com.tencent.bk.job.common.iam.util.IamV4ResourceTypeMapper;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanBasicInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackFilterDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackParentDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.request.V4CallbackRequestDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4BaseInstanceDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4InstanceInfoDTO;
import com.tencent.bk.sdk.iamv4.callback.dto.response.V4ListInstanceDataDTO;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行方案资源的 IAM v4 回调实现。
 *
 * <p>执行方案挂在作业模板下，各实例视图的 chain 末端都是「模板 → 执行方案」，
 * 因此 {@code list_instance} 的 parent 恒为 {@code job_template}。</p>
 */
@Slf4j
@Component
public class IamV4PlanCallbackService extends BaseIamV4CallbackService {

    private final TaskPlanService planService;
    private final ApplicationService applicationService;

    @Autowired
    public IamV4PlanCallbackService(TaskPlanService planService,
                                         ApplicationService applicationService,
                                         AppScopeMappingService appScopeMappingService,
                                         TenantEnvService tenantEnvService,
                                         IamV4AuthService iamV4AuthService) {
        super(appScopeMappingService, tenantEnvService, iamV4AuthService);
        this.planService = planService;
        this.applicationService = applicationService;
    }

    @Override
    public String getResourceType() {
        return IamV4ResourceTypeId.PLAN;
    }

    @Override
    protected V4ListInstanceDataDTO listInstance(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();

        TaskPlanQueryDTO planQuery = new TaskPlanQueryDTO();
        planQuery.setTemplateId(parseTemplateId(filter == null ? null : filter.getParent()));
        if (filter != null && StringUtils.isNotBlank(filter.getKeyword())) {
            planQuery.setName(filter.getKeyword());
        }

        PageData<TaskPlanInfoDTO> pageData = planService.listPageTaskPlansBasicInfo(
            planQuery, buildSearchCondition(request.getPage()), null);
        return IamV4RespUtil.buildListInstanceData(pageData,
            plan -> new V4BaseInstanceDTO(String.valueOf(plan.getId()), plan.getName()));
    }

    @Override
    protected List<V4InstanceInfoDTO> fetchInstanceInfo(V4CallbackRequestDTO request) {
        V4CallbackFilterDTO filter = request.getFilter();
        if (filter == null || CollectionUtils.isEmpty(filter.getIds())) {
            return Collections.emptyList();
        }
        List<Long> ids = parseLongIds(filter.getIds());
        List<TaskPlanBasicInfoDTO> planList = planService.listTaskPlanByIds(ids);

        Map<Long, TaskPlanBasicInfoDTO> planMap = new HashMap<>(planList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (TaskPlanBasicInfoDTO plan : planList) {
            planMap.put(plan.getId(), plan);
            appIdSet.add(plan.getAppId());
        }
        boolean needPath = isPathRequired(request);
        Map<Long, ResourceScope> appIdScopeMap = needPath
            ? applicationService.getScopeByAppIds(appIdSet) : Collections.emptyMap();

        List<V4InstanceInfoDTO> result = new ArrayList<>();
        for (Long id : ids) {
            TaskPlanBasicInfoDTO plan = planMap.get(id);
            if (plan == null) {
                log.warn("Cannot find job plan by id {}, may be deleted", id);
                continue;
            }
            V4InstanceInfoDTO instance = new V4InstanceInfoDTO();
            instance.setId(String.valueOf(plan.getId()));
            instance.setDisplayName(plan.getName());
            if (needPath) {
                instance.setBkIamPath(buildPlanPaths(plan, appIdScopeMap.get(plan.getAppId())));
            }
            result.add(instance);
        }
        return result;
    }

    private Long parseTemplateId(V4CallbackParentDTO parent) {
        if (parent == null || StringUtils.isBlank(parent.getId())) {
            throw new IllegalArgumentException("Parent job_template is required for listing job plans");
        }
        if (!IamV4ResourceTypeId.TEMPLATE.equals(parent.getType())) {
            throw new IllegalArgumentException("Unexpected parent resource type for job plan: " + parent.getType());
        }
        try {
            return Long.parseLong(parent.getId());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal job_template id: " + parent.getId());
        }
    }

    /**
     * 构造执行方案的拓扑路径列表，如 {@code ["/biz,2/job_template,10/"]}。路径只含祖先。
     */
    private List<String> buildPlanPaths(TaskPlanBasicInfoDTO plan, ResourceScope scope) {
        if (scope == null) {
            return null;
        }
        String scopeType = IamV4ResourceTypeMapper.getV4ResourceTypeIdForScope(scope);
        String path = "/" + scopeType + "," + scope.getId() + "/"
            + IamV4ResourceTypeId.TEMPLATE + "," + plan.getTemplateId() + "/";
        return Collections.singletonList(path);
    }
}
