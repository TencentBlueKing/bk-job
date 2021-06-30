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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.client.CronJobResourceClient;
import com.tencent.bk.job.analysis.client.TaskExecuteResultResourceClient;
import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.model.inner.AnalysisTaskResultItemLocation;
import com.tencent.bk.job.analysis.service.ApplicationService;
import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskStatusEnum;
import com.tencent.bk.job.analysis.task.analysis.anotation.AnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.enums.AnalysisResourceEnum;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultData;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultItem;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultVO;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 寻找周期内执行失败的定时任务
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@AnalysisTask("TimerTaskFailWatcher")
@Slf4j
public class TimerTaskFailWatcher extends AbstractTimerTaskWatcher {

    private final TaskExecuteResultResourceClient serviceTaskExecuteResultResourceClient;

    @Autowired
    public TimerTaskFailWatcher(
        DSLContext dslContext, AnalysisTaskDAO analysisTaskDAO,
        AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
        ApplicationService applicationService, CronJobResourceClient cronJobResourceClient,
        TaskExecuteResultResourceClient serviceTaskExecuteResultResourceClient
    ) {
        super(dslContext, analysisTaskDAO, analysisTaskInstanceDAO, applicationService, cronJobResourceClient);
        this.serviceTaskExecuteResultResourceClient = serviceTaskExecuteResultResourceClient;
    }

    @Override
    protected void analyseAppCrons(
        AnalysisTaskInstanceDTO analysisTaskInstanceDTO,
        List<ServiceCronJobDTO> cronJobVOList
    ) {
        List<FailCronJobBaseInfo> failCronJobBaseInfoList = new ArrayList<>();
        //2.遍历定时任务
        cronJobVOList.forEach(it -> {
            //3.拿到每一个定时任务在指定时间段内的执行结果并找出失败的
            log.info("begin to find fail result of task:" + it.getId() + "," + it.getName());
            PageData<ServiceTaskInstanceDTO> failResults = getFailResults(serviceTaskExecuteResultResourceClient, it);
            if (failResults.getTotal() > 0) {
                failCronJobBaseInfoList.add(
                    new FailCronJobBaseInfo(
                        AnalysisResourceEnum.TIMER_TASK,
                        new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result.ItemLocation"
                                + ".description.TimerTaskName}",
                            it.getName()),
                        it.getName()
                    )
                );
            }
        });
        //结果入库
        log.info(String.format("%d failResults are recorded:%s", failCronJobBaseInfoList.size(),
            JsonUtils.toJson(failCronJobBaseInfoList)));
        analysisTaskInstanceDTO.setResultData(
            JsonUtils.toJson(
                new AnalysisTaskResultData<>(
                    (long) (failCronJobBaseInfoList.size()),
                    failCronJobBaseInfoList)
            )
        );
        analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.SUCCESS.getValue());
        updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
        log.info("found failCronJobBaseInfoList are recorded");
    }

    @Override
    public AnalysisTaskResultVO generateResultVO(String descriptionTpl, String itemTpl, String data) {
        AnalysisTaskResultData<FailCronJobBaseInfo> resultData = JsonUtils.fromJson(data,
            new TypeReference<AnalysisTaskResultData<FailCronJobBaseInfo>>() {
            });
        String description;
        List<AnalysisTaskResultItem> contents = new ArrayList<>();
        if (resultData.getCount() == 0) {
            return null;
        } else if (resultData.getCount() == 1) {
            FailCronJobBaseInfo failCronJobBaseInfo = resultData.getData().get(0);
            description = itemTpl.replace("${taskName}", resultData.getData().get(0).getTaskName());
            contents.add(new AnalysisTaskResultItem(failCronJobBaseInfo.analysisResourceType.name(),
                failCronJobBaseInfo.location, description));
        } else {
            description = descriptionTpl.replace("${itemsCount}", "" + resultData.getData().size());
            resultData.getData().forEach(timerTaskBaseInfo ->
                contents.add(
                    new AnalysisTaskResultItem(
                        timerTaskBaseInfo.analysisResourceType.name(),
                        timerTaskBaseInfo.location,
                        timerTaskBaseInfo.getTaskName()
                    )
                )
            );
        }
        return new AnalysisTaskResultVO(description, contents);
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    private static class FailCronJobBaseInfo {
        private AnalysisResourceEnum analysisResourceType;
        private AnalysisTaskResultItemLocation location;
        private String taskName;
    }
}
