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

package com.tencent.bk.job.common.iam.http;

import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.constants.HttpHeader;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import io.micrometer.core.instrument.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class IamHttpClientServiceImpl implements HttpClientService {

    private final String DEFAULT_CHARSET = "UTF-8";
    private final ExtHttpHelper httpHelper = HttpHelperFactory.getDefaultHttpHelper();
    private final IamConfiguration iamConfiguration;

    public IamHttpClientServiceImpl(IamConfiguration iamConfiguration) {
        this.iamConfiguration = iamConfiguration;
        log.debug("IamHttpClientServiceImpl init");
    }

    @Override
    public String doHttpGet(String uri) {
        try {
            JobContextUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            JobContextUtil.addHttpMetricTag(Tag.of("api_name", uri));
            return httpHelper.get(buildUrl(uri), buildAuthHeader());
        } finally {
            JobContextUtil.clearHttpMetricTags();
        }
    }

    @Override
    public String doHttpPost(String uri, Object body) {
        try {
            JobContextUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            JobContextUtil.addHttpMetricTag(Tag.of("api_name", uri));
            return httpHelper.post(buildUrl(uri), DEFAULT_CHARSET, JsonUtils.toJson(body), buildAuthHeader());
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            return null;
        } finally {
            JobContextUtil.clearHttpMetricTags();
        }
    }

    @Override
    public String doHttpPut(String uri, Object body) {
        try {
            JobContextUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            JobContextUtil.addHttpMetricTag(Tag.of("api_name", uri));
            return httpHelper.put(buildUrl(uri), DEFAULT_CHARSET, JsonUtils.toJson(body), buildAuthHeader());
        } catch (Exception e) {
            log.error("Fail to request IAM", e);
            return null;
        } finally {
            JobContextUtil.clearHttpMetricTags();
        }
    }

    @Override
    public String doHttpDelete(String uri) {
        try {
            JobContextUtil.setHttpMetricName(CommonMetricNames.IAM_API_HTTP);
            JobContextUtil.addHttpMetricTag(Tag.of("api_name", uri));
            return httpHelper.delete(buildUrl(uri), buildAuthHeader());
        } finally {
            JobContextUtil.clearHttpMetricTags();
        }
    }

    private String buildUrl(String uri) {
        return iamConfiguration.getIamBaseUrl() + uri;
    }

    private List<Header> buildAuthHeader() {
        List<Header> headerList = new ArrayList<>();
        headerList.add(new BasicHeader(HttpHeader.BK_APP_CODE, iamConfiguration.getAppCode()));
        headerList.add(new BasicHeader(HttpHeader.BK_APP_SECRET, iamConfiguration.getAppSecret()));
        return headerList;
    }

}
