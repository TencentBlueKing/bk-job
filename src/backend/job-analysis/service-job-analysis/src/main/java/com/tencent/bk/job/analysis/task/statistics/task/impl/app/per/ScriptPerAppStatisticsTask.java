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
import com.tencent.bk.job.analysis.consts.TotalMetricEnum;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BasePerAppStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 统计脚本类型分布情况与脚本版本状态分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class ScriptPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;

    protected ScriptPerAppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                         DSLContext dslContext, ManageMetricsClient manageMetricsClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
    }

    private StatisticsDTO genScriptTotalDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_GLOBAL);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_GLOBAL_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(
            StatisticsConstants.DIMENSION_VALUE_GLOBAL_STATISTIC_TYPE_PREFIX
                + TotalMetricEnum.SCRIPT_COUNT.name()
        );
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genScriptTypeDTO(String dateStr, Long appId, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_SCRIPT);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_TYPE);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genScriptVersionStatusDTO(String dateStr, Long appId, String value,
                                                    String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_SCRIPT_VERSION);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_SCRIPT_VERSION_STATUS);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    public List<StatisticsDTO> calcAppScriptTotal(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        InternalResponse<Integer> resp = manageMetricsClient.countScripts(
            appId,
            null,
            null
        );
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote countScripts, resp:{}", resp);
            return Collections.emptyList();
        }
        Integer scriptCount = resp.getData();
        statisticsDTOList.add(genScriptTotalDTO(dateStr, appId, scriptCount.toString()));
        return statisticsDTOList;
    }

    public List<StatisticsDTO> calcAppScriptType(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        for (int i = 0; i < ScriptTypeEnum.values().length; i++) {
            ScriptTypeEnum scriptType = ScriptTypeEnum.values()[i];
            InternalResponse<Integer> resp = manageMetricsClient.countScripts(
                appId,
                scriptType,
                null
            );
            if (resp == null || !resp.isSuccess()) {
                log.warn("Fail to call remote countScripts, resp:{}", resp);
                continue;
            }
            Integer scriptCount = resp.getData();
            statisticsDTOList.add(genScriptTypeDTO(dateStr, appId, scriptCount.toString(),
                StatisticsConstants.DIMENSION_VALUE_SCRIPT_TYPE_PREFIX + scriptType.getName()));
        }
        return statisticsDTOList;
    }

    public List<StatisticsDTO> calcAppScriptVerStatus(String dateStr, Long appId) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        for (int i = 0; i < JobResourceStatusEnum.values().length; i++) {
            JobResourceStatusEnum jobResourceStatus = JobResourceStatusEnum.values()[i];
            InternalResponse<Integer> resp = manageMetricsClient.countScriptVersions(
                appId,
                null,
                jobResourceStatus
            );
            if (resp == null || !resp.isSuccess()) {
                log.warn("Fail to call remote countScriptVersions, resp:{}", resp);
                continue;
            }
            Integer scriptVersionCount = resp.getData();
            statisticsDTOList.add(
                genScriptVersionStatusDTO(
                    dateStr,
                    appId,
                    scriptVersionCount.toString(),
                    StatisticsConstants.DIMENSION_VALUE_SCRIPT_VERSION_STATUS_PREFIX
                        + jobResourceStatus.name()
                )
            );
        }
        return statisticsDTOList;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        // 按脚本类型统计
        statisticsDTOList.addAll(calcAppScriptType(timeTag, app.getId()));
        // 按脚本版本状态统计
        statisticsDTOList.addAll(calcAppScriptVerStatus(timeTag, app.getId()));
        // 统计总数
        statisticsDTOList.addAll(calcAppScriptTotal(timeTag, app.getId()));
        return statisticsDTOList;
    }
}
