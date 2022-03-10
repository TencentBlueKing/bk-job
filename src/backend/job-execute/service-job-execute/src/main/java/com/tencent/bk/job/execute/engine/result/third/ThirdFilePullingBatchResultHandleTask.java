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

package com.tencent.bk.job.execute.engine.result.third;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.client.FileSourceTaskResourceClient;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.result.ContinuousScheduledTask;
import com.tencent.bk.job.execute.engine.result.ScheduleStrategy;
import com.tencent.bk.job.execute.engine.result.StopTaskCounter;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.StopBatchTaskReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 第三方文件源文件下载进度拉取任务调度
 */
@Slf4j
public class ThirdFilePullingBatchResultHandleTask implements ContinuousScheduledTask {

    private final StepInstanceDTO stepInstance;
    private final List<FileSourceDTO> fileSourceList;
    private final String batchTaskId;
    private final boolean isForRetry;
    /**
     * 同步锁
     */
    private final Object stopMonitor = new Object();
    volatile AtomicBoolean isDoneWrapper = new AtomicBoolean(false);
    volatile AtomicBoolean isReadyForNextStepWrapper = new AtomicBoolean(false);
    private FileSourceTaskResourceClient fileSourceTaskResource;
    private TaskInstanceService taskInstanceService;
    private AccountService accountService;
    private LogService logService;
    private TaskExecuteControlMsgSender taskControlMsgSender;
    private FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private int pullTimes = 0;
    private long logStart = 0L;
    private long logLength = 100L;
    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;

    public ThirdFilePullingBatchResultHandleTask(StepInstanceDTO stepInstance, List<FileSourceDTO> fileSourceList,
                                                 String batchTaskId, boolean isForRetry) {
        this.stepInstance = stepInstance;
        this.fileSourceList = fileSourceList;
        this.batchTaskId = batchTaskId;
        this.isForRetry = isForRetry;
    }

    public void initDependentService(
        FileSourceTaskResourceClient fileSourceTaskResource,
        TaskInstanceService taskInstanceService,
        AccountService accountService,
        LogService logService,
        TaskExecuteControlMsgSender taskControlMsgSender,
        FileSourceTaskLogDAO fileSourceTaskLogDAO
    ) {
        this.fileSourceTaskResource = fileSourceTaskResource;
        this.taskInstanceService = taskInstanceService;
        this.accountService = accountService;
        this.logService = logService;
        this.taskControlMsgSender = taskControlMsgSender;
        this.fileSourceTaskLogDAO = fileSourceTaskLogDAO;
    }

    @Override
    public boolean isFinished() {
        return this.isDoneWrapper.get();
    }

    public boolean isReadyForNext() {
        return this.isReadyForNextStepWrapper.get();
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        // 每秒拉取一次任务状态
        return () -> 1000;
    }

    @Override
    public void execute() {
        getFileSourceBatchTaskResults(stepInstance, batchTaskId);
    }

