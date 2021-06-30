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

package com.tencent.bk.job.manage.model.dto.converter;

import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileStepDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TaskFileStepConverter {
    public static ServiceTaskFileStepDTO convertToServiceTaskFileStepDTO(TaskFileStepDTO taskFileStepDTO) {
        if (taskFileStepDTO == null) {
            return null;
        }
        ServiceTaskFileStepDTO serviceTaskFileStepDTO = new ServiceTaskFileStepDTO();
        serviceTaskFileStepDTO.setNotExistPathHandler(taskFileStepDTO.getNotExistPathHandler().getValue());
        serviceTaskFileStepDTO.setFileDuplicateHandle(taskFileStepDTO.getDuplicateHandler().getId());
        serviceTaskFileStepDTO.setDestinationFileLocation(taskFileStepDTO.getDestinationFileLocation());
        serviceTaskFileStepDTO.setDownloadSpeedLimit(taskFileStepDTO.getTargetSpeedLimit().intValue());
        serviceTaskFileStepDTO.setUploadSpeedLimit(taskFileStepDTO.getOriginSpeedLimit().intValue());
        serviceTaskFileStepDTO.setIgnoreError(taskFileStepDTO.getIgnoreError());
        serviceTaskFileStepDTO.setTimeout(taskFileStepDTO.getTimeout().intValue());
        serviceTaskFileStepDTO.setExecuteTarget(
            TaskTargetConverter.convertToServiceTaskTargetDTO(
                taskFileStepDTO.getDestinationHostList()
            )
        );
        List<TaskFileInfoDTO> originFileList = taskFileStepDTO.getOriginFileList();
        List<ServiceTaskFileInfoDTO> serviceTaskFileInfoDTOList = new ArrayList<>();
        if (originFileList != null && !originFileList.isEmpty()) {
            serviceTaskFileInfoDTOList =
                originFileList.parallelStream()
                    .map(TaskFileInfoConverter::convertToServiceTaskFileInfoDTO).collect(Collectors.toList());
        }
        serviceTaskFileStepDTO.setOriginFileList(serviceTaskFileInfoDTOList);
        return serviceTaskFileStepDTO;
    }
}
