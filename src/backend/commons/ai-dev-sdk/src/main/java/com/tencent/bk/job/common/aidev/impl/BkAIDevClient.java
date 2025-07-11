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

package com.tencent.bk.job.common.aidev.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.aidev.IBkAIDevClient;
import com.tencent.bk.job.common.aidev.config.CustomPaasLoginProperties;
import com.tencent.bk.job.common.aidev.exception.BkAIDevException;
import com.tencent.bk.job.common.aidev.model.common.AIDevMessage;
import com.tencent.bk.job.common.aidev.model.req.AIDevInput;
import com.tencent.bk.job.common.aidev.model.req.AIDevReq;
import com.tencent.bk.job.common.aidev.model.req.AIDevReqConfig;
import com.tencent.bk.job.common.aidev.model.req.AIDevReqData;
import com.tencent.bk.job.common.aidev.model.resp.AIDevChoice;
import com.tencent.bk.job.common.aidev.model.resp.AIDevData;
import com.tencent.bk.job.common.aidev.model.resp.AIDevResp;
import com.tencent.bk.job.common.aidev.model.resp.AIDevResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.HttpMethodEnum;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.esb.model.OpenApiRequestInfo;
import com.tencent.bk.job.common.esb.sdk.BkApiClient;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import com.tencent.bk.job.common.util.http.HttpMetricUtil;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("SameParameterValue")
public class BkAIDevClient extends BkApiClient implements IBkAIDevClient {

    private static final String URI_GET_HUN_YUAN_ANSWER =
        "/aidev/intelligence/raw_service/model-self_host-hunyuan-ChatCompletion/execute/";

    private final AppProperties appProperties;
    private final BkApiGatewayProperties.ApiGwConfig bkAIDevConfig;
    private final CustomPaasLoginProperties customPaasLoginProperties;

    public BkAIDevClient(MeterRegistry meterRegistry,
                         AppProperties appProperties,
                         CustomPaasLoginProperties customPaasLoginProperties,
                         BkApiGatewayProperties bkApiGatewayProperties) {
        super(
            meterRegistry,
            CommonMetricNames.BK_AI_DEV_API,
            getBkAIDevUrlSafely(bkApiGatewayProperties),
            HttpHelperFactory.getDefaultHttpHelper()
        );
        this.appProperties = appProperties;
        this.bkAIDevConfig = bkApiGatewayProperties.getBkAIDev();
        this.customPaasLoginProperties = customPaasLoginProperties;
    }

    private static String getBkAIDevUrlSafely(BkApiGatewayProperties bkApiGatewayProperties) {
        if (bkApiGatewayProperties == null || bkApiGatewayProperties.getBkAIDev() == null) {
            return null;
        }
        return bkApiGatewayProperties.getBkAIDev().getUrl();
    }

    @Override
    public String getHunYuanAnswer(String token, List<AIDevMessage> messageHistoryList, String userInput) {
        AIDevReq req = buildAIDevReq(messageHistoryList, userInput);
        BkApiAuthorization authorization = buildAuthorization(token);
        AIDevResp<List<AIDevData>> resp = requestBkAIDevApi(
            HttpMethodEnum.POST,
            URI_GET_HUN_YUAN_ANSWER,
            req,
            authorization,
            new TypeReference<AIDevResp<List<AIDevData>>>() {
            },
            true
        );
        List<AIDevData> aiDevDataList = resp.getData();
        AIDevResult aiDevResult = aiDevDataList.get(0).getResult();
        List<AIDevChoice> aiDevChoiceList = aiDevResult.getChoices();
        AIDevMessage aiDevMessage = aiDevChoiceList.get(0).getMessage();
        return aiDevMessage.getContent();
    }

    private AIDevReq buildAIDevReq(List<AIDevMessage> messageHistoryList, String userInput) {
        AIDevReq req = new AIDevReq();
        req.setConfig(AIDevReqConfig.hunyuanConfig());
        AIDevReqData data = new AIDevReqData();
        List<AIDevInput> inputs = new ArrayList<>();
        AIDevInput input = new AIDevInput();
        List<AIDevMessage> messages;
        if (CollectionUtils.isEmpty(messageHistoryList)) {
            messages = new ArrayList<>();
        } else {
            messages = new ArrayList<>(messageHistoryList);
        }
        AIDevMessage message = new AIDevMessage();
        message.setRole(AIDevMessage.ROLE_USER);
        message.setContent(userInput);
        messages.add(message);
        input.setMessages(messages);
        inputs.add(input);
        data.setInputs(inputs);
        req.setData(data);
        return req;
    }

    private BkApiAuthorization buildAuthorization(String token) {
        if (customPaasLoginProperties.isEnabled()) {
            return BkApiAuthorization.bkTicketUserAuthorization(
                getAppCode(),
                getAppSecret(),
                token
            );
        } else {
            return BkApiAuthorization.bkTokenUserAuthorization(
                getAppCode(),
                getAppSecret(),
                token
            );
        }
    }

    private String getAppCode() {
        String appCode = bkAIDevConfig.getAppCode();
        if (StringUtils.isNotBlank(appCode)) {
            return appCode;
        }
        return appProperties.getCode();
    }

    private String getAppSecret() {
        String appSecret = bkAIDevConfig.getAppSecret();
        if (StringUtils.isNotBlank(appSecret)) {
            return appSecret;
        }
        return appProperties.getSecret();
    }

    /**
     * 通过ApiGateway请求AIDev API的统一入口，监控数据埋点位置
     *
     * @param method        Http方法
     * @param uri           请求地址
     * @param reqBody       请求体内容
     * @param typeReference 指定了返回值类型的TypeReference对象
     * @param <R>           泛型：返回值类型
     * @return 返回值类型实例
     */
    private <R> R requestBkAIDevApi(HttpMethodEnum method,
                                    String uri,
                                    AIDevReq reqBody,
                                    BkApiAuthorization authorization,
                                    TypeReference<R> typeReference,
                                    Boolean idempotent) {
        try {
            HttpMetricUtil.setHttpMetricName(CommonMetricNames.BK_AI_DEV_API_HTTP);
            HttpMetricUtil.addTagForCurrentMetric(Tag.of("api_name", uri));
            OpenApiRequestInfo<Object> requestInfo = OpenApiRequestInfo
                .builder()
                .method(method)
                .uri(uri)
                .body(reqBody)
                .authorization(authorization)
                .setIdempotent(idempotent)
                .build();
            return requestApiAndWrapResponse(requestInfo, typeReference,
                HttpHelperFactory.getLongRetryableHttpHelper());
        } catch (Exception e) {
            throw new BkAIDevException(e, ErrorCode.BK_AI_DEV_API_DATA_ERROR, null);
        } finally {
            HttpMetricUtil.clearHttpMetric();
        }
    }

}
