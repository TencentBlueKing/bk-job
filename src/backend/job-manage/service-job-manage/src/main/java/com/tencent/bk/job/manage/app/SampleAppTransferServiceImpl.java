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

package com.tencent.bk.job.manage.app;

import com.tencent.bk.job.common.app.AppTransferService;
import com.tencent.bk.job.common.app.Scope;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 仅处理业务类型的示例转换类，仅用于验证代码兼容性
 */
@Service
public class SampleAppTransferServiceImpl implements AppTransferService {
    @Override
    public Long getAppIdByScope(String scopeType, String scopeId) {
        return Long.parseLong(scopeId);
    }

    @Override
    public Scope getScopeByAppId(Long appId) {
        return new Scope(ResourceId.BIZ, appId.toString());
    }

    @Override
    public Map<Long, Scope> getScopeByAppIds(Collection<Long> appIds) {
        Map<Long, Scope> map = new HashMap<>();
        for (Long appId : appIds) {
            map.put(appId, new Scope(ResourceId.BIZ, appId.toString()));
        }
        return map;
    }
}
