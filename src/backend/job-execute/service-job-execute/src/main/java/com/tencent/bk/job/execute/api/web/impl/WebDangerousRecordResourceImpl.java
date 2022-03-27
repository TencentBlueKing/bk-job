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

package com.tencent.bk.job.execute.api.web.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.api.web.WebDangerousRecordResource;
import com.tencent.bk.job.execute.model.DangerousRecordDTO;
import com.tencent.bk.job.execute.model.web.vo.DangerousRecordVO;
import com.tencent.bk.job.execute.service.DangerousRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebDangerousRecordResourceImpl implements WebDangerousRecordResource {
    private final DangerousRecordService dangerousRecordService;
    private final AppScopeMappingService appScopeMappingService;

    public WebDangerousRecordResourceImpl(DangerousRecordService dangerousRecordService,
                                          AppScopeMappingService appScopeMappingService) {
        this.dangerousRecordService = dangerousRecordService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public Response<PageData<DangerousRecordVO>> pageListDangerousRecords(String username,
                                                                          Long id,
                                                                          String scopeType,
                                                                          String scopeId,
                                                                          Long ruleId,
                                                                          String ruleExpression,
                                                                          String startTime,
                                                                          String endTime,
                                                                          Integer start,
                                                                          Integer pageSize,
                                                                          Integer startupMode,
                                                                          Integer mode,
                                                                          String operator,
                                                                          String client) {
        DangerousRecordDTO query = new DangerousRecordDTO();
        query.setId(id);
        query.setRuleId(ruleId);
        query.setRuleExpression(ruleExpression);
        query.setStartupMode(startupMode);
        query.setAction(mode);
        query.setOperator(operator);
        query.setClient(client);
        if (StringUtils.isNotEmpty(scopeType) && StringUtils.isNotEmpty(scopeId)) {
            query.setAppId(appScopeMappingService.getAppIdByScope(scopeType, scopeId));
        }

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (StringUtils.isNotBlank(startTime)) {
            baseSearchCondition.setCreateTimeStart(DateUtils.convertUnixTimestampFromDateTimeStr(startTime,
                "yyyy-MM-dd HH:mm:ss", ChronoUnit.MILLIS, ZoneId.systemDefault()));
        }
        if (StringUtils.isNotBlank(endTime)) {
            baseSearchCondition.setCreateTimeEnd(DateUtils.convertUnixTimestampFromDateTimeStr(endTime,
                "yyyy-MM-dd HH:mm:ss", ChronoUnit.MILLIS, ZoneId.systemDefault()));
        }
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);

        PageData<DangerousRecordDTO> pageData = dangerousRecordService.listPageDangerousRecord(query,
            baseSearchCondition);

        PageData<DangerousRecordVO> pageDataVO = new PageData<>();
        pageDataVO.setStart(pageData.getStart());
        pageDataVO.setTotal(pageData.getTotal());
        pageDataVO.setPageSize(pageData.getPageSize());
        if (CollectionUtils.isNotEmpty(pageData.getData())) {
            pageDataVO.setData(pageData.getData().stream().map(DangerousRecordDTO::toDangerousRecordVO)
                .collect(Collectors.toList()));
        }
        return Response.buildSuccessResp(pageDataVO);
    }
}
