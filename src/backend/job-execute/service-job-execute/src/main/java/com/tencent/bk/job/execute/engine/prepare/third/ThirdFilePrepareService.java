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

package com.tencent.bk.job.execute.engine.prepare.third;

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.client.FileSourceTaskResourceClient;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.ClearTaskFilesReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceBatchDownloadTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceTaskContent;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Primary
@Component
public class ThirdFilePrepareService {
    private final ResultHandleManager resultHandleManager;
    private final FileSourceTaskResourceClient fileSourceTaskResource;
    private final TaskInstanceService taskInstanceService;
    private final FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private final AccountService accountService;
    private final LogService logService;
    private final TaskExecuteControlMsgSender taskControlMsgSender;
    private ThirdFilePrepareTaskResultHandler resultHandler;
    private final Map<Long, ThirdFilePrepareTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public ThirdFilePrepareService(ResultHandleManager resultHandleManager,
                                   FileSourceTaskResourceClient fileSourceTaskResource,
                                   TaskInstanceService taskInstanceService,
                                   FileSourceTaskLogDAO fileSourceTaskLogDAO, AccountService accountService,
                                   LogService logService, TaskExecuteControlMsgSender taskControlMsgSender) {
        this.resultHandleManager = resultHandleManager;
        this.fileSourceTaskResource = fileSourceTaskResource;
        this.taskInstanceService = taskInstanceService;
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
        this.accountService = accountService;
        this.logService = logService;
        this.taskControlMsgSender = taskControlMsgSender;
    }

    private void setTaskInfoIntoThirdFileSource(TaskInfoDTO taskInfoDTO, FileSourceDTO fileSourceDTO) {
        String fileSourceTaskId = taskInfoDTO.getTaskId();
        if (fileSourceDTO.getServers() == null) {
            fileSourceDTO.setServers(new ServersDTO());
        }
        List<IpDTO> ipDTOList = new ArrayList<>();
        ipDTOList.add(new IpDTO(taskInfoDTO.getCloudId(), taskInfoDTO.getIp()));
        fileSourceDTO.getServers().setStaticIpList(ipDTOList);
        fileSourceDTO.getServers().setIpList(ipDTOList);
        fileSourceDTO.setFileSourceTaskId(fileSourceTaskId);
        fileSourceDTO.getFiles().forEach(fileDetailDTO -> {
            // 第二次处理，加上文件源名称的文件路径
            fileDetailDTO.setThirdFilePathWithFileSourceName(PathUtil.joinFilePath(taskInfoDTO.getFileSourceName(),
                fileDetailDTO.getThirdFilePath()));
        });
    }

    /**
     * 将任务信息设置到步骤的文件源信息及统计数据中去
     *
     * @param batchTaskInfoDTO
     * @param thirdFileSourceList
     */
    private void setBatchTaskInfoIntoThirdFileSource(BatchTaskInfoDTO batchTaskInfoDTO,
                                                     List<FileSourceDTO> thirdFileSourceList) {
        List<TaskInfoDTO> taskInfoList = batchTaskInfoDTO.getTaskInfoList();
        for (int i = 0; i < taskInfoList.size(); i++) {
            TaskInfoDTO taskInfoDTO = taskInfoList.get(i);
            FileSourceDTO fileSourceDTO = thirdFileSourceList.get(i);
            Integer fileSourceId = fileSourceDTO.getFileSourceId();
            if (fileSourceId != null && fileSourceId > 0) {
                setTaskInfoIntoThirdFileSource(taskInfoDTO, fileSourceDTO);
            }
        }
    }

    public void retryPrepareFile(Long stepInstanceId) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        int executeType = stepInstance.getExecuteType();
        // 当前仅有文件分发类步骤需要重试第三方文件源拉取
        if (TaskStepTypeEnum.FILE.getValue() != executeType) {
            return;
        }
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        Pair<List<FileSourceDTO>, List<FileSourceTaskContent>> thirdFileSource = parseThirdFileSource(fileSourceList);
        List<FileSourceDTO> thirdFileSourceList = thirdFileSource.getLeft();
        List<FileSourceTaskContent> fileSourceTaskList = thirdFileSource.getRight();
        if (thirdFileSourceList.isEmpty()) {
            return;
        }
        // 直接重新下载
        BatchTaskInfoDTO batchTaskInfoDTO = startFileSourceDownloadTask(stepInstance.getOperator(),
            stepInstance.getAppId(), stepInstance.getId(), stepInstance.getExecuteCount(), fileSourceTaskList);
        setBatchTaskInfoIntoThirdFileSource(batchTaskInfoDTO, thirdFileSourceList);
        log.debug("new batchFileSourceTask: {}", batchTaskInfoDTO.getBatchTaskId());

