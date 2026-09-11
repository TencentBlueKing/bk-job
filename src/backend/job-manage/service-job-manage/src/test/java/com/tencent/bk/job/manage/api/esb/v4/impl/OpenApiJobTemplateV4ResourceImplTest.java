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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.common.model.dto.KubeWorkloadObjectDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceBasicInfoDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.api.common.ExecuteAccountVariableValidator;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiJobTemplateV4ResourceImpl;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiV4JobTemplateWriteConverter;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetContainerDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalUserReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerFilterDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4KubeTopoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateWriteResultDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileSourceDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateGlobalVarDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateExecuteTargetDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateStepDTO;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.job.manage.service.template.TemplateLocalFileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private ServiceFileSourceResource fileSourceResource;
    private TemplateLocalFileService templateLocalFileService;
    private ExecuteAccountVariableValidator executeAccountVariableValidator;
    private OpenApiJobTemplateV4ResourceImpl resource;
    private User testUser;

    @BeforeEach
    void setUp() {
        templateService = mock(TaskTemplateService.class);
        appScopeMappingService = mock(AppScopeMappingService.class);
        fileSourceResource = mock(ServiceFileSourceResource.class);
        templateLocalFileService = mock(TemplateLocalFileService.class);
        executeAccountVariableValidator = mock(ExecuteAccountVariableValidator.class);
        testUser = new User(TENANT_ID, USERNAME, USERNAME);
        when(appScopeMappingService.getAppIdByScope(SCOPE_TYPE, SCOPE_ID)).thenReturn(APP_ID);
        when(appScopeMappingService.getScopeByAppId(APP_ID))
            .thenReturn(new ResourceScope(SCOPE_TYPE, SCOPE_ID));
        resource = new OpenApiJobTemplateV4ResourceImpl(
            templateService, appScopeMappingService, fileSourceResource,
            new OpenApiV4JobTemplateWriteConverter(templateLocalFileService, mock(ScriptManager.class)),
            executeAccountVariableValidator
        );
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

    /**
     * 把执行目标挂到一个执行对象类型的全局变量上再读回来，省去构造完整步骤。
     */
    private V4JobTemplateExecuteTargetDTO targetOfFirstVariable(TaskTargetDTO target) {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setName("TARGET");
        variable.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST);
        variable.setDefaultValue(target.toJsonString());
        variable.setDescription("");
        variable.setRequired(true);

        TaskTemplateInfoDTO template = buildTemplate(Collections.emptyList(), Collections.singletonList(variable));
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        return resource.getJobTemplateDetail(USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID)
            .getData().getGlobalVarList().get(0).getExecuteTarget();
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

    private TaskStepDTO buildFileSourceStep(long stepId, Integer... fileSourceIds) {
        TaskStepDTO step = new TaskStepDTO();
        step.setId(stepId);
        step.setName("file-step-" + stepId);
        step.setType(TaskStepTypeEnum.FILE);
        TaskFileStepDTO fileStepInfo = new TaskFileStepDTO();
        fileStepInfo.setOriginFileList(Arrays.stream(fileSourceIds).map(fileSourceId -> {
            TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
            fileInfo.setFileType(TaskFileTypeEnum.FILE_SOURCE);
            fileInfo.setFileSourceId(fileSourceId);
            fileInfo.setFileLocation(Collections.singletonList("/data/a.txt"));
            return fileInfo;
        }).collect(Collectors.toList()));
        fileStepInfo.setDestinationFileLocation("/tmp/");
        fileStepInfo.setIgnoreError(false);
        step.setFileStepInfo(fileStepInfo);
        return step;
    }

    private void mockFileSourceBasicInfo(ServiceFileSourceBasicInfoDTO... basicInfoList) {
        // 不用 buildSuccessResp，它依赖 Spring 上下文取 i18n 文案
        InternalResponse<List<ServiceFileSourceBasicInfoDTO>> response = new InternalResponse<>();
        response.setData(Arrays.asList(basicInfoList));
        when(fileSourceResource.listFileSourceBasicInfoByIds(eq(TENANT_ID), anyList())).thenReturn(response);
    }

    private List<V4JobTemplateFileSourceDTO> fileSourceListOf(OpenApiV4JobTemplateDetailDTO data, int stepIndex) {
        return data.getStepList().get(stepIndex).getFileInfo().getFileSourceList();
    }

    @Test
    @DisplayName("文件源步骤同时返回 file_source_id 与 file_source_code，重复 ID 只批量查一次")
    void file_source_id_and_code_backfilled_in_single_batch_query() {
        TaskTemplateInfoDTO template = buildTemplate(
            Arrays.asList(buildFileSourceStep(201L, 7, 8), buildFileSourceStep(202L, 7)),
            Collections.emptyList()
        );
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);
        mockFileSourceBasicInfo(
            new ServiceFileSourceBasicInfoDTO(7, "cos_a", "COS-A"),
            new ServiceFileSourceBasicInfoDTO(8, "cos_b", "COS-B")
        );

        OpenApiV4JobTemplateDetailDTO data = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ).getData();

        assertThat(fileSourceListOf(data, 0))
            .extracting(V4JobTemplateFileSourceDTO::getFileSourceId, V4JobTemplateFileSourceDTO::getFileSourceCode)
            .containsExactly(tuple(7, "cos_a"), tuple(8, "cos_b"));
        assertThat(fileSourceListOf(data, 1))
            .extracting(V4JobTemplateFileSourceDTO::getFileSourceId, V4JobTemplateFileSourceDTO::getFileSourceCode)
            .containsExactly(tuple(7, "cos_a"));

        ArgumentCaptor<List<Integer>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(fileSourceResource, times(1)).listFileSourceBasicInfoByIds(eq(TENANT_ID), idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(7, 8);
    }

    @Test
    @DisplayName("文件源已被删除时 file_source_code 为 null，file_source_id 照常返回")
    void file_source_code_null_when_file_source_missing() {
        TaskTemplateInfoDTO template = buildTemplate(
            Collections.singletonList(buildFileSourceStep(201L, 7, 9)),
            Collections.emptyList()
        );
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);
        mockFileSourceBasicInfo(new ServiceFileSourceBasicInfoDTO(7, "cos_a", "COS-A"));

        OpenApiV4JobTemplateDetailDTO data = resource.getJobTemplateDetail(
            USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID
        ).getData();

        assertThat(fileSourceListOf(data, 0))
            .extracting(V4JobTemplateFileSourceDTO::getFileSourceId, V4JobTemplateFileSourceDTO::getFileSourceCode)
            .containsExactly(tuple(7, "cos_a"), tuple(9, null));
    }

    @Test
    @DisplayName("模板未引用文件源时不发起批量查询")
    void no_batch_query_when_no_file_source_referenced() {
        TaskTemplateInfoDTO template = buildTemplate(
            Collections.singletonList(buildApprovalStep(101L, 1)),
            Collections.emptyList()
        );
        when(templateService.getTaskTemplate(any(User.class), eq(APP_ID), eq(TEMPLATE_ID))).thenReturn(template);

        resource.getJobTemplateDetail(USERNAME, APP_CODE, SCOPE_TYPE, SCOPE_ID, TEMPLATE_ID);

        verify(fileSourceResource, never()).listFileSourceBasicInfoByIds(any(), anyList());
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
    @DisplayName("静态容器：返回容器 ID 与落库快照中的可读信息，不回查 CMDB")
    void container_list_returns_snapshot() {
        TaskTargetContainerDTO container = new TaskTargetContainerDTO();
        container.setId(3001L);
        container.setContainerId("docker://abcdef");
        container.setName("nginx");
        container.setPodName("nginx-7d9f");
        container.setNamespace("default");
        container.setClusterUID("BCS-K8S-00001");
        container.setNodeIp("127.0.0.1");

        V4JobTemplateExecuteTargetDTO executeTarget =
            targetOfFirstVariable(new TaskTargetDTO(null, null, Collections.singletonList(container), null));

        assertThat(executeTarget.getContainerList()).hasSize(1);
        V4JobTemplateContainerDTO v4Container = executeTarget.getContainerList().get(0);
        assertThat(v4Container.getContainerId()).isEqualTo(3001L);
        assertThat(v4Container.getContainerUID()).isEqualTo("docker://abcdef");
        assertThat(v4Container.getName()).isEqualTo("nginx");
        assertThat(v4Container.getPodName()).isEqualTo("nginx-7d9f");
        assertThat(v4Container.getNamespace()).isEqualTo("default");
        assertThat(v4Container.getClusterUID()).isEqualTo("BCS-K8S-00001");
        assertThat(v4Container.getNodeIp()).isEqualTo("127.0.0.1");
    }

    @Test
    @DisplayName("静态容器：v4 写入的容器只有 ID，快照字段为空时不影响返回")
    void container_list_without_snapshot_returns_id_only() {
        TaskTargetContainerDTO container = new TaskTargetContainerDTO();
        container.setId(3002L);

        V4JobTemplateExecuteTargetDTO executeTarget =
            targetOfFirstVariable(new TaskTargetDTO(null, null, Collections.singletonList(container), null));

        V4JobTemplateContainerDTO v4Container = executeTarget.getContainerList().get(0);
        assertThat(v4Container.getContainerId()).isEqualTo(3002L);
        assertThat(v4Container.getName()).isNull();
        assertThat(v4Container.getClusterUID()).isNull();
    }

    @Test
    @DisplayName("动态筛选：拓扑 ID 原样返回，不翻译为集群 UID 或 namespace 名称")
    void container_filter_returns_raw_topo_ids() {
        KubeTopoDTO topo = new KubeTopoDTO();
        topo.setCluster(new KubeClusterObjectDTO(105L));
        topo.setNamespace(new KubeNamespaceObjectDTO(207L));
        topo.setWorkloads(Collections.singletonList(new KubeWorkloadObjectDTO("deployment", 309L)));

        KubeContainerFilter filter = new KubeContainerFilter();
        filter.setName("prod-workloads");
        filter.setKubeTopoList(Collections.singletonList(topo));
        filter.setPropConditions(Collections.singletonList(
            new KubePropCondition("pod_name", "contains", "nginx")));

        V4JobTemplateExecuteTargetDTO executeTarget =
            targetOfFirstVariable(new TaskTargetDTO(null, null, null, Collections.singletonList(filter)));

        assertThat(executeTarget.getContainerFilters()).hasSize(1);
        V4JobTemplateContainerFilterDTO v4Filter = executeTarget.getContainerFilters().get(0);
        assertThat(v4Filter.getName()).isEqualTo("prod-workloads");
        assertThat(v4Filter.getKubeTopoList()).hasSize(1);
        V4KubeTopoDTO v4Topo = v4Filter.getKubeTopoList().get(0);
        assertThat(v4Topo.getCluster().getId()).isEqualTo(105L);
        assertThat(v4Topo.getNamespace().getId()).isEqualTo(207L);
        assertThat(v4Topo.getWorkloads()).hasSize(1);
        assertThat(v4Topo.getWorkloads().get(0).getKind()).isEqualTo("deployment");
        assertThat(v4Topo.getWorkloads().get(0).getId()).isEqualTo(309L);
        // 运算符由字段唯一决定，不出现在响应里
        assertThat(v4Filter.getPropConditions()).hasSize(1);
        assertThat(v4Filter.getPropConditions().get(0).getField()).isEqualTo("pod_name");
        assertThat(v4Filter.getPropConditions().get(0).getValue()).isEqualTo("nginx");
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

    @Test
    @DisplayName("创建模板：转换后交给服务层，返回体带 scope 与毫秒时间戳")
    void createJobTemplate_delegates_to_service_and_maps_result() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setName("new-template");
        request.setStepList(Collections.singletonList(approvalStepReq(null, "step-1")));

        TaskTemplateInfoDTO created = buildTemplate(Collections.emptyList(), Collections.emptyList());
        created.setName("new-template");
        when(templateService.saveTaskTemplate(any(User.class), any(TaskTemplateInfoDTO.class))).thenReturn(created);

        OpenApiV4JobTemplateWriteResultDTO result =
            resource.createJobTemplate(USERNAME, APP_CODE, request).getData();

        ArgumentCaptor<TaskTemplateInfoDTO> captor = ArgumentCaptor.forClass(TaskTemplateInfoDTO.class);
        verify(templateService).saveTaskTemplate(any(User.class), captor.capture());
        assertThat(captor.getValue().getId()).isNull();
        assertThat(captor.getValue().getAppId()).isEqualTo(APP_ID);
        assertThat(captor.getValue().getCreator()).isEqualTo(USERNAME);
        assertThat(captor.getValue().getStepList()).extracting(TaskStepDTO::getName).containsExactly("step-1");

        assertThat(result.getJobTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(result.getName()).isEqualTo("new-template");
        assertThat(result.getScopeType()).isEqualTo(SCOPE_TYPE);
        assertThat(result.getScopeId()).isEqualTo(SCOPE_ID);
        assertThat(result.getCreateTime()).isEqualTo(1738220000000L);
        assertThat(result.getLastModifyTime()).isEqualTo(1738221000000L);
    }

    @Test
    @DisplayName("创建模板：账号变量校验先于落库，校验不过则不调用服务层")
    void createJobTemplate_validates_account_variable_before_save() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setName("new-template");
        request.setStepList(Collections.singletonList(approvalStepReq(null, "step-1")));

        doThrow(new InvalidParamException(ErrorCode.ILLEGAL_PARAM))
            .when(executeAccountVariableValidator).validate(eq(APP_ID), anyList(), anyList());

        assertThatThrownBy(() -> resource.createJobTemplate(USERNAME, APP_CODE, request))
            .isInstanceOf(InvalidParamException.class);
        verify(templateService, never()).saveTaskTemplate(any(User.class), any(TaskTemplateInfoDTO.class));
    }

    @Test
    @DisplayName("更新模板：省略的既有步骤被补上删除标记后交给服务层")
    void updateJobTemplate_marks_omitted_step_deleted() {
        TaskTemplateInfoDTO existing = buildTemplate(
            Arrays.asList(buildApprovalStep(101L, 1), buildApprovalStep(102L, 1)),
            Collections.emptyList()
        );
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(existing);
        when(templateService.updateTaskTemplate(any(User.class), any(TaskTemplateInfoDTO.class)))
            .thenReturn(existing);

        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setId(TEMPLATE_ID);
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setName("renamed-template");
        request.setStepList(Collections.singletonList(approvalStepReq(101L, "approval-step-101")));

        resource.updateJobTemplate(USERNAME, APP_CODE, request);

        ArgumentCaptor<TaskTemplateInfoDTO> captor = ArgumentCaptor.forClass(TaskTemplateInfoDTO.class);
        verify(templateService).updateTaskTemplate(any(User.class), captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(TEMPLATE_ID);
        assertThat(captor.getValue().getName()).isEqualTo("renamed-template");
        assertThat(captor.getValue().getStepList())
            .extracting(TaskStepDTO::getId, TaskStepDTO::getDelete)
            .containsExactly(tuple(101L, 0), tuple(102L, 1));
    }

    @Test
    @DisplayName("更新模板：模板不存在时抛 TEMPLATE_NOT_EXIST，不调用服务层更新")
    void updateJobTemplate_template_not_exist_throws() {
        when(templateService.getTaskTemplateById(APP_ID, TEMPLATE_ID)).thenReturn(null);

        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setId(TEMPLATE_ID);
        request.setScopeType(SCOPE_TYPE);
        request.setScopeId(SCOPE_ID);
        request.setName("renamed-template");
        request.setStepList(Collections.singletonList(approvalStepReq(101L, "approval-step-101")));

        assertThatThrownBy(() -> resource.updateJobTemplate(USERNAME, APP_CODE, request))
            .isInstanceOfSatisfying(NotFoundException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TEMPLATE_NOT_EXIST)
            );
        verify(templateService, never()).updateTaskTemplate(any(User.class), any(TaskTemplateInfoDTO.class));
    }

    private V4JobTemplateStepReq approvalStepReq(Long id, String name) {
        V4JobTemplateApprovalUserReq approvalUser = new V4JobTemplateApprovalUserReq();
        approvalUser.setUserList(Collections.singletonList("admin"));
        V4JobTemplateApprovalStepReq approvalInfo = new V4JobTemplateApprovalStepReq();
        approvalInfo.setApprovalUser(approvalUser);
        approvalInfo.setApprovalMessage("confirm to proceed");

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setId(id);
        step.setName(name);
        step.setType(TaskStepTypeEnum.APPROVAL.getValue());
        step.setApprovalInfo(approvalInfo);
        return step;
    }
}
