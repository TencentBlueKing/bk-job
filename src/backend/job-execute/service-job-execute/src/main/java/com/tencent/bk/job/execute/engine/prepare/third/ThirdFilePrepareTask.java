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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.client.FileSourceTaskResourceClient;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.dao.FileSourceTaskLogDAO;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.JobTaskContext;
import com.tencent.bk.job.execute.engine.result.ContinuousScheduledTask;
import com.tencent.bk.job.execute.engine.result.ScheduleStrategy;
import com.tencent.bk.job.execute.engine.result.StopTaskCounter;
import com.tencent.bk.job.execute.model.AccountDTO;
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
import com.tencent.bk.job.file_gateway.model.req.inner.StopBatchTaskReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.BatchTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileLogPieceDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ThirdFileSourceTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 第三方文件源文件下载进度拉取任务调度
 */
@Slf4j
public class ThirdFilePrepareTask implements ContinuousScheduledTask, JobTaskContext {

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
    private HostService hostService;
    private LogService logService;
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private FileSourceTaskLogDAO fileSourceTaskLogDAO;
    private final ThirdFilePrepareTaskResultHandler resultHandler;
    private int pullTimes = 0;
    private long logStart = 0L;

    /**
     * 任务是否已停止
     */
    private volatile boolean isStopped = false;

    public ThirdFilePrepareTask(
        StepInstanceDTO stepInstance,
        List<FileSourceDTO> fileSourceList,
        String batchTaskId,
        boolean isForRetry,
        ThirdFilePrepareTaskResultHandler resultHandler
    ) {
        this.stepInstance = stepInstance;
        this.fileSourceList = fileSourceList;
        this.batchTaskId = batchTaskId;
        this.isForRetry = isForRetry;
        this.resultHandler = resultHandler;
    }

    public void initDependentService(
        FileSourceTaskResourceClient fileSourceTaskResource,
        TaskInstanceService taskInstanceService,
        AccountService accountService,
        HostService hostService,
        LogService logService,
        TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
        FileSourceTaskLogDAO fileSourceTaskLogDAO
    ) {
        this.fileSourceTaskResource = fileSourceTaskResource;
        this.taskInstanceService = taskInstanceService;
        this.accountService = accountService;
        this.hostService = hostService;
        this.logService = logService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
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
        try {
            pullTimes += 1;
            BatchTaskStatusDTO taskStatus = getFileSourceBatchTaskResults(stepInstance, batchTaskId);
            log.info(
                "[{}]: pull {}, fileSourceBatchTaskStatus={}",
                stepInstance.getUniqueKey(),
                pullTimes,
                taskStatus.getSimpleDesc()
            );
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format(
                "[{}]: Fail to getFileSourceBatchTaskResults, batchTaskId={}",
                stepInstance.getUniqueKey(),
                batchTaskId
            );
            log.error(msg.getMessage(), e);
        }
    }

    public void stopThirdFilePulling() {
        // 停止第三方源文件拉取
        log.info(
            "[{}]: Stop cmd received, stop pulling task, batchTaskId={}",
            stepInstance.getUniqueKey(),
            batchTaskId
        );
        fileSourceTaskResource.stopBatchTasks(new StopBatchTaskReq(Collections.singletonList(batchTaskId)));
    }

