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

package com.tencent.bk.job.file.worker.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * File-Worker 启动期 access-ready 判定相关配置。
 * 配置前缀: job.file-worker.access-ready
 * <p>
 * 这些参数控制 Worker 通过反复请求 File-Gateway 的连通性回探接口、判定自己
 * 是否真正可被 Gateway 集群访问的策略。
 */
@Data
@ConfigurationProperties(prefix = "job.file-worker.access-ready")
public class AccessReadyProperties {

    /**
     * 连续回探成功阈值。
     * 由于 Gateway 端是集群部署，单次成功不能保证全部 Gateway Pod 都能访问到 Worker，
     * 仅当连续 N 次回探均成功时，才认为 Worker 已经真正达到 access-ready 状态。
     * 默认值 5。
     */
    private Integer requiredSuccessCount = 5;

    /**
     * 回探失败后等待的间隔时间，单位毫秒。
     * 注意：仅在本次回探失败后才会等待该时长再发起下一次回探；
     * 回探成功后会立即发起下一次回探，不会等待，避免节流过严拖慢启动。
     * 默认值 1000ms。
     */
    private Long checkIntervalMs = 1000L;

    /**
     * 总回探尝试上限（包含成功+失败次数）。
     * 达到该上限仍未满足连续成功阈值时，本轮 access-ready 判定结束并失败重入队，
     * 由事件循环再次触发新一轮判定。
     * 默认值 300。
     */
    private Integer maxCheckCount = 300;
}
