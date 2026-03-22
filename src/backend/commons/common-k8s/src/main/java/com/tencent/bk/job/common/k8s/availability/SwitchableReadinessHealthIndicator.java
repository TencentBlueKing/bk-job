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

package com.tencent.bk.job.common.k8s.availability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可切换的 Readiness 状态管理器。
 * <p>
 * 通过发布 {@link AvailabilityChangeEvent} 直接驱动 Spring Boot 的 Readiness 状态变更，
 * 使 K8S 的 {@code /actuator/health/readiness} 探针能够感知上下线操作：
 * <ul>
 *   <li>下线：发布 {@link ReadinessState#REFUSING_TRAFFIC}，K8S 停止向该 Pod 调度流量</li>
 *   <li>上线：发布 {@link ReadinessState#ACCEPTING_TRAFFIC}，K8S 重新向该 Pod 调度流量</li>
 * </ul>
 * Pod 本身不会被删除或重启，仅流量调度受影响。
 * </p>
 */
@Slf4j
public class SwitchableReadinessHealthIndicator {

    /**
     * 服务实例是否在线（true=在线/接受流量，false=下线/不接受流量）
     */
    private final AtomicBoolean online = new AtomicBoolean(true);

    private final ApplicationEventPublisher eventPublisher;

    public SwitchableReadinessHealthIndicator(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * 将服务实例上线（恢复接受流量）
     */
    public void goOnline() {
        boolean prev = online.getAndSet(true);
        if (!prev) {
            log.info("Service instance is going ONLINE, publishing ReadinessState.ACCEPTING_TRAFFIC.");
        }
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
    }

    /**
     * 将服务实例下线（停止接受流量，但 Pod 保持运行）
     */
    public void goOffline() {
        boolean prev = online.getAndSet(false);
        if (prev) {
            log.info("Service instance is going OFFLINE, publishing ReadinessState.REFUSING_TRAFFIC.");
        }
        AvailabilityChangeEvent.publish(eventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
    }

    /**
     * 当前是否在线
     */
    public boolean isOnline() {
        return online.get();
    }
}
