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

package com.tencent.bk.job.common.cc.mock;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.cc.model.tenantset.TenantSetInfo;
import com.tencent.bk.job.common.cc.sdk.ITenantSetCmdbClient;
import com.tencent.bk.job.common.util.json.JsonUtils;

import java.util.List;

public class MockTenantSetCmdbClient implements ITenantSetCmdbClient {
    @Override
    public List<TenantSetInfo> listAllTenantSet() {
        String respStr = "[\n" +
            "      {\n" +
            "        \"id\": 1,\n" +
            "        \"name\": \"All tenants\",\n" +
            "        \"maintainer\": \"\",\n" +
            "        \"description\": \"全租户\",\n" +
            "        \"default\": 1,\n" +
            "        \"bk_scope\": {\n" +
            "          \"match_all\": true\n" +
            "        },\n" +
            "        \"bk_created_at\": \"2021-09-06T08:10:50.168Z\",\n" +
            "        \"bk_updated_at\": \"2021-10-15T02:30:01.867Z\"\n" +
            "      }\n" +
            "    ]";
        return JsonUtils.fromJson(respStr, new TypeReference<List<TenantSetInfo>>() {
        });
    }
}
