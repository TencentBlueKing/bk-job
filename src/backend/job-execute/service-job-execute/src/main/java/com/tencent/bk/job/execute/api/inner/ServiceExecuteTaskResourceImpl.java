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
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.iam.AuthResultDTO;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
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

@RestController
@Slf4j
public class ServiceExecuteTaskResourceImpl implements ServiceExecuteTaskResource {

    private final TaskExecuteService taskExecuteService;

    private final MessageI18nService i18nService;

    private final WebAuthService webAuthService;

    @Autowired
    public ServiceExecuteTaskResourceImpl(TaskExecuteService taskExecuteService,
                                          MessageI18nService i18nService,
                                          WebAuthService webAuthService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
        this.webAuthService = webAuthService;
    }

    @Override
    public InternalResponse<ServiceTaskExecuteResult> executeTask(ServiceTaskExecuteRequest request) {
        log.info("Execute task, request={}", request);
        if (!checkExecuteTaskRequest(request)) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskExecuteParam executeParam = buildExecuteParam(request);
        TaskInstanceDTO taskInstanceDTO = taskExecuteService.createTaskInstanceForTask(executeParam);
        taskExecuteService.startTask(taskInstanceDTO.getId());

        ServiceTaskExecuteResult result = new ServiceTaskExecuteResult();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setName(taskInstanceDTO.getName());
        return InternalResponse.buildSuccessResp(result);
    }

    private TaskExecuteParam buildExecuteParam(ServiceTaskExecuteRequest request) {
        List<TaskVariableDTO> executeVariableValues = new ArrayList<>();
        if (request.getTaskVariables() != null) {
            for (ServiceTaskVariable serviceTaskVariable : request.getTaskVariables()) {
                TaskVariableDTO taskVariableDTO = new TaskVariableDTO();
                taskVariableDTO.setId(serviceTaskVariable.getId());
                if (serviceTaskVariable.getType() == TaskVariableTypeEnum.STRING.getType()
                    || serviceTaskVariable.getType() == TaskVariableTypeEnum.CIPHER.getType()
                    || serviceTaskVariable.getType() == TaskVariableTypeEnum.INDEX_ARRAY.getType()
                    || serviceTaskVariable.getType() == TaskVariableTypeEnum.ASSOCIATIVE_ARRAY.getType()) {
                    taskVariableDTO.setValue(serviceTaskVariable.getStringValue());
                } else if (serviceTaskVariable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                    ServiceTargetServers serviceServers = serviceTaskVariable.getServerValue();
                    ServersDTO serversDTO = convertToServersDTO(serviceServers);
                    taskVariableDTO.setTargetServers(serversDTO);
                } else if (serviceTaskVariable.getType() == TaskVariableTypeEnum.NAMESPACE.getType()) {
                    taskVariableDTO.setValue(serviceTaskVariable.getNamespaceValue());
                }
                executeVariableValues.add(taskVariableDTO);
            }
        }
        return TaskExecuteParam
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
    }

    private ServersDTO convertToServersDTO(ServiceTargetServers servers) {
        if (servers == null) {
            return null;
        }
        ServersDTO serversDTO = new ServersDTO();
        if (servers.getIps() != null) {
            List<IpDTO> staticIpList = new ArrayList<>();
            servers.getIps().forEach(ip -> staticIpList.add(new IpDTO(ip.getCloudAreaId(), ip.getIp())));
            serversDTO.setStaticIpList(staticIpList);
        }
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
