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

import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.KubeContainerFilter;
import com.tencent.bk.job.common.model.dto.KubePropCondition;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.api.esb.impl.v4.OpenApiV4JobTemplateWriteConverter;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetContainerDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateAccountReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateApprovalUserReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateContainerFilterDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateExecuteTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateVarTargetReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileDestinationReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileSourceReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateFileStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateGlobalVarReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplatePropConditionDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateScriptStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4JobTemplateStepReq;
import com.tencent.bk.job.manage.model.esb.v4.req.V4KubeObjectDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4KubeTopoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4KubeWorkloadObjectDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import com.tencent.bk.job.manage.service.ScriptManager;
import com.tencent.bk.job.manage.service.template.TemplateLocalFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link OpenApiV4JobTemplateWriteConverter} 单元测试，重点覆盖「终态 → 增量」转换。
 */
class OpenApiV4JobTemplateWriteConverterTest {

    private static final String USERNAME = "tester";
    private static final Long APP_ID = 2L;
    private static final Long TEMPLATE_ID = 1000L;

    private TemplateLocalFileService templateLocalFileService;
    private ScriptManager scriptManager;
    private OpenApiV4JobTemplateWriteConverter converter;

    @BeforeEach
    void setUp() {
        templateLocalFileService = mock(TemplateLocalFileService.class);
        scriptManager = mock(ScriptManager.class);
        converter = new OpenApiV4JobTemplateWriteConverter(templateLocalFileService, scriptManager);
    }

    /**
     * 让指定脚本版本存在，且其语言为 language。
     */
    private void mockScriptVersion(Long scriptVersionId, ScriptTypeEnum language) {
        ScriptDTO script = new ScriptDTO();
        script.setScriptVersionId(scriptVersionId);
        script.setType(language.getValue());
        when(scriptManager.batchGetScriptVersionsByIds(anyCollection()))
            .thenReturn(Collections.singletonMap(scriptVersionId, script));
    }

    // ------------------------------------------------------------ 创建

    @Test
    @DisplayName("创建：基础信息与步骤顺序按请求填充，creator 与状态由转换器补齐")
    void create_fills_basic_info_and_keeps_step_order() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setDescription("created via v4");
        request.setStepList(Arrays.asList(
            approvalStepReq(null, "step-a"),
            approvalStepReq(null, "step-b")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        assertThat(templateInfo.getAppId()).isEqualTo(APP_ID);
        assertThat(templateInfo.getName()).isEqualTo("api-template");
        assertThat(templateInfo.getDescription()).isEqualTo("created via v4");
        assertThat(templateInfo.getCreator()).isEqualTo(USERNAME);
        assertThat(templateInfo.getLastModifyUser()).isEqualTo(USERNAME);
        assertThat(templateInfo.getStatus()).isEqualTo(TaskTemplateStatusEnum.NEW);
        assertThat(templateInfo.getTags()).isEmpty();
        assertThat(templateInfo.getStepList())
            .extracting(TaskStepDTO::getName, TaskStepDTO::getId, TaskStepDTO::getDelete)
            .containsExactly(
                tuple("step-a", null, 0),
                tuple("step-b", null, 0)
            );
    }

    @Test
    @DisplayName("创建：请求携带步骤 ID 时被拒绝")
    void create_rejects_step_id() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setStepList(Collections.singletonList(approvalStepReq(11L, "step-a")));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("创建：本地文件由服务端补齐 hash 与大小")
    void create_fills_local_file_hash_and_size() {
        when(templateLocalFileService.getFileDetail(anyLong(), anyString()))
            .thenReturn(new TemplateLocalFileService.LocalFileDetail("md5-value", 1024L));

        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setStepList(Collections.singletonList(
            localFileStepReq(Collections.singletonList("2/uuid/tester/a.sh"))));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        assertThat(templateInfo.getStepList().get(0).getFileStepInfo().getOriginFileList())
            .singleElement()
            .satisfies(fileInfo -> {
                assertThat(fileInfo.getFileType()).isEqualTo(TaskFileTypeEnum.LOCAL);
                assertThat(fileInfo.getFileHash()).isEqualTo("md5-value");
                assertThat(fileInfo.getFileSize()).isEqualTo(1024L);
            });
    }

    @Test
    @DisplayName("创建：一个本地文件源条目携带多个路径时被拒绝")
    void create_rejects_multiple_paths_in_one_local_file_source() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setStepList(Collections.singletonList(
            localFileStepReq(Arrays.asList("2/uuid/tester/a.sh", "2/uuid/tester/b.sh"))));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("创建：本地脚本内容按 base64 解码后落库")
    void create_decodes_base64_script_content() {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setStepList(Collections.singletonList(localScriptStepReq()));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        assertThat(templateInfo.getStepList().get(0).getScriptStepInfo().getContent()).isEqualTo("echo hello");
        assertThat(templateInfo.getStepList().get(0).getScriptStepInfo().getScriptSource())
            .isEqualTo(TaskScriptSourceEnum.LOCAL);
    }

    @Test
    @DisplayName("创建：引用脚本只落库脚本 ID 与版本，不写入脚本内容")
    void create_keeps_script_reference_without_content() {
        mockScriptVersion(9527L, ScriptTypeEnum.SHELL);
        // 引用脚本即使带了内容也不该落库，内容以引用的脚本版本为准
        V4JobTemplateStepReq step = refScriptStep(9527L, "ZWNobyBoZWxsbw==");

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(step));

        TaskScriptStepDTO scriptStep = converter.toCreateTemplateInfo(USERNAME, APP_ID, request)
            .getStepList().get(0).getScriptStepInfo();

        assertThat(scriptStep.getScriptSource()).isEqualTo(TaskScriptSourceEnum.CITING);
        assertThat(scriptStep.getScriptId()).isEqualTo("script-uuid");
        assertThat(scriptStep.getScriptVersionId()).isEqualTo(9527L);
        assertThat(scriptStep.getContent()).isNull();
        assertThat(scriptStep.getLanguage()).isEqualTo(ScriptTypeEnum.SHELL);
    }

