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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.config.listener.StatisticConfig;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.model.web.CommonDistributionVO;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class TagStatisticService {

    private final StatisticsDAO statisticsDAO;
    private final StatisticConfig statisticConfig;

    @Autowired
    public TagStatisticService(StatisticsDAO statisticsDAO, StatisticConfig statisticConfig) {
        this.statisticsDAO = statisticsDAO;
        this.statisticConfig = statisticConfig;
    }

    private Map<String, Long> mergeMap(Map<String, Long> map1, Map<String, Long> map2) {
        for (Map.Entry<String, Long> entry : map2.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            if (map1.containsKey(key)) {
                map1.put(key, map1.get(key) + value);
            } else {
                map1.put(key, value);
            }
        }
        return map1;
    }

    public CommonDistributionVO tagDistributionStatistics(List<Long> appIdList, String date) {
        List<StatisticsDTO> statisticsDTOList = statisticsDAO.getStatisticsList(appIdList, null,
            StatisticsConstants.RESOURCE_TAG, StatisticsConstants.DIMENSION_TAG_STATISTIC_TYPE,
            StatisticsConstants.DIMENSION_VALUE_TAG_STATISTIC_TYPE_DISTRIBUTION_MAP, date);
        if (statisticsDTOList == null || statisticsDTOList.isEmpty()) {
            return null;
        }
        CommonDistributionVO commonDistributionVO = new CommonDistributionVO();
        Map<String, Long> map = new HashMap<>();
        // 合并多个业务的标签分布Map
        for (StatisticsDTO statisticsDTO : statisticsDTOList) {
            Map<String, Long> appTagDistributionMap = JsonUtils.fromJson(statisticsDTO.getValue(),
                new TypeReference<Map<String, Long>>() {
            });
            mergeMap(map, appTagDistributionMap);
        }
        List<Pair<String, Long>> tagList = new ArrayList<>();
        for (Map.Entry<String, Long> entry : map.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            tagList.add(Pair.of(key, value));
        }
        // 降序排序
        tagList.sort((o1, o2) -> o2.getRight().compareTo(o1.getRight()));
        if (tagList.size() > statisticConfig.getMaxTagNum()) {
            tagList = tagList.subList(0, statisticConfig.getMaxTagNum());
        }
        map.clear();
        for (Pair<String, Long> pair : tagList) {
            map.put(pair.getLeft(), pair.getRight());
        }
        commonDistributionVO.setLabelAmountMap(map);
        return commonDistributionVO;
    }
}
