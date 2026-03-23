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

import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;

@Slf4j
public class JobApplicationAvailabilityBean extends ApplicationAvailabilityBean {
    @Override
    public void onApplicationEvent(AvailabilityChangeEvent<?> event) {
        super.onApplicationEvent(event);
        if (ReadinessState.REFUSING_TRAFFIC == event.getState()) {
            // 柔性上下线触发的 REFUSING_TRAFFIC 不需要等待，
            // 仅在 Pod 真正关闭（优雅停机）时才等待调用方缓存刷新
            if (event.getSource() instanceof SwitchableReadinessHealthIndicator) {
                log.info("Soft offline triggered by SwitchableReadinessHealthIndicator, skip GracefulShutdown wait.");
                return;
            }
            // SpringCloud负载均衡缓存设置为20s，等待调用方缓存刷新后再真正关闭Spring容器
            int waitSeconds = 40;
            while (waitSeconds > 0) {
                ThreadUtils.sleep(1000);
                log.info("wait for GracefulShutdown, {}s left", waitSeconds--);
            }
        }
    }
}
