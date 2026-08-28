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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 快速分发文件引用第三方文件源的可用性校验单测
 */
@ExtendWith(MockitoExtension.class)
class FileSourceReferenceServiceImplTest {

    private static final String TENANT_ID = "tenant-1";
    private static final long APP_ID = 100L;

    @Mock
    private ServiceFileSourceResource fileSourceResource;

    @InjectMocks
    private FileSourceReferenceServiceImpl service;

    @Test
    @DisplayName("不含 fileType=3 的源文件时直接返回，不发起任何跨服务查询")
    void noThirdPartyFileSource_noRpc() {
        List<FileSourceDTO> fileSourceList = Collections.singletonList(
            fileSource(TaskFileTypeEnum.SERVER.getType(), null));

        assertThat(service.validateReferencedFileSources(TENANT_ID, APP_ID, fileSourceList)).isEmpty();
        verifyNoInteractions(fileSourceResource);
    }

    @Test
    @DisplayName("源文件列表为空时直接返回，不发起任何跨服务查询")
    void emptyFileSourceList_noRpc() {
        assertThat(service.validateReferencedFileSources(TENANT_ID, APP_ID, null)).isEmpty();
        verifyNoInteractions(fileSourceResource);
    }

    @Test
    @DisplayName("多个源文件引用同一文件源时 ID 去重，只发起一次查询")
    void duplicatedFileSourceId_queriedOnce() {
        stubAvailability(available(1));

        service.validateReferencedFileSources(TENANT_ID, APP_ID, Arrays.asList(
            fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1),
            fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1)));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(fileSourceResource, times(1))
            .checkFileSourceAvailability(eq(TENANT_ID), eq(APP_ID), captor.capture());
        assertThat(captor.getValue()).containsExactly(1);
    }

    @Test
    @DisplayName("文件源不在本业务可见范围内 → FILE_SOURCE_ID_NOT_IN_BIZ")
    void notInAppScope_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, false, APP_ID + 1, "other", true));

        assertThatThrownBy(() -> service.validateReferencedFileSources(TENANT_ID, APP_ID,
            Collections.singletonList(fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1))))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ);
    }

    @Test
    @DisplayName("文件源不存在 → 与未共享共用 FILE_SOURCE_ID_NOT_IN_BIZ，不泄漏存在性")
    void notExists_rejectedWithSameErrorCode() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, false, null, null, null));

        assertThatThrownBy(() -> service.validateReferencedFileSources(TENANT_ID, APP_ID,
            Collections.singletonList(fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1))))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ);
    }

    @Test
    @DisplayName("文件源可见但已禁用 → FILE_SOURCE_DISABLED")
    void disabled_rejected() {
        stubAvailability(new ServiceFileSourceAvailabilityDTO(1, true, APP_ID, "own", false));

        assertThatThrownBy(() -> service.validateReferencedFileSources(TENANT_ID, APP_ID,
            Collections.singletonList(fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1))))
            .isInstanceOf(FailedPreconditionException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_SOURCE_DISABLED);
    }

    @Test
    @DisplayName("查询失败 → InternalException，不放行")
    void queryFailed_throwsInternalException() {
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> failResp = new InternalResponse<>();
        failResp.setSuccess(false);
        failResp.setCode(ErrorCode.INTERNAL_ERROR);
        when(fileSourceResource.checkFileSourceAvailability(anyString(), anyLong(), any()))
            .thenReturn(failResp);

        assertThatThrownBy(() -> service.validateReferencedFileSources(TENANT_ID, APP_ID,
            Collections.singletonList(fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1))))
            .isInstanceOf(InternalException.class);
    }

    @Test
    @DisplayName("全部可用时返回查询结果供后续鉴权复用")
    void allAvailable_returnsQueryResult() {
        stubAvailability(available(1), available(2));

        List<ServiceFileSourceAvailabilityDTO> result = service.validateReferencedFileSources(
            TENANT_ID, APP_ID, Arrays.asList(
                fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 1),
                fileSource(TaskFileTypeEnum.FILE_SOURCE.getType(), 2)));

        assertThat(result).extracting(ServiceFileSourceAvailabilityDTO::getId).containsExactly(1, 2);
    }

    private void stubAvailability(ServiceFileSourceAvailabilityDTO... availabilities) {
        // 不用 InternalResponse.buildSuccessResp：它会经 I18nUtil 去取 Spring 容器里的 Bean，纯单测下拿不到
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> resp = new InternalResponse<>();
        resp.setSuccess(true);
        resp.setCode(ErrorCode.RESULT_OK);
        resp.setData(Arrays.asList(availabilities));
        when(fileSourceResource.checkFileSourceAvailability(anyString(), anyLong(), any())).thenReturn(resp);
    }

    private static ServiceFileSourceAvailabilityDTO available(int id) {
        return new ServiceFileSourceAvailabilityDTO(id, true, APP_ID + 1, "alias-" + id, true);
    }

    private static FileSourceDTO fileSource(int fileType, Integer fileSourceId) {
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setFileType(fileType);
        fileSource.setFileSourceId(fileSourceId);
        return fileSource;
    }
}
