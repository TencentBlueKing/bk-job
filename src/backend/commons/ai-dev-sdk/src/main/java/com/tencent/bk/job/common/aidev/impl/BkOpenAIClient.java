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

package com.tencent.bk.job.common.aidev.impl;

import com.tencent.bk.job.common.aidev.IBkOpenAIClient;
import com.tencent.bk.job.common.aidev.config.CustomPaasLoginProperties;
import com.tencent.bk.job.common.aidev.exception.BkOpenAIException;
import com.tencent.bk.job.common.aidev.model.common.AIDevMessage;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.util.json.JsonUtils;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressWarnings("SameParameterValue")
public class BkOpenAIClient implements IBkOpenAIClient {

    private static final String URI_LLM_V1 = "/appspace/gateway/llm/v1/";

    private final AppProperties appProperties;
    private final BkApiGatewayProperties.ApiGwConfig bkAIDevConfig;
    private final CustomPaasLoginProperties customPaasLoginProperties;
    private final String bkAIDevUrl;

    public BkOpenAIClient(MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          CustomPaasLoginProperties customPaasLoginProperties,
                          BkApiGatewayProperties bkApiGatewayProperties) {
        bkAIDevUrl = getBkAIDevUrlSafely(bkApiGatewayProperties);
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

    private String getLLMV1Url() {
        return bkAIDevUrl + URI_LLM_V1;
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

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<String> getHunYuanAnswerStream(String token,
                                                            List<AIDevMessage> messageHistoryList,
                                                            String userInput,
                                                            Consumer<String> partialRespConsumer) {
        final OpenAiClient client = OpenAiClient.builder()
            .baseUrl(getLLMV1Url())
            .openAiApiKey("empty")
            .customHeaders(singletonMap("X-Bkapi-Authorization", JsonUtils.toJson(buildAuthorization(token))))
            .logRequests()
            .logResponses()
            .build();
        OpenAiClient.OpenAiClientContext context = new OpenAiClient.OpenAiClientContext();
        ChatCompletionRequest request = buildRequest(messageHistoryList, userInput);
        ChatCompletionRequest streamRequest = ChatCompletionRequest.builder().from(request).stream(true).build();
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder responseBuilder = new StringBuilder();
        Consumer<ChatCompletionResponse> partialResponseHandler = response -> {
            List<ChatCompletionChoice> choices = response.choices();
            if (choices == null || choices.isEmpty()) {
                return;
            }
            String s = choices.get(0).delta().content();
            if (s != null) {
                responseBuilder.append(s);
                // TODO:限制最大消息内容
                if (partialRespConsumer != null) {
                    partialRespConsumer.accept(s);
                }
            }
        };
        client.chatCompletion(context, streamRequest)
            .onPartialResponse(partialResponseHandler)
            .onComplete(() -> future.complete(responseBuilder.toString()))
            .onError(future::completeExceptionally)
            .execute();
        return future;
    }

    private ChatCompletionRequest buildRequest(List<AIDevMessage> messageHistoryList, String userInput) {
        ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
            .model(BkChatCompletionModel.HUNYUAN.toString());
        if (CollectionUtils.isNotEmpty(messageHistoryList)) {
            for (AIDevMessage message : messageHistoryList) {
                if (message.isUserMessage()) {
                    builder.addUserMessage(message.getContent());
                } else if (message.isAssistantMessage()) {
                    builder.addAssistantMessage(message.getContent());
                }
            }
        }
        builder.addUserMessage(userInput);
        return builder.build();
    }
}
