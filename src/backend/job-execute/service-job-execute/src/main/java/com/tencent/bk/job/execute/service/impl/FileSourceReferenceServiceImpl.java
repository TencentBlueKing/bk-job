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
import com.tencent.bk.job.execute.service.FileSourceReferenceService;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceAvailabilityDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
// service-job-manage 下已有同名类，轻量化部署时两者会在同一容器内注册，故显式指定 Bean 名避免冲突
@Service("jobExecuteFileSourceReferenceService")
public class FileSourceReferenceServiceImpl implements FileSourceReferenceService {

    private final ServiceFileSourceResource fileSourceResource;

    @Autowired
    public FileSourceReferenceServiceImpl(ServiceFileSourceResource fileSourceResource) {
        this.fileSourceResource = fileSourceResource;
    }

    @Override
    public List<ServiceFileSourceAvailabilityDTO> validateReferencedFileSources(String tenantId,
                                                                                long appId,
                                                                                List<FileSourceDTO> fileSourceList) {
        Set<Integer> fileSourceIds = extractFileSourceIds(fileSourceList);
        if (fileSourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ServiceFileSourceAvailabilityDTO> availabilityList =
            queryAvailability(tenantId, appId, new ArrayList<>(fileSourceIds));
        availabilityList.forEach(availability -> denyIfUnavailable(appId, availability));
        return availabilityList;
    }

    private Set<Integer> extractFileSourceIds(List<FileSourceDTO> fileSourceList) {
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return Collections.emptySet();
        }
        return fileSourceList.stream()
            .filter(fileSource -> fileSource.getFileType() != null
                && TaskFileTypeEnum.FILE_SOURCE.getType() == fileSource.getFileType()
                && fileSource.getFileSourceId() != null)
            .map(FileSourceDTO::getFileSourceId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<ServiceFileSourceAvailabilityDTO> queryAvailability(String tenantId,
                                                                     long appId,
                                                                     List<Integer> fileSourceIdList) {
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> resp =
            fileSourceResource.checkFileSourceAvailability(tenantId, appId, fileSourceIdList);
        if (resp == null || !resp.isSuccess() || resp.getData() == null) {
            log.error("Fail to check file source availability, appId={}, fileSourceIds={}, resp={}",
                appId, fileSourceIdList, resp);
            throw new InternalException(ErrorCode.FILE_SOURCE_SERVICE_INVALID);
        }
        return resp.getData();
    }

    /**
     * 「不存在」与「归属其他业务且未共享」刻意共用同一个错误码：对调用方而言两者等价，
     * 区分开会让接口变成一个探测其他业务文件源 ID 是否存在的工具。真实原因只落到服务端日志。
     * 「已禁用」只在文件源本就可见时才暴露，同样是为了不泄漏不可见文件源的存在。
     */
    private void denyIfUnavailable(long appId, ServiceFileSourceAvailabilityDTO availability) {
        if (availability.isAvailable()) {
            return;
        }
        Integer fileSourceId = availability.getId();
        String fileSourceDisplayName = availability.getDisplayName();
        if (!availability.isInAppScope()) {
            log.warn("File source is not available for app, appId={}, " +
                    "fileSourceDisplayName={}, exists={}, ownerAppId={}",
                appId, fileSourceDisplayName, availability.exists(), availability.getOwnerAppId());
            throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ,
                new Object[]{String.valueOf(fileSourceId)});
        }
        log.warn("File source is disabled, appId={}, fileSourceDisplayName={}, ownerAppId={}",
            appId, fileSourceDisplayName, availability.getOwnerAppId());
        throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_DISABLED,
            new Object[]{fileSourceDisplayName});
    }
}
