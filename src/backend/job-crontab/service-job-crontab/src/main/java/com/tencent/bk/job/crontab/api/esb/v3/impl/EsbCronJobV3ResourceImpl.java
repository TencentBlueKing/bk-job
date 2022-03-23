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

package com.tencent.bk.job.crontab.api.esb.v3.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.api.common.CronCheckUtil;
import com.tencent.bk.job.crontab.api.esb.v3.EsbCronJobV3Resource;
import com.tencent.bk.job.crontab.auth.CronAuthService;
import com.tencent.bk.job.crontab.client.ServiceTaskPlanResourceClient;
import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobVariableDTO;
import com.tencent.bk.job.crontab.model.esb.v3.request.EsbGetCronDetailV3Request;
import com.tencent.bk.job.crontab.model.esb.v3.request.EsbGetCronListV3Request;
import com.tencent.bk.job.crontab.model.esb.v3.request.EsbSaveCronV3Request;
import com.tencent.bk.job.crontab.model.esb.v3.request.EsbUpdateCronStatusV3Request;
import com.tencent.bk.job.crontab.model.esb.v3.response.EsbCronInfoV3DTO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 26/2/2020 21:41
 */
@Slf4j
@RestController
public class EsbCronJobV3ResourceImpl implements EsbCronJobV3Resource {

