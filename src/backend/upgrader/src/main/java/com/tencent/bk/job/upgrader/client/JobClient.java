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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.http.BasicHttpReq;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.model.AppInfo;
import com.tencent.bk.job.upgrader.model.SimpleAppInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.util.List;

/**
 * Job接口调用客户端
 */
@Slf4j
public class JobClient extends AbstractJobClient {

    /**
     * Job API 处理请求成功
     */
    private static final int RESULT_OK = 0;

    private static final String URL_LIST_NORMAL_APPS = "/service/app/list/normal";

    private static final String URL_LIST_SPECIAL_APPS = "/service/app/list/special";

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();

    public JobClient(String jobHostUrl, String jobAuthToken) {
        super(jobHostUrl, jobAuthToken);
    }

    private <R> R getJobRespByReq(String method, String uri, BasicHttpReq reqBody,
                                  TypeReference<R> typeReference) {
        return getJobRespByReq(method, uri, reqBody, typeReference, null);
    }

    @SuppressWarnings("all")
    private <T, R> R getJobRespByReq(
        String method,
        String uri,
        BasicHttpReq reqBody,
        TypeReference<R> typeReference,
        ExtHttpHelper httpHelper
    ) {
        // URL模板变量替换
        uri = StringUtil.replacePathVariables(uri, reqBody);
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
        String respStr = null;
        try {
            if (method.equals(HttpGet.METHOD_NAME)) {
                respStr = doHttpGet(uri, reqBody, httpHelper);
            } else if (method.equals(HttpPost.METHOD_NAME)) {
                respStr = doHttpPost(uri, reqBody, httpHelper);
            }
            if (StringUtils.isBlank(respStr)) {
                log.error("fail:response is blank|method={}|uri={}|reqStr={}", method, uri, reqStr);
                throw new InternalException("response is blank", ErrorCode.API_ERROR);
            } else {
                log.debug("success|method={}|uri={}|reqStr={}|respStr={}", method, uri, reqStr, respStr);
            }
            R result =
                JSON_MAPPER.fromJson(respStr, typeReference);
            InternalResponse jobResp = (InternalResponse) result;
            if (jobResp == null) {
                log.error("fail:jobResp is null after parse|method={}|uri={}|reqStr={}|respStr={}", method, uri,
                    reqStr, respStr);
                throw new InternalException("jobResp is null after parse", ErrorCode.API_ERROR);
            } else if (jobResp.getCode() != RESULT_OK) {
                log.error(
                    "fail:jobResp code!=0|jobResp.code={}|jobResp" +
                        ".errorMessage={}|method={}|uri={}|reqStr={}|respStr={}",
                    jobResp.getCode(),
                    jobResp.getErrorMsg(),
                    method,
                    uri,
                    reqStr,
                    respStr
                );
                throw new InternalException("jobResp code!=0", ErrorCode.API_ERROR);
            }
            if (jobResp.getData() == null) {
                log.warn(
                    "warn:jobResp.getData() == null|jobResp.code={}|jobResp" +
                        ".errorMessage={}|method={}|uri={}|reqStr={}|respStr={}",
                    jobResp.getCode(),
                    jobResp.getErrorMsg(),
                    method,
                    uri,
                    reqStr,
                    respStr
                );
            }
            return result;
        } catch (Exception e) {
            String errorMsg = "Fail to request JOB data|method=" + method + "|uri=" + uri + "|reqStr=" + reqStr;
            log.error(errorMsg, e);
            throw new InternalException("Fail to request JOB data", ErrorCode.API_ERROR);
        }
    }

    public List<SimpleAppInfo> listNormalApps() {
        Response<List<SimpleAppInfo>> resp = getJobRespByReq(
            HttpGet.METHOD_NAME,
            URL_LIST_NORMAL_APPS,
            new BasicHttpReq(),
            new TypeReference<Response<List<SimpleAppInfo>>>() {
            });
        return resp.getData();
    }

    public List<AppInfo> listSpecialApps() {
        Response<List<AppInfo>> resp = getJobRespByReq(
            HttpGet.METHOD_NAME,
            URL_LIST_SPECIAL_APPS,
            new BasicHttpReq(),
            new TypeReference<Response<List<AppInfo>>>() {
            });
        return resp.getData();
    }
}
