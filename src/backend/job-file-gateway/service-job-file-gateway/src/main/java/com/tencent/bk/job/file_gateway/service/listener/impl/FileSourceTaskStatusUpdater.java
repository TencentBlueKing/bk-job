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

package com.tencent.bk.job.file_gateway.service.listener.impl;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.context.TaskContext;
import com.tencent.bk.job.file_gateway.service.context.impl.DefaultTaskContext;
import com.tencent.bk.job.file_gateway.service.listener.FileSourceTaskStatusChangeListener;
import com.tencent.bk.job.file_gateway.service.listener.FileTaskStatusChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileSourceTaskStatusUpdater implements FileTaskStatusChangeListener {

    private final DSLContext dslContext;
    private final FileSourceTaskDAO fileSourceTaskDAO;
    private final FileTaskDAO fileTaskDAO;
    private final List<FileSourceTaskStatusChangeListener> fileSourceTaskStatusChangeListenerList = new ArrayList<>();

    @Autowired
    public FileSourceTaskStatusUpdater(DSLContext dslContext, FileSourceTaskDAO fileSourceTaskDAO,
                                       FileTaskDAO fileTaskDAO, FileSourceTaskStatusChangeListener listener) {
        this.dslContext = dslContext;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
        this.fileTaskDAO = fileTaskDAO;
        this.addFileTaskStatusChangeListener(listener);
    }

    public void addFileTaskStatusChangeListener(FileSourceTaskStatusChangeListener listener) {
        this.fileSourceTaskStatusChangeListenerList.add(listener);
    }

    private void notifyFileTaskStatusChangeListeners(FileTaskDTO fileTaskDTO, FileSourceTaskDTO fileSourceTaskDTO,
                                                     FileWorkerDTO fileWorkerDTO, TaskStatusEnum previousStatus,
                                                     TaskStatusEnum currentStatus) {
        TaskContext context = new DefaultTaskContext(fileTaskDTO, fileSourceTaskDTO, fileWorkerDTO);
        if (!fileSourceTaskStatusChangeListenerList.isEmpty()) {
            boolean stop;
            for (FileSourceTaskStatusChangeListener listener : fileSourceTaskStatusChangeListenerList) {
                stop = listener.onStatusChange(context, previousStatus, currentStatus);
                if (stop) break;
            }
        }
    }

    @Override
    public boolean onStatusChange(TaskContext context, TaskStatusEnum previousStatus, TaskStatusEnum currentStatus) {
        FileSourceTaskDTO fileSourceTaskDTO = context.getFileSourceTaskDTO();
        FileTaskDTO fileTaskDTO = context.getFileTaskDTO();
        log.debug("Event:FileTask:{}:{}->{}", fileTaskDTO.getId(), previousStatus, currentStatus);
        String fileSourceTaskId = fileSourceTaskDTO.getId();
        TaskStatusEnum previousFileSourceTaskStatus = TaskStatusEnum.valueOf(fileSourceTaskDTO.getStatus());
        if (TaskStatusEnum.RUNNING.equals(currentStatus)) {
            if (!fileSourceTaskDTO.isDone()) {
                // 主任务RUNNING
                fileSourceTaskDTO.setStatus(TaskStatusEnum.RUNNING.getStatus());
                fileSourceTaskDAO.updateFileSourceTask(dslContext, fileSourceTaskDTO);
                logUpdatedFileSourceTaskStatus(fileSourceTaskId, TaskStatusEnum.RUNNING);
            } else {
                log.info("fileSourceTask {} already done, do not update to running", fileSourceTaskId);
            }
        } else if (TaskStatusEnum.SUCCESS.equals(currentStatus)) {
            //检查主任务是否成功
            if (fileTaskDAO.countFileTask(fileSourceTaskId, null).equals(fileTaskDAO.countFileTask(fileSourceTaskId,
                TaskStatusEnum.SUCCESS.getStatus()))) {
                // 主任务成功
                fileSourceTaskDTO.setStatus(TaskStatusEnum.SUCCESS.getStatus());
                fileSourceTaskDAO.updateFileSourceTask(dslContext, fileSourceTaskDTO);
                logUpdatedFileSourceTaskStatus(fileSourceTaskId, TaskStatusEnum.SUCCESS);
            } else {
                // 主任务尚未成功
                log.info("{} of task {} done, main task not finished yet", fileTaskDTO.getFilePath(), fileSourceTaskId);
            }
        } else if (TaskStatusEnum.FAILED.equals(currentStatus)) {
            // 主任务失败
            fileSourceTaskDTO.setStatus(TaskStatusEnum.FAILED.getStatus());
            fileSourceTaskDAO.updateFileSourceTask(dslContext, fileSourceTaskDTO);
            logUpdatedFileSourceTaskStatus(fileSourceTaskId, TaskStatusEnum.FAILED);
        } else if (TaskStatusEnum.STOPPED.equals(currentStatus)) {
            // 主任务停止
            fileSourceTaskDTO.setStatus(TaskStatusEnum.STOPPED.getStatus());
            fileSourceTaskDAO.updateFileSourceTask(dslContext, fileSourceTaskDTO);
            logUpdatedFileSourceTaskStatus(fileSourceTaskId, TaskStatusEnum.STOPPED);
        }
        // 通知其他关注事件的Listener
        TaskStatusEnum currentFileSourceTaskStatus = TaskStatusEnum.valueOf(fileSourceTaskDTO.getStatus());
        if (previousFileSourceTaskStatus != currentFileSourceTaskStatus) {
            notifyFileTaskStatusChangeListeners(fileTaskDTO, fileSourceTaskDTO, context.getFileWorkerDTO(),
                previousFileSourceTaskStatus, currentFileSourceTaskStatus);
        }
        return false;
    }

    private void logUpdatedFileSourceTaskStatus(String fileSourceTaskId, TaskStatusEnum status) {
        log.info("updated fileSourceTask:{},{}", fileSourceTaskId, status.name());
    }
}
