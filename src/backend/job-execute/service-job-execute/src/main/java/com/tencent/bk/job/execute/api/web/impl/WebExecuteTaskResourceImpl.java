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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.api.web.WebExecuteTaskResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.constants.StepOperationEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.StepRollingConfigDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.web.request.RedoTaskRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.web.request.WebFastPushFileRequest;
import com.tencent.bk.job.execute.model.web.request.WebStepOperation;
import com.tencent.bk.job.execute.model.web.request.WebTaskExecuteRequest;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileDestinationInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteFileSourceInfoVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteServersVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteTargetVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecuteVO;
import com.tencent.bk.job.execute.model.web.vo.StepOperationVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteVO;
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

import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.ASSOCIATIVE_ARRAY;
import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.CIPHER;
import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.HOST_LIST;
import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.INDEX_ARRAY;
import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.NAMESPACE;
import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.STRING;

@RestController
@Slf4j
public class WebExecuteTaskResourceImpl implements WebExecuteTaskResource {
    private final TaskExecuteService taskExecuteService;

    @Autowired
    public WebExecuteTaskResourceImpl(TaskExecuteService taskExecuteService) {
        this.taskExecuteService = taskExecuteService;
    }

    @Override
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_WEB,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_EXECUTE_PLAN
        })
    public Response<TaskExecuteVO> executeTask(String username,
                                               AppResourceScope appResourceScope,
                                               String scopeType,
                                               String scopeId,
                                               WebTaskExecuteRequest request) {
        log.info("Execute task, request={}", request);

        if (!checkExecuteTaskRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        List<TaskVariableDTO> executeVariableValues = buildExecuteVariables(request.getTaskVariables());

        TaskInstanceDTO taskInstanceDTO = taskExecuteService.executeJobPlan(
            TaskExecuteParam.builder().appId(appResourceScope.getAppId()).planId(request.getTaskId())
                .operator(username).executeVariableValues(executeVariableValues)
                .startupMode(TaskStartupModeEnum.NORMAL).build());

        TaskExecuteVO result = new TaskExecuteVO();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setName(taskInstanceDTO.getName());
        return Response.buildSuccessResp(result);
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
    public Response<TaskExecuteVO> redoTask(String username,
                                            AppResourceScope appResourceScope,
                                            String scopeType,
                                            String scopeId,
                                            RedoTaskRequest request) {
        log.info("Redo task, request={}", request);

        if (!checkRedoTaskRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        List<TaskVariableDTO> executeVariableValues = buildExecuteVariables(request.getTaskVariables());
        TaskInstanceDTO taskInstanceDTO = taskExecuteService.createTaskInstanceForRedo(appResourceScope.getAppId(),
            request.getTaskInstanceId(), username, executeVariableValues);

        TaskExecuteVO result = new TaskExecuteVO();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setName(taskInstanceDTO.getName());
        return Response.buildSuccessResp(result);
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
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_WEB,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_SCRIPT
        })
    public Response<StepExecuteVO> fastExecuteScript(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     WebFastExecuteScriptRequest request) {
        log.debug("Fast execute script, scope={}, operator={}, request={}", appResourceScope, username, request);

        if (!checkFastExecuteScriptRequest(request)) {
            log.warn("Fast execute script request is illegal!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        TaskInstanceDTO taskInstance = buildFastScriptTaskInstance(username, appResourceScope.getAppId(), request);
        StepInstanceDTO stepInstance = buildFastScriptStepInstance(username, appResourceScope.getAppId(), request);
        String decodeScriptContent = new String(Base64.decodeBase64(request.getContent()), StandardCharsets.UTF_8);
        stepInstance.setScriptContent(decodeScriptContent);
        StepRollingConfigDTO rollingConfig = null;
        if (request.isRollingEnabled()) {
            rollingConfig = StepRollingConfigDTO.fromRollingConfigVO(request.getRollingConfig());
        }

        return createAndStartFastTask(request.isRedoTask(), taskInstance, stepInstance, rollingConfig);
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
        if (targetServers == null || targetServers.getSelectedServers() == null) {
            log.warn("Fast execute script, target server is null!");
            return false;
        }
        if (CollectionUtils.isEmpty(targetServers.getSelectedServers().getHostList()) &&
            CollectionUtils.isEmpty(targetServers.getSelectedServers().getNodeList())
            && CollectionUtils.isEmpty(targetServers.getSelectedServers().getDynamicGroupIdList())) {
            log.warn("Fast execute script, target server is null!");
            return false;
        }
        if (request.getAccount() == null || request.getAccount() < 1) {
            log.warn("Fast execute script, accountId is invalid! accountId={}", request.getAccount());
            return false;
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
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setType(TaskTypeEnum.SCRIPT.getValue());
        taskInstance.setCurrentStepInstanceId(0L);
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
        stepInstance.setStatus(RunStatusEnum.BLANK);
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
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_WEB,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_FAST_FILE
        })
    public Response<StepExecuteVO> fastPushFile(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                WebFastPushFileRequest request) {
        log.debug("Fast send file, scope={}, operator={}, request={}", appResourceScope, username, request);
        if (!checkFastPushFileRequest(request)) {
            log.warn("Fast send file request is illegal!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        TaskInstanceDTO taskInstance = buildFastFileTaskInstance(username, appResourceScope.getAppId(), request);
        StepInstanceDTO stepInstance = buildFastFileStepInstance(username, appResourceScope.getAppId(), request);
        StepRollingConfigDTO rollingConfig = null;
        if (request.isRollingEnabled()) {
            rollingConfig = StepRollingConfigDTO.fromRollingConfigVO(request.getRollingConfig());
        }

        return createAndStartFastTask(false, taskInstance, stepInstance, rollingConfig);
    }

    private Response<StepExecuteVO> createAndStartFastTask(boolean isRedoTask,
                                                           TaskInstanceDTO taskInstance,
                                                           StepInstanceDTO stepInstance,
                                                           StepRollingConfigDTO rollingConfig) {
        FastTaskDTO fastTask = FastTaskDTO.builder().taskInstance(taskInstance).stepInstance(stepInstance)
            .rollingConfig(rollingConfig).build();
        long taskInstanceId;
        if (!isRedoTask) {
            taskInstanceId = taskExecuteService.executeFastTask(fastTask);
        } else {
            taskInstanceId = taskExecuteService.redoFastTask(fastTask);
        }
        StepExecuteVO stepExecuteVO = new StepExecuteVO();
        stepExecuteVO.setTaskInstanceId(taskInstanceId);
        stepExecuteVO.setStepInstanceId(stepInstance.getId());
        stepExecuteVO.setStepName(stepInstance.getName());
        return Response.buildSuccessResp(stepExecuteVO);
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
        if (targetServers == null || targetServers.getSelectedServers() == null) {
            log.warn("Fast send file, target server is null!");
            return false;
        }
        if (CollectionUtils.isEmpty(targetServers.getSelectedServers().getHostList()) &&
            CollectionUtils.isEmpty(targetServers.getSelectedServers().getNodeList())
            && CollectionUtils.isEmpty(targetServers.getSelectedServers().getDynamicGroupIdList())) {
            log.warn("Fast send file, target server is null!");
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
            }
        }
        if (StringUtils.isBlank(fileDestination.getPath())) {
            log.warn("Fast send file, targetPath is empty");
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
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setStartupMode(TaskStartupModeEnum.NORMAL.getValue());
        taskInstance.setOperator(username);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setCurrentStepInstanceId(0L);
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
        stepInstance.setStatus(RunStatusEnum.BLANK);
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

    @CompatibleImplementation(name = "ipv6", explain = "兼容IP，发布完成之后使用hostId，不再使用IP", version = "3.6.x")
    private ServersDTO convertToServersDTO(ExecuteTargetVO target) {
        if (target == null || target.getSelectedServers() == null) {
            return null;
        }
        ExecuteServersVO hostNode = target.getSelectedServers();
        ServersDTO serversDTO = new ServersDTO();
        if (CollectionUtils.isNotEmpty(hostNode.getHostList())) {
            List<HostDTO> staticIpList = new ArrayList<>();
            hostNode.getHostList().forEach(host -> {
                HostDTO targetHost = new HostDTO();
                if (host.getHostId() != null) {
                    targetHost.setHostId(host.getHostId());
                } else {
                    // 兼容IP，发布完成后删除
                    targetHost.setBkCloudId(host.getCloudId());
                    targetHost.setIp(host.getIp());
                }
                staticIpList.add(targetHost);
            });
            serversDTO.setStaticIpList(staticIpList);
        }
        if (CollectionUtils.isNotEmpty(hostNode.getDynamicGroupIdList())) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            hostNode.getDynamicGroupIdList().forEach(
                groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
            serversDTO.setDynamicServerGroups(dynamicServerGroups);
        }
        if (CollectionUtils.isNotEmpty(hostNode.getNodeList())) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            hostNode.getNodeList().forEach(
                topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getInstanceId(), topoNode.getObjectId())));
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
            TaskFileTypeEnum fileType = TaskFileTypeEnum.valueOf(fileSource.getFileType());
            FileSourceDTO fileSourceDTO = new FileSourceDTO();
            fileSourceDTO.setAccountId(fileSource.getAccountId());
            fileSourceDTO.setLocalUpload(TaskFileTypeEnum.LOCAL == fileType);
            fileSourceDTO.setFileType(fileType.getType());
            fileSourceDTO.setFileSourceId(fileSource.getFileSourceId());
            List<FileDetailDTO> files = new ArrayList<>();
            if (fileSource.getFileLocation() != null) {
                for (String file : fileSource.getFileLocation()) {
                    if (TaskFileTypeEnum.LOCAL == fileType) {
                        files.add(new FileDetailDTO(true, file, fileSource.getFileHash(),
                            Long.valueOf(fileSource.getFileSize())));
                    } else {
                        // 服务器文件与文件源文件都只用路径
                        files.add(new FileDetailDTO(file));
                    }
                }
            }
            fileSourceDTO.setFiles(files);
            if (fileType == TaskFileTypeEnum.SERVER) {
                // 服务器文件分发才需要解析主机参数
                fileSourceDTO.setServers(convertToServersDTO(fileSource.getHost()));
            }
            fileSourceDTOS.add(fileSourceDTO);
        });
        return fileSourceDTOS;
    }

    @Override
    public Response<StepOperationVO> doStepOperation(String username,
                                                     AppResourceScope appResourceScope,
                                                     String scopeType,
                                                     String scopeId,
                                                     Long stepInstanceId,
                                                     WebStepOperation operation) {
        StepOperationEnum stepOperationEnum = StepOperationEnum.getStepOperation(operation.getOperationCode());
        if (stepOperationEnum == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        StepOperationDTO stepOperation = new StepOperationDTO();
        stepOperation.setStepInstanceId(stepInstanceId);
        stepOperation.setOperation(stepOperationEnum);
        stepOperation.setConfirmReason(operation.getConfirmReason());
        int executeCount = taskExecuteService.doStepOperation(appResourceScope.getAppId(), username, stepOperation);
        StepOperationVO stepOperationVO = new StepOperationVO(stepInstanceId, executeCount);
        return Response.buildSuccessResp(stepOperationVO);
    }

    @Override
    public Response terminateJob(String username,
                                 AppResourceScope appResourceScope,
                                 String scopeType,
                                 String scopeId,
                                 Long taskInstanceId) {
        if (taskInstanceId == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        taskExecuteService.terminateJob(username, appResourceScope.getAppId(), taskInstanceId);
        return Response.buildSuccessResp(null);
    }
}
