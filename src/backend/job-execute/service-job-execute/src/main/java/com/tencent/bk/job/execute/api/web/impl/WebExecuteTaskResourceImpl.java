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

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.InSufficientPermissionException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.FilePathValidateUtil;
import com.tencent.bk.job.common.util.check.*;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.web.WebExecuteTaskResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.constants.StepOperationEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.*;
import com.tencent.bk.job.execute.model.web.request.*;
import com.tencent.bk.job.execute.model.web.vo.*;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.*;

@RestController
@Slf4j
public class WebExecuteTaskResourceImpl implements WebExecuteTaskResource {
    private final TaskExecuteService taskExecuteService;
    private final MessageI18nService i18nService;
    private final WebAuthService webAuthService;

    @Autowired
    public WebExecuteTaskResourceImpl(TaskExecuteService taskExecuteService, MessageI18nService i18nService,
                                      WebAuthService webAuthService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
        this.webAuthService = webAuthService;
    }

    @Override
    public ServiceResponse<TaskExecuteVO> executeTask(String username, Long appId, WebTaskExecuteRequest request) {
        log.info("Execute task, request={}", request);
        if (!checkExecuteTaskRequest(request)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        List<TaskVariableDTO> executeVariableValues = buildExecuteVariables(request.getTaskVariables());

        try {
            TaskInstanceDTO taskInstanceDTO = taskExecuteService.createTaskInstanceForTask(
                TaskExecuteParam.builder().appId(appId).planId(request.getTaskId()).operator(username)
                    .executeVariableValues(executeVariableValues)
                    .startupMode(TaskStartupModeEnum.NORMAL).build());
            taskExecuteService.startTask(taskInstanceDTO.getId());

            TaskExecuteVO result = new TaskExecuteVO();
            result.setTaskInstanceId(taskInstanceDTO.getId());
            result.setName(taskInstanceDTO.getName());
            return ServiceResponse.buildSuccessResp(result);
        } catch (InSufficientPermissionException e) {
            return handleInSufficientPermissionException(e);
        } catch (ServiceException e) {
            log.warn("Fail to start task", e);
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        } catch (Exception e) {
            log.warn("Fail to start task", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.STARTUP_TASK_FAIL,
                i18nService.getI18n(String.valueOf(ErrorCode.STARTUP_TASK_FAIL)));
        }
    }

    private boolean checkExecuteTaskRequest(WebTaskExecuteRequest request) {
        if (request.getTaskId() == null || request.getTaskId() <= 0) {
            log.warn("Execute task, taskId is empty!");
            return false;
        }
        if (request.getTaskVariables() != null) {
            for (ExecuteVariableVO webTaskVariable : request.getTaskVariables()) {
                if (webTaskVariable.getId() == null || webTaskVariable.getId() <= 0) {
                    log.warn("Execute task, variable id is invalid");
                    return false;
                }
                if (webTaskVariable.getType() == null
                    || TaskVariableTypeEnum.valOf(webTaskVariable.getType()) == null) {
                    log.warn("Execute task, variable type is invalid");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ServiceResponse<TaskExecuteVO> redoTask(String username, Long appId, RedoTaskRequest request) {
        log.info("Redo task, request={}", request);
        if (!checkRedoTaskRequest(request)) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        List<TaskVariableDTO> executeVariableValues = buildExecuteVariables(request.getTaskVariables());
        try {
            TaskInstanceDTO taskInstanceDTO = taskExecuteService.createTaskInstanceForRedo(appId,
                request.getTaskInstanceId(), username, executeVariableValues);
            taskExecuteService.startTask(taskInstanceDTO.getId());

            TaskExecuteVO result = new TaskExecuteVO();
            result.setTaskInstanceId(taskInstanceDTO.getId());
            result.setName(taskInstanceDTO.getName());
            return ServiceResponse.buildSuccessResp(result);
        } catch (InSufficientPermissionException e) {
            return handleInSufficientPermissionException(e);
        } catch (ServiceException e) {
            log.warn("Fail to redo task", e);
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        } catch (Exception e) {
            log.warn("Fail to redo task", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.STARTUP_TASK_FAIL,
                i18nService.getI18n(String.valueOf(ErrorCode.STARTUP_TASK_FAIL)));
        }
    }

    private List<TaskVariableDTO> buildExecuteVariables(List<ExecuteVariableVO> webTaskVariables) {
        List<TaskVariableDTO> executeVariableValues = new ArrayList<>();
        for (ExecuteVariableVO webTaskVariable : webTaskVariables) {
            TaskVariableDTO taskVariableDTO = new TaskVariableDTO();
            taskVariableDTO.setId(webTaskVariable.getId());
            if (webTaskVariable.getType() == STRING.getType()
                || webTaskVariable.getType() == INDEX_ARRAY.getType()
                || webTaskVariable.getType() == ASSOCIATIVE_ARRAY.getType()) {
                taskVariableDTO.setValue(webTaskVariable.getValue());
            } else if (webTaskVariable.getType() == CIPHER.getType()) {
                // 如果密码类型的变量传入为空或者“******”，那么密码使用系统中保存的
                if (webTaskVariable.getValue() == null || "******".equals(webTaskVariable.getValue())) {
                    continue;
                } else {
                    taskVariableDTO.setValue(webTaskVariable.getValue());
                }
            } else if (webTaskVariable.getType() == HOST_LIST.getType()) {
                ExecuteTargetVO webServers = webTaskVariable.getTargetValue();
                ServersDTO serversDTO = convertToServersDTO(webServers);
                taskVariableDTO.setTargetServers(serversDTO);
            } else if (webTaskVariable.getType() == NAMESPACE.getType()) {
                taskVariableDTO.setValue(webTaskVariable.getValue());
            }
            executeVariableValues.add(taskVariableDTO);
        }
        return executeVariableValues;
    }

    private boolean checkRedoTaskRequest(RedoTaskRequest request) {
        if (request.getTaskInstanceId() == null || request.getTaskInstanceId() <= 0) {
            log.warn("Redo task, taskInstanceId is empty!");
            return false;
        }
        if (request.getTaskVariables() != null) {
            for (ExecuteVariableVO webTaskVariable : request.getTaskVariables()) {
                if (webTaskVariable.getId() == null || webTaskVariable.getId() <= 0) {
                    log.warn("Redo task, variable id is empty");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ServiceResponse<StepExecuteVO> fastExecuteScript(String username, Long appId,
                                                            WebFastExecuteScriptRequest request) {
        log.debug("Fast execute script, appId={}, operator={}, request={}", appId, username, request);
        if (!checkFastExecuteScriptRequest(request)) {
            log.warn("Fast execute script request is illegal!");
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }

        TaskInstanceDTO taskInstance = buildFastScriptTaskInstance(username, appId, request);
        StepInstanceDTO stepInstance = buildFastScriptStepInstance(username, appId, request);
        String decodeScriptContent = new String(Base64.decodeBase64(request.getContent()), StandardCharsets.UTF_8);
        stepInstance.setScriptContent(decodeScriptContent);

        return createAndStartFastTask(request.isRedoTask(), taskInstance, stepInstance);
    }

    private boolean checkFastExecuteScriptRequest(WebFastExecuteScriptRequest request) {
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(
                new TrimChecker(),
                new NotEmptyChecker(),
                new IlegalCharChecker(),
                new MaxLengthChecker(60)
            );
            request.setName(stringCheckHelper.checkAndGetResult(request.getName()));
        } catch (StringCheckException e) {
            log.warn("Fast execute script, taskName is invalid:", e);
            return false;
        }
        if (request.getScriptVersionId() == null && StringUtils.isBlank(request.getContent())) {
            log.warn("Fast execute script, script info is empty!");
            return false;
        }
        if (!ScriptTypeEnum.isValid(request.getScriptLanguage())) {
            log.warn("Fast execute script, script type is invalid! scriptType={}", request.getScriptLanguage());
            return false;
        }
        ExecuteTargetVO targetServers = request.getTargetServers();
        if (targetServers == null || targetServers.getHostNodeInfo() == null) {
            log.warn("Fast execute script, target server is null!");
            return false;
        }
        if (CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getIpList()) &&
            CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getTopoNodeList())
            && CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getDynamicGroupList())) {
            log.warn("Fast execute script, target server is null!");
            return false;
        }
        if (!checkIpValid(targetServers.getHostNodeInfo().getIpList())) {
            return false;
        }
        if (request.getAccount() == null || request.getAccount() < 1) {
            log.warn("Fast execute script, accountId is invalid! accountId={}", request.getAccount());
            return false;
        }
        return true;
    }

    private boolean checkIpValid(List<ExecuteHostVO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return true;
        }
        for (ExecuteHostVO host : hosts) {
            if (host.getCloudAreaInfo() == null || host.getCloudAreaInfo().getId() == null) {
                log.warn("Check host:{}, cloudAreaId is empty!", host);
                return false;
            }
            if (StringUtils.isEmpty(host.getIp())) {
                log.warn("Check host:{}, ip is empty!", host);
                return false;
            }
        }
        return true;
    }

    private TaskInstanceDTO buildFastScriptTaskInstance(String username, Long appId,
                                                        WebFastExecuteScriptRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setName(request.getName());
        taskInstance.setTaskId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setAppId(appId);
        taskInstance.setStartupMode(TaskStartupModeEnum.NORMAL.getValue());
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setCurrentStepId(0L);
        taskInstance.setDebugTask(false);
        if (request.isRedoTask()) {
            taskInstance.setId(request.getTaskInstanceId());
        }
        return taskInstance;
    }


    private StepInstanceDTO buildFastScriptStepInstance(String userName, Long appId,
                                                        WebFastExecuteScriptRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(request.getName());
        stepInstance.setStepId(-1L);
        stepInstance.setAppId(appId);
        stepInstance.setTargetServers(convertToServersDTO(request.getTargetServers()));
        if (request.getScriptLanguage().equals(ScriptTypeEnum.SQL.getValue())) {
            stepInstance.setDbAccountId(request.getAccount());
            stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SQL.getValue());
        } else {
            stepInstance.setAccountId(request.getAccount());
            stepInstance.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue());
        }
        stepInstance.setOperator(userName);
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setScriptSource(request.getScriptSource());
        stepInstance.setScriptType(request.getScriptLanguage());
        stepInstance.setScriptContent(request.getContent());
        stepInstance.setScriptId(request.getScriptId());
        stepInstance.setScriptVersionId(request.getScriptVersionId());
        stepInstance.setTimeout(request.getTimeout());
        stepInstance.setScriptParam(request.getScriptParam());
        stepInstance.setSecureParam(request.getSecureParam() != null && request.getSecureParam() == 1);
        return stepInstance;
    }

    @Override
    public ServiceResponse<StepExecuteVO> fastPushFile(String username, Long appId, WebFastPushFileRequest request) {
        log.debug("Fast send file, appId={}, operator={}, request={}", appId, username, request);
        if (!checkFastPushFileRequest(request)) {
            log.warn("Fast send file request is illegal!");
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }

        TaskInstanceDTO taskInstance = buildFastFileTaskInstance(username, appId, request);
        StepInstanceDTO stepInstance = buildFastFileStepInstance(username, appId, request);

        return createAndStartFastTask(false, taskInstance, stepInstance);
    }

    private ServiceResponse<StepExecuteVO> createAndStartFastTask(boolean isRedoTask, TaskInstanceDTO taskInstance,
                                                                  StepInstanceDTO stepInstance) {
        long taskInstanceId;
        if (!isRedoTask) {
            taskInstanceId = taskExecuteService.createTaskInstanceFast(taskInstance, stepInstance);
        } else {
            taskInstanceId = taskExecuteService.createTaskInstanceForFastTaskRedo(taskInstance, stepInstance);
        }
        taskExecuteService.startTask(taskInstanceId);
        StepExecuteVO stepExecuteVO = new StepExecuteVO();
        stepExecuteVO.setTaskInstanceId(taskInstanceId);
        stepExecuteVO.setStepInstanceId(stepInstance.getId());
        stepExecuteVO.setStepName(stepInstance.getName());
        return ServiceResponse.buildSuccessResp(stepExecuteVO);
//        try {
//        } catch (InSufficientPermissionException e) {
//            return handleInSufficientPermissionException(e);
//        } catch (ServiceException e) {
//            log.warn("Fail to start task", e);
//            return ServiceResponse.buildCommonFailResp(e, i18nService);
//        } catch (Exception e) {
//            log.warn("Fail to start task", e);
//            return ServiceResponse.buildCommonFailResp(ErrorCode.STARTUP_TASK_FAIL, i18nService);
//        }
    }

    private <T> ServiceResponse<T> handleInSufficientPermissionException(InSufficientPermissionException e) {
        AuthResult authResult = e.getAuthResult();
        log.debug("Insufficient permission, authResult: {}", authResult);
        if (StringUtils.isEmpty(authResult.getApplyUrl())) {
            authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
        }
        return ServiceResponse.buildAuthFailResp(webAuthService.toAuthResultVO(authResult));
    }

    private boolean checkFastPushFileRequest(WebFastPushFileRequest request) {
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(
                new TrimChecker(),
                new NotEmptyChecker(),
                new IlegalCharChecker(),
                new MaxLengthChecker(60)
            );
            request.setName(stringCheckHelper.checkAndGetResult(request.getName()));
        } catch (StringCheckException e) {
            log.warn("Fast execute script, taskName is invalid:", e);
            return false;
        }
        ExecuteFileDestinationInfoVO fileDestination = request.getFileDestination();
        if (fileDestination == null) {
            log.warn("Fast send file, fileDestination is null!");
            return false;
        }
        ExecuteTargetVO targetServers = fileDestination.getServer();
        if (targetServers == null || targetServers.getHostNodeInfo() == null) {
            log.warn("Fast send file, target server is null!");
            return false;
        }
        if (CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getIpList()) &&
            CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getTopoNodeList())
            && CollectionUtils.isEmpty(targetServers.getHostNodeInfo().getDynamicGroupList())) {
            log.warn("Fast send file, target server is null!");
            return false;
        }
        if (!checkIpValid(targetServers.getHostNodeInfo().getIpList())) {
            return false;
        }
        if (fileDestination.getAccountId() == null || fileDestination.getAccountId() < 1) {
            log.warn("Fast send file, accountId is invalid! accountId={}", fileDestination.getAccountId());
            return false;
        }
        if (request.getFileSourceList() == null || request.getFileSourceList().isEmpty()) {
            log.warn("Fast send file, fileSources are empty!");
            return false;
        }
        for (ExecuteFileSourceInfoVO fileSource : request.getFileSourceList()) {
            if (CollectionUtils.isEmpty(fileSource.getFileLocation())) {
                log.warn("Fast send file ,files are empty");
                return false;
            }
            if (fileSource.getFileType() == TaskFileTypeEnum.SERVER.getType()) {
                if (fileSource.getAccountId() == null || fileSource.getAccountId() < 1) {
                    log.warn("Fast send file, account is empty!");
                    return false;
                }
                for (String file : fileSource.getFileLocation()) {
                    if (!FilePathValidateUtil.validateFileSystemAbsolutePath(file)) {
                        log.warn("Fast send file, fileLocation is null or illegal!");
                        return false;
                    }
                }
            }
        }
        if (!FilePathValidateUtil.validateFileSystemAbsolutePath(fileDestination.getPath())) {
            log.warn("Fast send file, fileDestinationPath is null or illegal!");
            return false;
        }

