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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.gse.constants.AgentAliveStatusEnum;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.feature.strategy.JobInstanceAttrToggleStrategy;
import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.feature.ToggleStrategyContextParams;
import com.tencent.bk.job.execute.common.cache.WhiteHostCache;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceExecuteObjects;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.ContainerService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.SEND_FILE;

/**
 * 作业实例执行对象处理器
 */
@Slf4j
@Service
public class TaskInstanceExecuteObjectProcessor {

    private final HostService hostService;
    private final ApplicationService applicationService;
    private final ContainerService containerService;
    private final AppScopeMappingService appScopeMappingService;
    private final WhiteHostCache whiteHostCache;
    private final AgentStateClient preferV2AgentStateClient;

    public TaskInstanceExecuteObjectProcessor(HostService hostService,
                                              ApplicationService applicationService,
                                              ContainerService containerService,
                                              AppScopeMappingService appScopeMappingService,
                                              WhiteHostCache whiteHostCache,
                                              @Qualifier(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT)
                                                  AgentStateClient preferV2AgentStateClient) {
        this.hostService = hostService;
        this.applicationService = applicationService;
        this.containerService = containerService;
        this.appScopeMappingService = appScopeMappingService;
        this.whiteHostCache = whiteHostCache;
        this.preferV2AgentStateClient = preferV2AgentStateClient;
    }

