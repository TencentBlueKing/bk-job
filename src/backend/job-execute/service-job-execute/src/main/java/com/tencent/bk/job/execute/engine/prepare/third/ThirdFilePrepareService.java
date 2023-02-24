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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.client.FileSourceTaskResourceClient;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileSourceTaskLogDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.ClearTaskFilesReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceBatchDownloadTaskReq;
import com.tencent.bk.job.file_gateway.model.req.inner.FileSourceTaskContent;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 第三方源文件准备服务，用于在分发前控制第三方源文件的准备
 */
@Slf4j
@Primary
@Component
public class ThirdFilePrepareService {
    private final ResultHandleManager resultHandleManager;
    private final FileSourceTaskResourceClient fileSourceTaskResource;
    private final TaskInstanceService taskInstanceService;
    private final FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private final AccountService accountService;
    private final HostService hostService;
    private final LogService logService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    // 记录第三方文件准备任务信息，用于在需要时查找并终止任务
    private final Map<String, ThirdFilePrepareTask> taskMap = new ConcurrentHashMap<>();

    @Autowired
    public ThirdFilePrepareService(ResultHandleManager resultHandleManager,
                                   FileSourceTaskResourceClient fileSourceTaskResource,
                                   TaskInstanceService taskInstanceService,
                                   FileSourceTaskLogDAO fileSourceTaskLogDAO,
                                   AccountService accountService,
                                   HostService hostService,
                                   LogService logService,
                                   TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher) {
        this.resultHandleManager = resultHandleManager;
        this.fileSourceTaskResource = fileSourceTaskResource;
        this.taskInstanceService = taskInstanceService;
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
        this.accountService = accountService;
        this.hostService = hostService;
        this.logService = logService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
    }

    /**
     * 将文件源文件下载任务的信息设置到分发源文件数据中
     *
     * @param taskInfoDTO   文件源文件下载任务信息
     * @param fileSourceDTO 分发源文件数据
     */
    private void setTaskInfoIntoThirdFileSource(TaskInfoDTO taskInfoDTO, FileSourceDTO fileSourceDTO) {
        String fileSourceTaskId = taskInfoDTO.getTaskId();
        if (fileSourceDTO.getServers() == null) {
            fileSourceDTO.setServers(new ServersDTO());
        }
        List<HostDTO> hostDTOList = new ArrayList<>();
        hostDTOList.add(new HostDTO(taskInfoDTO.getCloudId(), taskInfoDTO.getIp()));
        fileSourceDTO.getServers().setStaticIpList(hostDTOList);
        fileSourceDTO.getServers().setIpList(hostDTOList);
        fileSourceDTO.setFileSourceTaskId(fileSourceTaskId);
        fileSourceDTO.getFiles().forEach(fileDetailDTO -> {
            // 含文件源名称的文件路径
            fileDetailDTO.setThirdFilePathWithFileSourceName(
                PathUtil.joinFilePath(taskInfoDTO.getFileSourceName(), fileDetailDTO.getThirdFilePath())
            );
        });
    }

