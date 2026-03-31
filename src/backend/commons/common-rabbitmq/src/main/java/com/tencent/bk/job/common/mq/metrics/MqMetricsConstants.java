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

/**
 * MQ监控指标常量
 */
public class MqMetricsConstants {
    /**
     * MQ消费延迟指标名
     */
    public static final String NAME_JOB_MQ_CONSUME_DELAY = "job.mq.consume.delay";
    /**
     * MQ延迟消费次数
     */
    public static final String NAME_JOB_MQ_CONSUME_DELAY_COUNT = "job.mq.consume.delay.count";
    /**
     * MQ消息发送时间头
     */
    public static final String HEADER_NAME_SEND_TIME_MS = "job_send_time_ms";
    /**
     * MQ消费线程活跃数
     */
    public static final String NAME_JOB_MQ_CONSUMER_ACTIVE_COUNT = "job.mq.consumer.active.count";
    /**
     * MQ消费线程配置数
     */
    public static final String NAME_JOB_MQ_CONSUMER_CONFIGURED_COUNT = "job.mq.consumer.configured.count";
    /**
     * MQ消费线程最大数
     */
    public static final String NAME_JOB_MQ_CONSUMER_MAX_COUNT = "job.mq.consumer.max.count";

    /**
     * group标签
     */
    public static final String TAG_KEY_GROUP = "group";
    /**
     * binding标签
     */
    public static final String TAG_KEY_BINDING = "binding";
    /**
     * 消息名称标签
     */
    public static final String TAG_KEY_MESSAGE_NAME = "message_name";
    /**
     * MQ出站通道匹配模式
     */
    public static final String PATTERN_OUTBOUND_CHANNEL = "*-out-*";
}
