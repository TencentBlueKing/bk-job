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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.crontab.api.web.WebCronJobResource;
import com.tencent.bk.job.crontab.auth.CronAuthService;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class WebCronJobResourceImpl implements WebCronJobResource {

    private final CronJobService cronJobService;
    private final CronJobHistoryService cronJobHistoryService;
    private final CronAuthService cronAuthService;

    @Autowired
    public WebCronJobResourceImpl(CronJobService cronJobService,
                                  CronJobHistoryService cronJobHistoryService,
                                  CronAuthService cronAuthService) {
        this.cronJobService = cronJobService;
        this.cronJobHistoryService = cronJobHistoryService;
        this.cronAuthService = cronAuthService;
    }

    @Override
    public Response<PageData<CronJobVO>> listCronJobs(String username,
                                                      AppResourceScope appResourceScope,
                                                      String scopeType,
                                                      String scopeId,
                                                      Long cronJobId,
                                                      Long planId,
                                                      String name,
                                                      String creator,
                                                      String lastModifyUser,
                                                      Integer start,
                                                      Integer pageSize,
                                                      String orderField,
                                                      Integer order) {
        Long appId = appResourceScope.getAppId();

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
        cronJobInfoPageData.getData().forEach(cronJobInfo -> resultCronJobs.add(CronJobInfoDTO.toVO(cronJobInfo)));

        PageData<CronJobVO> resultPageData = new PageData<>();
        resultPageData.setStart(cronJobInfoPageData.getStart());
        resultPageData.setPageSize(cronJobInfoPageData.getPageSize());
        resultPageData.setTotal(cronJobInfoPageData.getTotal());
        resultPageData.setData(resultCronJobs);
        resultPageData.setExistAny(cronJobService.isExistAnyAppCronJob(appId));

        processCronJobPermission(appResourceScope, resultPageData);

        return Response.buildSuccessResp(resultPageData);
    }

    @Override
    public Response<List<CronJobVO>> listCronJobStatistic(String username,
                                                          AppResourceScope appResourceScope,
                                                          String scopeType,
                                                          String scopeId,
                                                          List<Long> cronJobId) {
        Long appId = appResourceScope.getAppId();

        List<CronJobInfoDTO> cronJobInfoList = cronJobService.listCronJobByIds(appId, cronJobId);
        List<CronJobVO> resultCronJobs = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(cronJobInfoList)) {
            cronJobInfoList.forEach(cronJobInfo -> {
                CronJobVO cronJobVO = CronJobInfoDTO.toBasicVO(cronJobInfo);
                cronJobVO.setVariableValue(Collections.emptyList());
                resultCronJobs.add(cronJobVO);
            });
        }

        processCronExecuteHistory(appId, resultCronJobs);

        return Response.buildSuccessResp(resultCronJobs);
    }

    private void processCronJobPermission(AppResourceScope appResourceScope, PageData<CronJobVO> resultPageData) {
        resultPageData.setCanCreate(
            cronAuthService.authCreateCron(JobContextUtil.getUsername(), appResourceScope).isPass());

        processCronJobPermission(appResourceScope, resultPageData.getData());
    }

    private void processCronJobPermission(AppResourceScope appResourceScope, List<CronJobVO> cronJobList) {
        List<Long> cronJobIdList = new ArrayList<>();
        cronJobList.forEach(cronJob -> cronJobIdList.add(cronJob.getId()));
        List<Long> allowedCronJob = cronAuthService.batchAuthManageCron(
            JobContextUtil.getUsername(), appResourceScope, cronJobIdList);
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
        Map<Long, ServiceCronTaskExecuteResultStatistics> cronJobExecuteHistory;
        Map<Long, CronJobLaunchResultStatistics> cronJobLaunchHistory;
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

    private void fillStatisticInfo(CronJobVO resultCronJob,
                                   ServiceCronTaskExecuteResultStatistics cronTaskExecuteResult,
                                   CronJobLaunchResultStatistics launchHistory) {
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
                int fail = 0;
                for (CronTaskExecuteResult executeRecord : lastExecuteRecords) {
                    RunStatusEnum taskStatus = RunStatusEnum.valueOf(executeRecord.getStatus());
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
                        case WAITING_USER:
                        default:
                            break;
                    }
                }
                resultCronJob.setTotalCount(total);
                resultCronJob.setFailCount(fail);
            }
        }
        fillStatisticInfoWithLaunchHistory(resultCronJob, lastFailTimeList, launchHistory);
    }

    private void fillStatisticInfoWithLaunchHistory(CronJobVO resultCronJob,
                                                    List<Long> lastFailTimeList,
                                                    CronJobLaunchResultStatistics launchHistory) {
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
    public Response<CronJobVO> getCronJobById(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Long cronJobId) {
        Long appId = appResourceScope.getAppId();

        CronJobVO cronJobVO = CronJobInfoDTO.toVO(cronJobService.getCronJobInfoById(appId, cronJobId));

        AuthResult authResult = cronAuthService.authManageCron(username,
            appResourceScope, cronJobId, cronJobVO.getName());
        if (authResult.isPass()) {
            return Response.buildSuccessResp(cronJobVO);
        } else {
            throw new PermissionDeniedException(authResult);
        }
    }

    @Override
    public Response<Long> saveCronJob(String username,
                                      AppResourceScope appResourceScope,
                                      String scopeType,
                                      String scopeId,
                                      Long cronJobId,
                                      CronJobCreateUpdateReq cronJobCreateUpdateReq) {

        Long appId = appResourceScope.getAppId();

        if (cronJobId > 0) {
            cronJobCreateUpdateReq.setId(cronJobId);
            AuthResult authResult = cronAuthService.authManageCron(username,
                appResourceScope, cronJobId, null);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        } else {
            AuthResult authResult = cronAuthService.authCreateCron(username,
                appResourceScope);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        }
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            cronJobCreateUpdateReq.setName(stringCheckHelper.checkAndGetResult(cronJobCreateUpdateReq.getName()));
        } catch (StringCheckException e) {
            log.warn("Cron Job Name is invalid:", e);
            throw new InvalidParamException(e, ErrorCode.ILLEGAL_PARAM);
        }
        CronJobInfoDTO cronJobInfoDTO = CronJobInfoDTO.fromReq(username, appId, cronJobCreateUpdateReq);
        if (cronJobInfoDTO.validate()) {
            try {
                Long finalCronJobId = cronJobService.saveCronJobInfo(cronJobInfoDTO);
                return Response.buildSuccessResp(finalCronJobId);
            } catch (TaskExecuteAuthFailedException e) {
                throw new PermissionDeniedException(e.getAuthResult());
            }
        } else {
            log.warn("Validate cron job failed!|{}", JobContextUtil.getDebugMessage());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Override
    public Response<Boolean> deleteCronJob(String username,
                                           AppResourceScope appResourceScope,
                                           String scopeType,
                                           String scopeId,
                                           Long cronJobId) {
        Long appId = appResourceScope.getAppId();

        AuthResult authResult = cronAuthService.authManageCron(username,
            appResourceScope, cronJobId, null);
        if (authResult.isPass()) {
            return Response.buildSuccessResp(cronJobService.deleteCronJobInfo(appId, cronJobId));
        } else {
            throw new PermissionDeniedException(authResult);
        }
    }

    @Override
    public Response<Boolean> changeCronJobEnableStatus(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       Long cronJobId,
                                                       Boolean enable) {
        Long appId = appResourceScope.getAppId();

        AuthResult authResult = cronAuthService.authManageCron(username,
            new AppResourceScope(appId), cronJobId, null);
        if (authResult.isPass()) {
            try {
                return Response
                    .buildSuccessResp(cronJobService.changeCronJobEnableStatus(username, appId, cronJobId, enable));
            } catch (TaskExecuteAuthFailedException e) {
                throw new PermissionDeniedException(e.getAuthResult());
            }
        } else {
            throw new PermissionDeniedException(authResult);
        }
    }

    @Override
    public Response<Boolean> checkCronJobName(String username,
                                              AppResourceScope appResourceScope,
                                              String scopeType,
                                              String scopeId,
                                              Long cronJobId,
                                              String name) {
        Long appId = appResourceScope.getAppId();
        return Response.buildSuccessResp(cronJobService.checkCronJobName(appId, cronJobId, name));
    }

    @Override
    public Response<Boolean> batchUpdateCronJob(String username,
                                                AppResourceScope appResourceScope,
                                                String scopeType,
                                                String scopeId,
                                                BatchUpdateCronJobReq batchUpdateCronJobReq) {
        Long appId = appResourceScope.getAppId();

        List<Long> cronJobInstanceList = new ArrayList<>();
        batchUpdateCronJobReq.getCronJobInfoList()
            .forEach(cronJobCreateUpdateReq -> cronJobInstanceList.add(cronJobCreateUpdateReq.getId()));
        List<Long> allowed =
            cronAuthService.batchAuthManageCron(username, appResourceScope, cronJobInstanceList);
        if (allowed.size() == cronJobInstanceList.size()) {
            return Response.buildSuccessResp(cronJobService.batchUpdateCronJob(appId, batchUpdateCronJobReq));
        } else {
            return Response.buildCommonFailResp(ErrorCode.BK_PERMISSION_DENIED);
        }
    }

    @Override
    public Response<List<CronJobVO>> getCronJobListByPlanId(String username,
                                                            AppResourceScope appResourceScope,
                                                            String scopeType,
                                                            String scopeId,
                                                            Long planId) {
        Long appId = appResourceScope.getAppId();

        List<CronJobInfoDTO> cronJobInfoList = cronJobService.listCronJobByPlanId(appId, planId);
        if (CollectionUtils.isNotEmpty(cronJobInfoList)) {
            List<CronJobVO> cronJobList =
                cronJobInfoList.parallelStream().map(CronJobInfoDTO::toBasicVO).collect(Collectors.toList());
            processCronJobPermission(appResourceScope, cronJobList);
            return Response.buildSuccessResp(cronJobList);
        } else {
            return Response.buildSuccessResp(Collections.emptyList());
        }
    }

    @Override
    public Response<Map<Long, List<CronJobVO>>> getCronJobListByPlanIdList(String username,
                                                                           AppResourceScope appResourceScope,
                                                                           String scopeType,
                                                                           String scopeId,
                                                                           List<Long> planIdList) {
        Long appId = appResourceScope.getAppId();

        Map<Long, List<CronJobInfoDTO>> cronJobInfoMap = cronJobService.listCronJobByPlanIds(appId, planIdList);
        if (MapUtils.isNotEmpty(cronJobInfoMap)) {
            Map<Long, List<CronJobVO>> cronJobMap = new HashMap<>(cronJobInfoMap.size());
            for (Map.Entry<Long, List<CronJobInfoDTO>> cronJobInfoListEntity : cronJobInfoMap.entrySet()) {
                List<CronJobInfoDTO> cronJobInfoList = cronJobInfoListEntity.getValue();
                List<CronJobVO> cronJobList =
                    cronJobInfoList.parallelStream().map(CronJobInfoDTO::toBasicVO).collect(Collectors.toList());
                processCronJobPermission(appResourceScope, cronJobList);
                cronJobMap.put(cronJobInfoListEntity.getKey(), cronJobList);
            }
            return Response.buildSuccessResp(cronJobMap);
        } else {
            return Response.buildSuccessResp(Collections.emptyMap());
        }
    }

    @Override
    public Response<PageData<CronJobLaunchHistoryVO>> getCronJobLaunchHistory(String username,
                                                                              AppResourceScope appResourceScope,
                                                                              String scopeType,
                                                                              String scopeId,
                                                                              Long cronJobId,
                                                                              Integer start,
                                                                              Integer pageSize) {
        Long appId = appResourceScope.getAppId();

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
                return Response.buildSuccessResp(resultPageData);
            }

            PageData<CronJobLaunchHistoryVO> resultPageData = new PageData<>();
            resultPageData.setStart(cronJobHistoryPageData.getStart());
            resultPageData.setPageSize(cronJobHistoryPageData.getPageSize());
            resultPageData.setTotal(cronJobHistoryPageData.getTotal());
            resultPageData.setData(resultCronJobHistories);

            return Response.buildSuccessResp(resultPageData);
        }
        return Response.buildCommonFailResp(ErrorCode.CRON_JOB_NOT_EXIST);
    }
}
