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

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.tencent.bk.job.common.iam.client.EsbIamClient;
import com.tencent.bk.job.common.iam.dto.BatchAuthByPathReq;
import com.tencent.bk.job.common.iam.dto.EsbIamAction;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchAuthedPolicy;
import com.tencent.bk.job.common.iam.dto.EsbIamBatchPathResource;
import com.tencent.bk.job.common.iam.dto.EsbIamPathItem;
import com.tencent.bk.job.common.iam.dto.EsbIamSubject;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.client.JobClient;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
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
    targetVersion = "3.5.0",
    targetExecuteTime = ExecuteTimeEnum.MAKE_UP)
public class BizSetAuthMigrationTask extends BaseUpgradeTask {

    private static final String PLACEHOLDER_BIZ_SET_ID = "{{biz_set_id}}";
    private static final String PLACEHOLDER_BIZ_SET_NAME = "{{biz_set_name}}";

    private JobClient jobManageClient;
    private EsbIamClient esbIamClient;
    private List<AppInfo> bizSetAppInfoList;
    private Map<Long, String> bizSetAppInfoMap;

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
        String jobAuthToken = getJobAuthToken();
        jobManageClient = new JobClient(
            getJobHostUrlByAddress((String) getProperties().get(ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS)),
            jobAuthToken
        );
        this.bizSetAppInfoList = getAllBizSetAppInfoFromManage();
        bizSetAppInfoMap = new HashMap<>();
        bizSetAppInfoList.forEach(appInfo -> {
            bizSetAppInfoMap.put(appInfo.getId(), appInfo.getName());
        });
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

    private List<AppInfo> getAllBizSetAppInfoFromManage() {
        try {
            return jobManageClient.listBizSetApps();
        } catch (Exception e) {
            log.error("Fail to get normal apps from job-manage, please confirm job-manage version>=3.3.5.0");
            throw e;
        }
    }

    /**
     * 调用权限中心接口进行批量授权
     *
     * @param esbIamSubject 授权对象
     * @param actions       授权操作列表
     * @param resourceList  批量依赖资源路径
     * @return 是否授权成功
     */
    private boolean batchAuth(EsbIamSubject esbIamSubject,
                              List<EsbIamAction> actions,
                              List<EsbIamBatchPathResource> resourceList) {
        try {
            List<EsbIamBatchAuthedPolicy> batchAuthedPolicy = getEsbIamClient().batchAuthByPath(actions,
                esbIamSubject, resourceList, null);
            log.info("batchAuthedPolicy={}", JsonUtils.toJson(batchAuthedPolicy));
            return true;
        } catch (Exception e) {
            log.error("Fail to auth subject {} to {}", JsonUtils.toJson(esbIamSubject), JsonUtils.toJson(actions), e);
            return false;
        }
    }

    public void printSeparateLine() {
        log.info("==================================================");
    }

    private void resetResourceListTpl(List<BatchAuthByPathReq> reqList) {
        fillResourceListTpl(reqList, PLACEHOLDER_BIZ_SET_ID, PLACEHOLDER_BIZ_SET_NAME);
    }

    private void fillResourceListTpl(List<BatchAuthByPathReq> reqlist,
                                     String bizSetId,
                                     String bizSetName) {
        for (BatchAuthByPathReq batchAuthByPathReq : reqlist) {
            List<EsbIamBatchPathResource> resourceList = batchAuthByPathReq.getResources();
            for (EsbIamBatchPathResource esbIamBatchPathResource : resourceList) {
                List<List<EsbIamPathItem>> paths = esbIamBatchPathResource.getPaths();
                if (CollectionUtils.isEmpty(paths)) {
                    continue;
                }
                for (List<EsbIamPathItem> path : paths) {
                    if (CollectionUtils.isEmpty(path)) {
                        continue;
                    }
                    for (EsbIamPathItem esbIamPathItem : path) {
                        esbIamPathItem.setId(bizSetId);
                        esbIamPathItem.setName(bizSetName);
                    }
                }
            }
        }
    }

    private void recordAuthActionResult(boolean result,
                                        List<String> successActionList,
                                        List<String> failedActionList,
                                        String maintainer,
                                        String bizSetAppStr,
                                        BatchAuthByPathReq req) {
        if (result) {
            log.info(
                "[Action] Success to auth maintainer:{}, app:{}, actions:{}",
                maintainer,
                bizSetAppStr,
                JsonUtils.toJson(req.getActions())
            );
            successActionList.addAll(
                req.getActions().parallelStream()
                    .map(EsbIamAction::getId)
                    .collect(Collectors.toList())
            );
        } else {
            log.warn(
                "[Action] Fail to auth maintainer:{}, app:{}, actions:{}, resources:{}",
                maintainer,
                bizSetAppStr,
                JsonUtils.toJson(req.getActions()),
                JsonUtils.toJson(req.getResources())
            );
            failedActionList.addAll(
                req.getActions().parallelStream()
                    .map(EsbIamAction::getId)
                    .collect(Collectors.toList())
            );
        }
    }

