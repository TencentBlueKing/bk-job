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

package com.tencent.bk.job.crontab.api.web.impl;

import com.google.common.base.CaseFormat;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.check.*;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.common.web.controller.AbstractJobController;
import com.tencent.bk.job.crontab.api.web.WebCronJobResource;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.exception.TaskExecuteAuthFailedException;
import com.tencent.bk.job.crontab.model.BatchUpdateCronJobReq;
import com.tencent.bk.job.crontab.model.CronJobCreateUpdateReq;
import com.tencent.bk.job.crontab.model.CronJobLaunchHistoryVO;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobLaunchResultStatistics;
import com.tencent.bk.job.crontab.service.CronJobHistoryService;
import com.tencent.bk.job.crontab.service.CronJobService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.model.inner.CronTaskExecuteResult;
import com.tencent.bk.job.execute.model.inner.ServiceCronTaskExecuteResultStatistics;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.util.PathBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 31/12/2019 16:36
 */
@Slf4j
@RestController
public class WebCronJobResourceImpl extends AbstractJobController implements WebCronJobResource {

    private final CronJobService cronJobService;
    private final CronJobHistoryService cronJobHistoryService;
    private final MessageI18nService i18nService;
    private final WebAuthService authService;

    @Autowired
    public WebCronJobResourceImpl(CronJobService cronJobService, CronJobHistoryService cronJobHistoryService,
                                  MessageI18nService i18nService,
                                  WebAuthService authService) {
        this.cronJobService = cronJobService;
        this.cronJobHistoryService = cronJobHistoryService;
        this.i18nService = i18nService;
        this.authService = authService;
    }

