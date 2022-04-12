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

package com.tencent.bk.job.analysis.task.analysis.task.impl;

import com.tencent.bk.job.analysis.client.CronJobResourceClient;
import com.tencent.bk.job.analysis.client.TaskExecuteResultResourceClient;
import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.service.ApplicationService;
import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskStatusEnum;
import com.tencent.bk.job.analysis.task.analysis.BaseAnalysisTask;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description 寻找周期内执行失败的定时任务
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@Slf4j
public abstract class AbstractTimerTaskWatcher extends BaseAnalysisTask {

    private final CronJobResourceClient cronJobResourceClient;

    @Autowired
    public AbstractTimerTaskWatcher(DSLContext dslContext, AnalysisTaskDAO analysisTaskDAO,
                                    AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
                                    ApplicationService applicationService,
                                    CronJobResourceClient cronJobResourceClient) {
        super(dslContext, analysisTaskDAO, analysisTaskInstanceDAO, applicationService);
        this.cronJobResourceClient = cronJobResourceClient;
    }

    abstract void analyseAppCrons(
        AnalysisTaskInstanceDTO analysisTaskInstanceDTO,
        List<ServiceCronJobDTO> cronJobVOList
    );

    PageData<ServiceTaskInstanceDTO> getFailResults(
        TaskExecuteResultResourceClient serviceTaskExecuteResultResourceClient,
        ServiceCronJobDTO cronJobDTO
    ) {
        PageData<ServiceTaskInstanceDTO> failResults = serviceTaskExecuteResultResourceClient.getTaskExecuteResult(
            cronJobDTO.getAppId(),
            null,
            null,
            RunStatusEnum.FAIL.getValue(),
            null,
            null,
            null,
            null,
            null,
            0,
            Integer.MAX_VALUE,
            cronJobDTO.getId()
        ).getData();
        log.info("" + failResults.getTotal()
            + " fail results found of cronJobId:"
            + cronJobDTO.getId()
            + "," + cronJobDTO.getName()
        );
        return failResults;
    }

    @Override
    public void run() {
        try {
            log.info("Task " + getTaskCode() + " start");
            Counter appCounter = new Counter();
            List<ServiceApplicationDTO> appInfoList = getAppInfoList();
            AnalysisTaskDTO analysisTask = getAnalysisTask();
            for (ServiceApplicationDTO applicationInfoDTO : appInfoList) {
                if (applicationInfoDTO.getAppType() != AppTypeEnum.NORMAL.getValue()) {
                    continue;
                }
                Long appId = applicationInfoDTO.getId();
                appCounter.addOne();
                log.info("beigin to analysis app:"
                    + appCounter.getValue()
                    + "/" + appInfoList.size()
                    + "," + appId
                    + "," + applicationInfoDTO.getName()
                );
                //初始化
                val analysisTaskInstanceDTO = new AnalysisTaskInstanceDTO(
                    null,
                    appId,
                    analysisTask.getId(),
                    AnalysisTaskStatusEnum.RUNNING.getValue(),
                    "",
                    analysisTask.getPriority(),
                    analysisTask.isActive(),
                    analysisTask.getCreator(),
                    System.currentTimeMillis(),
                    analysisTask.getCreator(),
                    System.currentTimeMillis()
                );
                try {
                    Long id = insertAnalysisTaskInstance(analysisTaskInstanceDTO);
                    log.info("taskId:" + id);
                    analysisTaskInstanceDTO.setId(id);
                    //1.拿到业务下所有开启的定时任务
                    val resp = cronJobResourceClient.listCronJobs(appId, true);
                    log.info("listAllCronJobs resp:" + JsonUtils.toJson(resp));
                    List<ServiceCronJobDTO> cronJobVOList = resp.getData();
                    log.info("" + cronJobVOList.size() + " cronJobs found");
                    LocalDateTime dateTime = LocalDateTime.now().minusSeconds(getAnalysisTask().getPeriodSeconds());
                    String startTime = DateUtils.formatLocalDateTime(dateTime, "yyyy-MM-dd HH:mm:ss");
                    log.debug(String.format("startTime=%s", startTime));
                    analyseAppCrons(analysisTaskInstanceDTO, cronJobVOList);
                } catch (Throwable t) {
                    analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.FAIL.getValue());
                    updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
                    log.warn(String.format("Task of app(id=%d) failed because of exception", appId), t);
                } finally {
                    log.info(String.format("Task:%s of app(id=%d) end", getTaskCode(), appId));
                }
            }
        } catch (Throwable t) {
            log.warn("Task failed because of exception", t);
        } finally {
            log.info("Task " + getTaskCode() + " end");
        }
    }
}
