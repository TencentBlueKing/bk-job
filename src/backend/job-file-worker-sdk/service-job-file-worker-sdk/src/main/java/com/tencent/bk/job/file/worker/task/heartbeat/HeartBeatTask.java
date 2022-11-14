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

package com.tencent.bk.job.file.worker.task.heartbeat;

import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.machine.MachineUtil;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import com.tencent.bk.job.file.worker.cos.service.EnvironmentService;
import com.tencent.bk.job.file.worker.cos.service.GatewayInfoService;
import com.tencent.bk.job.file.worker.cos.service.MetaDataService;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;

@Slf4j
@Service
public class HeartBeatTask {

    public static volatile boolean runFlag = true;

    private final JobHttpClient jobHttpClient;
    private final WorkerConfig workerConfig;
    private final GatewayInfoService gatewayInfoService;
    private final MetaDataService metaDataService;
    private final EnvironmentService environmentService;

    @Autowired
    public HeartBeatTask(JobHttpClient jobHttpClient,
                         WorkerConfig workerConfig,
                         GatewayInfoService gatewayInfoService,
                         MetaDataService metaDataService,
                         EnvironmentService environmentService) {
        this.jobHttpClient = jobHttpClient;
        this.workerConfig = workerConfig;
        this.gatewayInfoService = gatewayInfoService;
        this.metaDataService = metaDataService;
        this.environmentService = environmentService;
    }

    public static void stopHeartBeat() {
        runFlag = false;
    }

    private HeartBeatReq getWorkerInfo() {
        HeartBeatReq heartBeatReq = new HeartBeatReq();
        heartBeatReq.setName(workerConfig.getName());
        heartBeatReq.setTagList(workerConfig.getTagList());
        heartBeatReq.setAppId(workerConfig.getAppId());
        heartBeatReq.setToken(workerConfig.getToken());

        // 二进制部署环境与K8s环境差异处理
        heartBeatReq.setAccessHost(environmentService.getAccessHost());
        heartBeatReq.setInnerIp(environmentService.getInnerIp());

        heartBeatReq.setAccessPort(workerConfig.getAccessPort());
        heartBeatReq.setCloudAreaId(workerConfig.getCloudAreaId());
        heartBeatReq.setAbilityTagList(workerConfig.getAbilityTagList());
        heartBeatReq.setVersion(workerConfig.getVersion());
        heartBeatReq.setCpuOverload(MachineUtil.systemCPULoad());
        heartBeatReq.setMemRate(MachineUtil.memoryLoad());
        heartBeatReq.setMemFreeSpace((float) MachineUtil.freePhysicalMemorySize());
        try {
            heartBeatReq.setDiskRate(MachineUtil.getDiskLoad(workerConfig.getWorkspaceDirPath()));
            heartBeatReq.setDiskFreeSpace((float) MachineUtil.getDiskUsableSpace(workerConfig.getWorkspaceDirPath()));
        } catch (FileNotFoundException e) {
            log.warn("Fail to get disk info", e);
        }
        heartBeatReq.setOnlineStatus((byte) 1);
        heartBeatReq.setFileWorkerConfig(metaDataService.getFileWorkerConfig());
        return heartBeatReq;
    }

    public void run() {
        if (!runFlag) {
            log.info("HeartBeat closed, ignore");
            return;
        }
        String url = gatewayInfoService.getHeartBeatUrl();
        HttpReq req = HttpReqGenUtil.genSimpleJsonReq(url, getWorkerInfo());
        jobHttpClient.post(req);
    }
}
