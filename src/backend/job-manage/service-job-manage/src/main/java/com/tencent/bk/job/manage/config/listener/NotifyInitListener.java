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

package com.tencent.bk.job.manage.config.listener;

import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.paas.cmsi.CmsiApiClient;
import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.api.common.constants.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.JobRoleEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTriggerPolicyDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.notify.ResourceStatusChannel;
import com.tencent.bk.job.manage.model.web.request.notify.TriggerPolicy;
import com.tencent.bk.job.manage.service.NotifyService;
import com.tencent.bk.job.manage.service.globalsetting.GlobalSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@Profile("!test")
public class NotifyInitListener implements ApplicationListener<ApplicationReadyEvent> {

    private final CmsiApiClient cmsiApiClient;
    private final GlobalSettingsService globalSettingsService;
    private final NotifyService notifyService;
    private final AvailableEsbChannelDAO availableEsbChannelDAO;
    private final NotifyTriggerPolicyDAO notifyTriggerPolicyDAO;

    @Value("${job.manage.notify.default.channels.available:mail,weixin,rtx}")
    private final String defaultAvailableNotifyChannelsStr = "mail,weixin,rtx";

    @Autowired
    public NotifyInitListener(CmsiApiClient cmsiApiClient,
                              GlobalSettingsService globalSettingsService,
                              NotifyService notifyService,
                              AvailableEsbChannelDAO availableEsbChannelDAO,
                              NotifyTriggerPolicyDAO notifyTriggerPolicyDAO) {
        this.cmsiApiClient = cmsiApiClient;
        this.globalSettingsService = globalSettingsService;
        this.notifyService = notifyService;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyTriggerPolicyDAO = notifyTriggerPolicyDAO;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // 应用启动完成后的一些初始化操作
        // 1.消息通知默认配置
        initDefaultNotifyChannels();
        // 2.用户侧默认配置
        try {
            if (notifyTriggerPolicyDAO.countDefaultPolicies() == 0) {
                //
                NotifyPoliciesCreateUpdateReq req = new NotifyPoliciesCreateUpdateReq();
                List<TriggerPolicy> triggerPolicyList = new ArrayList<>();
                //1.页面执行策略
                TriggerPolicy pageExecuteTriggerPolicy = new TriggerPolicy();
                pageExecuteTriggerPolicy.setTriggerType(TriggerTypeEnum.PAGE_EXECUTE);
                setDefaultTargetAndChannels(pageExecuteTriggerPolicy);
                //2.API调用策略
                TriggerPolicy apiInvokeTriggerPolicy = new TriggerPolicy();
                apiInvokeTriggerPolicy.setTriggerType(TriggerTypeEnum.API_INVOKE);
                setDefaultTargetAndChannels(apiInvokeTriggerPolicy);
                //3.定时任务策略
                TriggerPolicy timerTaskTriggerPolicy = new TriggerPolicy();
                timerTaskTriggerPolicy.setTriggerType(TriggerTypeEnum.TIMER_TASK);
                setDefaultTargetAndChannels(timerTaskTriggerPolicy);
                //策略集成
                triggerPolicyList.add(pageExecuteTriggerPolicy);
                triggerPolicyList.add(apiInvokeTriggerPolicy);
                triggerPolicyList.add(timerTaskTriggerPolicy);
                req.setTriggerPoliciesList(triggerPolicyList);
                notifyService.saveAppDefaultNotifyPoliciesToLocal(
                    NotifyConsts.JOB_ADMINISTRATOR_NAME,
                    NotifyConsts.DEFAULT_APP_ID,
                    NotifyConsts.JOB_ADMINISTRATOR_NAME,
                    req
                );
            } else {
                log.info("default notify policies already configured");
            }
        } catch (Exception e) {
            log.error("Fail to config default notify policies", e);
        }
    }

    private void initDefaultNotifyChannels() {
        log.info("init default notify channels");
        List<EsbNotifyChannelDTO> esbNotifyChannelDTOList = cmsiApiClient.getNotifyChannelList();
        if (esbNotifyChannelDTOList == null) {
            log.error("Fail to get notify channels from esb, null");
            return;
        }
        saveDefaultNotifyChannelsToDb(esbNotifyChannelDTOList);
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public void saveDefaultNotifyChannelsToDb(List<EsbNotifyChannelDTO> esbNotifyChannelDTOList) {
        if (!globalSettingsService.isNotifyChannelConfiged()) {
            globalSettingsService.setNotifyChannelConfiged();
            availableEsbChannelDAO.deleteAll();
            for (EsbNotifyChannelDTO esbNotifyChannelDTO : esbNotifyChannelDTOList) {
                if (!esbNotifyChannelDTO.isActive()) {
                    continue;
                }
                availableEsbChannelDAO.insertAvailableEsbChannel(
                    new AvailableEsbChannelDTO(
                        esbNotifyChannelDTO.getType(),
                        true,
                        "admin",
                        LocalDateTime.now()
                    )
                );
            }
        }
    }

    private void setDefaultTargetAndChannels(TriggerPolicy policy) {
        //默认通知任务执行人
        policy.setRoleList(
            Collections.singletonList(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())
        );
        policy.setExtraObserverList(Collections.emptyList());
        //三种资源类型默认全部选中
        List<ResourceTypeEnum> pageExecuteResourceTypeList = new ArrayList<>();
        pageExecuteResourceTypeList.add(ResourceTypeEnum.SCRIPT);
        pageExecuteResourceTypeList.add(ResourceTypeEnum.FILE);
        pageExecuteResourceTypeList.add(ResourceTypeEnum.JOB);
        policy.setResourceTypeList(pageExecuteResourceTypeList);
        //成功、失败均默认邮件通知
        List<ResourceStatusChannel> resourceStatusChannelList = new ArrayList<>();
        List<String> defaultAvailableChannelList = StringUtil.strToList(defaultAvailableNotifyChannelsStr,
            String.class, "[,;]");
        resourceStatusChannelList.add(new ResourceStatusChannel(ExecuteStatusEnum.FAIL,
            defaultAvailableChannelList));
        policy.setResourceStatusChannelList(resourceStatusChannelList);
    }
}
