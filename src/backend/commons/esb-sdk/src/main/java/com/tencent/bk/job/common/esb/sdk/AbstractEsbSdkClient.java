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

package com.tencent.bk.job.common.esb.sdk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.bk.job.common.constant.HttpHeader.HDR_BK_LANG;
import static com.tencent.bk.job.common.constant.HttpHeader.HDR_CONTENT_TYPE;

/**
 * ESB API 调用基础实现
 */
public abstract class AbstractEsbSdkClient {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();
    public static final String BK_LANG_EN = "en";
    private final String esbHostUrl;
    private final String appSecret;
    private final String appCode;
    private final String lang;
    private final ExtHttpHelper defaultHttpHelper = HttpHelperFactory.getDefaultHttpHelper();
    /**
     * 是否对接ESB测试环境
     */
    private final boolean useEsbTestEnv;

    public AbstractEsbSdkClient(String esbHostUrl, String appCode, String appSecret,
                                String lang, boolean useEsbTestEnv) {
        this.esbHostUrl = esbHostUrl;
        this.appCode = appCode;
        this.appSecret = appSecret;
        if (StringUtils.isNotBlank(lang)) {
            this.lang = lang;
        } else {
            this.lang = BK_LANG_EN;
        }
        this.useEsbTestEnv = useEsbTestEnv;
    }

    public <R> EsbResp<R> getEsbRespByReq(String method,
                                          String uri,
                                          EsbReq reqBody,
                                          TypeReference<EsbResp<R>> typeReference,
                                          BkApiLogStrategy logStrategy) {
        return getEsbRespByReq(method, uri, reqBody, typeReference, null, logStrategy);
    }

    public <R> EsbResp<R> getEsbRespByReq(String method,
                                          String uri,
                                          EsbReq reqBody,
                                          TypeReference<EsbResp<R>> typeReference) {
        return getEsbRespByReq(method, uri, reqBody, typeReference, null, null);
    }

    public <R> EsbResp<R> getEsbRespByReq(String method,
                                          String uri,
                                          EsbReq reqBody,
                                          TypeReference<EsbResp<R>> typeReference,
                                          ExtHttpHelper httpHelper,
                                          BkApiLogStrategy logStrategy) {
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
        long startTime = System.currentTimeMillis();
        BkApiContext<? extends EsbReq, R> apiContext
            = new BkApiContext<>(method, uri, reqBody, null, null, 0, false);

        if (logStrategy != null) {
            logStrategy.logReq(log, apiContext);
        } else {
            if (log.isInfoEnabled()) {
                log.info("[AbstractEsbSdkClient] Request|method={}|uri={}|reqStr={}", method, uri, reqStr);
            }
        }

        try {
            requestEsbApi(apiContext, typeReference, httpHelper);
            apiContext.setSuccess(true);
            return apiContext.getResp();
        } finally {
            apiContext.setCostTime(System.currentTimeMillis() - startTime);
            if (logStrategy != null) {
                logStrategy.logResp(log, apiContext);
            } else {
                if (log.isInfoEnabled()) {
                    log.info("[AbstractEsbSdkClient] Response|method={}|uri={}|success={}|costTime={}|resp={}|",
                        method, uri, apiContext.isSuccess(), apiContext.getCostTime(),
                        apiContext.getOriginResp());
                }
            }
        }
    }

    private <R> void requestEsbApi(BkApiContext<? extends EsbReq, R> apiContext,
                                   TypeReference<EsbResp<R>> typeReference,
                                   ExtHttpHelper httpHelper) {
        String method = apiContext.getMethod();
        String uri = apiContext.getUri();
        EsbReq reqBody = apiContext.getReq();
        String reqStr = JsonUtils.toJsonWithoutSkippedFields(apiContext.getReq());
        EsbResp<R> esbResp;
        String respStr = null;
        try {
            if (method.equals(HttpGet.METHOD_NAME)) {
                respStr = doHttpGet(uri, reqBody, httpHelper);
            } else if (method.equals(HttpPost.METHOD_NAME)) {
                respStr = doHttpPost(uri, reqBody, httpHelper);
            }
            apiContext.setOriginResp(respStr);
            if (StringUtils.isBlank(respStr)) {
                String errorMsg = "[AbstractEsbSdkClient] " + method + " " + uri + ", error: " + "Response is blank";
                log.error(errorMsg);
                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
            }

            esbResp = JSON_MAPPER.fromJson(respStr, typeReference);
            apiContext.setResp(esbResp);
            if (!esbResp.getResult()) {
                log.warn(
                    "[AbstractEsbSdkClient] fail:esbResp code!=0|esbResp.requestId={}|esbResp.code={}|esbResp" +
                        ".message={}|method={}|uri={}|reqStr={}|respStr={}",
                    esbResp.getRequestId(),
                    esbResp.getCode(),
                    esbResp.getMessage(),
                    method,
                    uri,
                    reqStr,
                    respStr
                );
            }
            if (esbResp.getData() == null) {
                log.warn(
                    "[AbstractEsbSdkClient] warn:esbResp.getData() == null|esbResp.requestId={}|esbResp" +
                        ".code={}|esbResp.message={}|method={}|uri={}|reqStr={}|respStr={}",
                    esbResp.getRequestId(),
                    esbResp.getCode(),
                    esbResp.getMessage(),
                    method,
                    uri,
                    reqStr,
                    respStr
                );
            }
        } catch (Throwable e) {
            String errorMsg = "[AbstractEsbSdkClient] Fail to request ESB api|method=" + method
                + "|uri=" + uri
                + "|reqStr=" + reqStr
                + "|respStr=" + respStr;
            log.error(errorMsg, e);
            apiContext.setSuccess(false);
            throw new InternalException("Fail to request esb api", e, ErrorCode.API_ERROR);
        }
    }

