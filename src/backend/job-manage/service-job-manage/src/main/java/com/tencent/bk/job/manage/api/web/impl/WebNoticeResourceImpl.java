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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.i18n.locale.BkConsts;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.notice.IBkNoticeClient;
import com.tencent.bk.job.common.notice.config.BkNoticeProperties;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.web.WebNoticeResource;
import com.tencent.bk.job.manage.model.web.vo.notice.AnnouncementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class WebNoticeResourceImpl implements WebNoticeResource {

    private final IBkNoticeClient bkNoticeClient;
    private final BkNoticeProperties bkNoticeProperties;

    @Autowired
    public WebNoticeResourceImpl(@Autowired(required = false) IBkNoticeClient bkNoticeClient,
                                 BkNoticeProperties bkNoticeProperties) {
        this.bkNoticeClient = bkNoticeClient;
        this.bkNoticeProperties = bkNoticeProperties;
    }

    @Override
    public Response<List<AnnouncementVO>> getCurrentAnnouncements(String username, Integer offset, Integer limit) {
        if (!bkNoticeProperties.isEnabled()) {
            log.info("bkNotice not enabled, please check config value: bkNotice.enabled");
            return Response.buildSuccessResp(Collections.emptyList());
        }
        String userLang = JobContextUtil.getUserLang();
        String bkLang = LocaleUtils.getBkLang(userLang);
        if (bkLang == null) {
            bkLang = BkConsts.HEADER_VALUE_LANG_EN;
        }
        List<AnnouncementVO> resultList = bkNoticeClient.getCurrentAnnouncements(bkLang, offset, limit).stream()
            .map(AnnouncementVO::fromDTO)
            .collect(Collectors.toList());
        return Response.buildSuccessResp(resultList);
    }
}
