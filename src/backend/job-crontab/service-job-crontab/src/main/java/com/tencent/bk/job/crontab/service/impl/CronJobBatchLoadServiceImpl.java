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
import com.tencent.bk.job.crontab.model.dto.AddJobToQuartzResult;
import com.tencent.bk.job.crontab.model.dto.BatchAddResult;
import com.tencent.bk.job.crontab.model.dto.CronJobBasicInfoDTO;
import com.tencent.bk.job.crontab.service.BatchCronJobService;
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
    private final BatchCronJobService batchCronJobService;

    @Autowired
    public CronJobBatchLoadServiceImpl(CronJobService cronJobService, BatchCronJobService batchCronJobService) {
        this.cronJobService = cronJobService;
        this.batchCronJobService = batchCronJobService;
    }

    @Override
    @JobTransactional(transactionManager = "jobCrontabTransactionManager", timeout = 30)
    public CronLoadResult batchLoadCronToQuartz(int start, int limit) throws InterruptedException {
        checkInterrupt();
        List<CronJobBasicInfoDTO> cronJobBasicInfoList = cronJobService.listEnabledCronBasicInfoForUpdate(start, limit);
        BatchAddResult batchAddResult = batchCronJobService.batchAddJobToQuartz(cronJobBasicInfoList);
        List<CronJobBasicInfoDTO> failedCronList = extractFailedCronList(batchAddResult);
        CronLoadResult cronLoadResult = new CronLoadResult();
        cronLoadResult.setFetchNum(cronJobBasicInfoList.size());
        cronLoadResult.setSuccessNum(batchAddResult.getSuccessNum());
        cronLoadResult.setFailedNum(batchAddResult.getFailNum());
        cronLoadResult.setFailedCronList(failedCronList);
        return cronLoadResult;
    }

    /**
     * 从批量添加定时任务结果数据中提取失败的定时任务信息
     *
     * @param batchAddResult 批量添加定时任务结果
     * @return 失败的定时任务信息
     */
    private List<CronJobBasicInfoDTO> extractFailedCronList(BatchAddResult batchAddResult) {
        if (batchAddResult == null || batchAddResult.getTotalNum() == 0) {
            return new ArrayList<>();
        }
        List<CronJobBasicInfoDTO> failedCronList = new ArrayList<>();
        int successNum = batchAddResult.getSuccessNum();
        int failedNum = batchAddResult.getFailNum();
        if (failedNum > 0) {
            String message = MessageFormatter.format(
                "batchAddJobToQuartz result: {} failed, {} success",
                failedNum,
                successNum
            ).getMessage();
            if (batchAddResult.getFailRate() > 0.5) {
                log.error(message);
            } else {
                log.warn(message);
            }
            List<AddJobToQuartzResult> failedResultList = batchAddResult.getFailedResultList();
            for (AddJobToQuartzResult addJobToQuartzResult : failedResultList) {
                CronJobBasicInfoDTO cronJobBasicInfo = addJobToQuartzResult.getCronJobBasicInfo();
                failedCronList.add(cronJobBasicInfo);
                message = MessageFormatter.arrayFormat(
                    "Fail to load cronJob({},{},{}), reason={}",
                    new Object[]{
                        cronJobBasicInfo.getAppId(),
                        cronJobBasicInfo.getId(),
                        cronJobBasicInfo.getName(),
                        addJobToQuartzResult.getMessage()
                    }
                ).getMessage();
                log.warn(message, addJobToQuartzResult.getException());
            }
        } else {
            log.info("batchAddJobToQuartz result: All success, num={}", successNum);
        }
        return failedCronList;
    }

    private void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("batchLoadCronToQuartz thread is interrupted, exit");
        }
    }
}
