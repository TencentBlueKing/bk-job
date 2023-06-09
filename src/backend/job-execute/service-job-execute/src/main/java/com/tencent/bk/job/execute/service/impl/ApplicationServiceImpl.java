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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.api.inner.ServiceSyncResource;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service("jobExecuteApplicationService")
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ServiceApplicationResource applicationResource;
    private final ServiceSyncResource syncResource;

    @Autowired
    public ApplicationServiceImpl(ServiceApplicationResource applicationResource,
                                  ServiceSyncResource syncResource) {
        this.applicationResource = applicationResource;
        this.syncResource = syncResource;
    }

    @Override
    public ServiceApplicationDTO getAppById(long appId) {
        return applicationResource.queryAppById(appId);
    }

    @Override
    public ServiceApplicationDTO getAppByScope(String scopeType, String scopeId) {
        return applicationResource.queryAppByScope(scopeType, scopeId);
    }

    @Override
    public List<Long> listAllAppIds() {
        List<ServiceApplicationDTO> apps = syncResource.listAllApps();
        if (apps == null) {
            return Collections.emptyList();
        }
        return apps.stream().map(ServiceApplicationDTO::getId).collect(Collectors.toList());
    }
}
