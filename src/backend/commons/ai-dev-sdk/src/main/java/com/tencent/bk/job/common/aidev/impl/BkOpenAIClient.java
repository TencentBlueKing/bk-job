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

import com.tencent.bk.job.common.aidev.IBkOpenAIClient;
import com.tencent.bk.job.common.aidev.config.CustomPaasLoginProperties;
import com.tencent.bk.job.common.aidev.metrics.MetricsConstants;
import com.tencent.bk.job.common.aidev.model.common.AIDevMessage;
import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.esb.model.BkApiAuthorization;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.async.TraceRunnable;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.singletonMap;

@Slf4j
@SuppressWarnings("SameParameterValue")
public class BkOpenAIClient implements IBkOpenAIClient {

    private static final String URI_LLM_V1 = "/appspace/gateway/llm/v1/";
    // 存储于内存中的最大消息长度：5MB
    private static final int IN_MEMORY_MAX_MESSAGE_SIZE = 5 * 1024 * 1024;
    // 最大日志长度：500KB
    private static final int MAX_LOG_LENGTH = 500 * 1024;

    private final Tracer tracer;
    private final SpanNamer spanNamer;
    private final MeterRegistry meterRegistry;
    private final AppProperties appProperties;
    private final BkApiGatewayProperties.ApiGwConfig bkAIDevConfig;
    private final CustomPaasLoginProperties customPaasLoginProperties;
    private final String bkAIDevUrl;

    public BkOpenAIClient(Tracer tracer,
                          SpanNamer spanNamer,
                          MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          CustomPaasLoginProperties customPaasLoginProperties,
                          BkApiGatewayProperties bkApiGatewayProperties) {
        this.tracer = tracer;
        this.spanNamer = spanNamer;
        this.meterRegistry = meterRegistry;
        this.appProperties = appProperties;
        this.bkAIDevUrl = getBkAIDevUrlSafely(bkApiGatewayProperties);
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
        String username = JobContextUtil.getUsername();
        if (log.isDebugEnabled()) {
            String requestStr = request.toString();
            log.debug(
                "username={}, request={}, length={}",
                username,
                getLimitedLog(requestStr),
                requestStr.length()
            );
        }
        ChatCompletionRequest streamRequest = ChatCompletionRequest.builder().from(request).stream(true).build();
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder responseBuilder = new StringBuilder();
        Consumer<ChatCompletionResponse> tracedPartialResponseHandler = getTracedConsumer(
            getPartialResponseHandler(
                username,
                partialRespConsumer,
                responseBuilder
            )
        );
        long startTime = System.currentTimeMillis();
        Runnable streamingCompletionCallback = () -> {
            recordAIRespAllBlockDelay(
                System.currentTimeMillis() - startTime,
                Tags.of(Tag.of(MetricsConstants.TAG_KEY_STATUS, MetricsConstants.TAG_VALUE_STATUS_SUCCEED))
            );
            String respStr = responseBuilder.toString();
            if (log.isDebugEnabled()) {
                log.debug(
                    "username={}, response={}, length={}",
                    username,
                    getLimitedLog(respStr),
                    respStr.length()
                );
            }
            future.complete(respStr);
        };
        Runnable tracedStreamingCompletionCallback = new TraceRunnable(tracer, spanNamer, streamingCompletionCallback);
        Consumer<Throwable> errorHandler = throwable -> {
            recordAIRespAllBlockDelay(
                System.currentTimeMillis() - startTime,
                Tags.of(Tag.of(MetricsConstants.TAG_KEY_STATUS, MetricsConstants.TAG_VALUE_STATUS_ERROR))
            );
            String message = MessageFormatter.format(
                "Fail to get stream response, username={}",
                username
            ).getMessage();
            log.warn(message, throwable);
            future.completeExceptionally(throwable);
        };
        Consumer<Throwable> tracedErrorHandler = getTracedConsumer(errorHandler);
        client.chatCompletion(context, streamRequest)
            .onPartialResponse(tracedPartialResponseHandler)
            .onComplete(tracedStreamingCompletionCallback)
            .onError(tracedErrorHandler)
            .execute();
        return future;
    }

    private <T> Consumer<T> getTracedConsumer(Consumer<T> originConsumer) {
        Span parentSpan = tracer.currentSpan();
        return response -> {
            Span childSpan = tracer.nextSpan(parentSpan).start();
            try (Tracer.SpanInScope ignored = tracer.withSpan(childSpan)) {
                originConsumer.accept(response);
            } finally {
                childSpan.end();
            }
        };
    }

    @NotNull
    private Consumer<ChatCompletionResponse> getPartialResponseHandler(String username,
                                                                       Consumer<String> partialRespConsumer,
                                                                       StringBuilder responseBuilder) {
        final AtomicInteger bytesReceived = new AtomicInteger(0);
        final AtomicBoolean firstBlockRecorded = new AtomicBoolean(false);
        long startTime = System.currentTimeMillis();
        return response -> {
            List<ChatCompletionChoice> choices = response.choices();
            if (choices == null || choices.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("username={}, choices is null or empty", username);
                }
                return;
            }
            String content = choices.get(0).delta().content();
            if (content != null) {
                if (!firstBlockRecorded.get()) {
                    recordAIRespFirstBlockDelay(System.currentTimeMillis() - startTime);
                    firstBlockRecorded.set(true);
                }
                if (bytesReceived.get() > IN_MEMORY_MAX_MESSAGE_SIZE) {
                    log.warn(
                        "username={}, ignore content: {}, bytesReceived={}, exceed max message size {}",
                        username,
                        content,
                        bytesReceived.get(),
                        IN_MEMORY_MAX_MESSAGE_SIZE
                    );
                    return;
                }
                responseBuilder.append(content);
                bytesReceived.addAndGet(content.getBytes(StandardCharsets.UTF_8).length);
                if (partialRespConsumer != null) {
                    partialRespConsumer.accept(content);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("username={}, receive content: {}", username, content);
            }
        };
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
                } else if (message.isSystemMessage()) {
                    builder.addSystemMessage(message.getContent());
                }
            }
        }
        builder.addUserMessage(userInput);
        return builder.build();
    }

    private String getLimitedLog(String rawLog) {
        return StringUtil.substring(rawLog, MAX_LOG_LENGTH);
    }

    private void recordAIRespFirstBlockDelay(long delayMillis) {
        Timer.builder(MetricsConstants.NAME_AI_RESPONSE_DELAY_FIRST_BLOCK)
            .description("AI Response First Block Delay")
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(500))
            .maximumExpectedValue(Duration.ofSeconds(30L))
            .register(meterRegistry)
            .record(delayMillis, TimeUnit.MILLISECONDS);
    }

    private void recordAIRespAllBlockDelay(long delayMillis, Iterable<Tag> tags) {
        Timer.builder(MetricsConstants.NAME_AI_RESPONSE_DELAY_ALL_BLOCK)
            .description("AI Response All Block Delay")
            .tags(tags)
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofSeconds(1))
            .maximumExpectedValue(Duration.ofSeconds(180L))
            .register(meterRegistry)
            .record(delayMillis, TimeUnit.MILLISECONDS);
    }
}
