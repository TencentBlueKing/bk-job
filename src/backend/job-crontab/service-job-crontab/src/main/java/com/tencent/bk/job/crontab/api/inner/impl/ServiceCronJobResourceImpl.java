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

package com.tencent.bk.job.crontab.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.api.inner.ServiceCronJobResource;
import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;
import com.tencent.bk.job.crontab.model.CronJobCreateUpdateReq;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.crontab.model.inner.request.InternalUpdateCronStatusRequest;
import com.tencent.bk.job.crontab.service.CronJobService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 20/2/2020 20:09
 */
@Slf4j
@RestController
public class ServiceCronJobResourceImpl implements ServiceCronJobResource {

    private final CronJobService cronJobService;

    @Autowired
    public ServiceCronJobResourceImpl(CronJobService cronJobService) {
        this.cronJobService = cronJobService;
    }

    @Override
    public InternalResponse<List<ServiceCronJobDTO>> listCronJobs(Long appId, Boolean enable) {
        log.debug("list all cron jobs of appId:{}", appId);
        CronJobInfoDTO cronJobInfoDTO = new CronJobInfoDTO();
        cronJobInfoDTO.setAppId(appId);
        if (enable != null) {
            cronJobInfoDTO.setEnable(enable);
        }
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(Integer.MAX_VALUE);
        List<CronJobInfoDTO> cronJobDTOList = cronJobService.listPageCronJobInfos(cronJobInfoDTO,
            baseSearchCondition).getData();
        List<ServiceCronJobDTO> resultData = cronJobDTOList.stream()
            .map(CronJobInfoDTO::toServiceCronJobDTO).collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("cronJobDTOList.size={}", cronJobDTOList.size());
            log.info("resultData=" + JsonUtils.toJson(resultData));
        }
        return InternalResponse.buildSuccessResp(resultData);
    }

    @Override
    public InternalResponse<Long> saveCronJob(String username, Long appId, Long cronJobId,
                                              CronJobCreateUpdateReq cronJobCreateUpdateReq) {
        return null;
    }

    @Override
    public InternalResponse<Boolean> updateCronJobStatus(Long appId, Long cronJobId,
                                                         InternalUpdateCronStatusRequest request) {
        log.info("Update cron job status, appId: {}, cronJobId: {}, request: {}", appId, cronJobId, request);
        return InternalResponse.buildSuccessResp(cronJobService.changeCronJobEnableStatus(request.getOperator(),
            appId, cronJobId, CronStatusEnum.RUNNING.getStatus().equals(request.getStatus())));
    }

    @Override
    public InternalResponse<Map<Long, List<CronJobVO>>> batchListCronJobByPlanIds(Long appId, List<Long> planIdList) {
        Map<Long, List<CronJobInfoDTO>> cronJobInfoMap = cronJobService.listCronJobByPlanIds(appId, planIdList);
        if (MapUtils.isNotEmpty(cronJobInfoMap)) {
            Map<Long, List<CronJobVO>> cronJobMap = new HashMap<>(cronJobInfoMap.size());
            for (Map.Entry<Long, List<CronJobInfoDTO>> cronJobInfoEntry : cronJobInfoMap.entrySet()) {
                cronJobMap.put(cronJobInfoEntry.getKey(),
                    cronJobInfoEntry.getValue().stream().map(CronJobInfoDTO::toBasicVO).collect(Collectors.toList()));
            }
            return InternalResponse.buildSuccessResp(cronJobMap);
        }
        return InternalResponse.buildSuccessResp(null);
    }

    @Override
    public InternalResponse<Long> saveCronJobWithId(String username, Long appId, Long cronJobId, Long createTime,
                                                    Long lastModifyTime, String lastModifyUser,
                                                    CronJobCreateUpdateReq cronJobCreateUpdateReq) {
        JobContextUtil.setAllowMigration(true);
        CronJobInfoDTO cronJobInfoDTO = CronJobInfoDTO.fromReq(username, appId, cronJobCreateUpdateReq);
        cronJobInfoDTO.setId(cronJobId);
        cronJobInfoDTO.setCreator(username);
        if (createTime != null && createTime > 0) {
            cronJobInfoDTO.setCreateTime(createTime);
        } else {
            cronJobInfoDTO.setCreateTime(DateUtils.currentTimeSeconds());
        }
        if (StringUtils.isNotBlank(lastModifyUser)) {
            cronJobInfoDTO.setLastModifyUser(lastModifyUser);
        } else {
            cronJobInfoDTO.setLastModifyUser(username);
        }
        if (lastModifyTime != null && lastModifyTime > 0) {
            cronJobInfoDTO.setLastModifyTime(lastModifyTime);
        } else {
            cronJobInfoDTO.setLastModifyTime(DateUtils.currentTimeSeconds());
        }
        if (cronJobInfoDTO.validate()) {
            return InternalResponse.buildSuccessResp(cronJobService.insertCronJobInfoWithId(cronJobInfoDTO));
        } else {
            log.warn("Error while saving cron job!|{}", JobContextUtil.getDebugMessage());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
