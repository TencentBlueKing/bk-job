/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.gateway.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.config.EsbProperties;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.gateway.config.BkGatewayConfig;
import com.tencent.bk.job.gateway.model.esb.EsbPublicKeyDTO;
import com.tencent.bk.job.gateway.service.OpenApiJwtPublicKeyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class OpenApiJwtPublicKeyServiceImpl implements OpenApiJwtPublicKeyService {

    private final AppProperties appProperties;
    private final EsbProperties esbProperties;
    private final RestTemplate restTemplate;
    private volatile String esbJwtPublicKey;
    private volatile String bkApiGatewayPublicKey;
    private final BkApiGatewayProperties bkApiGatewayProperties;
    private final BkGatewayConfig bkApiGatewayConfig;

    private static final String URI_BK_APIGW_JWT_PUBLIC_KEY = "/api/v1/apis/{api_name}/public_key/";

    @Autowired
    public OpenApiJwtPublicKeyServiceImpl(AppProperties appProperties,
                                          EsbProperties esbProperties,
                                          RestTemplate restTemplate,
                                          BkApiGatewayProperties bkApiGatewayProperties,
                                          BkGatewayConfig bkApiGatewayConfig) {
        this.appProperties = appProperties;
        this.esbProperties = esbProperties;
        this.restTemplate = restTemplate;
        this.bkApiGatewayProperties = bkApiGatewayProperties;
        this.bkApiGatewayConfig = bkApiGatewayConfig;
    }

    @Override
    public String getEsbJWTPublicKey() {
        if (StringUtils.isNotEmpty(esbJwtPublicKey)) {
            return esbJwtPublicKey;
        }
        String url = getEsbUrl() + "api/c/compapi/v2/esb/get_api_public_key?bk_app_code={bk_app_code}&bk_app_secret" +
            "={bk_app_secret}&bk_username=admin";
        Map<String, Object> variables = new HashMap<>();
        variables.put("bk_app_code", appProperties.getCode());
        variables.put("bk_app_secret", appProperties.getSecret());
        EsbResp<EsbPublicKeyDTO> resp = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<EsbResp<EsbPublicKeyDTO>>() {
            }, variables).getBody();
        log.info("Get esb jwt public key, resp: {}", resp);
        if (resp == null || !resp.getCode().equals(ErrorCode.RESULT_OK) || resp.getData() == null) {
            log.error("Get esb jwt public key fail!");
            throw new RuntimeException("Get esb jwt public key fail");
        }
        String esbJwtPublicKey = resp.getData().getPublicKey();
        this.esbJwtPublicKey = esbJwtPublicKey;
        return esbJwtPublicKey;
    }

    @Override
    public String getBkApiGatewayJWTPublicKey() {
        if (StringUtils.isNotEmpty(bkApiGatewayPublicKey)) {
            return bkApiGatewayPublicKey;
        }
        String url = getBkApiGatewayUrl() + URI_BK_APIGW_JWT_PUBLIC_KEY.replace("{api_name}",
            bkApiGatewayConfig.getGatewayName());
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put("bk_app_code", appProperties.getCode());
        authInfo.put("bk_app_secret", appProperties.getSecret());
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Bkapi-Authorization", JsonUtils.toJson(authInfo));
        EsbResp<EsbPublicKeyDTO> resp = restTemplate.exchange(
            url,
            HttpMethod.GET,
            new HttpEntity<>(null, headers),
            new ParameterizedTypeReference<EsbResp<EsbPublicKeyDTO>>() {
            }
        ).getBody();

        log.info("Get bkApiGateway jwt public key, resp: {}", resp);
        if (resp == null || !resp.getCode().equals(ErrorCode.RESULT_OK) || resp.getData() == null) {
            log.error("Get bkApiGateway jwt public key fail!");
            throw new RuntimeException("Get gateway jwt public key fail");
        }
        this.bkApiGatewayPublicKey = resp.getData().getPublicKey();
        return bkApiGatewayPublicKey;
    }

    private String getEsbUrl() {
        String esbUrl = esbProperties.getService().getUrl();
        if (StringUtils.isEmpty(esbUrl)) {
            throw new RuntimeException("Illegal esb url!");
        }
        if (!esbUrl.endsWith("/")) {
            esbUrl = esbUrl + "/";
        }
        return esbUrl;
    }

    private String getBkApiGatewayUrl() {
        String bkApiGatewayUrl = bkApiGatewayProperties.getBkApiGateway().getUrl();
        if (StringUtils.isEmpty(bkApiGatewayUrl)) {
            throw new RuntimeException("Illegal gateway url!");
        }
        if (!bkApiGatewayUrl.endsWith("/")) {
            bkApiGatewayUrl = bkApiGatewayUrl + "/";
        }
        return bkApiGatewayUrl + "prod/";
    }
}
