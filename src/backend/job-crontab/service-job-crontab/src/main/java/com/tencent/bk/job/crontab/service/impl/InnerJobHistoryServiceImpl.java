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

package com.tencent.bk.job.crontab.service.impl;

import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.dao.InnerCronJobHistoryDAO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobHistoryDTO;
import com.tencent.bk.job.crontab.service.InnerJobHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @since 21/2/2020 22:27
 */
@Slf4j
@Component
public class InnerJobHistoryServiceImpl implements InnerJobHistoryService {

    private InnerCronJobHistoryDAO innerCronJobHistoryDAO;

    @Autowired
    public InnerJobHistoryServiceImpl(InnerCronJobHistoryDAO innerCronJobHistoryDAO) {
        this.innerCronJobHistoryDAO = innerCronJobHistoryDAO;
    }

    @Override
    public long insertHistory(String systemId, String jobKey, long scheduledFireTime) {
        return innerCronJobHistoryDAO.insertCronJobHistory(systemId, jobKey, scheduledFireTime);
    }

    @Override
    public InnerCronJobHistoryDTO getHistoryByIdAndTime(String systemId, String jobKey, long scheduledFireTime) {
        return innerCronJobHistoryDAO.getCronJobHistory(systemId, jobKey, scheduledFireTime);
    }

    @Override
    public boolean updateStatusByIdAndTime(String systemId, String jobKey, long scheduledFireTime,
                                           ExecuteStatusEnum status) {
        return innerCronJobHistoryDAO.updateStatusByIdAndTime(systemId, jobKey, scheduledFireTime, status.getValue());
    }

    @Override
    public int cleanHistory(long cleanBefore, boolean cleanAll) {
        return innerCronJobHistoryDAO.cleanHistory(cleanBefore, cleanAll);
    }
}
