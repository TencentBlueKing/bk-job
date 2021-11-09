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
import com.tencent.bk.job.analysis.client.TagClient;
import com.tencent.bk.job.analysis.dao.StatisticsDAO;
import com.tencent.bk.job.analysis.service.BasicServiceManager;
import com.tencent.bk.job.analysis.task.statistics.anotation.StatisticsTask;
import com.tencent.bk.job.analysis.task.statistics.task.BasePerAppStatisticsTask;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTagDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计标签名称被引情况
 */
@StatisticsTask
@Slf4j
@Service
public class TagPerAppStatisticsTask extends BasePerAppStatisticsTask {

    private final ManageMetricsClient manageMetricsClient;
    private final TagClient tagClient;

    protected TagPerAppStatisticsTask(BasicServiceManager basicServiceManager, StatisticsDAO statisticsDAO,
                                      DSLContext dslContext, ManageMetricsClient manageMetricsClient,
                                      TagClient tagClient) {
        super(basicServiceManager, statisticsDAO, dslContext);
        this.manageMetricsClient = manageMetricsClient;
        this.tagClient = tagClient;
    }

    private StatisticsDTO genTagDistributionStatisticsDTO(String dateStr, Long appId, String value) {
        StatisticsDTO statisticsDTO = new StatisticsDTO();
        statisticsDTO.setAppId(appId);
        statisticsDTO.setDate(dateStr);
        statisticsDTO.setResource(StatisticsConstants.RESOURCE_TAG);
        statisticsDTO.setDimension(StatisticsConstants.DIMENSION_TAG_STATISTIC_TYPE);
        statisticsDTO.setDimensionValue(StatisticsConstants.DIMENSION_VALUE_TAG_STATISTIC_TYPE_DISTRIBUTION_MAP);
        statisticsDTO.setValue(value);
        return statisticsDTO;
    }


    public List<StatisticsDTO> calcAppTagDistributionStatistics(String dateStr, Long appId,
                                                                List<ServiceTagDTO> tagList) {
        List<StatisticsDTO> statisticsDTOList = new ArrayList<>();
        Map<String, Long> tagCitedCountMap = new HashMap<>();
        for (ServiceTagDTO serviceTagDTO : tagList) {
            Long tagId = serviceTagDTO.getId();
            InternalResponse<Long> resp = manageMetricsClient.tagCitedCount(appId, tagId);
            if (resp == null || !resp.isSuccess()) {
                log.warn("Fail to call remote tagCitedCount, resp:{}", resp);
                continue;
            }
            Long citedCount = resp.getData();
            String key = serviceTagDTO.getName();
            if (tagCitedCountMap.containsKey(key)) {
                tagCitedCountMap.put(key, tagCitedCountMap.get(key) + citedCount);
            } else {
                tagCitedCountMap.put(key, citedCount);
            }
        }
        log.debug("appId={},tagCitedCountMap={}", appId, tagCitedCountMap);
        // 单条统计数据含义：公共标签及业务自定义标签分别在业务内被引用了多少次
        statisticsDTOList.add(genTagDistributionStatisticsDTO(dateStr, appId, JsonUtils.toJson(tagCitedCountMap)));
        return statisticsDTOList;
    }

    @Override
    public List<StatisticsDTO> getStatisticsFrom(ServiceApplicationDTO app, Long fromTime, Long toTime,
                                                 String timeTag) {
        // 获取公共标签
        InternalResponse<List<ServiceTagDTO>> resp = tagClient.listPublicTags();
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote listPublicTags, resp:{}", resp);
            return Collections.emptyList();
        }
        List<ServiceTagDTO> publicTagList = resp.getData();
        log.debug("publicTagList={}", publicTagList);
        Long appId = app.getId();
        resp = tagClient.listTags(appId);
        if (resp == null || !resp.isSuccess()) {
            log.warn("Fail to call remote listTags, resp:{}", resp);
            return Collections.emptyList();
        }
        List<ServiceTagDTO> appTagList = resp.getData();
        log.debug("appTagList={}", appTagList);
        // 添加公共标签
        appTagList.addAll(publicTagList);
        return calcAppTagDistributionStatistics(timeTag, appId, appTagList);
    }
}
