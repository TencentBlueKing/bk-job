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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.FeatureToggleModeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceAppInfoQueryService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExecuteAuthServiceImpl implements ExecuteAuthService {
    private final AuthHelper authHelper;
    private final ResourceNameQueryService resourceNameQueryService;
    private final HostService hostService;
    private final AuthService authService;
    private final AppAuthService appAuthService;
    private final TaskInstanceService taskInstanceService;
    private final JobExecuteConfig jobExecuteConfig;

    @Autowired
    public ExecuteAuthServiceImpl(AuthHelper authHelper,
                                  ResourceNameQueryService resourceNameQueryService,
                                  HostService hostService,
                                  AuthService authService,
                                  AppAuthService appAuthService,
                                  TaskInstanceService taskInstanceService,
                                  ResourceAppInfoQueryService resourceAppInfoQueryService,
                                  JobExecuteConfig jobExecuteConfig) {
        this.authHelper = authHelper;
        this.resourceNameQueryService = resourceNameQueryService;
        this.hostService = hostService;
        this.authService = authService;
        this.appAuthService = appAuthService;
        this.taskInstanceService = taskInstanceService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.authService.setResourceAppInfoQueryService(resourceAppInfoQueryService);
        this.authService.setResourceNameQueryService(resourceNameQueryService);
        this.appAuthService.setResourceAppInfoQueryService(resourceAppInfoQueryService);
        this.appAuthService.setResourceNameQueryService(resourceNameQueryService);
    }

    protected boolean isMaintainerOfResource(String username, ResourceTypeEnum resourceType, String resourceId) {
        // 业务集、全业务特殊鉴权
        return authService.authSpecialAppByMaintainer(username, resourceType, resourceId);
    }

    public AuthResult authFastExecuteScript(String username, AppResourceScope appResourceScope, ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        log.debug("Auth fast execute script, username:{}, appResourceScope:{}, hostInstances:{}", username,
            appResourceScope, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(
            username, ActionId.QUICK_EXECUTE_SCRIPT, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, servers, ip2HostIdMap, dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.QUICK_EXECUTE_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authFastPushFile(String username, AppResourceScope appResourceScope, ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        log.debug("Auth Fast transfer file, username:{}, appResourceScope:{}, hostInstances:{}", username,
            appResourceScope, hostInstanceList);
        boolean isAllowed = authHelper.isAllowed(
            username, ActionId.QUICK_TRANSFER_FILE, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass();
        }

        AuthResult authResult = AuthResult.fail();

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appResourceScope, servers,
            ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.QUICK_TRANSFER_FILE, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authExecuteAppScript(String username, AppResourceScope appResourceScope,
                                           String scriptId, String scriptName, ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO scriptInstance = buildExecutableInstance(appResourceScope, ResourceTypeEnum.SCRIPT, scriptId, null);

        log.debug("Auth execute script, username:{}, appResourceScope:{}, scriptId:{}, scriptInstance:{}, " +
                "hostInstances:{}",
            username,
            appResourceScope, scriptId, scriptInstance, hostInstanceList);
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

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, servers, ip2HostIdMap, dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.EXECUTE_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    private InstanceDTO buildExecutableInstance(AppResourceScope appResourceScope, ResourceTypeEnum resourceType,
                                                String resourceId,
                                                PathInfoDTO pathInfo) {
        InstanceDTO executeInstance = new InstanceDTO();
        executeInstance.setSystem(SystemId.JOB);
        executeInstance.setType(resourceType.getId());
        if (pathInfo == null) {
            executeInstance.setPath(buildAppScopePath(appResourceScope));
        } else {
            executeInstance.setPath(pathInfo);
        }
        executeInstance.setId(resourceId);
        return executeInstance;
    }

    public AuthResult authExecutePublicScript(String username, AppResourceScope appResourceScope,
                                              String scriptId, String scriptName, ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO scriptInstance = buildExecutableInstance(
            appResourceScope, ResourceTypeEnum.PUBLIC_SCRIPT, scriptId, null);

        log.debug("Auth execute public script, username:{}, appResourceScope:{}, scriptId:{}, scriptInstance:{}, " +
                "hostInstances:{}", username,
            appResourceScope, scriptId, scriptInstance, hostInstanceList);
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

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, servers, ip2HostIdMap, dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.EXECUTE_PUBLIC_SCRIPT, hostResources);
        log.debug("Auth execute script, authResult:{}", authResult);
        return authResult;
    }

    public AuthResult authExecutePlan(String username, AppResourceScope appResourceScope, Long templateId,
                                      Long planId, String planName, ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO planInstance = buildExecutableInstance(
            appResourceScope,
            ResourceTypeEnum.PLAN, planId.toString(),
            buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.TEMPLATE, templateId.toString()));

        log.debug("Auth execute plan, username:{}, appResourceScope:{}, planId:{}, planInstance:{}, hostInstances:{}",
            username, appResourceScope, planId, planInstance, hostInstanceList);
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

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, servers, ip2HostIdMap, dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.LAUNCH_JOB_PLAN, hostResources);
        log.debug("Auth execute plan, authResult:{}", authResult);
        return authResult;
    }

    @Override
    public AuthResult authDebugTemplate(String username, AppResourceScope appResourceScope, Long templateId,
                                        ServersDTO servers) {
        if (isMaintainerOfResource(
            username, IamUtil.getIamResourceTypeForResourceScope(appResourceScope), appResourceScope.getId())) {
            return AuthResult.pass();
        }
        Map<String, String> ip2HostIdMap = new HashMap<>();
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap = new HashMap<>();
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, servers, ip2HostIdMap,
            dynamicServerTopoNodeHierarchyMap);

        InstanceDTO jobTemplateInstance = buildExecutableInstance(appResourceScope, ResourceTypeEnum.TEMPLATE,
            templateId.toString(), null);

        log.debug("Auth execute job template, username:{}, appResourceScope:{}, planId:{}, templateInstance:{}, " +
            "hostInstances:{}", username, appResourceScope, templateId, jobTemplateInstance, hostInstanceList);
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

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, servers, ip2HostIdMap, dynamicServerTopoNodeHierarchyMap);
        authResult.addRequiredPermissions(ActionId.DEBUG_JOB_TEMPLATE, hostResources);
        log.debug("Auth execute job template, authResult:{}", authResult);
        return authResult;
    }

    private List<InstanceDTO> buildAppTopoNodeHostInstances(AppResourceScope appResourceScope) {
        List<InstanceDTO> topoNodeInstanceList = new ArrayList<>(1);
        InstanceDTO topoNodeInstance = new InstanceDTO();
        topoNodeInstance.setType(ResourceTypeEnum.HOST.getId());
        topoNodeInstance.setSystem(SystemId.CMDB);
        topoNodeInstance.setPath(buildAppScopePath(appResourceScope));
        topoNodeInstanceList.add(topoNodeInstance);
        return topoNodeInstanceList;
    }

    private List<InstanceDTO> buildBizStaticHostInstances(
        AppResourceScope appResourceScope,
        ServersDTO servers,
        Map<String, String> ip2HostIdMap
    ) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        Map<IpDTO, ServiceHostDTO> appHosts =
            hostService.batchGetHosts(servers.getStaticIpList());
        servers.getStaticIpList().forEach(hostIp -> {
            InstanceDTO hostInstance = new InstanceDTO();
            ServiceHostDTO host = appHosts.get(hostIp);
            if (host == null) {
                log.warn("Host: {} is not exist!", hostIp);
                throw new FailedPreconditionException(ErrorCode.SERVER_UNREGISTERED,
                    new Object[]{hostIp.getIp()});
            }
            String hostIdStr = host.getHostId().toString();
            hostInstance.setId(hostIdStr);
            hostInstance.setType(ResourceTypeEnum.HOST.getId());
            hostInstance.setSystem(SystemId.CMDB);
            hostInstance.setName(host.getIp());
            hostInstance.setPath(
                buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.HOST, hostIdStr));
            hostInstanceList.add(hostInstance);

            ip2HostIdMap.put(hostIp.convertToStrIp(), host.getHostId().toString());
        });
        return hostInstanceList;
    }

    private List<InstanceDTO> buildHostInstances(
        AppResourceScope appResourceScope,
        ServersDTO servers,
        Map<String, String> ip2HostIdMap,
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap
    ) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        // 静态IP
        if (!CollectionUtils.isEmpty(servers.getStaticIpList())) {
            switch (appResourceScope.getType()) {
                case BIZ:
                    hostInstanceList.addAll(buildBizStaticHostInstances(appResourceScope, servers, ip2HostIdMap));
                    break;
                case BIZ_SET:
                    InstanceDTO hostInstance = new InstanceDTO();
                    hostInstance.setType(ResourceTypeEnum.HOST.getId());
                    hostInstance.setSystem(SystemId.CMDB);
                    hostInstance.setPath(buildAppScopePath(appResourceScope));
                    hostInstanceList.add(hostInstance);
                    break;
                default:
                    throw new NotImplementedException(
                        "Unsupport appScopeType:" + appResourceScope.getType().getValue(),
                        ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
        // 动态topo节点
        if (!CollectionUtils.isEmpty(servers.getTopoNodes())) {
            // CMDB未提供权限中心使用的topo视图，暂时使用“业务”这个topo节点进行鉴权，不细化到集群、模块
            hostInstanceList.addAll(buildAppTopoNodeHostInstances(appResourceScope));
        }
        // 动态分组
        if (!CollectionUtils.isEmpty(servers.getDynamicServerGroups())) {
            servers.getDynamicServerGroups().forEach(serverGroup -> {
                InstanceDTO serverGroupInstance = new InstanceDTO();
                serverGroupInstance.setType(ResourceTypeEnum.HOST.getId());
                serverGroupInstance.setSystem(SystemId.CMDB);

                serverGroupInstance.setPath(
                    buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.DYNAMIC_GROUP,
                        serverGroup.getGroupId())
                );
                hostInstanceList.add(serverGroupInstance);
            });
        }
        return hostInstanceList;
    }

    private List<PermissionResource> convertBizStaticIpToPermissionResourceList(ServersDTO servers,
                                                                                Map<String, String> ip2HostIdMap) {
        List<PermissionResource> hostResources = new ArrayList<>();
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
        return hostResources;
    }

    /**
     * 获取业务/业务集资源的名称
     *
     * @param appResourceScope 资源范围
     * @return 资源名称
     */
    private String getResourceName(AppResourceScope appResourceScope) {
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        String resourceId = appResourceScope.getId();
        return resourceNameQueryService.getResourceName(resourceType, resourceId);
    }

    private List<PermissionResource> convertBizSetStaticIpToPermissionResourceList(AppResourceScope appResourceScope,
                                                                                   ServersDTO servers,
                                                                                   Map<String, String> ip2HostIdMap) {
        List<PermissionResource> hostResources = new ArrayList<>();
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(appResourceScope.getId());
        resource.setResourceType(ResourceTypeEnum.HOST);
        resource.setSubResourceType("biz_set");
        resource.setResourceName(getResourceName(appResourceScope));
        resource.setSystemId(SystemId.CMDB);
        resource.setType(ResourceTypeId.BUSINESS_SET);
        hostResources.add(resource);
        return hostResources;
    }

    private List<PermissionResource> convertTopoNodesToPermissionResourceList(AppResourceScope appResourceScope) {
        List<PermissionResource> hostResources = new ArrayList<>();
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(appResourceScope.getId());
        resource.setResourceType(ResourceTypeEnum.HOST);
        resource.setSubResourceType("topo");
        resource.setResourceName(getResourceName(appResourceScope));
        resource.setSystemId(SystemId.CMDB);
        resource.setType(CcNodeTypeEnum.BIZ.getType());
        resource.setParentHierarchicalResources(null);
        hostResources.add(resource);
        return hostResources;
    }

    private List<PermissionResource> convertDynamicGroupsToPermissionResourceList(AppResourceScope appResourceScope,
                                                                                  ServersDTO servers) {
        List<PermissionResource> hostResources = new ArrayList<>();
        servers.getDynamicServerGroups().forEach(serverGroup -> {
            PermissionResource resource = new PermissionResource();
            String groupId = serverGroup.getGroupId();
            resource.setResourceId(groupId);
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setSubResourceType("dynamic_group");
            resource.setType(ResourceTypeId.DYNAMIC_GROUP);
            resource.setResourceName(groupId);
            resource.setSystemId(SystemId.CMDB);
            resource.setParentHierarchicalResources(getDynamicGroupParentResources(appResourceScope));
            hostResources.add(resource);
        });
        return hostResources;
    }

    private List<PermissionResource> convertHostsToPermissionResourceList(
        AppResourceScope appResourceScope,
        ServersDTO servers,
        Map<String, String> ip2HostIdMap,
        Map<DynamicServerTopoNodeDTO, InstanceTopologyDTO> dynamicServerTopoNodeHierarchyMap
    ) {
        List<PermissionResource> hostResources = new ArrayList<>();

        if (!CollectionUtils.isEmpty(servers.getStaticIpList())) {
            switch (appResourceScope.getType()) {
                case BIZ:
                    hostResources.addAll(convertBizStaticIpToPermissionResourceList(servers, ip2HostIdMap));
                    break;
                case BIZ_SET:
                    hostResources.addAll(
                        convertBizSetStaticIpToPermissionResourceList(appResourceScope, servers, ip2HostIdMap));
                    break;
                default:
                    throw new NotImplementedException(
                        "Unsupport appScopeType:" + appResourceScope.getType().getValue(),
                        ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
        if (!CollectionUtils.isEmpty(servers.getTopoNodes())) {
            hostResources.addAll(convertTopoNodesToPermissionResourceList(appResourceScope));
        }
        if (!CollectionUtils.isEmpty(servers.getDynamicServerGroups())) {
            hostResources.addAll(convertDynamicGroupsToPermissionResourceList(appResourceScope, servers));
        }
        return hostResources;
    }

    private List<PermissionResource> getDynamicGroupParentResources(AppResourceScope appResourceScope) {
        PermissionResource appResource = buildAppResource(appResourceScope);
        return Collections.singletonList(appResource);
    }

    private PermissionResource buildAppResource(AppResourceScope appResourceScope) {
        ResourceTypeEnum resourceType = IamUtil.getIamResourceTypeForResourceScope(appResourceScope);
        String resourceId = appResourceScope.getId();
        PermissionResource appResource = new PermissionResource();
        appResource.setSystemId(SystemId.CMDB);
        appResource.setType(ResourceTypeEnum.BUSINESS.getId());
        appResource.setResourceType(resourceType);
        appResource.setResourceId(resourceId);
        appResource.setResourceName(resourceNameQueryService.getResourceName(resourceType, resourceId));
        return appResource;
    }

    @Override
    public AuthResult authViewTaskInstance(String username, AppResourceScope appResourceScope, long taskInstanceId) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null) {
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        if (username.equals(taskInstance.getOperator())) {
            return AuthResult.pass();
        }
        return appAuthService.auth(false, username, ActionId.VIEW_HISTORY, appResourceScope);
    }

    @Override
    public AuthResult authViewTaskInstance(String username, AppResourceScope appResourceScope,
                                           TaskInstanceDTO taskInstance) {
        if (username.equals(taskInstance.getOperator())) {
            return AuthResult.pass();
        }
        return appAuthService.auth(false, username, ActionId.VIEW_HISTORY, appResourceScope);
    }

    @Override
    public AuthResult authViewAllTaskInstance(String username, AppResourceScope appResourceScope) {
        return appAuthService.auth(false, username, ActionId.VIEW_HISTORY, appResourceScope);
    }

    @Override
    public AuthResult authAccountExecutable(String username, AppResourceScope appResourceScope, Long accountId) {
        if (!shouldAuthAccount(appResourceScope)) {
            return AuthResult.pass();
        }
        return authService.auth(false, username, ActionId.USE_ACCOUNT,
            ResourceTypeEnum.ACCOUNT, accountId.toString(), buildAppScopeResourcePath(
                appResourceScope, ResourceTypeEnum.ACCOUNT, accountId.toString()));
    }

    private boolean shouldAuthAccount(AppResourceScope appResourceScope) {
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
                    return grayAppIds.contains(appResourceScope.getAppId());
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

    private PathInfoDTO buildAppScopePath(AppResourceScope appResourceScope) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).build();
    }

    private PathInfoDTO buildAppScopeResourcePath(AppResourceScope appResourceScope,
                                                  ResourceTypeEnum resourceType,
                                                  String resourceId) {
        return PathBuilder.newBuilder(IamUtil.getIamResourceTypeIdForResourceScope(appResourceScope),
            appResourceScope.getId()).child(resourceType.getId(), resourceId).build();
    }

    @Override
    public AuthResult batchAuthAccountExecutable(String username, AppResourceScope appResourceScope,
                                                 Collection<Long> accountIds) {
        if (!shouldAuthAccount(appResourceScope)) {
            return AuthResult.pass();
        }
        List<PermissionResource> accountResources = accountIds.stream().map(accountId -> {
            PermissionResource accountResource = new PermissionResource();
            accountResource.setResourceId(accountId.toString());
            accountResource.setResourceType(ResourceTypeEnum.ACCOUNT);
            accountResource.setPathInfo(buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.ACCOUNT,
                accountId.toString()));
            return accountResource;
        }).collect(Collectors.toList());
        return appAuthService.batchAuthResources(username, ActionId.USE_ACCOUNT, appResourceScope,
            accountResources);
    }
}
