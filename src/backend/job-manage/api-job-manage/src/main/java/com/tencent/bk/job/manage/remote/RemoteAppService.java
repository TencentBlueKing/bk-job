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

package com.tencent.bk.job.manage.remote;

import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.remote.SimpleAppInfoDTO;

import java.util.List;

/**
 * 业务服务
 */
public interface RemoteAppService {
    /**
     * 根据Job业务ID获取业务信息
     *
     * @param appId Job业务ID
     * @return Job业务对象
     */
    ServiceApplicationDTO getAppById(long appId);

    /**
     * 根据Job业务ID获取所属租户ID
     *
     * @param appId Job业务ID
     * @return 所属租户ID
     */
    String getTenantIdByAppId(long appId);

    List<Long> listAllAppIds();

    List<Long> listAllAppIds(String tenantId);

    List<ServiceApplicationDTO> listLocalDBApps();

    List<ServiceApplicationDTO> listAppsByTenantId(String tenantId);

    String getAppNameFromCache(Long appId);

    List<SimpleAppInfoDTO> getSimpleAppInfoByIds(List<Long> appIdList);
}
