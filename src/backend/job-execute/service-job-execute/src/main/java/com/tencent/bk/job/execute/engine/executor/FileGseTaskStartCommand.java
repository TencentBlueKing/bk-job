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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_copy_fileinfoV2;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.common.util.VariableValueResolver;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.result.FileResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.FilePathUtils;
import com.tencent.bk.job.execute.engine.util.JobSrcFileUtils;
import com.tencent.bk.job.execute.engine.util.MacroUtil;
import com.tencent.bk.job.execute.engine.util.NFSUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.monitor.metrics.ExecuteMonitor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.FileAgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.sleuth.Tracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class FileGseTaskStartCommand extends AbstractGseTaskStartCommand {

    /**
     * 本地文件服务器 Agent
     */
    private HostDTO localAgentHost;
    /**
     * 待分发文件，文件传输的源文件
     */
    private Set<JobFile> sendFiles;
    /**
     * 本地文件的存储根目录
     */
    private final String fileStorageRootPath;
    /**
     * 源文件路径与目标路径映射关系
     * 格式： Map<源IP:源文件路径，目标路径>
     */
    private final Map<String, String> sourceDestPathMap = new HashMap<>();
    /**
     * 源文件原始文件路径与展示文件路径的映射
     */
    private Map<String, String> sourceFileDisplayMap = new HashMap<>();
    /**
     * 本地文件存储目录
     */
    private final String localUploadDir;
    /**
     * GSE 源 Agent 任务, Map<AgentId,AgentTask>
     */
    protected Map<String, AgentTaskDTO> sourceAgentTaskMap = new HashMap<>();

    private final FileAgentTaskService fileAgentTaskService;


    public FileGseTaskStartCommand(ResultHandleManager resultHandleManager,
                                   TaskInstanceService taskInstanceService,
                                   GseTaskService gseTaskService,
                                   FileAgentTaskService fileAgentTaskService,
                                   AccountService accountService,
                                   TaskInstanceVariableService taskInstanceVariableService,
                                   StepInstanceVariableValueService stepInstanceVariableValueService,
                                   AgentService agentService,
                                   LogService logService,
                                   TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                   ResultHandleTaskKeepaliveManager resultHandleTaskKeepaliveManager,
                                   ExecuteMonitor executeMonitor,
                                   JobExecuteConfig jobExecuteConfig,
                                   TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                   GseTasksExceptionCounter gseTasksExceptionCounter,
                                   StepInstanceService stepInstanceService,
                                   Tracer tracer,
                                   String requestId,
                                   TaskInstanceDTO taskInstance,
                                   StepInstanceDTO stepInstance,
                                   GseTaskDTO gseTask,
                                   String fileStorageRootPath) {
        super(resultHandleManager,
            taskInstanceService,
            gseTaskService,
            fileAgentTaskService,
            accountService,
            taskInstanceVariableService,
            stepInstanceVariableValueService,
            agentService,
            logService,
            taskExecuteMQEventDispatcher,
            resultHandleTaskKeepaliveManager,
            executeMonitor,
            jobExecuteConfig,
            taskEvictPolicyExecutor,
            gseTasksExceptionCounter,
            tracer,
            requestId,
            taskInstance,
            stepInstance,
            gseTask,
            stepInstanceService);

        this.fileAgentTaskService = fileAgentTaskService;
        this.fileStorageRootPath = fileStorageRootPath;
        this.localUploadDir = NFSUtils.getFileDir(fileStorageRootPath, FileDirTypeConf.UPLOAD_FILE_DIR);
    }

    @Override
    protected void preExecute() {
        // 设置本地文件服务器的Agent Ip
        this.localAgentHost = agentService.getLocalAgentHost();
        // 解析文件传输的源文件, 得到List<JobFile>
        parseSendFileList();
        resolvedTargetPathWithVariable();
        initFileSourceGseAgentTasks();
    }

    /**
     * 解析源文件
     */
    private void parseSendFileList() {
        resolveVariableForSourceFilePath(stepInstance.getFileSourceList(),
            buildStringGlobalVarKV(stepInputVariables));
        sendFiles = JobSrcFileUtils.parseSendFileList(stepInstance, localAgentHost, fileStorageRootPath);
        setAccountInfoForSourceFiles(sendFiles);
        // 初始化显示名称映射Map
        sourceFileDisplayMap = JobSrcFileUtils.buildSourceFileDisplayMapping(sendFiles, localUploadDir);
    }

    private void setAccountInfoForSourceFiles(Set<JobFile> sendFiles) {
        Map<String, AccountDTO> accounts = new HashMap<>();
        sendFiles.forEach(sendFile -> {
            String accountKey = sendFile.getAccountId() == null ? ("id_" + sendFile.getAccountId())
                : ("alias_" + sendFile.getAccountAlias());
            AccountDTO account = accounts.computeIfAbsent(accountKey,
                k -> accountService.getAccount(sendFile.getAccountId(), AccountCategoryEnum.SYSTEM,
                    sendFile.getAccountAlias(), stepInstance.getAppId()));
            if (account != null) {
                sendFile.setAccountId(account.getId());
                sendFile.setAccount(account.getAccount());
                sendFile.setAccountAlias(account.getAlias());
                sendFile.setPassword(account.getPassword());
            }
        });
    }

    private void resolveVariableForSourceFilePath(List<FileSourceDTO> fileSources,
                                                  Map<String, String> stepInputGlobalVariableValueMap) {
        if (stepInputGlobalVariableValueMap == null || stepInputGlobalVariableValueMap.isEmpty()) {
            return;
        }
        boolean isContainsVar = false;
        for (FileSourceDTO fileSource : fileSources) {
            if (CollectionUtils.isNotEmpty(fileSource.getFiles())) {
                for (FileDetailDTO file : fileSource.getFiles()) {
                    String resolvedFilePath = VariableValueResolver.resolve(file.getFilePath(),
                        stepInputGlobalVariableValueMap);
                    if (!resolvedFilePath.equals(file.getFilePath())) {
                        file.setResolvedFilePath(resolvedFilePath);
                        isContainsVar = true;
                    }
                }
            }
        }
        if (isContainsVar) {
            taskInstanceService.updateResolvedSourceFile(stepInstance.getId(), stepInstance.getFileSourceList());
        }
    }

    /**
     * 解析文件分发目标路径，替换变量
     */
    private void resolvedTargetPathWithVariable() {
        String resolvedTargetPath = VariableValueResolver.resolve(stepInstance.getFileTargetPath(),
            buildStringGlobalVarKV(stepInputVariables));
        resolvedTargetPath = MacroUtil.resolveDateWithStrfTime(resolvedTargetPath);
        stepInstance.setResolvedFileTargetPath(resolvedTargetPath);
        if (!resolvedTargetPath.equals(stepInstance.getFileTargetPath())) {
            taskInstanceService.updateResolvedTargetPath(stepInstance.getId(), resolvedTargetPath);
        }
    }

    /**
     * 初始化源文件服务器上传任务状态
     */
    private void initFileSourceGseAgentTasks() {
        Set<HostDTO> sourceHosts = new HashSet<>();
        if (sendFiles != null) {
            for (JobFile sendFile : sendFiles) {
                if (sendFile.getHost() != null) {
                    sourceHosts.add(sendFile.getHost());
                }
            }
        }
        List<AgentTaskDTO> fileSourceGseAgentTasks = new ArrayList<>();
        for (HostDTO sourceHost : sourceHosts) {
            AgentTaskDTO agentTask = new AgentTaskDTO(stepInstanceId, executeCount, batch, sourceHost.getHostId(),
                sourceHost.getAgentId());
            agentTask.setActualExecuteCount(executeCount);
            agentTask.setCloudIp(sourceHost.toCloudIp());
            agentTask.setDisplayIp(sourceHost.getDisplayIp());
            agentTask.setFileTaskMode(FileTaskModeEnum.UPLOAD);
            agentTask.setStatus(AgentTaskStatusEnum.WAITING);
            agentTask.setGseTaskId(gseTask.getId());
            sourceAgentTaskMap.put(sourceHost.getAgentId(), agentTask);
            fileSourceGseAgentTasks.add(agentTask);
        }
        fileAgentTaskService.batchSaveAgentTasks(fileSourceGseAgentTasks);
    }

    @Override
    protected GseTaskResponse startGseTask() {
        // 账号信息查询与填充
        AccountDTO accountInfo = accountService.getAccount(stepInstance.getAccountId(), AccountCategoryEnum.SYSTEM,
            stepInstance.getAccountAlias(), stepInstance.getAppId());
        if (accountInfo == null) {
            log.error("Start gse task fail, account is null!");
            return GseTaskResponse.fail(GseTaskResponse.ERROR_CODE_FAIL, "account is empty");
        }
        List<api_agent> dst = GseRequestUtils.buildAgentList(targetAgentTaskMap.keySet(), accountInfo.getAccount(),
            accountInfo.getPassword());

        List<api_copy_fileinfoV2> copyFileInfoList = new ArrayList<>();
        // 路径中变量解析与路径标准化预处理
        String targetDir = FilePathUtils.standardizedDirPath(stepInstance.getResolvedFileTargetPath());
        // 构造源路径与目标路径映射Map<String,FileDest>
        Map<String, FileDest> srcAndDestMap = JobSrcFileUtils.buildSourceDestPathMapping(sendFiles, targetDir,
            stepInstance.getFileTargetName());
        // 构造源路径与目标路径映射Map<String,String>供后续状态判定使用
        initSourceDestPathMap(srcAndDestMap);

        // 构造GSE文件分发请求
        for (JobFile file : sendFiles) {
            FileDest fileDest = srcAndDestMap.get(file.getUniqueKey());
            api_agent src = GseRequestUtils.buildAgent(file.getHost().toCloudIp(), file.getAccount(),
                file.getPassword());
            api_copy_fileinfoV2 copyFileInfo = GseRequestUtils.buildCopyFileInfo(src, file.getDir(), file.getFileName(),
                dst, fileDest.getDestDirPath(), fileDest.getDestName(),
                stepInstance.getFileDownloadSpeedLimit(), stepInstance.getFileUploadSpeedLimit(),
                stepInstance.getTimeout());

            // 设置文件路径不存在的处理方式
            if (stepInstance.getNotExistPathHandler() == NotExistPathHandlerEnum.CREATE_DIR.getValue()) {
                // 直接创建
                copyFileInfo.setMkdirflag(1);
            } else if (stepInstance.getNotExistPathHandler() == NotExistPathHandlerEnum.STEP_FAIL.getValue()) {
                // 直接失败
                copyFileInfo.setMkdirflag(0);
            } else {
                log.warn("NotExistPathHandler not supported：{}, supported handlers are:{}",
                    stepInstance.getNotExistPathHandler(), NotExistPathHandlerEnum.getDescStr());
            }
            copyFileInfoList.add(copyFileInfo);
        }
        if (CollectionUtils.isNotEmpty(copyFileInfoList)) {
            copyFileInfoList.get(0).setM_caller(buildGSETraceInfo());
        }
        saveInitialFileTaskLogs(sourceDestPathMap);
        return GseRequestUtils.sendCopyFileTaskRequest(stepInstanceId, copyFileInfoList);
    }

    /**
     * 保存文件执行日志初始状态
     */
    private void saveInitialFileTaskLogs(Map<String, String> sourceDestPathMap) {
        try {
            log.debug("[{}] SourceDestPathMap: {}", stepInstanceId, sourceDestPathMap);
            Map<Long, ServiceHostLogDTO> logs = new HashMap<>();
            // 每个要分发的源文件一条上传日志
            for (JobFile file : sendFiles) {
                Long sourceHostId = file.getHost().getHostId();
                ServiceHostLogDTO hostTaskLog = initServiceLogDTOIfAbsent(logs, stepInstanceId, executeCount,
                    sourceHostId, file.getHost().toCloudIp());
                hostTaskLog.addFileTaskLog(
                    new ServiceFileTaskLogDTO(
                        FileDistModeEnum.UPLOAD.getValue(),
                        null,
                        null,
                        null,
                        sourceHostId,
                        file.getHost().toCloudIp(),
                        file.getHost().toCloudIp(),
                        file.getStandardFilePath(),
                        file.getDisplayFilePath(),
                        "--",
                        FileDistStatusEnum.WAITING.getValue(),
                        FileDistStatusEnum.WAITING.getName(),
                        "--",
                        "--",
                        null)
                );
            }
            // 每个目标IP从每个要分发的源文件下载的一条下载日志
            for (AgentTaskDTO targetAgentTask : targetAgentTaskMap.values()) {
                HostDTO targetHost = agentIdHostMap.get(targetAgentTask.getAgentId());
                ServiceHostLogDTO ipTaskLog = initServiceLogDTOIfAbsent(logs, stepInstanceId, executeCount,
                    targetHost.getHostId(), targetHost.toCloudIp());
                for (JobFile file : sendFiles) {
                    Long sourceHostId = file.getHost().getHostId();
                    ipTaskLog.addFileTaskLog(
                        new ServiceFileTaskLogDTO(
                            FileDistModeEnum.DOWNLOAD.getValue(),
                            targetAgentTask.getHostId(),
                            targetHost.toCloudIp(),
                            getDestPath(file),
                            sourceHostId,
                            file.getHost().toCloudIp(),
                            file.getHost().toCloudIp(),
                            file.getStandardFilePath(),
                            file.getDisplayFilePath(),
                            "--",
                            FileDistStatusEnum.WAITING.getValue(),
                            FileDistStatusEnum.WAITING.getName(),
                            "--",
                            "--",
                            null)
                    );
                }
            }
            // 调用logService写入MongoDB
            writeLogs(logs);
        } catch (Throwable e) {
            log.warn("Save Initial File Task logs fail", e);
        }
    }

    private ServiceHostLogDTO initServiceLogDTOIfAbsent(Map<Long, ServiceHostLogDTO> logs, long stepInstanceId,
                                                        int executeCount, Long hostId, String cloudIp) {
        ServiceHostLogDTO hostTaskLog = logs.get(hostId);
        if (hostTaskLog == null) {
            hostTaskLog = new ServiceHostLogDTO();
            hostTaskLog.setStepInstanceId(stepInstanceId);
            hostTaskLog.setExecuteCount(executeCount);
            hostTaskLog.setHostId(hostId);
            hostTaskLog.setIp(cloudIp);
            logs.put(hostId, hostTaskLog);
        }
        return hostTaskLog;
    }

    private void writeLogs(Map<Long, ServiceHostLogDTO> executionLogs) {
        log.debug("Write file task initial logs, executionLogs: {}", executionLogs);
        logService.writeFileLogs(taskInstance.getCreateTime(), new ArrayList<>(executionLogs.values()));
    }

    private void initSourceDestPathMap(Map<String, FileDest> srcAndDestMap) {
        srcAndDestMap.forEach((fileKey, dest) -> this.sourceDestPathMap.put(fileKey, dest.getDestPath()));
    }

    private String getDestPath(JobFile sourceFile) {
        return sourceDestPathMap.get(sourceFile.getUniqueKey());
    }

    @Override
    protected void handleStartGseTaskError(GseTaskResponse gseTaskResponse) {

    }

    @Override
    protected void addResultHandleTask() {
        FileResultHandleTask fileResultHandleTask =
            new FileResultHandleTask(taskInstanceService,
                gseTaskService,
                logService,
                taskInstanceVariableService,
                stepInstanceVariableValueService,
                taskExecuteMQEventDispatcher,
                resultHandleTaskKeepaliveManager,
                taskEvictPolicyExecutor,
                fileAgentTaskService,
                stepInstanceService,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                targetAgentTaskMap,
                sourceAgentTaskMap,
                gseTask,
                sendFiles,
                fileStorageRootPath,
                sourceDestPathMap,
                sourceFileDisplayMap,
                requestId);
        resultHandleManager.handleDeliveredTask(fileResultHandleTask);
    }
}
