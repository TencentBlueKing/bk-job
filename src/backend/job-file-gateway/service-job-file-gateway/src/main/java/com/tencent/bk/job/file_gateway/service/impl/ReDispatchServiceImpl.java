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
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.DispatchService;
import com.tencent.bk.job.file_gateway.service.FileSourceService;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.FileTaskService;
import com.tencent.bk.job.file_gateway.service.FileWorkerService;
import com.tencent.bk.job.file_gateway.service.ReDispatchService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReDispatchServiceImpl implements ReDispatchService {

    private final DispatchService dispatchService;
    private final FileWorkerService fileWorkerService;
    private final FileSourceService fileSourceService;
    private final FileSourceTaskService fileSourceTaskService;
    private final FileTaskService fileTaskService;
    // 最多使用50线程进行重调度
    private final int MAX_THREAD_NUM_REDISPATCH = 50;
    private final AtomicInteger reDispatchThreadNum = new AtomicInteger(0);
    private final Set<String> reDispatchingTaskIds = new HashSet<>();

    @Autowired
    public ReDispatchServiceImpl(
        DispatchService dispatchService,
        FileWorkerService fileWorkerService,
        FileSourceService fileSourceService,
        FileSourceTaskService fileSourceTaskService,
        FileTaskService fileTaskService
    ) {
        this.dispatchService = dispatchService;
        this.fileWorkerService = fileWorkerService;
        this.fileSourceService = fileSourceService;
        this.fileSourceTaskService = fileSourceTaskService;
        this.fileTaskService = fileTaskService;
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
            if (reDispatchThreadNum.get() >= MAX_THREAD_NUM_REDISPATCH) {
                log.warn("reDispatch thread reach MAX_NUM:{}, do not reDispatch {}", MAX_THREAD_NUM_REDISPATCH, taskId);
            } else {
                Timer timer = new Timer();
                timer.schedule(new ReDispatchTask(taskId, intervalMills), initDelayMills);
            }
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
        if (reDispatchThreadNum.get() >= MAX_THREAD_NUM_REDISPATCH) {
            log.warn("reDispatch thread reach MAX_NUM:{}, do not reDispatch {}", MAX_THREAD_NUM_REDISPATCH,
                fileSourceTaskId);
            return false;
        } else {
            Timer timer = new Timer();
            timer.schedule(new ReDispatchTask(fileSourceTaskId, intervalMills), initDelayMills);
            return true;
        }
    }

    @Override
    public Integer getReDispatchThreadsNum(String username) {
        return reDispatchThreadNum.get();
    }

    class ReDispatchTask extends TimerTask {
        private final String fileSourceTaskId;
        private final Long intervalMills;

        ReDispatchTask(String fileSourceTaskId, Long intervalMills) {
            this.fileSourceTaskId = fileSourceTaskId;
            this.intervalMills = intervalMills;
        }

        @Override
        public void run() {
            synchronized (reDispatchingTaskIds) {
                if (reDispatchingTaskIds.contains(fileSourceTaskId)) {
                    log.info("task {} already in reDispatching, ignore", fileSourceTaskId);
                    return;
                }
                reDispatchingTaskIds.add(fileSourceTaskId);
            }
            boolean reDispatchSuccess = false;
            int retryCount = 0;
            try {
                reDispatchThreadNum.incrementAndGet();
                log.debug("taskId={}", fileSourceTaskId);
                FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskService.getFileSourceTaskById(fileSourceTaskId);
                log.debug("fileSourceTaskDTO={}", fileSourceTaskDTO);
                if (fileSourceTaskDTO == null) {
                    log.warn("Cannot find fileSourceTaskDTO by id {}", fileSourceTaskId);
                    return;
                }
                List<FileTaskDTO> fileTaskDTOList = fileTaskService.listFileTasks(fileSourceTaskId);
                log.debug("fileTaskDTOList={}", fileTaskDTOList);
                List<String> filePathList =
                    fileTaskDTOList.parallelStream().map(FileTaskDTO::getFilePath).collect(Collectors.toList());

                FileSourceDTO fileSourceDTO = fileSourceService.getFileSourceById(fileSourceTaskDTO.getFileSourceId());
                while (!reDispatchSuccess && retryCount < 100) {
                    // 1.删除现有子任务
                    log.debug("delete fileTasks of fileSourceTask {}", fileSourceTaskId);
                    fileTaskService.deleteTasks(fileSourceTaskId);
                    // 2.删除现有FileSourceTask任务
                    fileSourceTaskService.deleteFileSourceTaskById(fileSourceTaskId);
                    log.debug("delete fileSourceTask {}", fileSourceTaskId);
                    FileWorkerDTO fileWorkerDTO = dispatchService.findBestFileWorker(fileSourceDTO);
                    log.debug("found bestWorker:{}", fileSourceDTO);
                    if (fileWorkerDTO != null) {
                        // 3.重新派发任务
                        try {
                            TaskInfoDTO taskInfoDTO =
                                fileSourceTaskService.startFileSourceDownloadTaskWithId(
                                    fileSourceTaskDTO.getCreator(),
                                    fileSourceTaskDTO.getAppId(),
                                    fileSourceTaskDTO.getStepInstanceId(),
                                    fileSourceTaskDTO.getExecuteCount(),
                                    fileSourceTaskDTO.getBatchTaskId(),
                                    fileSourceTaskDTO.getFileSourceId(),
                                    filePathList,
                                    fileSourceTaskId
                                );
                            reDispatchSuccess = true;
                            log.info("reDispatch result of {}:{}", fileSourceTaskId, taskInfoDTO);
                        } catch (Exception e) {
                            retryCount += 1;
                            log.info("Fail to redispatch task {}, wait {}ms to retry {}", fileSourceTaskId,
                                intervalMills, retryCount);
                            try {
                                Thread.sleep(intervalMills);
                            } catch (InterruptedException interruptedException) {
                                log.error("redispatch wait interrupted", e);
                            }
                        }
                    } else {
                        // 3.暂时没有合适的FileWorker，延时等待
                        try {
                            retryCount += 1;
                            log.info("No suitable worker to redispatch task {}, wait {}ms to retry {}",
                                fileSourceTaskId, intervalMills, retryCount);
                            Thread.sleep(intervalMills);
                        } catch (InterruptedException e) {
                            log.error("redispatch wait interrupted", e);
                        }
                    }
                }
            } catch (Throwable t) {
                log.error("ReDispatchTask fail", t);
            } finally {
                synchronized (reDispatchingTaskIds) {
                    reDispatchingTaskIds.remove(fileSourceTaskId);
                }
                reDispatchThreadNum.decrementAndGet();
            }
        }
    }
}
