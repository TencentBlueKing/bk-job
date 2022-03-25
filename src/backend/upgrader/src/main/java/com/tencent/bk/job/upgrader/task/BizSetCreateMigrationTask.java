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
import com.tencent.bk.job.common.cc.model.bizset.BizSetAttr;
import com.tencent.bk.job.common.cc.model.bizset.BizSetFilter;
import com.tencent.bk.job.common.cc.model.bizset.BizSetInfo;
import com.tencent.bk.job.common.cc.model.bizset.BizSetScope;
import com.tencent.bk.job.common.cc.model.bizset.CreateBizSetReq;
import com.tencent.bk.job.common.cc.model.bizset.Rule;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.FileUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.jwt.BasicJwtManager;
import com.tencent.bk.job.common.util.jwt.JwtManager;
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
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
    targetVersion = "3.5.0.0",
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
        String securityPublicKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64);
        String securityPrivateKeyBase64 =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_JOB_SECURITY_PRIVATE_KEY_BASE64);
        String cmdbSupplierAccount =
            (String) properties.get(ParamNameConsts.CONFIG_PROPERTY_CMDB_DEFAULT_SUPPLIER_ACCOUNT);
        JwtManager jwtManager;
        try {
            jwtManager = new BasicJwtManager(securityPrivateKeyBase64, securityPublicKeyBase64);
        } catch (IOException | GeneralSecurityException e) {
            String msg = "Fail to generate jwt auth token";
            log.error(msg, e);
            throw new InternalException(msg, e, ErrorCode.INTERNAL_ERROR);
        }
        // 迁移过程最大预估时间：3h
        String jobAuthToken = jwtManager.generateToken(3 * 60 * 60 * 1000);
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
            operateDeptIdRule.setOperator(Rule.OPERATOR_EQUAL);
            operateDeptIdRule.setValue(operateDeptId);
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
     * 根据Job中现存业务集/全业务信息向CMDB创建业务集/全业务
     *
     * @param appInfo 业务集/全业务信息
     */
    private boolean createCMDBResourceForApp(AppInfo appInfo) {
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
        if (appInfo.getAppType() == AppTypeEnum.APP_SET.getValue()) {
            scope.setMatchAll(false);
            scope.setFilter(buildAppSetFilter(appInfo));
        } else if (appInfo.getAppType() == AppTypeEnum.ALL_APP.getValue()) {
            // 匹配所有业务
            scope.setMatchAll(true);
            scope.setFilter(null);
        } else {
            log.warn("Not support app type:{}", appInfo.getAppType());
            return false;
        }
        createBizSetReq.setScope(scope);
        try {
            List<BizSetInfo> bizSetList = esbCmdbClient.searchBizSetById(attr.getId());
            if (CollectionUtils.isEmpty(bizSetList)) {
                Long bizSetId = esbCmdbClient.createBizSet(createBizSetReq);
                log.info("bizSet {} created", bizSetId);
                return true;
            } else {
                log.warn("bizSet {} already exists, ignore", attr.getId());
                return false;
            }
        } catch (Exception e) {
            FormattingTuple msg = MessageFormatter.format("Fail to create bizSet {}", createBizSetReq);
            log.error(msg.getMessage(), e);
            return false;
        }
    }

    @Override
    public int execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        List<BasicBizSet> bizSetList = new ArrayList<>();
        for (AppInfo appInfo : bizSetAppInfoList) {
            // 1.调用CMDB接口创建业务集/全业务
            createCMDBResourceForApp(appInfo);
            bizSetList.add(new BasicBizSet(appInfo.getId(), appInfo.getName()));
        }
        // 2.生成更新CMDB数据库的业务集信息Json文件
        String content = JsonUtils.toJson(bizSetList);
        InputStream ins = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        try {
            File targetFile = new File("biz_set_list.json");
            FileUtil.writeInsToFile(ins, targetFile.getAbsolutePath());
            log.info("biz_set_list.json generated, please save it to continue the next step! path:{}",
                targetFile.getAbsolutePath());
        } catch (InterruptedException e) {
            log.error("Fail to gen biz_set_list.json", e);
        }
        return 0;
    }
}
