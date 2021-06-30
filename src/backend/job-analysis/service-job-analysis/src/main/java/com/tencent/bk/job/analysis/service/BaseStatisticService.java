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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.model.web.CommonDistributionVO;
import com.tencent.bk.job.analysis.model.web.DayDistributionElementVO;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;

@Slf4j
@Service
public class BaseStatisticService {

    protected List<DayDistributionElementVO> groupByDateAndDimensionValue(List<StatisticsDTO> statisticsDTOList,
                                                                          List<StatisticsDTO> failedStatisticsDTOList) {
        // 按日期+维度对多个业务数据进行聚合
        Map<Pair<String, String>, Long> dateDimensionValueTaskCountMap = new HashMap<>();
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            Pair<String, String> key = Pair.of(statisticsDTO.getDate(), statisticsDTO.getDimensionValue());
            if (dateDimensionValueTaskCountMap.containsKey(key)) {
                dateDimensionValueTaskCountMap.put(key,
                    dateDimensionValueTaskCountMap.get(key) + Long.parseLong(statisticsDTO.getValue()));
            } else {
                dateDimensionValueTaskCountMap.put(key, Long.parseLong(statisticsDTO.getValue()));
            }
        }
        log.debug("dateDimensionValueTaskCountMap={}", dateDimensionValueTaskCountMap);
        // 失败次数按日期对多个业务数据进行聚合
        Map<String, Long> dateFailCountMap = new HashMap<>();
        for (StatisticsDTO failedStatisticsDTO : failedStatisticsDTOList) {
            String key = failedStatisticsDTO.getDate();
            if (dateFailCountMap.containsKey(key)) {
                dateFailCountMap.put(key, dateFailCountMap.get(key) + Long.parseLong(failedStatisticsDTO.getValue()));
            } else {
                dateFailCountMap.put(key, Long.parseLong(failedStatisticsDTO.getValue()));
            }
        }
        // 按日期、维度取值聚合
        Map<String, DayDistributionElementVO> dayDetailMap = new HashMap<>();
        for (Map.Entry<Pair<String, String>, Long> entry : dateDimensionValueTaskCountMap.entrySet()) {
            Pair<String, String> key = entry.getKey();
            Long count = entry.getValue();
            String date = key.getLeft();
            String dimensionValue = key.getRight();
            if (dayDetailMap.containsKey(date)) {
                DayDistributionElementVO dayDistributionElementVO = dayDetailMap.get(date);
                dayDistributionElementVO.getDistribution().getLabelAmountMap().put(dimensionValue, count);
            } else {
                DayDistributionElementVO dayDistributionElementVO = new DayDistributionElementVO();
                dayDistributionElementVO.setDate(date);
                CommonDistributionVO distribution = new CommonDistributionVO();
                Map<String, Long> labelAmountMap = new HashMap<>();
                labelAmountMap.put(dimensionValue, count);
                distribution.setLabelAmountMap(labelAmountMap);
                dayDistributionElementVO.setDistribution(distribution);
                dayDetailMap.put(date, dayDistributionElementVO);
                if (dateFailCountMap.containsKey(date)) {
                    dayDistributionElementVO.setFailCount(dateFailCountMap.get(date));
                } else {
                    log.warn("Cannot find failCount data of {}, use default 0", date);
                    dayDistributionElementVO.setFailCount(0L);
                }
            }
        }
        log.debug("dayDetailMap={}", dayDetailMap);
        List<DayDistributionElementVO> dayDistributionElementVOList = new ArrayList<>(dayDetailMap.values());
        // 排序
        dayDistributionElementVOList.sort(Comparator.comparing(new Function<DayDistributionElementVO, String>() {
            @Override
            public String apply(DayDistributionElementVO dayDistributionElementVO) {
                return dayDistributionElementVO.getDate();
            }
        }));
        return dayDistributionElementVOList;
    }

}
