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

package com.tencent.bk.job.execute.api.esb;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * ESB API 基础服务类
 */
@Slf4j
public class EsbBaseResource {

    protected AppScopeMappingService appScopeMappingService;

    @Autowired
    @Lazy
    public void setAppScopeMappingService(AppScopeMappingService appScopeMappingService) {
        this.appScopeMappingService = appScopeMappingService;
    }

    /**
     * 补全ESB 请求中的appId/scopeType/scopeId
     * @param appScopeReq 请求
     */
    protected void fillAppResourceScope(EsbAppScopeReq appScopeReq) {
        boolean isExistScopeParam = StringUtils.isNotEmpty(appScopeReq.getScopeType()) &&
            StringUtils.isNotEmpty(appScopeReq.getScopeId());
        boolean isExistAppIdParam = appScopeReq.getAppId() != null;
        if (isExistScopeParam) {
            Long appId = appScopeMappingService.getAppIdByScope(appScopeReq.getScopeType(), appScopeReq.getScopeId());
            appScopeReq.setAppId(appId);
        } else if (isExistAppIdParam) {
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appScopeReq.getAppId());
            appScopeReq.setScopeType(resourceScope.getType().getValue());
            appScopeReq.setScopeId(resourceScope.getId());
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                new String[]{"bk_biz_id/bk_scope_type/bk_scope_id"});
        }
    }
}
