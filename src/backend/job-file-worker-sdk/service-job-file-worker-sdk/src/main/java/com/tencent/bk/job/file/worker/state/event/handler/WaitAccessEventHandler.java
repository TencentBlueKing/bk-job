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

package com.tencent.bk.job.file.worker.state.event.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import com.tencent.bk.job.file.worker.service.EnvironmentService;
import com.tencent.bk.job.file.worker.state.WorkerStateEnum;
import com.tencent.bk.job.file.worker.state.WorkerStateMachine;
import com.tencent.bk.job.file.worker.state.event.WorkerEvent;
import com.tencent.bk.job.file.worker.state.event.WorkerEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 等待Worker可被外界访问的事件处理器，实现检查与等待逻辑
 */
@Slf4j
@Component
public class WaitAccessEventHandler implements EventHandler {

    private final WorkerEventService workerEventService;
    private final WorkerStateMachine workerStateMachine;
    private final JobHttpClient jobHttpClient;
    private final String checkAccessUrl;

    @Autowired
    public WaitAccessEventHandler(@Lazy WorkerEventService workerEventService,
                                  WorkerStateMachine workerStateMachine,
                                  JobHttpClient jobHttpClient,
                                  WorkerConfig workerConfig,
                                  EnvironmentService environmentService) {
        this.workerEventService = workerEventService;
        this.workerStateMachine = workerStateMachine;
        this.jobHttpClient = jobHttpClient;
        this.checkAccessUrl = buildCheckAccessUrl(environmentService.getAccessHost(), workerConfig.getAccessPort());
    }

    @SuppressWarnings("HttpUrlsUsage")
    private String buildCheckAccessUrl(String accessHost, Integer accessPort) {
        return "http://" + accessHost + ":" + accessPort + "/actuator/health";
    }

    @Override
    public void handleEvent(WorkerEvent event) {
        WorkerStateEnum workerState = workerStateMachine.getWorkerState();
        switch (workerState) {
            case STARTING:
            case WAIT_ACCESS_READY:
                workerStateMachine.waitAccessReady();
                waitAccessReady();
                break;
            default:
                log.info("currentState:{}, waitAccessReady condition not satisfy, ignore", workerState);
                break;
        }
    }

    public void waitAccessReady() {
        boolean accessReady = checkAccess();
        if (accessReady) {
            // 1.状态切换
            workerStateMachine.accessReady();
            // 2.自身可被外界访问后立即触发心跳
            workerEventService.commitWorkerEvent(WorkerEvent.heartBeat());
        } else {
            // 3.检查失败，状态不变，继续检查
            workerEventService.commitWorkerEvent(WorkerEvent.waitAccessReady());
        }
    }

    private boolean checkAccess() {
        boolean accessReady = false;
        int maxCheckNum = 300;
        int checkNum = 0;
        int errorNum = 0;
        do {
            try {
                checkNum += 1;
                log.info("CheckAccess: url={}", checkAccessUrl);
                HttpReq req = HttpReqGenUtil.genUrlGetReq(checkAccessUrl);
                String respStr = jobHttpClient.get(req);
                HealthResult healthResult = JsonUtils.fromJson(respStr, new TypeReference<HealthResult>() {
                });
                String status = healthResult.getStatus();
                if (status != null && status.equalsIgnoreCase("UP")) {
                    accessReady = true;
                }
            } catch (Throwable t) {
                errorNum += 1;
                if (errorNum % 10 == 0) {
                    log.info("Fail to checkAccess", t);
                }
            }
            if (!accessReady && checkNum < maxCheckNum) {
                log.info("Access not ready, checkNum={}, wait 1s", checkNum);
                ThreadUtils.sleep(1000);
            }
        } while (!accessReady && checkNum < maxCheckNum);
        return accessReady;
    }
}
