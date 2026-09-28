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

package com.tencent.bk.job.file_gateway.model.req.inner;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Worker连通性回探请求
 * File-Worker 在启动阶段调用此请求，要求 File-Gateway 在本 Pod 内解析 Worker 的访问地址，
 * 以此判定 Gateway 集群是否已能解析到该 Worker。Gateway 不会向该地址发起连接。
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ConnectivityCheckReq {

    /**
     * Worker 所在集群名称
     */
    @Schema(description = "Worker 所在集群名称", required = true)
    private String clusterName;

    /**
     * Gateway 用于访问 Worker 的 host
     */
    @Schema(description = "Gateway 用于访问 Worker 的 host", required = true)
    private String accessHost;

    /**
     * Gateway 用于访问 Worker 的 port
     */
    @Schema(description = "Gateway 用于访问 Worker 的 port", required = true)
    private Integer accessPort;
}
