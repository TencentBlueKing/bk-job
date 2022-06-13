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

import com.tencent.bk.job.common.util.http.ExtHttpHelper;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 蓝鲸API调用客户端 - for BK API GATEWAY
 */
@Slf4j
public abstract class AbstractBkApiClient {

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();
    public static final String BK_LANG_EN = "en";
    private final String bkApiGatewayUrl;
    private final String appSecret;
    private final String appCode;
    private final String lang;
    private final ExtHttpHelper defaultHttpHelper = HttpHelperFactory.getDefaultHttpHelper();

    public AbstractBkApiClient(String bkApiGatewayUrl,
                               String appCode,
                               String appSecret,
                               String lang) {
        this.bkApiGatewayUrl = bkApiGatewayUrl;
        this.appCode = appCode;
        this.appSecret = appSecret;
        if (StringUtils.isNotBlank(lang)) {
            this.lang = lang;
        } else {
            this.lang = BK_LANG_EN;
        }
    }

//    /**
//     * 生成通过Web界面发过来的生成SDK调用请求构造协议基础参数
//     *
//     * @param reqClass 要构建返回的协议Req类
//     * @param bkToken  Cookie
//     * @param userName 用户id
//     * @param owner    开发商code-如果没有传入null
//     * @return 如果指定的协议请求Req类有问题，请返回null
//     */
//    public <T extends EsbReq> T makeBaseReqByWeb(Class<T> reqClass, String bkToken, String userName, String owner) {
//        T esbReq = null;
//        try {
//            esbReq = reqClass.newInstance();
//            esbReq.setBkToken(bkToken);
//            esbReq.setUserName(userName);
//            esbReq.setAppCode(appCode);
//            esbReq.setAppSecret(appSecret);
//            if (StringUtils.isEmpty(owner)) {
//                esbReq.setBkSupplierAccount("0");
//            } else {
//                esbReq.setBkSupplierAccount(owner);
//            }
//        } catch (InstantiationException | IllegalAccessException e) {
//            log.error("makeWebReq fail", e);
//        }
//        return esbReq;
//    }
//
//    /**
//     * 针对无登录态的SDK调用请求，构建协议请求Req类基础参数
//     *
//     * @param reqClass 要构建返回的协议Req类
//     * @param userName 用户id
//     * @param owner    开发商code-如果没有传入null
//     * @return 如果指定的协议请求Req类有问题，请返回null
//     */
//    public <T extends EsbReq> T makeBaseReq(Class<T> reqClass, String userName, String owner) {
//        T esbReq = null;
//        try {
//            esbReq = reqClass.newInstance();
//            esbReq.setUserName(userName);
//            esbReq.setAppCode(appCode);
//            esbReq.setAppSecret(appSecret);
//            if (StringUtils.isEmpty(owner)) {
//                esbReq.setBkSupplierAccount("0");
//            } else {
//                esbReq.setBkSupplierAccount(owner);
//            }
//        } catch (InstantiationException | IllegalAccessException e) {
//            log.error("makeWebReq fail", e);
//        }
//        return esbReq;
//    }
//
//    public String doHttpGet(String uri, EsbReq params) {
//        return doHttpGet(uri, params, defaultHttpHelper);
//    }
//
//    public String doHttpGet(String uri, EsbReq params, ExtHttpHelper httpHelper) {
//        if (httpHelper == null) {
//            httpHelper = defaultHttpHelper;
//        }
//        boolean error = false;
//        long start = System.currentTimeMillis();
//        String responseBody = null;
//        String url;
//        try {
//            if (!bkApiGatewayUrl.endsWith("/") && !uri.startsWith("/")) {
//                url = bkApiGatewayUrl + "/" + uri + params.toUrlParams();
//            } else {
//                url = bkApiGatewayUrl + uri + params.toUrlParams();
//            }
//            Header[] header;
//            if (useEsbTestEnv) {
//                header = new Header[2];
//                header[0] = new BasicHeader(HDR_BK_LANG, lang);
//                header[1] = new BasicHeader("x-use-test-env", "1");
//            } else {
//                header = new Header[1];
//                header[0] = new BasicHeader(HDR_BK_LANG, lang);
//            }
//            responseBody = httpHelper.get(url, header);
//            return responseBody;
//        } catch (Throwable e) {
//            log.error("Get url {}| params={}| exception={}", bkApiGatewayUrl + uri,
//                JsonUtils.toJsonWithoutSkippedFields(params),
//                e.getMessage());
//            error = true;
//            throw e;
//        } finally {
//            log.info("Get url {}| error={}| params={}| time={}| resp={}", bkApiGatewayUrl + uri, error,
//                JsonUtils.toJsonWithoutSkippedFields(params), (System.currentTimeMillis() - start), responseBody);
//        }
//    }
//
//    protected <T extends EsbReq> String doHttpPost(
//        String uri,
//        T params,
//        ExtHttpHelper httpHelper) {
//
//        if (httpHelper == null) {
//            httpHelper = defaultHttpHelper;
//        }
//        boolean error = false;
//        long start = System.currentTimeMillis();
//        String responseBody = null;
//        try {
//            String url;
//            if (!bkApiGatewayUrl.endsWith("/") && !uri.startsWith("/")) {
//                url = bkApiGatewayUrl + "/" + uri;
//            } else {
//                url = bkApiGatewayUrl + uri;
//            }
//            Header[] header;
//            if (useEsbTestEnv) {
//                header = new Header[3];
//                header[0] = new BasicHeader(HDR_BK_LANG, lang);
//                header[1] = new BasicHeader(HDR_CONTENT_TYPE, "application/json");
//                header[2] = new BasicHeader("x-use-test-env", "1");
//            } else {
//                header = new Header[2];
//                header[0] = new BasicHeader(HDR_BK_LANG, lang);
//                header[1] = new BasicHeader(HDR_CONTENT_TYPE, "application/json");
//            }
//            responseBody = httpHelper.post(url, "UTF-8", buildPostBody(params), header);
//            return responseBody;
//        } catch (Exception e) {
//            log.error("Post url {}| params={}| exception={}", uri, JsonUtils.toJsonWithoutSkippedFields(params),
//                e.getMessage());
//            error = true;
//            throw e;
//        } finally {
//            log.info("Post url {}| error={}| params={}| time={}| resp={}", uri, error,
//                JsonUtils.toJsonWithoutSkippedFields(params), (System.currentTimeMillis() - start), responseBody);
//        }
//    }
//
//    protected <T extends EsbReq> String buildPostBody(T params) {
//        return JsonUtils.toJson(params);
//    }
//
//
//    public <R> EsbResp<R> getEsbRespByReq(String method, String uri, EsbReq reqBody,
//                                          TypeReference<EsbResp<R>> typeReference) {
//        return getEsbRespByReq(method, uri, reqBody, typeReference, null);
//    }
//
//    public <R> EsbResp<R> getEsbRespByReq(String method, String uri, EsbReq reqBody,
//                                          TypeReference<EsbResp<R>> typeReference,
//                                          ExtHttpHelper httpHelper) {
//        String reqStr = JsonUtils.toJsonWithoutSkippedFields(reqBody);
//        String respStr = null;
//        try {
//            if (method.equals(HttpGet.METHOD_NAME)) {
//                respStr = doHttpGet(uri, reqBody, httpHelper);
//            } else if (method.equals(HttpPost.METHOD_NAME)) {
//                respStr = doHttpPost(uri, reqBody, httpHelper);
//            }
//            if (StringUtils.isBlank(respStr)) {
//                String errorMsg = method + " " + uri + ", error: " + "Response is blank";
//                log.error(errorMsg);
//                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
//            } else {
//                log.debug("success|method={}|uri={}|reqStr={}|respStr={}", method, uri, reqStr, respStr);
//            }
//            EsbResp<R> esbResp = JSON_MAPPER.fromJson(respStr, typeReference);
//            if (esbResp == null) {
//                String errorMsg = method + " " + uri + ", error: " + "Response is blank after parse";
//                log.error(errorMsg);
//                throw new InternalException(errorMsg, ErrorCode.API_ERROR);
//            } else if (!esbResp.getResult()) {
//                log.warn(
//                    "fail:esbResp code!=0|esbResp.requestId={}|esbResp.code={}|esbResp" +
//                        ".message={}|method={}|uri={}|reqStr={}|respStr={}",
//                    esbResp.getRequestId(),
//                    esbResp.getCode(),
//                    esbResp.getMessage(),
//                    method, uri, reqStr, respStr
//                );
//            }
//            if (esbResp.getData() == null) {
//                log.warn(
//                    "warn:esbResp.getData() == null|esbResp.requestId={}|esbResp.code={}|esbResp" +
//                        ".message={}|method={}|uri={}|reqStr={}|respStr={}"
//                    , esbResp.getRequestId()
//                    , esbResp.getCode()
//                    , esbResp.getMessage()
//                    , method, uri, reqStr, respStr
//                );
//            }
//            return esbResp;
//        } catch (Throwable e) {
//            String errorMsg = "Fail to request ESB data|method=" + method
//                + "|uri=" + uri
//                + "|reqStr=" + reqStr
//                + "|respStr=" + respStr;
//            log.error(errorMsg, e);
//            throw new InternalException("Fail to request esb api", e, ErrorCode.API_ERROR);
//        }
//    }
}
