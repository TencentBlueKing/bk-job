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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.auth.AccountAuthService;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.auth.TagAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.auth.TicketAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.web.request.OperationPermissionReq;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebPermissionResourceImplTest {
    private static final User USER = new User("tenant-a", "tester", "测试用户");
    private ApplicationService applicationService;
    private BusinessAuthService businessAuthService;
    private ScriptAuthService scriptAuthService;
    private AccountAuthService accountAuthService;
    private TagAuthService tagAuthService;
    private TicketAuthService ticketAuthService;
    private WebAuthService webAuthService;
    private TaskPlanService taskPlanService;
    private TaskTemplateService taskTemplateService;
    private TemplateAuthService templateAuthService;
    private PlanAuthService planAuthService;
    private NoResourceScopeAuthService noResourceScopeAuthService;
    private WebPermissionResourceImpl resource;
    private MockedStatic<I18nUtil> i18nUtil;

    @BeforeEach
    void setUp() {
        i18nUtil = mockStatic(I18nUtil.class);
        JobContextUtil.setUser(USER);
        businessAuthService = mock(BusinessAuthService.class);
        scriptAuthService = mock(ScriptAuthService.class);
        accountAuthService = mock(AccountAuthService.class);
        tagAuthService = mock(TagAuthService.class);
        ticketAuthService = mock(TicketAuthService.class);
        webAuthService = mock(WebAuthService.class);
        when(webAuthService.toAuthResultVO(anyBoolean(), any())).thenAnswer(invocation ->
            ((AuthResult) invocation.getArgument(1)).isPass() ? AuthResultVO.pass() : AuthResultVO.fail());
        taskPlanService = mock(TaskPlanService.class);
        taskTemplateService = mock(TaskTemplateService.class);
        templateAuthService = mock(TemplateAuthService.class);
        planAuthService = mock(PlanAuthService.class);
        noResourceScopeAuthService = mock(NoResourceScopeAuthService.class);
        applicationService = mock(ApplicationService.class);
        resource = new WebPermissionResourceImpl(webAuthService, businessAuthService,
            taskPlanService, taskTemplateService, applicationService, accountAuthService,
            tagAuthService, scriptAuthService, templateAuthService, planAuthService,
            ticketAuthService, noResourceScopeAuthService);
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
        i18nUtil.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"biz", "script", "job_template", "job_plan", "account", "tag", "ticket"})
    void rejectOtherTenant(String resourceType) {
        mockApplication("tenant-b");

        assertThatThrownBy(() -> resource.checkOperationPermission(USER.getUsername(), "biz", "2",
            ("biz".equals(resourceType) ? "biz/access_business" : resourceType + "/view"), "1000", true))
            .isInstanceOfSatisfying(NotFoundException.class,
                exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.APP_NOT_EXIST));
        verifyNoInteractions(businessAuthService, scriptAuthService, accountAuthService, tagAuthService,
            ticketAuthService, taskPlanService, taskTemplateService, templateAuthService, planAuthService, webAuthService);
    }

    @Test
    void post_shouldRejectOtherTenant() {
        mockApplication("tenant-b");
        OperationPermissionReq request = new OperationPermissionReq();
        request.setScopeType("biz");
        request.setScopeId("2");
        request.setOperation("job_plan/view");
        request.setResourceId("1000");

        assertThatThrownBy(() -> resource.checkOperationPermission(USER.getUsername(), request))
            .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(businessAuthService, taskPlanService, planAuthService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void templateView_shouldUseResourceAuth(boolean allowed) {
        // 同租户的模板查询仍由模板查看权限决定结果，不额外检查业务访问权限或预查询模板。
        mockApplication(USER.getTenantId());
        AuthResult authResult = allowed ? AuthResult.pass(USER) : AuthResult.fail(USER);
        when(templateAuthService.authViewJobTemplate(eq(USER), any(), eq(1000L))).thenReturn(authResult);

        assertThat(resource.checkOperationPermission(USER.getUsername(), "biz", "2",
            "job_template/view", "1000", true).getData().isPass()).isEqualTo(allowed);
        verify(templateAuthService).authViewJobTemplate(eq(USER), any(), eq(1000L));
        verify(webAuthService).toAuthResultVO(true, authResult);
        verifyNoInteractions(businessAuthService, taskTemplateService);
    }

    @Test
    void planView_shouldUseResourceAuth() {
        // 同租户的方案查询仍按业务加载方案，再检查方案查看权限。
        mockApplication(USER.getTenantId());
        TaskPlanInfoDTO plan = new TaskPlanInfoDTO();
        plan.setId(1000L);
        plan.setTemplateId(100L);
        when(taskPlanService.getTaskPlanById(2L, 1000L)).thenReturn(plan);
        when(planAuthService.authViewJobPlan(eq(USER), any(), eq(100L), eq(1000L), any()))
            .thenReturn(AuthResult.pass(USER));

        assertThat(resource.checkOperationPermission(USER.getUsername(), "biz", "2",
            "job_plan/view", "1000", true).getData().isPass()).isTrue();
    }

    @Test
    void bizAccessDenied_shouldReturnDenied() {
        // 租户一致不代表具有业务访问权限，原有业务访问鉴权拒绝时仍应返回拒绝。
        mockApplication(USER.getTenantId());
        when(businessAuthService.authAccessBusiness(eq(USER), any())).thenReturn(AuthResult.fail(USER));

        assertThat(resource.checkOperationPermission(USER.getUsername(), "biz", "2",
            "biz/access_business", null, true).getData().isPass()).isFalse();
        verify(businessAuthService).authAccessBusiness(eq(USER), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"public_script/create", "whitelist/view"})
    void publicScope_shouldNotRequireBiz(String operation) {
        // 公共脚本和白名单没有业务范围，应保留原有鉴权，不查询业务。
        when(noResourceScopeAuthService.authCreatePublicScript(USER)).thenReturn(AuthResult.pass(USER));
        when(noResourceScopeAuthService.authManageWhiteList(USER)).thenReturn(AuthResult.pass(USER));

        assertThat(resource.checkOperationPermission(USER.getUsername(), null, null,
            operation, null, true).getData().isPass()).isTrue();
        verifyNoInteractions(applicationService, businessAuthService);
    }

    private void mockApplication(String tenantId) {
        ApplicationDTO app = new ApplicationDTO();
        app.setId(2L);
        app.setTenantId(tenantId);
        app.setScope(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        when(applicationService.getAppByScope("biz", "2")).thenReturn(app);
    }
}
