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
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiJobTemplateV4ResourceImpl;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateGlobalVarDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateExecuteTargetDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateStepDTO;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OpenApiJobTemplateV4ResourceImpl} 单元测试。
 */
class OpenApiJobTemplateV4ResourceImplTest {

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "tester";
    private static final String APP_CODE = "bk_job";
    private static final Long APP_ID = 2L;
    private static final String SCOPE_TYPE = ResourceScopeTypeEnum.BIZ.getValue();
    private static final String SCOPE_ID = "2";
    private static final Long TEMPLATE_ID = 1000L;

    private TaskTemplateService templateService;
    private AppScopeMappingService appScopeMappingService;
    private OpenApiJobTemplateV4ResourceImpl resource;
    private User testUser;

    @BeforeEach
    void setUp() {
        templateService = mock(TaskTemplateService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        testUser = new User(TENANT_ID, USERNAME, USERNAME);
        when(appScopeMappingService.getAppIdByScope(SCOPE_TYPE, SCOPE_ID)).thenReturn(APP_ID);
        when(appScopeMappingService.getScopeByAppId(APP_ID))
            .thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        resource = new OpenApiJobTemplateV4ResourceImpl(templateService, appScopeMappingService);
        JobContextUtil.setUser(testUser);
    }

    @AfterEach
    void tearDown() {
        JobContextUtil.unsetContext();
    }

    private TaskTemplateInfoDTO buildTemplate(List<TaskStepDTO> steps, List<TaskVariableDTO> variables) {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
        template.setName("demo-template");
        template.setDescription("desc");
        template.setStatus(TaskTemplateStatusEnum.PUBLISHED);
        template.setCreator(USERNAME);
        template.setCreateTime(1738220000L);
        template.setLastModifyUser("editor");
        template.setLastModifyTime(1738221000L);
        template.setStepList(steps);
        template.setVariableList(variables);
        return template;
    }

    private TaskStepDTO buildApprovalStep(long id, int enable) {
        TaskStepDTO step = new TaskStepDTO();
        step.setId(id);
        step.setName("approval-step-" + id);
        step.setType(TaskStepTypeEnum.APPROVAL);
        step.setEnable(enable);
        TaskApprovalStepDTO approvalInfo = new TaskApprovalStepDTO();
        approvalInfo.setApprovalType(TaskApprovalTypeEnum.ANYONE);
        approvalInfo.setApprovalMessage("confirm");
        step.setApprovalStepInfo(approvalInfo);
        return step;
    }

    @Test
    @DisplayName("成功返回模板详情，时间戳转毫秒且包含全部步骤")
    void getJobTemplateDetail_success_maps_fields_and_all_steps() {
        TaskStepDTO enabledStep = buildApprovalStep(101L, 1);
        TaskStepDTO disabledStep = buildApprovalStep(102L, 0);
        TaskTemplateInfoDTO template = buildTemplate(
            Arrays.asList(enabledStep, disabledStep),
            Collections.emptyList()
        );
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        EsbV4Response<OpenApiV4JobTemplateDetailDTO> response = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        );

        OpenApiV4JobTemplateDetailDTO data = response.getData();
        assertThat(data.getId()).isEqualTo(TEMPLATE_ID);
        assertThat(data.getScopeType()).isEqualTo(SCOPE_TYPE);
        assertThat(data.getScopeId()).isEqualTo(SCOPE_ID);
        assertThat(data.getCreateTime()).isEqualTo(1738220000000L);
        assertThat(data.getLastModifyTime()).isEqualTo(1738221000000L);
        assertThat(data.getGlobalVarList()).isEmpty();
        assertThat(data.getStepList()).hasSize(2);
        assertThat(data.getStepList()).extracting(V4JobTemplateStepDTO::getId).containsExactly(101L, 102L);
    }

