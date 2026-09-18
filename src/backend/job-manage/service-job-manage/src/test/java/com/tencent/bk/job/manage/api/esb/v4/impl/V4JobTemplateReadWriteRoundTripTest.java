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

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.KubeClusterObjectDTO;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubeNamespaceObjectDTO;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.common.model.dto.KubeWorkloadObjectDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.toggle.feature.FeatureToggle;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiV4JobTemplateConverter;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiV4JobTemplateWriteConverter;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetContainerDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateAccountReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalUserReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateExecuteTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateVarTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileDestinationReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileSourceReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateGlobalVarReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateScriptStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateAccountDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateApprovalStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateExecuteTargetDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileSourceDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateFileStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateGlobalVarDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateScriptStepDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4JobTemplateStepDTO;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.host.CurrentTenantHostService;
import com.tencent.bk.job.manage.service.template.TemplateLocalFileService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 读—改—写闭环：{@code get_job_template_detail} 的响应经调用方的字段映射后，原样喂给 {@code update_job_template}，
 * 落库内容必须与原模板一致。
 * <p>
 * 这里同时验证读写不对称的三处只读字段（{@code file_source_code}、账号的 name/alias、静态容器快照）
 * 被忽略而非报错，最终引用仍由 ID 决定。
 */
@DisplayName("V4 作业模板：读—改—写闭环")
class V4JobTemplateReadWriteRoundTripTest {

    private static final String USERNAME = "tester";
    private static final Long APP_ID = 2L;
    private static final Long TEMPLATE_ID = 1000L;
    private static final Integer FILE_SOURCE_ID = 77;
    private static final String FILE_SOURCE_CODE = "prod-repo";
    private static final Long HOST_ID = 500L;

    private OpenApiV4JobTemplateWriteConverter writeConverter;
    private AppScopeMappingService appScopeMappingService;

    @BeforeEach
    void setUp() {
        writeConverter = new OpenApiV4JobTemplateWriteConverter(
            mock(TemplateLocalFileService.class), mock(ScriptManager.class));
        appScopeMappingService = mock(AppScopeMappingService.class);
        when(appScopeMappingService.getScopeByAppId(APP_ID))
            .thenReturn(new ResourceScope(ResourceScopeTypeEnum.BIZ, "2"));
        mockHostService();
    }

    @AfterAll
    static void tearDown() {
        resetStaticField(FeatureToggle.class, "featureManager");
        resetStaticField(ApplicationContextRegister.class, "context");
    }

