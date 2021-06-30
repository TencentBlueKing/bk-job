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

import com.tencent.bk.job.analysis.client.ExecuteMetricsClient;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.ExecuteBasePerAppStatisticsTask;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StatisticsTask
@Service
public class FastExecuteScriptPerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public FastExecuteScriptPerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                                 BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO
        , DSLContext dslContext) {
        super(executeMetricsClient, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getRunStatusBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT);//执行过的快速脚本
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_STEP_RUN_STATUS);//步骤状态
        return statisticsDTO;
    }

    private StatisticsDTO getScriptTypeBaseStatisticsDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT);//执行过的快速脚本
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_TYPE);//脚本类型
        return statisticsDTO;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 成功
        StatisticsDTO statisticsDTO = getRunStatusBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);//成功
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 失败
        statisticsDTO = getRunStatusBaseStatisticsDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);//失败
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 按类型统计
        for (ScriptTypeEnum scriptType : ScriptTypeEnum.values()) {
            statisticsDTO = getScriptTypeBaseStatisticsDTO(app, timeTag);
            statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_SCRIPT_TYPE_PREFIX
                + scriptType.getName());//类型
            addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        }
        return statisticsDTOList;
    }

    /**
     * 聚合多个业务的脚本类型与步骤状态数据
     *
     * @param dayTimeStr
     */
    public void aggAllAppScriptTypeAndStepStatus(String dayTimeStr) {
        for (ScriptTypeEnum scriptType : ScriptTypeEnum.values()) {
            aggAllAppScriptType(dayTimeStr,
                StatisticsConstants.DIMENSION_VALUE_SCRIPT_TYPE_PREFIX + scriptType.getName());
        }
        aggAllAppStepRunStatus(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);
        aggAllAppStepRunStatus(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);
    }

    public void aggAllAppScriptType(String dayTimeStr, String scriptTypeDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_SCRIPT_TYPE,
            scriptTypeDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_SCRIPT_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_TYPE);
        statisticsDTO.setDimensionValue(scriptTypeDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    public void aggAllAppStepRunStatus(String dayTimeStr, String stepRunStatusDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
            stepRunStatusDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_SCRIPT_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_STEP_RUN_STATUS);
        statisticsDTO.setDimensionValue(stepRunStatusDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        super.genStatisticsByDay(dateTime);
        // 聚合多个业务的数据
        String dayTimeStr = getDayTimeStr(dateTime);
        aggAllAppScriptTypeAndStepStatus(dayTimeStr);
    }

    @Override
    public boolean isDataComplete(String targetDateStr) {
        boolean executedFastScriptByScriptTypeDataExists = statisticsDAO.existsStatistics(
            null,
            null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT,
            StatisticsConstants.DIMENSION_SCRIPT_TYPE,
            null,
            targetDateStr
        );
        boolean executedFastScriptByStepRunStatusDataExists = statisticsDAO.existsStatistics(
            null,
            null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT,
            StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
            null,
            targetDateStr
        );
        boolean allAppExecutedFastScriptByScriptTypeDataExists = statisticsDAO.existsStatistics(
            null,
            null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_SCRIPT_OF_ALL_APP,
            StatisticsConstants.DIMENSION_SCRIPT_TYPE,
            null,
            targetDateStr
        );
        boolean allAppExecutedFastScriptByStepRunStatusDataExists = statisticsDAO.existsStatistics(
            null,
            null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_SCRIPT_OF_ALL_APP,
            StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
            null,
            targetDateStr
        );
        return executedFastScriptByScriptTypeDataExists
            && executedFastScriptByStepRunStatusDataExists
            && allAppExecutedFastScriptByScriptTypeDataExists
            && allAppExecutedFastScriptByStepRunStatusDataExists;
    }
}