        // 更新文件源任务状态
        taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
        // 异步处理文件下载任务
        ThirdFilePrepareTask pullingResultTask = asyncWatchThirdFilePulling(stepInstance,
            fileSourceList, batchTaskInfoDTO.getBatchTaskId(), true);
        // 重试先同步处理
        while (!pullingResultTask.isReadyForNext()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Sleep Interrupted", e);
            }
        }
        log.debug("continue to retry...");
    }

    private Pair<List<FileSourceDTO>, List<FileSourceTaskContent>> parseThirdFileSource(
        List<FileSourceDTO> fileSourceList
    ) {
        List<FileSourceDTO> thirdFileSourceList = new ArrayList<>();
        List<FileSourceTaskContent> fileSourceTaskList = new ArrayList<>();
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO == null) {
                log.warn("fileSourceDTO is null");
                continue;
            }
            Integer fileSourceId = fileSourceDTO.getFileSourceId();
            if (fileSourceId != null && fileSourceId > 0) {
                List<FileDetailDTO> files = fileSourceDTO.getFiles();
                if (files == null) {
                    log.warn("files is null");
                    continue;
                }
                for (FileDetailDTO file : files) {
                    if (file != null && StringUtils.isBlank(file.getThirdFilePath())) {
                        // 第一次处理，不含文件源名称的文件路径
                        file.setThirdFilePath(file.getFilePath());
                    }
                }
                // 第三方COS等文件源的文件任务处理
                List<String> filePaths =
                    files.parallelStream().map(FileDetailDTO::getThirdFilePath).collect(Collectors.toList());
                fileSourceTaskList.add(new FileSourceTaskContent(fileSourceId, filePaths));
                thirdFileSourceList.add(fileSourceDTO);
            }
        }
        return Pair.of(thirdFileSourceList, fileSourceTaskList);
    }

    public ThirdFilePrepareTask prepareThirdFileAsync(
        StepInstanceDTO stepInstance,
        ThirdFilePrepareTaskResultHandler resultHandler
    ) {
        this.resultHandler = resultHandler;
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        // 准备第三方源文件
        Pair<List<FileSourceDTO>, List<FileSourceTaskContent>> thirdFileSource = parseThirdFileSource(fileSourceList);
        List<FileSourceDTO> thirdFileSourceList = thirdFileSource.getLeft();
        List<FileSourceTaskContent> fileSourceTaskList = thirdFileSource.getRight();
        if (thirdFileSourceList == null || thirdFileSourceList.isEmpty()) {
            taskControlMsgSender.startGseStep(stepInstance.getId());
            return null;
        }
        log.debug("Start FileSourceBatchTask: {}", fileSourceTaskList);
        BatchTaskInfoDTO batchTaskInfoDTO = startFileSourceDownloadTask(stepInstance.getOperator(),
            stepInstance.getAppId(), stepInstance.getId(), stepInstance.getExecuteCount(), fileSourceTaskList);
        setBatchTaskInfoIntoThirdFileSource(batchTaskInfoDTO, thirdFileSourceList);
        log.debug("fileSourceList={}", fileSourceList);
        log.debug("Started batchTaskId:{}", batchTaskInfoDTO.getBatchTaskId());
        // 放进文件源下载任务进度表中
        FileSourceTaskLogDTO fileSourceTaskLogDTO = new FileSourceTaskLogDTO();
        fileSourceTaskLogDTO.setStepInstanceId(stepInstance.getId());
        fileSourceTaskLogDTO.setExecuteCount(stepInstance.getExecuteCount());
        fileSourceTaskLogDTO.setFileSourceBatchTaskId(batchTaskInfoDTO.getBatchTaskId());
        fileSourceTaskLogDTO.setStatus(TaskStatusEnum.INIT.getStatus().intValue());
        fileSourceTaskLogDTO.setStartTime(System.currentTimeMillis());
        fileSourceTaskLogDAO.saveFileSourceTaskLog(fileSourceTaskLogDTO);
        // 更新文件源任务状态
        taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
        // 异步处理文件下载任务
        ThirdFilePrepareTask task = asyncWatchThirdFilePulling(
            stepInstance,
            fileSourceList,
            batchTaskInfoDTO.getBatchTaskId(),
            false
        );
        taskMap.put(stepInstance.getId(), task);
        return task;
    }

    public void stopPrepareThirdFileAsync(long stepInstanceId) {
        ThirdFilePrepareTask task = taskMap.get(stepInstanceId);
        if (task != null) {
            task.stopThirdFilePulling();
        }
    }

    public void clearPreparedTmpFile(long stepInstanceId) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        // 找出所有第三方文件源的TaskId进行清理
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        List<String> fileSourceTaskIdList = new ArrayList<>();
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            Integer fileSourceId = fileSourceDTO.getFileSourceId();
            String fileSourceTaskId = fileSourceDTO.getFileSourceTaskId();
            if (fileSourceId != null && fileSourceId > 0) {
                if (StringUtils.isBlank(fileSourceTaskId)) {
                    log.warn("no fileSourceTask executed for fileSourceId:{}, stepInstanceId:{}", fileSourceId,
                        stepInstanceId);
                } else {
                    fileSourceTaskIdList.add(fileSourceTaskId);
                }
            }
        }
        log.debug("FileSourceTaskIds to be cleared:{}", fileSourceTaskIdList);
        if (!fileSourceTaskIdList.isEmpty()) {
            try {
                clearTaskFiles(fileSourceTaskIdList);
            } catch (Throwable t) {
                log.error("Fail to clearTaskFiles, fileSourceTaskIdList={}", fileSourceTaskIdList, t);
            }
        }
    }

    private void clearTaskFiles(List<String> taskIdList) {
        fileSourceTaskResource.clearTaskFiles(new ClearTaskFilesReq(taskIdList));
    }

    private ThirdFilePrepareTask asyncWatchThirdFilePulling(
        StepInstanceDTO stepInstance,
        List<FileSourceDTO> fileSourceList,
        String batchTaskId,
        boolean isForRetry
    ) {
        ThirdFilePrepareTask batchResultHandleTask =
            new ThirdFilePrepareTask(
                stepInstance,
                fileSourceList,
                batchTaskId,
                isForRetry,
                new RecordableThirdFilePrepareTaskResultHandler(stepInstance.getId(), resultHandler)
            );
        batchResultHandleTask.initDependentService(fileSourceTaskResource, taskInstanceService, accountService,
            logService, taskControlMsgSender, fileSourceTaskLogDAO);
        resultHandleManager.handleDeliveredTask(batchResultHandleTask);
        return batchResultHandleTask;
    }

    private BatchTaskInfoDTO startFileSourceDownloadTask(String username, Long appId, Long stepInstanceId,
                                                         Integer executeCount,
                                                         List<FileSourceTaskContent> fileSourceTaskList) {
        FileSourceBatchDownloadTaskReq req = new FileSourceBatchDownloadTaskReq();
        req.setAppId(appId);
        req.setStepInstanceId(stepInstanceId);
        req.setExecuteCount(executeCount);
        req.setFileSourceTaskList(fileSourceTaskList);
        InternalResponse<BatchTaskInfoDTO> resp = fileSourceTaskResource.startFileSourceBatchDownloadTask(username, req);
        log.debug("resp={}", resp);
        if (resp.isSuccess()) {
            return resp.getData();
        } else {
            throw new InternalException(resp.getCode(), resp.getErrorMsg());
        }
    }

    class RecordableThirdFilePrepareTaskResultHandler implements ThirdFilePrepareTaskResultHandler {

        long stepInstanceId;
        ThirdFilePrepareTaskResultHandler resultHandler;

        public RecordableThirdFilePrepareTaskResultHandler(
            long stepInstanceId,
            ThirdFilePrepareTaskResultHandler resultHandler
        ) {
            this.stepInstanceId = stepInstanceId;
            this.resultHandler = resultHandler;
        }

        @Override
        public void onSuccess(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onSuccess(taskContext);
        }

        @Override
        public void onStopped(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onStopped(taskContext);
        }

        @Override
        public void onFailed(JobTaskContext taskContext) {
            taskMap.remove(stepInstanceId);
            resultHandler.onFailed(taskContext);
        }
    }
}
