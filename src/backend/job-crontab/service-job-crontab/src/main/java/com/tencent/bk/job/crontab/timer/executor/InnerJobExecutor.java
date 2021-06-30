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

package com.tencent.bk.job.crontab.timer.executor;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobInfoDTO;
import com.tencent.bk.job.crontab.service.InnerJobHistoryService;
import com.tencent.bk.job.crontab.timer.AbstractQuartzJobBean;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * 内部定时任务执行器
 *
 * @since 16/2/2020 15:38
 */
@Slf4j
@Setter
public class InnerJobExecutor extends AbstractQuartzJobBean {

    @Autowired
    InnerJobHistoryService cronJobHistoryService;

    @Autowired
    private RestTemplate restTemplate;

    private String cronJobInfoStr;

    @Override
    public String name() {
        return "InnerCronJob";
    }

    @Override
    protected void executeInternalInternal(JobExecutionContext context) throws JobExecutionException {
        if (log.isDebugEnabled()) {
            log.debug("Execute inner task|{}", cronJobInfoStr);
        }

        InnerCronJobInfoDTO innerCronJobInfo = JsonUtils.fromJson(cronJobInfoStr, InnerCronJobInfoDTO.class);
        String systemId = innerCronJobInfo.getSystemId();
        String jobKey = innerCronJobInfo.getJobKey();
        long scheduledFireTime = getScheduledFireTime(context).toEpochMilli();

        InnerCronJobHistoryDTO cronJobHistory =
            cronJobHistoryService.getHistoryByIdAndTime(systemId, jobKey, scheduledFireTime);
        if (cronJobHistory != null) {
            log.warn("Job already running!|{}", cronJobHistory);
            return;
        }

        long historyId = cronJobHistoryService.insertHistory(systemId, jobKey, scheduledFireTime);
        if (log.isDebugEnabled()) {
            log.debug("Insert history finished!|{}", historyId);
        }

        try {
            ResponseEntity<String> stringResponseEntity;
            if (StringUtils.isNotBlank(innerCronJobInfo.getCallbackData())) {

                HttpHeaders headers = new HttpHeaders();
                MediaType type = MediaType.APPLICATION_JSON;
                headers.setContentType(type);
                String requestJson = innerCronJobInfo.getCallbackData();
                HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

                stringResponseEntity = restTemplate.postForEntity(
                    getCallbackUrl(innerCronJobInfo.getSystemId(), innerCronJobInfo.getCallbackUri()), entity,
                    String.class);
            } else {
                stringResponseEntity = restTemplate.getForEntity(
                    getCallbackUrl(innerCronJobInfo.getSystemId(), innerCronJobInfo.getCallbackUri()), String.class);
            }

            if (log.isDebugEnabled()) {
                log.debug("Execute result|{}", stringResponseEntity);
            }
            if (stringResponseEntity.getStatusCode() == HttpStatus.OK) {
                if (log.isDebugEnabled()) {
                    log.debug("Execute success! Return message|{}", stringResponseEntity.getBody());
                }
                cronJobHistoryService.updateStatusByIdAndTime(systemId, jobKey, scheduledFireTime,
                    ExecuteStatusEnum.SUCCESS);
            } else {
                log.error("Execute task failed!|{}|{}|{}|{}", systemId, jobKey, scheduledFireTime,
                    stringResponseEntity);
                cronJobHistoryService.updateStatusByIdAndTime(systemId, jobKey, scheduledFireTime,
                    ExecuteStatusEnum.FAIL);
            }
        } catch (RestClientException e) {
            log.error("Execute task failed!|{}|{}|{}", systemId, jobKey, scheduledFireTime, e);
            cronJobHistoryService.updateStatusByIdAndTime(systemId, jobKey, scheduledFireTime, ExecuteStatusEnum.FAIL);
        }
    }

    private String getCallbackUrl(String systemId, String callbackUrl) {
        return "http://" + systemId + callbackUrl;
    }
}
