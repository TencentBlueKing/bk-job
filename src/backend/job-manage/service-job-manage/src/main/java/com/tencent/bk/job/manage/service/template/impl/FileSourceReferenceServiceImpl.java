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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
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
import com.tencent.bk.job.manage.service.template.FileSourceReferenceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class FileSourceReferenceServiceImpl implements FileSourceReferenceService {

    private final ServiceFileSourceResource fileSourceResource;
    private final FileSourceAuthService fileSourceAuthService;
    private final AbstractTaskStepService taskStepService;

    @Autowired
    public FileSourceReferenceServiceImpl(
        ServiceFileSourceResource fileSourceResource,
        FileSourceAuthService fileSourceAuthService,
        @Qualifier("TaskTemplateStepServiceImpl") AbstractTaskStepService taskStepService) {
        this.fileSourceResource = fileSourceResource;
        this.fileSourceAuthService = fileSourceAuthService;
        this.taskStepService = taskStepService;
    }

    /**
     * 校验第三方文件源引用情况，校验业务内可用且用户有view权限
     *
     * @param username         操作者用户名
     * @param taskTemplateInfo 待保存的作业模板，须已带上步骤信息
     * @param create           是否为新建。更新时会放行原模板已引用的已禁用文件源
     */
    @Override
    public void validateReferencedFileSources(String username, TaskTemplateInfoDTO taskTemplateInfo, boolean create) {
        Set<Integer> referencedIds = extractFileSourceIds(taskTemplateInfo.getStepList());
        if (referencedIds.isEmpty()) {
            return;
        }
        Long appId = taskTemplateInfo.getAppId();
        // 查询文件源列表，包含该业务下可不可用
        List<ServiceFileSourceAvailabilityDTO> availabilityList =
            queryAvailability(appId, new ArrayList<>(referencedIds));
        // 兼容设计：更新时放行原模板已引用的已禁用文件源
        Set<Integer> grandfatheredIds = create
            ? Collections.emptySet()
            : taskStepService.listFileSourceIdsByTemplateId(taskTemplateInfo.getId());
        availabilityList.forEach(availability -> denyIfUnavailable(appId, availability, grandfatheredIds));
        authViewFileSource(username, appId, availabilityList);
    }

    /**
     * 抽取文件步骤中以第三方文件源为源文件的文件源 ID，已去重
     */
    private Set<Integer> extractFileSourceIds(List<TaskStepDTO> stepList) {
        if (CollectionUtils.isEmpty(stepList)) {
            return Collections.emptySet();
        }
        Set<Integer> fileSourceIdSet = new LinkedHashSet<>();
        for (TaskStepDTO step : stepList) {
            if (step.getType() != TaskStepTypeEnum.FILE) {
                continue;
            }
            TaskFileStepDTO fileStepInfo = step.getFileStepInfo();
            if (fileStepInfo == null || CollectionUtils.isEmpty(fileStepInfo.getOriginFileList())) {
                continue;
            }
            for (TaskFileInfoDTO fileInfo : fileStepInfo.getOriginFileList()) {
                if (TaskFileTypeEnum.FILE_SOURCE == fileInfo.getFileType() && fileInfo.getFileSourceId() != null) {
                    fileSourceIdSet.add(fileInfo.getFileSourceId());
                }
            }
        }
        return fileSourceIdSet;
    }

    private List<ServiceFileSourceAvailabilityDTO> queryAvailability(Long appId,
                                                                     List<Integer> fileSourceIdList) {
        InternalResponse<List<ServiceFileSourceAvailabilityDTO>> resp =
            fileSourceResource.checkFileSourceAvailability(appId, fileSourceIdList);
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
     * <p>
     * 已禁用的引用只在原模板已有时放行：与引用过期脚本版本的口径一致，让存量模板还能被编辑保存，
     * 但本次新引入的禁用文件源一律拒绝。归属/共享不合法的引用即使原模板已有也不放行。
     */
    private void denyIfUnavailable(Long appId,
                                   ServiceFileSourceAvailabilityDTO availability,
                                   Set<Integer> grandfatheredIds) {
        if (availability.isAvailable()) {
            return;
        }
        Integer fileSourceId = availability.getId();
        String fileSourceName = availability.getDisplayName();
        if (!availability.isInAppScope()) {
            log.warn("File source is not available for app, appId={}, fileSource={}, exists={}, ownerAppId={}",
                appId, fileSourceName, availability.exists(), availability.getOwnerAppId());
            throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_ID_NOT_IN_BIZ,
                new Object[]{String.valueOf(fileSourceId)});
        }
        if (grandfatheredIds.contains(fileSourceId)) {
            log.info("File source is disabled but already referenced by template, pass. appId={}, fileSource={}",
                appId, fileSourceName);
            return;
        }
        log.warn("File source is disabled, appId={}, fileSource={}, ownerAppId={}",
            appId, fileSourceName, availability.getOwnerAppId());
        throw new FailedPreconditionException(ErrorCode.FILE_SOURCE_DISABLED,
            new Object[]{fileSourceName});
    }

    /**
     * 只对归属本业务的文件源鉴权 view_file_source。其他业务共享过来的文件源，共享动作本身即代表授权。
     */
    private void authViewFileSource(String username,
                                    Long appId,
                                    List<ServiceFileSourceAvailabilityDTO> availabilityList) {
        Map<Integer, String> ownFileSourceIdToName = new HashMap<>();
        for (ServiceFileSourceAvailabilityDTO availability : availabilityList) {
            if (appId.equals(availability.getOwnerAppId())) {
                ownFileSourceIdToName.put(availability.getId(), availability.getAlias());
            }
        }
        if (ownFileSourceIdToName.isEmpty()) {
            return;
        }
        fileSourceAuthService.batchAuthViewFileSource(username, new AppResourceScope(appId), ownFileSourceIdToName)
            .denyIfNoPermission();
    }
}
