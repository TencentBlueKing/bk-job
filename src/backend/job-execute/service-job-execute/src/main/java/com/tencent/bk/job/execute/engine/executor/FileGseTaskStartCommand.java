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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.gse.v2.model.FileTransferTask;
import com.tencent.bk.job.common.gse.v2.model.GseTaskResponse;
import com.tencent.bk.job.common.gse.v2.model.SourceFile;
import com.tencent.bk.job.common.gse.v2.model.TargetFile;
import com.tencent.bk.job.common.gse.v2.model.TransferFileRequest;
import com.tencent.bk.job.common.util.CollectionUtil;
import com.tencent.bk.job.common.util.DataSizeConverter;
import com.tencent.bk.job.common.util.FilePathUtils;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.cache.CustomPasswordCache;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.util.VariableValueResolver;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.FileDest;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.engine.result.FileResultHandleTask;
import com.tencent.bk.job.execute.engine.util.JobSrcFileUtils;
import com.tencent.bk.job.execute.engine.util.MacroUtil;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.rolling.StepInstanceFileBatchService;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class FileGseTaskStartCommand extends AbstractGseTaskStartCommand {

    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceFileBatchService stepInstanceFileBatchService;
    /**
     * 待分发文件，文件传输的源文件
     */
    private Set<JobFile> srcFiles;
    /**
     * 所有的源文件，包含非法的
     */
    private Set<JobFile> allSrcFiles;
    /**
     * 本地文件的存储根目录
     */
    private final String fileStorageRootPath;

    /**
     * 文件源-GSE任务与JOB执行对象任务的映射关系
     */
    protected Map<ExecuteObjectGseKey, ExecuteObjectTask> sourceExecuteObjectTaskMap = new HashMap<>();
    /**
     * 源文件与目标文件路径映射关系
     */
    private final Map<JobFile, FileDest> srcDestFileMap = new HashMap<>();
    /**
     * 源文件与目标文件路径映射关系, 包含非法主机
     */
    private Map<JobFile, FileDest> allSrcDestFileMap;
    /**
     * 源执行对象任务列表(全量，包含非法的任务)
     */
    protected List<ExecuteObjectTask> sourceExecuteObjectTasks;

    /**
     * 当前步骤是否为源文件滚动步骤
     */
    private final boolean isFileSourceRollingStep;


    public FileGseTaskStartCommand(EngineDependentServiceHolder engineDependentServiceHolder,
                                   FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                   StepInstanceFileBatchService stepInstanceFileBatchService,
                                   JobExecuteConfig jobExecuteConfig,
                                   String requestId,
                                   TaskInstanceDTO taskInstance,
                                   StepInstanceDTO stepInstance,
                                   GseTaskDTO gseTask,
                                   String fileStorageRootPath,
                                   CustomPasswordCache customPasswordCache) {
        super(engineDependentServiceHolder,
            fileExecuteObjectTaskService,
            jobExecuteConfig,
            requestId,
            taskInstance,
            stepInstance,
            gseTask,
                customPasswordCache);
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceFileBatchService = stepInstanceFileBatchService;
        this.fileStorageRootPath = fileStorageRootPath;
        this.isFileSourceRollingStep = checkIfFileSourceRollingStep();
    }

    /**
     * 判断当前步骤是否为源文件滚动步骤
     *
     * @return 布尔值
     */
    private boolean checkIfFileSourceRollingStep() {
        if (!stepInstance.isRollingStep()) {
            return false;
        }
        RollingConfigDTO rollingConfigDTO = rollingConfigService.getRollingConfig(
            taskInstanceId,
            stepInstance.getRollingConfigId()
        );
        return rollingConfigDTO.isFileSourceRolling();
    }

    @Override
    protected void initExecutionContext() {
        super.initExecutionContext();
        // 解析文件源
        resolveFileSource();
        // 解析文件传输的源文件, 得到List<JobFile>
        parseSrcFiles();
        // 解析目标路径中的变量
        resolvedTargetPathWithVariable();
        // 解析源<->目标文件映射
        parseSrcDestFileMap();
        // 初始化执行对象任务
        initFileSourceExecuteObjectTasks();
        // 保存文件子任务的初始状态
        saveInitialFileTaskLogs();
    }

    private void parseSrcDestFileMap() {
        // 路径中变量解析与路径标准化预处理
        String targetDir = FilePathUtils.standardizedDirPath(stepInstance.getResolvedFileTargetPath());
        // 构造源路径与目标路径映射
        allSrcDestFileMap = JobSrcFileUtils.buildSourceDestPathMapping(allSrcFiles, targetDir,
            stepInstance.getFileTargetName());
        allSrcDestFileMap.forEach((sreFile, destFile) -> {
            if (sreFile.getExecuteObject().isExecutable()) {
                srcDestFileMap.put(sreFile, destFile);
            }
        });
    }

    /**
     * 解析文件源
     */
    private void resolveFileSource() {
        if (isFileSourceRollingStep) {
            // 解析一个滚动批次的源文件信息
            resolveFileSourceForStepBatch();
        } else {
            // 解析整个步骤的源文件信息
            resolveFileSourceForStep();
        }
    }

    /**
     * 解析整个步骤的源文件信息
     */
    private void resolveFileSourceForStep() {
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return;
        }
        // 解析源文件路径中的全局变量
        resolveVariableForSourceFilePath(fileSourceList, buildStringGlobalVarKV(stepInputVariables));
        // 更新源文件信息到DB
        stepInstanceService.updateResolvedSourceFile(
            stepInstance.getTaskInstanceId(),
            stepInstance.getId(),
            stepInstance.getFileSourceList()
        );
    }

    /**
     * 解析步骤当前滚动批次的源文件信息
     */
    private void resolveFileSourceForStepBatch() {
        List<FileSourceDTO> fileSourceList = getFileSourceListForBatch();
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return;
        }
        // 解析源文件路径中的全局变量
        resolveVariableForSourceFilePath(fileSourceList, buildStringGlobalVarKV(stepInputVariables));
        // 更新源文件信息到DB
        stepInstanceFileBatchService.updateResolvedSourceFile(
            stepInstance.getTaskInstanceId(),
            stepInstance.getId(),
            batch,
            fileSourceList
        );
    }

    /**
     * 解析源文件
     */
    private void parseSrcFiles() {
        List<FileSourceDTO> batchFileSourceList = getFileSourceListForBatch();
        allSrcFiles = JobSrcFileUtils.parseSrcFilesFromFileSource(
            stepInstance,
            batchFileSourceList,
            fileStorageRootPath
        );
        srcFiles = allSrcFiles.stream()
            .filter(file -> file.getExecuteObject().isExecutable())
            .collect(Collectors.toSet());
        // 设置源文件所在主机账号信息
        setAccountInfoForSourceFiles(srcFiles);
    }

    /**
     * 获取当前滚动批次需要传输的源文件信息
     *
     * @return 源文件信息
     */
    private List<FileSourceDTO> getFileSourceListForBatch() {
        if (isFileSourceRollingStep) {
            // 按源文件滚动
            StepInstanceFileBatchDTO stepInstanceFileBatch = stepInstanceFileBatchService.get(
                taskInstanceId,
                stepInstanceId,
                batch
            );
            if (stepInstanceFileBatch == null) {
                // 该滚动批次没有对应的源文件信息
                throw new InternalException(
                    "stepInstanceFileBatch is null, stepInstanceId=" + stepInstanceId + ", batch=" + batch
                );
            }
            return stepInstanceFileBatch.getFileSourceList();
        }
        // 其他滚动类型
        return stepInstance.getFileSourceList();
    }

    private void setAccountInfoForSourceFiles(Set<JobFile> sendFiles) {
        Map<String, AccountDTO> accounts = new HashMap<>();
        sendFiles.forEach(sendFile -> {
            String accountKey = sendFile.getAccountId() == null ? ("id_null")
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

    /**
     * 解析源文件路径中的变量，设置到resolvedFilePath字段中
     *
     * @param fileSources                     源文件信息
     * @param stepInputGlobalVariableValueMap 步骤输入全局变量表
     */
    private void resolveVariableForSourceFilePath(List<FileSourceDTO> fileSources,
                                                  Map<String, String> stepInputGlobalVariableValueMap) {
        if (stepInputGlobalVariableValueMap == null || stepInputGlobalVariableValueMap.isEmpty()) {
            return;
        }
        for (FileSourceDTO fileSource : fileSources) {
            if (CollectionUtils.isNotEmpty(fileSource.getFiles())) {
                for (FileDetailDTO file : fileSource.getFiles()) {
                    String resolvedFilePath = VariableValueResolver.resolve(file.getFilePath(),
                        stepInputGlobalVariableValueMap);
                    if (!resolvedFilePath.equals(file.getFilePath())) {
                        file.setResolvedFilePath(resolvedFilePath);
                    }
                }
            }
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
            stepInstanceService.updateResolvedTargetPath(stepInstance.getTaskInstanceId(),
                stepInstance.getId(), resolvedTargetPath);
        }
    }

    /**
     * 初始化源文件上传任务状态
     */
    private void initFileSourceExecuteObjectTasks() {
        Set<ExecuteObject> sourceExecuteObjects = new HashSet<>();
        if (allSrcFiles != null) {
            for (JobFile sendFile : allSrcFiles) {
                if (sendFile.getExecuteObject() != null) {
                    sourceExecuteObjects.add(sendFile.getExecuteObject());
                }
            }
        }
        sourceExecuteObjectTasks = new ArrayList<>();
        for (ExecuteObject sourceExecuteObject : sourceExecuteObjects) {
            ExecuteObjectTask executeObjectTask = new ExecuteObjectTask(
                taskInstanceId,
                stepInstanceId,
                executeCount,
                batch,
                sourceExecuteObject
            );
            executeObjectTask.setActualExecuteCount(executeCount);
            executeObjectTask.setFileTaskMode(FileTaskModeEnum.UPLOAD);
            executeObjectTask.setGseTaskId(gseTask.getId());

            if (sourceExecuteObject.isAgentIdEmpty()) {
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.AGENT_NOT_INSTALLED);
            } else if (sourceExecuteObject.isInvalid()) {
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.INVALID_EXECUTE_OBJECT);
            } else {
                executeObjectTask.setStatus(ExecuteObjectTaskStatusEnum.WAITING);
                sourceExecuteObjectTaskMap.put(sourceExecuteObject.toExecuteObjectGseKey(), executeObjectTask);
            }

            sourceExecuteObjectTasks.add(executeObjectTask);
        }
        fileExecuteObjectTaskService.batchSaveTasks(sourceExecuteObjectTasks);
    }

    @Override
    protected GseTaskResponse startGseTask() {
        TransferFileRequest request = new TransferFileRequest();

        // 账号信息查询与填充
        AccountDTO accountInfo = accountService.getAccount(stepInstance.getAccountId(), AccountCategoryEnum.SYSTEM,
            stepInstance.getAccountAlias(), stepInstance.getAppId());
        if (accountInfo == null) {
            log.error("Start gse task fail, account is null!");
            return GseTaskResponse.fail(GseTaskResponse.ERROR_CODE_FAIL, "account is empty");
        }

        List<Agent> targetAgents = gseClient.fillAgentAuthInfo(
            targetExecuteObjectTaskMap.values().stream()
                .map(executeObjectTask -> executeObjectTask.getExecuteObject().toGseAgent())
                .collect(Collectors.toList()),
            accountInfo.getAccount(),
            accountInfo.getPassword());
        // 构造GSE文件分发请求
        for (JobFile file : srcFiles) {
            Agent srcAgent = buildAgent(file.getExecuteObject(), file.getAccount(), file.getPassword());
            SourceFile sourceFile = new SourceFile(file.getFileName(), file.getDir(), srcAgent);

            FileDest fileDest = srcDestFileMap.get(file);
            TargetFile targetFile = new TargetFile(fileDest.getDestName(), fileDest.getDestDirPath(), targetAgents);

            FileTransferTask fileTask = new FileTransferTask(sourceFile, targetFile);
            request.addFileTask(fileTask);
        }

        // 设置文件路径不存在的处理方式
        if (stepInstance.getNotExistPathHandler() == NotExistPathHandlerEnum.CREATE_DIR.getValue()) {
            // 直接创建
            request.setAutoMkdir(true);
        } else if (stepInstance.getNotExistPathHandler() == NotExistPathHandlerEnum.STEP_FAIL.getValue()) {
            // 直接失败
            request.setAutoMkdir(false);
        }

        setSpeedLimit(request, stepInstance);
        request.setTimeout(stepInstance.getTimeout());

        return gseClient.asyncTransferFile(request);
    }

    private Agent buildAgent(ExecuteObject executeObject, String user, String password) {
        Agent agent = executeObject.toGseAgent();
        agent.setUser(user);
        agent.setPwd(password);
        return agent;
    }

    private void setSpeedLimit(TransferFileRequest request, StepInstanceDTO stepInstance) {
        if (stepInstance.getFileDownloadSpeedLimit() != null && stepInstance.getFileDownloadSpeedLimit() > 0) {
            // KB -> MB
            request.setDownloadSpeed(DataSizeConverter.convertKBToMB(stepInstance.getFileDownloadSpeedLimit()));
        } else {
            request.setDownloadSpeed(0);
        }
        if (stepInstance.getFileUploadSpeedLimit() != null && stepInstance.getFileUploadSpeedLimit() > 0) {
            // KB -> MB
            request.setUploadSpeed(DataSizeConverter.convertKBToMB(stepInstance.getFileUploadSpeedLimit()));
        } else {
            request.setUploadSpeed(0);
        }
    }

    /**
     * 保存文件执行日志初始状态
     */
    private void saveInitialFileTaskLogs() {
        try {
            Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> logs = new HashMap<>();
            addInitialFileUploadTaskLogs(logs);
            addInitialFileDownloadTaskLogs(logs);
            // 调用logService写入MongoDB
            writeLogs(logs);
        } catch (Throwable e) {
            log.warn("Save Initial File Task logs fail", e);
        }
    }

    private void addInitialFileUploadTaskLogs(Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> logs) {
        // 每个要分发的源文件一条上传日志
        for (JobFile file : allSrcFiles) {
            boolean isSourceValid = !file.getExecuteObject().isInvalid();
            boolean isSourceAgentInstalled = !file.getExecuteObject().isAgentIdEmpty();
            FileDistStatusEnum status = isSourceValid && isSourceAgentInstalled ?
                FileDistStatusEnum.WAITING : FileDistStatusEnum.FAILED;
            logService.addFileTaskLog(
                stepInstance,
                logs,
                file.getExecuteObject(),
                logService.buildUploadServiceFileTaskLogDTO(
                    stepInstance,
                    file,
                    status,
                    "--",
                    "--",
                    "--",
                    buildInitialFileTaskUploadLogContent(isSourceValid, isSourceAgentInstalled)
                )
            );
        }
    }

    private String buildInitialFileTaskUploadLogContent(boolean isSourceValid,
                                                        boolean isSourceAgentInstalled) {
        if (!isSourceValid) {
            return "Source execute object is invalid, please check whether the host belongs to the current business";
        } else if (!isSourceAgentInstalled) {
            return "Agent is not installed";
        } else {
            // 源、目标正常，无需写入错误日志
            return null;
        }
    }

    private void addInitialFileDownloadTaskLogs(Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> logs) {
        // 每个目标IP从每个要分发的源文件下载的一条下载日志
        targetExecuteObjectTasks
            .forEach(targetExecuteObjectTask -> {
                boolean isTargetValid = !targetExecuteObjectTask.getExecuteObject().isInvalid();
                boolean isTargetAgentInstalled = !targetExecuteObjectTask.getExecuteObject().isAgentIdEmpty();
                for (JobFile file : allSrcFiles) {
                    boolean isSourceValid = !file.getExecuteObject().isInvalid();
                    boolean isSourceAgentInstalled = !file.getExecuteObject().isAgentIdEmpty();
                    FileDistStatusEnum status = isTargetValid && isTargetAgentInstalled && isSourceValid
                        && isSourceAgentInstalled ? FileDistStatusEnum.WAITING : FileDistStatusEnum.FAILED;
                    logService.addFileTaskLog(
                        stepInstance,
                        logs,
                        targetExecuteObjectTask.getExecuteObject(),
                        logService.buildDownloadServiceFileTaskLogDTO(
                            stepInstance,
                            file,
                            targetExecuteObjectTask.getExecuteObject(),
                            getDestPath(file),
                            status,
                            "--",
                            "--",
                            "--",
                            buildInitialFileTaskDownloadLogContent(
                                isTargetValid,
                                isTargetAgentInstalled,
                                isSourceValid,
                                isSourceAgentInstalled
                            )
                        )
                    );
                }
            });
    }

    private String buildInitialFileTaskDownloadLogContent(boolean isTargetValid,
                                                          boolean isTargetAgentInstalled,
                                                          boolean isSourceValid,
                                                          boolean isSourceAgentInstalled) {
        if (!isTargetValid) {
            return "Target execute object is invalid, please check whether the host belongs to the current business";
        } else if (!isTargetAgentInstalled) {
            return "Agent is not installed";
        } else if (!isSourceValid) {
            return "Source execute object is invalid, please check whether the host belongs to the current business";
        } else if (!isSourceAgentInstalled) {
            return "Source agent is not installed";
        } else {
            // 源、目标正常，无需错误日志
            return null;
        }
    }

    private void writeLogs(Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executionLogs) {
        if (log.isDebugEnabled()) {
            log.debug("Write file task initial logs, executionLogs: {}", executionLogs);
        }
        logService.writeFileLogs(taskInstance, new ArrayList<>(executionLogs.values()));
    }


    private String getDestPath(JobFile sourceFile) {
        return allSrcDestFileMap.get(sourceFile).getDestPath();
    }

    @Override
    protected void handleStartGseTaskError(GseTaskResponse gseTaskResponse) {
        updateGseTaskExecutionInfo(null, RunStatusEnum.FAIL, DateUtils.currentTimeMillis());
    }

    @Override
    protected void addResultHandleTask() {
        FileResultHandleTask fileResultHandleTask =
            new FileResultHandleTask(
                engineDependentServiceHolder,
                fileExecuteObjectTaskService,
                taskInstance,
                stepInstance,
                taskVariablesAnalyzeResult,
                targetExecuteObjectTaskMap,
                sourceExecuteObjectTaskMap,
                gseTask,
                srcDestFileMap,
                requestId,
                CollectionUtil.mergeToArrayList(targetExecuteObjectTasks, sourceExecuteObjectTasks));
        resultHandleManager.handleDeliveredTask(fileResultHandleTask);
    }


    @Override
    protected boolean checkGseTaskExecutable() {
        if (this.targetExecuteObjectTaskMap.isEmpty()) {
            log.warn("File gse task target agent is empty, can not execute! Set gse task status fail");
            return false;
        }
        if (this.sourceExecuteObjectTaskMap.isEmpty()) {
            log.warn("File gse task source agent is empty, can not execute! Set gse task status fail");
            return false;
        }
        return true;
    }
}
