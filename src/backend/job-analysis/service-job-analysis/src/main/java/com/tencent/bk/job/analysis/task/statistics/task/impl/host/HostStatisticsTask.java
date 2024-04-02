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

package com.tencent.bk.job.analysis.task.statistics.task.impl.host;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BaseStatisticsTask;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceMetricsResource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 统计主机操作系统类型分布情况
 */
@StatisticsTask
@Slf4j
@Service
public class HostStatisticsTask extends BaseStatisticsTask {

    private final ServiceMetricsResource manageMetricsResource;
    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    protected HostStatisticsTask(BasicServiceManager basicServiceManager,
                                 StatisticsDAO statisticsDAO,
                                 @Qualifier("job-analysis-dsl-context") DSLContext dslContext,
                                 ServiceMetricsResource manageMetricsResource,
                                 IBizCmdbClient bizCmdbClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsResource = manageMetricsResource;
        this.bizCmdbClient = bizCmdbClient;
    }

    private StatisticsDTO genStatisticsDTO(String dateStr, String value, String dimensionValue) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(-1L);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_HOST_OF_ALL_APP);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_HOST_SYSTEM_TYPE);
        statisticsDTO.setDimensionValue(dimensionValue);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }

    private StatisticsDTO genHostOsTypeStatisticsDTO(String osTypeName, String dateStr, String value) {
        return genStatisticsDTO(dateStr, value, osTypeName);
    }

    private StatisticsDTO genOthersStatisticsDTO(String dateStr, String value) {
        return genStatisticsDTO(dateStr, value, StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_OTHERS);
    }

    public void calcAndSaveHostStatistics(String dateStr) {
        Set<String> allOsTypeNameSet = getAllOsTypeNameSet();

        InternalResponse<Map<String, Integer>> resp = manageMetricsResource.groupHostByOsType();
        Map<String, Integer> osTypeCountMap = resp.getData();
        // 统计所有类型的主机总量
        int totalHostCount = 0;
        for (Map.Entry<String, Integer> e : osTypeCountMap.entrySet()) {
            Integer count = e.getValue();
            totalHostCount += count;
        }
        Map<String, String> osTypeIdNameMap = bizCmdbClient.getOsTypeIdNameMap();
        int knownOsTypeHostCount = 0;
        for (Map.Entry<String, String> entry : osTypeIdNameMap.entrySet()) {
            String id = entry.getKey();
            String name = entry.getValue().toUpperCase();
            // 只为Job关注的系统类型主机生成统计数据
            if (!allOsTypeNameSet.contains(name)) {
                continue;
            }

            allOsTypeNameSet.remove(name);
            Integer hostCount = 0;
            if (osTypeCountMap.containsKey(id)) {
                hostCount = osTypeCountMap.get(id);
            }
            knownOsTypeHostCount += hostCount;
            statisticsDAO.upsertStatistics(
                dslContext,
                genHostOsTypeStatisticsDTO(name, dateStr, hostCount.toString())
            );
            log.debug("calcAndSaveHostStatistics: id={},name={},hostCount={}", id, name, hostCount);
        }
        // CMDB中没有的类型统计值默认置为0
        allOsTypeNameSet.forEach(osTypeName -> {
            log.debug("calcAndSaveHostStatistics: {} count=0", osTypeName);
            statisticsDAO.upsertStatistics(
                dslContext,
                genStatisticsDTO(dateStr, "0", osTypeName.toUpperCase())
            );
        });
        // Others
        int othersCount = totalHostCount - knownOsTypeHostCount;
        log.debug("calcAndSaveHostStatistics: othersCount={}", othersCount);
        statisticsDAO.upsertStatistics(dslContext, genOthersStatisticsDTO(dateStr, Long.toString(othersCount)));
    }

    @NotNull
    private static Set<String> getAllOsTypeNameSet() {
        Set<String> allOsTypeNameSet = new HashSet<>();
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_LINUX);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_WINDOWS);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_AIX);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_UNIX);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_SOLARIS);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_FREEBSD);
        allOsTypeNameSet.add(StatisticsConstants.DIMENSION_VALUE_HOST_SYSTEM_TYPE_MACOS);
        return allOsTypeNameSet;
    }

    @Override
    public void genStatisticsByDay(LocalDateTime dateTime) {
        calcAndSaveHostStatistics(getDayTimeStr(dateTime));
    }
}
