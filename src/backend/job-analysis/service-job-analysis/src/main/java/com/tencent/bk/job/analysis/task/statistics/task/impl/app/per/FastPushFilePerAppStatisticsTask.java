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
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@StatisticsTask
@Service
public class FastPushFilePerAppStatisticsTask extends ExecuteBasePerAppStatisticsTask {

    @Autowired
    public FastPushFilePerAppStatisticsTask(ExecuteMetricsClient executeMetricsClient,
                                            BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                            DSLContext dslContext) {
        super(executeMetricsClient, basicServiceManager, statisticsDAO, dslContext);
    }

    private StatisticsDTO getRunStatusBaseDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE);//执行过的快速文件
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_STEP_RUN_STATUS);//步骤状态
        return statisticsDTO;
    }

    private StatisticsDTO getFileTransferModeBaseDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE);//执行过的快速文件
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE);//文件传输模式
        return statisticsDTO;
    }

    private StatisticsDTO getFileSourceTypeBaseDTO(ServiceApplicationDTO app, String timeTag) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(app.getId());
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(timeTag);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE);//执行过的快速文件
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_FILE_SOURCE_TYPE);//源文件类型
        return statisticsDTO;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 任务状态：成功
        StatisticsDTO statisticsDTO = getRunStatusBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);//成功
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 任务状态：失败
        statisticsDTO = getRunStatusBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);//失败
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 文件传输模式：严谨模式
        statisticsDTO = getFileTransferModeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_STRICT);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 文件传输模式：强制模式
        statisticsDTO = getFileTransferModeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_FORCE);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 源文件类型：本地
        statisticsDTO = getFileSourceTypeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_FILE_SOURCE_TYPE_LOCAL);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        // 源文件类型：服务器文件
        statisticsDTO = getFileSourceTypeBaseDTO(app, timeTag);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_FILE_SOURCE_TYPE_SERVER);
        addExecuteStatisticsDTO(statisticsDTO, statisticsDTOList);
        return statisticsDTOList;
    }

    public void aggAllAppStepRunStatus(String dayTimeStr) {
        aggAllAppStepRunStatus(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);
        aggAllAppStepRunStatus(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);
    }

    public void aggAllAppStepRunStatus(String dayTimeStr, String stepRunStatusDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
            stepRunStatusDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_STEP_RUN_STATUS);
        statisticsDTO.setDimensionValue(stepRunStatusDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    public void aggAllAppFileTransferMode(String dayTimeStr) {
        aggAllAppFileTransferMode(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_STRICT);
        aggAllAppFileTransferMode(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_FORCE);
    }

    public void aggAllAppFileTransferMode(String dayTimeStr, String fileTransferModeDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE,
            fileTransferModeDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE);
        statisticsDTO.setDimensionValue(fileTransferModeDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    public void aggAllAppFileSourceType(String dayTimeStr) {
        aggAllAppFileSourceType(dayTimeStr, StatisticsConstants.DIMENSION_VALUE_FILE_SOURCE_TYPE_LOCAL);
        aggAllAppFileSourceType(dayTimeStr,
            StatisticsConstants.DIMENSION_VALUE_FILE_SOURCE_TYPE_SERVER);
    }

    public void aggAllAppFileSourceType(String dayTimeStr, String fileSourceTypeDimensionValue) {
        Long totalValue = statisticsDAO.getTotalValueOfStatisticsList(null,
            Collections.singletonList(StatisticsConstants.DEFAULT_APP_ID),
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_SOURCE_TYPE,
            fileSourceTypeDimensionValue, dayTimeStr);
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(StatisticsConstants.DEFAULT_APP_ID);
        statisticsDTO.setCreateTime(System.currentTimeMillis());
        statisticsDTO.setDate(dayTimeStr);
        statisticsDTO.setValue(totalValue.toString());
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_FILE_SOURCE_TYPE);
        statisticsDTO.setDimensionValue(fileSourceTypeDimensionValue);
        statisticsDAO.upsertStatistics(dslContext, statisticsDTO);
    }

    public void aggregateAllAppStatistics(String dayTimeStr) {
        aggAllAppStepRunStatus(dayTimeStr);
        aggAllAppFileTransferMode(dayTimeStr);
        aggAllAppFileSourceType(dayTimeStr);
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        super.genStatisticsByDay(dateTime);
        // 聚合多个业务的数据
        String dayTimeStr = getDayTimeStr(dateTime);
        aggregateAllAppStatistics(dayTimeStr);
    }

    @Override
    public boolean isDataComplete(String targetDateStr) {
        boolean executedFastFileByStepRunStatusDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_STEP_RUN_STATUS, null,
            targetDateStr);
        boolean executedFastFileByFileTransferModeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE, null,
            targetDateStr);
        boolean executedFastFileByFileSourceTypeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_SOURCE_TYPE, null,
            targetDateStr);
        boolean allAppExecutedFastFileByStepRunStatusDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP,
            StatisticsConstants.DIMENSION_STEP_RUN_STATUS, null, targetDateStr);
        boolean allAppExecutedFastFileByFileTransferModeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP,
            StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE, null, targetDateStr);
        boolean allAppExecutedFastFileByFileSourceTypeDataExists = statisticsDAO.existsStatistics(null, null,
            StatisticsConstants.RESOURCE_ONE_DAY_EXECUTED_FAST_FILE_OF_ALL_APP,
            StatisticsConstants.DIMENSION_FILE_SOURCE_TYPE, null, targetDateStr);
        return executedFastFileByStepRunStatusDataExists
            && executedFastFileByFileTransferModeDataExists
            && executedFastFileByFileSourceTypeDataExists
            && allAppExecutedFastFileByStepRunStatusDataExists
            && allAppExecutedFastFileByFileTransferModeDataExists
            && allAppExecutedFastFileByFileSourceTypeDataExists;
    }
}
