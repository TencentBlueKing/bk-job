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

package com.tencent.bk.job.common.web.interceptor;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.model.RepeatableReadHttpServletResponse;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.StringJoiner;

@Slf4j
@Component
public class EsbApiLogInterceptor extends HandlerInterceptorAdapter {

    private static final String ATTR_REQUEST_START = "request-start";
    private static final String ATTR_API_NAME = "api-name";
    private static final String ATTR_USERNAME = "username";
    private static final String ATTR_APP_CODE = "app-code";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(request instanceof RepeatableReadWriteHttpServletRequest)) {
            return true;
        }
        RepeatableReadWriteHttpServletRequest wrapperRequest = (RepeatableReadWriteHttpServletRequest) request;
        String desensitizedBody = "";
        String desensitizedQueryParams = "";
        String username = "";
        String appCode = "";
        String apiName = "";
        String lang = request.getHeader(JobCommonHeaders.BK_GATEWAY_LANG);
        String requestId = request.getHeader(JobCommonHeaders.BK_GATEWAY_REQUEST_ID);

        try {
            request.setAttribute(ATTR_REQUEST_START, System.currentTimeMillis());
            apiName = getAPIName(wrapperRequest.getRequestURI());
            request.setAttribute(ATTR_API_NAME, apiName);
            if (request.getMethod().equals(HttpMethod.POST.name())
                || request.getMethod().equals(HttpMethod.PUT.name())) {
                if (StringUtils.isNotBlank(wrapperRequest.getBody())) {
                    ObjectNode jsonBody = (ObjectNode) JsonUtils.toJsonNode(wrapperRequest.getBody());
                    if (jsonBody == null) {
                        return true;
                    }
                    username = jsonBody.get("bk_username") == null ? null : jsonBody.get("bk_username").asText();
                    appCode = jsonBody.get("bk_app_code") == null ? null : jsonBody.get("bk_app_code").asText();

                    // hidden sensitive data
                    jsonBody.set("bk_app_secret", TextNode.valueOf("***"));
                    desensitizedBody = jsonBody.toString();
                }
            } else if (request.getMethod().equals(HttpMethod.GET.name())) {
                username = request.getParameter("bk_username");
                appCode = request.getParameter("bk_app_code");
                desensitizedQueryParams = desensitizeQueryParams(request.getQueryString());

                request.setAttribute(ATTR_USERNAME, username);
                request.setAttribute(ATTR_APP_CODE, appCode);
            }
        } catch (Throwable e) {
            return true;
        } finally {
            log.info("request-id:{}|lang:{}|API:{}|uri:{}|appCode:{}|username:{}|body:{}|queryParams:{}", requestId,
                lang, apiName,
                request.getRequestURI(), appCode, username, desensitizedBody, desensitizedQueryParams);
        }
        return true;
    }

    private String desensitizeQueryParams(String queryParams) {
        String desensitizedQueryParams = queryParams;
        if (StringUtils.isNotEmpty(queryParams)) {
            StringJoiner joiner = new StringJoiner("&");
            String[] params = queryParams.split("&");
            if (params.length > 0) {
                for (String paramNameAndValue : params) {
                    String[] paramParts = paramNameAndValue.split("=");
                    if (paramParts.length >= 1) {
                        String paramName = paramParts[0];
                        String paramValue = paramParts.length == 2 ? paramParts[1] : "";
                        if (StringUtils.isNotEmpty(paramName) && paramName.trim().equals("bk_app_secret")) {
                            joiner.add(paramName + "=" + "***");
                        } else {
                            joiner.add(paramName + "=" + paramValue);
                        }
                        desensitizedQueryParams = joiner.toString();
                    }
                }
            }
        }
        return desensitizedQueryParams;
    }

    private String getAPIName(String uri) {
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception
        ex) throws Exception {
        if (!(response instanceof RepeatableReadHttpServletResponse)) {
            return;
        }
        RepeatableReadHttpServletResponse wrapperResponse = (RepeatableReadHttpServletResponse) response;
        try {
            Long startTimeInMills = (Long) request.getAttribute(ATTR_REQUEST_START);
            String apiName = (String) request.getAttribute(ATTR_API_NAME);
            String appCode = (String) request.getAttribute(ATTR_APP_CODE);
            String username = (String) request.getAttribute(ATTR_USERNAME);
            String requestId = request.getHeader(JobCommonHeaders.BK_GATEWAY_REQUEST_ID);
            int respStatus = response.getStatus();
            long cost = System.currentTimeMillis() - startTimeInMills;
            log.info("request-id:{}|API:{}|uri:{}|appCode:{}|username:{}|status:{}|resp:{}|cost:{}",
                requestId,
                apiName,
                request.getRequestURI(),
                appCode,
                username,
                respStatus,
                StringUtil.substring(wrapperResponse.getBodyAsText(), 10000),
                cost);
        } catch (Throwable e) {
            log.warn("Handle after completion fail", e);
        } finally {

            super.afterCompletion(request, response, handler, ex);
        }
    }
}
