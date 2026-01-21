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

package com.tencent.bk.job.common.model.dto.notify;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class CustomNotifyDTO {
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
    private List<StatusNotifyChannel> customNotifyChannel;

    public void buildCustomNotifyChannel(Map<String, List<String>> map) {
        this.setCustomNotifyChannel(
            map.entrySet().stream()
            .filter(entry -> ExecuteStatusEnum.hasName(entry.getKey()))
            .map(entry -> {
                StatusNotifyChannel channel = new StatusNotifyChannel();
                channel.setExecuteStatus(ExecuteStatusEnum.valueOf(entry.getKey()));
                channel.setChannelList(entry.getValue());
                return channel;
            }).collect(Collectors.toList())
        );
    }

    public static CustomNotifyVO toVO(CustomNotifyDTO customNotifyDTO) {
        CustomNotifyVO cronJobCustomNotifyVO = new CustomNotifyVO();
        cronJobCustomNotifyVO.setRoleList(customNotifyDTO.getRoleList());
        cronJobCustomNotifyVO.setExtraObserverList(customNotifyDTO.getExtraObserverList());
        if (CollectionUtils.isNotEmpty(customNotifyDTO.getCustomNotifyChannel())) {
            Map<String, List<String>> customNotifyChannelMap = new HashMap<>();
            customNotifyDTO.getCustomNotifyChannel().forEach(statusNotifyChannel ->
                customNotifyChannelMap.put(
                    statusNotifyChannel.getExecuteStatus().getName(),
                    statusNotifyChannel.getChannelList())
            );
            cronJobCustomNotifyVO.setResourceStatusChannelMap(customNotifyChannelMap);
        }
        return cronJobCustomNotifyVO;
    }

}
