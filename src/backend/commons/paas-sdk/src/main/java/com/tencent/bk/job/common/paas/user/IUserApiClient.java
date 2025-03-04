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

package com.tencent.bk.job.common.paas.user;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.model.OpenApiResponse;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import io.micrometer.core.instrument.Tag;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.tencent.bk.job.common.metrics.CommonMetricNames.USER_MANAGE_API_HTTP;

public interface IUserApiClient {

    public List<BkUserDTO> getAllUserList(String tenantId);

    public List<OpenApiTenant> listAllTenant();

    public BkUserDTO getUserByUsername(String username);

    public Map<String, BkUserDTO> listUsersByUsernames(Collection<String> usernames);

    default  <T, R> OpenApiResponse<R> requestBkUserApi(
        String apiName,
        OpenApiRequestInfo<T> request,
        Function<OpenApiRequestInfo<T>, OpenApiResponse<R>> requestHandler) {

        try {
            HttpMetricUtil.setHttpMetricName(USER_MANAGE_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", apiName));
            return requestHandler.apply(request);
        } catch (Throwable e) {
            String errorMsg = "Fail to request bk-user api|method=" + request.getMethod()
                + "|uri=" + request.getUri() + "|queryParams="
                + request.getQueryParams() + "|body="
                + JsonUtils.toJsonWithoutSkippedFields(JsonUtils.toJsonWithoutSkippedFields(request.getBody()));
            logError(errorMsg, e);
            throw new InternalException(e.getMessage(), e, ErrorCode.BK_USER_MANAGE_API_ERROR);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

    void logError(String message, Object... objects);
}
