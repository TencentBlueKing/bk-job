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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.crontab.model.dto.CronJobBasicInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobBatchLoadService;
import com.tencent.bk.job.crontab.service.CronJobLoadingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CronJobLoadingServiceImpl implements CronJobLoadingService {

    private final CronJobBatchLoadService cronJobBatchLoadService;
    private volatile boolean loadingCronToQuartz = false;

    @Autowired
    public CronJobLoadingServiceImpl(CronJobBatchLoadService cronJobBatchLoadService) {
        this.cronJobBatchLoadService = cronJobBatchLoadService;
    }

    @Override
    public void loadAllCronJob() {
        long start = System.currentTimeMillis();
        try {
            if (loadingCronToQuartz) {
                log.info("Last loading not finish, ignore");
                return;
            }
            loadingCronToQuartz = true;
            loadAllCronJobToQuartz();
        } catch (Exception e) {
            log.warn("Fail to loadAllCronJob", e);
        } finally {
            loadingCronToQuartz = false;
            log.info("loadAllCronJob end, duration={}ms", System.currentTimeMillis() - start);
        }
    }

    private void loadAllCronJobToQuartz() {
        int start = 0;
        int limit = 100;
        int currentFetchNum;
        int allFetchNum = 0;
        int successNum = 0;
        int failedNum = 0;
        List<CronJobBasicInfoDTO> failedCronList = new ArrayList<>();
        do {
            CronJobBatchLoadService.CronLoadResult loadResult = cronJobBatchLoadService.batchLoadCronToQuartz(
                start,
                limit
            );
            currentFetchNum = loadResult.getFetchNum();
            allFetchNum += currentFetchNum;
            successNum += loadResult.getSuccessNum();
            failedNum += loadResult.getFailedNum();
            if (CollectionUtils.isNotEmpty(loadResult.getFailedCronList())) {
                failedCronList.addAll(loadResult.getFailedCronList());
            }
            start += limit;
        } while (currentFetchNum > 0);
        log.info(
            "CronJobs load from db finished: fetchNum={}, successNum={}, failedNum={}, failedCronList={}",
            allFetchNum,
            successNum,
            failedNum,
            failedCronList
        );
    }
}
