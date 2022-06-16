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

package com.tencent.bk.job.execute.engine.result;

import com.google.common.collect.Sets;
import com.tencent.bk.gse.taskapi.api_map_rsp;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatus;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.consts.GSECode;
import com.tencent.bk.job.execute.engine.consts.GseConstants;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.gse.model.CopyFileRsp;
import com.tencent.bk.job.execute.engine.gse.model.GSEFileTaskResult;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileTaskLog;
import com.tencent.bk.job.execute.engine.model.GseLog;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.FilePathUtils;
import com.tencent.bk.job.execute.engine.util.NFSUtils;
import com.tencent.bk.job.execute.engine.util.Utils;
import com.tencent.bk.job.execute.engine.util.WindowsHelper;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.StopWatch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 文件任务执行结果处理
 */
@Slf4j
public class FileResultHandleTask extends AbstractResultHandleTask<api_map_rsp> {
    private static final String FILE_TASK_MODE_DOWNLOAD = "download";
    private static final String FILE_TASK_MODE_UPLOAD = "upload";
    /**
     * GSE 源 Agent 任务, Map<AgentId,AgentTask>
     */
    private final Map<String, AgentTaskDTO> sourceAgentTaskMap;
    /**
     * 待分发文件，文件传输的源文件
     */
    private final Set<JobFile> sendFiles;
    /**
     * 任务包含的源服务器
     */
    private final Set<String> sourceAgentIds = new HashSet<>();
    /**
     * 已经分析结果完成的源服务器
     */
    protected Set<String> analyseFinishedSourceAgentIds = new HashSet<>();
    /**
     * 成功的文件下载任务，key: agentId, value: 成功的文件任务名称
     */
    private final Map<String, Set<String>> successDownloadFileMap = new HashMap<>();
    /**
     * 已经结束的文件下载任务，key: agentId, value: 已完成文件任务名称
     */
    private final Map<String, Set<String>> finishedDownloadFileMap = new HashMap<>();
    /**
     * 下载文件任务数，key: agentId, value: 主机对应的文件任务数
     */
    private final Map<String, Integer> fileDownloadTaskNumMap = new HashMap<>();
    /**
     * 成功的文件上传任务，key: agentId, value: 成功的文件任务名称
     */
    private final Map<String, Set<String>> successUploadFileMap = new HashMap<>();
    /**
     * 已经结束的文件上传任务，key: agentId, value: 已完成文件任务名称
     */
    private final Map<String, Set<String>> finishedUploadFileMap = new HashMap<>();
    /**
     * 上传文件任务数，key: agentId, value: 主机对应的文件任务数
     */
    private final Map<String, Integer> fileUploadTaskNumMap = new HashMap<>();
    /**
     * 文件任务进度表
     */
    private final Map<String, Integer> processMap = new HashMap<>();
    /**
     * 本地文件上传目录
     */
    private final String localUploadDir;
    /**
     * 源文件ip与云区域IP的映射关系；为了规避GSE BUG，GSE Server的源IP在多文件的场景下会返回-1；
     * 为了解决GSE Server日志不返回源IP的云区域ID的问题
     */
    private final Map<String, String> intSourceIpMapping = new HashMap<>();
    /**
     * 分发的源文件路径与分发之后的目标文件路径的映射关系
     */
    private final Map<String, String> sourceDestPathMap;
    /**
     * 源文件真实路径与显示路径的映射关系，用于本地文件分发场景下的真实路径隐藏
     */
    private final Map<String, String> sourceFileDisplayMap;
    /**
     * 下载全部结束的时间
     */
    private long downloadFinishedTime = 0;
    /**
     * 未开始任务的文件源服务器
     */
    private final Set<String> notStartedFileSourceAgentIds = new HashSet<>();
    /**
     * 正在执行任务的文件源服务器
     */
    private final Set<String> runningFileSourceAgentIds = new HashSet<>();
    /**
     * 文件任务执行结果处理调度策略
     */
    private volatile ScheduleStrategy scheduleStrategy;
    /**
     * 任务基本信息，用于日志输出
     */
    private String taskInfo;

    public FileResultHandleTask(TaskInstanceService taskInstanceService,
                                GseTaskService gseTaskService,
                                LogService logService,
                                TaskInstanceVariableService taskInstanceVariableService,
                                StepInstanceVariableValueService stepInstanceVariableValueService,
                                TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                ExceptionStatusManager exceptionStatusManager,
                                TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                FileAgentTaskService fileAgentTaskService,
                                StepInstanceService stepInstanceService,
                                TaskInstanceDTO taskInstance,
                                StepInstanceDTO stepInstance,
                                TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                                Map<String, AgentTaskDTO> targetAgentTaskMap,
                                Map<String, AgentTaskDTO> sourceAgentTaskMap,
                                GseTaskDTO gseTask,
                                Set<JobFile> sendFiles,
                                String storageRootPath,
                                Map<String, String> sourceDestPathMap,
                                Map<String, String> sourceFileDisplayMap,
                                String requestId) {
        super(taskInstanceService,
            gseTaskService,
            logService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            exceptionStatusManager,
            taskEvictPolicyExecutor,
            fileAgentTaskService,
            stepInstanceService,
            taskInstance,
            stepInstance,
            taskVariablesAnalyzeResult,
            targetAgentTaskMap,
            gseTask,
            requestId);
        this.sourceAgentTaskMap = sourceAgentTaskMap;
        this.sendFiles = sendFiles;
        this.localUploadDir = NFSUtils.getFileDir(storageRootPath, FileDirTypeConf.UPLOAD_FILE_DIR);
        if (sourceDestPathMap != null) {
            this.sourceDestPathMap = sourceDestPathMap;
        } else {
            this.sourceDestPathMap = new HashMap<>();
        }
        this.sourceFileDisplayMap = sourceFileDisplayMap;
        initFileTaskNumMap();
        initSourceAgentIds();
        initFileSourceIntIpMapping();

        log.info("InitFileResultHandleTask|stepInstanceId: {}|sendFiles: {}|fileSourceAgentIds: {}|targetAgentIds: {}|"
                + "fileUploadTaskNumMap: {}|fileDownloadTaskNumMap: {}",
            stepInstance.getId(), sendFiles, sourceAgentIds, targetAgentIds, fileUploadTaskNumMap,
            fileDownloadTaskNumMap);
    }


