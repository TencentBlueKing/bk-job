/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.file.worker.cos.service;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.DefaultHttpHelper;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import com.tencent.bk.job.file.worker.task.heartbeat.HeartBeatTask;
import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import com.tencent.bk.job.file_gateway.model.req.inner.OffLineAndReDispatchReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class OpService {

    private final AbstractHttpHelper httpHelper = new DefaultHttpHelper();
    private final WorkerConfig workerConfig;
    private final FileTaskService fileTaskService;
    private final GatewayInfoService gatewayInfoService;
    private final TaskReporter taskReporter;

    @Autowired
    public OpService(WorkerConfig workerConfig, FileTaskService fileTaskService,
                     GatewayInfoService gatewayInfoService, TaskReporter taskReporter) {
        this.workerConfig = workerConfig;
        this.fileTaskService = fileTaskService;
        this.gatewayInfoService = gatewayInfoService;
        this.taskReporter = taskReporter;
    }

    public List<String> offLine() {
        List<String> runningTaskIdList = fileTaskService.getAllTaskIdList();
        // 停止心跳
        HeartBeatTask.stopHeartBeat();
        // 调网关接口下线自己
        String url = gatewayInfoService.getWorkerOffLineUrl();
        OffLineAndReDispatchReq offLineReq = new OffLineAndReDispatchReq();
        offLineReq.setWorkerId(workerConfig.getId());
        offLineReq.setAppId(workerConfig.getAppId());
        offLineReq.setToken(workerConfig.getToken());
        offLineReq.setTaskIdList(runningTaskIdList);
        offLineReq.setInitDelayMills(3000L);
        offLineReq.setIntervalMills(3000L);
        HttpReq req = HttpReqGenUtil.genSimpleJsonReq(url, offLineReq);
        String respStr = null;
        try {
            log.info(String.format("url=%s,body=%s,headers=%s", url, req.getBody(),
                JsonUtils.toJson(req.getHeaders())));
            respStr = httpHelper.post(url, req.getBody(), req.getHeaders());
            log.info(String.format("respStr=%s", respStr));
            // 停止任务
            Integer allStoppedFileCount = fileTaskService.stopTasksAtOnce(runningTaskIdList,
                new ThreadCommandBus.Command(TaskCommandEnum.STOP_QUIETLY, null));
            log.info("{} file tasks stopped", allStoppedFileCount);
            taskReporter.reportWorkerOffLine(runningTaskIdList, "FileWorker offline");
        } catch (Exception e) {
            log.error("Fail to request file-gateway:", e);
        }
        return runningTaskIdList;
    }

    public List<String> taskList() {
        List<String> runningTaskIdList = fileTaskService.getAllTaskIdList();
        return runningTaskIdList;
    }
}
