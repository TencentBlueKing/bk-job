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
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceBatchTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBatchTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.service.context.TaskContext;
import com.tencent.bk.job.file_gateway.service.listener.FileSourceTaskStatusChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BatchTaskStatusUpdater implements FileSourceTaskStatusChangeListener {

    private final DSLContext dslContext;
    private final FileSourceTaskDAO fileSourceTaskDAO;
    private final FileSourceBatchTaskDAO fileSourceBatchTaskDAO;

    @Autowired
    public BatchTaskStatusUpdater(DSLContext dslContext, FileSourceTaskDAO fileSourceTaskDAO,
                                  FileSourceBatchTaskDAO fileSourceBatchTaskDAO) {
        this.dslContext = dslContext;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
        this.fileSourceBatchTaskDAO = fileSourceBatchTaskDAO;
    }

    @Override
    public boolean onStatusChange(TaskContext context, TaskStatusEnum previousStatus, TaskStatusEnum currentStatus) {
        FileSourceTaskDTO fileSourceTaskDTO = context.getFileSourceTaskDTO();
        String batchTaskId = fileSourceTaskDTO.getBatchTaskId();
        FileSourceBatchTaskDTO fileSourceBatchTaskDTO = fileSourceBatchTaskDAO.getFileSourceBatchTaskById(dslContext,
            batchTaskId);
        if (StringUtils.isBlank(batchTaskId)) {
            return false;
        }
        // 若批量任务已处于终止态则不作响应
        if (TaskStatusEnum.SUCCESS.getStatus().equals(fileSourceBatchTaskDTO.getStatus())) {
            log.info("batchTask {} already done, ignore event:{}->{}", batchTaskId, previousStatus, currentStatus);
            return false;
        }
        if (TaskStatusEnum.SUCCESS.equals(currentStatus)) {
            //检查批量任务是否成功
            if (fileSourceTaskDAO.countFileSourceTasksByBatchTaskId(
                dslContext, batchTaskId, null)
                .equals(fileSourceTaskDAO.countFileSourceTasksByBatchTaskId(
                    dslContext, batchTaskId, TaskStatusEnum.SUCCESS.getStatus()))) {
                // 批量任务成功
                fileSourceBatchTaskDTO.setStatus(TaskStatusEnum.SUCCESS.getStatus());
                fileSourceBatchTaskDAO.updateFileSourceBatchTask(dslContext, fileSourceBatchTaskDTO);
            } else {
                // 主任务尚未成功
                log.info("task {} done, batchTask not finished yet", fileSourceTaskDTO.getId());
            }
        } else if (TaskStatusEnum.FAILED.equals(currentStatus)) {
            // 批量任务失败
            fileSourceBatchTaskDTO.setStatus(TaskStatusEnum.FAILED.getStatus());
            fileSourceBatchTaskDAO.updateFileSourceBatchTask(dslContext, fileSourceBatchTaskDTO);
        } else if (TaskStatusEnum.STOPPED.equals(currentStatus)) {
            // 批量任务停止
            fileSourceBatchTaskDTO.setStatus(TaskStatusEnum.STOPPED.getStatus());
            fileSourceBatchTaskDAO.updateFileSourceBatchTask(dslContext, fileSourceBatchTaskDTO);
        }
        return false;
    }
}
