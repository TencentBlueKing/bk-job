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

package com.tencent.bk.job.analysis.task.statistics.task.impl.app.per;

import com.tencent.bk.job.analysis.client.ManageMetricsClient;
import com.tencent.bk.job.analysis.consts.StepTypeEnum;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BasePerAppStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 统计作业模板步骤类型分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class TaskTemplateStepPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;

    protected TaskTemplateStepPerAppStatisticsTask(BasicServiceManager basicServiceManager,
                                                   StatisticsDAO statisticsDAO, DSLContext dslContext,
                                                   ManageMetricsClient manageMetricsClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
    }

    private StatisticsDTO genTaskTplStepTypeStatistics(String dateStr, Long appId, String value,
                                                       String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_TASK_TEMPLATE_STEP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_STEP_TYPE);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    public List<StatisticsDTO> calcAppTaskTemplateStepTypeStatistics(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 1.手工录入脚本的步骤统计
        InternalResponse<Integer> resp = manageMetricsClient.countTemplateSteps(appId, TaskStepTypeEnum.SCRIPT,
            TaskScriptSourceEnum.LOCAL, null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer localScriptStepCount = resp.getData();
        statisticsDTOList.add(genTaskTplStepTypeStatistics(dateStr, appId, localScriptStepCount.toString(),
            StatisticsConstants.DIMENSION_VALUE_STEP_TYPE_PREFIX + StepTypeEnum.SCRIPT_MANUAL.name()));
        // 2.引用脚本的步骤统计
        resp = manageMetricsClient.countTemplateSteps(appId, TaskStepTypeEnum.SCRIPT, TaskScriptSourceEnum.CITING,
            null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer citedAppScriptStepCount = resp.getData();
        resp = manageMetricsClient.countTemplateSteps(appId, TaskStepTypeEnum.SCRIPT, TaskScriptSourceEnum.PUBLIC,
            null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer citedPublicScriptStepCount = resp.getData();
        int citedScriptStepCount = citedAppScriptStepCount + citedPublicScriptStepCount;
        statisticsDTOList.add(genTaskTplStepTypeStatistics(dateStr, appId,
            Integer.toString(citedScriptStepCount),
            StatisticsConstants.DIMENSION_VALUE_STEP_TYPE_PREFIX + StepTypeEnum.SCRIPT_REF.name()));
        // 3.分发本地文件的步骤统计
        resp = manageMetricsClient.countTemplateSteps(
            appId,
            TaskStepTypeEnum.FILE,
            null,
            TaskFileTypeEnum.LOCAL
        );
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer localFileStepCount = resp.getData();
        statisticsDTOList.add(genTaskTplStepTypeStatistics(dateStr, appId, localFileStepCount.toString(),
            StatisticsConstants.DIMENSION_VALUE_STEP_TYPE_PREFIX + StepTypeEnum.FILE_LOCAL.name()));
        // 4.分发服务器文件的步骤统计
        resp = manageMetricsClient.countTemplateSteps(
            appId,
            TaskStepTypeEnum.FILE,
            null,
            TaskFileTypeEnum.SERVER
        );
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer serverFileStepCount = resp.getData();
        statisticsDTOList.add(genTaskTplStepTypeStatistics(dateStr, appId, serverFileStepCount.toString(),
            StatisticsConstants.DIMENSION_VALUE_STEP_TYPE_PREFIX + StepTypeEnum.FILE_SERVER.name()));
        // 5.人工审核的步骤统计
        resp = manageMetricsClient.countTemplateSteps(appId, TaskStepTypeEnum.APPROVAL, null, null);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countTemplateSteps, resp:{}", resp);
            return statisticsDTOList;
        }
        Integer approvalStepCount = resp.getData();
        statisticsDTOList.add(genTaskTplStepTypeStatistics(dateStr, appId, approvalStepCount.toString(),
            StatisticsConstants.DIMENSION_VALUE_STEP_TYPE_PREFIX + StepTypeEnum.CONFIRM.name()));
        return statisticsDTOList;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        return calcAppTaskTemplateStepTypeStatistics(timeTag, app.getId());
    }
}
