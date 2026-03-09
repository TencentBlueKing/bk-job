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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.PrefConsts;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.Map;

@Slf4j
@Service
public class NotifyTemplateService {

    private final NotifyTemplateDAO notifyTemplateDAO;

    @Autowired
    public NotifyTemplateService(NotifyTemplateDAO notifyTemplateDAO) {
        this.notifyTemplateDAO = notifyTemplateDAO;
    }

    private Pair<String, String> getTitleAndContentByLanguage(NotifyTemplateDTO templateDTO, String normalLang) {
        String title;
        String content;
        if (normalLang.equals(LocaleUtils.LANG_EN) || normalLang.equals(LocaleUtils.LANG_EN_US)) {
            title = templateDTO.getTitleEn();
            if (title == null || StringUtils.isEmpty(title)) {
                title = templateDTO.getTitle();
            }
            content = templateDTO.getContentEn();
            if (content == null || StringUtils.isEmpty(content)) {
                content = templateDTO.getContent();
            }
        } else {
            title = templateDTO.getTitle();
            content = templateDTO.getContent();
        }
        return Pair.of(title, content);
    }

    /**
     * 从模板生成通知消息（仅负责模板渲染，不做变量预处理）
     *
     * @param templateDTO 通知模板
     * @param normalLang 标准化的语言（zh_CN 或 en_US）
     * @param variablesMap 已完成预处理的变量 Map
     * @return 渲染后的通知消息
     */
    public ServiceNotificationMessage getNotificationMessageFromTemplate(
        NotifyTemplateDTO templateDTO,
        String normalLang,
        Map<String, String> variablesMap
    ) {
        // 国际化
        Pair<String, String> titleAndContent = getTitleAndContentByLanguage(templateDTO, normalLang);
        String title = titleAndContent.getLeft();
        String content = titleAndContent.getRight();

        // 模板渲染
        String pattern = "(\\{\\{(.*?)\\}\\})";
        StopWatch watch = new StopWatch();
        watch.start("replace title and content");
        title = StringUtil.replaceByRegex(title, pattern, variablesMap);
        content = StringUtil.replaceByRegex(content, pattern, variablesMap);
        watch.stop();
        if (watch.getTotalTimeMillis() > 1000) {
            log.warn(
                "{},{},{},{},{}",
                PrefConsts.TAG_PREF_SLOW + watch.prettyPrint(),
                title,
                content,
                pattern,
                variablesMap
            );
        }
        return new ServiceNotificationMessage(title, content);
    }

    /**
     * 获取通知消息（仅负责查询模板和渲染）
     * 注意：variablesMap 应该已经完成预处理（添加默认变量、替换 username）
     *
     * @param tenantId 租户 ID
     * @param templateCode 模板代码
     * @param channel 通知渠道
     * @param normalLang 标准化的语言
     * @param variablesMap 已完成预处理的变量 Map
     * @return 渲染后的通知消息
     */
    public ServiceNotificationMessage getNotificationMessage(
        String tenantId,
        String templateCode,
        String channel,
        String normalLang,
        Map<String, String> variablesMap
    ) {
        // 1.查出自定义模板信息
        NotifyTemplateDTO notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
            channel,
            templateCode,
            false,
            tenantId
        );
        if (notifyTemplateDTO == null) {
            // 2.未配置自定义模板则使用默认模板
            notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(
                channel,
                templateCode,
                true,
                TenantIdConstants.DEFAULT_TENANT_ID);
        }
        if (notifyTemplateDTO == null) {
            log.warn("Cannot find template of templateCode:{},channel:{}, plz config a default template",
                templateCode, channel);
            return null;
        }

        return getNotificationMessageFromTemplate(notifyTemplateDTO, normalLang, variablesMap);
    }
}
