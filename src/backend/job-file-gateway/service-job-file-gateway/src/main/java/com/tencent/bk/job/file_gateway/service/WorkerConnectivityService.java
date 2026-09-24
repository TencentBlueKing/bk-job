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

import com.tencent.bk.job.common.util.http.HttpUrlSafetyUtils;
import com.tencent.bk.job.file_gateway.model.req.inner.ConnectivityCheckReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.ConnectivityCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Worker 连通性回探服务
 * 由 File-Worker 在启动期反复调用，File-Gateway 在本 Pod 内解析请求中携带的 accessHost，
 * 借此证明该 Gateway Pod 已能解析到新 Worker 的地址。
 * 通过 Worker 侧对 Gateway 集群做"连续 N 次成功"判定，可规避 K8s 各 Pod 间 DNS 缓存
 * 时间差导致的瞬时不可达问题。
 * <p>
 * Gateway 只做 DNS 解析，不向请求方指定的地址发起任何连接；Worker 自身健康状态由 Worker 自行判断。
 * 解析走 {@link InetAddress#getAllByName}，与后续 Gateway 调用 Worker 时共用 JVM DNS 缓存。
 */
@Slf4j
@Service
public class WorkerConnectivityService {

    /**
     * 当无法解析出本机 Pod 标识时的兜底值
     */
    private static final String UNKNOWN_POD_HOSTNAME = "unknown";

    /**
     * 当前 Gateway Pod 标识，仅用于日志聚合验证 ClusterIP Service 的 L4 负载均衡是否将
     * Worker 的多次连通性回探请求分散到不同 Pod 上。启动时一次性解析后缓存，避免每次回探
     * 都触发环境变量/DNS 查询。
     */
    private final String podHostname;

    private HostResolver hostResolver = InetAddress::getAllByName;

    public WorkerConnectivityService() {
        this.podHostname = resolvePodHostname();
        log.info("WorkerConnectivityService initialized, podHostname={}", podHostname);
    }

    /**
     * 在当前 Gateway Pod 内解析 Worker 的访问地址。
     *
     * @param req 回探请求，携带 Worker 的访问地址
     * @return 回探结果（成功/失败 + 失败描述）
     */
    public ConnectivityCheckResult check(ConnectivityCheckReq req) {
        // 记录当前处理本次回探请求的 Gateway Pod 标识，便于跨 Pod 日志聚合人工验证
        // Worker 侧连续 N 次成功探测是否真的被 ClusterIP Service 分散到了多个 Gateway Pod
        log.info(
            "Handle connectivity check: handledByGatewayPod={}, " +
                "fromWorker(clusterName={}, accessHost={}, accessPort={})",
            podHostname,
            req.getClusterName(),
            req.getAccessHost(),
            req.getAccessPort()
        );
        String accessHost = req.getAccessHost();
        if (!HttpUrlSafetyUtils.isAllowedServiceHost(accessHost)) {
            return fail(req, "invalid worker access host");
        }
        try {
            InetAddress[] addresses = hostResolver.resolve(accessHost.trim());
            if (addresses == null || addresses.length == 0) {
                return fail(req, "UnknownHostException: no address resolved for " + accessHost);
            }
            return new ConnectivityCheckResult(true, null);
        } catch (UnknownHostException e) {
            return fail(req, "UnknownHostException: " + e.getMessage());
        } catch (Exception e) {
            return fail(req, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private ConnectivityCheckResult fail(ConnectivityCheckReq req, String errorMessage) {
        log.info(
            "Gateway connectivity check fail, cluster={}, accessHost={}, errorMessage={}",
            req.getClusterName(),
            req.getAccessHost(),
            errorMessage
        );
        return new ConnectivityCheckResult(false, errorMessage);
    }

    void setHostResolver(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
    }

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }

    /**
     * 解析当前 Gateway Pod 标识，按优先级依次回退：
     * <ol>
     *     <li>{@code BK_JOB_POD_NAME}：BK-JOB Helm Chart 标准注入（fieldRef: metadata.name），最稳定准确</li>
     *     <li>{@code HOSTNAME}：K8s 默认会以 Pod 名作为容器 hostname 注入，可作为通用兜底</li>
     *     <li>{@link InetAddress#getLocalHost()}：JVM 反查本机 hostname，用于二进制部署等兜底场景</li>
     * </ol>
     * 仅在 Bean 初始化时调用一次，性能开销可忽略。
     */
    private String resolvePodHostname() {
        String bkJobPodName = System.getenv("BK_JOB_POD_NAME");
        if (StringUtils.isNotBlank(bkJobPodName)) {
            return bkJobPodName;
        }
        String hostnameEnv = System.getenv("HOSTNAME");
        if (StringUtils.isNotBlank(hostnameEnv)) {
            return hostnameEnv;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Fail to resolve local hostname, fallback to {}", UNKNOWN_POD_HOSTNAME, e);
            return UNKNOWN_POD_HOSTNAME;
        }
    }
}
