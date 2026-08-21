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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.auth.FileSourceAuthService;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 作业模板保存时引用第三方文件源的校验与鉴权单测
 */
@ExtendWith(MockitoExtension.class)
class FileSourceReferenceServiceImplTest {

    private static final long APP_ID = 100L;
    private static final long TEMPLATE_ID = 2000L;

    @Mock
    private ServiceFileSourceResource fileSourceResource;
    @Mock
    private FileSourceAuthService fileSourceAuthService;
    @Mock
    private AbstractTaskStepService taskStepService;

    @InjectMocks
    private FileSourceReferenceServiceImpl service;

    private static final String USERNAME = "admin";

    @Test
    @DisplayName("模板未引用第三方文件源时不发起任何查询与鉴权")
    void noFileSourceReference_noRpc() {
        TaskTemplateInfoDTO template = template(fileStep(localFile()));

        service.validateReferencedFileSources(USERNAME, template, true);

        verifyNoInteractions(fileSourceResource, fileSourceAuthService, taskStepService);
    }

    @Test
    @DisplayName("创建时引用其他业务未共享的文件源 → FILE_SOURCE_ID_NOT_IN_BIZ")
    void create_notInAppScope_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, false, APP_ID + 1, "other", true));
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1)));

        assertThatThrownBy(() -> service.validateReferencedFileSources(USERNAME, template, true))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ);
    }

    @Test
    @DisplayName("创建时引用已禁用的文件源 → FILE_SOURCE_DISABLED，不做放行")
    void create_disabled_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, true, APP_ID, "own", false));
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1)));

        assertThatThrownBy(() -> service.validateReferencedFileSources(USERNAME, template, true))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_DISABLED);
        // 创建路径不需要回查原模板
        verify(taskStepService, never()).listFileSourceIdsByTemplateId(anyLong());
    }

    @Test
    @DisplayName("更新时原模板已引用的已禁用文件源被放行")
    void update_disabledButAlreadyReferenced_passed() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, true, APP_ID + 1, "shared", false));
        when(taskStepService.listFileSourceIdsByTemplateId(TEMPLATE_ID))
            .thenReturn(new HashSet<>(Collections.singletonList(1)));
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1)));
        template.setId(TEMPLATE_ID);

        assertThatCode(() -> service.validateReferencedFileSources(USERNAME, template, false))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("更新时新引入的已禁用文件源仍被拒")
    void update_newlyIntroducedDisabled_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(2, true, APP_ID + 1, "shared", false));
        when(taskStepService.listFileSourceIdsByTemplateId(TEMPLATE_ID))
            .thenReturn(new HashSet<>(Collections.singletonList(1)));
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(2)));
        template.setId(TEMPLATE_ID);

        assertThatThrownBy(() -> service.validateReferencedFileSources(USERNAME, template, false))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_DISABLED);
    }

    @Test
    @DisplayName("更新时原模板已引用但跨业务未共享的文件源仍被拒")
    void update_notInAppScopeEvenIfAlreadyReferenced_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, false, APP_ID + 1, "other", true));
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1)));
        template.setId(TEMPLATE_ID);

        assertThatThrownBy(() -> service.validateReferencedFileSources(USERNAME, template, false))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ);
    }

    @Test
    @DisplayName("只对归属本业务的文件源鉴权 view_file_source，共享过来的不鉴权")
    void authOnlyOwnFileSource() {
        stubAvailability(
            new ServiceFileSourceAvailabilityDTO(1, true, APP_ID, "own", true),
            new ServiceFileSourceAvailabilityDTO(2, true, APP_ID + 1, "shared", true));
        when(fileSourceAuthService.batchAuthViewFileSource(any(), any(), anyMap()))
            .thenReturn(AuthResult.pass());
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1), fileSourceFile(2)));

        service.validateReferencedFileSources(USERNAME, template, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<Integer, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(fileSourceAuthService, times(1))
            .batchAuthViewFileSource(any(), any(), captor.capture());
        assertThat(captor.getValue()).containsExactly(entry(1, "own"));
    }

    @Test
    @DisplayName("更新时原模板已引用但操作者无 view_file_source 权限 → 仍抛权限异常")
    void update_noViewPermission_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, true, APP_ID, "own", true));
        when(fileSourceAuthService.batchAuthViewFileSource(any(), any(), anyMap()))
            .thenReturn(AuthResult.fail());
        TaskTemplateInfoDTO template = template(fileStep(fileSourceFile(1)));
        template.setId(TEMPLATE_ID);

        assertThatThrownBy(() -> service.validateReferencedFileSources(USERNAME, template, false))
            .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    @DisplayName("多个步骤引用同一文件源时 ID 去重，批量查询与批量鉴权各只调一次")
    void multipleSteps_queryAndAuthOnce() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, true, APP_ID, "own", true));
        when(fileSourceAuthService.batchAuthViewFileSource(any(), any(), anyMap()))
            .thenReturn(AuthResult.pass());
        TaskTemplateInfoDTO template = template(
            fileStep(fileSourceFile(1)),
            fileStep(fileSourceFile(1)));

        service.validateReferencedFileSources(USERNAME, template, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileSourceResource, times(1))
            .checkFileSourceAvailability(anyLong(), captor.capture());
        assertThat(captor.getValue()).containsExactly(1);
        verify(fileSourceAuthService, times(1)).batchAuthViewFileSource(any(), any(), anyMap());
    }

    private void stubAvailability(ServiceFileSourceAvailabilityDTO... availabilities) {
        // 不用 InternalResponse.buildSuccessResp：它会经 I18nUtil 去取 Spring 容器里的 Bean，纯单测下拿不到
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> resp = new InternalResponse<>();
        resp.setSuccess(true);
        resp.setCode(ErrorCode.RESULT_OK);
        resp.setData(Arrays.asList(availabilities));
        when(fileSourceResource.checkFileSourceAvailability(anyLong(), any())).thenReturn(resp);
    }

    private static TaskTemplateInfoDTO template(TaskStepDTO... steps) {
        TaskTemplateInfoDTO template = new TaskTemplateInfoDTO();
        template.setAppId(APP_ID);
        template.setStepList(Arrays.asList(steps));
        return template;
    }

    private static TaskStepDTO fileStep(TaskFileInfoDTO... fileInfos) {
        TaskFileStepDTO fileStepInfo = new TaskFileStepDTO();
        fileStepInfo.setOriginFileList(Arrays.asList(fileInfos));
        TaskStepDTO step = new TaskStepDTO();
        step.setType(TaskStepTypeEnum.FILE);
        step.setFileStepInfo(fileStepInfo);
        return step;
    }

    private static TaskFileInfoDTO fileSourceFile(int fileSourceId) {
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
        fileInfo.setFileType(TaskFileTypeEnum.FILE_SOURCE);
        fileInfo.setFileSourceId(fileSourceId);
        return fileInfo;
    }

    private static TaskFileInfoDTO localFile() {
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
        fileInfo.setFileType(TaskFileTypeEnum.LOCAL);
        return fileInfo;
    }
}
