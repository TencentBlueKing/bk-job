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

package com.tencent.bk.job.manage.service.plan.impl;

import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 创建执行方案的 dryRun（预检）返回点单测。
 * <p>
 * <b>这个测试类存在的唯一目的：锁住"预检不落库"这条性质。</b>
 * 带审批的创建接口在创建审批任务时用 dryRun 触发完整业务校验，如果日后有人在返回点之上插入写操作，
 * 用户还没审批，执行方案已经建出来了。该失效模式没有任何外部症状，只能靠单测拦住。
 */
class V4JobPlanCreateServiceImplTest {

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "tester";
    private static final String SCOPE_TYPE = "biz";
    private static final String SCOPE_ID = "2";
    private static final Long APP_ID = 2L;
    private static final Long TEMPLATE_ID = 1000L;
    private static final Long STEP_ID = 101L;
    private static final String PLAN_NAME = "my-plan";

    private TaskPlanService planService;
    private TaskTemplateService templateService;
    private TemplateAuthService templateAuthService;
    private PlanAuthService planAuthService;

    private V4JobPlanCreateServiceImpl service;
    private User operator;

    @BeforeEach
    void setUp() {
        planService = mock(TaskPlanService.class);
        templateService = mock(TaskTemplateService.class);
        templateAuthService = mock(TemplateAuthService.class);
        planAuthService = mock(PlanAuthService.class);
        AppScopeMappingService appScopeMappingService = mock(AppScopeMappingService.class);
        TenantHostService tenantHostService = mock(TenantHostService.class);
        operator = new User(TENANT_ID, USERNAME, USERNAME);

        when(appScopeMappingService.getAppIdByScope(SCOPE_TYPE, SCOPE_ID)).thenReturn(APP_ID);
        when(appScopeMappingService.getScopeByAppId(APP_ID)).thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        when(templateAuthService.authViewJobTemplate(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID)))
            .thenReturn(AuthResult.pass(operator));
        when(planAuthService.authCreateJobPlan(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID), any()))
            .thenReturn(AuthResult.pass(operator));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(buildTemplate());
        when(planService.checkPlanName(eq(APP_ID), eq(TEMPLATE_ID), eq(0L), any())).thenReturn(true);

        service = new V4JobPlanCreateServiceImpl(
            planService,
            templateService,
            templateAuthService,
            planAuthService,
            appScopeMappingService,
            tenantHostService
        );
    }

    @Test
    @DisplayName("dryRun=true：走完全部校验后在执行方案落库之前返回，不产生任何写操作")
    void dryRun_noWriteOperations() {
        TaskPlanInfoDTO result = service.createJobPlan(operator, buildRequest(), true);

        verify(planService, never()).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
        // 未落库 —— 没有 id 就说明没走 createTaskPlan
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNull();
        // 预检结果要能拿来渲染审批单据：方案名、创建人、启用步骤都已解析完成
        assertThat(result.getName()).isEqualTo(PLAN_NAME);
        assertThat(result.getCreator()).isEqualTo(USERNAME);
        assertThat(result.getEnableStepList()).containsExactly(STEP_ID);
    }

    @Test
    @DisplayName("dryRun=true：模板查看与创建方案的鉴权照常真实执行")
    void dryRun_stillRunsAuth() {
        service.createJobPlan(operator, buildRequest(), true);

        verify(templateAuthService).authViewJobTemplate(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID));
        verify(planAuthService).authCreateJobPlan(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID), any());
    }

    @Test
    @DisplayName("dryRun=true：鉴权不通过时抛权限异常，预检不得放过越权请求")
    void dryRun_authFailStillThrows() {
        when(planAuthService.authCreateJobPlan(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID), any()))
            .thenReturn(AuthResult.fail(operator));

        assertThatThrownBy(() -> service.createJobPlan(operator, buildRequest(), true))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("dryRun=true：模板不存在时照常失败，让用户在发起阶段就拿到错误")
    void dryRun_templateNotExistStillThrows() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.createJobPlan(operator, buildRequest(), true))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("dryRun=true：方案名重复时照常失败")
    void dryRun_duplicatedPlanNameStillThrows() {
        when(planService.checkPlanName(eq(APP_ID), eq(TEMPLATE_ID), eq(0L), any())).thenReturn(false);

        assertThatThrownBy(() -> service.createJobPlan(operator, buildRequest(), true))
            .isInstanceOf(AlreadyExistsException.class);
    }

    @Test
    @DisplayName("dryRun=false：执行方案照常落库（正式创建行为不变）")
    void realRun_createsPlan() {
        TaskPlanInfoDTO savedPlan = new TaskPlanInfoDTO();
        savedPlan.setId(50001L);
        savedPlan.setName(PLAN_NAME);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class))).thenReturn(savedPlan);

        TaskPlanInfoDTO result = service.createJobPlan(operator, buildRequest(), false);

        verify(planService).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
        assertThat(result.getId()).isEqualTo(50001L);
    }

    private V4CreateJobPlanRequest buildRequest() {
        V4CreateJobPlanRequest request = new V4CreateJobPlanRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setJobTemplateId(TEMPLATE_ID);
        request.setName(PLAN_NAME);
        request.setEnableSteps(Collections.singletonList(STEP_ID));
        return request;
    }

    private TaskTemplateInfoDTO buildTemplate() {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
        TaskStepDTO step = new TaskStepDTO();
        step.setId(STEP_ID);
        template.setStepList(Collections.singletonList(step));
        template.setVariableList(Collections.emptyList());
        return template;
    }
}
