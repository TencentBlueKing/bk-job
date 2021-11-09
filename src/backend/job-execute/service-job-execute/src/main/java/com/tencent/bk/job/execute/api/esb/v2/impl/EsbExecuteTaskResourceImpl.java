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

package com.tencent.bk.job.execute.api.esb.v2.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.EsbGlobalVarDTO;
import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.EsbServerDTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.api.esb.v2.EsbExecuteTaskResource;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobExecuteDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbExecuteJobRequest;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class EsbExecuteTaskResourceImpl extends JobExecuteCommonProcessor implements EsbExecuteTaskResource {

    private final TaskExecuteService taskExecuteService;

    private final MessageI18nService i18nService;

    private final AuthService authService;

    @Autowired
    public EsbExecuteTaskResourceImpl(TaskExecuteService taskExecuteService, MessageI18nService i18nService,
                                      AuthService authService) {
        this.taskExecuteService = taskExecuteService;
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_execute_job"})
    public EsbResp<EsbJobExecuteDTO> executeJob(EsbExecuteJobRequest request) {
        log.info("Execute task, request={}", JsonUtils.toJson(request));
        ValidateResult checkResult = checkExecuteTaskRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Execute job request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        request.trimIps();

        List<TaskVariableDTO> executeVariableValues = new ArrayList<>();
        if (request.getGlobalVars() != null) {
            for (EsbGlobalVarDTO globalVar : request.getGlobalVars()) {
                TaskVariableDTO taskVariableDTO = new TaskVariableDTO();
                taskVariableDTO.setId(globalVar.getId());
                taskVariableDTO.setName(globalVar.getName());
                if ((globalVar.getIpList() != null || globalVar.getDynamicGroupIdList() != null
                    || globalVar.getTargetServer() != null) && StringUtils.isEmpty(globalVar.getValue())) {
                    ServersDTO serversDTO = convertToServersDTO(globalVar.getTargetServer(), globalVar.getIpList(),
                        globalVar.getDynamicGroupIdList());
                    taskVariableDTO.setTargetServers(serversDTO);
                } else {
                    taskVariableDTO.setValue(globalVar.getValue());
                }
                executeVariableValues.add(taskVariableDTO);
            }
        }
        TaskInstanceDTO taskInstanceDTO = taskExecuteService.createTaskInstanceForTask(
            TaskExecuteParam
                .builder()
                .appId(request.getAppId())
                .planId(request.getTaskId())
                .operator(request.getUserName())
                .executeVariableValues(executeVariableValues)
                .startupMode(TaskStartupModeEnum.API)
                .callbackUrl(request.getCallbackUrl())
                .appCode(request.getAppCode())
                .build());
        taskExecuteService.startTask(taskInstanceDTO.getId());

        EsbJobExecuteDTO result = new EsbJobExecuteDTO();
        result.setTaskInstanceId(taskInstanceDTO.getId());
        result.setTaskName(taskInstanceDTO.getName());
        return EsbResp.buildSuccessResp(result);
    }

    private ValidateResult checkExecuteTaskRequest(EsbExecuteJobRequest request) {
        if (request.getTaskId() == null || request.getTaskId() <= 0) {
            log.warn("Execute task, taskId is empty!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_job_id");
        }
        if (request.getGlobalVars() != null) {
            for (EsbGlobalVarDTO globalVar : request.getGlobalVars()) {
                if ((globalVar.getId() == null || globalVar.getId() <= 0)
                    && StringUtils.isBlank(globalVar.getName())) {
                    log.warn("Execute task, both variable id and name are empty");
                    return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                        "global_vars.id|global_vars.name");
                }
            }
        }
        return ValidateResult.pass();
    }

    private ServersDTO convertToServersDTO(EsbServerDTO servers, List<EsbIpDTO> ipList,
                                           List<String> dynamicGroupIdList) {
        if (servers == null && ipList == null && dynamicGroupIdList == null) {
            return null;
        }
        ServersDTO serversDTO = new ServersDTO();
        if (servers != null) {
            if (servers.getIps() != null) {
                List<IpDTO> staticIpList = new ArrayList<>();
                servers.getIps().forEach(ip -> staticIpList.add(new IpDTO(ip.getCloudAreaId(), ip.getIp())));
                serversDTO.setStaticIpList(staticIpList);
            }
            if (servers.getDynamicGroupIds() != null) {
                List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
                servers.getDynamicGroupIds().forEach(
                    groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
                serversDTO.setDynamicServerGroups(dynamicServerGroups);
            }
            if (servers.getTopoNodes() != null) {
                List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
                servers.getTopoNodes().forEach(
                    topoNode -> topoNodes.add(new DynamicServerTopoNodeDTO(topoNode.getId(), topoNode.getNodeType())));
                serversDTO.setTopoNodes(topoNodes);
            }
        } else {
            if (ipList != null) {
                List<IpDTO> staticIpList = new ArrayList<>();
                ipList.forEach(ip -> staticIpList.add(new IpDTO(ip.getCloudAreaId(), ip.getIp())));
                serversDTO.setStaticIpList(staticIpList);
            }
            if (dynamicGroupIdList != null) {
                List<DynamicServerGroupDTO> dynamicServerGroups = new ArrayList<>();
                dynamicGroupIdList.forEach(groupId -> dynamicServerGroups.add(new DynamicServerGroupDTO(groupId)));
                serversDTO.setDynamicServerGroups(dynamicServerGroups);
            }
        }
        return serversDTO;


    }
}
