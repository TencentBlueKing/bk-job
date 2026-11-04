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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.op.TenantOpResource;
import com.tencent.bk.job.manage.model.op.req.InitTenantReq;
import com.tencent.bk.job.manage.service.TenantInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户OP接口实现类
 */
@Slf4j
@RestController
public class TenantOpResourceImpl implements TenantOpResource {

    private final TenantInitService tenantInitService;

    @Autowired
    public TenantOpResourceImpl(TenantInitService tenantInitService) {
        this.tenantInitService = tenantInitService;
    }

    @Override
    public Response<Object> initTenant(InitTenantReq req) {
        String tenantId = req.getTenantId();
        try {
            Boolean taskResult = tenantInitService.initTenant(tenantId);
            if (taskResult == null) {
                // 任务已在其他实例执行
                return Response.buildCommonFailResp(ErrorCode.INIT_TENANT_TASK_ALREADY_RUNNING, new String[]{tenantId});
            }
            return Response.buildSuccessResp(taskResult);
        } catch (Exception e) {
            log.error("initTenantTask failed", e);
            return Response.buildCommonFailResp(ErrorCode.INIT_TENANT_ERROR, new String[]{tenantId});
        }
    }
}
