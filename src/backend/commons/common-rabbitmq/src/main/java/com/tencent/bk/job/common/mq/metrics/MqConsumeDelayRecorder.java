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

package com.tencent.bk.job.common.mq.metrics;

import com.tencent.bk.job.common.util.date.DateUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MQ延迟消费记录器
 */
@Slf4j
public abstract class MqConsumeDelayRecorder {
    /**
     * 时钟偏差阈值(ms)
     */
    private static final long CLOCK_SKEW_THRESHOLD_MS = -1000L;
    /**
     * 出现时钟偏差记录日志的时间间隔(ms)
     */
    private static final long CLOCK_SKEW_LOG_INTERVAL_MS = 60_000L;
    /**
     * 上次时钟偏差打印警告日志时间
     */
    private final AtomicLong lastClockSkewWarnLogTimeMs = new AtomicLong(0L);
    private static final String TIME_FORMATTER = "yyyy-MM-dd HH:mm:ss.SSS";
    protected final MeterRegistry meterRegistry;
    protected final MqMetricsProperties mqMetricsProperties;

    protected MqConsumeDelayRecorder(MeterRegistry meterRegistry,
                                     MqMetricsProperties mqMetricsProperties
    ) {
        this.meterRegistry = meterRegistry;
        this.mqMetricsProperties = mqMetricsProperties;
    }

    /**
     * 记录MQ消费延迟
     * 仅当消息延迟超过阈值时记录指标与日志
     *
     * @param binding binding名称
     * @param messageName 消息名称
     * @param message 消息体
     */
    public void recordConsumeDelay(String binding, String messageName, Message<?> message) {
        try {
            doRecordConsumeDelay(binding, messageName, message);
        } catch (Exception e) {
            log.error("Record mq consume delay failed, binding: {}, messageName: {}", binding, messageName, e);
        }
    }

    private void doRecordConsumeDelay(String binding, String messageName, Message<?> message) {
        if (!mqMetricsProperties.isEnabled()) {
            log.debug("Ignore mq consume delay record because metrics are disabled, binding: {}", binding);
            return;
        }
        Long sendTimeMs = parseSendTimestamp(message.getHeaders().get(MqMetricsConstants.HEADER_NAME_SEND_TIME_MS));
        if (sendTimeMs == null) {
            log.debug("Ignore mq consume delay record because send time header is missing, binding: {}", binding);
            return;
        }

        long consumeTimeMs = System.currentTimeMillis();
        long rawDelayMs  = consumeTimeMs - sendTimeMs;
        if (rawDelayMs <= CLOCK_SKEW_THRESHOLD_MS) {
            logClockSkewIfNecessary(binding, messageName, sendTimeMs, consumeTimeMs, rawDelayMs);
        }
        long delayMs = Math.max(rawDelayMs, 0L);
        long thresholdMs = getDelayThresholdMs();
        if (delayMs < thresholdMs) {
            log.debug(
                "Ignore mq consume delay record because delay is below threshold, binding: {}, " +
                    "messageName: {}, delayMs: {}, thresholdMs: {}",
                binding,
                messageName,
                delayMs,
                thresholdMs
            );
            return;
        }
        Tags tags = Tags.of(
            Tag.of(MqMetricsConstants.TAG_KEY_BINDING, binding),
            Tag.of(MqMetricsConstants.TAG_KEY_MESSAGE_NAME, messageName)
        );
        recordConsumeDelayMetrics(delayMs, tags);
        log.warn(
            "Delayed mq message detected, binding: {}, messageName: {}, sendTime: {}, consumeTime: {}, " +
                "delayMs: {}, thresholdMs: {}",
            binding,
            messageName,
            DateUtils.getDateStrFromUnixTimeMills(sendTimeMs, TIME_FORMATTER),
            DateUtils.getDateStrFromUnixTimeMills(consumeTimeMs, TIME_FORMATTER),
            delayMs,
            thresholdMs
        );
    }

    /**
     * 当消费端时间比发送端时间早1s以上时，说明机器间可能存在较大时钟偏差
     * 间歇打印warn日志
     */
    private void logClockSkewIfNecessary (
        String binding,
        String messageName,
        long sendTimeMs,
        long consumeTimeMs,
        long rawDelayMs
    ) {
        long now = System.currentTimeMillis();
        long lastWarnTimeMs = lastClockSkewWarnLogTimeMs.get();
        if (now - lastWarnTimeMs < CLOCK_SKEW_LOG_INTERVAL_MS) {
            return;
        }
        if (lastClockSkewWarnLogTimeMs.compareAndSet(lastWarnTimeMs, now)) {
            log.warn(
                "Large clock skew, binding: {}, messageName: {}, sendTime: {}, consumeTime: {}, rawDelayMs: {}",
                binding,
                messageName,
                DateUtils.getDateStrFromUnixTimeMills(sendTimeMs, TIME_FORMATTER),
                DateUtils.getDateStrFromUnixTimeMills(consumeTimeMs, TIME_FORMATTER),
                rawDelayMs
            );
        }
    }

    /**
     * 获取延迟消费阈值
     *
     * @return 延迟消费阈值，单位毫秒
     */
    protected long getDelayThresholdMs() {
        MqMetricsProperties.ConsumerMetric consumeMetric = getConsumeMetric();
        if (consumeMetric != null && consumeMetric.getDelayMs() > 0) {
            return consumeMetric.getDelayMs();
        }
        return getDefaultDelayThresholdMs();
    }

    /**
     * 获取当前服务消费配置
     *
     * @return 消费配置，未配置返回null
     */
    protected abstract MqMetricsProperties.ConsumerMetric getConsumeMetric();

    /**
     * 记录延迟消费相关指标
     *
     * @param delayMs 延迟时间
     * @param tags 指标tags
     */
    protected void recordConsumeDelayMetrics(long delayMs, Tags tags) {
        Timer.builder(MqMetricsConstants.NAME_JOB_MQ_CONSUME_DELAY)
            .tags(tags)
            .register(meterRegistry)
            .record(delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 解析消息发送时间头
     *
     * @param sendTimestampObj 原始发送时间头
     * @return 发送时间，解析失败返回null
     */
    protected Long parseSendTimestamp(Object sendTimestampObj) {
        if (sendTimestampObj instanceof Number) {
            return ((Number) sendTimestampObj).longValue();
        }
        if (sendTimestampObj instanceof String) {
            try {
                return Long.parseLong((String) sendTimestampObj);
            } catch (NumberFormatException e) {
                log.warn("Invalid mq send time header: {}", sendTimestampObj);
            }
        }
        return null;
    }

    /**
     * 获取默认延迟消费阈值
     *
     * @return 默认延迟消费阈值，单位毫秒
     */
    protected long getDefaultDelayThresholdMs() {
        return mqMetricsProperties.getConsumer().getDefaultDelayMs();
    }
}
