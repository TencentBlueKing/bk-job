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

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.FeatureToggleModeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteAuthService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TopoService;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExecuteAuthServiceImpl implements ExecuteAuthService {
    private final AuthHelper authHelper;
    private final ResourceNameQueryService resourceNameQueryService;
    private final HostService hostService;
    private final AuthService authService;
    private final TaskInstanceService taskInstanceService;
    private final TopoService topoService;
    private final ResourceAppInfoQueryService resourceAppInfoQueryService;
    private final JobExecuteConfig jobExecuteConfig;

    @Autowired
    public ExecuteAuthServiceImpl(AuthHelper authHelper,
                                  ResourceNameQueryService resourceNameQueryService,
                                  HostService hostService,
                                  AuthService authService,
                                  TaskInstanceService taskInstanceService,
                                  TopoService topoService, ResourceAppInfoQueryService resourceAppInfoQueryService,
                                  JobExecuteConfig jobExecuteConfig) {
        this.authHelper = authHelper;
        this.resourceNameQueryService = resourceNameQueryService;
        this.hostService = hostService;
        this.authService = authService;
        this.taskInstanceService = taskInstanceService;
        this.topoService = topoService;
        this.resourceAppInfoQueryService = resourceAppInfoQueryService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.authService.setResourceAppInfoQueryService(resourceAppInfoQueryService);
        this.authService.setResourceNameQueryService(resourceNameQueryService);
    }

    private static PathInfoDTO buildTopoNodePathInfo(InstanceTopologyDTO topoNode) {
        PathBuilder pathBuilder = null;
        List<InstanceTopologyDTO> parents = topoNode.getParents();
        if (parents != null) {
            for (InstanceTopologyDTO parent : parents) {
                if (pathBuilder == null) {
                    pathBuilder = PathBuilder.newBuilder(parent.getObjectId(), parent.getInstanceId().toString());
                } else {
                    pathBuilder = pathBuilder.child(parent.getObjectId(), parent.getInstanceId().toString());
                }
            }
        }

        if (pathBuilder == null) {
            pathBuilder = PathBuilder.newBuilder(topoNode.getObjectId(), topoNode.getInstanceId().toString());
        } else {
            pathBuilder = pathBuilder.child(topoNode.getObjectId(), topoNode.getInstanceId().toString());
        }
        return pathBuilder.build();
    }

    protected boolean isMaintainerOfResource(String username, ResourceTypeEnum resourceType, String resourceId) {
        // 业务集、全业务特殊鉴权
        return authService.authSpecialAppByMaintainer(username, resourceType, resourceId);
    }

    public AuthResult authFastExecuteScript(String username, Long appId, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        log.debug("Auth fast execute script, username:{}, appId:{}, hostInstances:{}", username,
            appId, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(
            username, ActionId.QUICK_EXECUTE_SCRIPT, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.QUICK_EXECUTE_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authFastPushFile(String username, Long appId, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        log.debug("Auth Fast transfer file, username:{}, appId:{}, hostInstances:{}", username,
            appId, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(
            username, ActionId.QUICK_TRANSFER_FILE, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.QUICK_TRANSFER_FILE, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authExecuteAppScript(String username, Long appId,
                                           String scriptId, String scriptName, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO scriptInstance = buildExecutableInstance(appId, ResourceTypeEnum.SCRIPT, scriptId, null);

        log.debug("Auth execute script, username:{}, appId:{}, scriptId:{}, scriptInstance:{}, hostInstances:{}",
            username,
            appId, scriptId, scriptInstance, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(username, ActionId.EXECUTE_SCRIPT, scriptInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        PermissionResource scriptResource = new PermissionResource();
        scriptResource.setSystemId(SystemId.JOB);
        scriptResource.setResourceId(scriptId);
        scriptResource.setResourceType(ResourceTypeEnum.SCRIPT);
        if (StringUtils.isNotEmpty(scriptName)) {
            scriptResource.setResourceName(scriptName);
        } else {
            scriptResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.SCRIPT, scriptId));
        }
        authResult.addRequiredPermission(ActionId.EXECUTE_SCRIPT, scriptResource);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.EXECUTE_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    private InstanceDTO buildExecutableInstance(Long appId, ResourceTypeEnum resourceType, String resourceId,
                                                PathInfoDTO pathInfo) {
        InstanceDTO executeInstance = new InstanceDTO();
        executeInstance.setSystem(SystemId.JOB);
        executeInstance.setType(resourceType.getId());
        if (pathInfo == null) {
            executeInstance.setPath(PathBuilder.newBuilder(
                ResourceTypeEnum.BUSINESS.getId(),
                appId.toString()
            ).build());
        } else {
            executeInstance.setPath(pathInfo);
        }
        executeInstance.setId(resourceId);
        return executeInstance;
    }

    public AuthResult authExecutePublicScript(String username, Long appId,
                                              String scriptId, String scriptName, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO scriptInstance = buildExecutableInstance(
            appId, ResourceTypeEnum.PUBLIC_SCRIPT, scriptId, null);

        log.debug("Auth execute public script, username:{}, appId:{}, scriptId:{}, scriptInstance:{}, " +
                "hostInstances:{}", username,
            appId, scriptId, scriptInstance, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(username, ActionId.EXECUTE_PUBLIC_SCRIPT, scriptInstance,
            hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        PermissionResource scriptResource = new PermissionResource();
        scriptResource.setSystemId(SystemId.JOB);
        scriptResource.setResourceId(scriptId);
        scriptResource.setResourceType(ResourceTypeEnum.PUBLIC_SCRIPT);
        if (StringUtils.isNotEmpty(scriptName)) {
            scriptResource.setResourceName(scriptName);
        } else {
            scriptResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.PUBLIC_SCRIPT,
                scriptId));
        }
        authResult.addRequiredPermission(ActionId.EXECUTE_PUBLIC_SCRIPT, scriptResource);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.EXECUTE_PUBLIC_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authExecutePlan(String username, Long appId, Long templateId,
                                      Long planId, String planName, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO planInstance = buildExecutableInstance(
            appId,
            ResourceTypeEnum.PLAN, planId.toString(),
            PathBuilder.newBuilder(
                ResourceTypeEnum.BUSINESS.getId(),
                appId.toString()).child(ResourceTypeEnum.TEMPLATE.getId(),
                templateId.toString()
            ).build());

        log.debug("Auth execute plan, username:{}, appId:{}, planId:{}, planInstance:{}, hostInstances:{}", username,
            appId, planId, planInstance, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(username, ActionId.LAUNCH_JOB_PLAN, planInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        PermissionResource planResource = new PermissionResource();
        planResource.setSystemId(SystemId.JOB);
        planResource.setResourceId(planId.toString());
        planResource.setResourceType(ResourceTypeEnum.PLAN);
        if (StringUtils.isNotEmpty(planName)) {
            planResource.setResourceName(planName);
        } else {
            planResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.PLAN,
                planId.toString()));
        }
        authResult.addRequiredPermission(ActionId.LAUNCH_JOB_PLAN, planResource);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.LAUNCH_JOB_PLAN, hostResources);
        log.debug("Auth execute plan, authResult:{}", authResult);
        return authResult;
    }

    @Override
    public AuthResult authDebugTemplate(String username, Long appId, Long templateId, ServersDTO servers) {
        if (isMaintainerOfResource(username, ResourceTypeEnum.BUSINESS, appId.toString())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO jobTemplateInstance = buildExecutableInstance(appId, ResourceTypeEnum.TEMPLATE,
            templateId.toString(), null);

        log.debug("Auth execute job template, username:{}, appId:{}, planId:{}, templateInstance:{}, " +
                "hostInstances:{}", username,
            appId, templateId, jobTemplateInstance, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(username, ActionId.DEBUG_JOB_TEMPLATE, jobTemplateInstance,
            hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        PermissionResource jobTemplateResource = new PermissionResource();
        jobTemplateResource.setSystemId(SystemId.JOB);
        jobTemplateResource.setResourceId(templateId.toString());
        jobTemplateResource.setResourceType(ResourceTypeEnum.TEMPLATE);
        jobTemplateResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.TEMPLATE,
            templateId.toString()));
        authResult.addRequiredPermission(ActionId.DEBUG_JOB_TEMPLATE, jobTemplateResource);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appId, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.DEBUG_JOB_TEMPLATE, hostResources);
        log.debug("Auth execute job template, authResult:{}", authResult);
        return authResult;
    }

    private List<InstanceDTO> buildTopoNodeHostInstances(
        long appId,
        List<DynamicServerTopoNodeDTO> topoNodes,
        Map<DynamicServerTopoNodeDTO,
            InstanceTopologyDTO> topoNodeHierarchyMap
    ) {
        List<InstanceTopologyDTO> hierarchyTopoList = topoService.batchGetTopoNodeHierarchy(appId, topoNodes);
        if (CollectionUtils.isEmpty(hierarchyTopoList) || hierarchyTopoList.size() != topoNodes.size()) {
            log.warn("Get topo path wrong!");
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }

        List<InstanceDTO> topoNodeInstanceList = new ArrayList<>(hierarchyTopoList.size());
        hierarchyTopoList.forEach(hierarchyTopoNode -> {
            InstanceDTO topoNodeInstance = new InstanceDTO();
            topoNodeInstance.setType(ResourceTypeEnum.HOST.getId());
            topoNodeInstance.setSystem(SystemId.CMDB);
            topoNodeInstance.setPath(buildTopoNodePathInfo(hierarchyTopoNode));
            topoNodeInstanceList.add(topoNodeInstance);

            Optional<DynamicServerTopoNodeDTO> dynamicServerTopoNode =
                topoNodes.stream().filter(topo ->
                    topo.getNodeType().equals(hierarchyTopoNode.getObjectId())
                        && topo.getTopoNodeId() == hierarchyTopoNode.getInstanceId())
                    .findFirst();
            dynamicServerTopoNode.ifPresent(dynamicServerTopoNodeDTO ->
                topoNodeHierarchyMap.put(dynamicServerTopoNodeDTO, hierarchyTopoNode));
        });

        return topoNodeInstanceList;
    }

    private List<InstanceDTO> buildAppTopoNodeHostInstances(long appId) {
        List<InstanceDTO> topoNodeInstanceList = new ArrayList<>(1);
        InstanceDTO topoNodeInstance = new InstanceDTO();
        topoNodeInstance.setType(ResourceTypeEnum.HOST.getId());
        topoNodeInstance.setSystem(SystemId.CMDB);
        topoNodeInstance.setPath(PathBuilder.newBuilder(
            ResourceTypeEnum.BUSINESS.getId(),
            String.valueOf(appId)
        ).build());
        topoNodeInstanceList.add(topoNodeInstance);
        return topoNodeInstanceList;
    }

    private List<InstanceDTO> buildHostInstances(
        Long appId,
        ServersDTO servers,
        Map<String, String> ip2HostIdMap,
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap
    ) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        // 静态IP
        if (!CollectionUtils.isEmpty(servers.getStaticIpList())) {
            Map<IpDTO, ApplicationHostInfoDTO> appHosts =
                hostService.batchGetHostsPreferCache(servers.getStaticIpList());
            servers.getStaticIpList().forEach(host -> {
                InstanceDTO hostInstance = new InstanceDTO();
                ApplicationHostInfoDTO hostInfo = appHosts.get(host);
                if (hostInfo == null) {
                    log.warn("Host: {}:{} is not exist!", host.getCloudAreaId(), host.getIp());
                    throw new FailedPreconditionException(ErrorCode.SERVER_UNREGISTERED, new Object[]{host.getIp()});
                }
                hostInstance.setId(hostInfo.getHostId().toString());
                hostInstance.setType(ResourceTypeEnum.HOST.getId());
                hostInstance.setSystem(SystemId.CMDB);
                hostInstance.setName(hostInfo.getIp());
                hostInstance.setPath(null);
                hostInstanceList.add(hostInstance);

                ip2HostIdMap.put(host.convertToStrIp(), hostInfo.getHostId().toString());
            });
        }
        // 动态topo节点
        if (!CollectionUtils.isEmpty(servers.getTopoNodes())) {
            // CMDB未提供权限中心使用的topo视图，暂时使用“业务”这个topo节点进行鉴权，不细化到集群、模块
//            hostInstanceList.addAll(buildTopoNodeHostInstances(appId, servers.getTopoNodes(),
//           dynamicServerTopoNodeHierarchyMap));
            hostInstanceList.addAll(buildAppTopoNodeHostInstances(appId));
        }
        // 动态分组
        if (!CollectionUtils.isEmpty(servers.getDynamicServerGroups())) {
            servers.getDynamicServerGroups().forEach(serverGroup -> {
                InstanceDTO serverGroupInstance = new InstanceDTO();
                serverGroupInstance.setType(ResourceTypeEnum.HOST.getId());
                serverGroupInstance.setSystem(SystemId.CMDB);

                serverGroupInstance.setPath(PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString())
                    .child(ResourceTypeEnum.DYNAMIC_GROUP.getId(), serverGroup.getGroupId()).build());
                hostInstanceList.add(serverGroupInstance);
            });
        }
        return hostInstanceList;
    }

    private List<PermissionResource> convertHostsToPermissionResourceList(
        Long appId,
        ServersDTO servers,
        Map<String,
            String> ip2HostIdMap,
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap
    ) {
        List<PermissionResource> hostResources = new ArrayList<>();
        if (!CollectionUtils.isEmpty(servers.getStaticIpList())) {
            servers.getStaticIpList().forEach(ipDTO -> {
                PermissionResource resource = new PermissionResource();
                resource.setResourceId(ip2HostIdMap.get(ipDTO.convertToStrIp()));
                resource.setResourceType(ResourceTypeEnum.HOST);
                resource.setSubResourceType("host");
                resource.setResourceName(ipDTO.getIp());
                resource.setSystemId(SystemId.CMDB);
                resource.setType("host");
                hostResources.add(resource);
            });
        }
        if (!CollectionUtils.isEmpty(servers.getTopoNodes())) {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(String.valueOf(appId));
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setSubResourceType("topo");
            resource.setResourceName("biz," + appId);
            resource.setSystemId(SystemId.CMDB);
            resource.setType(CcNodeTypeEnum.APP.getType());
            resource.setParentHierarchicalResources(null);
            hostResources.add(resource);
        }
        if (!CollectionUtils.isEmpty(servers.getDynamicServerGroups())) {
            servers.getDynamicServerGroups().forEach(serverGroup -> {
                PermissionResource resource = new PermissionResource();
                String groupId = serverGroup.getGroupId();
                resource.setResourceId(groupId);
                resource.setResourceType(ResourceTypeEnum.HOST);
                resource.setSubResourceType("dynamic_group");
                resource.setType("biz_custom_query");
                resource.setResourceName(groupId);
                resource.setSystemId(SystemId.CMDB);
                resource.setParentHierarchicalResources(getDynamicGroupParentResources(appId));
                hostResources.add(resource);
            });
        }
        return hostResources;
    }

    private List<PermissionResource> getDynamicGroupParentResources(Long appId) {
        PermissionResource appResource = buildAppResource(appId);
        return Collections.singletonList(appResource);
    }

    private List<PermissionResource> getTopoParentResources(InstanceTopologyDTO topo) {
        if (topo == null || CollectionUtils.isEmpty(topo.getParents())) {
            return Collections.emptyList();
        }
        return topo.getParents().stream().map(node -> {
            PermissionResource resource = new PermissionResource();
            resource.setSystemId(SystemId.CMDB);
            resource.setType(node.getObjectId());
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setResourceId(String.valueOf(node.getInstanceId()));
            resource.setResourceName(node.getInstanceName());
            return resource;
        }).collect(Collectors.toList());

    }

    private PermissionResource buildAppResource(Long appId) {
        PermissionResource appResource = new PermissionResource();
        appResource.setSystemId(SystemId.CMDB);
        appResource.setType(ResourceTypeEnum.BUSINESS.getId());
        appResource.setResourceType(ResourceTypeEnum.BUSINESS);
        appResource.setResourceId(appId.toString());
        appResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.BUSINESS,
            appId.toString()));
        return appResource;
    }

    @Override
    public AuthResult authViewTaskInstance(String username, Long appId, long taskInstanceId) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        if (username.equals(taskInstance.getOperator())) {
            return AuthResult.pass();
        }
        return authService.auth(false, username, ActionId.VIEW_HISTORY,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
    }

    @Override
    public AuthResult authViewTaskInstance(String username, Long appId, TaskInstanceDTO taskInstance) {
        if (username.equals(taskInstance.getOperator())) {
            return AuthResult.pass();
        }
        return authService.auth(false, username, ActionId.VIEW_HISTORY,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
    }

    @Override
    public AuthResult authViewAllTaskInstance(String username, Long appId) {
        return authService.auth(false, username, ActionId.VIEW_HISTORY,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
    }

    @Override
    public AuthResult authAccountExecutable(String username, Long appId, Long accountId) {
        if (!shouldAuthAccount(appId)) {
            return AuthResult.pass();
        }
        return authService.auth(false, username, ActionId.USE_ACCOUNT,
            ResourceTypeEnum.ACCOUNT, accountId.toString(), PathBuilder
                .newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString())
                .child(ResourceTypeEnum.ACCOUNT.getId(), accountId.toString()).build());
    }

    private boolean shouldAuthAccount(Long appId) {
        String authAccountEnableMode = jobExecuteConfig.getEnableAuthAccountMode();
        if (FeatureToggleModeEnum.ENABLED.getMode().equals(authAccountEnableMode)) {
            return true;
        } else if (FeatureToggleModeEnum.DISABLED.getMode().equals(authAccountEnableMode)) {
            return false;
        } else if (FeatureToggleModeEnum.GRAY.getMode().equals(authAccountEnableMode)) {
            // 如果配置了灰度业务，仅针对灰度业务启用账号鉴权
            if (StringUtils.isNotBlank(jobExecuteConfig.getAccountAuthGrayApps())) {
                try {
                    String[] grayApps = jobExecuteConfig.getAccountAuthGrayApps().split(",");
                    if (grayApps.length == 0) {
                        // 如果没有配置灰度业务ID,那么账号鉴权功能对所有业务关闭
                        return false;
                    }
                    Set<Long> grayAppIds = new HashSet<>();
                    for (String app : grayApps) {
                        grayAppIds.add(Long.valueOf(app.trim()));
                    }
                    return grayAppIds.contains(appId);
                } catch (Throwable e) {
                    // 如果配置灰度业务ID错误,那么账号鉴权功能对所有业务关闭
                    log.error("Parse account auth gray app fail!", e);
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public AuthResult batchAuthAccountExecutable(String username, Long appId, Collection<Long> accountIds) {
        if (!shouldAuthAccount(appId)) {
            return AuthResult.pass();
        }

        List<PermissionResource> accountResources = accountIds.stream().map(accountId -> {
            PermissionResource accountResource = new PermissionResource();
            accountResource.setResourceId(accountId.toString());
            accountResource.setResourceType(ResourceTypeEnum.ACCOUNT);
            accountResource.setPathInfo(PathBuilder.newBuilder(
                ResourceTypeEnum.BUSINESS.getId(),
                appId.toString()
            ).child(ResourceTypeEnum.ACCOUNT.getId(), accountId.toString()).build());
            return accountResource;
        }).collect(Collectors.toList());
        return authService.batchAuthResources(username, ActionId.USE_ACCOUNT, appId, accountResources);
    }
}
