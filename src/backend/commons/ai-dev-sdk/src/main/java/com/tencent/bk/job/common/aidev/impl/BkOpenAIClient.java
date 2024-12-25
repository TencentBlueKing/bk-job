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
import dev.ai4j.openai4j.chat.ChatCompletionModel;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final String model;

    public BkOpenAIClient(Tracer tracer,
                          SpanNamer spanNamer,
                          MeterRegistry meterRegistry,
                          AppProperties appProperties,
                          CustomPaasLoginProperties customPaasLoginProperties,
                          BkApiGatewayProperties bkApiGatewayProperties,
                          String model) {
        this.tracer = tracer;
        this.spanNamer = spanNamer;
        this.meterRegistry = meterRegistry;
        this.appProperties = appProperties;
        this.bkAIDevUrl = getBkAIDevUrlSafely(bkApiGatewayProperties);
        this.bkAIDevConfig = bkApiGatewayProperties.getBkAIDev();
        this.customPaasLoginProperties = customPaasLoginProperties;
        this.model = getValidModel(model);
        log.info("BkOpenAIClient inited using model {}", this.model);
    }

    /**
     * 获取有效的模型类型
     *
     * @param model 待校验的模型类型
     * @return 有效的模型类型
     * @throws IllegalArgumentException 传入的模型不被支持时抛出
     */
    private String getValidModel(String model) {
        if (StringUtils.isBlank(model)) {
            throw new IllegalArgumentException("model cannot be blank");
        }
        model = model.trim();
        Set<String> availableModels = new HashSet<>();
        for (BkChatCompletionModel bkChatCompletionModel : BkChatCompletionModel.values()) {
            String availableModel = bkChatCompletionModel.toString();
            if (availableModel.equals(model)) {
                return model;
            }
            availableModels.add(availableModel);
        }
        for (ChatCompletionModel chatCompletionModel : ChatCompletionModel.values()) {
            String availableModel = chatCompletionModel.toString();
            if (availableModel.equals(model)) {
                return model;
            }
            availableModels.add(availableModel);
        }
        String message = MessageFormatter.format(
            "invalid model: {}, available models: {}",
            model,
            StringUtil.concatCollection(availableModels)
        ).getMessage();
        throw new IllegalArgumentException(message);
    }

    /**
     * 获取AI平台接口根地址
     *
     * @param bkApiGatewayProperties 蓝鲸API网关配置
     * @return AI平台接口根地址
     */
    private static String getBkAIDevUrlSafely(BkApiGatewayProperties bkApiGatewayProperties) {
        if (bkApiGatewayProperties == null || bkApiGatewayProperties.getBkAIDev() == null) {
            return null;
        }
        return bkApiGatewayProperties.getBkAIDev().getUrl();
    }

    private String getLLMV1Url() {
        return bkAIDevUrl + URI_LLM_V1;
    }


    /**
     * 构造蓝鲸网关认证信息
     *
     * @param token 用户身份token
     * @return 认证信息
     */
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
     * 流式获取AI回复数据
     *
     * @param token               用户身份凭据
     * @param messageHistoryList  历史消息列表
     * @param userInput           用户输入
     * @param partialRespConsumer 分块消息处理器
     * @return 包含AI完整回复内容的Future
     */
    @Override
    public CompletableFuture<String> getAIAnswerStream(String token,
                                                       List<AIDevMessage> messageHistoryList,
                                                       String userInput,
                                                       Consumer<String> partialRespConsumer) {
        final OpenAiClient client = buildOpenAiClient(token);
        OpenAiClient.OpenAiClientContext context = new OpenAiClient.OpenAiClientContext();
        // 构造AI大模型接口请求
        ChatCompletionRequest request = buildRequest(messageHistoryList, userInput);
        String username = JobContextUtil.getUsername();
        logRequest(username, request);
        ChatCompletionRequest streamRequest = ChatCompletionRequest.builder().from(request).stream(true).build();
        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder responseBuilder = new StringBuilder();
        Consumer<ChatCompletionResponse> tracedPartialResponseHandler = getTracedConsumer(
            getPartialResponseHandler(username, partialRespConsumer, responseBuilder)
        );
        long startTime = System.currentTimeMillis();
        // 构造流式响应回调处理器
        Runnable streamingCompletionCallback = () -> {
            recordAIRespAllBlockDelay(
                System.currentTimeMillis() - startTime,
                Tags.of(Tag.of(MetricsConstants.TAG_KEY_STATUS, MetricsConstants.TAG_VALUE_STATUS_SUCCEED))
            );
            String respStr = responseBuilder.toString();
            logRespStr(username, respStr);
            future.complete(respStr);
        };
        Runnable tracedStreamingCompletionCallback = new TraceRunnable(tracer, spanNamer, streamingCompletionCallback);
        // 构造错误处理器
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
        // 调用AI大模型接口
        client.chatCompletion(context, streamRequest)
            .onPartialResponse(tracedPartialResponseHandler)
            .onComplete(tracedStreamingCompletionCallback)
            .onError(tracedErrorHandler)
            .execute();
        return future;
    }

    /**
     * 根据凭证信息构造OpenAiClient
     *
     * @param token 用户身份凭证
     * @return OpenAiClient
     */
    @SuppressWarnings("unchecked")
    private OpenAiClient buildOpenAiClient(String token) {
        return OpenAiClient.builder()
            .baseUrl(getLLMV1Url())
            .openAiApiKey("empty")
            .customHeaders(singletonMap("X-Bkapi-Authorization", JsonUtils.toJson(buildAuthorization(token))))
            .logRequests()
            .logResponses()
            .build();
    }

    /**
     * 按需打印请求内容
     *
     * @param username 用户名
     * @param request  请求
     */
    private void logRequest(String username, ChatCompletionRequest request) {
        if (log.isDebugEnabled()) {
            String requestStr = request.toString();
            log.debug(
                "username={}, request={}, length={}",
                username,
                getLimitedLog(requestStr),
                requestStr.length()
            );
        }
    }

    /**
     * 打印响应内容
     *
     * @param username 用户名
     * @param respStr  响应内容字符串
     */
    private void logRespStr(String username, String respStr) {
        if (log.isDebugEnabled()) {
            log.debug(
                "username={}, response={}, length={}",
                username,
                getLimitedLog(respStr),
                respStr.length()
            );
        }
    }

    /**
     * 对原始Consumer进行包装，传递Trace数据
     *
     * @param originConsumer 原始Consumer
     * @param <T>            Consumer消费的数据类型
     * @return 包装后的Consumer
     */
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

    /**
     * 根据传入的上层消费者生成底层框架需要的部分响应处理器
     *
     * @param username            用户名
     * @param partialRespConsumer 上层部分响应消费者
     * @param responseBuilder     请求构造器
     * @return 底层框架需要的部分响应处理器
     */
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

    /**
     * 构建AI对话请求
     *
     * @param messageHistoryList 历史对话记录
     * @param userInput          用户输入
     * @return AI对话请求
     */
    private ChatCompletionRequest buildRequest(List<AIDevMessage> messageHistoryList,
                                               String userInput) {
        ChatCompletionRequest.Builder builder = ChatCompletionRequest.builder()
            .model(model);
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

    /**
     * 对日志进行超长截断
     *
     * @param rawLog 原始日志
     * @return 截断后的日志
     */
    private String getLimitedLog(String rawLog) {
        return StringUtil.substring(rawLog, MAX_LOG_LENGTH);
    }

    /**
     * 记录AI首次响应延迟指标
     *
     * @param delayMillis 延迟的毫秒数
     */
    private void recordAIRespFirstBlockDelay(long delayMillis) {
        Timer.builder(MetricsConstants.NAME_AI_RESPONSE_DELAY_FIRST_BLOCK)
            .description("AI Response First Block Delay")
            .publishPercentileHistogram(true)
            .minimumExpectedValue(Duration.ofMillis(500))
            .maximumExpectedValue(Duration.ofSeconds(30L))
            .register(meterRegistry)
            .record(delayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录AI完成响应延迟指标
     *
     * @param delayMillis 延迟的毫秒数
     * @param tags        指标数据标签
     */
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