    /**
     * 处理作业实例的执行对象（获取、设置主机、容器等执行对象），并返回所有作业实例中包含的执行执行对象（分类）
     *
     * @param taskInstance     作业实例
     * @param stepInstanceList 步骤实例列表
     * @param variables        作业全局变量
     * @return 作业实例中包含的执行对象
     */
    public TaskInstanceExecuteObjects processExecuteObjects(TaskInstanceDTO taskInstance,
                                                            List<StepInstanceDTO> stepInstanceList,
                                                            Collection<TaskVariableDTO> variables) {

        StopWatch watch = new StopWatch("processExecuteObjects");
        try {
            if (!isContainerExecuteFeatureEnabled(taskInstance.getAppId())
                && isContainerExecuteJob(stepInstanceList, variables)) {
                // 未开启"容器执行"特性的灰度,返回错误
                throw new NotImplementedException(
                    "ContainerExecute is not support", ErrorCode.NOT_SUPPORT_FEATURE);
            }

            long appId = taskInstance.getAppId();
            // 获取执行对象
            watch.start("acquireAndSetExecuteObjects");
            TaskInstanceExecuteObjects taskInstanceExecuteObjects = new TaskInstanceExecuteObjects();
            // 获取并设置主机执行对象
            acquireAndSetHosts(taskInstanceExecuteObjects, taskInstance, stepInstanceList, variables);
            // 获取并设置容器执行对象
            acquireAndSetContainers(taskInstanceExecuteObjects, taskInstance, stepInstanceList);
            boolean isSupportExecuteObjectFeature = isSupportExecuteObjectFeature(taskInstance);
            // 合并所有执行对象
            mergeExecuteObjects(stepInstanceList, variables, isSupportExecuteObjectFeature);
            // 检查执行对象是否存在
            checkExecuteObjectExist(taskInstanceExecuteObjects);
            watch.stop();

            // 如果包含主机执行对象，需要获取主机白名单
            if (taskInstanceExecuteObjects.isContainsAnyHost()) {
                watch.start("getHostAllowedActions");
                taskInstanceExecuteObjects.setWhiteHostAllowActions(
                    getHostAllowedActions(
                        appId,
                        ListUtil.union(taskInstanceExecuteObjects.getValidHosts(),
                            taskInstanceExecuteObjects.getNotInAppHosts())));
                watch.stop();
            }

            // 检查执行对象是否可用
            watch.start("checkExecuteObjectAccessible");
            checkExecuteObjectAccessible(appId, stepInstanceList, taskInstanceExecuteObjects);
            watch.stop();

            return taskInstanceExecuteObjects;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("ProcessExecuteObjects is slow, taskInfo: {}", watch.prettyPrint());
            }
        }
    }

    private boolean isContainerExecuteFeatureEnabled(long appId) {
        return FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_CONTAINER_EXECUTE,
            FeatureExecutionContext.builder()
                .addContextParam(
                    ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    appScopeMappingService.getScopeByAppId(appId)
                )
        );
    }

    private boolean isContainerExecuteJob(List<StepInstanceDTO> stepInstanceList,
                                          Collection<TaskVariableDTO> variables) {
        boolean isContainerExecuteJob = stepInstanceList.stream()
            .anyMatch(stepInstance ->
                (stepInstance.getTargetExecuteObjects() != null &&
                    stepInstance.getTargetExecuteObjects().hasContainerExecuteObject()) ||
                    CollectionUtils.isNotEmpty(stepInstance.getFileSourceList()) &&
                        stepInstance.getFileSourceList().stream()
                            .anyMatch(fileSource -> fileSource.getServers() != null &&
                                fileSource.getServers().hasContainerExecuteObject()));
        if (isContainerExecuteJob) {
            return true;
        }

        if (CollectionUtils.isNotEmpty(variables)) {
            isContainerExecuteJob = variables.stream()
                .anyMatch(variable ->
                    variable.getExecuteTarget() != null && variable.getExecuteTarget().hasContainerExecuteObject());
        }
        return isContainerExecuteJob;
    }

    private void acquireAndSetHosts(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                    TaskInstanceDTO taskInstance,
                                    List<StepInstanceDTO> stepInstances,
                                    Collection<TaskVariableDTO> variables) {
        StopWatch watch = new StopWatch("AcquireAndSetHosts");
        try {
            long appId = taskInstance.getAppId();

            // 提取动态分组/topo节点
            Set<DynamicServerGroupDTO> groups = new HashSet<>();
            Set<DynamicServerTopoNodeDTO> topoNodes = new HashSet<>();
            stepInstances.forEach(stepInstance -> extractDynamicGroupsAndTopoNodes(stepInstance, groups, topoNodes));
            if (CollectionUtils.isNotEmpty(variables)) {
                variables.forEach(variable -> {
                    if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                        extractDynamicGroupsAndTopoNodes(variable.getExecuteTarget(), groups, topoNodes);
                    }
                });
            }

            // 获取动态分组的主机并设置
            fillDynamicGroupHosts(watch, appId, groups, stepInstances, variables);

            // 获取topo节点的主机并设置
            fillTopoNodeHosts(watch, appId, topoNodes, stepInstances, variables);

            // 提取作业包含的主机列表
            watch.start("extractHosts");
            Set<HostDTO> queryHosts = extractHosts(stepInstances, variables);
            watch.stop();

            if (CollectionUtils.isEmpty(queryHosts)) {
                return;
            }

            taskInstanceExecuteObjects.setContainsAnyHost(true);

            watch.start("batchGetAppHosts");
            ServiceListAppHostResultDTO queryHostsResult = hostService.batchGetAppHosts(appId, queryHosts,
                needRefreshHostBkAgentId(taskInstance));
            watch.stop();

            taskInstanceExecuteObjects.setValidHosts(queryHostsResult.getValidHosts());
            taskInstanceExecuteObjects.setNotExistHosts(queryHostsResult.getNotExistHosts());
            taskInstanceExecuteObjects.setNotInAppHosts(queryHostsResult.getNotInAppHosts());

            watch.start("fillTaskInstanceHostDetail");
            fillTaskInstanceHostDetail(taskInstance, stepInstances, variables, taskInstanceExecuteObjects);
            watch.stop();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("AcquireAndSetHosts slow, watch: {}", watch.prettyPrint());
            }
        }

    }

    private void extractDynamicGroupsAndTopoNodes(StepInstanceDTO stepInstance,
                                                  Set<DynamicServerGroupDTO> groups,
                                                  Set<DynamicServerTopoNodeDTO> topoNodes) {
        extractDynamicGroupsAndTopoNodes(stepInstance.getTargetExecuteObjects(), groups, topoNodes);
        if (stepInstance.isFileStep()) {
            List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
            for (FileSourceDTO fileSource : fileSources) {
                ExecuteTargetDTO executeTarget = fileSource.getServers();
                if (executeTarget != null && !fileSource.isLocalUpload()) {
                    // 服务器文件的处理
                    extractDynamicGroupsAndTopoNodes(executeTarget, groups, topoNodes);
                }
            }
        }
    }

    private void extractDynamicGroupsAndTopoNodes(ExecuteTargetDTO executeTarget,
                                                  Set<DynamicServerGroupDTO> groups,
                                                  Set<DynamicServerTopoNodeDTO> topoNodes) {
        if (executeTarget == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())) {
            groups.addAll(executeTarget.getDynamicServerGroups());
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())) {
            topoNodes.addAll(executeTarget.getTopoNodes());
        }
    }

    private void fillDynamicGroupHosts(StopWatch watch,
                                       long appId,
                                       Set<DynamicServerGroupDTO> groups,
                                       List<StepInstanceDTO> stepInstances,
                                       Collection<TaskVariableDTO> variables) {
        if (CollectionUtils.isEmpty(groups)) {
            return;
        }
        // 获取动态分组的主机并设置
        watch.start("fillDynamicGroupHosts");
        Map<DynamicServerGroupDTO, List<HostDTO>> dynamicGroupHosts =
            hostService.batchGetAndGroupHostsByDynamicGroup(appId, groups);
        stepInstances.forEach(stepInstance -> {
            setHostsForDynamicGroup(stepInstance.getTargetExecuteObjects(), dynamicGroupHosts);
            if (stepInstance.isFileStep()) {
                List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
                for (FileSourceDTO fileSource : fileSources) {
                    ExecuteTargetDTO executeTarget = fileSource.getServers();
                    if (executeTarget != null && !fileSource.isLocalUpload()) {
                        // 服务器文件的处理
                        setHostsForDynamicGroup(executeTarget, dynamicGroupHosts);
                    }
                }
            }
        });
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.forEach(variable -> {
                if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                    setHostsForDynamicGroup(variable.getExecuteTarget(), dynamicGroupHosts);
                }
            });
        }
        watch.stop();
    }

    private void setHostsForDynamicGroup(ExecuteTargetDTO executeTarget,
                                         Map<DynamicServerGroupDTO, List<HostDTO>> groups) {
        if (executeTarget != null && CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())) {
            executeTarget.getDynamicServerGroups().forEach(group -> group.setIpList(groups.get(group)));
        }
    }

    private void fillTopoNodeHosts(StopWatch watch,
                                   long appId,
                                   Set<DynamicServerTopoNodeDTO> topoNodes,
                                   List<StepInstanceDTO> stepInstances,
                                   Collection<TaskVariableDTO> variables) {
        if (CollectionUtils.isEmpty(topoNodes)) {
            return;
        }
        watch.start("fillTopoNodeHosts");
        Map<DynamicServerTopoNodeDTO, List<HostDTO>> topoNodeHosts =
            hostService.getAndGroupHostsByTopoNodes(appId, topoNodes);
        stepInstances.forEach(stepInstance -> {
            setHostsForTopoNode(stepInstance.getTargetExecuteObjects(), topoNodeHosts);
            if (stepInstance.isFileStep()) {
                List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
                for (FileSourceDTO fileSource : fileSources) {
                    ExecuteTargetDTO executeTarget = fileSource.getServers();
                    if (executeTarget != null && !fileSource.isLocalUpload()) {
                        // 服务器文件的处理
                        setHostsForTopoNode(executeTarget, topoNodeHosts);
                    }
                }
            }
        });
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.forEach(variable -> {
                if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                    setHostsForTopoNode(variable.getExecuteTarget(), topoNodeHosts);
                }
            });
        }
        watch.stop();
    }

    private void setHostsForTopoNode(ExecuteTargetDTO executeTarget,
                                     Map<DynamicServerTopoNodeDTO, List<HostDTO>> topoNodes) {
        if (executeTarget != null && CollectionUtils.isNotEmpty(executeTarget.getTopoNodes())) {
            executeTarget.getTopoNodes().forEach(topoNode -> topoNode.setIpList(topoNodes.get(topoNode)));
        }
    }

    private void fillTaskInstanceHostDetail(TaskInstanceDTO taskInstance,
                                            List<StepInstanceDTO> stepInstanceList,
                                            Collection<TaskVariableDTO> variables,
                                            TaskInstanceExecuteObjects taskInstanceExecuteObjects) {

        fillHostAgent(taskInstance, taskInstanceExecuteObjects);

        Map<String, HostDTO> hostMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getValidHosts())) {
            taskInstanceExecuteObjects.getValidHosts().forEach(host -> {
                hostMap.put("hostId:" + host.getHostId(), host);
                hostMap.put("hostIp:" + host.toCloudIp(), host);
            });
        }
        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getNotInAppHosts())) {
            taskInstanceExecuteObjects.getNotInAppHosts().forEach(host -> {
                hostMap.put("hostId:" + host.getHostId(), host);
                hostMap.put("hostIp:" + host.toCloudIp(), host);
            });
        }

        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isStepContainsExecuteObject()) {
                continue;
            }
            // 目标主机设置主机详情
            fillTargetHostDetail(stepInstance, hostMap);
            // 文件源设置主机详情
            fillFileSourceHostDetail(stepInstance, hostMap);
        }

        if (CollectionUtils.isNotEmpty(variables)) {
            variables.forEach(variable -> {
                if (variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                    fillHostsDetail(variable.getExecuteTarget(), hostMap);
                }
            });
        }
    }

    private void fillHostAgent(TaskInstanceDTO taskInstance, TaskInstanceExecuteObjects taskInstanceExecuteObjects) {
        boolean isUsingGseV2 = isUsingGseV2(taskInstance,
            ListUtil.union(taskInstanceExecuteObjects.getValidHosts(), taskInstanceExecuteObjects.getNotInAppHosts()));
        /*
         * 后续下发任务给GSE会根据agentId路由请求到GSE1.0/2.0。如果要使用GSE2.0，那么直接使用原始bk_agent_id;如果要使用GSE1.0,
         * 按照{云区域ID:ip}的方式构造agent_id
         */
        Set<HostDTO> invalidAgentIdHosts = new HashSet<>();

        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getValidHosts())) {
            taskInstanceExecuteObjects.getValidHosts()
                .forEach(host -> setHostAgentId(isUsingGseV2, host, invalidAgentIdHosts));
        }

        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getNotInAppHosts())) {
            taskInstanceExecuteObjects.getNotInAppHosts()
                .forEach(host -> setHostAgentId(isUsingGseV2, host, invalidAgentIdHosts));
        }

        if (CollectionUtils.isNotEmpty(invalidAgentIdHosts)) {
            // 如果存在主机没有agentID，不影响影响整个任务的执行。所以这里仅输出日志，不拦截整个任务的执行。后续执行代码会处理`主机没有agentId`的情况
            log.warn("Contains invalid agent id host, appId: {}, isUsingGseV2: {}, invalidHosts: {}",
                taskInstance.getAppId(), isUsingGseV2, invalidAgentIdHosts);
        }

        setAgentStatus(taskInstanceExecuteObjects.getValidHosts());
        setAgentStatus(taskInstanceExecuteObjects.getNotInAppHosts());
    }

    private void setHostAgentId(boolean isUsingGseV2, HostDTO host, Set<HostDTO> invalidAgentIdHosts) {
        // 如果对接GSE1.0,使用云区域+ipv4构造agentId
        if (!isUsingGseV2) {
            host.setAgentId(host.toCloudIp());
        }
        if (StringUtils.isBlank(host.getAgentId())) {
            invalidAgentIdHosts.add(host);
        }
    }

    private void fillTargetHostDetail(StepInstanceDTO stepInstance, Map<String, HostDTO> hostMap) {
        fillHostsDetail(stepInstance.getTargetExecuteObjects(), hostMap);
    }

    private void fillFileSourceHostDetail(StepInstanceDTO stepInstance, Map<String, HostDTO> hostMap) {
        if (stepInstance.getExecuteType() == SEND_FILE) {
            List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
            if (fileSourceList != null) {
                for (FileSourceDTO fileSource : fileSourceList) {
                    fillHostsDetail(fileSource.getServers(), hostMap);
                }
            }
        }
    }

    private void fillHostsDetail(ExecuteTargetDTO executeTargetDTO, Map<String, HostDTO> hostMap) {
        if (executeTargetDTO != null) {
            fillHostsDetail(executeTargetDTO.getStaticIpList(), hostMap);
            if (CollectionUtils.isNotEmpty(executeTargetDTO.getDynamicServerGroups())) {
                executeTargetDTO.getDynamicServerGroups()
                    .forEach(group -> fillHostsDetail(group.getIpList(), hostMap));
            }
            if (CollectionUtils.isNotEmpty(executeTargetDTO.getTopoNodes())) {
                executeTargetDTO.getTopoNodes().forEach(topoNode -> fillHostsDetail(topoNode.getIpList(), hostMap));
            }
        }
    }

    private void fillHostsDetail(Collection<HostDTO> hosts, Map<String, HostDTO> hostMap) {
        if (CollectionUtils.isNotEmpty(hosts)) {
            hosts.forEach(host -> {
                HostDTO hostDetail;
                if (host.getHostId() != null) {
                    hostDetail = hostMap.get("hostId:" + host.getHostId());
                } else {
                    hostDetail = hostMap.get("hostIp:" + host.toCloudIp());
                }
                host.updateByHost(hostDetail);
            });
        }
    }

    private void setAgentStatus(List<HostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        long start = System.currentTimeMillis();

        List<HostAgentStateQuery> hostAgentStateQueryList = new ArrayList<>(hosts.size());
        Map<HostDTO, HostAgentStateQuery> hostAgentStateQueryMap = new HashMap<>(hosts.size());
        hosts.forEach(hostDTO -> {
            HostAgentStateQuery hostAgentStateQuery = HostAgentStateQuery.from(hostDTO);
            hostAgentStateQueryList.add(hostAgentStateQuery);
            hostAgentStateQueryMap.put(hostDTO, hostAgentStateQuery);
        });

        // 此处用于记录下发任务时的Agent状态快照数据，因此使用最终真实下发任务的agentId获取状态
        Map<String, AgentState> agentStateMap = preferV2AgentStateClient.batchGetAgentState(hostAgentStateQueryList);

        for (HostDTO host : hosts) {
            HostAgentStateQuery hostAgentStateQuery = hostAgentStateQueryMap.get(host);
            String effectiveAgentId = preferV2AgentStateClient.getEffectiveAgentId(hostAgentStateQuery);
            if (StringUtils.isEmpty(effectiveAgentId)) {
                host.setAlive(AgentAliveStatusEnum.NOT_ALIVE.getStatusValue());
                continue;
            }
            AgentState agentState = agentStateMap.get(effectiveAgentId);
            if (agentState != null) {
                host.setAlive(AgentAliveStatusEnum.fromAgentState(agentState).getStatusValue());
            } else {
                host.setAlive(AgentAliveStatusEnum.NOT_ALIVE.getStatusValue());
            }
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn("SetAgentStatus slow, hostSize: {}, cost:{} ms", hosts.size(), cost);
        }
    }

    private void acquireAndSetContainers(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                         TaskInstanceDTO taskInstance,
                                         List<StepInstanceDTO> stepInstances) {

        // // 根据静态容器列表方式获取并设置容器执行对象
        acquireAndSetContainersByStaticContainerList(taskInstanceExecuteObjects,
            taskInstance, stepInstances);

        // 根据 ContainerFilter 方式获取并设置容器执行对象
        acquireAndSetContainersByContainerFilters(taskInstanceExecuteObjects,
            taskInstance, stepInstances);
    }

    private void acquireAndSetContainersByStaticContainerList(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                                              TaskInstanceDTO taskInstance,
                                                              List<StepInstanceDTO> stepInstances) {
        Set<Long> queryContainerIds = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstances) {
            queryContainerIds.addAll(
                stepInstance.extractStaticContainerList().stream()
                    .map(Container::getId)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(queryContainerIds)) {
            List<Container> containers = containerService.listContainerByIds(
                taskInstance.getAppId(), queryContainerIds);
            if (CollectionUtils.isNotEmpty(containers)) {
                fillTaskInstanceContainerDetail(taskInstanceExecuteObjects, stepInstances,
                    containers.stream().collect(
                        Collectors.toMap(Container::getId, container -> container, (oldValue, newValue) -> newValue)));
            }
        }
    }

    private void acquireAndSetContainersByContainerFilters(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                                           TaskInstanceDTO taskInstance,
                                                           List<StepInstanceDTO> stepInstances) {
        for (StepInstanceDTO stepInstance : stepInstances) {
            stepInstance.forEachExecuteObjects(executeObjects -> {
                if (CollectionUtils.isNotEmpty(executeObjects.getContainerFilters())) {
                    executeObjects.getContainerFilters().forEach(containerFilter -> {
                        List<Container> filteredContainers =
                            containerService.listContainerByContainerFilter(taskInstance.getAppId(), containerFilter);
                        if (CollectionUtils.isNotEmpty(filteredContainers)) {
                            taskInstanceExecuteObjects.addContainers(filteredContainers);
                            containerFilter.setContainers(filteredContainers);
                        }
                    });
                }
            });
        }
    }

    private void fillTaskInstanceContainerDetail(TaskInstanceExecuteObjects taskInstanceExecuteObjects,
                                                 List<StepInstanceDTO> stepInstanceList,
                                                 Map<Long, Container> containerMap) {
        Set<Long> notExistContainerIds = new HashSet<>();
        taskInstanceExecuteObjects.setNotExistContainerIds(notExistContainerIds);

        for (StepInstanceDTO stepInstance : stepInstanceList) {
            stepInstance.forEachExecuteObjects(executeObjects -> {
                if (CollectionUtils.isNotEmpty(executeObjects.getStaticContainerList())) {
                    executeObjects.getStaticContainerList()
                        .forEach(container -> {
                            Container containDetail = containerMap.get(container.getId());
                            if (containDetail == null) {
                                notExistContainerIds.add(container.getId());
                                return;
                            }
                            container.updatePropsByContainer(containDetail);
                        });
                }
            });
        }
    }

    private void mergeExecuteObjects(List<StepInstanceDTO> stepInstanceList,
                                     Collection<TaskVariableDTO> variables,
                                     boolean isSupportExecuteObjectFeature) {
        stepInstanceList.forEach(stepInstance ->
            stepInstance.buildStepFinalExecuteObjects(isSupportExecuteObjectFeature));
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.forEach(variable -> {
                if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()
                    && variable.getExecuteTarget() != null) {
                    variable.getExecuteTarget().buildMergedExecuteObjects(isSupportExecuteObjectFeature);
                }
            });
        }
    }

    private void checkExecuteObjectExist(TaskInstanceExecuteObjects taskInstanceExecuteObjects) {
        List<String> notExistExecuteObjectList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getNotExistHosts())) {
            notExistExecuteObjectList.addAll(taskInstanceExecuteObjects.getNotExistHosts().stream()
                .map(this::printHostIdOrIp).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(taskInstanceExecuteObjects.getNotExistContainerIds())) {
            notExistExecuteObjectList.addAll(
                taskInstanceExecuteObjects.getNotExistContainerIds().stream()
                    .map(containerId -> "(container_id:" + containerId + ")")
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(notExistExecuteObjectList)) {
            String executeObjectStr = StringUtils.join(notExistExecuteObjectList, ",");
            log.warn("The following execute object are not exist, notExistExecuteObjectList={}",
                notExistExecuteObjectList);
            throw new FailedPreconditionException(ErrorCode.EXECUTE_OBJECT_NOT_EXIST,
                new Object[]{notExistExecuteObjectList.size(), executeObjectStr});
        }
    }

    private void throwHostInvalidException(Long appId, Collection<HostDTO> invalidHosts) {
        ServiceApplicationDTO application = applicationService.getAppById(appId);
        String appName = application.getName();
        String hostListStr = StringUtils.join(invalidHosts.stream()
            .map(this::printHostIdOrIp).collect(Collectors.toList()), ",");
        log.warn("The following hosts are invalid, hosts={}", invalidHosts);
        throw new FailedPreconditionException(ErrorCode.HOST_INVALID,
            new Object[]{appName, invalidHosts.size(), hostListStr});
    }

    /**
     * 判断执行对象是否可以被当前作业使用
     *
     * @param appId                      业务 ID
     * @param stepInstanceList           作业步骤列表
     * @param taskInstanceExecuteObjects 作业实例中包含的执行对象
     */
    private void checkExecuteObjectAccessible(long appId,
                                              List<StepInstanceDTO> stepInstanceList,
                                              TaskInstanceExecuteObjects taskInstanceExecuteObjects) {
        if (CollectionUtils.isEmpty(taskInstanceExecuteObjects.getNotInAppHosts())) {
            return;
        }
        Map<Long, List<String>> whileHostAllowActions = taskInstanceExecuteObjects.getWhiteHostAllowActions();
        log.info("Contains hosts not in app, check white host config. notInAppHosts: {}, whileHostAllowActions: {}",
            taskInstanceExecuteObjects.getNotInAppHosts(), whileHostAllowActions);
        Map<Long, HostDTO> notInAppHostMap = taskInstanceExecuteObjects.getNotInAppHosts().stream()
            .collect(Collectors.toMap(HostDTO::getHostId, host -> host, (host1, host2) -> host2));

        // 非法的主机
        Set<HostDTO> invalidHosts = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isStepContainsExecuteObject()) {
                continue;
            }
            TaskStepTypeEnum stepType = stepInstance.getStepType();
            // 检查目标主机
            stepInstance.getTargetExecuteObjects().getExecuteObjectsCompatibly().stream()
                .filter(ExecuteObject::isHostExecuteObject)
                .forEach(executeObject -> {
                    if (isHostUnAccessible(stepType, executeObject.getHost(), notInAppHostMap, whileHostAllowActions)) {
                        invalidHosts.add(executeObject.getHost());
                    }
                });
            // 如果是文件分发任务，检查文件源
            checkFileSourceHostAccessible(invalidHosts, stepInstance, stepType, notInAppHostMap, whileHostAllowActions);
        }

        if (CollectionUtils.isNotEmpty(invalidHosts)) {
            // 检查是否在白名单配置
            log.warn("Found hosts not in target app: {}!", appId);
            throwHostInvalidException(appId, invalidHosts);
        }
    }

    private void checkFileSourceHostAccessible(Set<HostDTO> invalidHosts,
                                               StepInstanceDTO stepInstance,
                                               TaskStepTypeEnum stepType,
                                               Map<Long, HostDTO> notInAppHostMap,
                                               Map<Long, List<String>> whileHostAllowActions) {
        if (!stepInstance.isFileStep()) {
            return;
        }
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return;
        }
        for (FileSourceDTO fileSource : fileSourceList) {
            // 远程文件分发需要校验文件源主机;其他类型不需要
            if (fileSource.getFileType().equals(TaskFileTypeEnum.SERVER.getType())) {
                ExecuteTargetDTO executeTarget = fileSource.getServers();
                if (executeTarget == null || CollectionUtils.isEmpty(executeTarget.getExecuteObjectsCompatibly())) {
                    continue;
                }
                executeTarget.getExecuteObjectsCompatibly().stream()
                    .filter(ExecuteObject::isHostExecuteObject)
                    .forEach(executeObject -> {
                        if (isHostUnAccessible(stepType, executeObject.getHost(),
                            notInAppHostMap, whileHostAllowActions)) {
                            invalidHosts.add(executeObject.getHost());
                        }
                    });
            }
        }
    }

    private boolean isHostUnAccessible(TaskStepTypeEnum stepType,
                                       HostDTO host,
                                       Map<Long, HostDTO> notInAppHostMap,
                                       Map<Long, List<String>> whileHostAllowActions) {
        long hostId = host.getHostId();
        if (!notInAppHostMap.containsKey(host.getHostId())) {
            // 主机在当前业务下，可以使用
            return false;
        }
        // 如果主机不在当前业务下，需要判断主机白名单
        if (whileHostAllowActions == null || whileHostAllowActions.isEmpty()) {
            return true;
        }
        List<String> allowActions = whileHostAllowActions.get(hostId);
        String actionScope = (stepType == TaskStepTypeEnum.SCRIPT ?
            ActionScopeEnum.SCRIPT_EXECUTE.name() :
            (stepType == TaskStepTypeEnum.FILE ? ActionScopeEnum.FILE_DISTRIBUTION.name() : ""));
        return CollectionUtils.isEmpty(allowActions) || !allowActions.contains(actionScope);
    }

    private String printHostIdOrIp(HostDTO host) {
        if (StringUtils.isNotBlank(host.getPrimaryIp())) {
            // 优先使用ip，可读性更好
            return "(ip:" + host.getPrimaryIp() + ")";
        } else {
            return "(host_id:" + host.getHostId() + ")";
        }
    }

    private boolean isSupportExecuteObjectFeature(TaskInstanceDTO taskInstance) {
        FeatureExecutionContext featureExecutionContext =
            FeatureExecutionContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    appScopeMappingService.getScopeByAppId(taskInstance.getAppId()));

        boolean featureEnabled = FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_EXECUTE_OBJECT,
            featureExecutionContext
        );
        log.info("Check feature: {}, result: {}", FeatureIdConstants.FEATURE_EXECUTE_OBJECT, featureEnabled);
        return featureEnabled;
    }

    /**
     * @param appId 业务ID
     * @param hosts 主机列表
     * @return key=hostId, value: 允许的操作列表
     */
    private Map<Long, List<String>> getHostAllowedActions(long appId, Collection<HostDTO> hosts) {
        Map<Long, List<String>> hostAllowActionsMap = new HashMap<>();
        if (CollectionUtils.isEmpty(hosts)) {
            return hostAllowActionsMap;
        }
        for (HostDTO host : hosts) {
            List<String> allowActions = whiteHostCache.getHostAllowedAction(appId, host.getHostId());
            if (CollectionUtils.isNotEmpty(allowActions)) {
                hostAllowActionsMap.put(host.getHostId(), allowActions);
            }
        }
        return hostAllowActionsMap;
    }

    private boolean needRefreshHostBkAgentId(TaskInstanceDTO taskInstance) {
        /*
         * tmp: GSE Agent v1/v2 兼容期间特殊逻辑, 对于节点管理安装Agent插件的请求需要实时获取bk_agent_id。等后续只对接GSE V2 之后，
         * 此处代码可删除。 https://github.com/Tencent/bk-job/issues/1542
         */
        return taskInstance.getStartupMode() == TaskStartupModeEnum.API.getValue()
            && StringUtils.isNotEmpty(taskInstance.getAppCode())
            && (StringUtils.equals(taskInstance.getAppCode(), "bkc-nodeman")
            || StringUtils.equals(taskInstance.getAppCode(), "bk_nodeman"));
    }

    private boolean isUsingGseV2(TaskInstanceDTO taskInstance, Collection<HostDTO> taskInstanceHosts) {
        // 初始化Job任务灰度对接 GSE2.0 上下文
        FeatureExecutionContext featureExecutionContext =
            FeatureExecutionContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    appScopeMappingService.getScopeByAppId(taskInstance.getAppId()))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ANY_GSE_V2_AGENT_AVAILABLE,
                    () -> taskInstanceHosts.stream().anyMatch(host -> AgentUtils.isGseV2AgentId(host.getAgentId())))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE,
                    () -> taskInstanceHosts.stream().allMatch(
                        host -> AgentUtils.isGseV2AgentId(host.getAgentId())))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE,
                    () -> TaskStartupModeEnum.getStartupMode(taskInstance.getStartupMode()).getName())
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, taskInstance::getOperator);

        boolean isUsingGseV2 = FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_GSE_V2,
            featureExecutionContext
        );
        log.info("Use gse version {}", isUsingGseV2 ? "v2" : "v1");
        return isUsingGseV2;
    }

    /**
     * 提取步骤和全局变量中包含的所有主机
     *
     * @param stepInstanceList 步骤实例列表
     * @param variables        作业全局变量列表
     * @return 所有主机
     */
    public Set<HostDTO> extractHosts(List<StepInstanceDTO> stepInstanceList,
                                     Collection<TaskVariableDTO> variables) {
        Set<HostDTO> hosts = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isStepContainsExecuteObject()) {
                continue;
            }
            if (stepInstance.getTargetExecuteObjects() != null) {
                hosts.addAll(stepInstance.getTargetExecuteObjects().extractHosts());
            }
            if (stepInstance.getExecuteType() == SEND_FILE) {
                List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
                if (fileSourceList != null) {
                    for (FileSourceDTO fileSource : fileSourceList) {
                        if (fileSource.getServers() != null) {
                            hosts.addAll(fileSource.getServers().extractHosts());
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.stream()
                .filter(variable -> variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()
                    && variable.getExecuteTarget() != null)
                .forEach(variable -> hosts.addAll(variable.getExecuteTarget().extractHosts()));
        }
        return hosts;
    }
}
