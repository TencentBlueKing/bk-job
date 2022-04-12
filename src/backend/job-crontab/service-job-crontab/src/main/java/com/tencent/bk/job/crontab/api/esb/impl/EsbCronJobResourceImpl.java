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

package com.tencent.bk.job.crontab.api.esb.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.api.common.CronCheckUtil;
import com.tencent.bk.job.crontab.api.esb.EsbCronJobResource;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.esb.request.EsbGetCronListRequest;
import com.tencent.bk.job.crontab.model.esb.request.EsbSaveCronRequest;
import com.tencent.bk.job.crontab.model.esb.request.EsbUpdateCronStatusRequest;
import com.tencent.bk.job.crontab.model.esb.response.EsbCronInfoResponse;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 26/2/2020 21:41
 */
@Slf4j
@RestController
public class EsbCronJobResourceImpl implements EsbCronJobResource {

    private final CronJobService cronJobService;
    private final CronAuthService cronAuthService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbCronJobResourceImpl(CronJobService cronJobService,
                                  CronAuthService cronAuthService,
                                  AppScopeMappingService appScopeMappingService) {
        this.cronJobService = cronJobService;
        this.cronAuthService = cronAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_cron_list"})
    public EsbResp<List<EsbCronInfoResponse>> getCronList(EsbGetCronListRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        if (request.validate()) {
            if (request.getId() != null && request.getId() > 0) {
                CronJobInfoDTO cronJobInfoById = cronJobService.getCronJobInfoById(request.getAppId(), request.getId());
                return EsbResp
                    .buildSuccessResp(Collections.singletonList(CronJobInfoDTO.toEsbCronInfo(cronJobInfoById)));
            } else {
                CronJobInfoDTO cronJobCondition = new CronJobInfoDTO();
                cronJobCondition.setAppId(request.getAppId());
                BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                baseSearchCondition.setStart(0);
                baseSearchCondition.setLength(10);
                cronJobCondition.setName(request.getName());
                if (request.getStatus() != null) {
                    cronJobCondition.setEnable(request.getStatus() == 1);
                }

                baseSearchCondition.setStart(request.getStart());
                baseSearchCondition.setLength(request.getLength());
                baseSearchCondition.setCreator(request.getCreator());
                baseSearchCondition.setLastModifyUser(request.getLastModifyUser());
                if (StringUtils.isNotEmpty(request.getCreateTimeStart())) {
                    baseSearchCondition.setCreateTimeStart(DateUtils.convertUnixTimestampFromDateTimeStr(
                        request.getCreateTimeStart(), "yyyy-MM-dd", ChronoUnit.SECONDS, ZoneId.systemDefault()));
                }
                if (StringUtils.isNotEmpty(request.getCreateTimeEnd())) {
                    baseSearchCondition.setCreateTimeEnd(DateUtils.convertUnixTimestampFromDateTimeStr(
                        request.getCreateTimeEnd(), "yyyy-MM-dd", ChronoUnit.SECONDS, ZoneId.systemDefault()));
                }
                if (StringUtils.isNotEmpty(request.getLastModifyTimeStart())) {
                    baseSearchCondition.setLastModifyTimeStart(DateUtils.convertUnixTimestampFromDateTimeStr(
                        request.getLastModifyTimeStart(), "yyyy-MM-dd", ChronoUnit.SECONDS, ZoneId.systemDefault()));
                }
                if (StringUtils.isNotEmpty(request.getLastModifyTimeEnd())) {
                    baseSearchCondition.setLastModifyTimeEnd(DateUtils.convertUnixTimestampFromDateTimeStr(
                        request.getLastModifyTimeEnd(), "yyyy-MM-dd", ChronoUnit.SECONDS, ZoneId.systemDefault()));
                }
                PageData<CronJobInfoDTO> cronJobInfoPageData =
                    cronJobService.listPageCronJobInfos(cronJobCondition, baseSearchCondition);
                return EsbResp.buildSuccessResp(cronJobInfoPageData.getData().parallelStream()
                    .map(CronJobInfoDTO::toEsbCronInfo).collect(Collectors.toList()));
            }
        }
        return EsbResp.buildSuccessResp(null);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_update_cron_status"})
    public EsbResp<EsbCronInfoResponse> updateCronStatus(EsbUpdateCronStatusRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        String username = request.getUserName();
        Long appId = request.getAppId();
        if (request.validate()) {

            AuthResult authResult = cronAuthService.authManageCron(
                request.getUserName(), request.getAppResourceScope(), request.getId(), null
            );
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }

            Boolean updateResult;
            try {
                updateResult = cronJobService.changeCronJobEnableStatus(username, appId, request.getId(),
                    request.getStatus() == 1);
            } catch (TaskExecuteAuthFailedException e) {
                throw new PermissionDeniedException(e.getAuthResult());
            }
            if (updateResult) {
                EsbCronInfoResponse esbCronInfoResponse = new EsbCronInfoResponse();
                esbCronInfoResponse.setId(request.getId());
                return EsbResp.buildSuccessResp(esbCronInfoResponse);
            }
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
    }

    private void checkRequest(EsbSaveCronRequest request) {
        if (request == null) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                new Object[]{
                    "Request body cannot be null"
                });
        }
        request.validate();
        if (StringUtils.isNotBlank(request.getCronExpression())) {
            CronCheckUtil.checkCronExpression(request.getCronExpression(), "expression");
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_save_cron"})
    public EsbResp<EsbCronInfoResponse> saveCron(EsbSaveCronRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        EsbCronInfoResponse esbCronInfoResponse = new EsbCronInfoResponse();
        esbCronInfoResponse.setId(0L);
        checkRequest(request);
        AuthResult authResult;
        if (request.getId() != null && request.getId() > 0) {
            authResult = cronAuthService.authManageCron(
                request.getUserName(), request.getAppResourceScope(), request.getId(), null
            );
        } else {
            authResult = cronAuthService.authCreateCron(request.getUserName(), request.getAppResourceScope());
        }
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        cronJobInfo.setId(request.getId());
        cronJobInfo.setAppId(request.getAppId());
        cronJobInfo.setName(request.getName());
        cronJobInfo.setTaskPlanId(request.getPlanId());
        cronJobInfo.setCronExpression(CronExpressionUtil.fixExpressionForQuartz(request.getCronExpression()));
        if (cronJobInfo.getId() == null || cronJobInfo.getId() == 0) {
            cronJobInfo.setCreator(request.getUserName());
            cronJobInfo.setDelete(false);
        }
        cronJobInfo.setEnable(false);
        cronJobInfo.setLastModifyUser(request.getUserName());
        cronJobInfo.setLastModifyTime(DateUtils.currentTimeSeconds());

        Long cronId;
        try {
            cronId = cronJobService.saveCronJobInfo(cronJobInfo);
        } catch (TaskExecuteAuthFailedException e) {
            throw new PermissionDeniedException(e.getAuthResult());
        }
        if (cronId > 0) {
            esbCronInfoResponse = CronJobInfoDTO.toEsbCronInfo(cronJobService.getCronJobInfoById(cronId));
            return EsbResp.buildSuccessResp(esbCronInfoResponse);
        } else {
            throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
        }
    }
}