    private void checkTaskInstanceStatus(StepInstanceDTO stepInstance, String batchTaskId) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 刷新步骤状态
        stepInstance = taskInstanceService.getStepInstanceDetail(stepInstance.getId());
        // 如果任务处于“终止中”状态，触发任务终止
        if (taskInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
            if (RunStatusEnum.STOPPING.getValue().equals(stepInstance.getStatus())
                || RunStatusEnum.STOP_SUCCESS.getValue().equals(stepInstance.getStatus())) {
                // 已经发送过停止命令的就不再重复发送了
                return;
            }
            log.info("Task instance status is stopping, stop executing the step! taskInstanceId:{}, stepInstanceId:{}",
                taskInstance.getId(), stepInstance.getId());
            // 步骤状态变更
            taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.STOPPING.getValue());
            // 停止第三方源文件拉取
            log.debug("Stop cmd received, stop pulling task, batchTaskId={}", batchTaskId);
            fileSourceTaskResource.stopBatchTasks(new StopBatchTaskReq(Collections.singletonList(batchTaskId)));
        }
    }

    public BatchTaskStatusDTO getFileSourceBatchTaskResults(StepInstanceDTO stepInstance, String batchTaskId) {
        boolean isDone = false;
        checkTaskInstanceStatus(stepInstance, batchTaskId);
        BatchTaskStatusDTO batchTaskStatusDTO = null;
        boolean allLogDone = true;
        try {
            pullTimes += 1;
            ServiceResponse<BatchTaskStatusDTO> resp = fileSourceTaskResource.getBatchTaskStatusAndLogs(batchTaskId,
                logStart, logLength);
            log.debug("resp={}", resp);
            batchTaskStatusDTO = resp.getData();
            List<FileSourceTaskStatusDTO> fileSourceTaskStatusInfoList =
                batchTaskStatusDTO.getFileSourceTaskStatusInfoList();
            int maxLogSize = 0;
            // 写日志
            for (FileSourceTaskStatusDTO fileSourceTaskStatusDTO : fileSourceTaskStatusInfoList) {
                List<ServiceIpLogDTO> logList = fileSourceTaskStatusDTO.getLogList();
                if (logList != null && !logList.isEmpty()) {
                    writeLogs(stepInstance, logList);
                    if (logList.size() > maxLogSize) {
                        maxLogSize = logList.size();
                    }
                }
                allLogDone = allLogDone && fileSourceTaskStatusDTO.getLogEnd();
            }
            if (allLogDone) {
                logStart += maxLogSize;
            }
            // 任务结束了，且日志拉取完毕才算结束
            isDone = batchTaskStatusDTO.isDone() && allLogDone;
        } catch (Exception e) {
            log.warn("Exception occurred when getFileSourceTaskStatus, tried {} times", pullTimes, e);
        }
        // 超时处理：1h
        if (!isDone && pullTimes > 3600) {
            batchTaskStatusDTO = new BatchTaskStatusDTO();
            batchTaskStatusDTO.setBatchTaskId(batchTaskId);
            batchTaskStatusDTO.setStatus(TaskStatusEnum.FAILED.getStatus());
            log.debug("{} timeout", batchTaskId);
            isDone = true;
        }
        if (isDone) {
            handleFileSourceTaskResult(stepInstance, fileSourceList, batchTaskStatusDTO);
        }
        return batchTaskStatusDTO;
    }

    private void writeLogs(StepInstanceDTO stepInstance, List<ServiceIpLogDTO> logDTOList) {
        for (ServiceIpLogDTO serviceIpLogDTO : logDTOList) {
            logService.writeFileLogWithTimestamp(stepInstance.getCreateTime(), stepInstance.getId(),
                stepInstance.getExecuteCount(), serviceIpLogDTO.getIp(), serviceIpLogDTO, System.currentTimeMillis());
        }
    }

    private void handleFileSourceTaskResult(StepInstanceDTO stepInstance, List<FileSourceDTO> fileSourceList,
                                            BatchTaskStatusDTO batchTaskStatusDTO) {
        isDoneWrapper.set(true);
        // 更新文件源拉取任务耗时数据
        FileSourceTaskLogDTO fileSourceTaskLogDTO = fileSourceTaskLogDAO.getFileSourceTaskLogByBatchTaskId(batchTaskId);
        if (fileSourceTaskLogDTO != null) {
            Long endTime = System.currentTimeMillis();
            fileSourceTaskLogDAO.updateTimeConsumingByBatchTaskId(batchTaskId, null, endTime,
                endTime - fileSourceTaskLogDTO.getStartTime());
        }
        List<FileSourceTaskStatusDTO> resultList = batchTaskStatusDTO.getFileSourceTaskStatusInfoList();
        if (resultList.isEmpty()) {
            // 直接进行下一步
            taskControlMsgSender.continueGseFileStep(stepInstance.getId());
        } else {
            // 需要处理业务
            boolean allSuccess = true;
            boolean stopped = false;
            for (FileSourceTaskStatusDTO result : resultList) {
                // 有一个文件源任务不成功则不成功
                if (result == null || !TaskStatusEnum.SUCCESS.getStatus().equals(result.getStatus())) {
                    allSuccess = false;
                }
                // 有一个文件源任务被成功终止即为终止成功
                if (result != null && TaskStatusEnum.STOPPED.getStatus().equals(result.getStatus())) {
                    stopped = true;
                }
            }
            if (allSuccess) {
                Map<String, FileSourceTaskStatusDTO> map = new HashMap<>();
                resultList.forEach(result -> {
                    map.put(result.getTaskId(), result);
                });
                //添加服务器文件信息
                for (FileSourceDTO fileSourceDTO : fileSourceList) {
                    String fileSourceTaskId = fileSourceDTO.getFileSourceTaskId();
                    if (StringUtils.isNotBlank(fileSourceTaskId)) {
                        FileSourceTaskStatusDTO fileSourceTaskStatusDTO = map.get(fileSourceTaskId);
                        fileSourceDTO.setAccount("root");
                        AccountDTO accountDTO = accountService.getAccountByAccountName(stepInstance.getAppId(), "root");
                        if (accountDTO == null) {
                            //业务无root账号，报错提示
                            log.warn("No root account in appId={}, plz config one", stepInstance.getAppId());
                            taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.FAIL.getValue());
                            taskControlMsgSender.refreshTask(stepInstance.getTaskInstanceId());
                            return;
                        }
                        fileSourceDTO.setAccountId(accountDTO.getId());
                        fileSourceDTO.setLocalUpload(false);
                        ServersDTO servers = new ServersDTO();
                        IpDTO ipDTO = new IpDTO(fileSourceTaskStatusDTO.getCloudId(), fileSourceTaskStatusDTO.getIp());
                        List<IpDTO> ipDTOList = Collections.singletonList(ipDTO);
                        servers.addStaticIps(ipDTOList);
                        if (servers.getIpList() == null) {
                            servers.setIpList(ipDTOList);
                        } else {
                            servers.getIpList().addAll(ipDTOList);
                            // 去重
                            servers.setIpList(new ArrayList<>(new HashSet<>(servers.getIpList())));
                        }
                        fileSourceDTO.setServers(servers);
                        Map<String, String> filePathMap = fileSourceTaskStatusDTO.getFilePathMap();
                        log.debug("filePathMap={}", filePathMap);
                        List<FileDetailDTO> files = fileSourceDTO.getFiles();
                        // 设置downloadPath进行后续GSE分发
                        for (FileDetailDTO file : files) {
                            String downloadPath = filePathMap.get(file.getThirdFilePath());
                            file.setFilePath(downloadPath);
                            file.setResolvedFilePath(downloadPath);
                            log.debug("file={}", file);
                        }
                    }
                }
                //更新StepInstance
                taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
                if (!isForRetry) {
                    // 直接进行下一步
                    taskControlMsgSender.continueGseFileStep(stepInstance.getId());
                }
            } else if (stopped) {
                // 步骤状态变更
                taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.STOP_SUCCESS.getValue());
                // 任务状态变更
                taskControlMsgSender.refreshTask(stepInstance.getTaskInstanceId());
                // 强制终止成功后就不再下发GSE Task了
                // taskControlMsgSender.continueGseFileStep(stepInstance.getId());
            } else {
                // 文件源文件下载失败
                taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.FAIL.getValue());
                taskControlMsgSender.refreshTask(stepInstance.getTaskInstanceId());
            }
        }
        isReadyForNextStepWrapper.set(true);
    }

    @Override
    public void stop() {
        synchronized (stopMonitor) {
            if (!isStopped) {
                StopTaskCounter.getInstance().decrement(getTaskId());
                this.isStopped = true;
            }
        }
    }

    @Override
    public String getTaskId() {
        return "file_source_batch_task:" + this.stepInstance.getId() + ":" + this.stepInstance.getExecuteCount();
    }
}
