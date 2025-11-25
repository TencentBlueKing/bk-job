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
import lombok.extern.slf4j.Slf4j;
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
    public PageData<SimpleLogDTO> queryLogs(String queryString,
                                            String timeRange,
                                            String startTime,
                                            String endTime,
                                            Integer start,
                                            Integer size) {
        try {
            LogQueryReq logQueryReq = buildLogQueryReq(queryString, timeRange, startTime, endTime, start, size);
            // 按照时间升序
            fillReqWithTimeSortAsc(logQueryReq);
            
            LogQueryResp logQueryResp = bkLogApi.logSearch(logQueryReq);

            // 总记录数
            Integer total = 0;
            if (logQueryResp != null && logQueryResp.getHits() != null) {
                total = logQueryResp.getHits().getTotal();
            }
            
            List<SimpleLogDTO> data = convertToSimpleLogDTOs(logQueryResp);
            
            return new PageData<>(total, start, size, data);
        } catch (Exception e) {
            log.error("query job log with pagination fail ", e);
            throw new RuntimeException("query job log with pagination fail: " + e.getMessage(), e);
        }
    }

    /**
     * 构建LogQueryReq对象
     */
    private LogQueryReq buildLogQueryReq(String queryString,
                                         String timeRange,
                                         String startTime,
                                         String endTime,
                                         Integer start,
                                         Integer size) {
        LogQueryReq logQueryReq = new LogQueryReq();
        
        // 查job的日志，从配置文件读取索引相关信息
        logQueryReq.setIndices(bkLogProperties.getIndices());
        logQueryReq.setIndexSetId(bkLogProperties.getIndexSetId());

        logQueryReq.setStartTime(startTime);
        logQueryReq.setEndTime(endTime);
        logQueryReq.setUseTimeRange(startTime != null && endTime != null);
        logQueryReq.setTimeRange(timeRange);
        logQueryReq.setQueryString(queryString);
        
        // 设置分页参数
        logQueryReq.setStart(start);
        logQueryReq.setSize(size);

        return logQueryReq;
    }

    private void fillReqWithTimeSortAsc(LogQueryReq logQueryReq) {
        List<List<String>> sorts = new ArrayList<>();
        sorts.add(List.of("dtEventTimeStamp", "asc"));
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
