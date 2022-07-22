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

package com.tencent.bk.job.manage.service.impl.notify;

import com.tencent.bk.job.common.cc.constants.CmdbConstants;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.PrefConsts;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TypeUtil;
import com.tencent.bk.job.manage.config.JobManageConfig;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotifyTemplateService {

    private final ApplicationDAO applicationDAO;
    private final JobManageConfig jobManageConfig;
    private final NotifyTemplateDAO notifyTemplateDAO;

    @Autowired
    public NotifyTemplateService(ApplicationDAO applicationDAO,
                                 JobManageConfig jobManageConfig,
                                 NotifyTemplateDAO notifyTemplateDAO) {
        this.applicationDAO = applicationDAO;
        this.jobManageConfig = jobManageConfig;
        this.notifyTemplateDAO = notifyTemplateDAO;
    }

    public Map<String, NotifyTemplateDTO> getChannelTemplateMap(String templateCode) {
        Map<String, NotifyTemplateDTO> channelTemplateMap = new HashMap<>();
        List<NotifyTemplateDTO> notifyTemplateDTOList = notifyTemplateDAO.listNotifyTemplateByCode(templateCode);
        if (CollectionUtils.isEmpty(notifyTemplateDTOList)) {
            log.warn("no valid templates of code:{}", templateCode);
            return Collections.emptyMap();
        }
        // 优先使用自定义模板
        notifyTemplateDTOList.sort(Comparator.comparingInt(o -> TypeUtil.booleanToInt(o.isDefault())));
        notifyTemplateDTOList.forEach(notifyTemplateDTO -> channelTemplateMap.putIfAbsent(
            notifyTemplateDTO.getChannel(), notifyTemplateDTO
        ));
        return channelTemplateMap;
    }

    private String getDisplayIdStr(ResourceScope scope) {
        return scope.getType().getValue() + ":" + scope.getId();
    }

    private String getNormalUserLanguage(ApplicationDTO applicationDTO) {
        String userLang = JobContextUtil.getUserLang();
        if (userLang == null) {
            String appLang = applicationDTO.getLanguage();
            if (CmdbConstants.APP_LANG_VALUE_ZH_CN.equals(appLang)) {
                userLang = LocaleUtils.LANG_ZH_CN;
            } else if (CmdbConstants.APP_LANG_VALUE_EN_US.equals(appLang)) {
                userLang = LocaleUtils.LANG_EN_US;
            } else {
                log.warn("appLang=null, use zh_CN, appId={}", applicationDTO.getId());
                userLang = LocaleUtils.LANG_ZH_CN;
            }
        }
        return LocaleUtils.getNormalLang(userLang);
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

    public ServiceNotificationMessage getNotificationMessageFromTemplate(
        Long appId,
        NotifyTemplateDTO templateDTO,
        Map<String, String> variablesMap
    ) {
        ApplicationDTO applicationDTO = applicationDAO.getAppById(appId);
        if (applicationDTO == null) {
            log.error("cannot find applicationInfo of appId:{}", appId);
            return null;
        }
        String appName = applicationDTO.getName();
        String normalLang = getNormalUserLanguage(applicationDTO);
        //国际化
        Pair<String, String> titleAndContent = getTitleAndContentByLanguage(templateDTO, normalLang);
        String title = titleAndContent.getLeft();
        String content = titleAndContent.getRight();
        //添加默认变量
        ResourceScope scope = applicationDTO.getScope();
        variablesMap.putIfAbsent("BASE_HOST", jobManageConfig.getJobWebUrl());
        variablesMap.putIfAbsent("APP_ID", getDisplayIdStr(scope));
        variablesMap.putIfAbsent("task.bk_biz_id", scope.getId());
        variablesMap.putIfAbsent("APP_NAME", appName);
        variablesMap.putIfAbsent("task.bk_biz_name", appName);
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

    public ServiceNotificationMessage getNotificationMessage(Long appId, String templateCode, String channel,
                                                             Map<String, String> variablesMap) {
        //1.查出自定义模板信息
        NotifyTemplateDTO notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(channel, templateCode, false);
        if (notifyTemplateDTO == null) {
            //2.未配置自定义模板则使用默认模板
            notifyTemplateDTO = notifyTemplateDAO.getNotifyTemplate(channel, templateCode, true);
        }
        if (notifyTemplateDTO == null) {
            log.warn("Cannot find template of templateCode:{},channel:{}, plz config a default template",
                templateCode, channel);
            return null;
        }
        return getNotificationMessageFromTemplate(appId, notifyTemplateDTO, variablesMap);
    }
}
