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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceRealProjectNameResource;
import com.tencent.bk.job.manage.model.inner.request.ServiceSaveRealProjectNameReq;
import com.tentent.bk.job.common.api.artifactory.IRealProjectNameStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceRealProjectNameResourceImpl implements ServiceRealProjectNameResource {

    private final IRealProjectNameStore realProjectNameStore;

    @Autowired
    public ServiceRealProjectNameResourceImpl(IRealProjectNameStore realProjectNameStore) {
        this.realProjectNameStore = realProjectNameStore;
    }

    @Override
    public InternalResponse<String> queryRealProjectName(String saveKey) {
        return InternalResponse.buildSuccessResp(
            realProjectNameStore.queryRealProjectName(saveKey)
        );
    }

    @Override
    public InternalResponse<Void> saveRealProjectName(ServiceSaveRealProjectNameReq req) {
        realProjectNameStore.saveRealProjectName(req.getSaveKey(), req.getRealProjectName());
        return InternalResponse.buildSuccessResp(null);
    }
}