    /**
     * 初始化文件任务计数器
     */
    private void initFileTaskNumMap() {
        for (String agentId : this.targetAgentIds) {
            this.fileDownloadTaskNumMap.put(agentId, this.sendFiles.size());
        }

        for (JobFile sendFile : this.sendFiles) {
            String agentId = sendFile.getAgentId();
            this.fileUploadTaskNumMap.put(agentId, this.fileUploadTaskNumMap.get(agentId) == null ? 1 :
                (this.fileUploadTaskNumMap.get(agentId) + 1));
        }
    }

    private void initSourceAgentIds() {
        sourceAgentTaskMap.values().forEach(agentTask -> {
            this.notStartedFileSourceAgentIds.add(agentTask.getAgentId());
            this.sourceAgentIds.add(agentTask.getAgentId());
        });
    }

    private void initFileSourceIntIpMapping() {
        // gse bug，使用int保存ip会溢出，所以必须根据当前的源ip反向推出key中源ip
        if (this.intSourceIpMapping.isEmpty()) {
            for (String cloudIp : this.sourceAgentIds) {
                this.intSourceIpMapping.put(
                    String.valueOf((int) IpUtils.getStringIpToLong(cloudIp.split(":")[1])),
                    cloudIp);
            }
        }
    }

    @Override
    GseLogBatchPullResult<api_map_rsp> pullGseTaskResultInBatches() {
        api_map_rsp gseLog;
        if (CollectionUtils.isNotEmpty(this.analyseFinishedSourceAgentIds)
            || CollectionUtils.isNotEmpty(this.analyseFinishedTargetAgentIds)) {
            Set<String> notFinishedAgentIds = new HashSet<>();
            notFinishedAgentIds.addAll(notStartedFileSourceAgentIds);
            notFinishedAgentIds.addAll(runningFileSourceAgentIds);
            notFinishedAgentIds.addAll(notStartedTargetAgentIds);
            notFinishedAgentIds.addAll(runningTargetAgentIds);
            gseLog = GseRequestUtils.pullCopyFileTaskLog(this.stepInstanceId, this.gseTask.getGseTaskId(),
                notFinishedAgentIds);
        } else {
            gseLog = GseRequestUtils.pullCopyFileTaskLog(this.stepInstanceId, this.gseTask.getGseTaskId());
        }
        GseLogBatchPullResult<api_map_rsp> pullResult;
        if (gseLog != null) {
            pullResult = new GseLogBatchPullResult<>(
                true, true, new FileTaskLog(gseLog), null);
        } else {
            pullResult = new GseLogBatchPullResult<>(true, true, null, null);
        }
        return pullResult;
    }

    @Override
    GseTaskExecuteResult analyseGseTaskResult(GseLog<api_map_rsp> taskDetail) {
        Set<Map.Entry<String, String>> ipResults = taskDetail.getGseLog().getResult().entrySet();
        // 执行日志, Map<hostId, 日志>
        Map<Long, ServiceHostLogDTO> executionLogs = new HashMap<>();

        StopWatch watch = new StopWatch("analyse-gse-file-task");
        watch.start("analyse");
        for (Map.Entry<String, String> ipResult : ipResults) {
            CopyFileRsp copyFileRsp = parseCopyFileRspFromGSELog(ipResult);
            if (copyFileRsp == null) {
                continue;
            }
            GSEFileTaskResult fileTaskResult = copyFileRsp.getGseFileTaskResult();

            if (!shouldAnalyse(fileTaskResult)) {
                continue;
            }

            boolean isDownloadLog = fileTaskResult.isDownloadMode();
            String agentId = isDownloadLog ?
                fileTaskResult.getDestAgentId() : fileTaskResult.getSourceAgentId();

            if (isDownloadLog && this.targetAgentIds.contains(agentId)) {
                this.runningTargetAgentIds.add(agentId);
            } else if (!isDownloadLog && this.sourceAgentIds.contains(agentId)) {
                this.runningFileSourceAgentIds.add(agentId);
            }

            analyseFileResult(agentId, copyFileRsp, executionLogs, isDownloadLog);
        }
        watch.stop();

        // 保存文件分发日志
        watch.start("saveFileLog");
        writeFileTaskLogContent(executionLogs);
        watch.stop();

        // 保存任务执行结果
        watch.start("saveAgentTasks");
        batchSaveChangedGseAgentTasks();
        watch.stop();

        log.info("Analyse gse task log [{}] -> runningTargetAgentIds={}, " +
                "notStartedTargetAgentIds={}, runningFileSourceAgentIds={}, notStartedFileSourceAgentIds={}, " +
                "analyseFinishedTargetAgentIds={}, analyseFinishedSourceAgentIds={}, finishedDownloadFileMap={}, " +
                "successDownloadFileMap={}, finishedUploadFileMap={}, successUploadFileMap={}",
            this.stepInstanceId,
            this.runningTargetAgentIds,
            this.notStartedTargetAgentIds,
            this.runningFileSourceAgentIds,
            this.notStartedFileSourceAgentIds,
            this.analyseFinishedTargetAgentIds,
            this.analyseFinishedSourceAgentIds,
            this.finishedDownloadFileMap,
            this.successDownloadFileMap,
            this.finishedUploadFileMap,
            this.successUploadFileMap
        );

        if (watch.getTotalTimeMillis() > 1000L) {
            log.info("Analyse file gse task is slow, statistics: {}", watch.prettyPrint());
        }
        return analyseExecuteResult();
    }

