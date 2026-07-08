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

package com.tencent.bk.job.common.mq.consume;

import com.tencent.bk.job.common.mq.metrics.MqConsumeDelayRecorder;
import com.tencent.bk.job.common.mq.metrics.MqConsumeDelaySimulator;
import org.springframework.messaging.Message;

/**
 * MQ消息消费监听器顶层基类（模板方法模式）。
 * <p>
 * 通过模板方法 {@link #onEvent(Message)} 统一编排各服务消费监听器的公共流程：
 * <ol>
 *     <li>消费入口最前端模拟消息延迟（默认关闭，用于复现依赖不稳定时序的问题）；</li>
 *     <li>记录消息消费延迟指标；</li>
 *     <li>调用子类业务钩子 {@link #handleEvent(Message)} 执行各自的业务逻辑。</li>
 * </ol>
 * 子类只需实现业务钩子与 {@link #getBindingName()}，无需重复编写「模拟延迟 + 记录指标」样板代码。
 * 模拟延迟与指标记录分别由 {@link MqConsumeDelaySimulator} 与 {@link MqConsumeDelayRecorder}
 * 独立承担各自职责，基类仅负责调用编排。
 *
 * @param <T> 消息体类型
 */
public abstract class AbstractMqConsumeListener<T> {

    /**
     * MQ消息消费延迟模拟器，用于在测试环境模拟消息延迟
     */
    private final MqConsumeDelaySimulator mqConsumeDelaySimulator;
    /**
     * MQ延迟消费指标记录器
     */
    private final MqConsumeDelayRecorder mqConsumeDelayRecorder;

    protected AbstractMqConsumeListener(MqConsumeDelaySimulator mqConsumeDelaySimulator,
                                        MqConsumeDelayRecorder mqConsumeDelayRecorder) {
        this.mqConsumeDelaySimulator = mqConsumeDelaySimulator;
        this.mqConsumeDelayRecorder = mqConsumeDelayRecorder;
    }

    /**
     * 模板方法：编排MQ消息消费的公共流程。
     * 先在消费入口最前端模拟延迟，再记录延迟指标，最后执行子类业务逻辑。
     *
     * @param message 消息
     */
    public final void onEvent(Message<? extends T> message) {
        String binding = getBindingName();
        // 1.消费入口最前端模拟消息延迟，早于任何消费与记录逻辑，默认关闭
        mqConsumeDelaySimulator.simulateDelay(binding);
        // 2.记录消息消费延迟指标
        mqConsumeDelayRecorder.recordConsumeDelay(
            binding,
            message.getPayload().getClass().getSimpleName(),
            message
        );
        try {
            // 3.业务前置钩子 + 子类业务逻辑
            beforeHandle(message);
            handleEvent(message);
        } finally {
            afterHandle(message);
        }
    }

    /**
     * 业务处理前置钩子，默认空实现，子类可按需覆盖（如设置线程上下文）。
     *
     * @param message 消息
     */
    protected void beforeHandle(Message<? extends T> message) {
        // 默认空实现
    }

    /**
     * 业务处理后置钩子，默认空实现，子类可按需覆盖（如清理线程上下文）。
     *
     * @param message 消息
     */
    protected void afterHandle(Message<? extends T> message) {
        // 默认空实现
    }

    /**
     * 获取当前listener对应的binding名称，用于指标tag与日志排查。
     *
     * @return binding名称
     */
    protected abstract String getBindingName();

    /**
     * 子类实现的具体业务处理逻辑。
     *
     * @param message 消息
     */
    protected abstract void handleEvent(Message<? extends T> message);
}
