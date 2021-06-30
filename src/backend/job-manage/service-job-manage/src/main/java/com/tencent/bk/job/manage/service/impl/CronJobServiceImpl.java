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

package com.tencent.bk.job.manage.service.impl;

import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.manage.client.ServiceCronJobResourceClient;
import com.tencent.bk.job.manage.service.CronJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @since 22/2/2020 21:25
 */
@Slf4j
@Service
public class CronJobServiceImpl implements CronJobService {

    private ServiceCronJobResourceClient serviceCronJobResourceClient;

    @Autowired
    public CronJobServiceImpl(ServiceCronJobResourceClient serviceCronJobResourceClient) {
        this.serviceCronJobResourceClient = serviceCronJobResourceClient;
    }

    @Override
    public Map<Long, List<CronJobVO>> batchListCronJobByPlanIds(Long appId, List<Long> planIdList) {
        if (appId <= 0 || CollectionUtils.isEmpty(planIdList)) {
            return null;
        }
        ServiceResponse<Map<Long, List<CronJobVO>>> cronJobByPlanIds =
            serviceCronJobResourceClient.batchListCronJobByPlanIds(appId, planIdList);
        if (cronJobByPlanIds != null) {
            return cronJobByPlanIds.getData();
        }
        return null;
    }
}