    private void analyseFileResult(String agentId, CopyFileRsp copyFileRsp,
                                   Map<Long, ServiceHostLogDTO> executionLogs,
                                   boolean isDownloadLog) {
        AgentTaskDTO agentTask = getAgentTask(isDownloadLog, agentId);
        if (agentTask.getStartTime() == null) {
            agentTask.setStartTime(System.currentTimeMillis());
        }
        agentTask.setErrorCode(copyFileRsp.getFinalErrorCode());
        GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(copyFileRsp.getFinalErrorCode());
        GSEFileTaskResult fileTaskResult = copyFileRsp.getGseFileTaskResult();
        switch (errorCode) {
            case RUNNING:
                parseExecutionLog(copyFileRsp, executionLogs);
                agentTask.setStatus(AgentTaskStatus.RUNNING.getValue());
                if (isDownloadLog) {
                    this.notStartedTargetAgentIds.remove(agentId);
                }
                break;
            case FINISHED:
                parseExecutionLog(copyFileRsp, executionLogs);
                if (fileTaskResult.getProcess() == 100) {
                    if (isDownloadLog) {
                        addFinishedFile(true, true,
                            fileTaskResult.getDestAgentId(), fileTaskResult.getTaskId());
                    } else {
                        addFinishedFile(true, false,
                            fileTaskResult.getSourceAgentId(), fileTaskResult.getTaskId());
                    }
                    // 分析日志，更新successAgentIds、notStartedAgentIds等状态集合
                    analyseAgentTaskResult(errorCode.getValue(), agentId, fileTaskResult.getStartTime(),
                        fileTaskResult.getEndTime(), isDownloadLog);
                } else {
                    agentTask.setStatus(AgentTaskStatus.RUNNING.getValue());
                    this.notStartedTargetAgentIds.remove(agentId);
                }
                break;
            case TERMINATE:
                parseExecutionLog(copyFileRsp, executionLogs);
                if (isDownloadLog) {
                    addFinishedFile(false, true,
                        fileTaskResult.getDestAgentId(), fileTaskResult.getTaskId());
                } else {
                    addFinishedFile(false, false,
                        fileTaskResult.getSourceAgentId(), fileTaskResult.getTaskId());
                }
                analyseAgentTaskResult(errorCode.getValue(), agentId, fileTaskResult.getStartTime(),
                    fileTaskResult.getEndTime(), isDownloadLog);
                this.isTerminatedSuccess = true;
                break;
            default:
                dealIpTaskFail(copyFileRsp, executionLogs, isDownloadLog);
                break;
        }
    }

    private AgentTaskDTO getAgentTask(boolean isDownloadLog, String agentId) {
        if (isDownloadLog) {
            return targetAgentTasks.get(agentId);
        } else {
            return sourceAgentTaskMap.get(agentId);
        }
    }

