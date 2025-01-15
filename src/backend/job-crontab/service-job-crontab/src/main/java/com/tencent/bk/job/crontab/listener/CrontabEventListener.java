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

package com.tencent.bk.job.crontab.listener;

import com.tencent.bk.job.crontab.constant.CrontabActionEnum;
import com.tencent.bk.job.crontab.listener.event.CrontabEvent;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.service.QuartzService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 定时任务事件处理
 */
@Component("crontabEventListener")
@Slf4j
public class CrontabEventListener {

    private final CronJobService cronJobService;
    private final QuartzService quartzService;

    @Autowired
    public CrontabEventListener(CronJobService cronJobService, QuartzService quartzService) {
        this.cronJobService = cronJobService;
        this.quartzService = quartzService;
    }


    /**
     * 处理定时任务相关的事件
     *
     * @param crontabEvent 定时任务相关的事件
     */
    public void handleEvent(CrontabEvent crontabEvent) {
        log.info("Handle crontab event, event: {}, duration: {}ms", crontabEvent, crontabEvent.duration());
        long appId = crontabEvent.getAppId();
        long cronJobId = crontabEvent.getCronJobId();
        CrontabActionEnum action = CrontabActionEnum.valueOf(crontabEvent.getAction());
        try {
            switch (action) {
                case ADD_CRON:
                    CronJobInfoDTO cronJobInfoDTO = cronJobService.getCronJobInfoById(cronJobId);
                    refreshCronJobInQuartz(cronJobInfoDTO);
                    break;
                case DELETE_CRON:
                    deleteCronJobFromQuartz(appId, cronJobId);
                    break;
                default:
                    log.error("Invalid crontabEvent action: {}", action);
            }
        } catch (Throwable e) {
            String errorMsg = MessageFormatter.format(
                "Handle crontab event error, appId={}, cronJobId={}",
                appId,
                cronJobId
            ).getMessage();
            log.error(errorMsg, e);
        }
    }

    private void refreshCronJobInQuartz(CronJobInfoDTO cronJobInfoDTO) {
        if (cronJobInfoDTO == null) {
            return;
        }
        if (cronJobInfoDTO.getEnable()) {
            // 开启定时任务
            boolean result = cronJobService.checkAndAddJobToQuartz(cronJobInfoDTO.getAppId(), cronJobInfoDTO.getId());
            log.info(
                "add cronJob({},{}) to quartz, result={}",
                cronJobInfoDTO.getAppId(),
                cronJobInfoDTO.getId(),
                result
            );
        } else {
            // 关闭定时任务
            boolean result = quartzService.deleteJobFromQuartz(cronJobInfoDTO.getAppId(), cronJobInfoDTO.getId());
            log.info(
                "delete cronJob({},{}) from quartz, result={}",
                cronJobInfoDTO.getAppId(),
                cronJobInfoDTO.getId(),
                result
            );
        }
    }

    private void deleteCronJobFromQuartz(long appId, long cronJobId) {
        // 删除定时任务
        boolean result = quartzService.deleteJobFromQuartz(appId, cronJobId);
        log.info(
            "delete cronJob({},{}) from quartz, result={}",
            appId,
            cronJobId,
            result
        );
    }

}