    /**
     * 将任务信息设置到步骤的文件源信息及统计数据中去
     *
     * @param batchTaskInfoDTO    文件源批量任务信息
     * @param thirdFileSourceList 第三方文件源列表
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

    /**
     * 解析第三方文件源数据
     *
     * @param fileSourceList 文件源列表
     * @return Pair<第三方文件源列表, 文件源文件任务列表>
     */
    private Pair<List<FileSourceDTO>, List<FileSourceTaskContent>> parseThirdFileSource(
        List<FileSourceDTO> fileSourceList
    ) {
        List<FileSourceDTO> thirdFileSourceList = new ArrayList<>();
        List<FileSourceTaskContent> fileSourceTaskList = new ArrayList<>();
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            if (fileSourceDTO == null) {
                log.warn("FileSourceDTO is null, continue");
                continue;
            }
            Integer fileSourceId = fileSourceDTO.getFileSourceId();
            if (fileSourceId == null || fileSourceId <= 0) {
                log.warn("Invalid fileSourceId({}), continue", fileSourceId);
                continue;
            }
            List<FileDetailDTO> files = fileSourceDTO.getFiles();
            if (CollectionUtils.isEmpty(files)) {
                log.warn("Files is null or empty, continue");
                continue;
            }
            for (FileDetailDTO file : files) {
                if (file != null && StringUtils.isBlank(file.getThirdFilePath())) {
                    // 不含文件源名称的文件源文件路径
                    file.setThirdFilePath(file.getFilePath());
                }
            }
            // 收集第三方文件源文件路径
            List<String> filePaths = files.parallelStream()
                .map(FileDetailDTO::getThirdFilePath)
                .collect(Collectors.toList());
            // 收集文件源文件任务
            fileSourceTaskList.add(new FileSourceTaskContent(fileSourceId, filePaths));
            // 收集文件源信息
            thirdFileSourceList.add(fileSourceDTO);
        }
        return Pair.of(thirdFileSourceList, fileSourceTaskList);
    }

    public void prepareThirdFileAsync(
        StepInstanceDTO stepInstance,
        ThirdFilePrepareTaskResultHandler resultHandler
    ) {
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        // 解析第三方源文件，收集任务数据
        Pair<List<FileSourceDTO>, List<FileSourceTaskContent>> thirdFileSource = parseThirdFileSource(fileSourceList);
        List<FileSourceDTO> thirdFileSourceList = thirdFileSource.getLeft();
        List<FileSourceTaskContent> fileSourceTaskList = thirdFileSource.getRight();
        if (CollectionUtils.isEmpty(thirdFileSourceList)) {
            continueStepAtOnce(stepInstance);
            return;
        }
        log.debug("[{}]: Start FileSourceBatchTask: {}", stepInstance.getUniqueKey(), fileSourceTaskList);
        // 启动下载任务
        BatchTaskInfoDTO batchTaskInfoDTO = startFileSourceDownloadTask(
            stepInstance.getOperator(),
            stepInstance.getAppId(),
            stepInstance.getId(),
            stepInstance.getExecuteCount(),
            fileSourceTaskList
        );
        log.info(
            "[{}]: fileSourceDownloadTask started, batchTaskId:{},taskInfoList={}",
            stepInstance.getUniqueKey(),
            batchTaskInfoDTO.getBatchTaskId(),
            batchTaskInfoDTO.getTaskInfoList()
        );
        // 填充任务信息到分发源文件数据
        setBatchTaskInfoIntoThirdFileSource(batchTaskInfoDTO, thirdFileSourceList);
        log.debug("[{}]: fileSourceList={}", stepInstance.getUniqueKey(), fileSourceList);
        // 放进文件源下载任务进度表中
        FileSourceTaskLogDTO fileSourceTaskLogDTO = buildInitFileSourceTaskLog(stepInstance, batchTaskInfoDTO);
        fileSourceTaskLogDAO.saveFileSourceTaskLog(fileSourceTaskLogDTO);
        // 更新文件源任务状态
        taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
        // 异步轮询文件下载任务
        ThirdFilePrepareTask task = asyncWatchThirdFilePulling(
            stepInstance,
            fileSourceList,
            batchTaskInfoDTO.getBatchTaskId(),
            false,
            resultHandler
        );
        taskMap.put(stepInstance.getUniqueKey(), task);
    }

    /**
     * 立即继续步骤
     *
     * @param stepInstance 步骤实例
     */
    private void continueStepAtOnce(StepInstanceDTO stepInstance) {
        taskExecuteMQEventDispatcher.dispatchGseTaskEvent(
            GseTaskEvent.startGseTask(
                stepInstance.getId(),
                stepInstance.getExecuteCount(),
                stepInstance.getBatch(),
                null,
                null
            )
        );
    }

    /**
     * 构建文件源文件拉取任务的初始日志
     *
     * @param stepInstance     步骤实例
     * @param batchTaskInfoDTO 文件源文件拉取任务信息
     * @return 文件源任务日志
     */
    private FileSourceTaskLogDTO buildInitFileSourceTaskLog(StepInstanceDTO stepInstance,
                                                            BatchTaskInfoDTO batchTaskInfoDTO) {
        FileSourceTaskLogDTO fileSourceTaskLogDTO = new FileSourceTaskLogDTO();
        fileSourceTaskLogDTO.setStepInstanceId(stepInstance.getId());
        fileSourceTaskLogDTO.setExecuteCount(stepInstance.getExecuteCount());
        fileSourceTaskLogDTO.setFileSourceBatchTaskId(batchTaskInfoDTO.getBatchTaskId());
        fileSourceTaskLogDTO.setStatus(TaskStatusEnum.INIT.getStatus().intValue());
        fileSourceTaskLogDTO.setStartTime(System.currentTimeMillis());
        return fileSourceTaskLogDTO;
    }

    public void stopPrepareThirdFileAsync(StepInstanceDTO stepInstance) {
        ThirdFilePrepareTask task = taskMap.get(stepInstance.getUniqueKey());
        if (task != null) {
            task.stopThirdFilePulling();
        }
    }

    public void clearPreparedTmpFile(long stepInstanceId) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        // 找出所有第三方文件源的TaskId进行清理
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        List<String> fileSourceTaskIdList = findFileSourceTaskIds(stepInstanceId, fileSourceList);
        log.info("FileSourceTaskIds to be cleared:{}", fileSourceTaskIdList);
        if (CollectionUtils.isEmpty(fileSourceTaskIdList)) {
            return;
        }
        try {
            // 调用file-gateway接口通知清理临时文件
            clearTaskFiles(fileSourceTaskIdList);
        } catch (Throwable t) {
            log.error("Fail to clearTaskFiles, fileSourceTaskIdList={}", fileSourceTaskIdList, t);
        }
    }

    /**
     * 从源文件数据中找到所有的第三方文件源文件下载任务ID
     *
     * @param stepInstanceId 步骤Id
     * @param fileSourceList 源文件数据列表
     * @return 所有的第三方文件源文件下载任务ID
     */
    private List<String> findFileSourceTaskIds(long stepInstanceId, List<FileSourceDTO> fileSourceList) {
        List<String> fileSourceTaskIdList = new ArrayList<>();
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            Integer fileSourceId = fileSourceDTO.getFileSourceId();
            String fileSourceTaskId = fileSourceDTO.getFileSourceTaskId();
            if (fileSourceId == null || fileSourceId <= 0) {
                continue;
            }
            if (StringUtils.isBlank(fileSourceTaskId)) {
                log.warn(
                    "no fileSourceTask executed for fileSourceId:{}, stepInstanceId:{}",
                    fileSourceId,
                    stepInstanceId
                );
            } else {
                fileSourceTaskIdList.add(fileSourceTaskId);
            }
        }
        return fileSourceTaskIdList;
    }

    /**
     * 调用file-gateway接口通知清理某些任务产生的临时文件
     *
     * @param taskIdList 任务Id列表
     */
    private void clearTaskFiles(List<String> taskIdList) {
        fileSourceTaskResource.clearTaskFiles(new ClearTaskFilesReq(taskIdList));
    }

    /**
     * 异步监控第三方文件拉取过程
     *
     * @param stepInstance   步骤实例
     * @param fileSourceList 源文件数据列表
     * @param batchTaskId    第三方文件拉取任务ID
     * @param isForRetry     是否为步骤重试
     * @param resultHandler  结果处理器
     * @return 第三方源文件准备任务
     */
    private ThirdFilePrepareTask asyncWatchThirdFilePulling(
        StepInstanceDTO stepInstance,
        List<FileSourceDTO> fileSourceList,
        String batchTaskId,
        boolean isForRetry,
        ThirdFilePrepareTaskResultHandler resultHandler
    ) {
        ThirdFilePrepareTask batchResultHandleTask =
            new ThirdFilePrepareTask(
                stepInstance,
                fileSourceList,
                batchTaskId,
                isForRetry,
                new RecordableThirdFilePrepareTaskResultHandler(stepInstance, resultHandler)
            );
        batchResultHandleTask.initDependentService(
            fileSourceTaskResource, taskInstanceService, accountService,
            hostService, logService, taskExecuteMQEventDispatcher, fileSourceTaskLogDAO
        );
        resultHandleManager.handleDeliveredTask(batchResultHandleTask);
        return batchResultHandleTask;
    }

    /**
     * 开始第三方源文件下载任务
     *
     * @param username           操作者用户名
     * @param appId              Job业务ID
     * @param stepInstanceId     步骤实例ID
     * @param executeCount       重试次数
     * @param fileSourceTaskList 第三方文件源任务列表
     * @return 任务信息
     */
    private BatchTaskInfoDTO startFileSourceDownloadTask(String username,
                                                         Long appId,
                                                         Long stepInstanceId,
                                                         Integer executeCount,
                                                         List<FileSourceTaskContent> fileSourceTaskList) {
        FileSourceBatchDownloadTaskReq req = new FileSourceBatchDownloadTaskReq();
        req.setAppId(appId);
        req.setStepInstanceId(stepInstanceId);
        req.setExecuteCount(executeCount);
        req.setFileSourceTaskList(fileSourceTaskList);
        InternalResponse<BatchTaskInfoDTO> resp = fileSourceTaskResource.startFileSourceBatchDownloadTask(
            username,
            req
        );
        if (resp.isSuccess()) {
            log.debug("startFileSourceBatchDownloadTask, req={}, resp={}", req, resp);
            return resp.getData();
        } else {
            log.warn("Fail to startFileSourceBatchDownloadTask, req={}, resp={}", req, resp);
            throw new InternalException(resp.getErrorMsg(), resp.getCode());
        }
    }

    /**
     * 可记录的第三方源文件准备任务结果处理器
     */
    class RecordableThirdFilePrepareTaskResultHandler implements ThirdFilePrepareTaskResultHandler {

        StepInstanceDTO stepInstance;
        ThirdFilePrepareTaskResultHandler resultHandler;

        public RecordableThirdFilePrepareTaskResultHandler(
            StepInstanceDTO stepInstance,
            ThirdFilePrepareTaskResultHandler resultHandler
        ) {
            this.stepInstance = stepInstance;
            this.resultHandler = resultHandler;
        }

        @Override
        public void onSuccess(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onSuccess(taskContext);
        }

        @Override
        public void onStopped(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onStopped(taskContext);
        }

        @Override
        public void onFailed(JobTaskContext taskContext) {
            taskMap.remove(stepInstance.getUniqueKey());
            resultHandler.onFailed(taskContext);
        }
    }
}