    /*
     * 分析执行结果
     *
     * @return 任务执行结果
     */
    private GseTaskExecuteResult analyseExecuteResult() {
        GseTaskExecuteResult rst;
        // 目标下载全部完成
        if (this.notStartedTargetAgentIds.isEmpty() && this.runningTargetAgentIds.isEmpty()) {
            // 源上传全部完成
            if (this.notStartedFileSourceAgentIds.isEmpty() && this.runningFileSourceAgentIds.isEmpty()) {
                int successTargetIpNum = this.successTargetAgentIds.size();
                int targetIPNum = this.targetAgentIds.size();
                boolean isSuccess = !stepInstance.hasInvalidHost() && successTargetIpNum == targetIPNum;
                if (isSuccess) {
                    rst = GseTaskExecuteResult.SUCCESS;
                } else {
                    if (this.isTerminatedSuccess) {
                        rst = GseTaskExecuteResult.STOP_SUCCESS;
                    } else {
                        rst = GseTaskExecuteResult.FAILED;
                    }
                }
                log.info("[{}] AnalyseExecuteResult-> Result: finished. All source and target ip have completed tasks",
                    this.stepInstanceId);
            } else {
                // 场景：下载任务已全部结束，但是GSE未更新上传任务的状态。如果超过15s没有结束上传任务，那么任务结束
                if (this.downloadFinishedTime == 0) {
                    this.downloadFinishedTime = System.currentTimeMillis();
                }
                if (System.currentTimeMillis() - this.downloadFinishedTime > 15_000L) {
                    int targetIPNum = this.targetAgentIds.size();
                    int successTargetIpNum = this.successTargetAgentIds.size();
                    boolean isSuccess = !stepInstance.hasInvalidHost() && successTargetIpNum == targetIPNum;
                    rst = isSuccess ? GseTaskExecuteResult.SUCCESS : GseTaskExecuteResult.FAILED;
                    log.info("[{}] AnalyseExecuteResult-> Result: finished. Download tasks are finished, " +
                            "but upload tasks are not finished after 15 seconds. Ignore upload tasks",
                        this.stepInstanceId);
                } else {
                    rst = GseTaskExecuteResult.RUNNING;
                    log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks are finished, " +
                        "wait for upload task to complete.", this.stepInstanceId);
                }
            }

        } else {
            rst = GseTaskExecuteResult.RUNNING;
            log.info("[{}] AnalyseExecuteResult-> Result: running. Download tasks has not been finished",
                this.stepInstanceId);
        }
        return rst;
    }

    private boolean shouldAnalyse(GSEFileTaskResult fileTaskResult) {
        if (fileTaskResult == null) {
            return false;
        }
        boolean isDownloadMode = fileTaskResult.isDownloadMode();
        String agentId = isDownloadMode ?
            fileTaskResult.getDestAgentId() : fileTaskResult.getSourceAgentId();
        boolean shouldAnalyse = true;
        if (isDownloadMode) {
            if (this.analyseFinishedTargetAgentIds.contains(agentId) // 该ip已经日志分析结束，不要再分析
                // 该文件下载任务已结束
                || (this.finishedDownloadFileMap.get(agentId) != null
                && this.finishedDownloadFileMap.get(agentId).contains(fileTaskResult.getTaskId()))
                // 非目标IP
                || !this.fileDownloadTaskNumMap.containsKey(agentId)) {
                shouldAnalyse = false;
            }
        } else {
            if ((this.finishedUploadFileMap.get(agentId) != null
                && this.finishedUploadFileMap.get(agentId).contains(fileTaskResult.getTaskId()))
                // 非源IP
                || !this.fileUploadTaskNumMap.containsKey(agentId)) {
                shouldAnalyse = false;
            }
        }
        return shouldAnalyse;
    }

    private CopyFileRsp parseCopyFileRspFromGSELog(Map.Entry<String, String> ipResult) {
        log.info("[{}]: ParseIpResult: {}", this.stepInstanceId, ipResult);
        String taskInfo = ipResult.getValue();
        CopyFileRsp copyFileRsp;
        try {
            copyFileRsp = JsonUtils.fromJson(taskInfo, CopyFileRsp.class);
            if (copyFileRsp == null) {
                return null;
            }
        } catch (Throwable e) {
            log.error("[" + this.stepInstanceId + "]: Convert to CopyFileRsp error", e);
            return null;
        }

        // 新版GSE, 把所有任务相关的信息全部放入fileTaskResult，并且新增了status字段
        boolean isStandardGSEProtocol = isStandardGSEProtocol(copyFileRsp.getGseFileTaskResult());
        if (!isStandardGSEProtocol) {
            copyFileRsp = parseCopyFileRspFromResultKey(copyFileRsp, ipResult.getKey());
            if (copyFileRsp != null) {
                log.debug("Parse from resultKey, copyFileRsp: {}", copyFileRsp);
            }
        }
        return copyFileRsp;
    }

    private boolean isStandardGSEProtocol(GSEFileTaskResult fileTaskResult) {
        return fileTaskResult != null && fileTaskResult.getProtocolVersion() != null
            && fileTaskResult.getProtocolVersion() > 1;
    }

    private CopyFileRsp parseCopyFileRspFromResultKey(CopyFileRsp copyFileRsp, String resultKey) {
        boolean isGSEAgentLog =
            resultKey.startsWith(FILE_TASK_MODE_DOWNLOAD) || resultKey.startsWith(FILE_TASK_MODE_UPLOAD);
        // 过滤GSE Server 115状态的日志（GSE BUG, TO BE FIXED)
        if (!isGSEAgentLog && copyFileRsp.getFinalErrorCode().equals(GSECode.AtomicErrorCode.RUNNING.getValue())) {
            return null;
        }

        // 从key中提取任务信息
        String[] taskProps = resultKey.split(":");
        GSEFileTaskResult fileTaskResult = copyFileRsp.getGseFileTaskResult();
        if (fileTaskResult == null) {
            fileTaskResult = new GSEFileTaskResult();
            copyFileRsp.setGseFileTaskResult(fileTaskResult);
        }
        if (fileTaskResult.getMode() == null) {
            fileTaskResult.setMode(parseFileTaskModeFromKey(isGSEAgentLog, taskProps).getValue());
        }
        HostDTO cloudIp = parseCloudIpFromKey(taskProps);
        if (FileDistModeEnum.DOWNLOAD.getValue().equals(fileTaskResult.getMode())) {
            fileTaskResult.setDestIp(cloudIp.getIp());
            fileTaskResult.setDestCloudId(cloudIp.getBkCloudId());
            HostDTO fileSourceCloudIp = parseFileSourceIpFromKey(isGSEAgentLog, cloudIp, taskProps);
            if (fileSourceCloudIp != null) {
                fileTaskResult.setSourceCloudId(fileSourceCloudIp.getBkCloudId());
                fileTaskResult.setSourceIp(fileSourceCloudIp.getIp());
            }
            // GSE BUG, 只有目标文件信息，没有源文件信息
            String destFilePath = parseFilePathFromKey(taskProps);
            Pair<String, String> dirAndFileName = FilePathUtils.parseDirAndFileName(destFilePath);
            fileTaskResult.setDestDirPath(dirAndFileName.getLeft());
            fileTaskResult.setDestFileName(dirAndFileName.getRight());
        } else {
            fileTaskResult.setSourceIp(cloudIp.getIp());
            fileTaskResult.setSourceCloudId(cloudIp.getBkCloudId());
            String sourceFilePath = parseFilePathFromKey(taskProps);
            Pair<String, String> dirAndFileName = FilePathUtils.parseDirAndFileName(sourceFilePath);
            fileTaskResult.setSrcDirPath(dirAndFileName.getLeft());
            fileTaskResult.setSrcFileName(dirAndFileName.getRight());
        }
        return copyFileRsp;
    }

    private String parseFilePathFromKey(String[] taskProps) {
        String filePath = taskProps[taskProps.length - 3];
        if (taskProps.length > 4 && taskProps[taskProps.length - 4] != null) {
            // 如果是正则的文件， /tmp/REGEX:abc.*.txt 这种有:，在key中会被分开，要拼回去
            // GSE 的Redis Key问题 可能引入空格变=号，导致key被当成key=value, 所以要判断 taskProps.length > 4
            // Windows路径包含:
            if (taskProps[taskProps.length - 4].endsWith("REGEX")
                || WindowsHelper.isWindowsDiskPartition(taskProps[taskProps.length - 4])) {
                filePath = taskProps[taskProps.length - 4] + ":" + filePath;
            }
        }
        return filePath;
    }


    private HostDTO parseCloudIpFromKey(String[] taskProps) {
        String ip = taskProps[taskProps.length - 1];
        long cloudAreaId = Long.parseLong(taskProps[taskProps.length - 2].trim());
        return new HostDTO(cloudAreaId, ip);
    }

    private FileDistModeEnum parseFileTaskModeFromKey(boolean isGseAgentLog, String[] taskProps) {
        FileDistModeEnum fileDistMode;
        if (isGseAgentLog) {
            fileDistMode = taskProps[0].equals(FILE_TASK_MODE_DOWNLOAD) ? FileDistModeEnum.DOWNLOAD :
                FileDistModeEnum.UPLOAD;
        } else {
            HostDTO cloudIp = parseCloudIpFromKey(taskProps);
            // GSE Task Server 日志， 如果该key为目标服务器ip的日志，并且key的第一个字段如果为"-1"或者为源文件ip, 表示download日志
            String fileSourceCloudIp = this.intSourceIpMapping.get(taskProps[0]);
            if (this.targetAgentIds.contains(cloudIp.toCloudIp())
                && (taskProps[0].equals("-1")
                || (fileSourceCloudIp != null && this.sourceAgentIds.contains(fileSourceCloudIp)))) {
                fileDistMode = FileDistModeEnum.DOWNLOAD;
            } else {
                fileDistMode = FileDistModeEnum.UPLOAD;
            }
        }
        return fileDistMode;
    }

    private HostDTO parseFileSourceIpFromKey(boolean isGseAgentLog, HostDTO cloudIp, String[] taskProps) {
        HostDTO fileSourceIp = null;
        if (isGseAgentLog) {
            if (FILE_TASK_MODE_DOWNLOAD.equalsIgnoreCase(taskProps[0])) {
                // GSE BUG, download日志无法获取到源IP的云区域
                String fileSourceCloudIp = guessFileSourceCloudIp(taskProps[2]);
                if (StringUtils.isNotEmpty(fileSourceCloudIp)) {
                    fileSourceIp = HostDTO.fromCloudIp(fileSourceCloudIp);
                }
            } else {
                fileSourceIp = cloudIp;
            }
        } else {
            // GSE Task Server 日志， key的第一个字段为源IP;
            // GSE BUG, GSE Task Server 日志， key的第一个字段可能为-1
            String fileSourceCloudIp = guessFileSourceCloudIp(taskProps[0]);
            if (StringUtils.isNotEmpty(fileSourceCloudIp)) {
                fileSourceIp = HostDTO.fromCloudIp(fileSourceCloudIp);
            } else {
                if (this.sourceAgentIds.contains(cloudIp.toCloudIp())) {
                    fileSourceIp = cloudIp;
                }
            }
        }
        return fileSourceIp;
    }

    private String guessFileSourceCloudIp(String intIp) {
        // gse bug，使用int保存ip会溢出，所以必须根据当前的源ip反向推出key中源ip
        return this.intSourceIpMapping.get(intIp);
    }

    private void recordFinishedFileSourceAgentIds(CopyFileRsp copyFileRsp,
                                                  Map<Long, ServiceHostLogDTO> executionLogs,
                                                  Set<String> affectIps) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();
        String destAgentId = taskResult.getDestAgentId();
        AgentTaskDTO targetAgentTask = targetAgentTasks.get(destAgentId);
        log.info("Target agent down, sourceIp is null");
        for (String sourceAgentId : this.sourceAgentIds) {
            AgentTaskDTO sourceAgentTask = sourceAgentTaskMap.get(sourceAgentId);
            boolean isAddSuccess = addFinishedFile(false, true, destAgentId,
                GSEFileTaskResult.buildTaskId(taskResult.getMode(), sourceAgentId,
                    taskResult.getStandardSourceFilePath(),
                    destAgentId, taskResult.getStandardDestFilePath()));
            if (isAddSuccess) {
                addFileTaskLog(executionLogs,
                    new ServiceFileTaskLogDTO(
                        FileDistModeEnum.DOWNLOAD.getValue(),
                        targetAgentTask.getHostId(),
                        agentIdHostMap.get(targetAgentTask.getAgentId()).getIp(),
                        taskResult.getStandardDestFilePath(),
                        sourceAgentTask.getHostId(),
                        agentIdHostMap.get(sourceAgentTask.getAgentId()).getIp(),
                        agentIdHostMap.get(sourceAgentTask.getAgentId()).getDisplayIp(),
                        taskResult.getStandardSourceFilePath(),
                        taskResult.getStandardSourceFilePath() == null ? null :
                            sourceFileDisplayMap.get(taskResult.getStandardSourceFilePath()),
                        null,
                        FileDistStatusEnum.FAILED.getValue(),
                        FileDistStatusEnum.FAILED.getName(),
                        null,
                        null,
                        copyFileRsp.getFinalErrorMsg())
                );
                affectIps.add(sourceAgentId);
            }
        }
    }

    private void recordFinishedFile(CopyFileRsp copyFileRsp,
                                    Map<Long, ServiceHostLogDTO> executionLogs,
                                    Set<String> affectIps) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();
        String destAgentId = taskResult.getDestAgentId();
        String sourceAgentId = taskResult.getSourceAgentId();
        AgentTaskDTO destAgentTask = targetAgentTasks.get(destAgentId);
        AgentTaskDTO sourceAgentTask = sourceAgentTaskMap.get(sourceAgentId);
        boolean isAddSuccess = addFinishedFile(false, true, destAgentId,
            GSEFileTaskResult.buildTaskId(taskResult.getMode(), sourceAgentId, taskResult.getStandardSourceFilePath(),
                destAgentId, taskResult.getStandardDestFilePath()));
        if (isAddSuccess) {
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.DOWNLOAD.getValue(),
                    destAgentTask.getHostId(),
                    agentIdHostMap.get(destAgentTask.getAgentId()).getIp(),
                    taskResult.getStandardDestFilePath(),
                    sourceAgentTask.getHostId(),
                    agentIdHostMap.get(sourceAgentTask.getAgentId()).getIp(),
                    agentIdHostMap.get(sourceAgentTask.getAgentId()).getIp(),
                    taskResult.getStandardSourceFilePath(),
                    taskResult.getStandardSourceFilePath() == null ? null :
                        sourceFileDisplayMap.get(taskResult.getStandardSourceFilePath()), null,
                    FileDistStatusEnum.FAILED.getValue(),
                    FileDistStatusEnum.FAILED.getName(),
                    null,
                    null,
                    copyFileRsp.getFinalErrorMsg())
            );
            affectIps.add(sourceAgentId);
        }
    }

    private long getStartTimeOrDefault(GSEFileTaskResult content) {
        return (content != null && content.getStartTime() != null && content.getStartTime() > 0) ?
            content.getStartTime() : System.currentTimeMillis();
    }

    private long getEndTimeOrDefault(GSEFileTaskResult content) {
        return (content != null && content.getEndTime() != null && content.getEndTime() > 0) ?
            content.getEndTime() : System.currentTimeMillis();
    }

    private void dealIpTaskFail(CopyFileRsp copyFileRsp,
                                Map<Long, ServiceHostLogDTO> executionLogs,
                                boolean isDownloadLog) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();

        GSEFileTaskResult content = copyFileRsp.getGseFileTaskResult();
        long startTime = getStartTimeOrDefault(content);
        long endTime = getEndTimeOrDefault(content);
        if (isDownloadLog) {
            // 被该错误影响的目标Agent
            Set<String> affectedTargetAgentIds = new HashSet<>();
            String destAgentId = taskResult.getDestAgentId();
            String sourceAgentId = taskResult.getSourceAgentId();
            affectedTargetAgentIds.add(destAgentId);
            if (copyFileRsp.getFinalErrorCode().equals(GSECode.AGENT_DOWN) && StringUtils.isEmpty(sourceAgentId)) {
                // GSE BUG, agent异常场景需要特殊处理，此时，返回的源IP可能是-1
                // GSE Server 返回的download失败日志，在多个源文件的情况下，会丢失源ip的信息，需要job补全
                recordFinishedFileSourceAgentIds(copyFileRsp, executionLogs, affectedTargetAgentIds);
            } else {
                recordFinishedFile(copyFileRsp, executionLogs, affectedTargetAgentIds);
            }

            for (String affectAgentId : affectedTargetAgentIds) {
                if (affectAgentId.equals(destAgentId)) {
                    analyseAgentTaskResult(copyFileRsp.getFinalErrorCode(), affectAgentId, startTime, endTime, true);
                } else {
                    analyseAgentTaskResult(0, affectAgentId, startTime, endTime, true);
                }
            }
        } else {
            // 上传源IP本身处理
            String sourceAgentId = taskResult.getSourceAgentId();
            analyseAgentTaskResult(copyFileRsp.getFinalErrorCode(), sourceAgentId, startTime, endTime, false);

            // 如果上传源失败，除了影响上传，还会影响到下载的目标IP
            Set<String> affectedTargetIps = new HashSet<>();
            dealUploadFail(copyFileRsp, executionLogs, affectedTargetIps);
            for (String affectedTargetIp : affectedTargetIps) {
                analyseAgentTaskResult(0, affectedTargetIp, startTime, endTime, true);
            }
        }
    }

    /**
     * 根据copyFileRsp内容填充executionLogs与affectIps
     *
     * @param copyFileRsp            GSE响应内容
     * @param executionLogs          执行日志总Map
     * @param affectedTargetAgentIds 受影响的目标Agent集合
     */
    private void dealUploadFail(CopyFileRsp copyFileRsp, Map<Long, ServiceHostLogDTO> executionLogs,
                                Set<String> affectedTargetAgentIds) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();
        String sourceAgentId = taskResult.getSourceAgentId();
        HostDTO sourceHost = agentIdHostMap.get(sourceAgentId);
        // 记录源IP单个文件上传任务的结束状态
        addFinishedFile(false, false, taskResult.getSourceAgentId(), taskResult.getTaskId());

        String sourceFilePath = taskResult.getStandardSourceFilePath();
        String displayFilePath = sourceFileDisplayMap.get(sourceFilePath);
        boolean isLocalUploadFile = sourceFilePath.startsWith(this.localUploadDir);
        log.debug("StandardSourceFilePath: {}, localUploadDir: {}, isLocalUploadFile: {}, displayFilePath: {}",
            sourceFilePath, this.localUploadDir, isLocalUploadFile, displayFilePath);

        // 增加一条上传源失败的上传日志
        addFileTaskLog(executionLogs,
            new ServiceFileTaskLogDTO(
                FileDistModeEnum.UPLOAD.getValue(),
                null,
                null,
                null,
                sourceHost.getHostId(),
                sourceHost.getIp(),
                sourceHost.getDisplayIp(),
                sourceFilePath,
                displayFilePath,
                null,
                FileDistStatusEnum.FAILED.getValue(),
                FileDistStatusEnum.FAILED.getName(),
                null,
                null,
                copyFileRsp.getFinalErrorMsg())
        );

        // 源失败了，会影响所有目标IP对应的agent上的download任务
        for (String targetAgentId : this.targetAgentIds) {
            String destFilePath;
            HostDTO targetHost = agentIdHostMap.get(targetAgentId);
            if (isLocalUploadFile) {
                destFilePath = this.sourceDestPathMap.get(displayFilePath);
            } else {
                destFilePath =
                    this.sourceDestPathMap.get(
                        taskResult.getSourceAgentId() + ":" + taskResult.getStandardSourceFilePath());
            }
            // 记录目标IP单个文件下载任务的结束状态
            addFinishedFile(false, true, targetAgentId,
                GSEFileTaskResult.buildTaskId(taskResult.getMode(), taskResult.getSourceAgentId(),
                    taskResult.getStandardSourceFilePath(), targetAgentId, destFilePath));
            // 每个目标IP增加一条下载失败的日志到日志总Map中
            addFileTaskLog(executionLogs,
                new ServiceFileTaskLogDTO(
                    FileDistModeEnum.DOWNLOAD.getValue(),
                    targetHost.getHostId(),
                    targetHost.getIp(),
                    taskResult.getStandardDestFilePath(),
                    sourceHost.getHostId(),
                    sourceHost.getIp(),
                    sourceHost.getDisplayIp(),
                    taskResult.getStandardSourceFilePath(),
                    taskResult.getStandardSourceFilePath() == null ? null :
                        sourceFileDisplayMap.get(taskResult.getStandardSourceFilePath()),
                    null,
                    FileDistStatusEnum.FAILED.getValue(),
                    FileDistStatusEnum.FAILED.getName(),
                    null,
                    null,
                    copyFileRsp.getFinalErrorMsg())
            );
            affectedTargetAgentIds.add(targetAgentId);
        }
    }

    /**
     * 根据errorCode、fileNum、successNum更新successAgentIds状态集合与agentTask状态
     *
     * @param errorCode  GSE错误码
     * @param cloudIp    IP
     * @param fileNum    文件总数
     * @param successNum 成功分发的文件总数
     * @param isDownload 是否为下载过程
     * @param agentTask  ip对应日志
     */
    private void updateFinishedIpStatusAndLog(int errorCode,
                                              String cloudIp,
                                              int fileNum,
                                              int successNum,
                                              boolean isDownload,
                                              AgentTaskDTO agentTask) {
        boolean isTargetIp = targetAgentIds.contains(cloudIp);
        if (successNum >= fileNum) {
            // 每个文件都处理完了，才算IP完成执行
            if (isDownload && isTargetIp) {
                agentTask.setStatus(AgentTaskStatus.SUCCESS.getValue());
                this.successTargetAgentIds.add(cloudIp);
            }
        } else {
            int ipStatus = AgentTaskStatus.FAILED.getValue();
            if (errorCode != 0) {
                ipStatus = Utils.getStatusByGseErrorCode(errorCode);
                if (ipStatus < 0) {
                    ipStatus = AgentTaskStatus.FILE_ERROR_UNCLASSIFIED.getValue();
                }
            }
            agentTask.setStatus(ipStatus);
        }
    }

    /**
     * 分析日志，更新successAgentIds、notStartedAgentIds等状态集合，用于判定最终整体任务状态
     *
     * @param errorCode  GSE错误码
     * @param agentId    agentId
     * @param startTime  任务起始时间
     * @param endTime    任务终止时间
     * @param isDownload 是否为下载过程
     */
    private void analyseAgentTaskResult(int errorCode, String agentId, long startTime, long endTime,
                                        boolean isDownload) {
        int finishedNum;
        int fileNum;
        int successNum;
        boolean isTargetIp = targetAgentIds.contains(agentId);
        boolean isSourceIp = sourceAgentIds.contains(agentId);
        if (isDownload && isTargetIp) {
            finishedNum = this.finishedDownloadFileMap.get(agentId) == null ? 0 :
                this.finishedDownloadFileMap.get(agentId).size();
            fileNum = this.fileDownloadTaskNumMap.get(agentId) == null ? 0 : this.fileDownloadTaskNumMap.get(agentId);
            successNum = this.successDownloadFileMap.get(agentId) == null ? 0 :
                this.successDownloadFileMap.get(agentId).size();
        } else if (isSourceIp) {
            finishedNum = this.finishedUploadFileMap.get(agentId) == null ? 0 :
                this.finishedUploadFileMap.get(agentId).size();
            successNum = this.successUploadFileMap.get(agentId) == null ? 0 :
                this.successUploadFileMap.get(agentId).size();
            fileNum = this.fileUploadTaskNumMap.get(agentId) == null ? 0 : this.fileUploadTaskNumMap.get(agentId);
        } else {
            return;
        }

        AgentTaskDTO agentTask = getAgentTask(isDownload, agentId);
        if (finishedNum >= fileNum) {
            log.info("[{}] Ip analyse finished! ip: {}, finishedTaskNum: {}, expectedTaskNum: {}",
                stepInstanceId, agentId, finishedNum, fileNum);
            if (isDownload && isTargetIp) {
                // 更新IP统计状态集合，为agentTask设置任务起止时间
                dealAgentFinish(agentId, startTime, endTime, agentTask);
            }
            if (isSourceIp) {
                // 更新IP统计状态集合
                dealUploadIpFinished(agentId);
            }
            updateFinishedIpStatusAndLog(errorCode, agentId, fileNum, successNum, isDownload, agentTask);
        } else {
            agentTask.setStatus(AgentTaskStatus.RUNNING.getValue());
            this.notStartedTargetAgentIds.remove(agentId);
        }
    }

    private void dealUploadIpFinished(String sourceCloudIp) {
        this.runningFileSourceAgentIds.remove(sourceCloudIp);
        this.notStartedFileSourceAgentIds.remove(sourceCloudIp);
        this.analyseFinishedSourceAgentIds.add(sourceCloudIp);
    }

    /*
     * 从执行结果生成执行日志
     */
    private void parseExecutionLog(CopyFileRsp copyFileRsp, Map<Long, ServiceHostLogDTO> executionLogs) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();
        if (null != taskResult) {
            Integer mode = taskResult.getMode();
            boolean isDownloadLog = isDownloadLog(mode);
            String agentId = isDownloadLog ? taskResult.getDestAgentId() : taskResult.getSourceAgentId();
            GSECode.AtomicErrorCode errorCode = GSECode.AtomicErrorCode.getErrorCode(copyFileRsp.getFinalErrorCode());
            String key = taskResult.getTaskId();
            Integer process = processMap.computeIfAbsent(key, k -> -1);
            if (errorCode == GSECode.AtomicErrorCode.RUNNING && process.equals(taskResult.getProcess())) {
                return;
            }
            processMap.put(key, taskResult.getProcess());

            StringBuilder logContent = new StringBuilder();

            String filePath = isDownloadLog ? taskResult.getStandardDestFilePath() :
                taskResult.getStandardSourceFilePath();
            String displayFilePath = buildDisplayFilePath(isDownloadLog, filePath);
            if (filePath.endsWith("/") || filePath.endsWith("\\")) {
                // 传输的是目录，目录名以‘/’或‘\’结束
                logContent.append("Directory: ");
            } else {
                logContent.append("FileName: ");
            }
            logContent.append(displayFilePath);

            String fileSize = "--";
            String speed = "";
            String processText = "";

            if (taskResult.getSize() != null && taskResult.getSize() > 0) {
                // 兼容GSE不返回size的情况
                fileSize = GseConstants.tranByteReadable(taskResult.getSize());
                logContent.append(" FileSize: ").append(fileSize);
            }
            if (StringUtils.isNotEmpty(taskResult.getStatusDesc())) {
                logContent.append(" State: ").append(taskResult.getStatusDesc());
            }
            if (taskResult.getSpeed() != null) {
                speed = formatSpeed(taskResult.getSpeed()) + " KB/s";
                logContent.append(" Speed: ").append(speed);
            }
            if (taskResult.getProcess() != null) {
                processText = taskResult.getProcess() + "%";
                logContent.append(" Progress: ").append(processText);
            }
            if (StringUtils.isNotEmpty(taskResult.getStatusDesc())) {
                logContent.append(" StatusDesc: ").append(taskResult.getStatusDesc());
            }
            if (StringUtils.isNotBlank(copyFileRsp.getFinalErrorMsg())) {
                logContent.append(" Detail: ").append(copyFileRsp.getFinalErrorMsg());
            }
            String logContentStr = logContent.toString();

            FileDistStatusEnum status = parseFileTaskStatus(copyFileRsp, isDownloadLog);

            if (isDownloadLog) {
                HostDTO sourceHost = agentIdHostMap.get(taskResult.getSourceAgentId());
                HostDTO targetHost = agentIdHostMap.get(taskResult.getDestAgentId());
                addFileTaskLog(executionLogs,
                    new ServiceFileTaskLogDTO(
                        FileDistModeEnum.DOWNLOAD.getValue(),
                        targetHost.getHostId(),
                        targetHost.toCloudIp(),
                        taskResult.getStandardDestFilePath(),
                        sourceHost.getHostId(),
                        sourceHost.toCloudIp(),
                        sourceHost.toCloudIp(),
                        taskResult.getStandardSourceFilePath(),
                        taskResult.getStandardSourceFilePath() == null ? null :
                            sourceFileDisplayMap.get(taskResult.getStandardSourceFilePath()),
                        fileSize,
                        status.getValue(),
                        status.getName(),
                        speed,
                        processText,
                        logContentStr)
                );
            } else {
                HostDTO sourceHost = agentIdHostMap.get(taskResult.getSourceAgentId());
                addFileTaskLog(executionLogs,
                    new ServiceFileTaskLogDTO(
                        FileDistModeEnum.UPLOAD.getValue(),
                        null,
                        null,
                        null,
                        sourceHost.getHostId(),
                        sourceHost.toCloudIp(),
                        sourceHost.toCloudIp(),
                        filePath,
                        displayFilePath,
                        fileSize,
                        status.getValue(),
                        status.getName(),
                        speed,
                        processText,
                        logContentStr)
                );
            }
        }
    }

    private String buildDisplayFilePath(boolean isDownloadLog, String originFilePath) {
        String displayFilePath = originFilePath;
        if (!isDownloadLog) {
            displayFilePath = sourceFileDisplayMap.get(originFilePath);
        }
        return displayFilePath;
    }

    private FileDistStatusEnum parseFileTaskStatus(CopyFileRsp copyFileRsp, boolean isDownloadLog) {
        GSEFileTaskResult taskResult = copyFileRsp.getGseFileTaskResult();
        FileDistStatusEnum status;
        if (copyFileRsp.getFinalErrorCode().equals(GSECode.AtomicErrorCode.RUNNING.getValue())) {
            if (isDownloadLog) {
                status = FileDistStatusEnum.DOWNLOADING;
            } else {
                status = FileDistStatusEnum.UPLOADING;
            }
        } else if (copyFileRsp.getFinalErrorCode().equals(GSECode.AtomicErrorCode.FINISHED.getValue())) {
            if (taskResult.getProcess() < 100) {
                if (isDownloadLog) {
                    status = FileDistStatusEnum.DOWNLOADING;
                } else {
                    status = FileDistStatusEnum.UPLOADING;
                }
            } else {
                status = FileDistStatusEnum.FINISHED;
            }

        } else {
            status = FileDistStatusEnum.FAILED;
        }
        return status;
    }

    private boolean isDownloadLog(Integer mode) {
        return FileDistModeEnum.DOWNLOAD.getValue().equals(mode);
    }

    private String formatSpeed(int speed) {
        DecimalFormat formatter = new DecimalFormat("###,###");
        return formatter.format(speed);
    }


    private void addFileTaskLog(Map<Long, ServiceHostLogDTO> hostLogs, ServiceFileTaskLogDTO fileTaskLog) {
        Long hostId = isDownloadLog(fileTaskLog.getMode()) ? fileTaskLog.getDestHostId() :
            fileTaskLog.getSrcHostId();
        ServiceHostLogDTO hostLog = hostLogs.get(hostId);
        if (hostLog == null) {
            hostLog = new ServiceHostLogDTO();
            hostLog.setStepInstanceId(stepInstanceId);
            hostLog.setHostId(hostId);
            hostLog.setBatch(stepInstance.getBatch());
            hostLog.setExecuteCount(stepInstance.getExecuteCount());
            hostLogs.put(hostId, hostLog);
        }
        hostLog.addFileTaskLog(fileTaskLog);
    }

    private void writeFileTaskLogContent(Map<Long, ServiceHostLogDTO> executionLogs) {
        if (!executionLogs.isEmpty()) {
            logService.writeFileLogs(taskInstance.getCreateTime(), new ArrayList<>(executionLogs.values()));
        }
    }

    /**
     * 向某个IP上传/下载文件的已结束状态Map、已成功状态Map中添加记录
     *
     * @param isSuccess      单个文件上传/下载任务是否成功
     * @param isDownloadMode 是否为下载
     * @param agentId        bk_agent_id
     * @param taskId         单个文件任务唯一Key
     * @return 是否添加成功
     */
    private boolean addFinishedFile(boolean isSuccess, boolean isDownloadMode, String agentId, String taskId) {
        if (isDownloadMode) {
            return addFinishedFile(isSuccess, agentId, taskId, finishedDownloadFileMap, successDownloadFileMap);
        } else {
            return addFinishedFile(isSuccess, agentId, taskId, finishedUploadFileMap, successUploadFileMap);
        }
    }

    private boolean addFinishedFile(boolean isSuccess, String agentId, String taskId,
                                    Map<String, Set<String>> finishedFileMap,
                                    Map<String, Set<String>> successFileMap) {
        Set<String> finishedFileSet = finishedFileMap.computeIfAbsent(agentId, k -> Sets.newHashSet());
        boolean isAdd = finishedFileSet.add(taskId);

        if (isSuccess) {
            Set<String> successFileSet = successFileMap.computeIfAbsent(agentId, k -> Sets.newHashSet());
            successFileSet.add(taskId);
        }
        return isAdd;
    }

    @Override
    public boolean isFinished() {
        return !getExecuteResult().getResultCode().equals(GseTaskExecuteResult.RESULT_CODE_RUNNING);
    }

    @Override
    public ScheduleStrategy getScheduleStrategy() {
        if (scheduleStrategy == null) {
            this.scheduleStrategy = new FileTaskResultHandleScheduleStrategy();
        }
        return this.scheduleStrategy;
    }

    @Override
    public String toString() {
        if (this.taskInfo == null) {
            this.taskInfo = "FileTaskResultHandle-" + stepInstance.getTaskInstanceId() + "-" + stepInstance.getId();
        }
        return this.taskInfo;
    }
}
