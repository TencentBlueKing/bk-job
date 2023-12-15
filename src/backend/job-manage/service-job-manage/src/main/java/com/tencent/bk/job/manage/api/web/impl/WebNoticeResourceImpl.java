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

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.manage.api.web.WebNoticeResource;
import com.tencent.bk.job.manage.model.web.vo.notice.AnnouncementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class WebNoticeResourceImpl implements WebNoticeResource {

    @Override
    public Response<List<AnnouncementVO>> getCurrentAnnouncements(String username) {
        // Mock数据
        List<AnnouncementVO> resultList = new ArrayList<>();
        AnnouncementVO announcementVO = new AnnouncementVO();
        announcementVO.setTitle("这是通知标题");
        announcementVO.setContent("这是通知内容");
        announcementVO.setAnnounceType("event");
        announcementVO.setStartTime("2023-08-28T15:26:59.027000+08:00");
        announcementVO.setEndTime("2024-09-28T15:26:59.027000+08:00");
        resultList.add(announcementVO);
        announcementVO = new AnnouncementVO();
        announcementVO.setTitle("这是通知标题2");
        announcementVO.setContent("这是通知内容2");
        announcementVO.setAnnounceType("announce");
        announcementVO.setStartTime("2023-09-28T15:26:59.027000+08:00");
        announcementVO.setEndTime("2024-10-28T15:26:59.027000+08:00");
        resultList.add(announcementVO);
        return Response.buildSuccessResp(resultList);
    }
}
