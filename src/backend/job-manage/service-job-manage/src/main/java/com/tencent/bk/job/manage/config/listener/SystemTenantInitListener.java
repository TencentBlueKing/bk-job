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

package com.tencent.bk.job.manage.config.listener;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.tenant.ConditionalOnTenantEnabled;
import com.tencent.bk.job.manage.service.TenantInitResult;
import com.tencent.bk.job.manage.service.TenantInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 多租户模式下自动初始化系统租户，整个软件生命周期内至多成功执行一次。
 * 失败后不做进程内重试，下一次实例启动时会再次尝试。
 */
@Slf4j
@Component
@Profile("!test")
@ConditionalOnTenantEnabled
public class SystemTenantInitListener implements ApplicationListener<ApplicationReadyEvent> {

    private final TenantInitService tenantInitService;
    private final ThreadPoolExecutor initRunnerExecutor;

    @Autowired
    public SystemTenantInitListener(TenantInitService tenantInitService,
                                    @Qualifier("initRunnerExecutor") ThreadPoolExecutor initRunnerExecutor) {
        this.tenantInitService = tenantInitService;
        this.initRunnerExecutor = initRunnerExecutor;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // 单次初始化的耗时没有上界，必须异步执行，否则会拖住启动收尾流程导致存活/就绪探针判定实例不健康
        initRunnerExecutor.submit(this::initSystemTenant);
    }

    private void initSystemTenant() {
        TenantInitResult result = tenantInitService.initTenantOnce(TenantIdConstants.SYSTEM_TENANT_ID);
        log.info("System tenant auto init task finished, result={}", result);
    }
}
