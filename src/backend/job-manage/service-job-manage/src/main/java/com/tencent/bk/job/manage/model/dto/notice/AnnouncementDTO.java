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

package com.tencent.bk.job.manage.model.dto.notice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.model.web.vo.notice.AnnouncementVO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@NoArgsConstructor
@Data
public class AnnouncementDTO {

    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容列表（国际化，各种语言的内容）
     */
    @JsonProperty("content_list")
    private List<ContentWithLanguage> contentList;

    /**
     * 公告类型: event(活动通知)/announce(平台公告)
     */
    @JsonProperty("announce_type")
    private String announceType;

    /**
     * 开始时间
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * 结束时间
     */
    @JsonProperty("end_time")
    private String endTime;

    @NoArgsConstructor
    @Data
    public static class ContentWithLanguage {
        /**
         * 内容
         */
        private String content;

        /**
         * 语言
         */
        private String language;
    }

    public AnnouncementVO toVO() {
        AnnouncementVO announcementVO = new AnnouncementVO();
        announcementVO.setId(id);
        announcementVO.setTitle(title);
        announcementVO.setContent(chooseI18nContent());
        announcementVO.setAnnounceType(announceType);
        announcementVO.setStartTime(startTime);
        announcementVO.setEndTime(endTime);
        return announcementVO;
    }

    private String chooseI18nContent() {
        if (CollectionUtils.isEmpty(contentList)) {
            log.warn("contentList is null empty, please check response from bk-notice");
            return null;
        }
        String currentUserLang = JobContextUtil.getUserLang();
        if (StringUtils.isBlank(currentUserLang)) {
            currentUserLang = LocaleUtils.LANG_ZH_CN;
            log.warn("currentUserLang is blank, use default {}", currentUserLang);
        }
        for (ContentWithLanguage contentWithLanguage : contentList) {
            String lang = contentWithLanguage.getLanguage();
            if (StringUtils.isBlank(lang)) {
                lang = LocaleUtils.LANG_ZH_CN;
                log.warn("language is blank, consider as default {}", lang);
            } else {
                lang = LocaleUtils.getNormalLang(lang);
            }
            if (lang.equals(currentUserLang)) {
                return contentWithLanguage.content;
            }
        }
        log.warn("Cannot find content match currentUserLang:{}, please check response from bk-notice", currentUserLang);
        return null;
    }
}
