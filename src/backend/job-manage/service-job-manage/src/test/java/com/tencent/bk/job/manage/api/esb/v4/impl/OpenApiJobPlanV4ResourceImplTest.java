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
import com.tencent.bk.job.common.model.openapi.v3.EsbCmdbTopoNodeDTO;
import com.tencent.bk.job.common.model.openapi.v3.EsbDynamicGroupDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
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
 * {@link OpenApiJobPlanV4ResourceImpl} 单元测试：步骤解析、变量映射、执行目标转换、异常分支与响应字段。
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
        request.setEnableSteps(Collections.singletonList(101L));
        return request;
    }

    private TaskTemplateInfoDTO buildTemplate(List<Long> stepIds, List<TaskVariableDTO> variables) {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
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

    private void stubTemplateAndCreate(List<Long> stepIds, List<TaskVariableDTO> variables) {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID))
            .thenReturn(buildTemplate(stepIds, variables));
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));
    }

    @Test
    @DisplayName("enable_steps 未传时启用全部模板步骤；传入子集时按列表启用")
    void enableSteps_resolved_from_template_or_request() {
        stubTemplateAndCreate(Arrays.asList(101L, 102L, 103L), null);

        V4CreateJobPlanRequest allStepsRequest = buildBaseRequest();
        allStepsRequest.setEnableSteps(null);
        resource.createJobPlan(USERNAME, APP_CODE, allStepsRequest);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).containsExactlyInAnyOrder(101L, 102L, 103L);

        V4CreateJobPlanRequest subsetRequest = buildBaseRequest();
        subsetRequest.setEnableSteps(Arrays.asList(101L, 103L));
        resource.createJobPlan(USERNAME, APP_CODE, subsetRequest);

        verify(planService, times(2)).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).containsExactly(101L, 103L);
    }

    @Test
    @DisplayName("enable_steps 含非模板步骤 ID 时抛 ILLEGAL_PARAM")
    void enableSteps_outside_template_throws() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID))
            .thenReturn(buildTemplate(Arrays.asList(101L, 102L), null));

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setEnableSteps(Arrays.asList(101L, 999L));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("模板不存在时抛 TEMPLATE_NOT_EXIST")
    void template_not_exist_throws() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(null);

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, buildBaseRequest()))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TEMPLATE_NOT_EXIST)
            );
    }

    @Test
    @DisplayName("字符串变量按 name 覆盖默认值")
    void variables_override_default_by_name() {
        TaskVariableDTO var = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        stubTemplateAndCreate(Collections.singletonList(101L), Collections.singletonList(var));

        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("TARGET_DIR");
        item.setValue("/data/release");

        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setVariables(Collections.singletonList(item));

        resource.createJobPlan(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskVariableDTO sent = captor.getValue().getVariableList().get(0);
        assertThat(sent.getId()).isEqualTo(10L);
        assertThat(sent.getDefaultValue()).isEqualTo("/data/release");
        assertThat(sent.getFollowTemplate()).isFalse();
    }

    @Test
    @DisplayName("变量名不在模板中或请求内重复时抛 ILLEGAL_PARAM")
    void variables_unknown_or_duplicate_name_throws() {
        TaskVariableDTO var = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID))
            .thenReturn(buildTemplate(Collections.singletonList(101L), Collections.singletonList(var)));

        V4JobPlanVariableItem unknown = new V4JobPlanVariableItem();
        unknown.setName("UNKNOWN");
        unknown.setValue("x");
        V4CreateJobPlanRequest unknownRequest = buildBaseRequest();
        unknownRequest.setVariables(Collections.singletonList(unknown));
        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, unknownRequest))
            .isInstanceOf(InvalidParamException.class);

        V4JobPlanVariableItem dup1 = new V4JobPlanVariableItem();
        dup1.setName("TARGET_DIR");
        dup1.setValue("a");
        V4JobPlanVariableItem dup2 = new V4JobPlanVariableItem();
        dup2.setName("TARGET_DIR");
        dup2.setValue("b");
        V4CreateJobPlanRequest dupRequest = buildBaseRequest();
        dupRequest.setVariables(Arrays.asList(dup1, dup2));
        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, dupRequest))
            .isInstanceOf(InvalidParamException.class);

        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("follow_template=true 忽略 value；与 execute_target 互斥")
    void followTemplate_skips_value_and_rejects_execute_target() {
        TaskVariableDTO stringVar = buildTemplateVar(10L, "TARGET_DIR", TaskVariableTypeEnum.STRING, "/tmp");
        TaskVariableDTO hostVar = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        stubTemplateAndCreate(
            Collections.singletonList(101L),
            Arrays.asList(stringVar, hostVar)
        );

        V4JobPlanVariableItem followItem = new V4JobPlanVariableItem();
        followItem.setName("TARGET_DIR");
        followItem.setValue("/override");
        followItem.setFollowTemplate(true);

        V4CreateJobPlanRequest followRequest = buildBaseRequest();
        followRequest.setVariables(Collections.singletonList(followItem));
        resource.createJobPlan(USERNAME, APP_CODE, followRequest);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getVariableList().get(0).getDefaultValue()).isNull();

        V4ExecuteTargetDTO executeTarget = new V4ExecuteTargetDTO();
        executeTarget.setHostList(Collections.singletonList(hostWithId(10001L)));

        V4JobPlanVariableItem conflictItem = new V4JobPlanVariableItem();
        conflictItem.setName("HOST_TARGET");
        conflictItem.setFollowTemplate(true);
        conflictItem.setExecuteTarget(executeTarget);

        V4CreateJobPlanRequest conflictRequest = buildBaseRequest();
        conflictRequest.setVariables(Collections.singletonList(conflictItem));
        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, conflictRequest))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("执行目标：拒绝容器筛选；全空抛错；主机/分组/拓扑映射入 TaskTargetDTO")
    void executeTarget_validation_and_mapping() {
        TaskVariableDTO hostVar = buildTemplateVar(20L, "HOST_TARGET", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST, null);
        stubTemplateAndCreate(Collections.singletonList(101L), Collections.singletonList(hostVar));

        V4ExecuteTargetDTO withContainer = new V4ExecuteTargetDTO();
        withContainer.setKubeContainerFilters(Collections.singletonList(new V4ContainerFilter()));
        assertThatThrownBy(() -> createWithExecuteTarget(withContainer))
            .isInstanceOf(InvalidParamException.class);

        assertThatThrownBy(() -> createWithExecuteTarget(new V4ExecuteTargetDTO()))
            .isInstanceOf(InvalidParamException.class);

        OpenApiV4HostDTO hostById = hostWithId(10001L);
        OpenApiV4HostDTO hostByIp = new OpenApiV4HostDTO();
        hostByIp.setBkCloudId(0L);
        hostByIp.setIp("127.0.0.1");
        EsbDynamicGroupDTO group = new EsbDynamicGroupDTO();
        group.setId("dg-001");
        EsbCmdbTopoNodeDTO topo = new EsbCmdbTopoNodeDTO();
        topo.setId(2001L);
        topo.setNodeType("module");

        V4ExecuteTargetDTO mixed = new V4ExecuteTargetDTO();
        mixed.setHostList(Arrays.asList(hostById, hostByIp));
        mixed.setDynamicGroups(Collections.singletonList(group));
        mixed.setTopoNodes(Collections.singletonList(topo));

        createWithExecuteTarget(mixed);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        TaskTargetDTO target = TaskTargetDTO.fromJsonString(
            captor.getValue().getVariableList().get(0).getDefaultValue()
        );
        assertThat(target.getHostNodeList().getHostList()).hasSize(2);
        assertThat(target.getHostNodeList().getHostList().get(0).getHostId()).isEqualTo(10001L);
        assertThat(target.getHostNodeList().getHostList().get(1).getIp()).isEqualTo("127.0.0.1");
        assertThat(target.getHostNodeList().getDynamicGroupId()).containsExactly("dg-001");
        assertThat(target.getHostNodeList().getNodeInfoList()).hasSize(1);
        assertThat(target.getHostNodeList().getNodeInfoList().get(0).getType()).isEqualTo("module");
    }

    private void createWithExecuteTarget(V4ExecuteTargetDTO executeTarget) {
        V4JobPlanVariableItem item = new V4JobPlanVariableItem();
        item.setName("HOST_TARGET");
        item.setExecuteTarget(executeTarget);
        V4CreateJobPlanRequest request = buildBaseRequest();
        request.setVariables(Collections.singletonList(item));
        resource.createJobPlan(USERNAME, APP_CODE, request);
    }

    private static OpenApiV4HostDTO hostWithId(long hostId) {
        OpenApiV4HostDTO host = new OpenApiV4HostDTO();
        host.setBkHostId(hostId);
        return host;
    }

    @Test
    @DisplayName("方案名称已存在时抛 PLAN_NAME_EXIST")
    void plan_name_exist_throws() {
        stubTemplateAndCreate(Collections.singletonList(101L), null);
        when(planService.checkPlanName(eq(APP_ID), eq(TEMPLATE_ID), eq(0L), any())).thenReturn(false);

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, buildBaseRequest()))
            .isInstanceOfSatisfying(AlreadyExistsException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PLAN_NAME_EXIST)
            );
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("响应字段映射且 create_time 转为毫秒")
    void response_maps_fields_and_create_time_in_millis() {
        stubTemplateAndCreate(Collections.singletonList(101L), null);
        TaskPlanInfoDTO savedPlan = buildSavedPlan(true);
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class))).thenReturn(savedPlan);

        EsbV4Response<EsbJobPlanV4DTO> response = resource.createJobPlan(USERNAME, APP_CODE, buildBaseRequest());

        EsbJobPlanV4DTO data = response.getData();
        assertThat(data.getJobPlanId()).isEqualTo(50001L);
        assertThat(data.getCreateTime()).isEqualTo(savedPlan.getCreateTime() * 1000L);
        assertThat(data.getNeedUpdate()).isTrue();
        assertThat(data.getScopeType()).isEqualTo(SCOPE_TYPE);
    }

    @Test
    @DisplayName("鉴权失败时不进入模板查询与落库")
    void auth_failure_blocks_service() {
        when(templateAuthService.authViewJobTemplate(any(User.class), any(AppResourceScope.class), eq(TEMPLATE_ID)))
            .thenReturn(AuthResult.fail(testUser));

        assertThatThrownBy(() -> resource.createJobPlan(USERNAME, APP_CODE, buildBaseRequest()));
        verify(templateService, times(0)).getTaskTemplateById(anyLong(), anyLong());
        verify(planService, times(0)).createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class));
    }

    @Test
    @DisplayName("模板无步骤时 enableStepList 为空；方案名称去首尾空格")
    void edge_empty_steps_and_trimmed_plan_name() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID))
            .thenReturn(buildTemplate(Collections.emptyList(), null));
        when(planService.createTaskPlan(any(User.class), any(TaskPlanInfoDTO.class)))
            .thenReturn(buildSavedPlan(false));

        V4CreateJobPlanRequest emptyStepsRequest = buildBaseRequest();
        emptyStepsRequest.setEnableSteps(null);
        resource.createJobPlan(USERNAME, APP_CODE, emptyStepsRequest);

        ArgumentCaptor<TaskPlanInfoDTO> captor = ArgumentCaptor.forClass(TaskPlanInfoDTO.class);
        verify(planService).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getEnableStepList()).isEmpty();

        stubTemplateAndCreate(Collections.singletonList(101L), null);
        V4CreateJobPlanRequest trimRequest = buildBaseRequest();
        trimRequest.setName("  trimmed  ");
        resource.createJobPlan(USERNAME, APP_CODE, trimRequest);

        verify(planService, times(2)).createTaskPlan(any(User.class), captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("trimmed");
    }

}