    @Test
    @DisplayName("模板不存在时抛 TEMPLATE_NOT_EXIST")
    void template_not_exist_throws() {
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID)))
            .thenThrow(new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST));

        assertThatThrownBy(() -> resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TEMPLATE_NOT_EXIST)
            );
    }

    @Test
    @DisplayName("无权限时 getTaskTemplate 抛 PermissionDeniedException")
    void no_permission_throws() {
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID)))
            .thenThrow(new PermissionDeniedException(AuthResult.fail(testUser)));

        assertThatThrownBy(() -> resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ))
            .isInstanceOf(PermissionDeniedException.class);
        verify(templateService, times(1)).getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID));
    }

    @Test
    @DisplayName("host_list 映射：ip_list 三字段不含扩展字段")
    void host_list_mapped_from_ip_list() {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(10001L);
        host.setCloudAreaId(0L);
        host.setIp("127.0.0.1");
        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        hostNode.setHostList(Collections.singletonList(host));
        TaskTargetDTO target = new TaskTargetDTO(null, hostNode, null, null);

        TaskVariableDTO hostVar = new TaskVariableDTO();
        hostVar.setName("HOST_TARGET");
        hostVar.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        hostVar.setDefaultValue(target.toJsonString());
        hostVar.setDescription("");
        hostVar.setRequired(true);

        TaskTemplateInfoDTO template = buildTemplate(Collections.emptyList(), Collections.singletonList(hostVar));
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        V4JobTemplateExecuteTargetDTO executeTarget = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ).getData().getGlobalVarList().get(0).getExecuteTarget();

        assertThat(executeTarget.getHostList()).hasSize(1);
        assertThat(executeTarget.getHostList().get(0).getBkHostId()).isEqualTo(10001L);
        assertThat(executeTarget.getHostList().get(0).getBkCloudId()).isEqualTo(0L);
        assertThat(executeTarget.getHostList().get(0).getIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("密文变量返回 ******")
    void cipher_variable_masked() {
        TaskVariableDTO cipherVar = new TaskVariableDTO();
        cipherVar.setName("SECRET");
        cipherVar.setType(TaskVariableTypeEnum.CIPHER);
        cipherVar.setDefaultValue("real-secret");
        cipherVar.setDescription("secret var");
        cipherVar.setRequired(true);

        TaskTemplateInfoDTO template = buildTemplate(Collections.emptyList(), Collections.singletonList(cipherVar));
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        EsbV4Response<OpenApiV4JobTemplateDetailDTO> response = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        );

        V4JobTemplateGlobalVarDTO globalVar = response.getData().getGlobalVarList().get(0);
        assertThat(globalVar.getValue()).isEqualTo(TaskVariableTypeEnum.CIPHER.getMask());
        assertThat(globalVar.getRequired()).isEqualTo(1);
        assertThat(globalVar.getName()).isEqualTo("SECRET");
    }

    @Test
    @DisplayName("响应不含 status、script_status、version、tags、global_var_list[].id")
    void response_excludes_deprecated_fields() {
        TaskVariableDTO stringVar = new TaskVariableDTO();
        stringVar.setId(99L);
        stringVar.setName("DIR");
        stringVar.setType(TaskVariableTypeEnum.STRING);
        stringVar.setDefaultValue("/tmp");
        stringVar.setDescription("");
        stringVar.setRequired(false);

        TaskTemplateInfoDTO template = buildTemplate(Collections.emptyList(), Collections.singletonList(stringVar));
        template.setScriptStatus(1);
        template.setVersion("v1");
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        OpenApiV4JobTemplateDetailDTO data = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ).getData();

        V4JobTemplateGlobalVarDTO globalVar = data.getGlobalVarList().get(0);
        assertThat(globalVar.getName()).isEqualTo("DIR");
        assertThat(globalVar.getValue()).isEqualTo("/tmp");
        assertThat(OpenApiV4JobTemplateDetailDTO.class.getDeclaredFields())
            .noneMatch(field -> "status".equals(field.getName()));
        assertThat(V4JobTemplateGlobalVarDTO.class.getDeclaredFields())
            .noneMatch(field -> "id".equals(field.getName()));
    }
}
