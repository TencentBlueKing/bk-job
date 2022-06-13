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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.analysis.consts.AnalysisConsts;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.manage.api.web.WebGlobalSettingsQueryResource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.TitleFooterVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@Slf4j
public class WebGlobalSettingsQueryResourceImpl implements WebGlobalSettingsQueryResource, DisposableBean {

    private final GlobalSettingsService globalSettingsService;
    private final ApplicationService applicationService;
    private final JobManageConfig jobManageConfig;
    private final NoResourceScopeAuthService noResourceScopeAuthService;
    private final AppAuthService appAuthService;
    private final ScriptService scriptService;

    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
        5, 5, 30, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());

    @Autowired
    public WebGlobalSettingsQueryResourceImpl(GlobalSettingsService globalSettingsService,
                                              ApplicationService applicationService,
                                              JobManageConfig jobManageConfig,
                                              NoResourceScopeAuthService noResourceScopeAuthService,
                                              AppAuthService appAuthService,
                                              ScriptService scriptService) {
        this.globalSettingsService = globalSettingsService;
        this.applicationService = applicationService;
        this.jobManageConfig = jobManageConfig;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
        this.appAuthService = appAuthService;
        this.scriptService = scriptService;
    }

    @Override
    public Response<List<NotifyChannelWithIconVO>> listNotifyChannel(String username) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyChannel(username));
    }

    @Override
    public Response<AccountNameRulesWithDefaultVO> getAccountNameRules(String username) {
        return Response.buildSuccessResp(globalSettingsService.getAccountNameRules());
    }

    @Override
    public Response<Boolean> isAdmin(String username) {
        AtomicBoolean flag = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(9);
        executor.submit(() -> {
            try {
                AuthResult createWhiteListAuthResultVO = noResourceScopeAuthService.authCreateWhiteList(username);
                flag.set(flag.get() || createWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult manageWhiteListAuthResultVO = noResourceScopeAuthService.authManageWhiteList(username);
                flag.set(flag.get() || manageWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult createPublicScriptAuthResultVO = noResourceScopeAuthService.authCreatePublicScript(username);
                flag.set(flag.get() || createPublicScriptAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_PUBLIC_SCRIPT, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                // 是否能管理某些公共脚本
                List<String> canManagePublicScriptIds =
                    noResourceScopeAuthService.batchAuthManagePublicScript(username,
                        scriptService.listScriptIds(JobConstants.PUBLIC_APP_ID));
                // 是否能够创建公共脚本
                AuthResult authResult = noResourceScopeAuthService.authCreatePublicScript(username);
                flag.set(flag.get() || !canManagePublicScriptIds.isEmpty() || authResult.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult globalSettingsAuthResultVO = noResourceScopeAuthService.authGlobalSetting(username);
                flag.set(flag.get() || globalSettingsAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.GLOBAL_SETTINGS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult authResult = noResourceScopeAuthService.authViewDashBoard(username,
                    AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID);
                flag.set(flag.get() || authResult.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.DASHBOARD_VIEW, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult serviceInfoAuthResultVO = noResourceScopeAuthService.authViewServiceState(username);
                flag.set(flag.get() || serviceInfoAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult highRiskRuleAuthResultVO = noResourceScopeAuthService.authHighRiskDetectRule(username);
                flag.set(flag.get() || highRiskRuleAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult highRiskRecordAuthResultVO = noResourceScopeAuthService.authHighRiskDetectRecord(username);
                flag.set(flag.get() || highRiskRecordAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("latch Interrupted", e);
        }
        return Response.buildSuccessResp(flag.get());
    }

    @Override
    public Response<String> getCMDBServerUrl(String username) {
        return Response.buildSuccessResp(jobManageConfig.getCmdbServerUrl());
    }

    @Override
    public Response<String> getApplyBusinessUrl(String username, String scopeType, String scopeId) {
        AppResourceScope appResourceScope = new AppResourceScope(scopeType, scopeId, null);
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO != null) {
            appResourceScope.setAppId(applicationDTO.getId());
            // 普通业务/开启了CMDB业务集特性的业务集：从权限中心接口获取权限申请URL链接
            if (appResourceScope.getType() == ResourceScopeTypeEnum.BIZ
                || FeatureToggle.isCmdbBizSetEnabledForApp(appResourceScope.getAppId())) {
                return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(appResourceScope));
            }
            // 未开启CMDB业务集特性的业务集：需要联系Job管理员添加为运维人员
            return Response.buildCommonFailResp(ErrorCode.NEED_APP_SET_CONFIG);
        } else {
            return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(null));
        }
    }

    @Override
    public Response<String> getCMDBAppIndexUrl(String username, String scopeType, String scopeId) {
        String scopeTypePlaceholderValue = ResourceScopeTypeEnum.from(scopeType) == ResourceScopeTypeEnum.BIZ ?
            "business" : "business-set";
        return Response.buildSuccessResp(jobManageConfig.getCmdbServerUrl()
            + jobManageConfig.getCmdbAppIndexPath()
            .replace("{scopeType}", scopeTypePlaceholderValue)
            .replace("{scopeId}", scopeId));
    }

    @Override
    public Response<TitleFooterVO> getTitleFooter() {
        return Response.buildSuccessResp(globalSettingsService.getTitleFooter());
    }

    @Override
    public Response<String> getDocCenterBaseUrl(String username) {
        return Response.buildSuccessResp(globalSettingsService.getDocCenterBaseUrl());
    }

    @Override
    public Response<Map<String, String>> getRelatedSystemUrls(String username) {
        return Response.buildSuccessResp(globalSettingsService.getRelatedSystemUrls(username));
    }

    @Override
    public Response<Map<String, Object>> getJobConfig(String username) {
        return Response.buildSuccessResp(globalSettingsService.getJobConfig(username));
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
