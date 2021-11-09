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

package com.tencent.bk.job.common.paas.login;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.util.http.HttpConPoolUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class CustomLoginClient implements ILoginClient {
    private static final String API_GET_USER_INFO = "/user/get_info/";
    private String customLoginApiUrl;

    public CustomLoginClient(String customLoginApiUrl) {
        if (customLoginApiUrl.endsWith("/")) {
            this.customLoginApiUrl = customLoginApiUrl.substring(0, customLoginApiUrl.length() - 1);
        } else {
            this.customLoginApiUrl = customLoginApiUrl;
        }
    }

    @Override
    public BkUserDTO getUserInfoByToken(String token) {
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("app_code", "job");
        queryParams.put("bk_ticket", token);

        try {
            String response = doHttpGet(API_GET_USER_INFO, queryParams, "bk_ticket");
            log.info(response);
            if (StringUtils.isBlank(response)) {
                return null;
            }

            Resp<UserDTO> resp = JsonUtils.fromJson(response, new TypeReference<Resp<UserDTO>>() {
            });
            if (resp == null) {
                log.warn("Get user info, resp is empty");
                return null;
            }
            if (!resp.getRet().equals(Resp.SUCCESS_CODE)) {
                log.warn("Get user info return fail code:{}", resp.getRet());
                return null;
            }
            if (resp.getData() == null) {
                log.warn("Get user info, resp data is empty");
                return null;
            }
            BkUserDTO bkUserDto = new BkUserDTO();
            bkUserDto.setUsername(resp.getData().getUsername());
            return bkUserDto;
        } catch (Exception e) {
            log.error("Error occur when get user info", e);
            return null;
        }
    }

    private String doHttpGet(String uri, Map<String, String> queryParams, String secretField) {
        boolean error = false;
        long start = System.currentTimeMillis();
        String responseBody = null;
        String queryParamStr = buildQueryParams(queryParams);
        String url = customLoginApiUrl + uri + queryParamStr;
        try {
            responseBody = HttpConPoolUtil.get(false, url, null);
            return responseBody;
        } catch (Throwable e) {
            log.error("doHttpGet| url={}| params={}| exception={}", uri, buildPrintedParams(queryParams, secretField),
                e.getMessage());
            error = true;
            throw e;
        } finally {
            log.info("doHttpGet| error={}| url={}| params={}| time={}| resp={}", error, uri,
                buildPrintedParams(queryParams, secretField), (System.currentTimeMillis() - start), responseBody);
        }
    }

    private String buildQueryParams(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        List<String> queryItems = new ArrayList<>(queryParams.size());
        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            queryItems.add(queryParam.getKey() + "=" + urlEncode(queryParam.getValue()));
        }
        return "?" + String.join("&", queryItems);
    }

    private String buildPrintedParams(Map<String, String> queryParams, String skippedField) {
        if (queryParams == null || queryParams.isEmpty()) {
            return "";
        }

        List<String> queryItems = new ArrayList<>(queryParams.size());
        for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
            if (!queryParam.getKey().equalsIgnoreCase(skippedField)) {
                queryItems.add(queryParam.getKey() + "=" + queryParam.getValue());
            }
        }
        return String.join("&", queryItems);
    }

    private String urlEncode(String str) {
        try {
            return URLEncoder.encode(str, "utf-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encode failed");
        }
    }

    @Override
    public BkUserDTO getUserInfoByUserName(String userName) {
        return null;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    private static class Resp<T> {
        public static final Integer SUCCESS_CODE = 0;

        private Integer ret;

        private T data;
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    private static class UserDTO {
        private String username;
    }
}
