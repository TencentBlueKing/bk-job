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
import com.tencent.bk.job.common.iam.dto.*;
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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * 账号使用权限迁移任务
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class)
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.5.0.0",
    targetExecuteTime = ExecuteTimeEnum.MAKE_UP)
public class BizSetAuthMigrationTask extends BaseUpgradeTask {

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

    public BizSetAuthMigrationTask(Properties properties) {
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
            throw new InternalException(msg, e, ErrorCode.INTERNAL_ERROR);
        }
        String jobAuthToken = jwtManager.generateToken(60 * 60 * 1000);
        jobManageClient = new JobClient(
            getJobHostUrlByAddress((String) getProperties().get(ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS)),
            jobAuthToken
        );
        this.appInfoList = getAllBizSetAppInfoFromManage();
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

    private List<AppInfo> getAllBizSetAppInfoFromManage() {
        try {
            return jobManageClient.listBizSetApps();
        } catch (Exception e) {
            log.error("Fail to get normal apps from job-manage, please confirm job-manage version>=3.3.5.0");
            throw e;
        }
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
        // 1.权限模板数据读取  TODO
        // List<EsbIamBatchPathResource> resourceList=JsonUtils.fromJson(authTemplateStr, new TypeReference<List<EsbIamBatchPathResource>>() {
        //});
        printSeparateLine();

        printSeparateLine();
        log.info("Begin to auth according to oldPolicies:");
        // 2.业务集运维人员授权  TODO

        // 3.新权限策略查询  TODO

        printSeparateLine();
        log.info("newAuthorizedPolicies:");
        //showPolicies(newAuthorizedPolicies);
        printSeparateLine();
        return 0;
    }
}
