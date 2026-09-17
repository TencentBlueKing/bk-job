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
import com.tencent.bk.job.common.iam.util.IamAuthVersionHelper;
import com.tencent.bk.job.common.iam.util.IamUtil;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.execute.auth.ExecuteAuthDecision;
import com.tencent.bk.job.execute.auth.ExecuteAuthExecutor;
import com.tencent.bk.job.execute.auth.ExecuteAuthResource;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.dto.InstanceDTO;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 执行类鉴权的路由层：按灰度开关在 v3 / v4 执行器间选择，并负责无权限结果与申请信息的组装。
 *
 * <p>鉴权判定本身下沉到 {@link ExecuteAuthExecutor}（{@link V3ExecuteAuthExecutor} /
 * {@link V4ExecuteAuthExecutor}）；无权限详情的构造与版本无关，统一留在本层。</p>
 */
@Slf4j
@Service
public class ExecuteAuthServiceImpl implements ExecuteAuthService {

    private final ExecuteAuthExecutor v3Executor;
    private final ExecuteAuthExecutor v4Executor;
    private final ExecuteTargetInstanceBuilder executeTargetInstanceBuilder;
    private final ResourceNameQueryService resourceNameQueryService;
    private final AuthService authService;
    private final AppAuthService appAuthService;
    private final JobExecuteConfig jobExecuteConfig;

    @Autowired
    public ExecuteAuthServiceImpl(@Qualifier("v3ExecuteAuthExecutor")
                                  ExecuteAuthExecutor v3ExecuteAuthExecutor,
                                  @Qualifier("v4ExecuteAuthExecutor")
                                  ExecuteAuthExecutor v4ExecuteAuthExecutor,
                                  ExecuteTargetInstanceBuilder executeTargetInstanceBuilder,
                                  @Qualifier("jobExecuteResourceNameQueryService")
                                  ResourceNameQueryService resourceNameQueryService,
                                  AuthService authService,
                                  AppAuthService appAuthService,
                                  JobExecuteConfig jobExecuteConfig) {
        this.v3Executor = v3ExecuteAuthExecutor;
        this.v4Executor = v4ExecuteAuthExecutor;
        this.executeTargetInstanceBuilder = executeTargetInstanceBuilder;
        this.resourceNameQueryService = resourceNameQueryService;
        this.authService = authService;
        this.appAuthService = appAuthService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.authService.setResourceNameQueryService(resourceNameQueryService);
        this.appAuthService.setResourceNameQueryService(resourceNameQueryService);
    }

    private ExecuteAuthExecutor selectExecutor(User user) {
        return IamAuthVersionHelper.useV4(user.getUsername()) ? v4Executor : v3Executor;
    }

    @Override
    public AuthResult authFastExecuteScript(User user,
                                            AppResourceScope appResourceScope,
                                            ExecuteTargetDTO executeTarget) {
        return authExecute(user, ActionId.QUICK_EXECUTE_SCRIPT, appResourceScope, null, null, executeTarget);
    }

    @Override
    public AuthResult authFastPushFile(User user,
                                       AppResourceScope appResourceScope,
                                       ExecuteTargetDTO executeTarget) {
        return authExecute(user, ActionId.QUICK_TRANSFER_FILE, appResourceScope, null, null, executeTarget);
    }

    @Override
    public AuthResult authExecuteAppScript(User user,
                                           AppResourceScope appResourceScope,
                                           String scriptId,
                                           String scriptName,
                                           ExecuteTargetDTO executeTarget) {
        ExecuteAuthResource resource = ExecuteAuthResource.of(
            ResourceTypeEnum.SCRIPT, scriptId, ActionId.VIEW_SCRIPT);
        return authExecute(user, ActionId.EXECUTE_SCRIPT, appResourceScope, resource, scriptName, executeTarget);
    }

