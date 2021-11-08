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

package com.tencent.bk.job.upgrader.task;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.EsbIamAction;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchPathResource;
import com.tencent.bk.job.common.iam.dto.EsbIamPathItem;
import com.tencent.bk.job.common.iam.dto.EsbIamSubject;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.jwt.BasicJwtManager;
import com.tencent.bk.job.common.util.jwt.JwtManager;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.client.IamClient;
import com.tencent.bk.job.upgrader.client.JobClient;
import com.tencent.bk.job.upgrader.iam.JobIamHelper;
import com.tencent.bk.job.upgrader.model.ActionPolicies;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.model.Policy;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import com.tencent.bk.sdk.iam.constants.SystemId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 账号使用权限迁移任务
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class)
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.3.5.0",
    targetExecuteTime = ExecuteTimeEnum.MAKE_UP)
public class UseAccountPermissionMigrationTask extends BaseUpgradeTask {

    private JobIamHelper jobIamHelper;
    private JobClient jobManageClient;

    private IamClient iamClient;
    private EsbIamClient esbIamClient;
    private List<AppInfo> appInfoList;
    private Map<Long, String> appInfoMap;

    private String getJobHostUrlByAddress(String address) {
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }
        return address;
    }

    public UseAccountPermissionMigrationTask(Properties properties) {
        super(properties);
    }

    @Override
    public void init() {
        jobIamHelper = new JobIamHelper(
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE),
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET),
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_IAM_BASE_URL),
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_ESB_SERVICE_URL)
        );
        String securityPublicKeyBase64 =
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
        String securityPrivateKeyBase64 =
            (String) getProperties().get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PRIVATE_KEY_BASE64);
        JwtManager jwtManager = null;
        try {
            jwtManager = new BasicJwtManager(securityPrivateKeyBase64, securityPublicKeyBase64);
        } catch (IOException | GeneralSecurityException e) {
            String msg = "Fail to generate jwt auth token";
            log.error(msg, e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR, msg);
        }
        String jobAuthToken = jwtManager.generateToken(60 * 60 * 1000);
        jobManageClient = new JobClient(
            getJobHostUrlByAddress((String) getProperties().get(ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS)),
            jobAuthToken
        );
        this.appInfoList = getAllNormalAppInfoFromManage();
        appInfoMap = new HashMap<>();
        appInfoList.forEach(appInfo -> {
            appInfoMap.put(appInfo.getId(), appInfo.getName());
        });
    }

    private IamClient getIamClient() {
        Properties properties = getProperties();
        if (iamClient == null) {
            iamClient = new IamClient(
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_IAM_BASE_URL),
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE),
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET)
            );
        }
        return iamClient;
    }

    private EsbIamClient getEsbIamClient() {
        Properties properties = getProperties();
        if (esbIamClient == null) {
            esbIamClient = new EsbIamClient(
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ESB_SERVICE_URL),
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE),
                (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET),
                false
            );
        }
        return esbIamClient;
    }

    private List<Policy> queryAuthorizedPolicies(String actionId) {
        ActionPolicies actionPolicies = getIamClient().getActionPolicies(actionId);
        return actionPolicies.getResults();
    }

    private List<AppInfo> getAllNormalAppInfoFromManage() {
        try {
            return jobManageClient.listNormalApps();
        } catch (Exception e) {
            log.error("Fail to get normal apps from job-manage, please confirm job-manage version>=3.3.5.0");
            throw e;
        }
    }

    /**
     * 根据策略计算出有权限的业务Id列表
     *
     * @param policy
     * @return
     */
    private List<Long> getAuthorizedAppIdList(Policy policy) {
        BusinessAuthHelper businessAuthHelper = jobIamHelper.businessAuthHelper();
        return businessAuthHelper.getAuthedAppIdList(
            null,
            policy.getExpression(),
            appInfoList.parallelStream().map(AppInfo::getId).collect(Collectors.toList())
        );
    }

    /**
     * 根据业务ID获取业务名称
     *
     * @param appId
     * @return
     */
    private String getAppNameById(Long appId) {
        if (appInfoMap.containsKey(appId)) return appInfoMap.get(appId);
        return null;
    }

    private boolean authByPolicy(Policy policy) {
        if ("any".equals(policy.getExpression().getOperator().getOperator())) {
            // 授予任意业务任意账号权限
            log.info("auth any biz permission to {}", policy.getSubject());
            return authAnyBizUseAccountByPolicy(policy);
        } else {
            List<Long> appIdList = getAuthorizedAppIdList(policy);
            log.info("auth {} permission to {}", appIdList, policy.getSubject());
            // 授予业务下任意账号权限
            return authBizUseAccountByPolicy(policy, appIdList);
        }
    }

    private List<EsbIamAction> getUseAccountActions() {
        EsbIamAction esbIamAction = new EsbIamAction();
        esbIamAction.setId(ActionId.USE_ACCOUNT);
        List<EsbIamAction> actions = new ArrayList<>();
        actions.add(esbIamAction);
        return actions;
    }

    private EsbIamSubject getSubjectByPolicy(Policy policy) {
        EsbIamSubject esbIamSubject = new EsbIamSubject();
        esbIamSubject.setId(policy.getSubject().getId());
        esbIamSubject.setType(policy.getSubject().getType());
        return esbIamSubject;
    }

    /**
     * 授予任意业务下任意执行账号使用的权限
     *
     * @param policy 权限策略
     * @return 是否授权成功
     */
    private boolean authAnyBizUseAccountByPolicy(Policy policy) {
        EsbIamBatchPathResource esbIamBatchPathResource = new EsbIamBatchPathResource();
        esbIamBatchPathResource.setSystem(SystemId.JOB);
        esbIamBatchPathResource.setType(ResourceTypeEnum.ACCOUNT.getId());
        esbIamBatchPathResource.setPaths(new ArrayList<>());
        return batchAuth(policy, getUseAccountActions(), getSubjectByPolicy(policy), esbIamBatchPathResource);
    }

    /**
     * 授予某些业务下任意执行账号使用的权限
     *
     * @param policy 权限策略
     * @return 是否授权成功
     */
    private boolean authBizUseAccountByPolicy(Policy policy, List<Long> appIdList) {
        EsbIamBatchPathResource esbIamBatchPathResource = new EsbIamBatchPathResource();
        esbIamBatchPathResource.setSystem(SystemId.JOB);
        esbIamBatchPathResource.setType(ResourceTypeEnum.ACCOUNT.getId());
        List<List<EsbIamPathItem>> pathList = new ArrayList<>();
        for (Long appId : appIdList) {
            List<EsbIamPathItem> esbIamPathItemList = new ArrayList<>();
            EsbIamPathItem pathItem = new EsbIamPathItem();
            pathItem.setType(ResourceTypeEnum.BUSINESS.getId());
            pathItem.setId(appId.toString());
            pathItem.setName(getAppNameById(appId));
            esbIamPathItemList.add(pathItem);
            pathList.add(esbIamPathItemList);
        }
        esbIamBatchPathResource.setPaths(pathList);
        return batchAuth(policy, getUseAccountActions(), getSubjectByPolicy(policy), esbIamBatchPathResource);
    }

    /**
     * 调用权限中心接口进行批量授权
     *
     * @param policy                  权限策略
     * @param actions                 授权操作列表
     * @param esbIamSubject           授权对象
     * @param esbIamBatchPathResource 批量资源路径
     * @return 是否授权成功
     */
    private boolean batchAuth(
        Policy policy,
        List<EsbIamAction> actions,
        EsbIamSubject esbIamSubject,
        EsbIamBatchPathResource esbIamBatchPathResource
    ) {
        List<EsbIamBatchPathResource> resourceList = new ArrayList<>();
        resourceList.add(esbIamBatchPathResource);
        try {
            List<EsbIamBatchAuthedPolicy> batchAuthedPolicy = getEsbIamClient().batchAuthByPath(actions,
                esbIamSubject, resourceList, policy.getExpiredAt());
            log.info("batchAuthedPolicy={}", JsonUtils.toJson(batchAuthedPolicy));
            return true;
        } catch (Exception e) {
            log.error("Fail to auth subject {} to {}", JsonUtils.toJson(policy.getSubject()), policy.getExpiredAt(), e);
            return false;
        }
    }

    public void printSeparateLine() {
        log.info("==================================================");
    }

    public void showPolicies(List<Policy> policies) {
        policies.forEach(policy -> {
            log.info("{}: {} expiredAt {}, expression:{}", policy.getId(),
                policy.getSubject().getType() + ":" + policy.getSubject().getName(),
                policy.getExpiredAt(), JsonUtils.toJson(policy.getExpression()));
        });
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        String oldActionId = ActionId.LIST_BUSINESS;
        // 1.旧权限数据读取
        List<Policy> oldAuthorizedPolicies = queryAuthorizedPolicies(oldActionId);
        printSeparateLine();
        log.info("oldAuthorizedPolicies of {}:", oldActionId);
        showPolicies(oldAuthorizedPolicies);
        printSeparateLine();
        log.info("Begin to auth according to oldPolicies:");
        // 2.新权限数据授权
        oldAuthorizedPolicies.forEach(policy -> {
            log.info(
                "auth {}:{}:{}:{}",
                policy.getSubject().getType(),
                policy.getSubject().getName(),
                policy.getExpiredAt(),
                authByPolicy(policy));
        });
        // 3.新权限策略查询
        String newActionId = ActionId.USE_ACCOUNT;
        List<Policy> newAuthorizedPolicies = queryAuthorizedPolicies(newActionId);
        printSeparateLine();
        log.info("newAuthorizedPolicies:");
        showPolicies(newAuthorizedPolicies);
        printSeparateLine();
        return 0;
    }
}
