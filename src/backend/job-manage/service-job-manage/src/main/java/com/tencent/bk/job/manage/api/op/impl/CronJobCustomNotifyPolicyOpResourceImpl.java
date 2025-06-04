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

package com.tencent.bk.job.manage.api.op.impl;

import com.tencent.bk.job.common.constant.CronJobNotifyType;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.crontab.api.inner.ServiceCronJobResource;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.manage.api.common.constants.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.api.op.CronJobCustomNotifyPolicyOpResource;
import com.tencent.bk.job.manage.model.inner.ServiceSpecificResourceNotifyPolicyDTO;
import com.tencent.bk.job.manage.model.web.request.notify.ResourceStatusChannel;
import com.tencent.bk.job.manage.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class CronJobCustomNotifyPolicyOpResourceImpl implements CronJobCustomNotifyPolicyOpResource {

    private final NotifyService notifyService;
    private final ServiceCronJobResource cronJobResource;

    @Autowired
    public CronJobCustomNotifyPolicyOpResourceImpl(NotifyService notifyService,
                                                   ServiceCronJobResource cronJobResource) {
        this.notifyService = notifyService;
        this.cronJobResource = cronJobResource;
    }

    @Override
    public Response<List<Long>> batchSyncCronJobCustomNotifyPolicy(String username, List<Long> cronTaskIdList) {
        List<Long> successCronTaskIdList = new ArrayList<>();
        cronTaskIdList.forEach(cronTaskId -> {
            syncSingleCronJobNotifyPolicy(successCronTaskIdList, cronTaskId);
        });
        return Response.buildSuccessResp(successCronTaskIdList);
    }

    @Override
    public Response<List<Long>> batchDelete(String username, Long appId, List<Long> cronTaskIdList) {
        Integer cronType = ResourceTypeEnum.CRON.getType();
        List<Long> deletedCronTaskIdList = new ArrayList<>();
        for (Long cronTaskId : cronTaskIdList) {
            int cnt = notifyService.deleteAppResourceNotifyPolicies(appId, cronType, String.valueOf(cronTaskId));
            if (cnt > 0) {
                deletedCronTaskIdList.add(cronTaskId);
            }
        }
        return Response.buildSuccessResp(deletedCronTaskIdList);
    }

    private void syncSingleCronJobNotifyPolicy(List<Long> successCronTaskIdList, Long cronTaskId) {
        log.info("begin to syncSingleCronJobNotifyPolicy by op, cronTaskId={}", cronTaskId);
        ServiceCronJobDTO serviceCronJobDTO = cronJobResource.getCronJobById(cronTaskId).getData();

        if (serviceCronJobDTO == null) {
            log.warn("syncSingleCronJobNotifyPolicy by op fail, cron task with id {} not exist", cronTaskId);
            return;
        } else if (!Objects.equals(serviceCronJobDTO.getNotifyType(), CronJobNotifyType.CUSTOM.getType())) {
            log.warn("skip syncSingleCronJobNotifyPolicy by op, cron task with id {} notify policy is not custom",
                cronTaskId);
            return;
        }

        Long appId = serviceCronJobDTO.getAppId();
        ServiceSpecificResourceNotifyPolicyDTO specificResourceNotifyPolicyDTO =
            new ServiceSpecificResourceNotifyPolicyDTO();

        specificResourceNotifyPolicyDTO.setAppId(appId);
        specificResourceNotifyPolicyDTO.setTriggerType(TriggerTypeEnum.TIMER_TASK);
        specificResourceNotifyPolicyDTO.setResourceType(ResourceTypeEnum.CRON.getType());
        specificResourceNotifyPolicyDTO.setResourceId(serviceCronJobDTO.getId());
        specificResourceNotifyPolicyDTO.setRoleList(serviceCronJobDTO.getCustomCronJobNotifyDTO().getRoleList());
        specificResourceNotifyPolicyDTO.setExtraObserverList(
            serviceCronJobDTO.getCustomCronJobNotifyDTO().getExtraObserverList());
        specificResourceNotifyPolicyDTO.setResourceStatusChannelList(
            serviceCronJobDTO.getCustomCronJobNotifyDTO().getCustomNotifyChannel().stream()
                .map(cronJobStatusNotifyChannel -> new ResourceStatusChannel(
                        ExecuteStatusEnum.get(cronJobStatusNotifyChannel.getExecuteStatus().getValue()),
                        cronJobStatusNotifyChannel.getChannelList()
                    )
                ).collect(Collectors.toList())
        );

        notifyService.saveSpecificResourceNotifyPolicies(
            appId,
            serviceCronJobDTO.getLastModifyUser(),
            specificResourceNotifyPolicyDTO
        );

        successCronTaskIdList.add(cronTaskId);
    }
}
