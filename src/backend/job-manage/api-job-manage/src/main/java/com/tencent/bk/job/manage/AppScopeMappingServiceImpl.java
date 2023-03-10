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

package com.tencent.bk.job.manage;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.inner.ServiceApplicationResource;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 业务与资源范围转换
 */
@Slf4j
public class AppScopeMappingServiceImpl extends AbstractLocalCacheAppScopeMappingService {

    private final ServiceApplicationResource applicationResource;

    public AppScopeMappingServiceImpl(ServiceApplicationResource applicationResource) {
        this.applicationResource = applicationResource;
    }

    @Override
    public Long queryAppByScope(ResourceScope resourceScope) throws NotFoundException {
        ServiceApplicationDTO app = applicationResource.queryAppByScope(
            resourceScope.getType().getValue(), resourceScope.getId());
        if (app == null) {
            log.error("App not found, query scope: {}", resourceScope);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        if (app.getId() == null) {
            // 如果查询到的业务缺少ID参数，抛出异常避免缓存非法数据
            log.error("Empty appId for application, reject cache! query scope: {}", resourceScope);
            throw new InternalException("Empty appId for application", ErrorCode.INTERNAL_ERROR);
        }
        return app.getId();
    }

    @Override
    public ResourceScope queryScopeByAppId(Long appId) throws NotFoundException {
        ServiceApplicationDTO app = applicationResource.queryAppById(appId);
        if (app == null) {
            log.error("App not found, query appId: {}", appId);
            throw new NotFoundException(ErrorCode.APP_NOT_EXIST);
        }
        if (StringUtils.isEmpty(app.getScopeType()) || StringUtils.isEmpty(app.getScopeId())) {
            // 如果查询到的业务缺少scopeType|scopeId参数，抛出异常避免缓存非法数据
            log.error("Empty scopeType|scopeId for application, reject cache! query appId: {}", appId);
            throw new InternalException("Empty scopeType|scopeId for application", ErrorCode.INTERNAL_ERROR);
        }
        return new ResourceScope(app.getScopeType(), app.getScopeId());
    }
}
