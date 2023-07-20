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

package com.tencent.bk.job.execute.api.inner;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.common.web.metrics.CustomTimed;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.metrics.ExecuteMetricsConstants;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceTargetServers;
import com.tencent.bk.job.execute.model.inner.ServiceTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import com.tencent.bk.job.execute.model.inner.request.ServiceTaskExecuteRequest;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.TaskVariableTypeEnum.CIPHER;

@RestController
@Slf4j
public class ServiceExecuteTaskResourceImpl implements ServiceExecuteTaskResource {

    private final TaskExecuteService taskExecuteService;

    private final WebAuthService webAuthService;

    @Autowired
    public ServiceExecuteTaskResourceImpl(TaskExecuteService taskExecuteService,
                                          WebAuthService webAuthService) {
        this.taskExecuteService = taskExecuteService;
        this.webAuthService = webAuthService;
    }

    @Override
    @CustomTimed(metricName = ExecuteMetricsConstants.NAME_JOB_TASK_START,
        extraTags = {
            ExecuteMetricsConstants.TAG_KEY_START_MODE, ExecuteMetricsConstants.TAG_VALUE_START_MODE_CRON,
            ExecuteMetricsConstants.TAG_KEY_TASK_TYPE, ExecuteMetricsConstants.TAG_VALUE_TASK_TYPE_EXECUTE_PLAN
        })
    public InternalResponse<ServiceTaskExecuteResult> executeTask(ServiceTaskExecuteRequest request) {
        log.info("Execute task, request={}", request);
        if (!checkExecuteTaskRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskExecuteParam executeParam = buildExecuteParam(request);
        TaskInstanceDTO taskInstanceDTO = taskExecuteService.executeJobPlan(executeParam);

        ServiceTaskExecuteResult result = new ServiceTaskExecuteResult();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setName(taskInstanceDTO.getName());
        return InternalResponse.buildSuccessResp(result);
    }

    private TaskExecuteParam buildExecuteParam(ServiceTaskExecuteRequest request) {
        List<TaskVariableDTO> executeVariableValues = new ArrayList<>();
        TaskExecuteParam taskExecuteParam = TaskExecuteParam
            .builder()
            .appId(request.getAppId())
            .planId(request.getPlanId())
            .operator(request.getOperator())
            .executeVariableValues(executeVariableValues)
            .startupMode(TaskStartupModeEnum.getStartupMode(request.getStartupMode()))
            .cronTaskId(request.getCronTaskId())
            .taskName(request.getTaskName())
            .skipAuth(request.getCronTaskId() != null && request.isSkipAuth())
            .build();
        if (request.getTaskVariables() == null) {
            return taskExecuteParam;
        }
        // 解析构造全局变量
        for (ServiceTaskVariable serviceTaskVariable : request.getTaskVariables()) {
            TaskVariableDTO taskVariableDTO = new TaskVariableDTO();
            taskVariableDTO.setId(serviceTaskVariable.getId());
            if (serviceTaskVariable.getType() == TaskVariableTypeEnum.STRING.getType()
                || serviceTaskVariable.getType() == TaskVariableTypeEnum.INDEX_ARRAY.getType()
                || serviceTaskVariable.getType() == TaskVariableTypeEnum.ASSOCIATIVE_ARRAY.getType()) {
                taskVariableDTO.setValue(serviceTaskVariable.getStringValue());
            } else if (serviceTaskVariable.getType() == CIPHER.getType()) {
                // 如果密码类型的变量传入为空或者“******”，那么密码使用系统中保存的
                if (serviceTaskVariable.getStringValue() == null || "******".equals(serviceTaskVariable.getStringValue())) {
                    continue;
                } else {
                    taskVariableDTO.setValue(serviceTaskVariable.getStringValue());
                }
            } else if (serviceTaskVariable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                ServiceTargetServers serviceServers = serviceTaskVariable.getServerValue();
                ServersDTO serversDTO = convertToServersDTO(serviceServers);
                taskVariableDTO.setTargetServers(serversDTO);
            } else if (serviceTaskVariable.getType() == TaskVariableTypeEnum.NAMESPACE.getType()) {
                taskVariableDTO.setValue(serviceTaskVariable.getNamespaceValue());
            }
            executeVariableValues.add(taskVariableDTO);
        }
        taskExecuteParam.setExecuteVariableValues(executeVariableValues);
        return taskExecuteParam;
    }

    private ServersDTO convertToServersDTO(ServiceTargetServers servers) {
        if (servers == null) {
            return null;
        }
        ServersDTO serversDTO = new ServersDTO();
        serversDTO.setStaticIpList(servers.getIps());
        if (servers.getDynamicGroupIds() != null) {
            List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
            servers.getDynamicGroupIds()
                .forEach(groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
            serversDTO.setDynamicServerGroups(dynamicServerGroups);
        }
        if (servers.getTopoNodes() != null) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            servers.getTopoNodes().forEach(topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(),
                topoNode.getNodeType())));
            serversDTO.setTopoNodes(topoNodes);
        }
        return serversDTO;
    }

    private boolean checkExecuteTaskRequest(ServiceTaskExecuteRequest request) {
        if (request.getPlanId() == null || request.getPlanId() <= 0) {
            log.warn("Execute task, taskId is empty!");
            return false;
        }
        if (request.getTaskVariables() != null) {
            for (ServiceTaskVariable serviceTaskVariable : request.getTaskVariables()) {
                if (serviceTaskVariable.getId() == null || serviceTaskVariable.getId() <= 0) {
                    log.warn("Execute task, variable id is empty");
                    return false;
                }
                if (serviceTaskVariable.getType() == null
                    || TaskVariableTypeEnum.valOf(serviceTaskVariable.getType()) == null) {
                    log.warn("Invalid variable type:{}", serviceTaskVariable.getType());
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public InternalResponse<AuthResultDTO> authExecuteTask(ServiceTaskExecuteRequest request) {
        log.info("Auth execute task, request={}", request);
        if (!checkExecuteTaskRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskExecuteParam executeParam = buildExecuteParam(request);

        AuthResultDTO authResult = null;
        try {
            taskExecuteService.authExecuteJobPlan(executeParam);
        } catch (PermissionDeniedException e) {
            authResult = AuthResult.toAuthResultDTO(e.getAuthResult());
            log.debug("Insufficient permission, authResult: {}", authResult);
            if (StringUtils.isEmpty(authResult.getApplyUrl())) {
                authResult.setApplyUrl(webAuthService.getApplyUrl(e.getAuthResult().getRequiredActionResources()));
            }
        }
        return InternalResponse.buildSuccessResp(authResult);
    }
}