    @Test
    @DisplayName("创建：引用脚本的语言取自被引用版本，请求里传的 script_language 被忽略")
    void ref_script_language_comes_from_version_not_request() {
        mockScriptVersion(9527L, ScriptTypeEnum.PYTHON);
        V4JobTemplateStepReq step = refScriptStep(9527L, null);
        // 调用方传了与实际版本不符的语言，落库必须以版本为准，否则页面会显示错误的脚本语言
        step.getScriptInfo().setScriptLanguage(ScriptTypeEnum.SHELL.getValue());

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(step));

        TaskScriptStepDTO scriptStep = converter.toCreateTemplateInfo(USERNAME, APP_ID, request)
            .getStepList().get(0).getScriptStepInfo();

        assertThat(scriptStep.getLanguage()).isEqualTo(ScriptTypeEnum.PYTHON);
    }

    @Test
    @DisplayName("创建：引用脚本不传 script_language 时也能补出语言，不会因语言为空落库失败")
    void ref_script_without_language_is_filled_from_version() {
        mockScriptVersion(9527L, ScriptTypeEnum.PYTHON);
        V4JobTemplateStepReq step = refScriptStep(9527L, null);

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(step));

        TaskScriptStepDTO scriptStep = converter.toCreateTemplateInfo(USERNAME, APP_ID, request)
            .getStepList().get(0).getScriptStepInfo();

        assertThat(scriptStep.getLanguage()).isEqualTo(ScriptTypeEnum.PYTHON);
    }

    @Test
    @DisplayName("创建：引用的脚本版本不存在时报参数错误，而不是让空语言落到 DAO 抛 NPE")
    void ref_script_with_unknown_version_is_rejected() {
        when(scriptManager.batchGetScriptVersionsByIds(anyCollection())).thenReturn(Collections.emptyMap());
        V4JobTemplateStepReq step = refScriptStep(9527L, null);

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(step));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("9527"));
    }

    @Test
    @DisplayName("创建：多个步骤引用脚本时只查一次脚本版本")
    void ref_script_versions_are_queried_in_one_batch() {
        mockScriptVersion(9527L, ScriptTypeEnum.SHELL);
        V4CreateJobTemplateRequest request = createRequest(Arrays.asList(
            refScriptStep(9527L, null), refScriptStep(9527L, null)));

        converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        verify(scriptManager, times(1)).batchGetScriptVersionsByIds(anyCollection());
    }

    private V4JobTemplateStepReq refScriptStep(Long scriptVersionId, String scriptContent) {
        V4JobTemplateScriptStepReq scriptInfo = new V4JobTemplateScriptStepReq();
        scriptInfo.setScriptType(TaskScriptSourceEnum.CITING.getType());
        scriptInfo.setScriptId("script-uuid");
        scriptInfo.setScriptVersionId(scriptVersionId);
        scriptInfo.setScriptContent(scriptContent);
        scriptInfo.setAccount(accountReq());
        scriptInfo.setExecuteTarget(variableTargetReq());

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setName("cite-script");
        step.setType(TaskStepTypeEnum.SCRIPT.getValue());
        step.setScriptInfo(scriptInfo);
        return step;
    }

    // ------------------------------------------------------------ 更新：步骤 diff

    @Test
    @DisplayName("更新：省略的既有步骤被补上 delete=1，保留的步骤 ID 不变")
    void update_marks_omitted_steps_as_deleted() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Arrays.asList(existingApprovalStep(1L), existingApprovalStep(2L), existingApprovalStep(3L)),
            Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Arrays.asList(
            approvalStepReq(1L, "step-1"),
            approvalStepReq(3L, "step-3")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getStepList())
            .extracting(TaskStepDTO::getId, TaskStepDTO::getDelete)
            .containsExactly(tuple(1L, 0), tuple(3L, 0), tuple(2L, 1));
    }

    @Test
    @DisplayName("更新：删除首个步骤时同样补 delete=1，避免旧链头重新出现")
    void update_marks_first_step_as_deleted() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Arrays.asList(existingApprovalStep(1L), existingApprovalStep(2L), existingApprovalStep(3L)),
            Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Arrays.asList(
            approvalStepReq(2L, "step-2"),
            approvalStepReq(3L, "step-3")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(deletedStepIds(templateInfo)).containsExactly(1L);
        assertThat(keptStepIds(templateInfo)).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("更新：重排步骤时 ID 不变，顺序按请求数组")
    void update_reorders_steps_without_changing_ids() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Arrays.asList(existingApprovalStep(1L), existingApprovalStep(2L), existingApprovalStep(3L)),
            Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Arrays.asList(
            approvalStepReq(3L, "step-3"),
            approvalStepReq(1L, "step-1"),
            approvalStepReq(2L, "step-2")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(keptStepIds(templateInfo)).containsExactly(3L, 1L, 2L);
        assertThat(deletedStepIds(templateInfo)).isEmpty();
    }

    @Test
    @DisplayName("更新：不带 ID 的步骤视为新增，与既有步骤混排")
    void update_treats_step_without_id_as_new() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Arrays.asList(
            approvalStepReq(1L, "step-1"),
            approvalStepReq(null, "new-step")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getStepList())
            .extracting(TaskStepDTO::getId, TaskStepDTO::getName)
            .containsExactly(tuple(1L, "step-1"), tuple(null, "new-step"));
    }

    @Test
    @DisplayName("更新：步骤 ID 不属于该模板时拒绝，并指明非法 ID")
    void update_rejects_step_id_of_other_template() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(999L, "step-999")));

        assertThatThrownBy(() -> converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("999"));
    }

    @Test
    @DisplayName("更新：同一请求内步骤 ID 重复时拒绝")
    void update_rejects_duplicated_step_id() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Arrays.asList(existingApprovalStep(1L), existingApprovalStep(2L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Arrays.asList(
            approvalStepReq(1L, "step-1"),
            approvalStepReq(1L, "step-1-again")
        ));

        assertThatThrownBy(() -> converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("duplicated step id"));
    }

    @Test
    @DisplayName("更新：不传 name 时回填模板原名，而不是留空")
    void update_without_name_backfills_existing_name() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setName(null);

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        // 留空会让服务层的重名校验退化成 name = NULL，并让审计记到没有名字的变更
        assertThat(templateInfo.getName()).isEqualTo("demo-template");
    }

    @Test
    @DisplayName("更新：name 传全空白等同于不改名")
    void update_with_blank_name_backfills_existing_name() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setName("   ");

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getName()).isEqualTo("demo-template");
    }

    @Test
    @DisplayName("更新：传了 name 就按新名字改，不被原名覆盖")
    void update_with_name_renames_template() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setName("renamed-template");

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getName()).isEqualTo("renamed-template");
    }

    @Test
    @DisplayName("更新：v4 不接收 tags，模板原有标签被保留")
    void update_keeps_existing_tags() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());
        TagDTO tag = new TagDTO();
        tag.setId(7L);
        tag.setName("prod");
        existing.setTags(Collections.singletonList(tag));

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getTags()).extracting(TagDTO::getId).containsExactly(7L);
    }

    // ------------------------------------------------------------ 更新：变量 diff

    @Test
    @DisplayName("更新：同名变量解析为既有变量 ID，省略的变量补删除标记")
    void update_matches_variables_by_name() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)),
            Arrays.asList(existingVariable(21L, "KEEP"), existingVariable(22L, "DROP")));

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setGlobalVarList(Arrays.asList(
            stringVariableReq("KEEP", "new-value"),
            stringVariableReq("ADDED", "added-value")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getVariableList())
            .extracting(TaskVariableDTO::getName, TaskVariableDTO::getId, TaskVariableDTO::getDelete)
            .containsExactly(
                tuple("KEEP", 21L, false),
                tuple("ADDED", 0L, false),
                tuple("DROP", 22L, true)
            );
        assertThat(templateInfo.getVariableList().get(0).getDefaultValue()).isEqualTo("new-value");
    }

    @Test
    @DisplayName("更新：请求内变量重名时拒绝")
    void update_rejects_duplicated_variable_name() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)), Collections.emptyList());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setGlobalVarList(Arrays.asList(
            stringVariableReq("DUP", "a"),
            stringVariableReq("DUP", "b")
        ));

        assertThatThrownBy(() -> converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("DUP"));
    }

    @Test
    @DisplayName("更新：密文变量原样回写掩码时保留原值")
    void update_keeps_cipher_value_when_mask_written_back() {
        TaskVariableDTO cipherVariable = new TaskVariableDTO();
        cipherVariable.setId(31L);
        cipherVariable.setName("SECRET");
        cipherVariable.setType(TaskVariableTypeEnum.CIPHER);
        cipherVariable.setDefaultValue("real-secret");
        cipherVariable.setChangeable(true);
        cipherVariable.setRequired(false);
        cipherVariable.setDelete(false);
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)),
            Collections.singletonList(cipherVariable));

        V4JobTemplateGlobalVarReq cipherReq = new V4JobTemplateGlobalVarReq();
        cipherReq.setName("SECRET");
        cipherReq.setType(TaskVariableTypeEnum.CIPHER.getType());
        cipherReq.setValue(TaskVariableTypeEnum.CIPHER.getMask());

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setGlobalVarList(Collections.singletonList(cipherReq));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getVariableList().get(0).getDefaultValue()).isEqualTo("real-secret");
    }

    @Test
    @DisplayName("容器：静态容器只取 container_id 落库，回传的快照字段被忽略")
    void static_container_keeps_id_only() {
        V4JobTemplateContainerDTO containerReq = new V4JobTemplateContainerDTO(3001L);
        containerReq.setName("nginx");
        containerReq.setPodName("nginx-7d9f");
        containerReq.setNamespace("default");
        containerReq.setClusterUID("BCS-K8S-00001");

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(containerReq, null))));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        List<TaskTargetContainerDTO> containers =
            templateInfo.getStepList().get(0).getScriptStepInfo().getExecuteTarget().getContainerList();
        assertThat(containers).hasSize(1);
        assertThat(containers.get(0).getId()).isEqualTo(3001L);
        assertThat(containers.get(0).getName()).isNull();
        assertThat(containers.get(0).getNamespace()).isNull();
    }

    @Test
    @DisplayName("容器：动态筛选按拓扑层级逐级落库，集群 ID 原样透传不做翻译")
    void container_filter_maps_topo_hierarchy() {
        V4KubeTopoDTO clusterOnly = new V4KubeTopoDTO();
        clusterOnly.setCluster(new V4KubeObjectDTO(105L));

        V4KubeTopoDTO downToWorkload = new V4KubeTopoDTO();
        downToWorkload.setCluster(new V4KubeObjectDTO(106L));
        downToWorkload.setNamespace(new V4KubeObjectDTO(207L));
        downToWorkload.setWorkloads(Collections.singletonList(
            new V4KubeWorkloadObjectDTO("deployment", 309L)));

        V4JobTemplateContainerFilterDTO filterReq = new V4JobTemplateContainerFilterDTO();
        filterReq.setName("prod-workloads");
        filterReq.setKubeTopoList(Arrays.asList(clusterOnly, downToWorkload));

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(null, filterReq))));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        List<KubeContainerFilter> filters =
            templateInfo.getStepList().get(0).getScriptStepInfo().getExecuteTarget().getContainerFilters();
        assertThat(filters).hasSize(1);
        KubeContainerFilter filter = filters.get(0);
        assertThat(filter.getName()).isEqualTo("prod-workloads");
        assertThat(filter.getKubeTopoList()).hasSize(2);

        KubeTopoDTO firstTopo = filter.getKubeTopoList().get(0);
        assertThat(firstTopo.getCluster().getId()).isEqualTo(105L);
        assertThat(firstTopo.getNamespace()).isNull();
        assertThat(firstTopo.getWorkloads()).isNull();

        KubeTopoDTO secondTopo = filter.getKubeTopoList().get(1);
        assertThat(secondTopo.getCluster().getId()).isEqualTo(106L);
        assertThat(secondTopo.getNamespace().getId()).isEqualTo(207L);
        assertThat(secondTopo.getWorkloads()).hasSize(1);
        assertThat(secondTopo.getWorkloads().get(0).getKind()).isEqualTo("deployment");
        assertThat(secondTopo.getWorkloads().get(0).getId()).isEqualTo(309L);
    }

    @Test
    @DisplayName("容器：动态筛选的 prop_conditions 字段非法时拒绝")
    void container_filter_rejects_illegal_prop_condition() {
        V4KubeTopoDTO topo = new V4KubeTopoDTO();
        topo.setCluster(new V4KubeObjectDTO(105L));

        V4JobTemplateContainerFilterDTO filterReq = new V4JobTemplateContainerFilterDTO();
        filterReq.setKubeTopoList(Collections.singletonList(topo));
        filterReq.setPropConditions(Collections.singletonList(
            new V4JobTemplatePropConditionDTO("not_a_field", "x")));

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(null, filterReq))));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("容器：接口不收运算符，落库时按字段补齐为页面固定使用的那一个")
    void container_filter_fills_canonical_operator_per_field() {
        V4KubeTopoDTO topo = new V4KubeTopoDTO();
        topo.setCluster(new V4KubeObjectDTO(105L));

        V4JobTemplateContainerFilterDTO filterReq = new V4JobTemplateContainerFilterDTO();
        filterReq.setKubeTopoList(Collections.singletonList(topo));
        filterReq.setPropConditions(Arrays.asList(
            new V4JobTemplatePropConditionDTO("container_name", "nginx"),
            new V4JobTemplatePropConditionDTO("container_container_uid", "docker://abcdef"),
            new V4JobTemplatePropConditionDTO("pod_name", "nginx-7d9f"),
            new V4JobTemplatePropConditionDTO("pod_labels", "app=nginx,tier!=frontend")));

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(null, filterReq))));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        List<KubePropCondition> conditions = templateInfo.getStepList().get(0).getScriptStepInfo()
            .getExecuteTarget().getContainerFilters().get(0).getPropConditions();
        assertThat(conditions)
            .extracting(KubePropCondition::getField, KubePropCondition::getOperator, KubePropCondition::getValue)
            .containsExactly(
                tuple("container_name", "contains", "nginx"),
                tuple("container_container_uid", "equal", "docker://abcdef"),
                tuple("pod_name", "contains", "nginx-7d9f"),
                tuple("pod_labels", "equal", "app=nginx,tier!=frontend")
            );
    }

    @Test
    @DisplayName("容器：pod_labels 的表达式不合法时拒绝")
    void container_filter_rejects_illegal_pod_labels_expression() {
        V4KubeTopoDTO topo = new V4KubeTopoDTO();
        topo.setCluster(new V4KubeObjectDTO(105L));

        V4JobTemplateContainerFilterDTO filterReq = new V4JobTemplateContainerFilterDTO();
        filterReq.setKubeTopoList(Collections.singletonList(topo));
        filterReq.setPropConditions(Collections.singletonList(
            new V4JobTemplatePropConditionDTO("pod_labels", "ab@c=x")));

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(null, filterReq))));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("容器：字段非法时报错列出全部可选字段，便于调用方自查")
    void container_filter_error_lists_supported_fields() {
        V4KubeTopoDTO topo = new V4KubeTopoDTO();
        topo.setCluster(new V4KubeObjectDTO(105L));

        V4JobTemplateContainerFilterDTO filterReq = new V4JobTemplateContainerFilterDTO();
        filterReq.setKubeTopoList(Collections.singletonList(topo));
        filterReq.setPropConditions(Collections.singletonList(
            new V4JobTemplatePropConditionDTO("container_id", "x")));

        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(containerTargetReq(null, filterReq))));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e))
                .contains("container_name", "container_container_uid", "pod_name", "pod_labels"));
    }

    @Test
    @DisplayName("容器：更新既有容器目标步骤不再被拒绝，容器配置按请求覆盖")
    void update_step_with_container_target_is_allowed() {
        TaskStepDTO existingStep = existingApprovalStep(1L);
        existingStep.setType(TaskStepTypeEnum.SCRIPT);
        TaskScriptStepDTO existingScriptStep = new TaskScriptStepDTO();
        TaskTargetDTO existingTarget = new TaskTargetDTO();
        TaskTargetContainerDTO existingContainer = new TaskTargetContainerDTO();
        existingContainer.setId(3001L);
        existingTarget.setContainerList(Collections.singletonList(existingContainer));
        existingScriptStep.setExecuteTarget(existingTarget);
        existingStep.setScriptStepInfo(existingScriptStep);
        TaskTemplateInfoDTO existing =
            existingTemplate(Collections.singletonList(existingStep), Collections.emptyList());

        V4JobTemplateStepReq stepReq =
            scriptStepWithTarget(containerTargetReq(new V4JobTemplateContainerDTO(3002L), null));
        stepReq.setId(1L);
        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(stepReq));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        List<TaskTargetContainerDTO> containers =
            templateInfo.getStepList().get(0).getScriptStepInfo().getExecuteTarget().getContainerList();
        assertThat(containers).extracting(TaskTargetContainerDTO::getId).containsExactly(3002L);
    }

    @Test
    @DisplayName("文件源：行 id 补 0，服务层靠 id>0 区分原地更新与新增，为 null 会 NPE")
    void file_source_gets_zero_id_placeholder() {
        when(templateLocalFileService.getFileDetail(anyLong(), anyString()))
            .thenReturn(new TemplateLocalFileService.LocalFileDetail("md5-value", 1024L));
        V4JobTemplateStepReq step = localFileStepReq(Collections.singletonList("2/8f1c/admin/app.tar.gz"));

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(step));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        assertThat(templateInfo.getStepList().get(0).getFileStepInfo().getOriginFileList())
            .singleElement()
            .satisfies(file -> assertThat(file.getId()).isZero());
    }

    @Test
    @DisplayName("变量：新增变量补 id=0 占位，服务层靠 id>0 区分新增与更新，为 null 会 NPE")
    void new_variable_gets_zero_id_placeholder() {
        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(
            scriptStepWithTarget(variableTargetReq())));
        request.setGlobalVarList(Collections.singletonList(stringVariableReq("TARGET_DIR", "/data/release")));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        assertThat(templateInfo.getVariableList()).singleElement()
            .satisfies(variable -> assertThat(variable.getId()).isZero());
    }

    @Test
    @DisplayName("变量：更新时新增的变量同样补 id=0，既有变量保留原 id")
    void update_mixes_new_and_existing_variable_ids() {
        TaskTemplateInfoDTO existing = existingTemplate(
            Collections.singletonList(existingApprovalStep(1L)),
            Collections.singletonList(existingVariable(21L, "KEEP")));

        V4UpdateJobTemplateRequest request = updateRequest(Collections.singletonList(
            approvalStepReq(1L, "step-1")));
        request.setGlobalVarList(Arrays.asList(
            stringVariableReq("KEEP", "v1"),
            stringVariableReq("ADDED", "v2")
        ));

        TaskTemplateInfoDTO templateInfo = converter.toUpdateTemplateInfo(USERNAME, APP_ID, request, existing);

        assertThat(templateInfo.getVariableList())
            .extracting(TaskVariableDTO::getName, TaskVariableDTO::getId)
            .containsExactly(tuple("KEEP", 21L), tuple("ADDED", 0L));
    }

    @Test
    @DisplayName("执行目标：variable 与具体目标同时出现时拒绝，避免落库静默丢弃主机")
    void execute_target_rejects_variable_mixed_with_concrete_target() {
        V4JobTemplateExecuteTargetReq target = containerTargetReq(new V4JobTemplateContainerDTO(3001L), null);
        target.setVariable("TARGET_HOSTS");
        V4CreateJobTemplateRequest request = createRequest(
            Collections.singletonList(scriptStepWithTarget(target)));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("exclusive"));
    }

    @Test
    @DisplayName("变量默认值：执行目标为空时报错，报错定位到 global_var_list")
    void variable_default_target_rejects_empty() {
        V4JobTemplateGlobalVarReq variableReq = new V4JobTemplateGlobalVarReq();
        variableReq.setName("TARGET_HOSTS");
        variableReq.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        variableReq.setExecuteTarget(new V4JobTemplateVarTargetReq());

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(
            scriptStepWithTarget(variableTargetReq())));
        request.setGlobalVarList(Collections.singletonList(variableReq));

        assertThatThrownBy(() -> converter.toCreateTemplateInfo(USERNAME, APP_ID, request))
            .isInstanceOf(InvalidParamException.class)
            .satisfies(e -> assertThat(errorReason(e)).contains("execute_target"));
    }

    @Test
    @DisplayName("变量默认值：容器目标可正常落库，不含 variable 字段")
    void variable_default_target_keeps_container() {
        V4JobTemplateVarTargetReq varTarget = new V4JobTemplateVarTargetReq();
        varTarget.setContainerList(Collections.singletonList(new V4JobTemplateContainerDTO(3001L)));

        V4JobTemplateGlobalVarReq variableReq = new V4JobTemplateGlobalVarReq();
        variableReq.setName("TARGET_HOSTS");
        variableReq.setType(TaskVariableTypeEnum.EXECUTE_OBJECT_LIST.getType());
        variableReq.setExecuteTarget(varTarget);

        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(
            scriptStepWithTarget(variableTargetReq())));
        request.setGlobalVarList(Collections.singletonList(variableReq));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        String defaultValue = templateInfo.getVariableList().get(0).getDefaultValue();
        assertThat(defaultValue).contains("\"id\":3001").doesNotContain("variable");
    }

    @Test
    @DisplayName("容器：执行目标只有容器时不算空，主机维度缺省不报错")
    void container_only_target_is_not_empty() {
        V4CreateJobTemplateRequest request = createRequest(Collections.singletonList(
            scriptStepWithTarget(containerTargetReq(new V4JobTemplateContainerDTO(3001L), null))));

        TaskTemplateInfoDTO templateInfo = converter.toCreateTemplateInfo(USERNAME, APP_ID, request);

        TaskTargetDTO target = templateInfo.getStepList().get(0).getScriptStepInfo().getExecuteTarget();
        assertThat(target.getVariable()).isNull();
        assertThat(target.getContainerList()).hasSize(1);
    }

    // ------------------------------------------------------------ 构造工具

    /**
     * 参数错误的原因文案放在 errorParams 的第二位，异常自身的 message 为空。
     */
    private String errorReason(Throwable e) {
        Object[] errorParams = ((InvalidParamException) e).getErrorParams();
        return errorParams == null ? "" : String.valueOf(errorParams[errorParams.length - 1]);
    }

    private List<Long> keptStepIds(TaskTemplateInfoDTO templateInfo) {
        return templateInfo.getStepList().stream()
            .filter(step -> step.getDelete() == 0)
            .map(TaskStepDTO::getId)
            .collect(Collectors.toList());
    }

    private List<Long> deletedStepIds(TaskTemplateInfoDTO templateInfo) {
        return templateInfo.getStepList().stream()
            .filter(step -> step.getDelete() == 1)
            .map(TaskStepDTO::getId)
            .collect(Collectors.toList());
    }

    private TaskTemplateInfoDTO existingTemplate(List<TaskStepDTO> steps, List<TaskVariableDTO> variables) {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setId(TEMPLATE_ID);
        template.setAppId(APP_ID);
        template.setName("demo-template");
        template.setStepList(steps);
        template.setVariableList(variables);
        template.setTags(Collections.emptyList());
        return template;
    }

    private TaskStepDTO existingApprovalStep(Long id) {
        TaskStepDTO step = new TaskStepDTO();
        step.setId(id);
        step.setName("step-" + id);
        step.setType(TaskStepTypeEnum.APPROVAL);
        step.setDelete(0);
        return step;
    }

    private TaskVariableDTO existingVariable(Long id, String name) {
        TaskVariableDTO variable = new TaskVariableDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING);
        variable.setDefaultValue("old-value");
        variable.setChangeable(true);
        variable.setRequired(false);
        variable.setDelete(false);
        return variable;
    }

    private V4CreateJobTemplateRequest createRequest(List<V4JobTemplateStepReq> stepList) {
        V4CreateJobTemplateRequest request = new V4CreateJobTemplateRequest();
        request.setName("api-template");
        request.setStepList(stepList);
        return request;
    }

    private V4UpdateJobTemplateRequest updateRequest(List<V4JobTemplateStepReq> stepList) {
        V4UpdateJobTemplateRequest request = new V4UpdateJobTemplateRequest();
        request.setId(TEMPLATE_ID);
        request.setName("demo-template");
        request.setStepList(stepList);
        return request;
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

    private V4JobTemplateStepReq localScriptStepReq() {
        V4JobTemplateScriptStepReq scriptInfo = new V4JobTemplateScriptStepReq();
        scriptInfo.setScriptType(TaskScriptSourceEnum.LOCAL.getType());
        scriptInfo.setScriptContent("ZWNobyBoZWxsbw==");
        scriptInfo.setScriptLanguage(ScriptTypeEnum.SHELL.getValue());
        scriptInfo.setAccount(accountReq());
        scriptInfo.setExecuteTarget(variableTargetReq());

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setName("run-script");
        step.setType(TaskStepTypeEnum.SCRIPT.getValue());
        step.setScriptInfo(scriptInfo);
        return step;
    }

    private V4JobTemplateStepReq localFileStepReq(List<String> fileList) {
        V4JobTemplateFileSourceReq fileSource = new V4JobTemplateFileSourceReq();
        fileSource.setFileType(TaskFileTypeEnum.LOCAL.getType());
        fileSource.setFileList(fileList);

        V4JobTemplateFileDestinationReq destination = new V4JobTemplateFileDestinationReq();
        destination.setPath("/data/dest");
        destination.setAccount(accountReq());
        destination.setExecuteTarget(variableTargetReq());

        V4JobTemplateFileStepReq fileInfo = new V4JobTemplateFileStepReq();
        fileInfo.setFileSourceList(Collections.singletonList(fileSource));
        fileInfo.setFileDestination(destination);

        V4JobTemplateStepReq step = new V4JobTemplateStepReq();
        step.setName("transfer-file");
        step.setType(TaskStepTypeEnum.FILE.getValue());
        step.setFileInfo(fileInfo);
        return step;
    }

    private V4JobTemplateGlobalVarReq stringVariableReq(String name, String value) {
        V4JobTemplateGlobalVarReq variable = new V4JobTemplateGlobalVarReq();
        variable.setName(name);
        variable.setType(TaskVariableTypeEnum.STRING.getType());
        variable.setValue(value);
        return variable;
    }

    private V4JobTemplateAccountReq accountReq() {
        V4JobTemplateAccountReq account = new V4JobTemplateAccountReq();
        account.setId(1L);
        return account;
    }

    /**
     * 用变量引用型执行目标，避免触发依赖 Spring 上下文的主机明细补全。
     */
    private V4JobTemplateExecuteTargetReq variableTargetReq() {
        V4JobTemplateExecuteTargetReq target = new V4JobTemplateExecuteTargetReq();
        target.setVariable("TARGET_HOSTS");
        return target;
    }

    private V4JobTemplateExecuteTargetReq containerTargetReq(V4JobTemplateContainerDTO container,
                                                             V4JobTemplateContainerFilterDTO filter) {
        V4JobTemplateExecuteTargetReq target = new V4JobTemplateExecuteTargetReq();
        if (container != null) {
            target.setContainerList(Collections.singletonList(container));
        }
        if (filter != null) {
            target.setContainerFilters(Collections.singletonList(filter));
        }
        return target;
    }

    private V4JobTemplateStepReq scriptStepWithTarget(V4JobTemplateExecuteTargetReq target) {
        V4JobTemplateStepReq step = localScriptStepReq();
        step.getScriptInfo().setExecuteTarget(target);
        return step;
    }
}