    /**
     * 写转换器末尾会调用 {@link TaskTargetDTO#fillHostDetail} 回查主机明细，需要 Spring 上下文里有主机服务。
     */
    private void mockHostService() {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(HOST_ID);
        host.setCloudAreaId(0L);
        host.setIp("127.0.0.1");

        CurrentTenantHostService hostService = mock(CurrentTenantHostService.class);
        when(hostService.listHostsByHostIds(any()))
            .thenReturn(Collections.singletonMap(HOST_ID, host));
        when(hostService.listHostsByIps(any())).thenReturn(Collections.emptyMap());

        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getBean(CurrentTenantHostService.class)).thenReturn(hostService);
        new ApplicationContextRegister().setApplicationContext(context);
    }

    private static void resetStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("reset static field fail: " + clazz.getName() + "#" + fieldName, e);
        }
    }

    @Test
    @DisplayName("读出的模板原样写回，步骤 ID、步骤内容与变量 ID 均不变，且不产生删除标记")
    void detail_written_back_as_is_keeps_everything() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        TaskTemplateInfoDTO rewritten = roundTrip(original);

        assertThat(rewritten.getName()).isEqualTo(original.getName());
        assertThat(rewritten.getDescription()).isEqualTo(original.getDescription());
        assertThat(rewritten.getId()).isEqualTo(TEMPLATE_ID);
        // 步骤全部保留，没有任何一步被判定为删除
        assertThat(rewritten.getStepList())
            .extracting(TaskStepDTO::getId, TaskStepDTO::getName, TaskStepDTO::getType, TaskStepDTO::getDelete)
            .containsExactly(
                tuple(10L, "run-script", TaskStepTypeEnum.SCRIPT, 0),
                tuple(20L, "push-file", TaskStepTypeEnum.FILE, 0),
                tuple(30L, "approve", TaskStepTypeEnum.APPROVAL, 0)
            );
        // 变量按名称匹配回既有 ID，同样没有删除标记
        assertThat(rewritten.getVariableList())
            .extracting(TaskVariableDTO::getId, TaskVariableDTO::getName, TaskVariableDTO::getDelete)
            .containsExactly(
                tuple(101L, "plain", false),
                tuple(102L, "secret", false),
                tuple(103L, "targets", false)
            );
    }

    @Test
    @DisplayName("读出的模板原样写回，脚本步骤内容与执行目标不变")
    void script_step_survives_round_trip() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        TaskScriptStepDTO rewritten = roundTrip(original).getStepList().get(0).getScriptStepInfo();
        TaskScriptStepDTO expected = original.getStepList().get(0).getScriptStepInfo();

        assertThat(rewritten.getScriptSource()).isEqualTo(expected.getScriptSource());
        assertThat(rewritten.getContent()).isEqualTo(expected.getContent());
        assertThat(rewritten.getLanguage()).isEqualTo(expected.getLanguage());
        assertThat(rewritten.getScriptParam()).isEqualTo(expected.getScriptParam());
        assertThat(rewritten.getWindowsInterpreter()).isEqualTo(expected.getWindowsInterpreter());
        assertThat(rewritten.getTimeout()).isEqualTo(expected.getTimeout());
        assertThat(rewritten.getSecureParam()).isEqualTo(expected.getSecureParam());
        assertThat(rewritten.getIgnoreError()).isEqualTo(expected.getIgnoreError());
        assertThat(rewritten.getAccount()).isEqualTo(expected.getAccount());

        TaskHostNodeDTO hostNode = rewritten.getExecuteTarget().getHostNodeList();
        assertThat(hostNode.getHostList()).extracting(ApplicationHostDTO::getHostId).containsExactly(HOST_ID);
        assertThat(hostNode.getDynamicGroupId()).containsExactly("dg-1");
        assertThat(hostNode.getNodeInfoList())
            .extracting(TaskNodeInfoDTO::getId, TaskNodeInfoDTO::getType)
            .containsExactly(tuple(88L, "module"));
    }

    @Test
    @DisplayName("读出的模板原样写回，文件步骤的文件源引用不变；只读的 file_source_code 被忽略")
    void file_step_survives_round_trip() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        // 读接口确实回了 code，写回时它没有对应入参，最终引用只由 file_source_id 决定
        OpenApiV4JobTemplateDetailDTO detail = readDetail(original);
        V4JobTemplateFileStepDTO fileInfo = detail.getStepList().get(1).getFileInfo();
        assertThat(fileInfo.getFileSourceList().get(1).getFileSourceCode()).isEqualTo(FILE_SOURCE_CODE);

        TaskFileStepDTO rewritten = roundTrip(original).getStepList().get(1).getFileStepInfo();
        TaskFileStepDTO expected = original.getStepList().get(1).getFileStepInfo();

        assertThat(rewritten.getOriginFileList())
            .extracting(TaskFileInfoDTO::getFileType, TaskFileInfoDTO::getFileLocation,
                TaskFileInfoDTO::getFileSourceId)
            .containsExactly(
                tuple(
                    TaskFileTypeEnum.SERVER, Collections.singletonList("/data/a.txt"), null),
                tuple(
                    TaskFileTypeEnum.FILE_SOURCE, Collections.singletonList("bucket/b.txt"), FILE_SOURCE_ID)
            );
        assertThat(rewritten.getOriginFileList().get(0).getHostAccount()).isEqualTo(1001L);
        assertThat(rewritten.getDestinationFileLocation()).isEqualTo(expected.getDestinationFileLocation());
        assertThat(rewritten.getExecuteAccount()).isEqualTo(expected.getExecuteAccount());
        assertThat(rewritten.getTimeout()).isEqualTo(expected.getTimeout());
        assertThat(rewritten.getOriginSpeedLimit()).isEqualTo(expected.getOriginSpeedLimit());
        assertThat(rewritten.getTargetSpeedLimit()).isEqualTo(expected.getTargetSpeedLimit());
        assertThat(rewritten.getIgnoreError()).isEqualTo(expected.getIgnoreError());
        // transfer_mode 是 duplicateHandler + notExistPathHandler 的合成值，闭环后两者都要还原
        assertThat(rewritten.getDuplicateHandler()).isEqualTo(expected.getDuplicateHandler());
        assertThat(rewritten.getNotExistPathHandler()).isEqualTo(expected.getNotExistPathHandler());
    }

    @Test
    @DisplayName("读出的模板原样写回，审批步骤不变")
    void approval_step_survives_round_trip() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        TaskApprovalStepDTO rewritten = roundTrip(original).getStepList().get(2).getApprovalStepInfo();
        TaskApprovalStepDTO expected = original.getStepList().get(2).getApprovalStepInfo();

        assertThat(rewritten.getApprovalType()).isEqualTo(expected.getApprovalType());
        assertThat(rewritten.getApprovalMessage()).isEqualTo(expected.getApprovalMessage());
        assertThat(rewritten.getNotifyChannel()).isEqualTo(expected.getNotifyChannel());
        assertThat(rewritten.getApprovalUser().getUserList()).isEqualTo(expected.getApprovalUser().getUserList());
        assertThat(rewritten.getApprovalUser().getRoleList()).isEqualTo(expected.getApprovalUser().getRoleList());
    }

    @Test
    @DisplayName("读出的模板原样写回，密文变量保留原值而非掩码，容器变量的执行目标不变")
    void variables_survive_round_trip() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        List<TaskVariableDTO> rewritten = roundTrip(original).getVariableList();

        assertThat(rewritten.get(0).getDefaultValue()).isEqualTo("v1");
        // 读接口把密文变量替换成掩码，原样写回时必须识别出「未修改」并保留原值
        assertThat(rewritten.get(1).getDefaultValue()).isEqualTo("real-password");
        assertThat(rewritten.get(2).getDefaultValue())
            .isEqualTo(original.getVariableList().get(2).getDefaultValue());
    }

    @Test
    @DisplayName("读出的模板原样写回，容器配置不变；只读的容器快照字段被忽略，引用仍由 container_id 决定")
    void container_target_survives_round_trip() {
        TaskTemplateInfoDTO original = buildOriginalTemplate();

        // 读接口回了完整快照，调用方原样带回
        OpenApiV4JobTemplateDetailDTO detail = readDetail(original);
        V4JobTemplateExecuteTargetDTO readTarget = detail.getStepList().get(1).getFileInfo()
            .getFileDestination().getExecuteTarget();
        assertThat(readTarget.getContainerList().get(0).getName()).isEqualTo("nginx");

        TaskTargetDTO rewritten = roundTrip(original).getStepList().get(1)
            .getFileStepInfo().getDestinationHostList();

        assertThat(rewritten.getContainerList())
            .extracting(TaskTargetContainerDTO::getId, TaskTargetContainerDTO::getName)
            .containsExactly(tuple(3001L, null));

        KubeContainerFilter filter = rewritten.getContainerFilters().get(0);
        assertThat(filter.getName()).isEqualTo("prod-pods");
        assertThat(filter.getKubeTopoList().get(0).getCluster().getId()).isEqualTo(105L);
        assertThat(filter.getKubeTopoList().get(0).getNamespace().getId()).isEqualTo(206L);
        assertThat(filter.getKubeTopoList().get(0).getWorkloads())
            .extracting(KubeWorkloadObjectDTO::getKind, KubeWorkloadObjectDTO::getId)
            .containsExactly(tuple("deployment", 307L));
        // 读接口不返回运算符，写回时按字段补回原值，闭环后与原模板一致
        assertThat(filter.getPropConditions())
            .extracting(KubePropCondition::getField, KubePropCondition::getOperator, KubePropCondition::getValue)
            .containsExactly(
                tuple("pod_labels", "equal", "app=nginx,tier!=frontend"),
                tuple("pod_name", "contains", "nginx"));
    }

    // ------------------------------------------------------------ 闭环驱动

    private TaskTemplateInfoDTO roundTrip(TaskTemplateInfoDTO original) {
        V4UpdateJobTemplateRequest request = toUpdateRequest(readDetail(original));
        return writeConverter.toUpdateTemplateInfo(USERNAME, APP_ID, request, original);
    }

    private OpenApiV4JobTemplateDetailDTO readDetail(TaskTemplateInfoDTO original) {
        Map<Integer, String> fileSourceCodeMap = new HashMap<>();
        fileSourceCodeMap.put(FILE_SOURCE_ID, FILE_SOURCE_CODE);
        return OpenApiV4JobTemplateConverter.toDetailDTO(original, appScopeMappingService, fileSourceCodeMap);
    }

    // ------------------------------------------------------------ 调用方视角的字段映射：响应 -> 更新请求

    private V4UpdateJobTemplateRequest toUpdateRequest(OpenApiV4JobTemplateDetailDTO detail) {
        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setId(detail.getId());
        request.setName(detail.getName());
        request.setDescription(detail.getDescription());
        request.setGlobalVarList(detail.getGlobalVarList().stream()
            .map(this::toGlobalVarReq)
            .collect(Collectors.toList()));
        request.setStepList(detail.getStepList().stream()
            .map(this::toStepReq)
            .collect(Collectors.toList()));
        return request;
    }

    private V4JobTemplateGlobalVarReq toGlobalVarReq(V4JobTemplateGlobalVarDTO var) {
        V4JobTemplateGlobalVarReq req = new V4JobTemplateGlobalVarReq();
        req.setName(var.getName());
        req.setType(var.getType());
        req.setDescription(var.getDescription());
        req.setRequired(var.getRequired());
        req.setValue(var.getValue());
        req.setExecuteTarget(toVarTargetReq(var.getExecuteTarget()));
        return req;
    }

    private V4JobTemplateStepReq toStepReq(V4JobTemplateStepDTO step) {
        V4JobTemplateStepReq req = new V4JobTemplateStepReq();
        req.setId(step.getId());
        req.setName(step.getName());
        req.setType(step.getType());
        if (step.getScriptInfo() != null) {
            req.setScriptInfo(toScriptStepReq(step.getScriptInfo()));
        }
        if (step.getFileInfo() != null) {
            req.setFileInfo(toFileStepReq(step.getFileInfo()));
        }
        if (step.getApprovalInfo() != null) {
            req.setApprovalInfo(toApprovalStepReq(step.getApprovalInfo()));
        }
        return req;
    }

    private V4JobTemplateScriptStepReq toScriptStepReq(V4JobTemplateScriptStepDTO scriptInfo) {
        V4JobTemplateScriptStepReq req = new V4JobTemplateScriptStepReq();
        req.setScriptType(scriptInfo.getScriptType());
        req.setScriptId(scriptInfo.getScriptId());
        req.setScriptVersionId(scriptInfo.getScriptVersionId());
        req.setScriptContent(scriptInfo.getScriptContent());
        req.setScriptLanguage(scriptInfo.getScriptLanguage());
        req.setScriptParam(scriptInfo.getScriptParam());
        req.setWindowsInterpreter(scriptInfo.getWindowsInterpreter());
        req.setScriptTimeout(scriptInfo.getScriptTimeout());
        req.setIsParamSensitive(scriptInfo.getIsParamSensitive());
        req.setIsIgnoreError(scriptInfo.getIsIgnoreError());
        req.setAccount(toAccountReq(scriptInfo.getAccount()));
        req.setExecuteTarget(toTargetReq(scriptInfo.getExecuteTarget()));
        return req;
    }

    private V4JobTemplateFileStepReq toFileStepReq(V4JobTemplateFileStepDTO fileInfo) {
        V4JobTemplateFileStepReq req = new V4JobTemplateFileStepReq();
        req.setFileSourceList(fileInfo.getFileSourceList().stream()
            .map(this::toFileSourceReq)
            .collect(Collectors.toList()));
        V4JobTemplateFileDestinationReq destination = new V4JobTemplateFileDestinationReq();
        destination.setPath(fileInfo.getFileDestination().getPath());
        destination.setAccount(toAccountReq(fileInfo.getFileDestination().getAccount()));
        destination.setExecuteTarget(toTargetReq(fileInfo.getFileDestination().getExecuteTarget()));
        req.setFileDestination(destination);
        req.setTimeout(fileInfo.getTimeout());
        req.setTransferMode(fileInfo.getTransferMode());
        req.setSourceSpeedLimit(fileInfo.getSourceSpeedLimit());
        req.setDestinationSpeedLimit(fileInfo.getDestinationSpeedLimit());
        req.setIsIgnoreError(fileInfo.getIsIgnoreError());
        return req;
    }

    /**
     * 写入侧没有 file_source_code 入参，调用方原样回写时它自然被丢弃。
     */
    private V4JobTemplateFileSourceReq toFileSourceReq(V4JobTemplateFileSourceDTO fileSource) {
        V4JobTemplateFileSourceReq req = new V4JobTemplateFileSourceReq();
        req.setFileList(fileSource.getFileList());
        req.setFileType(fileSource.getFileType());
        req.setFileSourceId(fileSource.getFileSourceId());
        req.setAccount(toAccountReq(fileSource.getAccount()));
        req.setExecuteTarget(toTargetReq(fileSource.getExecuteTarget()));
        return req;
    }

    private V4JobTemplateApprovalStepReq toApprovalStepReq(V4JobTemplateApprovalStepDTO approvalInfo) {
        V4JobTemplateApprovalStepReq req = new V4JobTemplateApprovalStepReq();
        req.setApprovalType(approvalInfo.getApprovalType());
        V4JobTemplateApprovalUserReq user = new V4JobTemplateApprovalUserReq();
        user.setUserList(approvalInfo.getApprovalUser().getUserList());
        user.setRoleList(approvalInfo.getApprovalUser().getRoleList());
        req.setApprovalUser(user);
        req.setApprovalMessage(approvalInfo.getApprovalMessage());
        req.setNotifyChannel(approvalInfo.getNotifyChannel());
        return req;
    }

    /**
     * 账号的 name / alias 是只读回显，写入侧只有 id 与 account_var。
     */
    private V4JobTemplateAccountReq toAccountReq(V4JobTemplateAccountDTO account) {
        if (account == null) {
            return null;
        }
        V4JobTemplateAccountReq req = new V4JobTemplateAccountReq();
        req.setId(account.getId());
        req.setAccountVar(account.getAccountVar());
        return req;
    }

    /**
     * 执行目标的叶子类型（主机、动态分组、拓扑节点、容器）读写共用，直接搬运即可。
     */
    private V4JobTemplateExecuteTargetReq toTargetReq(V4JobTemplateExecuteTargetDTO target) {
        if (target == null) {
            return null;
        }
        V4JobTemplateExecuteTargetReq req = fillTargetReq(new V4JobTemplateExecuteTargetReq(), target);
        req.setVariable(target.getVariable());
        return req;
    }

    /**
     * 变量默认值的执行目标不含 variable，读接口在该位置也不会返回它。
     */
    private V4JobTemplateVarTargetReq toVarTargetReq(V4JobTemplateExecuteTargetDTO target) {
        if (target == null) {
            return null;
        }
        return fillTargetReq(new V4JobTemplateVarTargetReq(), target);
    }

    private <T extends V4JobTemplateTargetReq> T fillTargetReq(T req, V4JobTemplateExecuteTargetDTO target) {
        req.setHostList(target.getHostList());
        req.setDynamicGroups(target.getDynamicGroups());
        req.setTopoNodes(target.getTopoNodes());
        req.setContainerList(target.getContainerList());
        req.setContainerFilters(target.getContainerFilters());
        return req;
    }

    // ------------------------------------------------------------ 原始模板

    private TaskTemplateInfoDTO buildOriginalTemplate() {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
        template.setName("round-trip");
        template.setDescription("built by web");
        template.setCreator("creator");
        template.setCreateTime(1600000000L);
        template.setLastModifyUser("creator");
        template.setLastModifyTime(1600000000L);
        template.setTags(Collections.emptyList());
        template.setStepList(Arrays.asList(buildScriptStep(), buildFileStep(), buildApprovalStep()));
        template.setVariableList(Arrays.asList(
            buildVariable(101L, "plain", TaskVariableTypeEnum.STRING, "v1"),
            buildVariable(102L, "secret", TaskVariableTypeEnum.CIPHER, "real-password"),
            buildVariable(103L, "targets", TaskVariableTypeEnum.EXECUTE_OBJECT_LIST,
                hostTarget().toJsonString())
        ));
        return template;
    }

    private TaskStepDTO buildScriptStep() {
        TaskScriptStepDTO scriptStep = new TaskScriptStepDTO();
        scriptStep.setScriptSource(TaskScriptSourceEnum.LOCAL);
        scriptStep.setContent("echo hello");
        scriptStep.setLanguage(ScriptTypeEnum.SHELL);
        scriptStep.setScriptParam("--verbose");
        scriptStep.setWindowsInterpreter("");
        scriptStep.setTimeout(600L);
        scriptStep.setSecureParam(true);
        scriptStep.setIgnoreError(false);
        scriptStep.setAccount(1000L);
        scriptStep.setExecuteTarget(hostTarget());

        TaskStepDTO step = new TaskStepDTO();
        step.setId(10L);
        step.setName("run-script");
        step.setType(TaskStepTypeEnum.SCRIPT);
        step.setScriptStepInfo(scriptStep);
        return step;
    }

    private TaskStepDTO buildFileStep() {
        TaskFileInfoDTO serverFile = new TaskFileInfoDTO();
        serverFile.setFileType(TaskFileTypeEnum.SERVER);
        serverFile.setFileLocation(Collections.singletonList("/data/a.txt"));
        serverFile.setHostAccount(1001L);
        serverFile.setHost(hostTarget());

        TaskFileInfoDTO fileSourceFile = new TaskFileInfoDTO();
        fileSourceFile.setFileType(TaskFileTypeEnum.FILE_SOURCE);
        fileSourceFile.setFileLocation(Collections.singletonList("bucket/b.txt"));
        fileSourceFile.setFileSourceId(FILE_SOURCE_ID);

        TaskFileStepDTO fileStep = new TaskFileStepDTO();
        fileStep.setOriginFileList(Arrays.asList(serverFile, fileSourceFile));
        fileStep.setDestinationFileLocation("/tmp/");
        fileStep.setExecuteAccount(1002L);
        fileStep.setDestinationHostList(containerTarget());
        fileStep.setTimeout(1200L);
        fileStep.setOriginSpeedLimit(100L);
        fileStep.setTargetSpeedLimit(200L);
        fileStep.setIgnoreError(true);
        fileStep.setDuplicateHandler(
            DuplicateHandlerEnum.GROUP_BY_IP);
        fileStep.setNotExistPathHandler(
            NotExistPathHandlerEnum.CREATE_DIR);

        TaskStepDTO step = new TaskStepDTO();
        step.setId(20L);
        step.setName("push-file");
        step.setType(TaskStepTypeEnum.FILE);
        step.setFileStepInfo(fileStep);
        return step;
    }

    private TaskStepDTO buildApprovalStep() {
        UserRoleInfoDTO approvalUser = new UserRoleInfoDTO();
        approvalUser.setUserList(Collections.singletonList("admin"));
        approvalUser.setRoleList(Collections.singletonList("JOB_RESOURCE_TRIGGER_USER"));

        TaskApprovalStepDTO approvalStep = new TaskApprovalStepDTO();
        approvalStep.setApprovalType(TaskApprovalTypeEnum.ANYONE);
        approvalStep.setApprovalUser(approvalUser);
        approvalStep.setApprovalMessage("please approve");
        approvalStep.setNotifyChannel(Collections.singletonList("weixin"));

        TaskStepDTO step = new TaskStepDTO();
        step.setId(30L);
        step.setName("approve");
        step.setType(TaskStepTypeEnum.APPROVAL);
        step.setApprovalStepInfo(approvalStep);
        return step;
    }

    private TaskVariableDTO buildVariable(Long id, String name, TaskVariableTypeEnum type, String value) {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(type);
        variable.setDefaultValue(value);
        variable.setDescription("desc of " + name);
        variable.setRequired(true);
        variable.setChangeable(true);
        variable.setDelete(false);
        variable.setFollowTemplate(false);
        return variable;
    }

    private TaskTargetDTO hostTarget() {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(HOST_ID);
        host.setCloudAreaId(0L);
        host.setIp("127.0.0.1");

        TaskNodeInfoDTO topoNode = new TaskNodeInfoDTO();
        topoNode.setId(88L);
        topoNode.setType("module");

        TaskHostNodeDTO hostNode = new TaskHostNodeDTO();
        hostNode.setHostList(Collections.singletonList(host));
        hostNode.setDynamicGroupId(Collections.singletonList("dg-1"));
        hostNode.setNodeInfoList(Collections.singletonList(topoNode));

        TaskTargetDTO target = new TaskTargetDTO();
        target.setHostNodeList(hostNode);
        return target;
    }

    private TaskTargetDTO containerTarget() {
        TaskTargetContainerDTO container = new TaskTargetContainerDTO();
        container.setId(3001L);
        container.setContainerId("docker://abcdef");
        container.setName("nginx");
        container.setPodName("nginx-7d9f");
        container.setNamespace("default");
        container.setClusterUID("BCS-K8S-00001");

        KubeTopoDTO topo = new KubeTopoDTO();
        topo.setCluster(new KubeClusterObjectDTO(105L));
        topo.setNamespace(new KubeNamespaceObjectDTO(206L));
        topo.setWorkloads(Collections.singletonList(new KubeWorkloadObjectDTO("deployment", 307L)));

        KubeContainerFilter filter = new KubeContainerFilter();
        filter.setName("prod-pods");
        filter.setKubeTopoList(Collections.singletonList(topo));
        filter.setPropConditions(Arrays.asList(
            new KubePropCondition("pod_labels", "equal", "app=nginx,tier!=frontend"),
            new KubePropCondition("pod_name", "contains", "nginx")));

        TaskTargetDTO target = new TaskTargetDTO();
        target.setContainerList(Collections.singletonList(container));
        target.setContainerFilters(Collections.singletonList(filter));
        return target;
    }
}