    @Override
    public ServiceResponse<PageData<CronJobVO>> listCronJobs(
        String username,
        Long appId,
        Long cronJobId,
        Long planId,
        String name,
        String creator,
        String lastModifyUser,
        Integer start,
        Integer pageSize,
        String orderField,
        Integer order
    ) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        CronJobInfoDTO cronJobCondition = new CronJobInfoDTO();
        cronJobCondition.setAppId(appId);
        cronJobCondition.setName(name);
        cronJobCondition.setId(cronJobId);
        if (planId != null && planId > 0) {
            cronJobCondition.setTaskPlanId(planId);
        }

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);
        if (StringUtils.isNoneBlank(creator)) {
            baseSearchCondition.setCreator(creator);
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            baseSearchCondition.setLastModifyUser(lastModifyUser);
        }
        if (StringUtils.isNotBlank(orderField)) {
            baseSearchCondition.setOrderField(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, orderField));
        }
        baseSearchCondition.setOrder(order);
        PageData<CronJobInfoDTO> cronJobInfoPageData =
            cronJobService.listPageCronJobInfos(cronJobCondition, baseSearchCondition);
        List<CronJobVO> resultCronJobs = new ArrayList<>();
        if (cronJobInfoPageData != null) {
            cronJobInfoPageData.getData().forEach(cronJobInfo -> resultCronJobs.add(CronJobInfoDTO.toVO(cronJobInfo)));
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.CRON_JOB_NOT_EXIST, i18nService);
        }

        PageData<CronJobVO> resultPageData = new PageData<>();
        resultPageData.setStart(cronJobInfoPageData.getStart());
        resultPageData.setPageSize(cronJobInfoPageData.getPageSize());
        resultPageData.setTotal(cronJobInfoPageData.getTotal());
        resultPageData.setData(resultCronJobs);
        resultPageData.setExistAny(cronJobService.isExistAnyAppCronJob(appId));

        processCronJobPermission(cronJobCondition.getAppId(), resultPageData);

        ServiceResponse<PageData<CronJobVO>> resp = ServiceResponse.buildSuccessResp(resultPageData);

        return resp;
    }

    @Override
    public ServiceResponse<List<CronJobVO>> listCronJobStatistic(String username, Long appId, List<Long> cronJobId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<CronJobInfoDTO> cronJobInfoList = cronJobService.listCronJobByIds(appId, cronJobId);
        List<CronJobVO> resultCronJobs = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(cronJobInfoList)) {
            cronJobInfoList.forEach(cronJobInfo -> {
                CronJobVO cronJobVO = CronJobInfoDTO.toBasicVO(cronJobInfo);
                cronJobVO.setVariableValue(Collections.emptyList());
                resultCronJobs.add(cronJobVO);
            });
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.CRON_JOB_NOT_EXIST, i18nService);
        }

        processCronExecuteHistory(appId, resultCronJobs);

        return ServiceResponse.buildSuccessResp(resultCronJobs);
    }

    private void processCronJobPermission(Long appId, PageData<CronJobVO> resultPageData) {

        resultPageData.setCanCreate(
            authService.auth(false, JobContextUtil.getUsername(), ActionId.CREATE_CRON,
                ResourceTypeEnum.BUSINESS, appId.toString(), null).isPass());

        processCronJobPermission(appId, resultPageData.getData());
    }

    private void processCronJobPermission(Long appId, List<CronJobVO> cronJobList) {
        List<String> cronJobIdList = new ArrayList<>();
        cronJobList.forEach(cronJob -> cronJobIdList.add(cronJob.getId().toString()));
        List<Long> allowedCronJob = authService
            .batchAuth(JobContextUtil.getUsername(), ActionId.MANAGE_CRON, appId, ResourceTypeEnum.CRON, cronJobIdList)
            .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        cronJobList.forEach(cronJob -> {
            cronJob.setCanManage(allowedCronJob.contains(cronJob.getId()));
            if (!cronJob.getCanManage()) {
                cronJob.setVariableValue(null);
            }
        });
    }

    private void processCronExecuteHistory(Long appId, List<CronJobVO> resultCronJobs) {
        if (CollectionUtils.isEmpty(resultCronJobs)) {
            return;
        }
        Map<Long, ServiceCronTaskExecuteResultStatistics> cronJobExecuteHistory = null;
        Map<Long, CronJobLaunchResultStatistics> cronJobLaunchHistory = null;
        try {
            List<Long> cronJobIdList =
                resultCronJobs.parallelStream().map(CronJobVO::getId).collect(Collectors.toList());
            cronJobExecuteHistory = cronJobService.getCronJobExecuteHistory(appId, cronJobIdList);
            cronJobLaunchHistory = cronJobHistoryService.getCronTaskLaunchResultStatistics(appId, cronJobIdList);
        } catch (Exception e) {
            log.error("Error while processing cron history!|{}", appId, e);
            return;
        }
        if (MapUtils.isEmpty(cronJobExecuteHistory) && MapUtils.isEmpty(cronJobLaunchHistory)) {
            return;
        }
        for (CronJobVO resultCronJob : resultCronJobs) {
            ServiceCronTaskExecuteResultStatistics cronTaskExecuteResult = null;
            if (cronJobExecuteHistory != null) {
                cronTaskExecuteResult = cronJobExecuteHistory.get(resultCronJob.getId());
            }

            CronJobLaunchResultStatistics launchHistory = null;
            if (cronJobLaunchHistory != null) {
                launchHistory = cronJobLaunchHistory.get(resultCronJob.getId());
            }

            if (cronTaskExecuteResult == null && launchHistory == null) {
                continue;
            }
            fillStatisticInfo(resultCronJob, cronTaskExecuteResult, launchHistory);
        }
    }

    private void fillStatisticInfo(
        CronJobVO resultCronJob,
        ServiceCronTaskExecuteResultStatistics cronTaskExecuteResult,
        CronJobLaunchResultStatistics launchHistory
    ) {
        List<Long> lastFailTimeList = new ArrayList<>();
        resultCronJob.setTotalCount(0);
        resultCronJob.setFailCount(0);

        if (cronTaskExecuteResult != null) {
            List<CronTaskExecuteResult> lastExecuteRecords = cronTaskExecuteResult.getLast24HourExecuteRecords();
            if (CollectionUtils.isEmpty(lastExecuteRecords)) {
                lastExecuteRecords = cronTaskExecuteResult.getLast10ExecuteRecords();
            }

            if (CollectionUtils.isNotEmpty(lastExecuteRecords)) {
                int total = 0;
                int success = 0;
                int fail = 0;
                for (CronTaskExecuteResult executeRecord : lastExecuteRecords) {
                    RunStatusEnum taskStatus = RunStatusEnum.valueOf(executeRecord.getStatus());
                    if (taskStatus != null) {
                        switch (taskStatus) {
                            // 跳过
                            case SKIPPED:
                                // 忽略错误
                            case IGNORE_ERROR:
                                // 手动结束
                            case TERMINATED:
                                // 强制终止成功
                            case STOP_SUCCESS:
                                // 执行成功
                            case SUCCESS:
                                total += 1;
                                success += 1;
                                if (resultCronJob.getLastExecuteStatus() == 0) {
                                    resultCronJob.setLastExecuteStatus(1);
                                }
                                break;
                            // 执行失败
                            case FAIL:
                                total += 1;
                                fail += 1;
                                lastFailTimeList.add(executeRecord.getExecuteTime());
                                if (resultCronJob.getLastExecuteStatus() == 0) {
                                    resultCronJob.setLastExecuteStatus(2);
                                }
                                break;
                            // 状态异常
                            case ABNORMAL_STATE:
                                // 强制终止中
                            case STOPPING:
                                // 等待执行
                            case BLANK:
                                // 正在执行
                            case RUNNING:
                                // 等待用户
                            case WAITING:
                            default:
                                break;
                        }
                    }
                }
                resultCronJob.setTotalCount(total);
                resultCronJob.setFailCount(fail);
            }
        }
        fillStatisticInfoWithLaunchHistory(resultCronJob, lastFailTimeList, launchHistory);
    }

    private void fillStatisticInfoWithLaunchHistory(
        CronJobVO resultCronJob,
        List<Long> lastFailTimeList,
        CronJobLaunchResultStatistics launchHistory
    ) {
        if (launchHistory != null) {
            List<CronJobHistoryDTO> lastLaunchRecords = launchHistory.getLast24HourExecuteRecords();
            if (CollectionUtils.isEmpty(lastLaunchRecords)) {
                lastLaunchRecords = launchHistory.getLast10ExecuteRecords();
            }
            if (CollectionUtils.isNotEmpty(lastLaunchRecords)) {
                for (CronJobHistoryDTO lastLaunchRecord : lastLaunchRecords) {
                    if (ExecuteStatusEnum.FAIL.equals(lastLaunchRecord.getStatus())) {
                        resultCronJob.setFailCount(resultCronJob.getFailCount() + 1);
                        resultCronJob.setTotalCount(resultCronJob.getTotalCount() + 1);
                        lastFailTimeList.add(lastLaunchRecord.getScheduledTime());
                    }
                }
                if (ExecuteStatusEnum.FAIL.equals(lastLaunchRecords.get(0).getStatus())) {
                    resultCronJob.setLastExecuteStatus(2);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(lastFailTimeList)) {
            lastFailTimeList.sort(Collections.reverseOrder());
            if (lastFailTimeList.size() > 10) {
                lastFailTimeList = lastFailTimeList.subList(0, 10);
            }
        }
        resultCronJob.setLastFailRecord(lastFailTimeList);
        if (log.isDebugEnabled()) {
            log.debug("Fill cron execute info finished|{}", resultCronJob);
        }
    }

    @Override
    public ServiceResponse<CronJobVO> getCronJobById(String username, Long appId, Long cronJobId) {

        CronJobVO cronJobVO = CronJobInfoDTO.toVO(cronJobService.getCronJobInfoById(appId, cronJobId));

        AuthResultVO authResult = authService.auth(true, username, ActionId.MANAGE_CRON,
            ResourceTypeEnum.CRON, cronJobId.toString(), buildCronJobPathInfo(appId));
        if (authResult.isPass()) {
            return ServiceResponse.buildSuccessResp(cronJobVO);
        } else {
            return ServiceResponse.buildAuthFailResp(authResult);
        }
    }

    @Override
    public ServiceResponse<Long> saveCronJob(String username, Long appId, Long cronJobId,
                                             CronJobCreateUpdateReq cronJobCreateUpdateReq) {

        if (cronJobId > 0) {
            cronJobCreateUpdateReq.setId(cronJobId);
            AuthResultVO authResult = authService.auth(true, username, ActionId.MANAGE_CRON,
                ResourceTypeEnum.CRON, cronJobId.toString(), buildCronJobPathInfo(appId));
            if (!authResult.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResult);
            }
        } else {
            AuthResultVO authResult = authService.auth(true, username, ActionId.CREATE_CRON,
                ResourceTypeEnum.BUSINESS, appId.toString(), null);
            if (!authResult.isPass()) {
                return ServiceResponse.buildAuthFailResp(authResult);
            }
        }
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            cronJobCreateUpdateReq.setName(stringCheckHelper.checkAndGetResult(cronJobCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("Cron Job Name is invalid:", e);
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM)));
        }
        CronJobInfoDTO cronJobInfoDTO = CronJobInfoDTO.fromReq(username, appId, cronJobCreateUpdateReq);
        if (cronJobInfoDTO.validate()) {
            try {
                Long finalCronJobId = cronJobService.saveCronJobInfo(cronJobInfoDTO);
                return ServiceResponse.buildSuccessResp(finalCronJobId);
            } catch (TaskExecuteAuthFailedException e) {
                return ServiceResponse.buildAuthFailResp(e.getAuthResultVO());
            }
        } else {
            log.warn("Validate cron job failed!|{}", JobContextUtil.getDebugMessage());
            return ServiceResponse.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM,
                i18nService.getI18n(String.valueOf(ErrorCode.ILLEGAL_PARAM))
                    + JsonUtils.toJson(JobContextUtil.getDebugMessage()));
        }
    }

    @Override
    public ServiceResponse<Boolean> deleteCronJob(String username, Long appId, Long cronJobId) {
        AuthResultVO authResult = authService.auth(true, username, ActionId.MANAGE_CRON,
            ResourceTypeEnum.CRON, cronJobId.toString(), buildCronJobPathInfo(appId));
        if (authResult.isPass()) {
            return ServiceResponse.buildSuccessResp(cronJobService.deleteCronJobInfo(appId, cronJobId));
        } else {
            return ServiceResponse.buildAuthFailResp(authResult);
        }
    }

    @Override
    public ServiceResponse<Boolean> changeCronJobEnableStatus(String username, Long appId, Long cronJobId,
                                                              Boolean enable) {
        try {
            AuthResultVO authResult = authService.auth(true, username, ActionId.MANAGE_CRON,
                ResourceTypeEnum.CRON, cronJobId.toString(), buildCronJobPathInfo(appId));
            if (authResult.isPass()) {
                try {
                    return ServiceResponse
                        .buildSuccessResp(cronJobService.changeCronJobEnableStatus(username, appId, cronJobId, enable));
                } catch (TaskExecuteAuthFailedException e) {
                    return ServiceResponse.buildAuthFailResp(e.getAuthResultVO());
                }
            } else {
                return ServiceResponse.buildAuthFailResp(authResult);
            }
        } catch (ServiceException e) {
            return ServiceResponse.buildCommonFailResp(e, i18nService);
        }
    }

    private PathInfoDTO buildCronJobPathInfo(Long appId) {
        return PathBuilder.newBuilder(ResourceTypeEnum.BUSINESS.getId(), appId.toString()).build();
    }

    @Override
    public ServiceResponse<Boolean> checkCronJobName(String username, Long appId, Long cronJobId, String name) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }
        return ServiceResponse.buildSuccessResp(cronJobService.checkCronJobName(appId, cronJobId, name));
    }

    @Override
    public ServiceResponse<Boolean> batchUpdateCronJob(String username, Long appId,
                                                       BatchUpdateCronJobReq batchUpdateCronJobReq) {
        List<String> cronJobInstanceList = new ArrayList<>();
        batchUpdateCronJobReq.getCronJobInfoList()
            .forEach(cronJobCreateUpdateReq -> cronJobInstanceList.add(cronJobCreateUpdateReq.getId().toString()));
        List<Long> allowed =
            authService.batchAuth(username, ActionId.MANAGE_CRON, appId, ResourceTypeEnum.CRON, cronJobInstanceList)
                .parallelStream().map(Long::valueOf).collect(Collectors.toList());
        if (allowed.size() == cronJobInstanceList.size()) {
            return ServiceResponse.buildSuccessResp(cronJobService.batchUpdateCronJob(appId, batchUpdateCronJobReq));
        } else {
            return ServiceResponse.buildCommonFailResp(ErrorCode.API_NO_PERMISSION);
        }
    }

    @Override
    public ServiceResponse<List<CronJobVO>> getCronJobListByPlanId(String username, Long appId, Long planId) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        List<CronJobInfoDTO> cronJobInfoList = cronJobService.listCronJobByPlanId(appId, planId);
        if (CollectionUtils.isNotEmpty(cronJobInfoList)) {
            List<CronJobVO> cronJobList =
                cronJobInfoList.parallelStream().map(CronJobInfoDTO::toBasicVO).collect(Collectors.toList());
            processCronJobPermission(appId, cronJobList);
            return ServiceResponse.buildSuccessResp(cronJobList);
        } else {
            return ServiceResponse.buildSuccessResp(Collections.emptyList());
        }
    }

    @Override
    public ServiceResponse<Map<Long, List<CronJobVO>>> getCronJobListByPlanIdList(String username, Long appId,
                                                                                  List<Long> planIdList) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        Map<Long, List<CronJobInfoDTO>> cronJobInfoMap = cronJobService.listCronJobByPlanIds(appId, planIdList);
        if (MapUtils.isNotEmpty(cronJobInfoMap)) {
            Map<Long, List<CronJobVO>> cronJobMap = new HashMap<>(cronJobInfoMap.size());
            for (Map.Entry<Long, List<CronJobInfoDTO>> cronJobInfoListEntity : cronJobInfoMap.entrySet()) {
                List<CronJobInfoDTO> cronJobInfoList = cronJobInfoListEntity.getValue();
                List<CronJobVO> cronJobList =
                    cronJobInfoList.parallelStream().map(CronJobInfoDTO::toBasicVO).collect(Collectors.toList());
                processCronJobPermission(appId, cronJobList);
                cronJobMap.put(cronJobInfoListEntity.getKey(), cronJobList);
            }
            return ServiceResponse.buildSuccessResp(cronJobMap);
        } else {
            return ServiceResponse.buildSuccessResp(Collections.emptyMap());
        }
    }

    @Override
    public ServiceResponse<PageData<CronJobLaunchHistoryVO>> getCronJobLaunchHistory(String username, Long appId,
                                                                                     Long cronJobId, Integer start,
                                                                                     Integer pageSize) {
        AuthResultVO authResultVO = authService.auth(true, username, ActionId.LIST_BUSINESS,
            ResourceTypeEnum.BUSINESS, appId.toString(), null);
        if (!authResultVO.isPass()) {
            return ServiceResponse.buildAuthFailResp(authResultVO);
        }

        CronJobInfoDTO cronJobInfo = cronJobService.getCronJobInfoById(appId, cronJobId);
        if (cronJobInfo != null) {
            CronJobHistoryDTO historyCondition = new CronJobHistoryDTO();
            historyCondition.setAppId(appId);
            historyCondition.setCronJobId(cronJobId);
            historyCondition.setStatus(ExecuteStatusEnum.FAIL);

            BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
            baseSearchCondition.setStart(start);
            baseSearchCondition.setLength(pageSize);
            PageData<CronJobHistoryDTO> cronJobHistoryPageData =
                cronJobHistoryService.listPageHistoryByCondition(historyCondition,
                    baseSearchCondition);

            List<CronJobLaunchHistoryVO> resultCronJobHistories = new ArrayList<>();
            if (cronJobHistoryPageData != null) {
                cronJobHistoryPageData.getData().forEach(cronJobHistoryInfo ->
                    resultCronJobHistories.add(CronJobHistoryDTO.toVO(cronJobHistoryInfo)));
            } else {
                PageData<CronJobLaunchHistoryVO> resultPageData = new PageData<>();
                resultPageData.setStart(start);
                resultPageData.setPageSize(pageSize);
                resultPageData.setTotal(0L);
                resultPageData.setData(Collections.emptyList());
                return ServiceResponse.buildSuccessResp(resultPageData);
            }

            PageData<CronJobLaunchHistoryVO> resultPageData = new PageData<>();
            resultPageData.setStart(cronJobHistoryPageData.getStart());
            resultPageData.setPageSize(cronJobHistoryPageData.getPageSize());
            resultPageData.setTotal(cronJobHistoryPageData.getTotal());
            resultPageData.setData(resultCronJobHistories);

            return ServiceResponse.buildSuccessResp(resultPageData);
        }
        return ServiceResponse.buildCommonFailResp(ErrorCode.CRON_JOB_NOT_EXIST);
    }
}
