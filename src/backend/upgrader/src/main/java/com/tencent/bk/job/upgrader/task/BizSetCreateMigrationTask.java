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

import com.tencent.bk.job.common.cc.model.bizset.BasicBizSet;
import com.tencent.bk.job.common.cc.model.bizset.BatchUpdateBizSetData;
import com.tencent.bk.job.common.cc.model.bizset.BatchUpdateBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.BizSetAttr;
import com.tencent.bk.job.common.cc.model.bizset.BizSetFilter;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetScope;
import com.tencent.bk.job.common.cc.model.bizset.CreateBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.Rule;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.client.EsbCmdbClient;
import com.tencent.bk.job.upgrader.client.JobClient;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import com.tencent.bk.job.upgrader.task.param.ParamNameConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * 业务集、全业务向CMDB迁移的资源创建任务
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class)
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.5.0",
    targetExecuteTime = ExecuteTimeEnum.AFTER_UPDATE_JOB)
public class BizSetCreateMigrationTask extends BaseUpgradeTask {

    private JobClient jobManageClient;

    private EsbCmdbClient esbCmdbClient;
    private List<AppInfo> bizSetAppInfoList;

    private String getJobHostUrlByAddress(String address) {
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }
        return address;
    }

    public BizSetCreateMigrationTask(Properties properties) {
        super(properties);
    }

    @Override
    public void init() {
        String appCode = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_CODE);
        String appSecret = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_APP_SECRET);
        String esbBaseUrl = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_ESB_SERVICE_URL);
        String cmdbSupplierAccount =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_CMDB_DEFAULT_SUPPLIER_ACCOUNT);
        String jobAuthToken = getJobAuthToken();
        jobManageClient = new JobClient(
            getJobHostUrlByAddress((String) properties.get(ParamNameConsts.INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS)),
            jobAuthToken
        );
        esbCmdbClient = new EsbCmdbClient(esbBaseUrl, appCode, appSecret, "zh-cn", cmdbSupplierAccount);
        // 从job-manage拉取业务集/全业务信息
        this.bizSetAppInfoList = getAllBizSetAppInfoFromManage();
    }

    private List<AppInfo> getAllBizSetAppInfoFromManage() {
        try {
            return jobManageClient.listBizSetApps();
        } catch (Exception e) {
            log.error("Fail to get special apps from job-manage, please confirm job-manage version>=3.5.0.0");
            throw e;
        }
    }

    /**
     * 为业务集构造选择业务的过滤条件
     *
     * @param appInfo 业务集信息
     * @return 过滤条件
     */
    private BizSetFilter buildAppSetFilter(AppInfo appInfo) {
        BizSetFilter filter = new BizSetFilter();
        filter.setCondition(BizSetFilter.CONDITION_AND);

        List<Rule> rules = new ArrayList<>();
        // 指定所有子业务ID
        List<Long> subAppIds = appInfo.getSubAppIds();
        if (subAppIds == null) {
            subAppIds = new ArrayList<>();
        }
        // 指定业务所属部门ID
        Long operateDeptId = appInfo.getOperateDeptId();
        // 空业务集
        if (CollectionUtils.isEmpty(subAppIds) && operateDeptId == null) {
            Rule subAppIdsRule = new Rule();
            subAppIdsRule.setField("bk_biz_id");
            subAppIdsRule.setOperator(Rule.OPERATOR_IN);
            subAppIdsRule.setValue(subAppIds);
            rules.add(subAppIdsRule);
            filter.setRules(rules);
            return filter;
        }
        if (!CollectionUtils.isEmpty(subAppIds)) {
            Rule subAppIdsRule = new Rule();
            subAppIdsRule.setField("bk_biz_id");
            subAppIdsRule.setOperator(Rule.OPERATOR_IN);
            subAppIdsRule.setValue(subAppIds);
            rules.add(subAppIdsRule);
        }
        if (operateDeptId != null) {
            Rule operateDeptIdRule = new Rule();
            operateDeptIdRule.setField("bk_operate_dept_id");
            operateDeptIdRule.setOperator(Rule.OPERATOR_IN);
            operateDeptIdRule.setValue(Collections.singletonList(operateDeptId));
            rules.add(operateDeptIdRule);
        }

        filter.setRules(rules);
        return filter;
    }

    /**
     * 构造符合CMDB规则的运维人员字段
     *
     * @param rawStr Job原始运维人员字段
     * @return 符合CMDB规则的运维人员字段
     */
    private String buildMaintainerStr(String rawStr) {
        String[] maintainers = rawStr.split("[,;]");
        return String.join(",", maintainers);
    }

    /**
     * 获取业务集最终在CMDB中的ID，用于判定存在性决定是否创建
     *
     * @param appInfo 业务信息
     * @return 业务集ID
     */
    private long getFinalBizSetId(AppInfo appInfo) {
        if (StringUtils.isNotBlank(appInfo.getScopeId())) {
            return Long.parseLong(appInfo.getScopeId());
        }
        return appInfo.getId();
    }

    /**
     * 调用CMDB接口更新业务集属性信息
     *
     * @param attr  业务集属性
     * @param scope 业务选择范围
     * @return 是否成功更新
     */
    private Boolean updateBizSet(BizSetAttr attr, BizSetScope scope) {
        BatchUpdateBizSetReq batchUpdateBizSetReq = esbCmdbClient.makeCmdbBaseReq(BatchUpdateBizSetReq.class);
        batchUpdateBizSetReq.setBizSetIds(Collections.singletonList(attr.getId()));
        BatchUpdateBizSetData data = new BatchUpdateBizSetData();
        data.setAttr(attr);
        data.setScope(scope);
        batchUpdateBizSetReq.setData(data);
        return esbCmdbClient.batchUpdateBizSet(batchUpdateBizSetReq);
    }

    /**
     * 根据Job中现存业务集/全业务信息向CMDB创建业务集/全业务
     *
     * @param appInfo 业务集/全业务信息
     * @return 业务集是否已存在于CMDB中
     */
    private boolean createOrUpdateCMDBResourceForApp(AppInfo appInfo) {
        CreateBizSetReq createBizSetReq = esbCmdbClient.makeCmdbBaseReq(CreateBizSetReq.class);
        String desc = "Auto created by bk-job migration";
        String supplierAccount = (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_CMDB_DEFAULT_SUPPLIER_ACCOUNT);
        BizSetAttr attr = BizSetAttr.builder()
            .id(getFinalBizSetId(appInfo))
            .name(appInfo.getName())
            .desc(desc)
            .maintainer(buildMaintainerStr(appInfo.getMaintainers()))
            .timeZone(appInfo.getTimeZone())
            .language(appInfo.getLanguage())
            .supplierAccount(supplierAccount)
            .build();
        createBizSetReq.setAttr(attr);
        BizSetScope scope = new BizSetScope();
        if (appInfo.isAllBizSet()) {
            // 全业务
            scope.setMatchAll(true);
            scope.setFilter(null);
        } else if (appInfo.getAppType() == AppTypeEnum.APP_SET.getValue()) {
            // 普通业务集
            scope.setMatchAll(false);
            scope.setFilter(buildAppSetFilter(appInfo));
        } else {
            log.warn("Not support app type:{}", appInfo.getAppType());
            return false;
        }
        createBizSetReq.setScope(scope);
        try {
            List<BizSetInfo> bizSetList = esbCmdbClient.searchBizSetById(attr.getId());
            if (CollectionUtils.isEmpty(bizSetList)) {
                Long bizSetId = esbCmdbClient.createBizSet(createBizSetReq);
                if (bizSetId != null && bizSetId > 0) {
                    log.info("bizSet {} created", bizSetId);
                } else {
                    log.warn("fail to create bizSet {}", attr.getId());
                    return false;
                }
            } else {
                if (attr.getId() == JobConstants.DEFAULT_ALL_BIZ_SET_ID) {
                    // CMDB内置的全业务，不更新
                    log.info("bizSet {} is all-business bizSet created by cmdb, ignore", attr.getId());
                } else {
                    // 更新业务集
                    log.info("bizSet {} already exists, update:{}", attr.getId(), updateBizSet(attr, scope));
                }
            }
            return true;
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format("Fail to create bizSet {}", createBizSetReq);
            log.error(msg.getMessage(), e);
            return false;
        }
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        List<BasicBizSet> successfulBizSetList = new ArrayList<>();
        List<BasicBizSet> failedBizSetList = new ArrayList<>();
        int successCount = 0;
        for (AppInfo appInfo : bizSetAppInfoList) {
            BasicBizSet bizSet = new BasicBizSet(getFinalBizSetId(appInfo), appInfo.getName());
            // 1.调用CMDB接口创建业务集/全业务
            if (createOrUpdateCMDBResourceForApp(appInfo)) {
                successCount += 1;
                successfulBizSetList.add(bizSet);
            } else {
                failedBizSetList.add(bizSet);
            }
        }
        if (successCount == bizSetAppInfoList.size()) {
            log.info("all {} bizSets migrated to CMDB", successCount);
        } else {
            log.warn(
                "{}/{} bizSets migrated to CMDB, please check log to confirm failed bizSets",
                successCount,
                bizSetAppInfoList.size()
            );
            log.warn("Failed bizSets:{}", JsonUtils.toJson(failedBizSetList));
        }
        // 2.生成更新CMDB数据库的业务集信息Json文件
        String content = JsonUtils.toJson(successfulBizSetList);
        InputStream ins = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        try {
            File targetFile = new File("biz_set_list.json");
            FileUtil.writeInsToFile(ins, targetFile.getAbsolutePath());
            log.info("biz_set_list.json generated, please save it to continue the next step! path:{}",
                targetFile.getAbsolutePath());
        } catch (InterruptedException e) {
            log.error("Fail to gen biz_set_list.json", e);
            return 1;
        }
        if (!failedBizSetList.isEmpty()) {
            return 1;
        }
        return 0;
    }
}
