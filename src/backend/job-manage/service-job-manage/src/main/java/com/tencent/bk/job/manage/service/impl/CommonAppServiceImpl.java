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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.AbstractLocalCacheCommonAppService;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommonAppServiceImpl extends AbstractLocalCacheCommonAppService {

    private final ApplicationService applicationService;

    @Autowired
    public CommonAppServiceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
        GlobalAppScopeMappingService.register(this);
    }

    @Override
    protected BasicApp queryAppByScope(ResourceScope resourceScope) throws NotFoundException {
        ApplicationDTO app = applicationService.getAppByScope(resourceScope);
        if (app == null) {
            log.error("App not exist, resourceScope: {}", resourceScope);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return convertToBasicApp(app);
    }

    @Override
    protected BasicApp queryAppByAppId(Long appId) throws NotFoundException {
        ApplicationDTO app = applicationService.getAppByAppId(appId);
        if (app == null) {
            log.error("App not exist, appId: {}", appId);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        return convertToBasicApp(app);
    }

    private BasicApp convertToBasicApp(ApplicationDTO app) {
        BasicApp basicApp = new BasicApp();
        basicApp.setId(app.getId());
        basicApp.setName(app.getName());
        basicApp.setScope(app.getScope());
        basicApp.setTenantId(app.getTenantId());
        return basicApp;
    }
}
