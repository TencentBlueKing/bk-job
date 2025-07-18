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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.notify.CustomNotifyDTO;
import com.tencent.bk.job.crontab.model.dto.CronJobInfoDTO;
import com.tencent.bk.job.crontab.service.CustomNotifyPolicyService;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceNotificationResource;
import com.tencent.bk.job.manage.model.inner.ServiceSpecificResourceNotifyPolicyDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CustomNotifyPolicyServiceImpl implements CustomNotifyPolicyService {

    private final ServiceNotificationResource notificationResource;

    @Autowired
    public CustomNotifyPolicyServiceImpl(
        ServiceNotificationResource notificationResource
    ) {
        this.notificationResource = notificationResource;
    }

    @Override
    public void createOrUpdateCronJobCustomNotifyPolicy(Long cronJobId, CronJobInfoDTO cronJobInfoDTO) {
        log.info("Start create or update cronJob custom notify policy with cronJobId:{}", cronJobId);

        ServiceSpecificResourceNotifyPolicyDTO specificResourceNotifyPolicy =
            new ServiceSpecificResourceNotifyPolicyDTO();
        specificResourceNotifyPolicy.setAppId(cronJobInfoDTO.getAppId());
        specificResourceNotifyPolicy.setTriggerType(TriggerTypeEnum.TIMER_TASK.getType());
        specificResourceNotifyPolicy.setResourceType(ResourceTypeEnum.CRON.getType());
        specificResourceNotifyPolicy.setResourceId(cronJobId);
        if (cronJobInfoDTO.getCustomCronJobNotifyDTO() != null) {
            specificResourceNotifyPolicy.setRoleList(cronJobInfoDTO.getCustomCronJobNotifyDTO().getRoleList());
            specificResourceNotifyPolicy.setExtraObserverList(
                cronJobInfoDTO.getCustomCronJobNotifyDTO().getExtraObserverList()
            );
            Map<Integer, List<String>> statusChannelMap = new HashMap<>();
            cronJobInfoDTO.getCustomCronJobNotifyDTO().getCustomNotifyChannel().forEach(statusNotifyChannel -> {
                statusChannelMap.put(
                    statusNotifyChannel.getExecuteStatus().getValue(),
                    statusNotifyChannel.getChannelList()
                );
            });
            specificResourceNotifyPolicy.setResourceStatusChannelMap(statusChannelMap);
        }
        InternalResponse<Boolean> resp = notificationResource.createOrUpdateSpecificResourceNotifyPolicy(
            cronJobInfoDTO.getLastModifyUser(),
            cronJobInfoDTO.getAppId(),
            specificResourceNotifyPolicy);
        if (resp == null || !resp.isSuccess()) {
            throw new InternalException(ErrorCode.SAVE_CRON_CUSTOM_NOTIFY_FAILED, new Object[]{cronJobId});
        }
    }

    @Override
    public void deleteCronJobCustomNotifyPolicy(Long appId, Long cronJobId) {
        Integer cnt = notificationResource.deleteSpecificResourceNotifyPolicy(
            appId,
            ResourceTypeEnum.CRON.getType(),
            String.valueOf(cronJobId)
        ).getData();
        log.info("Deleted custom notify policy with cron task id:{}, affect resources:{}", cronJobId, cnt);
    }

    @Override
    public CustomNotifyDTO getCronJobCustomNotifyPolicyById(Long appId, Long cronJobId) {
        return notificationResource.getSpecificResourceNotifyPolicy(
            appId,
            ResourceTypeEnum.CRON.getType(),
            String.valueOf(cronJobId),
            TriggerTypeEnum.TIMER_TASK.getType()
        ).getData();
    }

}
