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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务实例柔性上下线管理接口。
 * <p>
 * 提供对 Readiness 探针状态的编程控制，用于 K8S 环境下的柔性上下线：
 * <ul>
 *   <li>PUT /manage/service/availability/offline — 下线：停止接受新流量，Pod 继续运行</li>
 *   <li>PUT /manage/service/availability/online  — 上线：重新开始接受流量</li>
 *   <li>GET /manage/service/availability         — 查询当前在线状态</li>
 * </ul>
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/service/manage/availability")
public class ServiceInstanceAvailabilityController {

    private final SwitchableReadinessHealthIndicator switchableReadinessHealthIndicator;

    public ServiceInstanceAvailabilityController(
        SwitchableReadinessHealthIndicator switchableReadinessHealthIndicator) {
        this.switchableReadinessHealthIndicator = switchableReadinessHealthIndicator;
    }

    /**
     * 查询服务实例当前在线状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailability() {
        Map<String, Object> result = new HashMap<>();
        result.put("online", switchableReadinessHealthIndicator.isOnline());
        return ResponseEntity.ok(result);
    }

    /**
     * 将服务实例上线（恢复接受流量）
     */
    @PutMapping("/online")
    public ResponseEntity<Map<String, Object>> online() {
        log.info("Received request to go ONLINE");
        switchableReadinessHealthIndicator.goOnline();
        Map<String, Object> result = new HashMap<>();
        result.put("online", true);
        result.put("message", "Service instance is now ONLINE");
        return ResponseEntity.ok(result);
    }

    /**
     * 将服务实例下线（停止接受流量，Pod 保持运行）
     */
    @PutMapping("/offline")
    public ResponseEntity<Map<String, Object>> offline() {
        log.info("Received request to go OFFLINE");
        switchableReadinessHealthIndicator.goOffline();
        Map<String, Object> result = new HashMap<>();
        result.put("online", false);
        result.put("message", "Service instance is now OFFLINE, no longer accepting traffic");
        return ResponseEntity.ok(result);
    }
}
