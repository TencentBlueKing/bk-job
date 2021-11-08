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
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.model.ActionPolicies;
import com.tencent.bk.job.upgrader.model.IamReq;
import com.tencent.bk.job.upgrader.model.IamResp;
import com.tencent.bk.job.upgrader.model.Policy;
import com.tencent.bk.job.upgrader.model.PolicyListReq;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限中心接口调用客户端
 */
@Slf4j
public class IamClient extends AbstractIamClient {

    /**
     * IAM API 处理请求成功
     */
    private static final int RESULT_OK = 0;

    private static final String URL_QUERY_POLICIES = "/api/v1/systems/{systemId}/policies";

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();

    public IamClient(String iamHostUrl, String appCode, String appSecret) {
        super(iamHostUrl, appCode, appSecret);
    }

    private <R> R getIamRespByReq(String method, String uri, IamReq reqBody,
                                  TypeReference<R> typeReference) throws RuntimeException {
        return getIamRespByReq(method, uri, reqBody, typeReference, null);
    }

    @SuppressWarnings("all")
    private <T, R> R getIamRespByReq(String method, String uri, IamReq reqBody, TypeReference<R> typeReference,
                                     AbstractHttpHelper httpHelper) throws RuntimeException {
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
                throw new InternalException(ErrorCode.IAM_API_DATA_ERROR, "response is blank");
            } else {
                log.debug("success|method={}|uri={}|reqStr={}|respStr={}", method, uri, reqStr, respStr);
            }
            R result = JSON_MAPPER.fromJson(respStr, typeReference);
            IamResp iamResp = (IamResp) result;
            if (iamResp == null) {
                log.error("fail:iamResp is null after parse|method={}|uri={}|reqStr={}|respStr={}", method, uri,
                    reqStr, respStr);
                throw new InternalException(ErrorCode.IAM_API_DATA_ERROR, "iamResp is null after parse");
            } else if (iamResp.getCode() != RESULT_OK) {
                log.error(
                    "fail:iamResp code!=0|iamResp.code={}|iamResp" +
                        ".message={}|method={}|uri={}|reqStr={}|respStr={}"
                    , iamResp.getCode()
                    , iamResp.getMessage()
                    , method, uri, reqStr, respStr
                );
                throw new InternalException(ErrorCode.IAM_API_DATA_ERROR, "iamResp code!=0");
            }
            if (iamResp.getData() == null) {
                log.warn(
                    "warn:iamResp.getData() == null|iamResp.code={}|iamResp" +
                        ".message={}|method={}|uri={}|reqStr={}|respStr={}"
                    , iamResp.getCode()
                    , iamResp.getMessage()
                    , method, uri, reqStr, respStr
                );
            }
            return result;
        } catch (Throwable e) {
            String errorMsg = "Fail to request IAM data|method=" + method + "|uri=" + uri + "|reqStr=" + reqStr;
            log.error(errorMsg, e);
            throw new InternalException(e, ErrorCode.IAM_API_DATA_ERROR, "Fail to request IAM data");
        }
    }

    public ActionPolicies getActionPolicies(String actionId) {
        PolicyListReq req = new PolicyListReq();
        req.setActionId(actionId);
        int page = 1;
        req.setPage(page);
        req.setPageSize(500);
        IamResp<ActionPolicies> resp;
        ActionPolicies finalData = null;
        List<Policy> results;
        do {
            resp = getIamRespByReq(HttpGet.METHOD_NAME, URL_QUERY_POLICIES, req,
                new TypeReference<IamResp<ActionPolicies>>() {
                });
            if (finalData == null) {
                finalData = resp.getData();
                results = finalData.getResults();
                if (finalData.getResults() == null) {
                    finalData.setResults(new ArrayList<>());
                }
            } else {
                results = resp.getData().getResults();
                if (results != null && !results.isEmpty()) {
                    finalData.getResults().addAll(results);
                } else {
                    log.info("There is no data in page {}, end", page);
                }
            }
            req.setPage(++page);
        } while (results != null && !results.isEmpty());
        return finalData;
    }
}
