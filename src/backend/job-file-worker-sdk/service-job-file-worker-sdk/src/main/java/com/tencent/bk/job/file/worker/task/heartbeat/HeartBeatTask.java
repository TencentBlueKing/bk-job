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
import com.tencent.bk.job.common.util.http.AbstractHttpHelper;
import com.tencent.bk.job.common.util.http.DefaultHttpHelper;
import com.tencent.bk.job.common.util.http.HttpReqGenUtil;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.util.machine.MachineUtil;
import com.tencent.bk.job.file.worker.config.WorkerConfig;
import com.tencent.bk.job.file.worker.cos.service.GatewayInfoService;
import com.tencent.bk.job.file.worker.cos.service.MetaDataService;
import com.tencent.bk.job.file_gateway.model.req.inner.HeartBeatReq;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;

@Slf4j
@Service
public class HeartBeatTask {

    public static volatile boolean runFlag = true;

    private final AbstractHttpHelper httpHelper = new DefaultHttpHelper();

    private final WorkerConfig workerConfig;
    private final GatewayInfoService gatewayInfoService;
    private final MetaDataService metaDataService;

    @Autowired
    public HeartBeatTask(WorkerConfig workerConfig, GatewayInfoService gatewayInfoService,
                         MetaDataService metaDataService) {
        this.workerConfig = workerConfig;
        this.gatewayInfoService = gatewayInfoService;
        this.metaDataService = metaDataService;
    }

    public static void stopHeartBeat() {
        runFlag = false;
    }

    private HeartBeatReq getWorkerInfo() {
        String innerIp = IpUtils.getFirstMachineIP();
        HeartBeatReq heartBeatReq = new HeartBeatReq();
        heartBeatReq.setId(workerConfig.getId());
        heartBeatReq.setAppId(workerConfig.getAppId());
        heartBeatReq.setToken(workerConfig.getToken());
        if (StringUtils.isBlank(workerConfig.getAccessHost())) {
            heartBeatReq.setAccessHost(innerIp);
        } else {
            heartBeatReq.setAccessHost(workerConfig.getAccessHost());
        }
        heartBeatReq.setAccessPort(workerConfig.getAccessPort());
        heartBeatReq.setCloudAreaId(workerConfig.getCloudAreaId());
        if (StringUtils.isBlank(workerConfig.getInnerIp())) {
            heartBeatReq.setInnerIp(innerIp);
        } else {
            heartBeatReq.setInnerIp(workerConfig.getInnerIp());
        }
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
        String respStr;
        try {
            log.info(String.format("url=%s,body=%s,headers=%s", url, req.getBody(),
                JsonUtils.toJson(req.getHeaders())));
            respStr = httpHelper.post(url, req.getBody(), req.getHeaders());
            log.info(String.format("respStr=%s", respStr));
        } catch (Exception e) {
            log.error("Fail to request file-gateway:", e);
        }
    }
}
