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
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.api.web.WebGlobalSettingsQueryResource;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.AccountNameRulesWithDefaultVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.TitleFooterVO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Description
 * @Date 2020/2/27
 * @Version 1.0
 */

@RestController
@Slf4j
public class WebGlobalSettingsQueryResourceImpl implements WebGlobalSettingsQueryResource, DisposableBean {

    private GlobalSettingsService globalSettingsService;
    private ApplicationService applicationService;
    private AuthService authService;
    private AppAuthService appAuthService;
    private JobManageConfig jobManageConfig;

    private ThreadPoolExecutor executor = new ThreadPoolExecutor(
        5, 5, 30, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>());

    @Autowired
    public WebGlobalSettingsQueryResourceImpl(GlobalSettingsService globalSettingsService,
                                              ApplicationService applicationService,
                                              AuthService authService,
                                              AppAuthService appAuthService,
                                              JobManageConfig jobManageConfig) {
        this.globalSettingsService = globalSettingsService;
        this.applicationService = applicationService;
        this.authService = authService;
        this.appAuthService = appAuthService;
        this.jobManageConfig = jobManageConfig;
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
                AuthResult createWhiteListAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.CREATE_WHITELIST
                );
                flag.set(flag.get() || createWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult manageWhiteListAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.MANAGE_WHITELIST
                );
                flag.set(flag.get() || manageWhiteListAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_WHITELIST, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult createPublicScriptAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.CREATE_PUBLIC_SCRIPT
                );
                flag.set(flag.get() || createPublicScriptAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.CREATE_PUBLIC_SCRIPT, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult managePublicScriptAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE
                );
                flag.set(flag.get() || managePublicScriptAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.MANAGE_PUBLIC_SCRIPT_INSTANCE, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult globalSettingsAuthResultVO = authService.auth(
                    false, username, ActionId.GLOBAL_SETTINGS);
                flag.set(flag.get() || globalSettingsAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.GLOBAL_SETTINGS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                List<String> resourceIdList = new ArrayList<>();
                resourceIdList.add(AnalysisConsts.GLOBAL_DASHBOARD_VIEW_ID);
                List<String> authedIdList = authService.batchAuth(username, ActionId.DASHBOARD_VIEW,
                    ResourceTypeEnum.DASHBOARD_VIEW, resourceIdList);
                flag.set(flag.get() || !authedIdList.isEmpty());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.DASHBOARD_VIEW, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult serviceInfoAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.SERVICE_STATE_ACCESS
                );
                flag.set(flag.get() || serviceInfoAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult highRiskRuleAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.HIGH_RISK_DETECT_RULE
                );
                flag.set(flag.get() || highRiskRuleAuthResultVO.isPass());
            } catch (Throwable t) {
                log.error("Fail to auth {} to {}", ActionId.SERVICE_STATE_ACCESS, username, t);
            } finally {
                latch.countDown();
            }
        });
        executor.submit(() -> {
            try {
                AuthResult highRiskRecordAuthResultVO = authService.auth(
                    false,
                    username,
                    ActionId.HIGH_RISK_DETECT_RECORD
                );
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
    public Response<String> getApplyBusinessUrl(String username, Long appId) {
        ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
        if (applicationDTO != null && applicationDTO.getAppType() == AppTypeEnum.NORMAL) {
            return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(appId));
        } else if (applicationDTO != null) {
            return Response.buildCommonFailResp(ErrorCode.NEED_APP_SET_CONFIG);
        } else {
            return Response.buildSuccessResp(appAuthService.getBusinessApplyUrl(null));
        }
    }

    @Override
    public Response<String> getCMDBAppIndexUrl(String username, Long appId) {
        return Response.buildSuccessResp(jobManageConfig.getCmdbServerUrl()
            + jobManageConfig.getCmdbAppIndexPath().replace("{appId}", appId.toString()));
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
    public void destroy() throws Exception {
        executor.shutdown();
    }
}
