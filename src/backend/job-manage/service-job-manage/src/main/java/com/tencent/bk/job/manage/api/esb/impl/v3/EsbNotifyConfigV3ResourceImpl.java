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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.api.esb.v3.EsbNotifyConfigV3Resource;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.TriggerPolicyDTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbNotifyPolicyV3DTO;
import com.tencent.bk.job.manage.service.NotifyService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class EsbNotifyConfigV3ResourceImpl implements EsbNotifyConfigV3Resource {

    private final NotifyService notifyService;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public EsbNotifyConfigV3ResourceImpl(NotifyService notifyService,
                                         AppScopeMappingService appScopeMappingService) {
        this.notifyService = notifyService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_notify_config"})
    public EsbResp<List<EsbNotifyPolicyV3DTO>> getNotifyConfig(String username,
                                                                String appCode,
                                                                String scopeType,
                                                                String scopeId) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);

        // 所有的通知渠道
        List<String> allNotifyChannelTypes = notifyService.listAllNotifyChannel(JobContextUtil.getTenantId())
            .stream()
            .map(NotifyEsbChannelDTO::getType)
            .collect(Collectors.toList());
        // 平台配置的可用通知渠道列表
        List<String> availableChannels = notifyService.getAvailableChannelTypeList(JobContextUtil.getTenantId())
            .stream()
            .filter(allNotifyChannelTypes::contains)
            .collect(Collectors.toList());
        List<TriggerPolicyDTO> notifyPolicies = notifyService.listAppDefaultNotifyPolicies(username, appId);
        List<EsbNotifyPolicyV3DTO> esbNotifyPolicies = notifyPolicies.stream()
            .map(policy -> convert(policy, availableChannels))
            .collect(Collectors.toList());
        
        return EsbResp.buildSuccessResp(esbNotifyPolicies);
    }

    /**
     * 关联平台可用消息通知渠道
     *
     * @param triggerPolicyDTO 消息通知策略
     * @param availableChannelTypes 平台可用的通知渠道类型列表
     * @return 消息通知配置
     */
    private EsbNotifyPolicyV3DTO convert(TriggerPolicyDTO triggerPolicyDTO, List<String> availableChannelTypes) {
        EsbNotifyPolicyV3DTO esbNotifyPolicy = new EsbNotifyPolicyV3DTO();
        esbNotifyPolicy.setTriggerType(triggerPolicyDTO.getTriggerType());
        esbNotifyPolicy.setResourceTypeList(triggerPolicyDTO.getResourceTypeList());
        esbNotifyPolicy.setRoleList(triggerPolicyDTO.getRoleList());
        esbNotifyPolicy.setExtraObserverList(triggerPolicyDTO.getExtraObserverList());

        Map<String, Map<String, Boolean>> convertedChannelMap = new HashMap<>();
        
        // 填充 SUCCESS、FAIL、READY
        Map<String, List<String>> sourceChannelMap = triggerPolicyDTO.getResourceStatusChannelMap();
        for (ExecuteStatusEnum status : ExecuteStatusEnum.values()) {
            List<String> enabledChannels = (sourceChannelMap != null) 
                ? sourceChannelMap.get(status.name())
                : null;
            Map<String, Boolean> channelEnabledMap = new HashMap<>();
            // 若是某状态没有启动任何通知渠道，则也要填充为{"SUCCESS": {"channel1": false"}}
            if (CollectionUtils.isEmpty(enabledChannels)) {
                channelEnabledMap = getAllFalseChannelMap(availableChannelTypes);
            } else {
                for (String channelType : availableChannelTypes) {
                    channelEnabledMap.put(channelType, enabledChannels.contains(channelType));
                }
            }
            convertedChannelMap.put(status.name(), channelEnabledMap);
        }
        esbNotifyPolicy.setResourceStatusChannelMap(convertedChannelMap);

        return esbNotifyPolicy;
    }

    private Map<String, Boolean> getAllFalseChannelMap(List<String> availableChannelTypes) {
        Map<String, Boolean> channelEnabledMap = new HashMap<>();
        for (String channelType : availableChannelTypes) {
            channelEnabledMap.put(channelType, false);
        }
        return channelEnabledMap;
    }
}
