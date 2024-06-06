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

package com.tencent.bk.job.file_gateway.service.dispatch.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import com.tencent.bk.job.file_gateway.service.dispatch.ReDispatchService;
import com.tencent.bk.job.file_gateway.service.dispatch.ReDispatchTaskService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Timer;

@Slf4j
@Service
public class ReDispatchServiceImpl implements ReDispatchService {

    private final FileWorkerService fileWorkerService;
    private final FileSourceTaskService fileSourceTaskService;
    private final ReDispatchTaskService reDispatchTaskService;
    // 最多使用50线程进行重调度
    private final int MAX_THREAD_NUM_REDISPATCH = 50;

    @Autowired
    public ReDispatchServiceImpl(
        FileWorkerService fileWorkerService,
        FileSourceTaskService fileSourceTaskService,
        ReDispatchTaskService reDispatchTaskService
    ) {
        this.fileWorkerService = fileWorkerService;
        this.fileSourceTaskService = fileSourceTaskService;
        this.reDispatchTaskService = reDispatchTaskService;
    }

    @Override
    public List<String> reDispatchByWorker(
        String accessHost,
        Integer accessPort,
        List<String> taskIdList,
        Long initDelayMills,
        Long intervalMills
    ) {
        FileWorkerDTO fileWorkerDTO = fileWorkerService.getFileWorker(accessHost, accessPort);
        if (fileWorkerDTO == null) {
            FormattingTuple msg = MessageFormatter.format(
                "Fail to find file-worker by accessHost:{} accessPort:{}", accessHost, accessPort
            );
            log.warn(msg.getMessage());
            throw new InternalException(
                ErrorCode.FILE_WORKER_NOT_FOUND,
                new String[]{
                    "accessHost:" + accessHost + ",accessPort:" + accessPort,
                }
            );
        }
        Long workerId = fileWorkerDTO.getId();
        log.debug("worker {} apply to reDispatch tasks:{}, initDelayMills={}, intervalMills={}", workerId, taskIdList
            , initDelayMills, intervalMills);
        // 1.立即下线Worker
        int affectedWorkerNum = fileWorkerService.offLine(workerId);
        log.info("{} worker state changed to offline", affectedWorkerNum);
        // 2.任务延时重调度
        for (String taskId : taskIdList) {
            if (getReDispatchThreadsNum() >= MAX_THREAD_NUM_REDISPATCH) {
                log.warn("reDispatch thread reach MAX_NUM:{}, do not reDispatch {}", MAX_THREAD_NUM_REDISPATCH, taskId);
                continue;
            }
            Timer timer = new Timer();
            ReDispatchTask reDispatchTask = buildReDispatchTask(taskId, intervalMills);
            timer.schedule(reDispatchTask, initDelayMills);
        }
        return taskIdList;
    }

    @Override
    public boolean reDispatchByGateway(String fileSourceTaskId, Long initDelayMills, Long intervalMills) {
        // 1.尝试通知Worker主动取消该任务
        FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskService.getFileSourceTaskById(fileSourceTaskId);
        if (fileSourceTaskDTO == null) {
            log.warn("task not exist, ignore, id={}", fileSourceTaskId);
            return false;
        }
        try {
            fileSourceTaskService.recallTasks(Collections.singletonList(fileSourceTaskId));
        } catch (Throwable t) {
            log.warn("Fail to recallTask:{}", fileSourceTaskId, t);
        }
        // 2.重调度
        if (getReDispatchThreadsNum() >= MAX_THREAD_NUM_REDISPATCH) {
            log.warn("reDispatch thread reach MAX_NUM:{}, do not reDispatch {}", MAX_THREAD_NUM_REDISPATCH,
                fileSourceTaskId);
            return false;
        }
        Timer timer = new Timer();
        ReDispatchTask reDispatchTask = buildReDispatchTask(fileSourceTaskId, intervalMills);
        timer.schedule(reDispatchTask, initDelayMills);
        return true;
    }

    private ReDispatchTask buildReDispatchTask(String fileSourceTaskId, Long intervalMills) {
        return new ReDispatchTask(
            reDispatchTaskService,
            fileSourceTaskId,
            intervalMills
        );
    }

    @Override
    public Integer getReDispatchThreadsNum() {
        return ReDispatchTask.getReDispatchThreadsNum();
    }

}
