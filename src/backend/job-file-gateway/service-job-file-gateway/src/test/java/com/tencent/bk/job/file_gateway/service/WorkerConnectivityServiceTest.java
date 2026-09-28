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

import com.tencent.bk.job.file_gateway.model.req.inner.ConnectivityCheckReq;
import com.tencent.bk.job.file_gateway.model.resp.inner.ConnectivityCheckResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WorkerConnectivityService 仅做 DNS 解析的回探测试")
class WorkerConnectivityServiceTest {

    private static final String WORKER_HOST = "bk-job-file-worker-0.bk-job-file-worker.bk-job";

    private static ConnectivityCheckReq req(String host) {
        return new ConnectivityCheckReq("default", host, 19810);
    }

    @Test
    @DisplayName("地址可解析时回探成功")
    void successWhenResolvable() throws UnknownHostException {
        WorkerConnectivityService service = new WorkerConnectivityService();
        InetAddress loopback = InetAddress.getByName("127.0.0.1");
        service.setHostResolver(host -> new InetAddress[]{loopback});

        ConnectivityCheckResult result = service.check(req(WORKER_HOST));

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("地址不可解析时回探失败并返回原因")
    void failWhenUnresolvable() {
        WorkerConnectivityService service = new WorkerConnectivityService();
        service.setHostResolver(host -> {
            throw new UnknownHostException(host);
        });

        ConnectivityCheckResult result = service.check(req(WORKER_HOST));

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("UnknownHostException").contains(WORKER_HOST);
    }

    @Test
    @DisplayName("host 夹带 path/userinfo 等非法字符时直接失败，不做解析")
    void rejectInvalidHostWithoutResolving() {
        WorkerConnectivityService service = new WorkerConnectivityService();
        AtomicInteger resolveCount = new AtomicInteger();
        service.setHostResolver(host -> {
            resolveCount.incrementAndGet();
            return new InetAddress[0];
        });

        assertThat(service.check(req("127.0.0.1/latest/meta-data")).getSuccess()).isFalse();
        assertThat(service.check(req("worker@127.0.0.1")).getSuccess()).isFalse();
        assertThat(service.check(req("")).getSuccess()).isFalse();
        assertThat(resolveCount.get()).isZero();
    }
}
