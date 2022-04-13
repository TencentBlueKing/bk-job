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

import brave.Tracing;
import com.tencent.bk.gse.taskapi.api_agent;
import com.tencent.bk.gse.taskapi.api_copy_fileinfoV2;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.common.util.VariableValueResolver;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.consts.FileDirTypeConf;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.gse.GseRequestUtils;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.GseTaskResponse;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.result.FileResultHandleTask;
import com.tencent.bk.job.execute.engine.result.ResultHandleManager;
import com.tencent.bk.job.execute.engine.result.ha.ResultHandleTaskKeepaliveManager;
import com.tencent.bk.job.execute.engine.util.FilePathUtils;
import com.tencent.bk.job.execute.engine.util.IpHelper;
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
import com.tencent.bk.job.execute.service.AgentTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class FileGseTaskStartCommand extends AbstractGseTaskStartCommand {

    /**
     * 本地文件服务器 Agent ip
     */
    private String localAgentIp;
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
     * 文件源主机
     */
    private final Set<String> fileSourceIps = new HashSet<>();


    public FileGseTaskStartCommand(ResultHandleManager resultHandleManager,
                                   TaskInstanceService taskInstanceService,
                                   GseTaskService gseTaskService,
                                   AgentTaskService agentTaskService,
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
                                   ExceptionStatusManager exceptionStatusManager,
                                   GseTasksExceptionCounter gseTasksExceptionCounter,
                                   Tracing tracing,
                                   String requestId,
                                   TaskInstanceDTO taskInstance,
                                   StepInstanceDTO stepInstance,
                                   GseTaskDTO gseTask,
                                   String fileStorageRootPath) {
        super(resultHandleManager,
            taskInstanceService,
            gseTaskService,
            agentTaskService,
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
            exceptionStatusManager,
            gseTasksExceptionCounter,
            tracing,
            requestId,
            taskInstance,
            stepInstance,
            gseTask);

        this.fileStorageRootPath = fileStorageRootPath;
        this.localUploadDir = NFSUtils.getFileDir(fileStorageRootPath, FileDirTypeConf.UPLOAD_FILE_DIR);
    }

    @Override
    protected void preExecute() {
        // 设置本地文件服务器的Agent Ip
        this.localAgentIp = agentService.getLocalAgentBindIp();
        // 解析文件传输的源文件, 得到List<JobFile>
        parseSendFileList();
        resolvedTargetPathWithVariable();
        initFileSourceHosts();
        initFileSourceGseAgentTasks();
    }

    /**
     * 解析源文件
     */
    private void parseSendFileList() {
        resolveVariableForSourceFilePath(stepInstance.getFileSourceList(),
            buildStringGlobalVarKV(stepInputVariables));
        sendFiles = JobSrcFileUtils.parseSendFileList(stepInstance, localAgentIp, fileStorageRootPath);
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
     * 初始化文件源服务器主机
     */
    private void initFileSourceHosts() {
        if (sendFiles != null) {
            for (JobFile sendFile : sendFiles) {
                fileSourceIps.add(sendFile.getCloudIp());
            }
        }
    }

    /**
     * 初始化源文件服务器上传任务状态
     */
    private void initFileSourceGseAgentTasks() {
        List<AgentTaskDTO> fileSourceGseAgentTasks = new ArrayList<>();
        for (String sourceIp : fileSourceIps) {
            if (targetIps.contains(sourceIp) && agentTaskMap.get(sourceIp) != null) {
                AgentTaskDTO agentTask = agentTaskMap.get(sourceIp);
                agentTask.setSourceServer(true);
                fileSourceGseAgentTasks.add(agentTask);
            } else {
                AgentTaskDTO agentTask = new AgentTaskDTO(stepInstanceId, executeCount, batch);
                agentTask.setTargetServer(false);
                agentTask.setSourceServer(true);
                agentTask.setStatus(IpStatus.WAITING.getValue());
                agentTask.setGseTaskId(gseTask.getId());
                agentTask.setIp(sourceIp);
                agentTaskMap.put(sourceIp, agentTask);
                fileSourceGseAgentTasks.add(agentTask);
            }
        }
        if (!fileSourceGseAgentTasks.isEmpty()) {
            agentTaskService.batchSaveAgentTasks(fileSourceGseAgentTasks);
        }
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
        List<api_agent> dst = GseRequestUtils.buildAgentList(targetIps, accountInfo.getAccount(),
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
            FileDest fileDest = srcAndDestMap.get(file.getFileUniqueKey());
            api_agent src = GseRequestUtils.buildAgent(file.getCloudIp(), file.getAccount(),
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
            Map<String, ServiceIpLogDTO> logs = new HashMap<>();
            // 每个要分发的源文件一条上传日志
            for (JobFile file : sendFiles) {
                String fileSourceIp = file.isLocalUploadFile() ? IpHelper.fix1To0(localAgentIp) :
                    file.getCloudIp();
                ServiceIpLogDTO ipTaskLog = initServiceLogDTOIfAbsent(logs, stepInstanceId, executeCount, fileSourceIp);
                ipTaskLog.addFileTaskLog(new ServiceFileTaskLogDTO(FileDistModeEnum.UPLOAD.getValue(), null, null,
                    fileSourceIp, fileSourceIp, file.getStandardFilePath(), file.getDisplayFilePath(), "--",
                    FileDistStatusEnum.WAITING.getValue(), FileDistStatusEnum.WAITING.getName(), "--", "--", null));
            }
            // 每个目标IP从每个要分发的源文件下载的一条下载日志
            for (String fileTargetIp : targetIps) {
                ServiceIpLogDTO ipTaskLog = initServiceLogDTOIfAbsent(logs, stepInstanceId, executeCount, fileTargetIp);
                for (JobFile file : sendFiles) {
                    String fileSourceIp = file.isLocalUploadFile() ? IpHelper.fix1To0(localAgentIp) :
                        file.getCloudIp();
                    ipTaskLog.addFileTaskLog(new ServiceFileTaskLogDTO(FileDistModeEnum.DOWNLOAD.getValue(),
                        fileTargetIp,
                        getDestPath(file), fileSourceIp, fileSourceIp, file.getStandardFilePath(),
                        file.getDisplayFilePath(), "--",
                        FileDistStatusEnum.WAITING.getValue(), FileDistStatusEnum.WAITING.getName(), "--", "--", null));
                }
            }
            // 调用logService写入MongoDB
            writeLogs(logs);
        } catch (Throwable e) {
            log.warn("Save Initial File Task logs fail", e);
        }
    }

    private ServiceIpLogDTO initServiceLogDTOIfAbsent(Map<String, ServiceIpLogDTO> logs, long stepInstanceId,
                                                      int executeCount, String ip) {
        ServiceIpLogDTO ipTaskLog = logs.get(ip);
        if (ipTaskLog == null) {
            ipTaskLog = new ServiceIpLogDTO();
            ipTaskLog.setStepInstanceId(stepInstanceId);
            ipTaskLog.setIp(ip);
            ipTaskLog.setExecuteCount(executeCount);
            logs.put(ip, ipTaskLog);
        }
        return ipTaskLog;
    }

    private void writeLogs(Map<String, ServiceIpLogDTO> executionLogs) {
        log.debug("Write file task initial logs, executionLogs: {}", executionLogs);
        logService.writeFileLogs(taskInstance.getCreateTime(), new ArrayList<>(executionLogs.values()));
    }

    private void initSourceDestPathMap(Map<String, FileDest> srcAndDestMap) {
        srcAndDestMap.forEach((fileKey, dest) -> this.sourceDestPathMap.put(fileKey, dest.getDestPath()));
    }

    private String getDestPath(JobFile sourceFile) {
        return sourceDestPathMap.get(sourceFile.getFileUniqueKey());
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
                exceptionStatusManager,
                taskEvictPolicyExecutor,
                agentTaskService,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                agentTaskMap,
                gseTask,
                targetIps,
                sendFiles,
                fileStorageRootPath,
                sourceDestPathMap,
                sourceFileDisplayMap,
                requestId);
        resultHandleManager.handleDeliveredTask(fileResultHandleTask);
    }
}
