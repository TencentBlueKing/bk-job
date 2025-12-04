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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.FeatureToggleModeEnum;
import com.tencent.bk.job.common.exception.NotImplementedException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.model.PermissionResource;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.ResourceNameQueryService;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
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
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final AuthService authService;
    private final AppAuthService appAuthService;
    private final JobExecuteConfig jobExecuteConfig;
    private final TopoService topoService;

    @Autowired
    public ExecuteAuthServiceImpl(AuthHelper authHelper,
                                  @Qualifier("jobExecuteResourceNameQueryService")
                                  ResourceNameQueryService resourceNameQueryService,
                                  AuthService authService,
                                  AppAuthService appAuthService,
                                  JobExecuteConfig jobExecuteConfig,
                                  TopoService topoService) {
        this.authHelper = authHelper;
        this.resourceNameQueryService = resourceNameQueryService;
        this.authService = authService;
        this.appAuthService = appAuthService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.topoService = topoService;
        this.authService.setResourceNameQueryService(resourceNameQueryService);
        this.appAuthService.setResourceNameQueryService(resourceNameQueryService);
    }

    public AuthResult authFastExecuteScript(User user,
                                            AppResourceScope appResourceScope,
                                            ExecuteTargetDTO executeTarget) {

        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);
        if (log.isDebugEnabled()) {
            log.debug("Auth fast execute script, username:{}, appResourceScope:{}, hostInstances:{}",
                user.getUsername(), appResourceScope, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(),
            user.getUsername(), ActionId.QUICK_EXECUTE_SCRIPT, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.QUICK_EXECUTE_SCRIPT, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute script, authResult:{}", authResult);
        }
        return authResult;
    }

    public AuthResult authFastPushFile(User user,
                                       AppResourceScope appResourceScope,
                                       ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);
        if (log.isDebugEnabled()) {
            log.debug("Auth Fast transfer file, username:{}, appResourceScope:{}, hostInstances:{}", user.getUsername(),
                appResourceScope, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(),
            user.getUsername(), ActionId.QUICK_TRANSFER_FILE, null, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.QUICK_TRANSFER_FILE, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute script, authResult:{}", authResult);
        }
        return authResult;
    }

    public AuthResult authExecuteAppScript(User user, AppResourceScope appResourceScope,
                                           String scriptId, String scriptName, ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);

        InstanceDTO scriptInstance = buildExecutableInstance(appResourceScope, ResourceTypeEnum.SCRIPT, scriptId, null);

        if (log.isDebugEnabled()) {
            log.debug("Auth execute script, username:{}, appResourceScope:{}, scriptId:{}, scriptInstance:{}, " +
                    "hostInstances:{}",
                user.getUsername(), appResourceScope, scriptId, scriptInstance, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            ActionId.EXECUTE_SCRIPT, scriptInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

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
            appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.EXECUTE_SCRIPT, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute script, authResult:{}", authResult);
        }
        return authResult;
    }

    private InstanceDTO buildExecutableInstance(AppResourceScope appResourceScope,
                                                ResourceTypeEnum resourceType,
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

    public AuthResult authExecutePublicScript(User user,
                                              AppResourceScope appResourceScope,
                                              String scriptId,
                                              String scriptName,
                                              ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);

        InstanceDTO scriptInstance = buildExecutableInstance(
            appResourceScope, ResourceTypeEnum.PUBLIC_SCRIPT, scriptId, null);

        if (log.isDebugEnabled()) {
            log.debug("Auth execute public script, username:{}, appResourceScope:{}, scriptId:{}, scriptInstance:{}, " +
                "hostInstances:{}", user.getUsername(), appResourceScope, scriptId, scriptInstance, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            ActionId.EXECUTE_PUBLIC_SCRIPT, scriptInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

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
            appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.EXECUTE_PUBLIC_SCRIPT, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute script, authResult:{}", authResult);
        }
        return authResult;
    }

    public AuthResult authExecutePlan(User user,
                                      AppResourceScope appResourceScope,
                                      Long templateId,
                                      Long planId,
                                      String planName,
                                      ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);

        InstanceDTO planInstance = buildExecutableInstance(
            appResourceScope,
            ResourceTypeEnum.PLAN,
            planId.toString(),
            buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.TEMPLATE, templateId.toString()));

        if (log.isDebugEnabled()) {
            log.debug("Auth execute plan, username:{}, appResourceScope:{}, planId:{}, planInstance:{}," +
                    " hostInstances:{}",
                user.getUsername(), appResourceScope, planId, planInstance, hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(),
            ActionId.LAUNCH_JOB_PLAN, planInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

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
            appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.LAUNCH_JOB_PLAN, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute plan, authResult:{}", authResult);
        }
        return authResult;
    }

    @Override
    public AuthResult authDebugTemplate(User user,
                                        AppResourceScope appResourceScope,
                                        Long templateId,
                                        ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = buildHostInstances(appResourceScope, executeTarget);

        InstanceDTO jobTemplateInstance = buildExecutableInstance(appResourceScope, ResourceTypeEnum.TEMPLATE,
            templateId.toString(), null);

        if (log.isDebugEnabled()) {
            log.debug("Auth execute job template, username:{}, appResourceScope:{}, planId:{}, templateInstance:{}, " +
                    "hostInstances:{}", user.getUsername(), appResourceScope, templateId, jobTemplateInstance,
                hostInstanceList);
        }
        boolean isAllowed = authHelper.isAllowed(user.getTenantId(), user.getUsername(), ActionId.DEBUG_JOB_TEMPLATE,
            jobTemplateInstance, hostInstanceList);

        if (isAllowed) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);

        PermissionResource jobTemplateResource = new PermissionResource();
        jobTemplateResource.setSystemId(SystemId.JOB);
        jobTemplateResource.setResourceId(templateId.toString());
        jobTemplateResource.setResourceType(ResourceTypeEnum.TEMPLATE);
        jobTemplateResource.setResourceName(resourceNameQueryService.getResourceName(ResourceTypeEnum.TEMPLATE,
            templateId.toString()));
        authResult.addRequiredPermission(ActionId.DEBUG_JOB_TEMPLATE, jobTemplateResource);

        List<PermissionResource> hostResources = convertHostsToPermissionResourceList(
            appResourceScope, executeTarget);
        authResult.addRequiredPermissions(ActionId.DEBUG_JOB_TEMPLATE, hostResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth execute job template, authResult:{}", authResult);
        }
        return authResult;
    }

    private List<InstanceDTO> buildAppTopoNodeHostInstances(AppResourceScope appResourceScope,
                                                            List<DynamicServerTopoNodeDTO> topoNodes) {
        long bizId = Long.parseLong(appResourceScope.getId());
        List<InstanceTopologyDTO> topoNodeTopologyList = topoService.batchGetTopoNodeHierarchy(bizId, topoNodes);
        Map<String, InstanceTopologyDTO> topoNodeTopologyMap = new HashMap<>();
        for (InstanceTopologyDTO instanceTopologyDTO : topoNodeTopologyList) {
            topoNodeTopologyMap.put(instanceTopologyDTO.getUniqueKey(), instanceTopologyDTO);
        }
        List<InstanceDTO> topoNodeInstanceList = new ArrayList<>(topoNodes.size());
        for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
            String topoNodeKey = topoNode.getUniqueKey();
            InstanceTopologyDTO nodeTopology = topoNodeTopologyMap.get(topoNodeKey);
            if (nodeTopology == null) {
                log.info("Cannot find topoPath for node {}, ignore", topoNodeKey);
                continue;
            }
            InstanceDTO topoNodeInstance = new InstanceDTO();
            topoNodeInstance.setName(String.valueOf(topoNode.getTopoNodeId()));
            topoNodeInstance.setType(ResourceTypeEnum.HOST.getId());
            topoNodeInstance.setSystem(SystemId.CMDB);
            topoNodeInstance.setPath(buildIamPathForTopoNode(nodeTopology));
            topoNodeInstanceList.add(topoNodeInstance);
        }
        return topoNodeInstanceList;
    }

    private PathInfoDTO buildIamPathForTopoNode(InstanceTopologyDTO nodeTopology) {
        List<InstanceTopologyDTO> parents = nodeTopology.getParents();
        if (parents == null) {
            parents = Collections.emptyList();
        }
        List<InstanceTopologyDTO> pathNodeList = new ArrayList<>(parents);
        pathNodeList.add(nodeTopology);
        // 权限路径不支持自定义节点，过滤掉
        pathNodeList = pathNodeList.stream().filter(parent ->
            CcNodeTypeEnum.BIZ.getType().equals(parent.getObjectId())
                || CcNodeTypeEnum.SET.getType().equals(parent.getObjectId())
                || CcNodeTypeEnum.MODULE.getType().equals(parent.getObjectId())
        ).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(pathNodeList)) {
            return null;
        }
        return buildPathInfoDTO(pathNodeList);
    }

    private PathInfoDTO buildPathInfoDTO(List<InstanceTopologyDTO> pathNodeList) {
        PathInfoDTO rootPathInfo = null;
        PathInfoDTO endPathInfo = null;
        for (InstanceTopologyDTO pathNode : pathNodeList) {
            if (rootPathInfo == null) {
                rootPathInfo = new PathInfoDTO();
                rootPathInfo.setType(pathNode.getObjectId());
                rootPathInfo.setId(pathNode.getInstanceId().toString());
                endPathInfo = rootPathInfo;
            } else {
                PathInfoDTO childPathInfo = new PathInfoDTO();
                childPathInfo.setType(pathNode.getObjectId());
                childPathInfo.setId(pathNode.getInstanceId().toString());
                endPathInfo.setChild(childPathInfo);
                endPathInfo = childPathInfo;
            }
        }
        return rootPathInfo;
    }

    private List<InstanceDTO> buildBizStaticHostInstances(AppResourceScope appResourceScope,
                                                          ExecuteTargetDTO executeTarget) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        executeTarget.getStaticIpList().forEach(host -> {
            InstanceDTO hostInstance = new InstanceDTO();
            String hostIdStr = host.getHostId().toString();
            hostInstance.setId(hostIdStr);
            hostInstance.setType(ResourceTypeEnum.HOST.getId());
            hostInstance.setSystem(SystemId.CMDB);
            hostInstance.setName(host.getIp());
            hostInstance.setPath(
                buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.HOST, hostIdStr));
            hostInstanceList.add(hostInstance);
        });
        return hostInstanceList;
    }

    private List<InstanceDTO> buildHostInstances(AppResourceScope appResourceScope,
                                                 ExecuteTargetDTO executeObjects) {
        List<InstanceDTO> hostInstanceList = new ArrayList<>();
        // 静态IP
        if (!CollectionUtils.isEmpty(executeObjects.getStaticIpList())) {
            switch (appResourceScope.getType()) {
                case BIZ:
                    hostInstanceList.addAll(buildBizStaticHostInstances(appResourceScope, executeObjects));
                    break;
                case BIZ_SET:
                case TENANT_SET:
                    InstanceDTO hostInstance = new InstanceDTO();
                    hostInstance.setType(ResourceTypeEnum.HOST.getId());
                    hostInstance.setSystem(SystemId.CMDB);
                    hostInstance.setPath(buildAppScopePath(appResourceScope));
                    hostInstanceList.add(hostInstance);
                    break;
                default:
                    throw new NotImplementedException(
                        "Unsupported appScopeType:" + appResourceScope.getType().getValue(),
                        ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
        // 动态topo节点
        List<DynamicServerTopoNodeDTO> topoNodes = executeObjects.getTopoNodes();
        if (!CollectionUtils.isEmpty(topoNodes)) {
            hostInstanceList.addAll(buildAppTopoNodeHostInstances(appResourceScope, topoNodes));
        }
        // 动态分组
        if (!CollectionUtils.isEmpty(executeObjects.getDynamicServerGroups())) {
            executeObjects.getDynamicServerGroups().forEach(serverGroup -> {
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
        // 静态容器
        if (!CollectionUtils.isEmpty(executeObjects.getStaticContainerList())) {
            // 静态容器按照业务鉴权
            InstanceDTO hostInstance = new InstanceDTO();
            hostInstance.setType(ResourceTypeEnum.HOST.getId());
            hostInstance.setSystem(SystemId.CMDB);
            hostInstance.setPath(buildAppScopePath(appResourceScope));
            hostInstanceList.add(hostInstance);
        }
        return hostInstanceList;
    }

    private List<PermissionResource> convertBizStaticIpToPermissionResourceList(ExecuteTargetDTO executeTarget) {
        List<PermissionResource> hostResources = new ArrayList<>();
        executeTarget.getStaticIpList().forEach(host -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(String.valueOf(host.getHostId()));
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setSubResourceType("host");
            resource.setResourceName(host.getIp());
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

    private List<PermissionResource> convertBizSetStaticIpToPermissionResourceList(AppResourceScope appResourceScope) {
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

    private List<PermissionResource> convertTenantSetStaticIpToPermissionResourceList(AppResourceScope appResourceScope) {
        List<PermissionResource> hostResources = new ArrayList<>();
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(appResourceScope.getId());
        resource.setResourceType(ResourceTypeEnum.HOST);
        resource.setSubResourceType("tenant_set");
        resource.setResourceName(getResourceName(appResourceScope));
        resource.setSystemId(SystemId.CMDB);
        resource.setType(ResourceTypeId.TENANT_SET);
        hostResources.add(resource);
        return hostResources;
    }

    private List<PermissionResource> convertTopoNodesToPermissionResourceList(AppResourceScope appResourceScope,
                                                                              List<DynamicServerTopoNodeDTO> topoNodes) {
        List<InstanceDTO> hostInstanceList = buildAppTopoNodeHostInstances(appResourceScope, topoNodes);
        List<PermissionResource> finalPermissionResourceList = new ArrayList<>();
        for (InstanceDTO instanceDTO : hostInstanceList) {
            List<PermissionResource> permissionResourceList = convert(instanceDTO.getPath());
            if (CollectionUtils.isEmpty(permissionResourceList)) {
                continue;
            }
            int lastNodeIndex = permissionResourceList.size() - 1;
            PermissionResource lastNode = permissionResourceList.get(lastNodeIndex);
            permissionResourceList.remove(lastNodeIndex);
            lastNode.setParentHierarchicalResources(permissionResourceList);
            finalPermissionResourceList.add(lastNode);
        }
        return finalPermissionResourceList;
    }

    private List<PermissionResource> convert(PathInfoDTO pathInfoDTO) {
        List<PermissionResource> permissionResourceList = new ArrayList<>();
        if (pathInfoDTO == null) {
            return permissionResourceList;
        }
        PathInfoDTO currentNode = pathInfoDTO;
        while (currentNode != null) {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(currentNode.getId());
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setSubResourceType("topo");
            resource.setResourceName(currentNode.getType() + "_" + currentNode.getId());
            resource.setSystemId(SystemId.CMDB);
            resource.setType(currentNode.getType());
            permissionResourceList.add(resource);
            currentNode = currentNode.getChild();
        }
        return permissionResourceList;
    }

    private List<PermissionResource> convertDynamicGroupsToPermissionResourceList(AppResourceScope appResourceScope,
                                                                                  ExecuteTargetDTO executeTarget) {
        List<PermissionResource> hostResources = new ArrayList<>();
        executeTarget.getDynamicServerGroups().forEach(serverGroup -> {
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

    private List<PermissionResource> convertContainersToPermissionResourceList(AppResourceScope appResourceScope) {
        List<PermissionResource> hostResources = new ArrayList<>();
        PermissionResource resource = new PermissionResource();
        resource.setResourceId(appResourceScope.getId());
        resource.setResourceType(ResourceTypeEnum.HOST);
        resource.setSubResourceType("container");
        resource.setResourceName(getResourceName(appResourceScope));
        resource.setSystemId(SystemId.CMDB);
        resource.setType(CcNodeTypeEnum.BIZ.getType());
        resource.setParentHierarchicalResources(null);
        hostResources.add(resource);
        return hostResources;
    }

    private List<PermissionResource> convertHostsToPermissionResourceList(AppResourceScope appResourceScope,
                                                                          ExecuteTargetDTO executeTarget) {
        List<PermissionResource> hostResources = new ArrayList<>();

        // 静态IP
        if (!CollectionUtils.isEmpty(executeTarget.getStaticIpList())) {
            switch (appResourceScope.getType()) {
                case BIZ:
                    hostResources.addAll(convertBizStaticIpToPermissionResourceList(executeTarget));
                    break;
                case BIZ_SET:
                    hostResources.addAll(
                        convertBizSetStaticIpToPermissionResourceList(appResourceScope));
                    break;
                case TENANT_SET:
                    hostResources.addAll(
                        convertTenantSetStaticIpToPermissionResourceList(appResourceScope));
                    break;
                default:
                    throw new NotImplementedException(
                        "Unsupported appScopeType:" + appResourceScope.getType().getValue(),
                        ErrorCode.NOT_SUPPORT_FEATURE);
            }
        }
        // 动态topo节点
        List<DynamicServerTopoNodeDTO> topoNodes = executeTarget.getTopoNodes();
        if (CollectionUtils.isNotEmpty(topoNodes)) {
            hostResources.addAll(convertTopoNodesToPermissionResourceList(appResourceScope, topoNodes));
        }
        // 动态分组
        if (!CollectionUtils.isEmpty(executeTarget.getDynamicServerGroups())) {
            hostResources.addAll(convertDynamicGroupsToPermissionResourceList(appResourceScope, executeTarget));
        }
        // 静态容器
        if (!CollectionUtils.isEmpty(executeTarget.getStaticContainerList())) {
            hostResources.addAll(convertContainersToPermissionResourceList(appResourceScope));
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
    public void authViewTaskInstance(User user,
                                     AppResourceScope appResourceScope,
                                     TaskInstanceDTO taskInstance) throws PermissionDeniedException {
        if (user.getUsername().equals(taskInstance.getOperator())) {
            return;
        }
        AuthResult authResult = appAuthService.auth(user, ActionId.VIEW_HISTORY, appResourceScope);
        authResult.denyIfNoPermission();
    }

    @Override
    public AuthResult checkViewTaskInstancePermission(User user,
                                                      AppResourceScope appResourceScope,
                                                      TaskInstanceDTO taskInstance) {
        if (user.getUsername().equals(taskInstance.getOperator())) {
            return AuthResult.pass(user);
        }
        return appAuthService.auth(user, ActionId.VIEW_HISTORY, appResourceScope);
    }

    @Override
    public AuthResult authViewAllTaskInstance(User user, AppResourceScope appResourceScope) {
        return appAuthService.auth(user, ActionId.VIEW_HISTORY, appResourceScope);
    }

    @Override
    public AuthResult authAccountExecutable(User user, AppResourceScope appResourceScope, Long accountId) {
        if (!shouldAuthAccount(appResourceScope)) {
            return AuthResult.pass(user);
        }
        return authService.auth(user, ActionId.USE_ACCOUNT,
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
    public AuthResult batchAuthAccountExecutable(User user,
                                                 AppResourceScope appResourceScope,
                                                 Collection<Long> accountIds) {
        if (!shouldAuthAccount(appResourceScope)) {
            return AuthResult.pass(user);
        }
        List<PermissionResource> accountResources = accountIds.stream().map(accountId -> {
            PermissionResource accountResource = new PermissionResource();
            accountResource.setResourceId(accountId.toString());
            accountResource.setResourceType(ResourceTypeEnum.ACCOUNT);
            accountResource.setPathInfo(buildAppScopeResourcePath(appResourceScope, ResourceTypeEnum.ACCOUNT,
                accountId.toString()));
            return accountResource;
        }).collect(Collectors.toList());
        return appAuthService.batchAuthResources(user, ActionId.USE_ACCOUNT, appResourceScope,
            accountResources);
    }
}
