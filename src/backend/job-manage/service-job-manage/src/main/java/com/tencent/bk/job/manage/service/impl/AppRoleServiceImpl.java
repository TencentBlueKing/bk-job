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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.cc.model.AppRoleDTO;
import com.tencent.bk.job.common.cc.sdk.BizSetCmdbClient;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.service.AppRoleService;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class AppRoleServiceImpl implements AppRoleService {

    private final ApplicationService applicationService;
    private final BizSetCmdbClient bizSetCmdbClient;

    @Autowired
    public AppRoleServiceImpl(@Lazy ApplicationService applicationService,
                              BizSetCmdbClient bizSetCmdbClient) {
        this.applicationService = applicationService;
        this.bizSetCmdbClient = bizSetCmdbClient;
    }

    @Override
    public List<AppRoleDTO> listAppRoles(String lang) {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(lang);
        return bizCmdbClient.listRoles();
    }

    @Override
    public Set<String> listAppUsersByRole(Long appId, String role) {
        ApplicationDTO application = applicationService.getAppByAppId(appId);
        ResourceScope scope = application.getScope();
        if (scope.getType() == ResourceScopeTypeEnum.BIZ) {
            IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
            return bizCmdbClient.listUsersByRole(Long.valueOf(scope.getId()), role);
        } else if (scope.getType() == ResourceScopeTypeEnum.BIZ_SET) {
            // 业务集当前只支持运维人员
            return bizSetCmdbClient.listUsersByRole(Long.valueOf(scope.getId()), role);
        } else {
            log.warn("Not supported resourceScope:{}", scope);
        }
        return Collections.emptySet();
    }
}
