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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.service.HostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

/**
 * 机器Agent状态同步逻辑
 */
@Slf4j
@Service
public class AgentStatusSyncService {

    private final ApplicationDAO applicationDAO;
    private final ApplicationHostDAO applicationHostDAO;
    private final HostService hostService;

    @Autowired
    public AgentStatusSyncService(ApplicationDAO applicationDAO,
                                  ApplicationHostDAO applicationHostDAO,
                                  HostService hostService) {
        this.applicationDAO = applicationDAO;
        this.applicationHostDAO = applicationHostDAO;
        this.hostService = hostService;
    }

    private Pair<Long, Long> syncHostAgentStatus() {
        long gseInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        StopWatch hostAgentStatusWatch = new StopWatch();
        hostAgentStatusWatch.start("listAllHostSimpleInfo");
        List<HostSimpleDTO> localHosts = applicationHostDAO.listAllHostSimpleInfo();
        hostAgentStatusWatch.stop();
        hostAgentStatusWatch.start("getAgentStatusByAppInfo from GSE");
        long startTime = System.currentTimeMillis();
        hostService.fillHostStatus(localHosts);
        gseInterfaceTimeConsuming += (System.currentTimeMillis() - startTime);
        hostAgentStatusWatch.stop();
        hostAgentStatusWatch.start("updateHostsStatus to local DB");
        startTime = System.currentTimeMillis();
        int succUpdateHosts = hostService.updateHostsStatus(localHosts);
        writeToDBTimeConsuming += (System.currentTimeMillis() - startTime);
        hostAgentStatusWatch.stop();
        if (hostAgentStatusWatch.getTotalTimeMillis() > 180000) {
            log.info("syncHostAgentStatus too slow, run statistics,totalHosts:{};succHosts:{};timeConsume:{}",
                localHosts.size(), succUpdateHosts, hostAgentStatusWatch.prettyPrint());
        }
        log.debug("syncHostAgentStatus Performance,totalHosts:{};succHosts:{};timeConsume:{}", localHosts.size(),
            succUpdateHosts, hostAgentStatusWatch);
        return Pair.of(gseInterfaceTimeConsuming, writeToDBTimeConsuming);
    }

    public void syncAgentStatusFromGSE() {
        log.info(Thread.currentThread().getName() + ":begin to sync agentStatus from GSE");
        long gseInterfaceTimeConsuming = 0L;
        long writeToDBTimeConsuming = 0L;
        try {
            Pair<Long, Long> timeConsumingPair = syncHostAgentStatus();
            gseInterfaceTimeConsuming += timeConsumingPair.getFirst();
            writeToDBTimeConsuming += timeConsumingPair.getSecond();
            log.info(Thread.currentThread().getName() + ":Finished:sync agentStatus from GSE," +
                    "gseInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms,rate={}",
                gseInterfaceTimeConsuming, writeToDBTimeConsuming,
                gseInterfaceTimeConsuming / (0. + writeToDBTimeConsuming));
        } catch (Throwable t) {
            log.error("syncAgentStatus from GSE fail：", t);
        }
    }

}
