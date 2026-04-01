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

package com.tencent.bk.job.service.impl;

import com.tencent.bk.job.config.BkLogProperties;
import com.tencent.bk.job.service.JobLogQueryService;
import com.tencent.bk.job.service.api.bklog.BkLogApi;
import com.tencent.bk.job.service.api.bklog.LogQueryReq;
import com.tencent.bk.job.service.api.bklog.LogQueryResp;
import com.tencent.bk.job.service.model.PageData;
import com.tencent.bk.job.service.model.SimpleLogDTO;
import com.tencent.bk.job.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JobLogQueryServiceImpl implements JobLogQueryService {

    private final BkLogApi bkLogApi;
    private final BkLogProperties bkLogProperties;

    @Autowired
    public JobLogQueryServiceImpl(BkLogApi bkLogApi, BkLogProperties bkLogProperties) {
        this.bkLogApi = bkLogApi;
        this.bkLogProperties = bkLogProperties;
    }

    @Override
    public PageData<SimpleLogDTO> queryLogs(String source,
                                            String queryString,
                                            String timeRange,
                                            String startTime,
                                            String endTime,
                                            Integer start,
                                            Integer size,
                                            Boolean asc) {
        try {
            BkLogProperties.LogSource logSource = bkLogProperties.getSource(source);
            LogQueryReq logQueryReq = buildLogQueryReq(
                logSource, queryString, timeRange, startTime, endTime, start, size);

            fillReqWithSortField(logQueryReq, logSource.getSortField(), asc);

            LogQueryResp logQueryResp = bkLogApi.logSearch(logQueryReq);

            Integer total = 0;
            if (logQueryResp != null && logQueryResp.getHits() != null) {
                total = logQueryResp.getHits().getTotal();
            }

            List<SimpleLogDTO> data = convertToSimpleLogDTOs(logQueryResp);

            return new PageData<>(total, start, size, data);
        } catch (Exception e) {
            log.error("query job log with pagination fail, source={}", source, e);
            throw new RuntimeException("query job log with pagination fail", e);
        }
    }

    /**
     * 构建LogQueryReq对象
     */
    private LogQueryReq buildLogQueryReq(BkLogProperties.LogSource logSource,
                                         String queryString,
                                         String timeRange,
                                         String startTime,
                                         String endTime,
                                         Integer start,
                                         Integer size) {
        LogQueryReq logQueryReq = new LogQueryReq();

        logQueryReq.setIndices(logSource.getIndices());
        logQueryReq.setIndexSetId(logSource.getIndexSetId());

        String startTimeStr = startTime;
        String endTimeStr = endTime;

        // 优先使用timeRange，转化为毫秒时间戳的startTime和endTime
        if (StringUtils.isNotBlank(timeRange)) {
            Pair<Long, Long> startAndEndTime = TimeUtils.getTimesByRelativeTimeFromNow(timeRange);
            startTimeStr = String.valueOf(startAndEndTime.getLeft());
            endTimeStr = String.valueOf(startAndEndTime.getRight());
        }

        // 日志平台openAPI接口只能识别预定义对几个值，所以使用更通用的startTime和endTime
        logQueryReq.setStartTime(startTimeStr);
        logQueryReq.setEndTime(endTimeStr);
        logQueryReq.setUseTimeRange(true);
        logQueryReq.setQueryString(queryString);
        
        // 设置分页参数
        logQueryReq.setStart(start);
        logQueryReq.setSize(size);

        return logQueryReq;
    }

    /**
     * 添加排序字段，默认降序
     *
     * @param logQueryReq 请求
     * @param sortField   排序字段
     * @param orderByAsc  是否升序
     */
    private void fillReqWithSortField(LogQueryReq logQueryReq, String sortField, Boolean orderByAsc) {
        List<List<String>> sorts = new ArrayList<>();
        String orderStr = Boolean.TRUE.equals(orderByAsc) ? "asc" : "desc";
        if (StringUtils.isBlank(sortField)) {
            sortField = "log_time";
        }
        sorts.add(List.of(sortField, orderStr));
        logQueryReq.setSortList(sorts);
    }

    /**
     * 将LogQueryResp转换为SimpleLogDTO列表
     */
    private List<SimpleLogDTO> convertToSimpleLogDTOs(LogQueryResp logQueryResp) {
        List<SimpleLogDTO> simpleLogs = new ArrayList<>();

        if (logQueryResp == null || logQueryResp.getHits() == null ||
            logQueryResp.getHits().getHits() == null) {
            return simpleLogs;
        }

        for (LogQueryResp.Hit hit : logQueryResp.getHits().getHits()) {
            if (hit.getSource() != null) {
                SimpleLogDTO simpleLog = extract(hit);
                simpleLogs.add(simpleLog);
            }
        }

        return simpleLogs;
    }

    private static SimpleLogDTO extract(LogQueryResp.Hit hit) {
        SimpleLogDTO simpleLog = new SimpleLogDTO();
        simpleLog.setLog(hit.getSource().getLog());
        simpleLog.setDtEventTimeStamp(hit.getSource().getDtEventTimeStamp());
        simpleLog.setPath(hit.getSource().getPath());
        simpleLog.setServerIp(hit.getSource().getServerIp());
        simpleLog.setLevel(hit.getSource().getLevel());
        simpleLog.setRequestId(hit.getSource().getRequestId());
        simpleLog.setService(hit.getSource().getService());
        simpleLog.setContent(hit.getSource().getContent());
        simpleLog.setTime(hit.getSource().getTime());
        return simpleLog;
    }

}
