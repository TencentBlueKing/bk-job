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

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.execute.api.web.WebTaskInstanceResource;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.converter.TaskInstanceConverter;
import com.tencent.bk.job.execute.model.db.RollingConfigDetailDO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteApprovalStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileDestinationInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileSourceInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteScriptStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteStepVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.RollingConfigVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceDetailVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.model.web.vo.TaskOperationLogVO;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.util.FileTransferModeUtil;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
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
    private final TaskOperationLogService taskOperationLogService;
    private final MessageI18nService i18nService;
    private final BusinessAuthService businessAuthService;
    private final RollingConfigService rollingConfigService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;
    private final StepInstanceService stepInstanceService;

    @Autowired
    public WebTaskInstanceResourceImpl(TaskInstanceService taskInstanceService,
                                       TaskInstanceVariableService taskInstanceVariableService,
                                       TaskOperationLogService taskOperationLogService,
                                       MessageI18nService i18nService,
                                       BusinessAuthService businessAuthService,
                                       RollingConfigService rollingConfigService,
                                       TaskInstanceAccessProcessor taskInstanceAccessProcessor,
                                       StepInstanceService stepInstanceService) {
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.taskOperationLogService = taskOperationLogService;
        this.i18nService = i18nService;
        this.businessAuthService = businessAuthService;
        this.rollingConfigService = rollingConfigService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
        this.stepInstanceService = stepInstanceService;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    @CompatibleImplementation(name = "dao_add_task_instance_id", deprecatedVersion = "3.11.x",
        type = CompatibleType.DEPLOY, explain = "发布完成后可以删除")
    public Response<ExecuteStepVO> getStepInstanceDetail(String username,
                                                         AppResourceScope appResourceScope,
                                                         String scopeType,
                                                         String scopeId,
                                                         Long stepInstanceId) {
        // 兼容代码，部署完成后删除
        StepInstanceBaseDTO stepInstance = stepInstanceService.getBaseStepInstanceById(stepInstanceId);
        return getStepInstanceDetailV2(username, appResourceScope, scopeType, scopeId,
            stepInstance.getTaskInstanceId(), stepInstanceId);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<ExecuteStepVO> getStepInstanceDetailV2(String username,
                                                           AppResourceScope appResourceScope,
                                                           String scopeType,
                                                           String scopeId,
                                                           Long taskInstanceId,
                                                           Long stepInstanceId) {
        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(taskInstanceId, stepInstanceId);

        taskInstanceAccessProcessor.processBeforeAccess(username,
            appResourceScope.getAppId(), stepInstance.getTaskInstanceId());

        ExecuteStepVO stepVO = convertToStepVO(stepInstance);
        fillRollingConfigForRollingStep(stepVO, stepInstance);

        return Response.buildSuccessResp(stepVO);
    }

    private void fillRollingConfigForRollingStep(ExecuteStepVO stepVO, StepInstanceBaseDTO stepInstance) {
        if (stepInstance.isRollingStep()) {
            RollingConfigVO rollingConfigVO = new RollingConfigVO();
            RollingConfigDTO rollingConfigDTO =
                rollingConfigService.getRollingConfig(stepInstance.getTaskInstanceId(),
                    stepInstance.getRollingConfigId());
            RollingConfigDetailDO rollingConfig = rollingConfigDTO.getConfigDetail();
            rollingConfigVO.setMode(rollingConfig.getMode());
            if (rollingConfigDTO.isBatchRollingStep(stepInstance.getId())) {
                rollingConfigVO.setExpr(rollingConfig.getExpr());
            }
            stepVO.setRollingConfig(rollingConfigVO);
            stepVO.setRollingEnabled(true);
        }
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
            fileSourceVO.setHost(fileSource.getServers().convertToTaskTargetVO());
            fileSources.add(fileSourceVO);
        }
        fileStepVO.setFileSourceList(fileSources);
    }

    private ExecuteStepVO convertToStepVO(StepInstanceDTO stepInstance) {
        ExecuteStepVO stepVO = new ExecuteStepVO();
        stepVO.setName(stepInstance.getName());
        StepExecuteTypeEnum stepType = stepInstance.getExecuteType();
        if (stepType == StepExecuteTypeEnum.EXECUTE_SCRIPT || stepType == StepExecuteTypeEnum.EXECUTE_SQL) {
            stepVO.setType(TaskStepTypeEnum.SCRIPT.getValue());
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
            scriptStepVO.setWindowsInterpreter(stepInstance.getWindowsInterpreter());
            scriptStepVO.setTimeout(stepInstance.getTimeout());
            scriptStepVO.setScriptLanguage(stepInstance.getScriptType().getValue());
            scriptStepVO.setScriptSource(stepInstance.getScriptSource());
            scriptStepVO.setScriptId(stepInstance.getScriptId());
            scriptStepVO.setScriptVersionId(stepInstance.getScriptVersionId());
            scriptStepVO.setExecuteTarget(stepInstance.getTargetExecuteObjects().convertToTaskTargetVO());
            scriptStepVO.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
            stepVO.setScriptStepInfo(scriptStepVO);
        } else if (stepType == StepExecuteTypeEnum.SEND_FILE) {
            stepVO.setType(TaskStepTypeEnum.FILE.getValue());
            ExecuteFileStepVO fileStepVO = new ExecuteFileStepVO();

            ExecuteFileDestinationInfoVO fileDestinationInfoVO = new ExecuteFileDestinationInfoVO();
            fileDestinationInfoVO.setAccountName(stepInstance.getAccount());
            fileDestinationInfoVO.setAccountId(stepInstance.getAccountId());
            if (StringUtils.isNotEmpty(stepInstance.getResolvedFileTargetPath())) {
                fileDestinationInfoVO.setPath(stepInstance.getResolvedFileTargetPath());
            } else {
                fileDestinationInfoVO.setPath(stepInstance.getFileTargetPath());
            }
            fileDestinationInfoVO.setServer(stepInstance.getTargetExecuteObjects().convertToTaskTargetVO());
            fileStepVO.setFileDestination(fileDestinationInfoVO);

            fileStepVO.setIgnoreError(stepInstance.isIgnoreError() ? 1 : 0);
            Integer transferMode = FileTransferModeUtil.getTransferMode(
                stepInstance.getFileDuplicateHandle(),
                stepInstance.getNotExistPathHandler()
            ).getValue();
            fileStepVO.setTransferMode(transferMode);
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
            stepVO.setType(TaskStepTypeEnum.APPROVAL.getValue());
            ExecuteApprovalStepVO approvalStepVO = new ExecuteApprovalStepVO();
            approvalStepVO.setApprovalMessage(stepInstance.getConfirmMessage());
            stepVO.setApprovalStepInfo(approvalStepVO);
        }
        return stepVO;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<ExecuteVariableVO>> getTaskInstanceVariables(String username,
                                                                      AppResourceScope appResourceScope,
                                                                      String scopeType,
                                                                      String scopeId,
                                                                      Long taskInstanceId) {

        taskInstanceAccessProcessor.processBeforeAccess(username, appResourceScope.getAppId(), taskInstanceId);

        List<TaskVariableDTO> taskVariables = taskInstanceVariableService.getByTaskInstanceId(taskInstanceId);
        List<ExecuteVariableVO> variableVOS = new ArrayList<>();
        if (taskVariables != null) {
            taskVariables.forEach(variable -> {
                variableVOS.add(convertToVariableVO(variable));
            });
        }
        return Response.buildSuccessResp(variableVOS);
    }

    private ExecuteVariableVO convertToVariableVO(TaskVariableDTO variable) {
        ExecuteVariableVO vo = new ExecuteVariableVO();
        vo.setId(variable.getId());
        vo.setName(variable.getName());
        vo.setType(variable.getType());
        vo.setChangeable(variable.isChangeable() ? 1 : 0);
        vo.setRequired(variable.isRequired() ? 1 : 0);
        if (variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
            ExecuteTargetDTO executeTarget = variable.getExecuteTarget();
            if (executeTarget != null && executeTarget.getExecuteObjectsCompatibly() != null) {
                TaskTargetVO taskTargetVO = executeTarget.convertToTaskTargetVO();
                vo.setTargetValue(taskTargetVO);
            }
        } else if (variable.getType().equals(TaskVariableTypeEnum.CIPHER.getType())) {
            vo.setValue("******");
        } else {
            vo.setValue(variable.getValue());
        }
        return vo;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<TaskOperationLogVO>> getTaskInstanceOperationLog(String username,
                                                                          AppResourceScope appResourceScope,
                                                                          String scopeType,
                                                                          String scopeId,
                                                                          Long taskInstanceId) {

        taskInstanceAccessProcessor.processBeforeAccess(username, appResourceScope.getAppId(), taskInstanceId);

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
            vo.setBatch(detail.getBatch());
            vo.setDetail(buildDetail(operationLog.getOperationEnum(), operationLog.getDetail()));
            vos.add(vo);
        }
        return Response.buildSuccessResp(vos);
    }

    private String buildDetail(UserOperationEnum operationCode, OperationLogDTO.OperationDetail detail) {
        switch (operationCode) {
            case START:
                if (detail.getStartupMode().equals(TaskStartupModeEnum.WEB.getValue())) {
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
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<TaskInstanceDetailVO> getTaskInstanceDetail(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long taskInstanceId) {

        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstanceDetail(username,
            appResourceScope.getAppId(), taskInstanceId);

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
            fillRollingConfigForRollingStep(stepVO, stepInstance);
            stepVOS.add(stepVO);
        });
        taskInstanceDetailVO.setSteps(stepVOS);

        if (taskInstanceDTO.getVariables() != null && !taskInstanceDTO.getVariables().isEmpty()) {
            List<ExecuteVariableVO> taskVariableVOS = new ArrayList<>();
            taskInstanceDTO.getVariables().forEach(variable -> {
                taskVariableVOS.add(convertToVariableVO(variable));
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
        AuthResult authResult = businessAuthService.authAccessBusiness(
            username, new AppResourceScope(taskInstance.getAppId())
        );
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        return Response.buildSuccessResp(TaskInstanceConverter.convertToTaskInstanceVO(taskInstance));
    }
}
