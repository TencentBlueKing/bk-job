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

import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.dto.EsbIamAction;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchPathResource;
import com.tencent.bk.job.common.iam.dto.EsbIamSubject;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.client.IamClient;
import com.tencent.bk.job.upgrader.model.ActionPolicies;
import com.tencent.bk.job.upgrader.model.Policy;
import com.tencent.bk.sdk.iam.constants.SystemId;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 管理公共脚本权限迁移任务
 */
@Slf4j
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.3.4.0",
    targetExecuteTime = ExecuteTimeEnum.BEFORE_UPDATE_JOB)
public class ManagePublicScriptPermissionMigrationTask extends BaseUpgradeTask {

    private IamClient iamClient;
    private EsbIamClient esbIamClient;

    public ManagePublicScriptPermissionMigrationTask(Properties properties) {
        super(properties);
    }

    private IamClient getIamClient() {
        Properties properties = getProperties();
        if (iamClient == null) {
            iamClient = new IamClient(
                (String) properties.get("iam.base-url"),
                (String) properties.get("app.code"),
                (String) properties.get("app.secret")
            );
        }
        return iamClient;
    }

    private EsbIamClient getEsbIamClient() {
        Properties properties = getProperties();
        if (esbIamClient == null) {
            esbIamClient = new EsbIamClient(
                (String) properties.get("esb.service.url"),
                (String) properties.get("app.code"),
                (String) properties.get("app.secret"),
                false
            );
        }
        return esbIamClient;
    }

    private List<Policy> queryAuthorizedPolicies(String actionId) {
        ActionPolicies actionPolicies = getIamClient().getActionPolicies(actionId);
        return actionPolicies.getResults();
    }

    private boolean authByPolicy(Policy policy) {
        EsbIamAction esbIamAction = new EsbIamAction();
        esbIamAction.setId(ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE);
        List<EsbIamAction> actions = new ArrayList<>();
        actions.add(esbIamAction);
        EsbIamSubject esbIamSubject = new EsbIamSubject();
        esbIamSubject.setId(policy.getSubject().getId());
        esbIamSubject.setType(policy.getSubject().getType());
        List<EsbIamBatchPathResource> resourceList = new ArrayList<>();
        EsbIamBatchPathResource esbIamBatchPathResource = new EsbIamBatchPathResource();
        esbIamBatchPathResource.setSystem(SystemId.JOB);
        esbIamBatchPathResource.setType(ResourceTypeEnum.PUBLIC_SCRIPT.getId());
        esbIamBatchPathResource.setPaths(new ArrayList<>());
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
        String oldActionId = "manage_public_script";
        // 1.旧权限数据读取
        List<Policy> oldAuthorizedPolicies = queryAuthorizedPolicies(oldActionId);
        printSeparateLine();
        log.info("oldAuthorizedPolicies:");
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
        String newActionId = ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE;
        List<Policy> newAuthorizedPolicies = queryAuthorizedPolicies(newActionId);
        printSeparateLine();
        log.info("newAuthorizedPolicies:");
        showPolicies(newAuthorizedPolicies);
        printSeparateLine();
        return 0;
    }
}
