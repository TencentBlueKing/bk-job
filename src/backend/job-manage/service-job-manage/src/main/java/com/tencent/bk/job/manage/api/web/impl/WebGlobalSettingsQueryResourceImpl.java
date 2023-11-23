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

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.analysis.consts.AnalysisConsts;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.api.web.WebGlobalSettingsQueryResource;
import com.tencent.bk.job.manage.auth.NoResourceScopeAuthService;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.PlatformInfoVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import com.tencent.bk.job.manage.service.PublicScriptService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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
    private final PublicScriptService publicScriptService;
    private final ThreadPoolExecutor adminAuthExecutor;

    @Autowired
    public WebGlobalSettingsQueryResourceImpl(GlobalSettingsService globalSettingsService,
                                              ApplicationService applicationService,
                                              JobManageConfig jobManageConfig,
                                              NoResourceScopeAuthService noResourceScopeAuthService,
                                              AppAuthService appAuthService,
                                              PublicScriptService publicScriptService,
                                              @Qualifier("adminAuthExecutor") ThreadPoolExecutor adminAuthExecutor) {
        this.globalSettingsService = globalSettingsService;
        this.applicationService = applicationService;
        this.jobManageConfig = jobManageConfig;
        this.noResourceScopeAuthService = noResourceScopeAuthService;
        this.appAuthService = appAuthService;
        this.publicScriptService = publicScriptService;
        this.adminAuthExecutor = adminAuthExecutor;
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<List<NotifyChannelWithIconVO>> listNotifyChannel(String username) {
        return Response.buildSuccessResp(globalSettingsService.listNotifyChannel(username));
    }

    @Override
    @AuditEntry(actionId = ActionId.GLOBAL_SETTINGS)
    public Response<AccountNameRulesWithDefaultVO> getAccountNameRules(String username) {
        return Response.buildSuccessResp(globalSettingsService.getAccountNameRules());
    }

    @Override
    public Response<Boolean> isAdmin(String username) {
        AtomicBoolean flag = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(9);
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult createWhiteListAuthResultVO = noResourceScopeAuthService.authCreateWhiteList(username);
                flag.set(flag.get() || createWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult manageWhiteListAuthResultVO = noResourceScopeAuthService.authManageWhiteList(username);
                flag.set(flag.get() || manageWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult createPublicScriptAuthResultVO = noResourceScopeAuthService.authCreatePublicScript(username);
                flag.set(flag.get() || createPublicScriptAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_PUBLIC_SCRIPT, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
            try {
                // 是否能管理某些公共脚本
                List<String> canManagePublicScriptIds =
                    noResourceScopeAuthService.batchAuthManagePublicScript(username,
                        publicScriptService.listScriptIds());
                // 是否能够创建公共脚本
                AuthResult authResult = noResourceScopeAuthService.authCreatePublicScript(username);
                flag.set(flag.get() || !canManagePublicScriptIds.isEmpty() || authResult.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult globalSettingsAuthResultVO = noResourceScopeAuthService.authGlobalSetting(username);
                flag.set(flag.get() || globalSettingsAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.GLOBAL_SETTINGS, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
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
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult serviceInfoAuthResultVO = noResourceScopeAuthService.authViewServiceState(username);
                flag.set(flag.get() || serviceInfoAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
            try {
                AuthResult highRiskRuleAuthResultVO = noResourceScopeAuthService.authHighRiskDetectRule(username);
                flag.set(flag.get() || highRiskRuleAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        adminAuthExecutor.submit(() -> {
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
        if (StringUtils.isBlank(scopeType) || StringUtils.isBlank(scopeId)) {
            return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(null));
        }
        AppResourceScope appResourceScope = new AppResourceScope(scopeType, scopeId, null);
        ApplicationDTO applicationDTO = applicationService.getAppByScope(appResourceScope);
        if (applicationDTO != null) {
            appResourceScope.setAppId(applicationDTO.getId());
            return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(appResourceScope));
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
    public Response<PlatformInfoVO> getRenderedPlatformInfo() {
        return Response.buildSuccessResp(globalSettingsService.getRenderedPlatformInfo());
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
        adminAuthExecutor.shutdown();
    }
}