    private final CronJobService cronJobService;
    private final CronAuthService cronAuthService;
    private final ServiceTaskPlanResourceClient taskPlanResource;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbCronJobV3ResourceImpl(CronJobService cronJobService,
                                    CronAuthService cronAuthService,
                                    ServiceTaskPlanResourceClient taskPlanResource,
                                    AppScopeMappingService appScopeMappingService) {
        this.cronJobService = cronJobService;
        this.cronAuthService = cronAuthService;
        this.taskPlanResource = taskPlanResource;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbCronInfoV3DTO>> getCronList(String username,
                                                                String appCode,
                                                                Long bkBizId,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long id,
                                                                String creator,
                                                                String name,
                                                                Integer status,
                                                                Long createTimeStart,
                                                                Long createTimeEnd,
                                                                String lastModifyUser,
                                                                Long lastModifyTimeStart,
                                                                Long lastModifyTimeEnd,
                                                                Integer start,
                                                                Integer length) {
        EsbGetCronListV3Request request = new EsbGetCronListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBkBizId(bkBizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setId(id);
        request.setCreator(creator);
        request.setName(name);
        request.setStatus(status);
        request.setCreateTimeStart(createTimeStart);
        request.setLastModifyUser(lastModifyUser);
        request.setCreateTimeEnd(createTimeEnd);
        request.setLastModifyTimeEnd(lastModifyTimeEnd);
        request.setLastModifyTimeStart(lastModifyTimeStart);
        request.setStart(start);
        request.setLength(length);
        return getCronListUsingPost(request);
    }

    @Override
    public EsbResp<EsbCronInfoV3DTO> getCronDetail(String username,
                                                   String appCode,
                                                   Long bkBizId,
                                                   String scopeType,
                                                   String scopeId,
                                                   Long id) {
        EsbGetCronDetailV3Request request = new EsbGetCronDetailV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBkBizId(bkBizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setId(id);
        return getCronDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_cron_list"})
    public EsbResp<EsbPageDataV3<EsbCronInfoV3DTO>> getCronListUsingPost(EsbGetCronListV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        if (request.validate()) {
            if (request.getId() != null && request.getId() > 0) {
                CronJobInfoDTO cronJobInfoById = cronJobService.getCronJobInfoById(request.getAppId(), request.getId());
                cronJobInfoById.setVariableValue(null);
                List<EsbCronInfoV3DTO> data = Collections
                    .singletonList(CronJobInfoDTO.toEsbCronInfoV3(cronJobInfoById));
                EsbPageDataV3<EsbCronInfoV3DTO> esbPageDataV3 = new EsbPageDataV3<>();
                esbPageDataV3.setTotal(1L);
                esbPageDataV3.setStart(request.getStart());
                esbPageDataV3.setLength(request.getLength());
                if (request.getStart() == 0) {
                    esbPageDataV3.setData(data);
                } else {
                    esbPageDataV3.setData(Collections.emptyList());
                }
                return EsbResp.buildSuccessResp(esbPageDataV3);
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
                if (request.getCreateTimeStart() != null && request.getCreateTimeEnd() != null) {
                    baseSearchCondition.setCreateTimeStart(request.getCreateTimeStart());
                    baseSearchCondition.setCreateTimeEnd(request.getCreateTimeEnd());
                }
                if (request.getLastModifyTimeStart() != null && request.getLastModifyTimeEnd() != null) {
                    baseSearchCondition.setLastModifyTimeStart(request.getLastModifyTimeStart());
                    baseSearchCondition.setLastModifyTimeEnd(request.getLastModifyTimeEnd());
                }
                PageData<CronJobInfoDTO> cronJobInfoPageData = cronJobService.listPageCronJobInfos(cronJobCondition,
                    baseSearchCondition);
                List<EsbCronInfoV3DTO> cronInfoV3ResponseData = cronJobInfoPageData.getData().parallelStream()
                    .peek(cronJobInfoDTO -> cronJobInfoDTO.setVariableValue(null))
                    .map(CronJobInfoDTO::toEsbCronInfoV3).collect(Collectors.toList());
                EsbPageDataV3<EsbCronInfoV3DTO> esbPageDataV3 = new EsbPageDataV3<>();
                esbPageDataV3.setTotal(cronJobInfoPageData.getTotal());
                esbPageDataV3.setStart(cronJobInfoPageData.getStart());
                esbPageDataV3.setLength(cronJobInfoPageData.getPageSize());
                esbPageDataV3.setData(cronInfoV3ResponseData);
                return EsbResp.buildSuccessResp(esbPageDataV3);
            }
        }
        return EsbResp.buildSuccessResp(null);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_update_cron_status"})
    public EsbResp<EsbCronInfoV3DTO> updateCronStatus(EsbUpdateCronStatusV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        String username = request.getUserName();
        Long appId = request.getAppId();
        request.validate();
        AuthResult authResult = cronAuthService.authManageCron(
            request.getUserName(),
            new AppResourceScope(request.getScopeType(), request.getScopeId(), request.getAppId()),
            request.getId(),
            null
        );
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        Boolean updateResult;
        try {
            updateResult = cronJobService.changeCronJobEnableStatus(username, appId, request.getId(),
                CronStatusEnum.RUNNING.getStatus().equals(request.getStatus()));
        } catch (TaskExecuteAuthFailedException e) {
            throw new PermissionDeniedException(e.getAuthResult());
        }
        if (updateResult) {
            EsbCronInfoV3DTO esbCronInfoV3DTO = new EsbCronInfoV3DTO();
            esbCronInfoV3DTO.setId(request.getId());
            return EsbResp.buildSuccessResp(esbCronInfoV3DTO);
        }
        throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
    }

    private void checkAndFillGlobalVar(Long planId,
                                       List<EsbGlobalVarV3DTO> globalVarV3DTOList,
                                       CronJobInfoDTO cronJobInfo) {
        // 校验id/name，解析id
        for (EsbGlobalVarV3DTO esbGlobalVarV3DTO : globalVarV3DTOList) {
            Long id = esbGlobalVarV3DTO.getId();
            String name = esbGlobalVarV3DTO.getName();
            if (id == null && StringUtils.isBlank(name)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_REASON,
                    new String[]{"id/name of globalVar cannot be null/blank at the same time"});
            }
            ServiceTaskVariableDTO taskVariableDTO;
            if (id == null) {
                // 根据name解析id
                InternalResponse<ServiceTaskVariableDTO> resp = taskPlanResource
                    .getGlobalVarByName(planId, name);
                if (!resp.isSuccess()) {
                    throw new InternalException(resp.getCode());
                }
                taskVariableDTO = resp.getData();
            } else {
                // 根据id解析name，无论是否传入name都根据id解析name作为正确值
                InternalResponse<ServiceTaskVariableDTO> resp = taskPlanResource
                    .getGlobalVarById(planId, id);
                if (!resp.isSuccess()) {
                    throw new InternalException(resp.getCode());
                }
                taskVariableDTO = resp.getData();
                if (!StringUtils.isBlank(name) && !name.equals(resp.getData().getName())) {
                    log.info("Ignore given name {}, use name {} parsed by id", name, resp.getData());
                }
            }
            esbGlobalVarV3DTO.setId(taskVariableDTO.getId());
            esbGlobalVarV3DTO.setName(taskVariableDTO.getName());
            esbGlobalVarV3DTO.setType(taskVariableDTO.getType());
        }
        cronJobInfo.setVariableValue(globalVarV3DTOList.parallelStream().map(globalVarV3DTO -> {
            CronJobVariableDTO cronJobVariableDTO = new CronJobVariableDTO();
            cronJobVariableDTO.setId(globalVarV3DTO.getId());
            cronJobVariableDTO.setName(globalVarV3DTO.getName());
            cronJobVariableDTO.setType(TaskVariableTypeEnum.valOf(globalVarV3DTO.getType()));
            cronJobVariableDTO.setValue(globalVarV3DTO.getValue());
            cronJobVariableDTO.setServer(ServerDTO.fromEsbServerV3(globalVarV3DTO.getServer()));
            return cronJobVariableDTO;
        }).collect(Collectors.toList()));
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_save_cron"})
    public EsbResp<EsbCronInfoV3DTO> saveCron(EsbSaveCronV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        EsbCronInfoV3DTO esbCronInfoV3DTO = new EsbCronInfoV3DTO();
        esbCronInfoV3DTO.setId(0L);
        checkRequest(request);
        Long appId = request.getAppId();
        AuthResult authResult;
        if (request.getId() != null && request.getId() > 0) {
            authResult = cronAuthService.authManageCron(
                request.getUserName(),
                request.getAppResourceScope(),
                request.getId(),
                null
            );
        } else {
            authResult = cronAuthService.authCreateCron(
                request.getUserName(),
                request.getAppResourceScope()
            );
        }
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
        cronJobInfo.setId(request.getId());
        cronJobInfo.setAppId(request.getAppId());
        cronJobInfo.setName(request.getName());
        cronJobInfo.setTaskPlanId(request.getPlanId());
        cronJobInfo.setCronExpression(CronExpressionUtil.fixExpressionForQuartz(request.getCronExpression()));
        cronJobInfo.setExecuteTime(request.getExecuteTime());
        List<EsbGlobalVarV3DTO> globalVarV3DTOList = request.getGlobalVarList();
        if (globalVarV3DTOList != null) {
            checkAndFillGlobalVar(request.getPlanId(), globalVarV3DTOList, cronJobInfo);
        }
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
            esbCronInfoV3DTO = CronJobInfoDTO.toEsbCronInfoV3Response(cronJobService.getCronJobInfoById(cronId));
            return EsbResp.buildSuccessResp(esbCronInfoV3DTO);
        } else {
            throw new InternalException(ErrorCode.UPDATE_CRON_JOB_FAILED);
        }
    }

    private void checkRequest(EsbSaveCronV3Request request) {
        // 定时任务表达式有效性校验
        if (StringUtils.isNotBlank(request.getCronExpression())) {
            CronCheckUtil.checkCronExpression(request.getCronExpression(), "expression");
        }
    }

    @Override
    public EsbResp<EsbCronInfoV3DTO> getCronDetailUsingPost(EsbGetCronDetailV3Request request) {
        request.fillAppResourceScope(appScopeMappingService);
        if (request.validate()) {
            CronJobInfoDTO cronJobInfoById = cronJobService.getCronJobInfoById(request.getAppId(), request.getId());
            return EsbResp.buildSuccessResp(CronJobInfoDTO.toEsbCronInfoV3(cronJobInfoById));
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