    @Override
    public AuthResult authExecutePublicScript(User user,
                                              AppResourceScope appResourceScope,
                                              String scriptId,
                                              String scriptName,
                                              ExecuteTargetDTO executeTarget) {
        // 公共脚本默认可见，无伴随的 view 校验
        ExecuteAuthResource resource = ExecuteAuthResource.of(
            ResourceTypeEnum.PUBLIC_SCRIPT, scriptId, null);
        return authExecute(
            user, ActionId.EXECUTE_PUBLIC_SCRIPT, appResourceScope, resource, scriptName, executeTarget);
    }

    @Override
    public AuthResult authExecutePlan(User user,
                                      AppResourceScope appResourceScope,
                                      Long templateId,
                                      Long planId,
                                      String planName,
                                      ExecuteTargetDTO executeTarget) {
        ExecuteAuthResource resource = ExecuteAuthResource.ofChild(
            ResourceTypeEnum.PLAN, planId.toString(),
            ResourceTypeEnum.TEMPLATE, templateId.toString(),
            ActionId.VIEW_JOB_PLAN);
        return authExecute(user, ActionId.LAUNCH_JOB_PLAN, appResourceScope, resource, planName, executeTarget);
    }

    @Override
    public AuthResult authDebugTemplate(User user,
                                        AppResourceScope appResourceScope,
                                        Long templateId,
                                        ExecuteTargetDTO executeTarget) {
        ExecuteAuthResource resource = ExecuteAuthResource.of(
            ResourceTypeEnum.TEMPLATE, templateId.toString(), ActionId.VIEW_JOB_TEMPLATE);
        return authExecute(user, ActionId.DEBUG_JOB_TEMPLATE, appResourceScope, resource, null, executeTarget);
    }

    /**
     * 执行类鉴权统一入口：判定下沉到版本执行器，未通过时在本层组装无权限详情。
     *
     * @param resourceName 资源展示名，为空时回查
     */
    private AuthResult authExecute(User user,
                                   String actionId,
                                   AppResourceScope appResourceScope,
                                   ExecuteAuthResource resource,
                                   String resourceName,
                                   ExecuteTargetDTO executeTarget) {
        ExecuteAuthDecision decision = selectExecutor(user)
            .authExecute(user, actionId, appResourceScope, resource, executeTarget);
        if (decision.isPass()) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);
        String failedActionId = decision.getFailedActionId();
        boolean needResource = resource != null
            && decision.getFailureScope() != ExecuteAuthDecision.FailureScope.EXECUTE_TARGET;
        boolean needExecuteTarget =
            decision.getFailureScope() != ExecuteAuthDecision.FailureScope.RESOURCE;

