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

package com.tencent.bk.job.crontab.model;

import lombok.Data;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class CustomCronJobNotifyDTO {
    /**
     * 自定义通知角色
     */
    private List<String> roleList;

    /**
     * 自定义额外通知人
     */
    private List<String> extraObserverList;

    /**
     * 自定义通知，当notifyType为CUSTOM时生效
     * 执行状态与对应通知渠道列表
     */
    private List<CronJobStatusNotifyChannel> customNotifyChannel = Collections.emptyList();

    public static CronJobCustomNotifyVO toVO(CustomCronJobNotifyDTO customCronJobNotifyDTO) {
        CronJobCustomNotifyVO cronJobCustomNotifyVO = new CronJobCustomNotifyVO();
        cronJobCustomNotifyVO.setRoleList(customCronJobNotifyDTO.getRoleList());
        cronJobCustomNotifyVO.setExtraObserverList(customCronJobNotifyDTO.getExtraObserverList());
        Map<String, List<String>> customNotifyChannelMap = new HashMap<>();
        customCronJobNotifyDTO.getCustomNotifyChannel().forEach(cronJobStatusNotifyChannel ->
            customNotifyChannelMap.put(
                cronJobStatusNotifyChannel.getExecuteStatus().getName(), cronJobStatusNotifyChannel.getChannelList())
        );
        cronJobCustomNotifyVO.setResourceStatusChannelMap(customNotifyChannelMap);
        return cronJobCustomNotifyVO;
    }

    public static CustomCronJobNotifyDTO fromReq(CronJobCreateUpdateReq cronJobCreateUpdateReq) {
        CustomCronJobNotifyDTO cronJobNotifyDTO = new CustomCronJobNotifyDTO();
        if (cronJobCreateUpdateReq.getNotifyUser() != null) {
            cronJobNotifyDTO.setRoleList(cronJobCreateUpdateReq.getNotifyUser().getRoleList());
            cronJobNotifyDTO.setExtraObserverList(cronJobCreateUpdateReq.getNotifyUser().getUserList());
        }
        cronJobNotifyDTO.setCustomNotifyChannel(cronJobCreateUpdateReq.getCustomNotifyChannel());
        return cronJobNotifyDTO;
    }

}
