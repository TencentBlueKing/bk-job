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

package com.tencent.bk.job.manage.config;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.ITenantSetCmdbClient;
import com.tencent.bk.job.common.tenant.ConditionalOnTenantDisabled;
import com.tencent.bk.job.common.tenant.ConditionalOnTenantEnabled;
import com.tencent.bk.job.manage.background.sync.tenantset.ITenantSetSyncService;
import com.tencent.bk.job.manage.background.sync.tenantset.NonTenantSetSyncService;
import com.tencent.bk.job.manage.background.sync.tenantset.TenantSetSyncService;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Slf4j
@Configuration
public class TenantConfiguration {
    @Bean
    @ConditionalOnTenantEnabled
    public ITenantSetSyncService tenantSetSyncService(ApplicationDAO applicationDAO,
                                                      NoTenantHostDAO noTenantHostDAO,
                                                      ApplicationService applicationService,
                                                      IBizCmdbClient bizCmdbClient,
                                                      ITenantSetCmdbClient tenantSetCmdbClient,
                                                      CmdbConfig cmdbConfig) {
        log.info("init tenantSetSyncService");
        return new TenantSetSyncService(
            applicationDAO,
            noTenantHostDAO,
            applicationService,
            bizCmdbClient,
            tenantSetCmdbClient,
            cmdbConfig
        );
    }

    @Bean
    @ConditionalOnTenantDisabled
    public ITenantSetSyncService nonTenantSetSyncService() {
        log.info("init nonTenantSetSyncService");
        return new NonTenantSetSyncService();
    }
}
