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

package com.tencent.bk.job.manage.service.globalsetting.impl;

import com.tencent.bk.audit.annotations.ActionAuditRecord;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.common.constants.notify.NotifyConsts;
import com.tencent.bk.job.manage.dao.notify.AvailableEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.notify.AvailableEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.web.request.notify.SetAvailableNotifyChannelReq;
import com.tencent.bk.job.manage.model.web.vo.globalsetting.NotifyChannelWithIconVO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateStatusVO;
import com.tencent.bk.job.manage.model.web.vo.notify.TemplateBasicInfo;
import com.tencent.bk.job.manage.service.NotifyService;
import com.tencent.bk.job.manage.service.globalsetting.EsbNotifyChannelService;
import com.tencent.bk.job.manage.service.impl.notify.NotifyChannelInitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EsbNotifyChannelServiceImpl implements EsbNotifyChannelService {

    private final NotifyTemplateDAO notifyTemplateDAO;
    private final AvailableEsbChannelDAO availableEsbChannelDAO;
    private final NotifyEsbChannelDAO notifyEsbChannelDAO;
    private final MessageI18nService i18nService;
    private final NotifyChannelInitService notifyChannelInitService;
    private final NotifyService notifyService;

    @Autowired
    public EsbNotifyChannelServiceImpl(NotifyTemplateDAO notifyTemplateDAO,
                                       AvailableEsbChannelDAO availableEsbChannelDAO,
                                       NotifyEsbChannelDAO notifyEsbChannelDAO,
                                       MessageI18nService i18nService,
                                       NotifyChannelInitService notifyChannelInitService,
                                       NotifyService notifyService) {
        this.notifyTemplateDAO = notifyTemplateDAO;
        this.availableEsbChannelDAO = availableEsbChannelDAO;
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.i18nService = i18nService;
        this.notifyChannelInitService = notifyChannelInitService;
        this.notifyService = notifyService;
    }

    @Override
    public List<ChannelTemplateStatusVO> listChannelTemplateStatus(String username) {
        List<ChannelTemplateStatusVO> resultList = new ArrayList<>();
        List<String> messageCodeList = Arrays.asList(
            NotifyConsts.NOTIFY_TEMPLATE_CODE_CONFIRMATION,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_SUCCESS,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_FAILURE,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_END,
            NotifyConsts.NOTIFY_TEMPLATE_CODE_CRON_EXECUTE_FAILED
        );
        List<NotifyChannelWithIconVO> notifyChannelWithIconVOList = listNotifyChannel(username);
        String tenantId = JobContextUtil.getTenantId();
        notifyChannelWithIconVOList.forEach(notifyChannelWithIconVO -> {
            String channelCode = notifyChannelWithIconVO.getCode();
            List<TemplateBasicInfo> configStatusList = new ArrayList<>();
            messageCodeList.forEach(messageCode -> {
                Boolean configStatus = notifyTemplateDAO.existsNotifyTemplate(
                    channelCode,
                    messageCode,
                    false,
                    tenantId);
                configStatusList.add(new TemplateBasicInfo(messageCode,
                    i18nService.getI18n(NotifyConsts.NOTIFY_TEMPLATE_NAME_PREFIX + messageCode), configStatus));
            });
            resultList.add(new ChannelTemplateStatusVO(
                channelCode,
                notifyChannelWithIconVO.getName(),
                notifyChannelWithIconVO.getIcon(),
                notifyChannelWithIconVO.getIsActive(),
                configStatusList
            ));
        });
        return resultList;
    }

    @Override
    public List<NotifyChannelWithIconVO> listNotifyChannel(String username) {
        String tenantId = JobContextUtil.getTenantId();

        // 懒加载当前租户下的消息通知渠道
        notifyChannelInitService.tryToInitDefaultNotifyChannelsWithSingleTenant(tenantId);

        List<NotifyEsbChannelDTO> allNotifyChannelList =
            notifyEsbChannelDAO.listNotifyEsbChannel(tenantId).stream()
                .filter(NotifyEsbChannelDTO::isActive).collect(Collectors.toList());
        List<AvailableEsbChannelDTO> availableNotifyChannelList =
            availableEsbChannelDAO.listAvailableEsbChannel(tenantId);
        Set<String> availableNotifyChannelTypeSet =
            availableNotifyChannelList.stream().map(AvailableEsbChannelDTO::getType).collect(Collectors.toSet());
        return allNotifyChannelList.stream().map(it -> {
            String icon = it.getIcon();
            String prefix = "data:image/png;base64,";
            if (icon != null && !icon.startsWith("data:image")) {
                icon = prefix + icon;
            }
            return new NotifyChannelWithIconVO(
                it.getType(),
                it.getLabel(),
                icon,
                availableNotifyChannelTypeSet.contains(it.getType())
            );
        }).collect(Collectors.toList());
    }

    @Override
    @ActionAuditRecord(
        actionId = ActionId.GLOBAL_SETTINGS,
        content = EventContentConstants.EDIT_GLOBAL_SETTINGS
    )
    public Integer setAvailableNotifyChannel(String username, SetAvailableNotifyChannelReq req) {
        return notifyService.setAvailableNotifyChannel(username, req);
    }
}
