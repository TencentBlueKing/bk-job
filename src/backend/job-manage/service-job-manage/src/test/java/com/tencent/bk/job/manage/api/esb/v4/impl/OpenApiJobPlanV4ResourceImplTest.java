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

package com.tencent.bk.job.manage.api.esb.v4.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.execute.model.esb.v4.req.OpenApiV4HostDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ContainerFilter;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiJobPlanV4ResourceImpl;
import com.tencent.bk.job.manage.auth.PlanAuthService;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.EsbJobPlanV4DTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobPlanRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobPlanVariableItem;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.job.common.validation.NoXss;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OpenAPI V4 创建执行方案的单元测试，覆盖 Issue 第 7 节中 T1-T15、T16-T20 的关键场景及 2 个辅助断言。
 * 实现类必须直接构造 {@link TaskPlanInfoDTO} 并调用 {@link TaskPlanService#createTaskPlan(User, TaskPlanInfoDTO)}，
 * 不得引用任何 Web 层请求体（如 TaskPlanCreateUpdateReq）或 Web 层 VO（如 TaskVariableVO）。
 * T6/T7 断言 need_update 字段直接透传 Service 返回的 TaskPlanInfoDTO#needUpdate，不重写 if-else 比较逻辑。
 */
class OpenApiJobPlanV4ResourceImplTest {

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "tester";
    private static final String APP_CODE = "bk_job";
    private static final Long APP_ID = 2L;
    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String SCOPE_ID = "2";
    private static final Long TEMPLATE_ID = 1000L;
    private static final String PLAN_NAME = "my-plan";

    private TaskPlanService planService;
    private TaskTemplateService templateService;
    private TemplateAuthService templateAuthService;
    private PlanAuthService planAuthService;
    private AppScopeMappingService appScopeMappingService;

    private OpenApiJobPlanV4ResourceImpl resource;
    private User testUser;

    @BeforeEach
    void setUp() {
        planService = mock(TaskPlanService.class);
        templateService = mock(TaskTemplateService.class);
        templateAuthService = mock(TemplateAuthService.class);
        planAuthService = mock(PlanAuthService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        testUser = new User(TENANT_ID, USERNAME, USERNAME);

        when(appScopeMappingService.getAppIdByScope(SCOPE_TYPE, SCOPE_ID)).thenReturn(APP_ID);
        when(appScopeMappingService.getScopeByAppId(APP_ID))
            .thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        when(templateAuthService.authViewJobTemplate(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID)))
            .thenReturn(AuthResult.pass(testUser));
        when(planAuthService.authCreateJobPlan(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID), any()))
            .thenReturn(AuthResult.pass(testUser));
        when(planService.checkPlanName(eq(APP_ID), eq(TEMPLATE_ID), eq(0L), any())).thenReturn(true);

        resource = new OpenApiJobPlanV4ResourceImpl(
            planService,
            templateService,
            templateAuthService,
            planAuthService,
            appScopeMappingService
        );

        // 实现类落库时通过 JobContextUtil.getUser() 获取调用方 User（与 WebTaskPlanResourceImpl 一致），
        // 单测未走 Spring/JobCommonInterceptor，需要显式设置 JobContext 中的 User
        JobContextUtil.setUser(testUser);
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    private V4CreateJobPlanRequest buildBaseRequest() {
        V4CreateJobPlanRequest request = new V4CreateJobPlanRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setJobTemplateId(TEMPLATE_ID);
        request.setName(PLAN_NAME);
        return request;
    }

    private TaskTemplateInfoDTO buildTemplate(List<Long> stepIds, List<TaskVariableDTO> variables) {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
        template.setName("tpl");
        List<TaskStepDTO> stepList = new ArrayList<>();
        for (Long stepId : stepIds) {
            TaskStepDTO step = new TaskStepDTO();
            step.setId(stepId);
            stepList.add(step);
        }
        template.setStepList(stepList);
        template.setVariableList(variables == null ? Collections.emptyList() : variables);
        return template;
    }

    private TaskVariableDTO buildTemplateVar(Long id, String name, TaskVariableTypeEnum type, String defaultValue) {
        TaskVariableDTO var = new TaskVariableDTO();
        var.setId(id);
        var.setName(name);
        var.setType(type);
        var.setDefaultValue(defaultValue);
        var.setDescription("");
        var.setChangeable(true);
        var.setRequired(false);
        return var;
    }

    private TaskPlanInfoDTO buildSavedPlan(Boolean needUpdate) {
        TaskPlanInfoDTO savedPlan = new TaskPlanInfoDTO();
        savedPlan.setId(50001L);
        savedPlan.setAppId(APP_ID);
        savedPlan.setTemplateId(TEMPLATE_ID);
        savedPlan.setName(PLAN_NAME);
        savedPlan.setCreator(USERNAME);
        savedPlan.setCreateTime(1738220000L);
        savedPlan.setNeedUpdate(needUpdate);
        return savedPlan;
    }

    @Test
    @DisplayName("T1: enable_steps 未传入时，启用全部模板步骤")
    void t1_enableSteps_default_to_all_template_steps() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L, 102L, 103L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(null);

        EsbV4Response<EsbJobPlanV4DTO> response = resource.createJobPlan(USERNAME, APP_CODE, request);

        assertThat(response.getData()).isNotNull();
        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).containsExactlyInAnyOrder(101L, 102L, 103L);
    }

    @Test
    @DisplayName("T2: enable_steps 传入合法模板步骤 ID，按列表启用")
    void t2_enableSteps_subset_of_template_steps() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L, 102L, 103L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Arrays.asList(101L, 103L));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).containsExactly(101L, 103L);
    }

    @Test
    @DisplayName("T3: enable_steps 包含非模板步骤 ID，抛 ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON")
    void t3_enableSteps_outside_template_throws() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L, 102L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Arrays.asList(101L, 999L));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("T4: 模板不存在时抛 TEMPLATE_NOT_EXIST")
    void t4_template_not_exist_throws() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(null);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TEMPLATE_NOT_EXIST)
            );
    }

    @Test
    @DisplayName("T5: 方案名称字段 @NoXss 校验拒绝 HTML 特殊字符")
    void t5_invalid_plan_name_fails_no_xss_validation() throws NoSuchFieldException {
        NoXss noXss = V4CreateJobPlanRequest.class.getDeclaredField("name").getAnnotation(NoXss.class);
        NoXss.Validator validator = new NoXss.Validator();
        validator.initialize(noXss);

        assertThat(validator.isValid("bad<name", null)).isFalse();
        assertThat(validator.isValid(PLAN_NAME, null)).isTrue();
    }

    @Test
    @DisplayName("T6: Service 返回 needUpdate=false 时，OpenAPI 直接透传该字段")
    void t6_needUpdate_false_passed_through_from_service() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        // 手动构造 Service 返回的 TaskPlanInfoDTO，明确 needUpdate=false
        TaskPlanInfoDTO savedPlan = buildSavedPlan(false);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class))).thenReturn(savedPlan);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        EsbV4Response<EsbJobPlanV4DTO> response = resource.createJobPlan(USERNAME, APP_CODE, request);

        // 严禁单测自己模拟比较逻辑：直接断言响应字段等于 Service 返回的 needUpdate
        assertThat(response.getData().getNeedUpdate()).isFalse();
    }

    @Test
    @DisplayName("T7: Service 返回 needUpdate=true 时，OpenAPI 直接透传不重写比较逻辑")
    void t7_needUpdate_true_passed_through_from_service() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        // 手动构造 Service 返回的 TaskPlanInfoDTO，明确 needUpdate=true
        TaskPlanInfoDTO savedPlan = buildSavedPlan(true);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class))).thenReturn(savedPlan);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        EsbV4Response<EsbJobPlanV4DTO> response = resource.createJobPlan(USERNAME, APP_CODE, request);

        assertThat(response.getData().getNeedUpdate()).isTrue();
    }

    @Test
    @DisplayName("T8: variables 按 name 解析模板变量并覆盖默认值；构造的 TaskVariableDTO 字段完整")
    void t8_variables_lookup_by_name_and_override_default_value() {
        TaskVariableDTO var = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("TARGET_DIR");
        item.setValue("/data/release");

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        // 落库 DTO 的基础字段必须直接装配自 OpenAPI 入参，不经过 Web 层任何 *Req/*VO
        TaskPlanInfoDTO planInfo = captor.getValue();
        assertThat(planInfo.getAppId()).isEqualTo(APP_ID);
        assertThat(planInfo.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(planInfo.getName()).isEqualTo(PLAN_NAME);
        assertThat(planInfo.getCreator()).isEqualTo(USERNAME);
        assertThat(planInfo.getDebug()).isFalse();
        List<TaskVariableDTO> sentVariables = planInfo.getVariableList();
        assertThat(sentVariables).hasSize(1);
        TaskVariableDTO sent = sentVariables.get(0);
        // OpenAPI Impl 直接构造 TaskVariableDTO：id/name/type/changeable/required/delete 取自模板，
        // defaultValue 取自请求 value，followTemplate 取自请求项
        assertThat(sent.getId()).isEqualTo(10L);
        assertThat(sent.getName()).isEqualTo("TARGET_DIR");
        assertThat(sent.getType()).isEqualTo(TaskVariableTypeEnum.STRING);
        assertThat(sent.getDefaultValue()).isEqualTo("/data/release");
        assertThat(sent.getChangeable()).isTrue();
        assertThat(sent.getRequired()).isFalse();
        assertThat(sent.getDelete()).isFalse();
        assertThat(sent.getFollowTemplate()).isFalse();
    }

    @Test
    @DisplayName("T9: variables 名称在模板中不存在，抛 ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON")
    void t9_variable_name_not_in_template_throws() {
        TaskVariableDTO var = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("UNKNOWN_VAR");
        item.setValue("anything");

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
    }

    @Test
    @DisplayName("T10a: follow_template=true 且传入 execute_target 时抛 ILLEGAL_PARAM")
    void t10a_followTemplate_true_with_execute_target_throws() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(10001L);
        executeTarget.setHostList(Collections.singletonList(host));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setFollowTemplate(true);
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("T10: follow_template=true 时忽略 value，保留模板默认值")
    void t10_followTemplate_true_skips_value_override() {
        TaskVariableDTO var = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("TARGET_DIR");
        item.setValue("/data/release");
        item.setFollowTemplate(true);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        List<TaskVariableDTO> sentVariables = captor.getValue().getVariableList();
        assertThat(sentVariables).hasSize(1);
        // follow_template=true 时不覆盖 value/defaultValue，保留模板默认值
        assertThat(sentVariables.get(0).getDefaultValue()).isNull();
    }

    @Test
    @DisplayName("T11: EXECUTE_OBJECT_LIST 传入 kube_container_filters 时抛 ILLEGAL_PARAM")
    void t11_execute_object_list_with_container_filter_throws() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setKubeContainerFilters(Collections.singletonList(new V4ContainerFilter()));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("T16: EXECUTE_OBJECT_LIST 静态主机覆盖（hostId 与 cloudId+ip）")
    void t16_execute_object_list_static_host_override() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        OpenApiV4HostDTO hostById = new OpenApiV4HostDTO();
        hostById.setBkHostId(10001L);
        OpenApiV4HostDTO hostByIp = new OpenApiV4HostDTO();
        hostByIp.setBkCloudId(0L);
        hostByIp.setIp("10.0.0.1");

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setHostList(Arrays.asList(hostById, hostByIp));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskTargetDTO target = TaskTargetDTO.fromJsonString(
            captor.getValue().getVariableList().get(0).getDefaultValue()
        );
        assertThat(target.getHostNodeList().getHostList()).hasSize(2);
        assertThat(target.getHostNodeList().getHostList().get(0).getHostId()).isEqualTo(10001L);
        assertThat(target.getHostNodeList().getHostList().get(1).getCloudAreaId()).isEqualTo(0L);
        assertThat(target.getHostNodeList().getHostList().get(1).getIp()).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("T17: EXECUTE_OBJECT_LIST 动态分组覆盖")
    void t17_execute_object_list_dynamic_group_override() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        EsbDynamicGroupDTO group = new EsbDynamicGroupDTO();
        group.setId("dg-001");

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setDynamicGroups(Collections.singletonList(group));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskTargetDTO target = TaskTargetDTO.fromJsonString(
            captor.getValue().getVariableList().get(0).getDefaultValue()
        );
        assertThat(target.getHostNodeList().getDynamicGroupId()).containsExactly("dg-001");
    }

    @Test
    @DisplayName("T18: EXECUTE_OBJECT_LIST 拓扑节点覆盖")
    void t18_execute_object_list_topo_node_override() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        EsbCmdbTopoNodeDTO topoNode = new EsbCmdbTopoNodeDTO();
        topoNode.setId(2001L);
        topoNode.setNodeType("module");

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setTopoNodes(Collections.singletonList(topoNode));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskTargetDTO target = TaskTargetDTO.fromJsonString(
            captor.getValue().getVariableList().get(0).getDefaultValue()
        );
        assertThat(target.getHostNodeList().getNodeInfoList()).hasSize(1);
        assertThat(target.getHostNodeList().getNodeInfoList().get(0).getId()).isEqualTo(2001L);
        assertThat(target.getHostNodeList().getNodeInfoList().get(0).getType()).isEqualTo("module");
    }

    @Test
    @DisplayName("T19: EXECUTE_OBJECT_LIST 主机+动态分组+拓扑节点混合覆盖")
    void t19_execute_object_list_mixed_host_dimensions() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(10001L);
        EsbDynamicGroupDTO group = new EsbDynamicGroupDTO();
        group.setId("dg-001");
        EsbCmdbTopoNodeDTO topoNode = new EsbCmdbTopoNodeDTO();
        topoNode.setId(2001L);
        topoNode.setNodeType("module");

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setHostList(Collections.singletonList(host));
        executeTarget.setDynamicGroups(Collections.singletonList(group));
        executeTarget.setTopoNodes(Collections.singletonList(topoNode));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskTargetDTO target = TaskTargetDTO.fromJsonString(
            captor.getValue().getVariableList().get(0).getDefaultValue()
        );
        assertThat(target.getHostNodeList().getHostList()).hasSize(1);
        assertThat(target.getHostNodeList().getDynamicGroupId()).containsExactly("dg-001");
        assertThat(target.getHostNodeList().getNodeInfoList()).hasSize(1);
    }

    @Test
    @DisplayName("T20: EXECUTE_OBJECT_LIST execute_target 全空时抛 ILLEGAL_PARAM")
    void t20_execute_object_list_empty_execute_target_throws() {
        TaskVariableDTO var = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(new V4ExecuteTargetDTO());

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Collections.singletonList(item));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("T12: variables 中变量名重复时抛 ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON")
    void t12_duplicate_variable_names_throws() {
        TaskVariableDTO var = buildTemplateVar(10L, "DUP", TaskVariableTypeEnum.STRING, "");
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), Collections.singletonList(var));
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);

        V4JobPlanVariableItem item1 = new V4JobPlanVariableItem();
        item1.setName("DUP");
        item1.setValue("v1");
        V4JobPlanVariableItem item2 = new V4JobPlanVariableItem();
        item2.setName("DUP");
        item2.setValue("v2");

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));
        request.setVariables(Arrays.asList(item1, item2));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
    }

    @Test
    @DisplayName("T13: 方案名称已存在时抛 PLAN_NAME_EXIST")
    void t13_plan_name_exist_throws() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.checkPlanName(eq(APP_ID), eq(TEMPLATE_ID), eq(0L), any())).thenReturn(false);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(AlreadyExistsException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAN_NAME_EXIST)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("T14: 响应字段完整且 create_time 转换为毫秒")
    void t14_response_fields_complete_and_create_time_in_millis() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        TaskPlanInfoDTO savedPlan = buildSavedPlan(false);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class))).thenReturn(savedPlan);

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        EsbV4Response<EsbJobPlanV4DTO> response = resource.createJobPlan(USERNAME, APP_CODE, request);

        EsbJobPlanV4DTO data = response.getData();
        assertThat(data).isNotNull();
        assertThat(data.getJobPlanId()).isEqualTo(50001L);
        assertThat(data.getJobPlanName()).isEqualTo(PLAN_NAME);
        assertThat(data.getJobTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(data.getCreator()).isEqualTo(USERNAME);
        // Web 层存储为秒，OpenAPI 返回毫秒
        assertThat(data.getCreateTime()).isEqualTo(savedPlan.getCreateTime() * 1000L);
        assertThat(data.getScopeType()).isEqualTo(SCOPE_TYPE);
        assertThat(data.getScopeId()).isEqualTo(SCOPE_ID);
        assertThat(data.getNeedUpdate()).isFalse();
    }

    @Test
    @DisplayName("T15: 权限校验失败时不会进入 Service")
    void t15_permission_check_blocks_creation() {
        when(templateAuthService.authViewJobTemplate(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID)))
            .thenReturn(AuthResult.fail(testUser));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Collections.singletonList(101L));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request));
        verify(templateService, times(0)).getTaskTemplateById(anyLong(), anyLong());
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("辅助断言：未设置 enable_steps 且模板无步骤时，传递给 Service 的 enableStepList 为空")
    void empty_template_steps_results_in_empty_enable_list() {
        TaskTemplateInfoDTO template = buildTemplate(Collections.emptyList(), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(null);

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).isEmpty();
    }

    @Test
    @DisplayName("辅助断言：createTaskPlan 接收到的 TaskPlanInfoDTO 中名称已去空格")
    void plan_name_is_trimmed_before_service_call() {
        TaskTemplateInfoDTO template = buildTemplate(Arrays.asList(101L), null);
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(template);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setName("  trimmed  ");
        request.setEnableSteps(Collections.singletonList(101L));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("trimmed");
    }

}
