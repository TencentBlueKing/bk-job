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

package com.tencent.bk.job.manage.config.listener;

import com.tencent.bk.job.common.paas.model.EsbNotifyChannelDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.JobRoleEnum;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTriggerPolicyDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.web.request.notify.NotifyPoliciesCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.notify.ResourceStatusChannel;
import com.tencent.bk.job.manage.model.web.request.notify.TriggerPolicy;
import com.tencent.bk.job.manage.service.GlobalSettingsService;
import com.tencent.bk.job.manage.service.NotifyService;
import com.tencent.bk.job.manage.service.PaaSService;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class ApplicationReadyEventListener implements ApplicationListener<ApplicationReadyEvent> {

    final PaaSService paaSService;
    final GlobalSettingsService globalSettingsService;
    final NotifyService notifyService;
    final AvailableEsbChannelDAO availableEsbChannelDAO;
    final NotifyTriggerPolicyDAO notifyTriggerPolicyDAO;
    final DSLContext dslContext;

    @Value("${notify.default.channels.available:mail,weixin,rtx}")
    private final String defaultAvailableNotifyChannelsStr = "mail,weixin,rtx";

    @Autowired
    public ApplicationReadyEventListener(PaaSService paaSService, GlobalSettingsService globalSettingsService,
                                         NotifyService notifyService, AvailableEsbChannelDAO availableEsbChannelDAO,
                                         NotifyTriggerPolicyDAO notifyTriggerPolicyDAO, DSLContext dslContext) {
        this.paaSService = paaSService;
        this.globalSettingsService = globalSettingsService;
        this.notifyService = notifyService;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyTriggerPolicyDAO = notifyTriggerPolicyDAO;
        this.dslContext = dslContext;
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // 应用启动完成后的一些初始化操作
        // 1.消息通知默认配置
        try {
            log.info("init default notify channels");
            List<EsbNotifyChannelDTO> esbNotifyChannelDTOList = paaSService.getAllChannelList("", "100");
            if (esbNotifyChannelDTOList == null) {
                log.error("Fail to get notify channels from esb, null");
                return;
            }
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                if (!globalSettingsService.isNotifyChannelConfiged(context)) {
                    globalSettingsService.setNotifyChannelConfiged(context);
                    availableEsbChannelDAO.deleteAll(context);
                    for (EsbNotifyChannelDTO esbNotifyChannelDTO : esbNotifyChannelDTOList) {
                        if (esbNotifyChannelDTO.isActive()) {
                            availableEsbChannelDAO.insertAvailableEsbChannel(context,
                                new AvailableEsbChannelDTO(esbNotifyChannelDTO.getType(), true, "admin",
                                    LocalDateTime.now()));
                        }
                    }
                }
            });
        } catch (IOException e) {
            log.error("Fail to get notify channels from esb", e);
        } catch (Exception e) {
            log.error("Fail to init default notify channels", e);
        }
        // 2.用户侧默认配置
        try {
            if (notifyTriggerPolicyDAO.countDefaultPolicies() == 0) {
                //
                NotifyPoliciesCreateUpdateReq req = new NotifyPoliciesCreateUpdateReq();
                List<TriggerPolicy> triggerPolicyList = new ArrayList<>();
                //1.页面执行策略
                TriggerPolicy pageExecuteTriggerPolicy = new TriggerPolicy();
                pageExecuteTriggerPolicy.setTriggerType(TriggerTypeEnum.PAGE_EXECUTE);
                //默认通知任务执行人
                pageExecuteTriggerPolicy.setRoleList(
                    Collections.singletonList(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())
                );
                pageExecuteTriggerPolicy.setExtraObserverList(Collections.emptyList());
                //三种资源类型默认全部选中
                List<ResourceTypeEnum> pageExecuteResourceTypeList = new ArrayList<>();
                pageExecuteResourceTypeList.add(ResourceTypeEnum.SCRIPT);
                pageExecuteResourceTypeList.add(ResourceTypeEnum.FILE);
                pageExecuteResourceTypeList.add(ResourceTypeEnum.JOB);
                pageExecuteTriggerPolicy.setResourceTypeList(pageExecuteResourceTypeList);
                //成功、失败均默认邮件通知
                List<ResourceStatusChannel> resourceStatusChannelList = new ArrayList<>();
                List<String> defaultAvailableChannelList = StringUtil.strToList(defaultAvailableNotifyChannelsStr,
                    String.class, "[,;]");
                resourceStatusChannelList.add(new ResourceStatusChannel(ExecuteStatusEnum.FAIL,
                    defaultAvailableChannelList));
                pageExecuteTriggerPolicy.setResourceStatusChannelList(resourceStatusChannelList);
                //2.API调用策略
                TriggerPolicy apiInvokeTriggerPolicy = new TriggerPolicy();
                apiInvokeTriggerPolicy.setTriggerType(TriggerTypeEnum.API_INVOKE);
                //默认通知任务执行人
                apiInvokeTriggerPolicy.setRoleList(
                    Collections.singletonList(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())
                );
                apiInvokeTriggerPolicy.setExtraObserverList(Collections.emptyList());
                //三种资源类型默认全部选中
                List<ResourceTypeEnum> apiInvokeResourceTypeList = new ArrayList<>();
                apiInvokeResourceTypeList.add(ResourceTypeEnum.SCRIPT);
                apiInvokeResourceTypeList.add(ResourceTypeEnum.FILE);
                apiInvokeResourceTypeList.add(ResourceTypeEnum.JOB);
                apiInvokeTriggerPolicy.setResourceTypeList(apiInvokeResourceTypeList);
                //成功、失败均默认邮件通知
                List<ResourceStatusChannel> apiInvokeResourceStatusChannelList = new ArrayList<>();
                apiInvokeResourceStatusChannelList.add(new ResourceStatusChannel(ExecuteStatusEnum.FAIL,
                    defaultAvailableChannelList));
                apiInvokeTriggerPolicy.setResourceStatusChannelList(apiInvokeResourceStatusChannelList);
                //3.定时任务策略
                TriggerPolicy timerTaskTriggerPolicy = new TriggerPolicy();
                timerTaskTriggerPolicy.setTriggerType(TriggerTypeEnum.TIMER_TASK);
                //默认通知任务执行人
                timerTaskTriggerPolicy.setRoleList(
                    Collections.singletonList(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())
                );
                timerTaskTriggerPolicy.setExtraObserverList(Collections.emptyList());
                //三种资源类型默认全部选中
                List<ResourceTypeEnum> timerTaskResourceTypeList = new ArrayList<>();
                timerTaskResourceTypeList.add(ResourceTypeEnum.SCRIPT);
                timerTaskResourceTypeList.add(ResourceTypeEnum.FILE);
                timerTaskResourceTypeList.add(ResourceTypeEnum.JOB);
                timerTaskTriggerPolicy.setResourceTypeList(timerTaskResourceTypeList);
                //成功、失败均默认邮件通知
                List<ResourceStatusChannel> timerTaskResourceStatusChannelList = new ArrayList<>();
                timerTaskResourceStatusChannelList.add(new ResourceStatusChannel(ExecuteStatusEnum.FAIL,
                    defaultAvailableChannelList));
                timerTaskTriggerPolicy.setResourceStatusChannelList(timerTaskResourceStatusChannelList);
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
}