    public BatchTaskStatusDTO getFileSourceBatchTaskResults(StepInstanceDTO stepInstance, String batchTaskId) {
        boolean isDone = false;
        BatchTaskStatusDTO batchTaskStatusDTO = null;
        boolean allLogDone = true;
        try {
            long logLength = 100L;
            InternalResponse<BatchTaskStatusDTO> resp = fileSourceTaskResource.getBatchTaskStatusAndLogs(
                batchTaskId,
                logStart,
                logLength
            );
            if (log.isDebugEnabled()) {
                log.debug(
                    "[{}]: batchTaskId={},resp={}",
                    stepInstance.getUniqueKey(),
                    batchTaskId,
                    JsonUtils.toJson(resp)
                );
            }
            batchTaskStatusDTO = resp.getData();
            List<FileSourceTaskStatusDTO> fileSourceTaskStatusInfoList =
                batchTaskStatusDTO.getFileSourceTaskStatusInfoList();
            int maxLogSize = 0;
            // 写日志
            for (FileSourceTaskStatusDTO fileSourceTaskStatusDTO : fileSourceTaskStatusInfoList) {
                List<ThirdFileSourceTaskLogDTO> logList = fileSourceTaskStatusDTO.getLogList();
                if (logList != null && !logList.isEmpty()) {
                    writeLogs(stepInstance, fileSourceTaskStatusDTO, logList);
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
            FormattingTuple msg = MessageFormatter.format(
                "[{}][{}]:Exception occurred when getFileSourceTaskStatus, tried {} times",
                new Object[]{
                    stepInstance.getId(),
                    batchTaskId,
                    pullTimes
                }
            );
            log.warn(msg.getMessage(), e);
        }
        // 超时处理：1h
        if (!isDone && pullTimes > 3600) {
            batchTaskStatusDTO = new BatchTaskStatusDTO();
            batchTaskStatusDTO.setBatchTaskId(batchTaskId);
            batchTaskStatusDTO.setStatus(TaskStatusEnum.FAILED.getStatus());
            log.info("[{}]: batchTaskId={} timeout 3600s", stepInstance.getUniqueKey(), batchTaskId);
            isDone = true;
        }
        if (isDone) {
            isDoneWrapper.set(true);
            handleFileSourceTaskResult(stepInstance, batchTaskStatusDTO);
            isReadyForNextStepWrapper.set(true);
        }
        return batchTaskStatusDTO;
    }

    private void handleFileSourceTaskResult(
        StepInstanceDTO stepInstance,
        BatchTaskStatusDTO batchTaskStatusDTO
    ) {
        // 更新文件源拉取任务耗时数据
        FileSourceTaskLogDTO fileSourceTaskLogDTO = fileSourceTaskLogDAO.getFileSourceTaskLogByBatchTaskId(batchTaskId);
        if (fileSourceTaskLogDTO != null) {
            Long endTime = System.currentTimeMillis();
            fileSourceTaskLogDAO.updateTimeConsumingByBatchTaskId(batchTaskId, null, endTime,
                endTime - fileSourceTaskLogDTO.getStartTime());
        }
        List<FileSourceTaskStatusDTO> taskStatusList = batchTaskStatusDTO.getFileSourceTaskStatusInfoList();
        if (taskStatusList.isEmpty()) {
            // 直接成功
            log.info(
                "[{}]: batchTaskId={}, fileSourceTaskStatusInfoList is empty, skip",
                stepInstance.getUniqueKey(),
                batchTaskId
            );
            resultHandler.onSuccess(this);
            return;
        }
        // 需要处理业务
        Pair<Boolean, Boolean> statePair = checkSuccessAndStopState(taskStatusList);
        boolean allSuccess = statePair.getLeft();
        boolean stopped = statePair.getRight();
        if (allSuccess) {
            onSuccess(taskStatusList);
        } else if (stopped) {
            resultHandler.onStopped(this);
        } else {
            resultHandler.onFailed(this);
        }
    }

    private Pair<Boolean, Boolean> checkSuccessAndStopState(List<FileSourceTaskStatusDTO> fileSourceTaskStatusList) {
        boolean allSuccess = true;
        boolean stopped = false;
        for (FileSourceTaskStatusDTO fileSourceTaskStatus : fileSourceTaskStatusList) {
            // 有一个文件源任务不成功则不成功
            if (fileSourceTaskStatus == null) {
                log.warn("[{}]:some fileSourceTaskStatus is null", stepInstance.getUniqueKey());
                allSuccess = false;
                continue;
            }
            if (!TaskStatusEnum.SUCCESS.getStatus().equals(fileSourceTaskStatus.getStatus())) {
                log.info(
                    "[{}]: fileSourceTaskId={},status={},message={}",
                    stepInstance.getUniqueKey(),
                    fileSourceTaskStatus.getTaskId(),
                    TaskStatusEnum.valueOf(fileSourceTaskStatus.getStatus()),
                    fileSourceTaskStatus.getMessage()
                );
                allSuccess = false;
            }
            // 有一个文件源任务被成功终止即为终止成功
            if (TaskStatusEnum.STOPPED.getStatus().equals(fileSourceTaskStatus.getStatus())) {
                stopped = true;
            }
        }
        return Pair.of(allSuccess, stopped);
    }

    private void onSuccess(List<FileSourceTaskStatusDTO> fileSourceTaskStatusList) {
        Map<String, FileSourceTaskStatusDTO> map = new HashMap<>();
        fileSourceTaskStatusList.forEach(taskStatus -> map.put(taskStatus.getTaskId(), taskStatus));
        //添加服务器文件信息
        boolean isGseV2Task = stepInstance.isTargetGseV2Agent();
        for (FileSourceDTO fileSourceDTO : fileSourceList) {
            String fileSourceTaskId = fileSourceDTO.getFileSourceTaskId();
            if (StringUtils.isNotBlank(fileSourceTaskId)) {
                FileSourceTaskStatusDTO fileSourceTaskStatusDTO = map.get(fileSourceTaskId);
                fileSourceDTO.setAccount("root");
                AccountDTO accountDTO = accountService.getAccountByAccountName(stepInstance.getAppId(), "root");
                if (accountDTO == null) {
                    //业务无root账号，报错提示
                    log.error(
                        "[{}]: No root account in appId={}, plz config one",
                        stepInstance.getUniqueKey(),
                        stepInstance.getAppId()
                    );
                    taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.FAIL.getValue());
                    taskExecuteMQEventDispatcher.dispatchJobEvent(
                        JobEvent.refreshJob(stepInstance.getTaskInstanceId(),
                            EventSource.buildStepEventSource(stepInstance.getId())));
                    return;
                }
                fileSourceDTO.setAccountId(accountDTO.getId());
                fileSourceDTO.setLocalUpload(false);

                ServersDTO servers = new ServersDTO();
                HostDTO hostDTO = parseFileWorkerHostWithCache(
                    fileSourceTaskStatusDTO.getCloudId(),
                    fileSourceTaskStatusDTO.getIpProtocol(),
                    fileSourceTaskStatusDTO.getIp()
                );
                if (hostDTO == null) {
                    log.error(
                        "[{}]: Cannot find file-worker host info by IP{} (cloudAreaId={}, ip={}), " +
                            "plz check whether file-worker gse agent is installed",
                        stepInstance.getUniqueKey(),
                        fileSourceTaskStatusDTO.getIpProtocol(),
                        fileSourceTaskStatusDTO.getCloudId(),
                        fileSourceTaskStatusDTO.getIp()
                    );
                    throw new InternalException(ErrorCode.FILE_WORKER_NOT_FOUND);
                }

                HostDTO sourceHost = hostDTO.clone();
                if (isGseV2Task) {
                    if (StringUtils.isBlank(sourceHost.getAgentId())) {
                        log.error("Using gseV2, source host agent id is empty! host: {}", sourceHost);
                        throw new InternalException(ErrorCode.CAN_NOT_FIND_AVAILABLE_FILE_WORKER);
                    }
                } else {
                    sourceHost.setAgentId(sourceHost.toCloudIp());
                }
                List<HostDTO> hostDTOList = Collections.singletonList(sourceHost);
                servers.addStaticIps(hostDTOList);
                if (servers.getIpList() == null) {
                    servers.setIpList(hostDTOList);
                } else {
                    servers.getIpList().addAll(hostDTOList);
                    // 去重
                    servers.setIpList(new ArrayList<>(new HashSet<>(servers.getIpList())));
                }
                fileSourceDTO.setServers(servers);
                Map<String, String> filePathMap = fileSourceTaskStatusDTO.getFilePathMap();
                log.debug(
                    "[{}]: filePathMap={}",
                    stepInstance.getUniqueKey(),
                    filePathMap
                );
                List<FileDetailDTO> files = fileSourceDTO.getFiles();
                // 设置downloadPath进行后续GSE分发
                for (FileDetailDTO file : files) {
                    String downloadPath = filePathMap.get(file.getThirdFilePath());
                    file.setFilePath(downloadPath);
                    file.setResolvedFilePath(downloadPath);
                }
            }
        }
        //更新StepInstance
        taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), fileSourceList);
        resultHandler.onSuccess(this);
    }

    // 查询file-worker对应主机信息使用60s缓存，避免短时间内多次重复查询
    private final LoadingCache<Triple<Long, String, String>, HostDTO> fileWorkerHostCache = CacheBuilder.newBuilder()
        .maximumSize(1).expireAfterWrite(60, TimeUnit.SECONDS).
            build(new CacheLoader<Triple<Long, String, String>, HostDTO>() {
                      @SuppressWarnings("all")
                      @Override
                      public HostDTO load(Triple<Long, String, String> triple) {
                          return parseFileWorkerHost(triple.getLeft(), triple.getMiddle(), triple.getRight());
                      }
                  }
            );

    private HostDTO parseFileWorkerHostWithCache(Long cloudAreaId, String ipProtocol, String ip) {
        try {
            return fileWorkerHostCache.get(buildCacheKey(cloudAreaId, ipProtocol, ip));
        } catch (ExecutionException e) {
            log.error("Fail to parseFileWorkerHostWithCache", e);
            return null;
        }
    }

    private Triple<Long, String, String> buildCacheKey(Long cloudAreaId, String ipProtocol, String ip) {
        return Triple.of(cloudAreaId, ipProtocol, ip);
    }

    private HostDTO parseFileWorkerHost(Long cloudAreaId, String ipProtocol, String ip) {
        if (StringUtils.isBlank(ipProtocol)) {
            ipProtocol = IpUtils.inferProtocolByIp(ip);
            log.info("ipProtocol is null or blank, use {} inferred by ip {}", ipProtocol, ip);
        }
        HostDTO hostDTO;
        if (IpUtils.PROTOCOL_IP_V6.equalsIgnoreCase(ipProtocol)) {
            hostDTO = ServiceHostDTO.toHostDTO(hostService.getHostByCloudIpv6(cloudAreaId, ip));
        } else {
            hostDTO = ServiceHostDTO.toHostDTO(hostService.getHost(new HostDTO(cloudAreaId, ip)));
        }
        log.debug("host get by ({},{},{}) is {}", ipProtocol, cloudAreaId, ip, hostDTO);
        return hostDTO;
    }

    private void writeLogs(StepInstanceDTO stepInstance,
                           FileSourceTaskStatusDTO fileSourceTaskStatusDTO,
                           List<ThirdFileSourceTaskLogDTO> logDTOList) {
        List<ServiceHostLogDTO> serviceHostLogDTOList = new ArrayList<>();
        for (ThirdFileSourceTaskLogDTO logDTO : logDTOList) {
            HostDTO host = parseFileWorkerHostWithCache(
                fileSourceTaskStatusDTO.getCloudId(),
                fileSourceTaskStatusDTO.getIpProtocol(),
                fileSourceTaskStatusDTO.getIp()
            );
            serviceHostLogDTOList.add(buildServiceHostLogDTO(host, logDTO));
        }
        logService.writeFileLogsWithTimestamp(
            stepInstance.getCreateTime(),
            serviceHostLogDTOList,
            System.currentTimeMillis()
        );
    }

    /**
     * 将第三方文件源文件的下载任务日志转为统一的日志格式进行存储
     *
     * @param host                   机器信息
     * @param thirdFileSourceTaskLog 第三方源文件下载任务日志
     * @return 统一格式日志实体
     */
    private ServiceHostLogDTO buildServiceHostLogDTO(HostDTO host, ThirdFileSourceTaskLogDTO thirdFileSourceTaskLog) {
        if (thirdFileSourceTaskLog == null) {
            return null;
        }
        ServiceHostLogDTO serviceHostLog = new ServiceHostLogDTO();
        serviceHostLog.setStepInstanceId(stepInstance.getId());
        serviceHostLog.setExecuteCount(stepInstance.getExecuteCount());
        serviceHostLog.setBatch(stepInstance.getBatch());
        serviceHostLog.setHostId(host.getHostId());
        serviceHostLog.setIp(host.getIp());
        serviceHostLog.setFileTaskLogs(buildFileTaskLogs(host, thirdFileSourceTaskLog.getFileTaskLogs()));
        return serviceHostLog;
    }

    /**
     * 将第三方文件源任务日志转为Job系统统一格式日志用于存储与展示
     *
     * @param host          日志关联的主机信息
     * @param fileLogPieces 文件下载日志列表
     * @return Job日志列表
     */
    private List<ServiceFileTaskLogDTO> buildFileTaskLogs(HostDTO host, List<FileLogPieceDTO> fileLogPieces) {
        if (CollectionUtils.isEmpty(fileLogPieces)) {
            return Collections.emptyList();
        }
        return fileLogPieces.stream().map(fileLogPiece -> {
            ServiceFileTaskLogDTO serviceFileTaskLog = new ServiceFileTaskLogDTO();
            // 第三方源文件下载后用于分发上传，仅有源文件相关属性
            serviceFileTaskLog.setMode(FileDistModeEnum.UPLOAD.getValue());
            serviceFileTaskLog.setSrcIp(fileLogPiece.getSrcIp());
            serviceFileTaskLog.setSrcHostId(host.getHostId());
            serviceFileTaskLog.setSrcFile(fileLogPiece.getSrcFile());
            serviceFileTaskLog.setDisplaySrcFile(fileLogPiece.getDisplaySrcFile());
            serviceFileTaskLog.setSize(fileLogPiece.getSize());
            serviceFileTaskLog.setStatus(fileLogPiece.getStatus());
            serviceFileTaskLog.setStatusDesc(fileLogPiece.getStatusDesc());
            serviceFileTaskLog.setSpeed(fileLogPiece.getSpeed());
            serviceFileTaskLog.setProcess(fileLogPiece.getProcess());
            serviceFileTaskLog.setContent(fileLogPiece.getContent());
            return serviceFileTaskLog;
        }).collect(Collectors.toList());
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

    @Override
    public boolean isForRetry() {
        return isForRetry;
    }
}
