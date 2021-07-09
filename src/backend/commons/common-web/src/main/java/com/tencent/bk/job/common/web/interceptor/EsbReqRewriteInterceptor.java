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
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class EsbReqRewriteInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(request instanceof RepeatableReadWriteHttpServletRequest)) {
            return true;
        }
        RepeatableReadWriteHttpServletRequest wrapperRequest = (RepeatableReadWriteHttpServletRequest) request;

        String appCode = request.getHeader(JobCommonHeaders.APP_CODE);
        String username = request.getHeader(JobCommonHeaders.USERNAME);

        try {
            if (request.getMethod().equals(HttpMethod.POST.name())
                || request.getMethod().equals(HttpMethod.PUT.name())) {
                if (StringUtils.isNotBlank(wrapperRequest.getBody())) {
                    ObjectNode jsonBody = (ObjectNode) JsonUtils.toJsonNode(wrapperRequest.getBody());
                    if (jsonBody == null) {
                        return true;
                    }
                    // reset username and appCode
                    if (StringUtils.isNotBlank(username)) {
                        String usernameInBody = jsonBody.get("bk_username").asText();
                        if (StringUtils.isNotEmpty(usernameInBody) && !usernameInBody.equals(username)) {
                            log.error("Invalid username, usernameInBody: {}, username: {}", usernameInBody, username);
                        }
                        jsonBody.set("bk_username", new TextNode(username));
                    } else {
                        log.error("Header {} is missing", JobCommonHeaders.USERNAME);
                    }

                    if (StringUtils.isNotBlank(appCode)) {
                        String appCodeInBody = jsonBody.get("bk_app_code").asText();
                        if (StringUtils.isNotEmpty(appCodeInBody) && !appCodeInBody.equals(appCode)) {
                            log.error("Invalid appCode, appCodeInBody: {}, appCode: {}", appCodeInBody, appCode);
                        }
                        jsonBody.set("bk_app_code", new TextNode(appCode));
                    } else {
                        log.error("Header {} is missing", JobCommonHeaders.APP_CODE);
                    }

                    // hidden sensitive data
                    jsonBody.set("bk_app_secret", TextNode.valueOf("***"));
                    wrapperRequest.setBody(jsonBody.toString());
                }
            }
        } catch (Throwable e) {
            log.warn("Rewrite EsbReq error", e);
            return true;
        }
        return true;
    }
}
