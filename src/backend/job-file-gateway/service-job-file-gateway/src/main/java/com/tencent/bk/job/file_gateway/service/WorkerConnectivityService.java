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

package com.tencent.bk.job.file_gateway.service;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.file_gateway.model.req.inner.ConnectivityCheckReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.ConnectivityCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Worker 连通性回探服务
 * 由 File-Worker 在启动期反复调用，File-Gateway 根据请求中携带的 accessHost/accessPort
 * 主动访问 Worker 的 /actuator/health 端点，借此真实证明该 Gateway Pod 能访问到 Worker。
 * 通过 Worker 侧对 Gateway 集群做"连续 N 次成功"判定，可规避 K8s 各 Pod 间 DNS 缓存
 * 时间差导致的瞬时不可达问题。
 */
@Slf4j
@Service
public class WorkerConnectivityService {

    /**
     * Worker 健康检查端点路径
     */
    private static final String WORKER_HEALTH_PATH = "/actuator/health";

    private final JobHttpClient jobHttpClient;

    @Autowired
    public WorkerConnectivityService(JobHttpClient jobHttpClient) {
        this.jobHttpClient = jobHttpClient;
    }

    /**
     * 由 Gateway 主动回探 Worker 的健康检查端点。
     *
     * @param req 回探请求，携带 Worker 的访问地址
     * @return 回探结果（成功/失败 + 失败描述）
     */
    public ConnectivityCheckResult check(ConnectivityCheckReq req) {
        String url = buildHealthUrl(req.getAccessHost(), req.getAccessPort());
        try {
            HttpReq httpReq = HttpReqGenUtil.genUrlGetReq(url);
            jobHttpClient.get(httpReq);
            return new ConnectivityCheckResult(true, null);
        } catch (Exception e) {
            // 捕获 UnknownHostException / IOException / RestClientException 等所有异常，
            // 只把简短错误信息回传给 Worker，避免日志被压垮。
            String errorMessage = buildErrorMessage(e);
            log.info(
                "Gateway connectivity check fail, cluster={}, url={}, errorMessage={}",
                req.getClusterName(),
                url,
                errorMessage
            );
            return new ConnectivityCheckResult(false, errorMessage);
        }
    }

    @SuppressWarnings("HttpUrlsUsage")
    private String buildHealthUrl(String accessHost, Integer accessPort) {
        return "http://" + accessHost + ":" + accessPort + WORKER_HEALTH_PATH;
    }

    /**
     * 构造对 Worker 友好的简短错误信息（包含异常类型+原始 message），避免泄漏堆栈。
     */
    private String buildErrorMessage(Throwable t) {
        Throwable cause = t;
        // 取最内层 cause，避免被 Spring 异常包装多层后丢失真实原因（如 UnknownHostException）
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
