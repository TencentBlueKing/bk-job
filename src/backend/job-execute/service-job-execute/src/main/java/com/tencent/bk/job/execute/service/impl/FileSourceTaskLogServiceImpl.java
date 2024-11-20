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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.dao.common.IdGen;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.service.FileSourceTaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FileSourceTaskLogServiceImpl implements FileSourceTaskLogService {

    private final FileSourceTaskLogDAO fileSourceTaskLogDAO;

    private final IdGen idGen;

    public FileSourceTaskLogServiceImpl(FileSourceTaskLogDAO fileSourceTaskLogDAO, IdGen idGen) {
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
        this.idGen = idGen;
    }

    @Override
    public int addFileSourceTaskLog(FileSourceTaskLogDTO fileSourceTaskLog) {
        fileSourceTaskLog.setId(idGen.genFileSourceTaskLogId());
        return fileSourceTaskLogDAO.insertFileSourceTaskLog(fileSourceTaskLog);
    }

    @Override
    public int updateFileSourceTaskLog(FileSourceTaskLogDTO fileSourceTaskLog) {
        return fileSourceTaskLogDAO.updateFileSourceTaskLogByStepInstance(fileSourceTaskLog);
    }

    @Override
    public FileSourceTaskLogDTO getFileSourceTaskLog(Long taskInstanceId, long stepInstanceId, int executeCount) {
        return fileSourceTaskLogDAO.getFileSourceTaskLog(taskInstanceId, stepInstanceId, executeCount);
    }

    @Override
    public FileSourceTaskLogDTO getFileSourceTaskLogByBatchTaskId(Long taskInstanceId, String fileSourceBatchTaskId) {
        return fileSourceTaskLogDAO.getFileSourceTaskLogByBatchTaskId(taskInstanceId, fileSourceBatchTaskId);
    }

    @Override
    public int updateTimeConsumingByBatchTaskId(Long taskInstanceId,
                                                String fileSourceBatchTaskId,
                                                Long startTime,
                                                Long endTime,
                                                Long totalTime) {
        return fileSourceTaskLogDAO.updateTimeConsumingByBatchTaskId(
            taskInstanceId,
            fileSourceBatchTaskId,
            startTime,
            endTime,
            totalTime
        );
    }
}
