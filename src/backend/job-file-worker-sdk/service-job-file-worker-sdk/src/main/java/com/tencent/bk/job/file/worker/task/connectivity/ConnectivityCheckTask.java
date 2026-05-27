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

package com.tencent.bk.job.file.worker.task.connectivity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.config.ClusterProperties;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import com.tencent.bk.job.file.worker.service.EnvironmentService;
import com.tencent.bk.job.file.worker.service.GatewayInfoService;
import com.tencent.bk.job.file.worker.service.JwtTokenService;
import com.tencent.bk.job.file_gateway.model.req.inner.ConnectivityCheckReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.ConnectivityCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 连通性回探任务。
 * 由 Worker 调用 File-Gateway 的 /remote/fileWorker/connectivityCheck 接口，
 * 让 Gateway 主动回探当前 Worker 的健康端点，从而真实判定 Worker 是否可被 Gateway 集群访问。
 * 与 {@link com.tencent.bk.job.file.worker.task.heartbeat.HeartBeatTask} 平级。
 */
@Slf4j
@Service
public class ConnectivityCheckTask {

    private final JobHttpClient jobHttpClient;
    private final ClusterProperties clusterProperties;
    private final WorkerConfig workerConfig;
    private final GatewayInfoService gatewayInfoService;
    private final EnvironmentService environmentService;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public ConnectivityCheckTask(JobHttpClient jobHttpClient,
                                 ClusterProperties clusterProperties,
                                 WorkerConfig workerConfig,
                                 GatewayInfoService gatewayInfoService,
                                 EnvironmentService environmentService,
                                 JwtTokenService jwtTokenService) {
        this.jobHttpClient = jobHttpClient;
        this.clusterProperties = clusterProperties;
        this.workerConfig = workerConfig;
        this.gatewayInfoService = gatewayInfoService;
        this.environmentService = environmentService;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * 构造连通性回探请求体，使用与心跳一致的 accessHost/accessPort，
     * 确保 Gateway 回探的目标与后续心跳所注册的访问地址完全一致。
     */
    private ConnectivityCheckReq buildReq() {
        ConnectivityCheckReq req = new ConnectivityCheckReq();
        req.setClusterName(clusterProperties.getName());
        // 二进制部署环境与K8s环境差异处理，与心跳逻辑保持一致
        req.setAccessHost(environmentService.getAccessHost());
        req.setAccessPort(workerConfig.getAccessPort());
        return req;
    }

    /**
     * 发起一次连通性回探请求，并返回 Gateway 侧的回探结果。
     * 网络异常或 Gateway 不可达时，封装为 success=false 的结果返回，避免抛出异常打断重试循环。
     */
    public ConnectivityCheckResult doCheck() {
        String url = gatewayInfoService.getConnectivityCheckUrl();
        ConnectivityCheckReq body = buildReq();
        log.debug("ConnectivityCheck: url={},body={}", url, JsonUtils.toJson(body));
        try {
            HttpReq req = HttpReqGenUtil.genSimpleJsonReq(
                url,
                jwtTokenService.getJwtTokenHeaders(),
                body
            );
            String respStr = jobHttpClient.post(req);
            Response<ConnectivityCheckResult> resp = JsonUtils.fromJson(
                respStr,
                new TypeReference<Response<ConnectivityCheckResult>>() {
                }
            );
            if (resp == null || resp.getData() == null) {
                return new ConnectivityCheckResult(false, "empty response from gateway");
            }
            return resp.getData();
        } catch (Exception e) {
            // Gateway 自身不可达（启动期、滚动升级等场景），将异常转为失败结果，由调用方继续重试
            String errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.info("Fail to request gateway connectivity check, url={}, errorMessage={}", url, errorMessage);
            return new ConnectivityCheckResult(false, errorMessage);
        }
    }
}
