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

package com.tencent.bk.job.manage.api.esb.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.EsbGetJobListResource;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.esb.EsbJobBasicInfoDTO;
import com.tencent.bk.job.manage.model.esb.request.EsbGetJobListRequest;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobListResourceImpl implements EsbGetJobListResource {
    private final TaskPlanService taskPlanService;
    private final BusinessAuthService businessAuthService;

    public EsbGetJobListResourceImpl(TaskPlanService taskPlanService,
                                     BusinessAuthService businessAuthService) {
        this.taskPlanService = taskPlanService;
        this.businessAuthService = businessAuthService;
    }


    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_job_list"})
    public EsbResp<List<EsbJobBasicInfoDTO>> getJobList(EsbGetJobListRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get job list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }
        long appId = request.getAppId();

        // TODO: 通过scopeType与scopeId构造AppResourceScope
        AuthResult authResult =
            businessAuthService.authAccessBusiness(request.getUserName(), new AppResourceScope(request.getAppId()));
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        TaskPlanQueryDTO taskPlanQueryDTO = new TaskPlanQueryDTO();
        taskPlanQueryDTO.setAppId(appId);
        taskPlanQueryDTO.setName(request.getName());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        if (request.getStart() != null) {
            baseSearchCondition.setStart(request.getStart());
        } else {
            baseSearchCondition.setStart(0);
        }
        if (request.getLength() != null) {
            baseSearchCondition.setLength(request.getLength());
        } else {
            baseSearchCondition.setStart(-1);
            baseSearchCondition.setLength(-1);
        }
        baseSearchCondition.setCreator(request.getCreator());
        baseSearchCondition.setLastModifyUser(request.getLastModifyUser());

        if (StringUtils.isNotEmpty(request.getCreateTimeStart())) {
            baseSearchCondition.setCreateTimeStart(
                DateUtils.convertUnixTimestampFromDateTimeStr(request.getCreateTimeStart(), "yyyy-MM-dd",
                    ChronoUnit.SECONDS, ZoneId.systemDefault()));
        }
        if (StringUtils.isNotEmpty(request.getCreateTimeEnd())) {
            baseSearchCondition.setCreateTimeEnd(
                DateUtils.convertUnixTimestampFromDateTimeStr(request.getCreateTimeEnd(), "yyyy-MM-dd",
                    ChronoUnit.SECONDS, ZoneId.systemDefault()));
        }
        if (StringUtils.isNotEmpty(request.getLastModifyTimeStart())) {
            baseSearchCondition.setLastModifyTimeStart(
                DateUtils.convertUnixTimestampFromDateTimeStr(request.getLastModifyTimeStart(), "yyyy-MM-dd",
                    ChronoUnit.SECONDS, ZoneId.systemDefault()));
        }
        if (StringUtils.isNotEmpty(request.getLastModifyTimeEnd())) {
            baseSearchCondition.setLastModifyTimeEnd(
                DateUtils.convertUnixTimestampFromDateTimeStr(request.getLastModifyTimeEnd(), "yyyy-MM-dd",
                    ChronoUnit.SECONDS, ZoneId.systemDefault()));
        }

        PageData<TaskPlanInfoDTO> pageTaskPlans = taskPlanService.listPageTaskPlansBasicInfo(taskPlanQueryDTO,
            baseSearchCondition, null);
        if (pageTaskPlans == null || pageTaskPlans.getData() == null || pageTaskPlans.getData().isEmpty()) {
            return EsbResp.buildSuccessResp(Collections.emptyList());
        }
        return EsbResp.buildSuccessResp(convertToEsbJobBasicInfoDTO(pageTaskPlans.getData()));
    }

    private ValidateResult checkRequest(EsbGetJobListRequest request) {
        if (request.getAppId() == null || request.getAppId() < 1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        // TODO 暂不校验，后面补上
        return ValidateResult.pass();
    }

    private List<EsbJobBasicInfoDTO> convertToEsbJobBasicInfoDTO(List<TaskPlanInfoDTO> taskPlans) {
        List<EsbJobBasicInfoDTO> results = new ArrayList<>(taskPlans.size());
        for (TaskPlanInfoDTO taskPlan : taskPlans) {
            EsbJobBasicInfoDTO result = new EsbJobBasicInfoDTO();
            result.setAppId(taskPlan.getAppId());
            result.setCreator(taskPlan.getCreator());
            result.setLastModifyUser(taskPlan.getLastModifyUser());
            result.setCreateTime(DateUtils.formatUnixTimestamp(taskPlan.getCreateTime(),
                ChronoUnit.SECONDS, "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault()));
            result.setId(taskPlan.getId());
            result.setName(taskPlan.getName());
            if (taskPlan.getLastModifyTime() != null) {
                result.setLastModifyTime(DateUtils.formatUnixTimestamp(taskPlan.getLastModifyTime(),
                    ChronoUnit.SECONDS, "yyyy-MM-dd HH:mm:ss", ZoneId.systemDefault()));
            }
            results.add(result);
        }
        return results;
    }
}
