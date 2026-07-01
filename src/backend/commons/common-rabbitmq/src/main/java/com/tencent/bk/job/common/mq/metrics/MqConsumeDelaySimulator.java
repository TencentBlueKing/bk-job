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

import lombok.extern.slf4j.Slf4j;

/**
 * MQ消息消费延迟模拟器
 * 在消息处理前 Sleep 配置的时长，用于在测试环境模拟消息延迟，复现依赖不稳定时序的问题。
 * 通过 {@link MqConsumeDelaySimulateProperties} 控制是否开启与延迟时长，默认关闭。
 */
@Slf4j
public class MqConsumeDelaySimulator {

    private final MqConsumeDelaySimulateProperties properties;

    public MqConsumeDelaySimulator(MqConsumeDelaySimulateProperties properties) {
        this.properties = properties;
    }

    /**
     * 按配置模拟消息消费延迟，仅在开启且延迟时长大于0时生效。
     *
     * @param binding 当前消费的binding名称，用于日志排查
     */
    public void simulateDelay(String binding) {
        if (!properties.isEnabled()) {
            return;
        }
        long delayMs = properties.getDelayMs();
        if (delayMs <= 0) {
            return;
        }
        try {
            log.info("Simulate mq consume delay, binding: {}, delayMs: {}", binding, delayMs);
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            // 恢复中断标志，便于上层感知中断
            Thread.currentThread().interrupt();
            log.warn("Interrupted while simulating mq consume delay, binding: {}, delayMs: {}", binding, delayMs, e);
        }
    }
}
