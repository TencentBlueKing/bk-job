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

import com.tencent.bk.job.common.esb.model.job.v3.EsbAccountV3BasicDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbFileSourceV3DTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileInfoDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskFileSourceInfoVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 3/10/2019 17:04
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskFileInfoDTO {
    private Long id;

    private Long stepId;

    private TaskFileTypeEnum fileType;

    private List<String> fileLocation;

    private String fileHash;

    private Long fileSize;

    private TaskTargetDTO host;

    private Long hostAccount;

    private Integer fileSourceId;

    public static TaskFileSourceInfoVO toVO(TaskFileInfoDTO fileInfo) {
        if (fileInfo == null) {
            return null;
        }
        TaskFileSourceInfoVO fileInfoVO = new TaskFileSourceInfoVO();
        fileInfoVO.setId(fileInfo.getId());
        fileInfoVO.setFileType(fileInfo.getFileType().getType());
        fileInfoVO.setFileLocation(fileInfo.getFileLocation());
        fileInfoVO.setFileHash(fileInfo.getFileHash());
        fileInfoVO.setHost(TaskTargetDTO.toVO(fileInfo.getHost()));
        fileInfoVO.setAccount(fileInfo.getHostAccount());
        if (fileInfo.getFileSize() != null) {
            fileInfoVO.setFileSize(String.valueOf(fileInfo.getFileSize()));
        } else {
            fileInfoVO.setFileSize(null);
        }
        fileInfoVO.setFileSourceId(fileInfo.getFileSourceId());
        return fileInfoVO;
    }

    public static TaskFileInfoDTO fromVO(Long stepId, TaskFileSourceInfoVO fileInfoVO) {
        if (fileInfoVO == null) {
            return null;
        }
        TaskFileInfoDTO fileInfo = new TaskFileInfoDTO();
        fileInfo.setId(fileInfoVO.getId());
        fileInfo.setStepId(stepId);
        fileInfo.setFileType(TaskFileTypeEnum.valueOf(fileInfoVO.getFileType()));
        if (CollectionUtils.isNotEmpty(fileInfoVO.getFileLocation())) {
            fileInfo.setFileLocation(fileInfoVO.getFileLocation());
        }
        fileInfo.setFileHash(fileInfoVO.getFileHash());
        fileInfo.setHost(TaskTargetDTO.fromVO(fileInfoVO.getHost()));
        fileInfo.setHostAccount(fileInfoVO.getAccount());
        if (StringUtils.isNotBlank(fileInfoVO.getFileSize())) {
            fileInfo.setFileSize(Long.valueOf(fileInfoVO.getFileSize()));
        } else {
            fileInfo.setFileSize(null);
        }
        fileInfo.setFileSourceId(fileInfoVO.getFileSourceId());
        return fileInfo;
    }

    public static EsbFileSourceV3DTO toEsbFileSourceV3(TaskFileInfoDTO taskFileInfo) {
        if (taskFileInfo == null) {
            return null;
        }
        EsbFileSourceV3DTO esbFileSource = new EsbFileSourceV3DTO();
        esbFileSource.setFiles(taskFileInfo.getFileLocation());
        EsbAccountV3BasicDTO account = new EsbAccountV3BasicDTO();
        account.setId(taskFileInfo.getHostAccount());
        esbFileSource.setAccount(account);
        esbFileSource.setServer(TaskTargetDTO.toEsbServerV3(taskFileInfo.getHost()));
        return esbFileSource;
    }

    public static ServiceTaskFileInfoDTO toServiceFileInfo(TaskFileInfoDTO taskFileInfo) {
        if (taskFileInfo == null) {
            return null;
        }
        ServiceTaskFileInfoDTO serviceFileInfo = new ServiceTaskFileInfoDTO();
        serviceFileInfo.setId(taskFileInfo.getId());
        serviceFileInfo.setFileType(taskFileInfo.getFileType().getType());
        serviceFileInfo.setFileLocation(taskFileInfo.getFileLocation());
        serviceFileInfo.setFileHash(taskFileInfo.getFileHash());
        serviceFileInfo.setFileSize(taskFileInfo.getFileSize());
        serviceFileInfo.setExecuteTarget(taskFileInfo.getHost().toServiceTaskTargetDTO());
        serviceFileInfo.setAccount(new ServiceAccountDTO());
        serviceFileInfo.getAccount().setId(taskFileInfo.getHostAccount());
        return serviceFileInfo;
    }
}
