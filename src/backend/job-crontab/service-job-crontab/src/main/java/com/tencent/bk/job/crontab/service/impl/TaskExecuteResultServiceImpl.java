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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.client.ServiceTaskExecuteResultResourceClient;
import com.tencent.bk.job.crontab.service.TaskExecuteResultService;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.job.execute.model.inner.request.ServiceGetCronTaskExecuteStatisticsRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @since 2/3/2020 22:24
 */
@Slf4j
@Service
public class TaskExecuteResultServiceImpl implements TaskExecuteResultService {

    private ServiceTaskExecuteResultResourceClient serviceTaskExecuteResultResourceClient;

    @Autowired
    public TaskExecuteResultServiceImpl(
        ServiceTaskExecuteResultResourceClient serviceTaskExecuteResultResourceClient
    ) {
        this.serviceTaskExecuteResultResourceClient = serviceTaskExecuteResultResourceClient;
    }

    @Override
    public Map<Long, ServiceCronTaskExecuteResultStatistics> getCronTaskExecuteResultStatistics(
        long appId,
        List<Long> cronIdList
    ) {
        if (appId < 0 || CollectionUtils.isEmpty(cronIdList)) {
            return null;
        }
        try {
            ServiceGetCronTaskExecuteStatisticsRequest getCronTaskExecuteStatisticsRequest =
                new ServiceGetCronTaskExecuteStatisticsRequest();
            getCronTaskExecuteStatisticsRequest.setAppId(appId);
            getCronTaskExecuteStatisticsRequest.setCronTaskIdList(cronIdList);

            if (log.isDebugEnabled()) {
                log.debug("Get cron execute result|{}|{}", appId, getCronTaskExecuteStatisticsRequest);
            }

            InternalResponse<Map<Long, ServiceCronTaskExecuteResultStatistics>> cronTaskExecuteResultStatResp =
                serviceTaskExecuteResultResourceClient.getCronTaskExecuteResultStatistics(
                    getCronTaskExecuteStatisticsRequest
                );

            if (log.isDebugEnabled()) {
                log.debug("Get cron execute result response|{}|{}|{}", appId, getCronTaskExecuteStatisticsRequest,
                    JsonUtils.toJson(cronTaskExecuteResultStatResp));
            }

            if (cronTaskExecuteResultStatResp != null
                && cronTaskExecuteResultStatResp.getData() != null) {
                return cronTaskExecuteResultStatResp.getData();
            } else {
                log.error("Error while query cron execute history!|{}", cronTaskExecuteResultStatResp);
                return null;
            }
        } catch (Exception e) {
            log.error("Get cron execute result failed!|{}|{}", appId, cronIdList, e);
            return null;
        }
    }
}
