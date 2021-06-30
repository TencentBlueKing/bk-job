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

package com.tencent.bk.job.upgrader.client;

import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.DefaultHttpHelper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.model.IamReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.util.ArrayList;
import java.util.List;

import static com.tencent.bk.job.common.constant.HttpHeader.HDR_CONTENT_TYPE;

@Slf4j
public abstract class AbstractIamClient {
    private String iamHostUrl;
    private String appSecret;
    private String appCode;
    private AbstractHttpHelper defaultHttpHelper = new DefaultHttpHelper();

    public AbstractIamClient(String iamHostUrl, String appCode, String appSecret) {
        this.iamHostUrl = iamHostUrl;
        this.appCode = appCode;
        this.appSecret = appSecret;
    }

    public String doHttpGet(String uri, IamReq params) throws Exception {
        return doHttpGet(uri, params, defaultHttpHelper);
    }

    private List<Header> getBasicHeaders() {
        List<Header> headerList = new ArrayList<>();
        headerList.add(new BasicHeader("X-Bk-App-Code", appCode));
        headerList.add(new BasicHeader("X-Bk-App-Secret", appSecret));
        headerList.add(new BasicHeader("X-Bk-IAM-Version", "1"));
        return headerList;
    }

    public String doHttpGet(String uri, IamReq params, AbstractHttpHelper httpHelper) throws Exception {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        boolean error = false;
        long start = System.currentTimeMillis();
        String responseBody = null;
        String url = iamHostUrl;
        try {
            if (!iamHostUrl.endsWith("/") && !uri.startsWith("/")) {
                url = iamHostUrl + "/" + uri + params.toUrlParams();
            } else {
                url = iamHostUrl + uri + params.toUrlParams();
            }
            responseBody = httpHelper.get(url, getBasicHeaders());
            return responseBody;
        } catch (Exception e) {
            log.warn("Get url {}| params={}| exception={}", iamHostUrl + uri,
                JsonUtils.toJsonWithoutSkippedFields(params),
                e.getMessage());
            error = true;
            throw e;
        } finally {
            log.info("Get url {}| error={}| params={}| time={}| resp={}", iamHostUrl + uri, error,
                JsonUtils.toJsonWithoutSkippedFields(params), (System.currentTimeMillis() - start), responseBody);
        }
    }

    protected <T extends IamReq> String doHttpPost(String uri, T params) throws Exception {
        return doHttpPost(uri, params, defaultHttpHelper);
    }

    protected <T extends IamReq> String doHttpPost(
        String uri, T params,
        AbstractHttpHelper httpHelper
    ) throws Exception {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        boolean error = false;
        long start = System.currentTimeMillis();
        String responseBody = null;
        try {
            String url;
            if (!iamHostUrl.endsWith("/") && !uri.startsWith("/")) {
                url = iamHostUrl + "/" + uri;
            } else {
                url = iamHostUrl + uri;
            }
            List<Header> headerList = getBasicHeaders();
            headerList.add(new BasicHeader(HDR_CONTENT_TYPE, "application/json"));
            responseBody = httpHelper.post(url, "UTF-8", buildPostBody(params), headerList);
            return responseBody;
        } catch (Exception e) {
            log.warn("Post url {}| params={}| exception={}", uri, JsonUtils.toJsonWithoutSkippedFields(params),
                e.getMessage());
            error = true;
            throw e;
        } finally {
            log.info("Post url {}| error={}| params={}| time={}| resp={}", uri, error,
                JsonUtils.toJsonWithoutSkippedFields(params), (System.currentTimeMillis() - start), responseBody);
        }
    }

    protected <T extends IamReq> String buildPostBody(T params) {
        return JsonUtils.toJson(params);
    }

}
