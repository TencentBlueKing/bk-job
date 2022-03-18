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

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.execute.api.web.WebTaskInstanceResource;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.converter.TaskInstanceConverter;
import com.tencent.bk.job.execute.model.web.vo.ExecuteApprovalStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteCloudAreaInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileDestinationInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileSourceInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteHostVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteScriptStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteServersVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteTargetVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceDetailVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.model.web.vo.TaskOperationLogVO;
import com.tencent.bk.job.execute.service.ServerService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class WebTaskInstanceResourceImpl implements WebTaskInstanceResource {
    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final ServerService serverService;
    private final TaskOperationLogService taskOperationLogService;
    private final MessageI18nService i18nService;
    private final ExecuteAuthService executeAuthService;
    private final AuthService authService;

    @Autowired
    public WebTaskInstanceResourceImpl(TaskInstanceService taskInstanceService,
                                       TaskInstanceVariableService taskInstanceVariableService,
                                       ServerService serverService,
                                       TaskOperationLogService taskOperationLogService,
                                       MessageI18nService i18nService,
                                       ExecuteAuthService executeAuthService,
                                       AuthService authService) {
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.serverService = serverService;
        this.taskOperationLogService = taskOperationLogService;
        this.i18nService = i18nService;
        this.executeAuthService = executeAuthService;
        this.authService = authService;
    }

    @Override
    public Response<ExecuteStepVO> getStepInstanceDetail(String username,
                                                         AppResourceScope appResourceScope,
                                                         String scopeType,
                                                         String scopeId,
                                                         Long stepInstanceId) {

        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        if (stepInstance == null) {
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        if (!stepInstance.getAppId().equals(appResourceScope.getAppId())) {
            log.warn("StepInstance:{} is not in app:{}", stepInstanceId, appResourceScope.getAppId());
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        AuthResult authResult = authViewStepInstance(username, appResourceScope, stepInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        ExecuteStepVO stepVO = convertToStepVO(stepInstance);
        return Response.buildSuccessResp(stepVO);
    }

    private AuthResult authViewTaskInstance(String username, AppResourceScope appResourceScope,
                                            TaskInstanceDTO taskInstance) {
        return executeAuthService.authViewTaskInstance(username, appResourceScope, taskInstance);
    }

    private AuthResult authViewStepInstance(String username, AppResourceScope appResourceScope,
                                            StepInstanceDTO stepInstance) {
        String operator = stepInstance.getOperator();
        if (username.equals(operator)) {
            return AuthResult.pass();
        }
        return executeAuthService.authViewTaskInstance(
            username, appResourceScope, stepInstance.getTaskInstanceId());
    }

    private void convertFileSources(ExecuteFileStepVO fileStepVO, StepInstanceDTO stepInstance) {
        List<ExecuteFileSourceInfoVO> fileSources = new ArrayList<>();
        for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
            ExecuteFileSourceInfoVO fileSourceVO = new ExecuteFileSourceInfoVO();
            fileSourceVO.setAccountId(fileSource.getAccountId());
            fileSourceVO.setAccountName(fileSource.getAccount());
            if (fileSource.getFileType() == null) {
                fileSourceVO.setFileType(fileSource.isLocalUpload() ? TaskFileTypeEnum.LOCAL.getType() :
                    TaskFileTypeEnum.SERVER.getType());
            } else {
                fileSourceVO.setFileType(fileSource.getFileType());
            }
            if (fileSource.isLocalUpload()) {
                fileSourceVO.setFileSize(String.valueOf(fileSource.getFiles().get(0).getFileSize()));
                fileSourceVO.setFileHash(fileSource.getFiles().get(0).getFileHash());
                List<String> localUploadFiles = new ArrayList<>();
                localUploadFiles.add(fileSource.getFiles().get(0).getFilePath());
                fileSourceVO.setFileLocation(localUploadFiles);
            } else if ((fileSource.getFileSourceId() != null && fileSource.getFileSourceId() > 0)
                || (fileSource.getFileType() != null
                && fileSource.getFileType() == TaskFileTypeEnum.FILE_SOURCE.getType())) {
                List<String> files = new ArrayList<>();
                fileSource.getFiles().forEach(fileDetailDTO -> {
                    if (StringUtils.isNotEmpty(fileDetailDTO.getThirdFilePath())) {
                        files.add(fileDetailDTO.getThirdFilePath());
                    } else {
                        files.add(fileDetailDTO.getFilePath());
                    }
                });
                fileSourceVO.setFileLocation(files);
                fileSourceVO.setFileSourceId(fileSource.getFileSourceId());
            } else {
                List<String> files = new ArrayList<>();
                fileSource.getFiles().forEach(fileDetailDTO -> {
                    if (StringUtils.isNotEmpty(fileDetailDTO.getResolvedFilePath())) {
                        files.add(fileDetailDTO.getResolvedFilePath());
                    } else {
                        files.add(fileDetailDTO.getFilePath());
                    }
                });
                fileSourceVO.setFileLocation(files);
            }
            fileSourceVO.setHost(convertToServers(fileSource.getServers()));
            fileSources.add(fileSourceVO);
        }
        fileStepVO.setFileSourceList(fileSources);
    }

    private ExecuteStepVO convertToStepVO(StepInstanceDTO stepInstance) {
        ExecuteStepVO stepVO = new ExecuteStepVO();
        stepVO.setName(stepInstance.getName());
        StepExecuteTypeEnum stepType = StepExecuteTypeEnum.valueOf(stepInstance.getExecuteType());
        if (stepType == null) {
            log.warn("Invalid step type!");
            return null;
        }
        if (stepType == StepExecuteTypeEnum.EXECUTE_SCRIPT || stepType == StepExecuteTypeEnum.EXECUTE_SQL) {
            stepVO.setType(TaskStepTypeEnum.SCRIPT.getType());
            ExecuteScriptStepVO scriptStepVO = new ExecuteScriptStepVO();
            if (stepType == StepExecuteTypeEnum.EXECUTE_SCRIPT) {
                scriptStepVO.setAccountId(stepInstance.getAccountId());
                scriptStepVO.setAccountName(stepInstance.getAccount());
            } else {
                scriptStepVO.setAccountId(stepInstance.getDbAccountId());
                scriptStepVO.setAccountName(stepInstance.getDbAccount());
            }
            scriptStepVO.setContent(Base64Util.encodeContentToStr(stepInstance.getScriptContent()));
            if (stepInstance.isSecureParam()) {
                scriptStepVO.setScriptParam("******");
                scriptStepVO.setSecureParam(1);
            } else {
                if (StringUtils.isNotEmpty(stepInstance.getResolvedScriptParam())) {
                    scriptStepVO.setScriptParam(stepInstance.getResolvedScriptParam());
                } else {
                    scriptStepVO.setScriptParam(stepInstance.getScriptParam());
                }
                scriptStepVO.setSecureParam(0);
            }
            scriptStepVO.setTimeout(stepInstance.getTimeout());
            scriptStepVO.setScriptLanguage(stepInstance.getScriptType());
            scriptStepVO.setScriptSource(stepInstance.getScriptSource());
            scriptStepVO.setScriptId(stepInstance.getScriptId());
            scriptStepVO.setScriptVersionId(stepInstance.getScriptVersionId());
            scriptStepVO.setExecuteTarget(convertToServers(stepInstance.getTargetServers()));
            scriptStepVO.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
            stepVO.setScriptStepInfo(scriptStepVO);
        } else if (stepType == StepExecuteTypeEnum.SEND_FILE) {
            stepVO.setType(TaskStepTypeEnum.FILE.getType());
            ExecuteFileStepVO fileStepVO = new ExecuteFileStepVO();

            ExecuteFileDestinationInfoVO fileDestinationInfoVO = new ExecuteFileDestinationInfoVO();
            fileDestinationInfoVO.setAccountName(stepInstance.getAccount());
            fileDestinationInfoVO.setAccountId(stepInstance.getAccountId());
            if (StringUtils.isNotEmpty(stepInstance.getResolvedFileTargetPath())) {
                fileDestinationInfoVO.setPath(stepInstance.getResolvedFileTargetPath());
            } else {
                fileDestinationInfoVO.setPath(stepInstance.getFileTargetPath());
            }
            fileDestinationInfoVO.setServer(convertToServers(stepInstance.getTargetServers()));
            fileStepVO.setFileDestination(fileDestinationInfoVO);

            fileStepVO.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
            fileStepVO.setTransferMode(
                ExecuteFileStepVO.getTransferMode(DuplicateHandlerEnum.valueOf(stepInstance.getFileDuplicateHandle()),
                    NotExistPathHandlerEnum.valueOf(stepInstance.getNotExistPathHandler())));
            if (stepInstance.getFileDownloadSpeedLimit() != null) {
                fileStepVO.setTargetSpeedLimit(stepInstance.getFileDownloadSpeedLimit() >> 10);
            }
            if (stepInstance.getFileUploadSpeedLimit() != null) {
                fileStepVO.setOriginSpeedLimit(stepInstance.getFileUploadSpeedLimit() >> 10);
            }
            fileStepVO.setTimeout(stepInstance.getTimeout());

            if (CollectionUtils.isNotEmpty(stepInstance.getFileSourceList())) {
                convertFileSources(fileStepVO, stepInstance);
            }
            stepVO.setFileStepInfo(fileStepVO);
        } else if (stepType == StepExecuteTypeEnum.MANUAL_CONFIRM) {
            stepVO.setType(TaskStepTypeEnum.APPROVAL.getType());
            ExecuteApprovalStepVO approvalStepVO = new ExecuteApprovalStepVO();
            approvalStepVO.setApprovalMessage(stepInstance.getConfirmMessage());
            stepVO.setApprovalStepInfo(approvalStepVO);
        }
        return stepVO;
    }

    private ExecuteTargetVO convertToServers(ServersDTO serversDTO) {
        if (serversDTO == null) {
            return null;
        }
        ExecuteTargetVO targetServer = new ExecuteTargetVO();
        targetServer.setVariable(serversDTO.getVariable());
        ExecuteServersVO taskHostNodeVO = new ExecuteServersVO();
        if (serversDTO.getIpList() != null) {
            List<ExecuteHostVO> hosts = new ArrayList<>();
            for (IpDTO ip : serversDTO.getIpList()) {
                ExecuteHostVO host = new ExecuteHostVO();
                ExecuteCloudAreaInfoVO cloudAreaInfoVO = new ExecuteCloudAreaInfoVO(ip.getCloudAreaId(), ip.getIp());
                host.setIp(ip.getIp());
                host.setAlive(ip.getAlive());
                host.setCloudAreaInfo(cloudAreaInfoVO);
                hosts.add(host);
            }
            taskHostNodeVO.setIpList(hosts);
            targetServer.setHostNodeInfo(taskHostNodeVO);
        }
        return targetServer;
    }

    @Override
    public Response<List<ExecuteVariableVO>> getTaskInstanceVariables(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId,
                                                                      Long taskInstanceId) {

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null || !taskInstance.getAppId().equals(appResourceScope.getAppId())) {
            log.warn("TaskInstance:{} is not in app:{}", taskInstanceId, appResourceScope.getAppId());
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        AuthResult authResult = authViewTaskInstance(username, appResourceScope, taskInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<TaskVariableDTO> taskVariables = taskInstanceVariableService.getByTaskInstanceId(taskInstanceId);
        List<ExecuteVariableVO> variableVOS = new ArrayList<>();
        if (taskVariables != null) {
            taskVariables.forEach(variable -> {
                variableVOS.add(convertToVariableVO(appResourceScope.getAppId(), variable));
            });
        }
        return Response.buildSuccessResp(variableVOS);
    }

    private ExecuteVariableVO convertToVariableVO(long appId, TaskVariableDTO variable) {
        ExecuteVariableVO vo = new ExecuteVariableVO();
        vo.setId(variable.getId());
        vo.setName(variable.getName());
        vo.setType(variable.getType());
        vo.setChangeable(variable.isChangeable() ? 1 : 0);
        vo.setRequired(variable.isRequired() ? 1 : 0);
        if (variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
            ServersDTO servers = variable.getTargetServers();
            ExecuteTargetVO taskTargetVO = new ExecuteTargetVO();
            if (servers.getIpList() != null) {
                ExecuteServersVO taskHostNodeVO = new ExecuteServersVO();
                List<ExecuteHostVO> hosts = new ArrayList<>(servers.getIpList().size());
                for (IpDTO ip : servers.getIpList()) {
                    ExecuteHostVO host = new ExecuteHostVO();
                    host.setIp(ip.getIp());
                    host.setAlive(ip.getAlive());
                    ExecuteCloudAreaInfoVO cloudAreaInfoVO = new ExecuteCloudAreaInfoVO(ip.getCloudAreaId(),
                        serverService.getCloudAreaName(appId, ip.getCloudAreaId()));
                    host.setCloudAreaInfo(cloudAreaInfoVO);
                    hosts.add(host);
                }
                taskHostNodeVO.setIpList(hosts);
                taskTargetVO.setHostNodeInfo(taskHostNodeVO);
            }
            vo.setTargetValue(taskTargetVO);
        } else if (variable.getType().equals(TaskVariableTypeEnum.CIPHER.getType())) {
            vo.setValue("******");
        } else {
            vo.setValue(variable.getValue());
        }
        return vo;
    }

    @Override
    public Response<List<TaskOperationLogVO>> getTaskInstanceOperationLog(String username,
                                                                          AppResourceScope appResourceScope,
                                                                          String scopeType,
                                                                          String scopeId,
                                                                          Long taskInstanceId) {

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null || !taskInstance.getAppId().equals(appResourceScope.getAppId())) {
            log.warn("TaskInstance:{} is not in app:{}", taskInstanceId, appResourceScope.getAppId());
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        AuthResult authResult = authViewTaskInstance(username, appResourceScope, taskInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<OperationLogDTO> operationLogs = taskOperationLogService.listOperationLog(taskInstanceId);
        if (operationLogs == null || operationLogs.isEmpty()) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<TaskOperationLogVO> vos = new ArrayList<>(operationLogs.size());
        for (OperationLogDTO operationLog : operationLogs) {
            TaskOperationLogVO vo = new TaskOperationLogVO();
            vo.setId(operationLog.getId());
            vo.setTaskInstanceId(operationLog.getTaskInstanceId());
            vo.setOperator(operationLog.getOperator());
            vo.setOperationCode(operationLog.getOperationEnum().getValue());
            vo.setOperationName(i18nService.getI18n(operationLog.getOperationEnum().getI18nKey()));
            vo.setCreateTime(operationLog.getCreateTime());
            OperationLogDTO.OperationDetail detail = operationLog.getDetail();
            vo.setStepInstanceId(detail.getStepInstanceId());
            vo.setStepName(detail.getStepName());
            vo.setRetry(detail.getExecuteCount());
            vo.setDetail(buildDetail(operationLog.getOperationEnum(), operationLog.getDetail()));
            vos.add(vo);
        }
        return Response.buildSuccessResp(vos);
    }

    private String buildDetail(UserOperationEnum operationCode, OperationLogDTO.OperationDetail detail) {
        switch (operationCode) {
            case START:
                if (detail.getStartupMode().equals(TaskStartupModeEnum.NORMAL.getValue())) {
                    return i18nService.getI18n("user.operation.detail.start.web");
                } else if (detail.getStartupMode().equals(TaskStartupModeEnum.CRON.getValue())) {
                    return i18nService.getI18n("user.operation.detail.start.cron");
                } else if (detail.getStartupMode().equals(TaskStartupModeEnum.API.getValue())) {
                    return i18nService.getI18nWithArgs("user.operation.detail.start.api", detail.getAppCode());
                } else {
                    return "";
                }
            case CONFIRM_CONTINUE:
                return i18nService.getI18nWithArgs("user.operation.detail.confirm.continue", detail.getConfirmReason());
            case CONFIRM_TERMINATE:
                return i18nService.getI18nWithArgs("user.operation.detail.confirm.terminate",
                    detail.getConfirmReason());
            case CONFIRM_RESTART:
                return i18nService.getI18n("user.operation.detail.confirm.restart");
            case IGNORE_ERROR:
                return i18nService.getI18n("user.operation.detail.ignore_error");
            case NEXT_STEP:
                return i18nService.getI18n("user.operation.detail.next_step");
            case TERMINATE_JOB:
                return i18nService.getI18n("user.operation.detail.terminate_job");
            case RETRY_STEP_ALL:
                return i18nService.getI18n("user.operation.detail.retry_step_all");
            case RETRY_STEP_FAIL:
                return i18nService.getI18n("user.operation.detail.retry_step_fail");
            default:
                return "";
        }
    }

    @Override
    public Response<TaskInstanceDetailVO> getTaskInstanceDetail(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long taskInstanceId) {

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstanceDetail(taskInstanceId);
        if (taskInstance == null || !taskInstance.getAppId().equals(appResourceScope.getAppId())) {
            log.warn("TaskInstance:{} is not in app:{}", taskInstanceId, appResourceScope.getAppId());
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        AuthResult authResult = authViewTaskInstance(username, appResourceScope, taskInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(convertToTaskInstanceDetailVO(taskInstance));
    }

    private TaskInstanceDetailVO convertToTaskInstanceDetailVO(TaskInstanceDTO taskInstanceDTO) {
        TaskInstanceDetailVO taskInstanceDetailVO = new TaskInstanceDetailVO();
        TaskInstanceVO taskInstanceVO = TaskInstanceConverter.convertToTaskInstanceVO(taskInstanceDTO);
        taskInstanceDetailVO.setTaskInstance(taskInstanceVO);

        List<StepInstanceDTO> stepInstances = taskInstanceDTO.getStepInstances();
        List<ExecuteStepVO> stepVOS = new ArrayList<>(stepInstances.size());
        stepInstances.forEach(stepInstance -> {
            ExecuteStepVO stepVO = convertToStepVO(stepInstance);
            stepVOS.add(stepVO);
        });
        taskInstanceDetailVO.setSteps(stepVOS);

        if (taskInstanceDTO.getVariables() != null && !taskInstanceDTO.getVariables().isEmpty()) {
            List<ExecuteVariableVO> taskVariableVOS = new ArrayList<>();
            taskInstanceDTO.getVariables().forEach(variable -> {
                taskVariableVOS.add(convertToVariableVO(taskInstanceDTO.getAppId(), variable));
            });
            taskInstanceDetailVO.setVariables(taskVariableVOS);
        }

        return taskInstanceDetailVO;
    }

    @Override
    public Response<TaskInstanceVO> getTaskInstanceBasic(String username, Long taskInstanceId) {
        if (taskInstanceId == null || taskInstanceId <= 0) {
            log.warn("Get task instance basic, task instance id is null or empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        AuthResult authResult = authService.auth(true, username, ActionId.ACCESS_BUSINESS,
            ResourceTypeEnum.BUSINESS, taskInstance.getAppId().toString(), null);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(TaskInstanceConverter.convertToTaskInstanceVO(taskInstance));
    }
}
