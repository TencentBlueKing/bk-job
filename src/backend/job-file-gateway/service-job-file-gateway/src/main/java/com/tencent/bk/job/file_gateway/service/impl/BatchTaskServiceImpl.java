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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceBatchTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBatchTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceTaskContent;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.BatchTaskService;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatchTaskServiceImpl implements BatchTaskService {

    private final FileSourceTaskService fileSourceTaskService;
    private final FileSourceBatchTaskDAO fileSourceBatchTaskDAO;
    private final DSLContext dslContext;
    private final FileSourceTaskDAO fileSourceTaskDAO;

    @Autowired
    public BatchTaskServiceImpl(FileSourceTaskService fileSourceTaskService,
                                FileSourceBatchTaskDAO fileSourceBatchTaskDAO, DSLContext dslContext,
                                FileSourceTaskDAO fileSourceTaskDAO) {
        this.fileSourceTaskService = fileSourceTaskService;
        this.fileSourceBatchTaskDAO = fileSourceBatchTaskDAO;
        this.dslContext = dslContext;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
    }

    @Override
    public BatchTaskInfoDTO startFileSourceBatchDownloadTask(String username, Long appId, Long stepInstanceId,
                                                             Integer executeCount,
                                                             List<FileSourceTaskContent> fileSourceTaskList) {
        BatchTaskInfoDTO batchTaskInfoDTO = new BatchTaskInfoDTO();
        FileSourceBatchTaskDTO fileSourceBatchTaskDTO = new FileSourceBatchTaskDTO();
        fileSourceBatchTaskDTO.setAppId(appId);
        fileSourceBatchTaskDTO.setCreator(username);
        fileSourceBatchTaskDTO.setCreateTime(System.currentTimeMillis());
        fileSourceBatchTaskDTO.setStepInstanceId(stepInstanceId);
        fileSourceBatchTaskDTO.setExecuteCount(executeCount);
        fileSourceBatchTaskDTO.setStatus(TaskStatusEnum.INIT.getStatus());
        String batchTaskId = fileSourceBatchTaskDAO.insertFileSourceBatchTask(dslContext, fileSourceBatchTaskDTO);
        batchTaskInfoDTO.setBatchTaskId(batchTaskId);
        List<TaskInfoDTO> taskInfoDTOList = new ArrayList<>();
        for (FileSourceTaskContent fileSourceTaskContent : fileSourceTaskList) {
            TaskInfoDTO taskInfoDTO = fileSourceTaskService.startFileSourceDownloadTask(username, appId,
                stepInstanceId, executeCount, batchTaskId, fileSourceTaskContent.getFileSourceId(),
                fileSourceTaskContent.getFilePathList());
            taskInfoDTOList.add(taskInfoDTO);
        }
        batchTaskInfoDTO.setTaskInfoList(taskInfoDTOList);
        return batchTaskInfoDTO;
    }

    public List<String> getFileSourceTaskIdListByBatch(List<String> batchTaskIdList) {
        List<String> fileSourceTaskIdList = new ArrayList<>();
        for (String batchTaskId : batchTaskIdList) {
            List<FileSourceTaskDTO> fileSourceTaskDTOList = fileSourceTaskDAO.listByBatchTaskId(batchTaskId);
            fileSourceTaskIdList.addAll(fileSourceTaskDTOList.parallelStream()
                .map(FileSourceTaskDTO::getId).collect(Collectors.toList()));
        }
        return fileSourceTaskIdList;
    }

    @Override
    public Integer stopBatchTasks(List<String> batchTaskIdList) {
        List<String> fileSourceTaskIdList = getFileSourceTaskIdListByBatch(batchTaskIdList);
        return fileSourceTaskService.stopTasks(fileSourceTaskIdList);
    }

    @Override
    public BatchTaskStatusDTO getBatchTaskStatusAndLogs(String batchTaskId, Long logStart, Long logLength) {
        BatchTaskStatusDTO batchTaskStatusDTO = new BatchTaskStatusDTO();
        batchTaskStatusDTO.setBatchTaskId(batchTaskId);
        FileSourceBatchTaskDTO fileSourceBatchTaskDTO = fileSourceBatchTaskDAO.getFileSourceBatchTaskById(dslContext,
            batchTaskId);
        if (fileSourceBatchTaskDTO == null) {
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        batchTaskStatusDTO.setStatus(fileSourceBatchTaskDTO.getStatus());
        List<FileSourceTaskStatusDTO> fileSourceTaskStatusInfoList = new ArrayList<>();
        List<FileSourceTaskDTO> fileSourceTaskDTOList = fileSourceTaskDAO.listByBatchTaskId(batchTaskId);
        for (FileSourceTaskDTO fileSourceTaskDTO : fileSourceTaskDTOList) {
            FileSourceTaskStatusDTO fileSourceTaskStatusDTO =
                fileSourceTaskService.getFileSourceTaskStatusAndLogs(fileSourceTaskDTO.getId(), logStart, logLength);
            fileSourceTaskStatusInfoList.add(fileSourceTaskStatusDTO);
        }
        batchTaskStatusDTO.setFileSourceTaskStatusInfoList(fileSourceTaskStatusInfoList);
        return batchTaskStatusDTO;
    }

    @Override
    public Integer clearBatchTaskFiles(List<String> batchTaskIdList) {
        List<String> fileSourceTaskIdList = getFileSourceTaskIdListByBatch(batchTaskIdList);
        return fileSourceTaskService.clearTaskFiles(fileSourceTaskIdList);
    }
}
