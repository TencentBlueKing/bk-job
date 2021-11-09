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

package com.tencent.bk.job.gateway.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.gateway.config.BkConfig;
import com.tencent.bk.job.gateway.model.esb.EsbPublicKeyDTO;
import com.tencent.bk.job.gateway.service.EsbJwtPublicKeyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class EsbJwtPublicKeyServiceImpl implements EsbJwtPublicKeyService {

    private BkConfig bkConfig;
    private RestTemplate restTemplate;
    private volatile String publicKey;

    @Autowired
    public EsbJwtPublicKeyServiceImpl(BkConfig bkConfig, RestTemplate restTemplate) {
        this.bkConfig = bkConfig;
        this.restTemplate = restTemplate;
    }

    @Override
    public String getEsbJWTPublicKey() {
        if (StringUtils.isNotEmpty(publicKey)) {
            return publicKey;
        }
        String url = getEsbUrl() + "api/c/compapi/v2/esb/get_api_public_key?bk_app_code={bk_app_code}&bk_app_secret" +
            "={bk_app_secret}&bk_username=admin";
        Map<String, Object> variables = new HashMap<>();
        variables.put("bk_app_code", bkConfig.getAppCode());
        variables.put("bk_app_secret", bkConfig.getAppSecret());
        EsbResp<EsbPublicKeyDTO> resp = restTemplate.exchange(url, HttpMethod.GET, null,
            new ParameterizedTypeReference<EsbResp<EsbPublicKeyDTO>>() {
            }, variables).getBody();
        log.info("Get esb jwt public key, resp: {}", resp);
        if (resp == null || !resp.getCode().equals(ErrorCode.RESULT_OK) || resp.getData() == null) {
            log.error("Get esb jwt public key fail!");
            throw new RuntimeException("Get esb jwt public key fail");
        }
        String publicKey = resp.getData().getPublicKey();
        log.info("Get esb public key success, public key : {}", publicKey);
        this.publicKey = publicKey;
        return publicKey;
    }

    private String getEsbUrl() {
        String esbUrl = bkConfig.getEsbUrl();
        if (StringUtils.isEmpty(esbUrl)) {
            throw new RuntimeException("Illegal esb url!");
        }
        if (!esbUrl.endsWith("/")) {
            esbUrl = esbUrl + "/";
        }
        return esbUrl;
    }
}
