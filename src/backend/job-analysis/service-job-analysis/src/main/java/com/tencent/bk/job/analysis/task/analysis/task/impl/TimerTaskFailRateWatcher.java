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

package com.tencent.bk.job.analysis.task.analysis.task.impl;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.tencent.bk.job.crontab.api.inner.ServiceCronJobResource;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.execute.api.inner.ServiceTaskExecuteResultResource;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 寻找周期内定时任务失败率超过60%的定时任务
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@AnalysisTask("TimerTaskFailRateWatcher")
@Slf4j
public class TimerTaskFailRateWatcher extends AbstractTimerTaskWatcher {

    private final ServiceTaskExecuteResultResource taskExecuteResultResource;

    @Autowired
    public TimerTaskFailRateWatcher(AnalysisTaskDAO analysisTaskDAO,
                                    AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
                                    ApplicationService applicationService,
                                    ServiceCronJobResource cronJobResource,
                                    ServiceTaskExecuteResultResource taskExecuteResultResource) {
        super(analysisTaskDAO, analysisTaskInstanceDAO, applicationService, cronJobResource);
        this.taskExecuteResultResource = taskExecuteResultResource;
    }

    @Override
    protected void analyseAppCrons(
        AnalysisTaskInstanceDTO analysisTaskInstanceDTO,
        List<ServiceCronJobDTO> cronJobVOList
    ) {
        Long appId = analysisTaskInstanceDTO.getAppId();
        List<HighFailRateCronJobBaseInfo> highFailRateCronJobBaseInfoList = new ArrayList<>();
        //2.遍历定时任务
        cronJobVOList.forEach(it -> {
            //3.拿到每一个定时任务在指定时间段内的执行结果并找出失败的
            log.info("begin to find fail result of task:" + it.getId() + "," + it.getName());
            PageData<ServiceTaskInstanceDTO> failResult = getFailResults(taskExecuteResultResource, it);
            log.info("begin to find success result of task:" + it.getId() + "," + it.getName());
            PageData<ServiceTaskInstanceDTO> successResult =
                taskExecuteResultResource.getTaskExecuteResult(
                    appId,
                    null,
                    null,
                    RunStatusEnum.SUCCESS.getValue(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    Integer.MAX_VALUE,
                    it.getId()
                ).getData();
            log.info("" + successResult.getTotal()
                + " success results found of cronJobId:"
                + it.getId()
                + "," + it.getName()
            );
            long executedNum = failResult.getTotal() + successResult.getTotal();
            if (executedNum > 0 && (float) successResult.getTotal() / executedNum < 0.6) {
                highFailRateCronJobBaseInfoList.add(
                    new HighFailRateCronJobBaseInfo(
                        AnalysisResourceEnum.TIMER_TASK,
                        new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result.ItemLocation.description.TimerTaskName}",
                            it.getName()),
                        it.getName()
                    )
                );
            }
        });
        log.info("highFailRateCronJobBaseInfoList:" + JsonUtils.toJson(highFailRateCronJobBaseInfoList));
        //结果入库
        analysisTaskInstanceDTO.setResultData(
            JsonUtils.toJson(
                new AnalysisTaskResultData<>(
                    (long) highFailRateCronJobBaseInfoList.size(),
                    highFailRateCronJobBaseInfoList)
            )
        );
        analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.SUCCESS.getValue());
        updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
        log.info("found highFailRateCronJobNameList are recorded");
    }

    @Override
    public AnalysisTaskResultVO renderResultVO(String descriptionTpl, String itemTpl, String data) {
        AnalysisTaskResultData<HighFailRateCronJobBaseInfo> resultData = JsonUtils.fromJson(data,
            new TypeReference<AnalysisTaskResultData<HighFailRateCronJobBaseInfo>>() {
            });
        String description;
        List<AnalysisTaskResultItem> contents = new ArrayList<>();
        if (resultData.getCount() == 0) {
            return null;
        } else if (resultData.getCount() == 1) {
            HighFailRateCronJobBaseInfo highFailRateCronJobBaseInfo = resultData.getData().get(0);
            description = itemTpl.replace("${taskName}", resultData.getData().get(0).getTaskName());
            contents.add(new AnalysisTaskResultItem(highFailRateCronJobBaseInfo.analysisResourceType.name(),
                highFailRateCronJobBaseInfo.location, description));
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
    private static class HighFailRateCronJobBaseInfo {
        private AnalysisResourceEnum analysisResourceType;
        private AnalysisTaskResultItemLocation location;
        private String taskName;
    }
}
