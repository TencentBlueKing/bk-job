/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileDestinationV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbFileStepV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileStepDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileDestinationInfoVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileStepVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 3/10/2019 17:03
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskFileStepDTO {

    private Long id;

    private Long stepId;

    private List<TaskFileInfoDTO> originFileList;

    private String destinationFileLocation;

    private Long executeAccount;

    private TaskTargetDTO destinationHostList;

    private Long timeout;

    private Long originSpeedLimit;

    private Long targetSpeedLimit;

    private DuplicateHandlerEnum duplicateHandler;

    private NotExistPathHandlerEnum notExistPathHandler;

    private Boolean ignoreError;

    public static TaskFileStepVO toVO(TaskFileStepDTO fileStep) {
        if (fileStep == null) {
            return null;
        }
        TaskFileStepVO fileStepVO = new TaskFileStepVO();
        if (CollectionUtils.isNotEmpty(fileStep.getOriginFileList())) {
            fileStepVO.setFileSourceList(
                fileStep.getOriginFileList().stream().map(TaskFileInfoDTO::toVO).collect(Collectors.toList()));
        }
        TaskFileDestinationInfoVO destinationInfo = new TaskFileDestinationInfoVO();
        destinationInfo.setPath(fileStep.getDestinationFileLocation());
        destinationInfo.setAccount(fileStep.getExecuteAccount());
        destinationInfo.setServer(TaskTargetDTO.toVO(fileStep.getDestinationHostList()));
        fileStepVO.setFileDestination(destinationInfo);
        fileStepVO.setTimeout(fileStep.getTimeout());
        fileStepVO.setOriginSpeedLimit(fileStep.getOriginSpeedLimit());
        fileStepVO.setTargetSpeedLimit(fileStep.getTargetSpeedLimit());
        fileStepVO.setTransferMode(TaskFileStepVO.getTransferMode(fileStep.getDuplicateHandler(),
            fileStep.getNotExistPathHandler()));
        fileStepVO.setIgnoreError(fileStep.getIgnoreError() ? 1 : 0);
        return fileStepVO;
    }

    public static TaskFileStepDTO fromVO(Long stepId, TaskFileStepVO fileStepVO) {
        if (fileStepVO == null) {
            return null;
        }
        TaskFileStepDTO fileStep = new TaskFileStepDTO();
        fileStep.setStepId(stepId);
        if (CollectionUtils.isNotEmpty(fileStepVO.getFileSourceList())) {
            fileStep.setOriginFileList(fileStepVO.getFileSourceList().stream()
                .map(fileInfo -> TaskFileInfoDTO.fromVO(stepId, fileInfo)).collect(Collectors.toList()));
        }
        if (fileStepVO.getFileDestination() != null) {
            fileStep.setDestinationFileLocation(fileStepVO.getFileDestination().getPath());
            fileStep.setExecuteAccount(fileStepVO.getFileDestination().getAccount());
            fileStep.setDestinationHostList(TaskTargetDTO.fromVO(fileStepVO.getFileDestination().getServer()));
        }
        fileStep.setTimeout(fileStepVO.getTimeout());
        fileStep.setOriginSpeedLimit(fileStepVO.getOriginSpeedLimit());
        fileStep.setTargetSpeedLimit(fileStepVO.getTargetSpeedLimit());
        fileStep.setDuplicateHandler(DuplicateHandlerEnum.valueOf(fileStepVO.getDuplicateHandler()));
        fileStep.setNotExistPathHandler(NotExistPathHandlerEnum.valueOf(fileStepVO.getNotExistPathHandler()));
        fileStep.setIgnoreError(fileStepVO.getIgnoreError() == 1);
        return fileStep;
    }

    public static EsbFileStepV3DTO toEsbFileInfoV3(TaskFileStepDTO fileStepInfo) {
        if (fileStepInfo == null) {
            return null;
        }
        EsbFileStepV3DTO esbFileStep = new EsbFileStepV3DTO();
        if (CollectionUtils.isNotEmpty(fileStepInfo.getOriginFileList())) {
            esbFileStep.setFileSourceList(fileStepInfo.getOriginFileList().parallelStream()
                .map(TaskFileInfoDTO::toEsbFileSourceV3).collect(Collectors.toList()));
        }
        EsbFileDestinationV3DTO esbFileDestination = new EsbFileDestinationV3DTO();
        esbFileDestination.setPath(fileStepInfo.getDestinationFileLocation());
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setId(fileStepInfo.getExecuteAccount());
        esbFileDestination.setAccount(account);
        esbFileDestination.setServer(TaskTargetDTO.toEsbServerV3(fileStepInfo.getDestinationHostList()));
        esbFileStep.setFileDestination(esbFileDestination);
        esbFileStep.setTimeout(fileStepInfo.getTimeout());
        esbFileStep.setSourceSpeedLimit(fileStepInfo.getOriginSpeedLimit());
        esbFileStep.setDestinationSpeedLimit(fileStepInfo.getTargetSpeedLimit());
        esbFileStep.setTransferMode(TaskFileStepVO.getTransferMode(fileStepInfo.getDuplicateHandler(),
            fileStepInfo.getNotExistPathHandler()));
        return esbFileStep;
    }

    public static ServiceTaskFileStepDTO toServiceFileInfo(TaskFileStepDTO fileStepInfo) {
        if (fileStepInfo == null) {
            return null;
        }
        ServiceTaskFileStepDTO serviceFileStep = new ServiceTaskFileStepDTO();
        if (CollectionUtils.isNotEmpty(fileStepInfo.getOriginFileList())) {
            serviceFileStep.setOriginFileList(fileStepInfo.getOriginFileList().parallelStream()
                .map(TaskFileInfoDTO::toServiceFileInfo).collect(Collectors.toList()));
        }
        serviceFileStep.setDestinationFileLocation(fileStepInfo.getDestinationFileLocation());
        serviceFileStep.setAccount(new ServiceAccountDTO());
        serviceFileStep.getAccount().setId(fileStepInfo.getExecuteAccount());
        if (fileStepInfo.getDestinationHostList() != null) {
            serviceFileStep.setExecuteTarget(fileStepInfo.getDestinationHostList().toServiceTaskTargetDTO());
        }
        if (fileStepInfo.getOriginSpeedLimit() != null) {
            serviceFileStep.setDownloadSpeedLimit(fileStepInfo.getOriginSpeedLimit().intValue());
        }
        if (fileStepInfo.getTargetSpeedLimit() != null) {
            serviceFileStep.setUploadSpeedLimit(fileStepInfo.getTargetSpeedLimit().intValue());
        }
        if (fileStepInfo.getTimeout() != null) {
            serviceFileStep.setTimeout(fileStepInfo.getTimeout().intValue());
        }
        serviceFileStep.setIgnoreError(fileStepInfo.getIgnoreError());
        serviceFileStep.setFileDuplicateHandle(fileStepInfo.getDuplicateHandler().getId());
        serviceFileStep.setNotExistPathHandler(fileStepInfo.getNotExistPathHandler().getValue());
        return serviceFileStep;
    }
}
