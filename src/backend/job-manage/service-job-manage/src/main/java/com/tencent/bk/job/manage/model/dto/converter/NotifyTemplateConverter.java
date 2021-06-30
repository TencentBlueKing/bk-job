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

package com.tencent.bk.job.manage.model.dto.converter;

import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.dao.notify.NotifyEsbChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyEsbChannelDTO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.web.vo.notify.ChannelTemplateDetailVO;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotifyTemplateConverter {

    final private MessageI18nService i18nService;
    final private NotifyEsbChannelDAO notifyEsbChannelDAO;
    final private DSLContext dslContext;

    @Autowired
    public NotifyTemplateConverter(MessageI18nService i18nService, NotifyEsbChannelDAO notifyEsbChannelDAO,
                                   DSLContext dslContext) {
        this.i18nService = i18nService;
        this.notifyEsbChannelDAO = notifyEsbChannelDAO;
        this.dslContext = dslContext;
    }

    public ChannelTemplateDetailVO convertToChannelTemplateDetailVO(NotifyTemplateDTO notifyTemplateDTO) {
        if (notifyTemplateDTO == null) {
            return null;
        }
        String channel = notifyTemplateDTO.getChannel();
        List<NotifyEsbChannelDTO> channelDTOList = notifyEsbChannelDAO.listNotifyEsbChannel(dslContext);
        String channelLabel = "";
        for (NotifyEsbChannelDTO notifyEsbChannelDTO : channelDTOList) {
            if (notifyEsbChannelDTO.getType().equals(channel)) {
                channelLabel = notifyEsbChannelDTO.getLabel();
                break;
            }
        }
        String title;
        String content;
        String normalLang = LocaleUtils.getNormalLang(JobContextUtil.getUserLang());
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            title = notifyTemplateDTO.getTitleEn();
            content = notifyTemplateDTO.getContentEn();
        } else {
            title = notifyTemplateDTO.getTitle();
            content = notifyTemplateDTO.getContent();
        }
        return new ChannelTemplateDetailVO(
            channel,
            channelLabel,
            notifyTemplateDTO.getCode(),
            i18nService.getI18n(NotifyConsts.NOTIFY_TEMPLATE_NAME_PREFIX + notifyTemplateDTO.getCode()),
            title,
            content,
            notifyTemplateDTO.getLastModifyUser(),
            notifyTemplateDTO.getLastModifyTime()
        );
    }
}
