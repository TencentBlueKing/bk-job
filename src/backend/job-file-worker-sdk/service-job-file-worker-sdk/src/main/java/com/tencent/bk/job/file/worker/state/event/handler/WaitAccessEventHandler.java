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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.file.worker.config.AccessReadyProperties;
import com.tencent.bk.job.file.worker.state.WorkerStateEnum;
import com.tencent.bk.job.file.worker.state.WorkerStateMachine;
import com.tencent.bk.job.file.worker.state.event.WorkerEvent;
import com.tencent.bk.job.file.worker.state.event.WorkerEventService;
import com.tencent.bk.job.file.worker.task.connectivity.ConnectivityCheckTask;
import com.tencent.bk.job.file.worker.task.health.SelfHealthCheckTask;
import com.tencent.bk.job.file_gateway.model.resp.inner.ConnectivityCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * 等待Worker可被外界访问的事件处理器，实现检查与等待逻辑。
 * <p>
 * 每轮需同时满足：Worker 进程内自检健康状态为 UP，且 File-Gateway 能在其 Pod 内解析 Worker 访问地址。
 * 仅当连续 N 轮都满足，才认为 Worker 可被 Gateway 集群访问并上报心跳
 * （规避 K8s 各 Pod 间 DNS 缓存时间差导致的瞬时不可达）。
 */
@Slf4j
@Component
public class WaitAccessEventHandler implements EventHandler {

    private final WorkerEventService workerEventService;
    private final WorkerStateMachine workerStateMachine;
    private final ConnectivityCheckTask connectivityCheckTask;
    private final SelfHealthCheckTask selfHealthCheckTask;
    private final AccessReadyProperties accessReadyProperties;

    @Autowired
    public WaitAccessEventHandler(@Lazy WorkerEventService workerEventService,
                                  WorkerStateMachine workerStateMachine,
                                  ConnectivityCheckTask connectivityCheckTask,
                                  SelfHealthCheckTask selfHealthCheckTask,
                                  AccessReadyProperties accessReadyProperties) {
        this.workerEventService = workerEventService;
        this.workerStateMachine = workerStateMachine;
        this.connectivityCheckTask = connectivityCheckTask;
        this.selfHealthCheckTask = selfHealthCheckTask;
        this.accessReadyProperties = accessReadyProperties;
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

    /**
     * 每轮同时检查 Worker 自身健康与 Gateway 地址解析，两者都成功才计一次成功：
     * <ul>
     *     <li>连续成功 {@code requiredSuccessCount} 次 → 视为达成，立即返回 true；</li>
     *     <li>任意一项失败 → 连续成功计数清零，等待 {@code checkIntervalMs} 后重试；</li>
     *     <li>成功但未达连续阈值 → 立即发起下一轮检查，不等待；</li>
     *     <li>累计尝试次数达到 {@code maxCheckCount} → 返回 false，由事件循环重新入队。</li>
     * </ul>
     */
    private boolean checkAccess() {
        int requiredSuccessCount = accessReadyProperties.getRequiredSuccessCount();
        long checkIntervalMs = accessReadyProperties.getCheckIntervalMs();
        int maxCheckCount = accessReadyProperties.getMaxCheckCount();
        int consecutiveSuccess = 0;
        int totalAttempts = 0;
        while (totalAttempts < maxCheckCount) {
            totalAttempts++;
            String failReason = checkOnce();
            if (failReason == null) {
                consecutiveSuccess++;
                log.info(
                    "AccessCheck success, consecutive={}/{}, totalAttempts={}/{}",
                    consecutiveSuccess, requiredSuccessCount, totalAttempts, maxCheckCount
                );
                if (consecutiveSuccess >= requiredSuccessCount) {
                    return true;
                }
                // 成功立即发起下一轮检查，不 sleep，避免节流过严拖慢启动
            } else {
                String logMessage = I18nUtil.getI18nMessage(
                    String.valueOf(ErrorCode.FILE_WORKER_CONNECTIVITY_CHECK_FAIL),
                    new Object[]{failReason}
                );
                log.info(
                    "{}, consecutive reset to 0 (was {}), totalAttempts={}/{}",
                    logMessage, consecutiveSuccess, totalAttempts, maxCheckCount
                );
                consecutiveSuccess = 0;
                if (totalAttempts < maxCheckCount) {
                    ThreadUtils.sleep(checkIntervalMs);
                }
            }
        }
        log.warn(
            "AccessCheck reached maxCheckCount={} without {} consecutive successes, will re-enqueue",
            maxCheckCount, requiredSuccessCount
        );
        return false;
    }

    /**
     * @return 两项检查都成功时返回 null，否则返回失败原因
     */
    private String checkOnce() {
        String selfUnhealthyReason = selfHealthCheckTask.checkUnhealthyReason();
        if (selfUnhealthyReason != null) {
            return selfUnhealthyReason;
        }
        ConnectivityCheckResult result = connectivityCheckTask.doCheck();
        if (result != null && Boolean.TRUE.equals(result.getSuccess())) {
            return null;
        }
        return "gateway: " + (result == null ? "null result" : result.getErrorMessage());
    }
}