    private void recordAuthMaintainerResult(List<String> failedActionList,
                                            List<String> successMaintainerList,
                                            List<String> failedMaintainerList,
                                            String maintainer,
                                            int successActionCount,
                                            int actionCount,
                                            String bizSetAppStr) {
        // 所有Action全部授权成功
        if (failedActionList.isEmpty()) {
            successMaintainerList.add(maintainer);
            log.info(
                "[Maintainer] Success to auth maintainer:{} to {} actions of app {}",
                maintainer,
                successActionCount,
                bizSetAppStr
            );
        } else {
            failedMaintainerList.add(maintainer);
            log.warn(
                "[Maintainer] Fail to auth maintainer:{} to {}/{} actions of app {}, failed actions:{}",
                maintainer,
                failedActionList,
                actionCount,
                bizSetAppStr,
                JsonUtils.toJson(failedActionList)
            );
        }
    }

    private void recordAuthBizSetResult(List<String> failedMaintainerList,
                                        List<String> successBizSetList,
                                        List<String> failedBizSetList,
                                        int successMaintainerCount,
                                        int maintainerCount,
                                        String bizSetAppStr) {
        if (failedMaintainerList.isEmpty()) {
            successBizSetList.add(bizSetAppStr);
            log.info(
                "[BizSet] Success to auth {} maintainers of app {}",
                successMaintainerCount,
                bizSetAppStr
            );
        } else {
            failedBizSetList.add(bizSetAppStr);
            log.warn(
                "[BizSet] Fail to auth {}/{} maintainers of app {}, failed maintainers:{}",
                failedMaintainerList.size(),
                maintainerCount,
                bizSetAppStr,
                JsonUtils.toJson(failedMaintainerList)
            );
        }
    }

    private void showGlobalAuthResult(List<String> failedBizSetList,
                                      List<String> successBizSetList) {
        if (failedBizSetList.isEmpty()) {
            log.info(
                "[Global] Success to auth {} bizSetApps",
                successBizSetList.size()
            );
        } else {
            log.warn(
                "[Global] Fail to auth {}/{} bizSetApps, failed bizSetApps:{}",
                failedBizSetList.size(),
                bizSetAppInfoList.size(),
                JsonUtils.toJson(failedBizSetList)
            );
        }
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        // 1.权限模板数据读取
        String bizSetTplFileName = "biz_set_auth_template.json";
        InputStream ins = getClass().getClassLoader().getResourceAsStream(bizSetTplFileName);
        String authTemplateStr = IOUtils.toString(ins, Charsets.UTF_8);
        List<BatchAuthByPathReq> reqList = JsonUtils.fromJson(
            authTemplateStr, new TypeReference<List<BatchAuthByPathReq>>() {
            });
        log.info("{} auth request template(s) loaded", reqList.size());
        printSeparateLine();
        log.info("Begin to auth according to job app info:");
        // 2.业务集运维人员授权
        List<String> successBizSetList = new ArrayList<>();
        List<String> failedBizSetList = new ArrayList<>();
        for (AppInfo bizSetAppInfo : bizSetAppInfoList) {
            resetResourceListTpl(reqList);
            fillResourceListTpl(reqList, bizSetAppInfo.getId().toString(), bizSetAppInfo.getName());
            String maintainerStr = bizSetAppInfo.getMaintainers();
            if (StringUtils.isBlank(maintainerStr)) {
                log.warn(
                    "maintainer is blank, app:{},{}, ignore",
                    bizSetAppInfo.getId(),
                    bizSetAppInfo.getName()
                );
                continue;
            }
            String[] maintainers = maintainerStr.split("[,;]");
            List<String> maintainerList = new ArrayList<>();
            for (String maintainer : maintainers) {
                if (!maintainerList.contains(maintainer)) {
                    maintainerList.add(maintainer);
                }
            }
            List<String> successMaintainerList = new ArrayList<>();
            List<String> failedMaintainerList = new ArrayList<>();
            for (String maintainer : maintainerList) {
                EsbIamSubject esbIamSubject = new EsbIamSubject("user", maintainer);
                List<String> successActionList = new ArrayList<>();
                List<String> failedActionList = new ArrayList<>();
                int actionCount = 0;
                for (BatchAuthByPathReq req : reqList) {
                    actionCount += req.getActions().size();
                    boolean result = batchAuth(esbIamSubject, req.getActions(), req.getResources());
                    recordAuthActionResult(
                        result, successActionList, failedActionList,
                        maintainer, bizSetAppInfo.getIdAndName(), req
                    );
                }
                recordAuthMaintainerResult(
                    failedActionList, successMaintainerList, failedMaintainerList,
                    maintainer, successActionList.size(), actionCount, bizSetAppInfo.getIdAndName()
                );
            }
            recordAuthBizSetResult(
                failedMaintainerList, successBizSetList, failedBizSetList,
                successMaintainerList.size(), maintainerList.size(), bizSetAppInfo.getIdAndName()
            );
        }
        showGlobalAuthResult(failedBizSetList, successBizSetList);
        log.info("auth bizSetApps finished!");
        printSeparateLine();
        return 0;
    }
}
