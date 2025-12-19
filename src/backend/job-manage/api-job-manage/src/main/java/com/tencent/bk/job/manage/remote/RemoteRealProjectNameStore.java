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

package com.tencent.bk.job.manage.remote;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.manage.api.inner.ServiceRealProjectNameResource;
import com.tencent.bk.job.manage.model.inner.request.ServiceSaveRealProjectNameReq;
import com.tentent.bk.job.common.api.artifactory.IRealProjectNameStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * 通过远程调用实现真实项目名称存储接口
 */
@Slf4j
public class RemoteRealProjectNameStore implements IRealProjectNameStore {

    private final ServiceRealProjectNameResource realProjectNameResource;
    private final DiscoveryClient discoveryClient;

    public RemoteRealProjectNameStore(ServiceRealProjectNameResource realProjectNameResource,
                                      DiscoveryClient discoveryClient) {
        this.realProjectNameResource = realProjectNameResource;
        this.discoveryClient = discoveryClient;
    }

    @Override
    public boolean waitUntilStoreServiceReady(Integer maxWaitSeconds) {
        boolean ready;
        long startTimeMills = System.currentTimeMillis();
        int sleepMills = 1000;
        long totalWaitTimeMills;
        do {
            ready = checkJobManageServiceReady();
            if (ready) {
                totalWaitTimeMills = System.currentTimeMillis() - startTimeMills;
                long thresholdMillsToLog = 1000L;
                if (totalWaitTimeMills > thresholdMillsToLog) {
                    log.info(
                        "job-manage service is ready, totalWaitTime={}s",
                        (System.currentTimeMillis() - startTimeMills) / 1000.0
                    );
                }
                return true;
            }
            log.info("job-manage service is not ready, wait {}ms", sleepMills);
            ThreadUtils.sleep(sleepMills);
            // 等待超时判断
            totalWaitTimeMills = System.currentTimeMillis() - startTimeMills;
            if (maxWaitSeconds != null && totalWaitTimeMills >= maxWaitSeconds * 1000L) {
                log.warn("job-manage service is not ready after maxWaitSeconds: {}s", maxWaitSeconds);
                return false;
            }
        } while (true);
    }

    /**
     * 检查job-manage服务是否准备就绪
     *
     * @return 布尔值
     */
    private boolean checkJobManageServiceReady() {
        try {
            List<ServiceInstance> jobManageInstanceList = discoveryClient.getInstances(
                JobConstants.SERVICE_NAME_JOB_MANAGE
            );
            return CollectionUtils.isNotEmpty(jobManageInstanceList);
        } catch (Throwable t) {
            log.warn("Fail to get job-manage service instance list", t);
            return false;
        }
    }

    @Override
    public void saveRealProjectName(String saveKey, String realProjectName) {
        realProjectNameResource.saveRealProjectName(
            new ServiceSaveRealProjectNameReq(
                saveKey,
                realProjectName
            )
        );
    }

    @Override
    public String queryRealProjectName(String saveKey) {
        return realProjectNameResource.queryRealProjectName(saveKey).getData();
    }
}