        if (needResource) {
            authResult.addRequiredPermission(failedActionId, buildResourcePermission(resource, resourceName));
        }
        if (needExecuteTarget) {
            authResult.addRequiredPermissions(failedActionId,
                convertHostsToPermissionResourceList(appResourceScope, executeTarget));
        }
        if (log.isDebugEnabled()) {
            log.debug("Auth execute denied, actionId:{}, decision:{}, authResult:{}",
                actionId, decision, authResult);
        }
        return authResult;
    }

    private PermissionResource buildResourcePermission(ExecuteAuthResource resource, String resourceName) {
        PermissionResource permissionResource = new PermissionResource();
        permissionResource.setSystemId(SystemId.JOB);
        permissionResource.setResourceId(resource.getResourceId());
        permissionResource.setResourceType(resource.getResourceType());
        permissionResource.setResourceName(StringUtils.isNotEmpty(resourceName) ? resourceName
            : resourceNameQueryService.getResourceName(resource.getResourceType(), resource.getResourceId()));
        return permissionResource;
    }

    private List<PermissionResource> convertBizStaticIpToPermissionResourceList(
        AppResourceScope appResourceScope, ExecuteTargetDTO executeTarget) {
        List<PermissionResource> hostResources = new ArrayList<>();
        List<PermissionResource> parentResources = Collections.singletonList(buildAppResource(appResourceScope));
        executeTarget.getStaticIpList().forEach(host -> {
            PermissionResource resource = new PermissionResource();
            resource.setResourceId(String.valueOf(host.getHostId()));
            resource.setResourceType(ResourceTypeEnum.HOST);
            resource.setSubResourceType("host");
            resource.setResourceName(host.getIp());
            resource.setSystemId(SystemId.CMDB);
            resource.setType("host");
            resource.setParentHierarchicalResources(parentResources);
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

    private List<PermissionResource> convertTenantSetStaticIpToPermissionResourceList(
        AppResourceScope appResourceScope) {
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

    private List<PermissionResource> convertTopoNodesToPermissionResourceList(
        AppResourceScope appResourceScope, List<DynamicServerTopoNodeDTO> topoNodes) {
        List<InstanceDTO> hostInstanceList =
            executeTargetInstanceBuilder.buildAppTopoNodeHostInstances(appResourceScope, topoNodes);
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

    private List<PermissionResource> convertDynamicGroupsToPermissionResourceList(
        AppResourceScope appResourceScope, ExecuteTargetDTO executeTarget) {
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
                    hostResources.addAll(convertBizStaticIpToPermissionResourceList(
                        appResourceScope, executeTarget));
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
        // 容器：静态容器与动态条件过滤器（containerFilters）共用业务级容器鉴权资源，去重后只构造一次
        if (!CollectionUtils.isEmpty(executeTarget.getStaticContainerList())
            || !CollectionUtils.isEmpty(executeTarget.getContainerFilters())) {
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
            ResourceTypeEnum.ACCOUNT, accountId.toString(), ExecuteIamPathBuilder.buildAppScopeResourcePath(
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
            accountResource.setPathInfo(ExecuteIamPathBuilder.buildAppScopeResourcePath(
                appResourceScope, ResourceTypeEnum.ACCOUNT, accountId.toString()));
            return accountResource;
        }).collect(Collectors.toList());
        return appAuthService.batchAuthResources(user, ActionId.USE_ACCOUNT, appResourceScope,
            accountResources);
    }

    @Override
    public AuthResult batchAuthViewFileSource(User user,
                                              AppResourceScope appResourceScope,
                                              Map<Integer, String> fileSourceIdToName) {
        if (MapUtils.isEmpty(fileSourceIdToName)) {
            return AuthResult.pass(user);
        }
        List<String> idList = fileSourceIdToName.keySet().stream()
            .map(String::valueOf)
            .collect(Collectors.toList());
        Set<String> allowedIdSet = new HashSet<>(appAuthService.batchAuth(user, ActionId.VIEW_FILE_SOURCE,
            appResourceScope, ResourceTypeEnum.FILE_SOURCE, idList));

        List<PermissionResource> deniedResources = new ArrayList<>();
        fileSourceIdToName.forEach((fileSourceId, fileSourceName) -> {
            String idStr = fileSourceId.toString();
            if (allowedIdSet.contains(idStr)) {
                return;
            }
            PermissionResource resource = new PermissionResource();
            resource.setSystemId(SystemId.JOB);
            resource.setResourceId(idStr);
            resource.setResourceType(ResourceTypeEnum.FILE_SOURCE);
            // 别名由上游查询时一并带回，此处不再回查：job-execute 的 ResourceNameQueryService 不认识 FILE_SOURCE
            resource.setResourceName(StringUtils.isNotEmpty(fileSourceName) ? fileSourceName : idStr);
            resource.setPathInfo(ExecuteIamPathBuilder.buildAppScopePath(appResourceScope));
            deniedResources.add(resource);
        });
        if (deniedResources.isEmpty()) {
            return AuthResult.pass(user);
        }

        AuthResult authResult = AuthResult.fail(user);
        authResult.addRequiredPermissions(ActionId.VIEW_FILE_SOURCE, deniedResources);
        if (log.isDebugEnabled()) {
            log.debug("Auth view file source, authResult:{}", authResult);
        }
        return authResult;
    }
}