    public <R> EsbResp<R> getEsbRespByReq(String method,
                                          String uri,
                                          EsbReq reqBody,
                                          TypeReference<EsbResp<R>> typeReference,
                                          ExtHttpHelper httpHelper) {
        return getEsbRespByReq(method, uri, reqBody, typeReference, httpHelper, null);
    }

    protected <T extends EsbReq> T makeBaseReqByWeb(Class<T> reqClass, String bkToken) {
        return makeBaseReqByWeb(reqClass, bkToken, "", "");
    }

    /**
     * 生成通过Web界面发过来的生成SDK调用请求构造协议基础参数
     *
     * @param reqClass 要构建返回的协议Req类
     * @param bkToken  bkToken
     * @param userName 用户id
     * @param owner    开发商code-如果没有传入null
     * @return EsbReq
     */
    protected <T extends EsbReq> T makeBaseReqByWeb(Class<T> reqClass, String bkToken, String userName, String owner) {
        T esbReq;
        try {
            esbReq = reqClass.newInstance();
            esbReq.setBkToken(bkToken);
            esbReq.setUserName(userName);
            esbReq.setAppCode(appCode);
            esbReq.setAppSecret(appSecret);
            if (StringUtils.isEmpty(owner)) {
                esbReq.setBkSupplierAccount("0");
            } else {
                esbReq.setBkSupplierAccount(owner);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("[AbstractEsbSdkClient] makeWebReq fail", e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return esbReq;
    }

    /**
     * 针对无登录态的SDK调用请求，构建协议请求Req类基础参数
     *
     * @param reqClass 要构建返回的协议Req类
     * @param userName 用户id
     * @param owner    开发商code-如果没有传入null
     * @return EsbReq
     */
    protected <T extends EsbReq> T makeBaseReq(Class<T> reqClass, String userName, String owner) {
        T esbReq;
        try {
            esbReq = reqClass.newInstance();
            esbReq.setUserName(userName);
            esbReq.setAppCode(appCode);
            esbReq.setAppSecret(appSecret);
            if (StringUtils.isEmpty(owner)) {
                esbReq.setBkSupplierAccount("0");
            } else {
                esbReq.setBkSupplierAccount(owner);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("[AbstractEsbSdkClient] makeWebReq fail", e);
            throw new InternalException(e, ErrorCode.INTERNAL_ERROR);
        }
        return esbReq;
    }

    private String doHttpGet(String uri, EsbReq params, ExtHttpHelper httpHelper) {
        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        String responseBody;
        String url;
        if (!esbHostUrl.endsWith("/") && !uri.startsWith("/")) {
            url = esbHostUrl + "/" + uri + params.toUrlParams();
        } else {
            url = esbHostUrl + uri + params.toUrlParams();
        }
        Header[] header;
        if (useEsbTestEnv) {
            header = new Header[2];
            header[0] = new BasicHeader(HDR_BK_LANG, lang);
            header[1] = new BasicHeader("x-use-test-env", "1");
        } else {
            header = new Header[1];
            header[0] = new BasicHeader(HDR_BK_LANG, lang);
        }
        responseBody = httpHelper.get(url, header);
        return responseBody;
    }

    private <T extends EsbReq> String doHttpPost(String uri,
                                                 T params,
                                                 ExtHttpHelper httpHelper) {

        if (httpHelper == null) {
            httpHelper = defaultHttpHelper;
        }
        String responseBody;
        String url;
        if (!esbHostUrl.endsWith("/") && !uri.startsWith("/")) {
            url = esbHostUrl + "/" + uri;
        } else {
            url = esbHostUrl + uri;
        }
        Header[] header;
        if (useEsbTestEnv) {
            header = new Header[3];
            header[0] = new BasicHeader(HDR_BK_LANG, lang);
            header[1] = new BasicHeader(HDR_CONTENT_TYPE, "application/json");
            header[2] = new BasicHeader("x-use-test-env", "1");
        } else {
            header = new Header[2];
            header[0] = new BasicHeader(HDR_BK_LANG, lang);
            header[1] = new BasicHeader(HDR_CONTENT_TYPE, "application/json");
        }
        responseBody = httpHelper.post(url, "UTF-8", buildPostBody(params), header);
        return responseBody;
    }

    protected <T extends EsbReq> String buildPostBody(T params) {
        return JsonUtils.toJson(params);
    }
}
