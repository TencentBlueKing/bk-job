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

import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.crontab.model.dto.CronJobBasicInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobBatchLoadService;
import com.tencent.bk.job.crontab.service.CronJobService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CronJobBatchLoadServiceImpl implements CronJobBatchLoadService {

    private final CronJobService cronJobService;

    @Autowired
    public CronJobBatchLoadServiceImpl(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    @Override
    @JobTransactional(transactionManager = "jobCrontabTransactionManager", timeout = 30)
    public CronLoadResult batchLoadCronToQuartz(int start, int limit) throws InterruptedException {
        int successNum = 0;
        int failedNum = 0;
        List<CronJobBasicInfoDTO> failedCronList = new ArrayList<>();
        List<CronJobBasicInfoDTO> cronJobBasicInfoList = cronJobService.listEnabledCronBasicInfoForUpdate(start, limit);
        for (CronJobBasicInfoDTO cronJobBasicInfoDTO : cronJobBasicInfoList) {
            checkInterrupt();
            boolean result = false;
            try {
                result = cronJobService.addJobToQuartz(
                    cronJobBasicInfoDTO.getAppId(),
                    cronJobBasicInfoDTO.getId()
                );
                if (result) {
                    successNum += 1;
                } else {
                    failedNum += 1;
                    failedCronList.add(cronJobBasicInfoDTO);
                }
            } catch (Exception e) {
                failedNum += 1;
                failedCronList.add(cronJobBasicInfoDTO);
                String message = MessageFormatter.format(
                    "Fail to addJobToQuartz, cronJob={}",
                    cronJobBasicInfoDTO
                ).getMessage();
                log.warn(message, e);
            }
            if (log.isDebugEnabled()) {
                log.debug(
                    "load cronJob({},{},{}), result={}",
                    cronJobBasicInfoDTO.getAppId(),
                    cronJobBasicInfoDTO.getId(),
                    cronJobBasicInfoDTO.getName(),
                    result
                );
            }
        }
        CronLoadResult cronLoadResult = new CronLoadResult();
        cronLoadResult.setFetchNum(cronJobBasicInfoList.size());
        cronLoadResult.setSuccessNum(successNum);
        cronLoadResult.setFailedNum(failedNum);
        cronLoadResult.setFailedCronList(failedCronList);
        return cronLoadResult;
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("batchLoadCronToQuartz thread is interrupted, exit");
        }
    }
}
