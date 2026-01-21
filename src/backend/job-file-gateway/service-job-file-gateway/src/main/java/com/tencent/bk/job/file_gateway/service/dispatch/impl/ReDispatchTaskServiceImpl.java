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

package com.tencent.bk.job.file_gateway.service.dispatch.impl;

import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.file_gateway.metrics.MetricsConstants;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.FileTaskService;
import com.tencent.bk.job.file_gateway.service.dispatch.ReDispatchTaskService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReDispatchTaskServiceImpl implements ReDispatchTaskService {

    private final FileSourceTaskService fileSourceTaskService;
    private final FileTaskService fileTaskService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public ReDispatchTaskServiceImpl(FileSourceTaskService fileSourceTaskService,
                                     FileTaskService fileTaskService,
                                     MeterRegistry meterRegistry) {
        this.fileSourceTaskService = fileSourceTaskService;
        this.fileTaskService = fileTaskService;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 对文件源任务进行重调度，过程中开启事务保证数据一致性
     *
     * @param fileSourceTaskId 文件源任务ID
     * @return 重调度结果
     */
    @Override
    @JobTransactional(transactionManager = "jobFileGatewayTransactionManager")
    public TaskInfoDTO reDispatchFileSourceTask(String fileSourceTaskId) {
        long startTime = System.currentTimeMillis();
        FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskService.getFileSourceTaskById(fileSourceTaskId);
        String reDispatchStatus = null;
        try {
            TaskInfoDTO taskInfoDTO = doReDispatchFileSourceTask(fileSourceTaskDTO);
            reDispatchStatus = MetricsConstants.TAG_VALUE_REDISPATCH_STATUS_SUCCESS;
            return taskInfoDTO;
        } catch (Exception e) {
            reDispatchStatus = MetricsConstants.TAG_VALUE_REDISPATCH_STATUS_ERROR;
            throw e;
        } finally {
            long timeConsumingMills = System.currentTimeMillis() - startTime;
            recordReDispatchCost(timeConsumingMills, buildDispatchTags(fileSourceTaskDTO.getAppId(), reDispatchStatus));
        }
    }

    private Iterable<Tag> buildDispatchTags(Long appId, String reDispatchStatus) {
        List<Tag> tagList = new ArrayList<>();
        tagList.add(Tag.of(MetricsConstants.TAG_KEY_MODULE, MetricsConstants.TAG_VALUE_MODULE_FILE_GATEWAY));
        tagList.add(Tag.of(MetricsConstants.TAG_KEY_APP_ID, String.valueOf(appId)));
        tagList.add(Tag.of(MetricsConstants.TAG_KEY_DISPATCH_RESULT, reDispatchStatus));
        return tagList;
    }

    private TaskInfoDTO doReDispatchFileSourceTask(FileSourceTaskDTO fileSourceTaskDTO) {
        String fileSourceTaskId = fileSourceTaskDTO.getId();
        Long oldFileWorkerId = fileSourceTaskDTO.getFileWorkerId();
        List<FileTaskDTO> fileTaskDTOList = fileTaskService.listFileTasks(fileSourceTaskId);
        List<String> filePathList =
            fileTaskDTOList.stream().map(FileTaskDTO::getFilePath).collect(Collectors.toList());
        // 1.删除现有子任务
        int deletedTaskNum = fileTaskService.deleteTasks(fileSourceTaskId);
        // 2.删除现有FileSourceTask任务
        int deletedFileSourceTaskNum = fileSourceTaskService.deleteFileSourceTaskById(fileSourceTaskId);
        // 3.重新派发任务
        TaskInfoDTO taskInfoDTO = fileSourceTaskService.startFileSourceDownloadTaskWithId(
            fileSourceTaskDTO.getCreator(),
            fileSourceTaskDTO.getAppId(),
            fileSourceTaskDTO.getStepInstanceId(),
            fileSourceTaskDTO.getExecuteCount(),
            fileSourceTaskDTO.getBatchTaskId(),
            fileSourceTaskDTO.getFileSourceId(),
            filePathList,
            fileSourceTaskId
        );
        log.info(
            "FileSourceTask(id={}, oldFileWorkerId={}) reDispatched to worker(id={},accessHost={})," +
                " [ {} fileTask, {} fileSourceTask] deleted and re-inserted",
            fileSourceTaskId,
            oldFileWorkerId,
            taskInfoDTO.getWorkerId(),
            taskInfoDTO.getWorkerAccessHost(),
            deletedTaskNum,
            deletedFileSourceTaskNum
        );
        return taskInfoDTO;
    }

    private void recordReDispatchCost(long timeConsumingMillis, Iterable<Tag> tags) {
        Timer.builder(MetricsConstants.NAME_FILE_GATEWAY_REDISPATCH_TIME)
            .description("ReDispatch FileSourceTask Cost")
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(10))
            .maximumExpectedValue(Duration.ofSeconds(60L))
            .register(meterRegistry)
            .record(timeConsumingMillis, TimeUnit.MILLISECONDS);
    }
}