        return true;

    }


    private TaskInstanceDTO buildFastFileTaskInstance(String username, Long appId, WebFastPushFileRequest request) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setType(TaskTypeEnum.FILE.getValue());
        taskInstance.setName(request.getName());
        taskInstance.setTaskId(-1L);
        taskInstance.setCronTaskId(-1L);
        taskInstance.setTaskTemplateId(-1L);
        taskInstance.setAppId(appId);
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setStartupMode(TaskStartupModeEnum.NORMAL.getValue());
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setCurrentStepId(0L);
        taskInstance.setDebugTask(false);
        return taskInstance;
    }


    private StepInstanceDTO buildFastFileStepInstance(String userName, Long appId, WebFastPushFileRequest request) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setName(request.getName());
        ExecuteFileDestinationInfoVO fileDestination = request.getFileDestination();
        stepInstance.setAccountId(fileDestination.getAccountId());
        stepInstance.setTargetServers(convertToServersDTO(fileDestination.getServer()));
        stepInstance.setFileTargetPath(fileDestination.getPath());
        stepInstance.setStepId(-1L);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE.getValue());
        stepInstance.setFileSourceList(convertFileSource(request.getFileSourceList()));
        stepInstance.setAppId(appId);
        stepInstance.setOperator(userName);
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        if (request.getDownloadSpeedLimit() != null && request.getDownloadSpeedLimit() > 0) {
            // MB->KB
            stepInstance.setFileDownloadSpeedLimit(request.getDownloadSpeedLimit() << 10);
        }
        if (request.getUploadSpeedLimit() != null && request.getUploadSpeedLimit() > 0) {
            // MB->KB
            stepInstance.setFileUploadSpeedLimit(request.getUploadSpeedLimit() << 10);
        }
        stepInstance.setTimeout(request.getTimeout());
        stepInstance.setFileDuplicateHandle(request.getDuplicateHandler());
        stepInstance.setNotExistPathHandler(request.getNotExistPathHandler());
        return stepInstance;
    }

    private ServersDTO convertToServersDTO(ExecuteTargetVO target) {
        if (target == null || target.getHostNodeInfo() == null) {
            return null;
        }
        ExecuteServersVO hostNode = target.getHostNodeInfo();
        ServersDTO serversDTO = new ServersDTO();
        if (CollectionUtils.isNotEmpty(hostNode.getIpList())) {
            List<IpDTO> staticIpList = new ArrayList<>();
            hostNode.getIpList().forEach(host -> staticIpList.add(new IpDTO(host.getCloudAreaInfo().getId(),
                host.getIp())));
            serversDTO.setStaticIpList(staticIpList);
        }
        if (CollectionUtils.isNotEmpty(hostNode.getDynamicGroupList())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            hostNode.getDynamicGroupList().forEach(
                groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
            serversDTO.setDynamicServerGroups(dynamicServerGroups);
        }
        if (CollectionUtils.isNotEmpty(hostNode.getTopoNodeList())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            hostNode.getTopoNodeList().forEach(
                topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(), topoNode.getType())));
            serversDTO.setTopoNodes(topoNodes);
        }
        return serversDTO;
    }

    private List<FileSourceDTO> convertFileSource(List<ExecuteFileSourceInfoVO> fileSources) {
        if (fileSources == null) {
            return null;
        }
        List<FileSourceDTO> fileSourceDTOS = new ArrayList<>();
        fileSources.forEach(fileSource -> {
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            fileSourceDTO.setAccountId(fileSource.getAccountId());
            fileSourceDTO.setLocalUpload(TaskFileTypeEnum.LOCAL.getType() == fileSource.getFileType());
            fileSourceDTO.setFileType(fileSource.getFileType());
            fileSourceDTO.setFileSourceId(fileSource.getFileSourceId());
            List<FileDetailDTO> files = new ArrayList<>();
            if (fileSource.getFileLocation() != null) {
                for (String file : fileSource.getFileLocation()) {
                    if (TaskFileTypeEnum.LOCAL.getType() == fileSource.getFileType()) {
                        files.add(new FileDetailDTO(true, file, fileSource.getFileHash(),
                            Long.valueOf(fileSource.getFileSize())));
                    } else {
                        // 服务器文件与文件源文件都只用路径
                        files.add(new FileDetailDTO(file));
                    }
                }
            }
            fileSourceDTO.setFiles(files);
            fileSourceDTO.setServers(convertToServersDTO(fileSource.getHost()));
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }

    @Override
    public ServiceResponse<StepOperationVO> doStepOperation(String username, Long appId, Long stepInstanceId,
                                                            WebStepOperation operation) {
        StepOperationEnum stepOperationEnum = StepOperationEnum.getStepOperation(operation.getOperationCode());
        if (stepOperationEnum == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        try {
            StepOperationDTO stepOperation = new StepOperationDTO();
            stepOperation.setStepInstanceId(stepInstanceId);
            stepOperation.setOperation(stepOperationEnum);
            stepOperation.setConfirmReason(operation.getConfirmReason());
            int executeCount = taskExecuteService.doStepOperation(appId, username, stepOperation);
            StepOperationVO stepOperationVO = new StepOperationVO(stepInstanceId, executeCount);
            return ServiceResponse.buildSuccessResp(stepOperationVO);
        } catch (ServiceException e) {
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        }
    }

    @Override
    public ServiceResponse terminateJob(String username, Long appId, Long taskInstanceId) {
        if (taskInstanceId == null) {
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        try {
            taskExecuteService.terminateJob(username, appId, taskInstanceId);
        } catch (ServiceException e) {
            log.warn("Terminate job fail, username={}, appId={}, taskInstanceId={}, errorCode={}", username, appId,
                taskInstanceId, e.getErrorCode());
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        }
        return ServiceResponse.buildSuccessResp(null);
    }
}
