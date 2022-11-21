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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.manage.dao.whiteip.WhiteIPAppRelDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPAppRelDTO;
import com.tencent.bk.job.manage.model.migration.UpdateAppIdResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.List;

/**
 * 对IP白名单数据中的全业务ID更新为对所有业务生效的全局ID
 */
@Slf4j
@Service
public class UpdateAppIdForWhiteIpMigrationTask {

    private final WhiteIPAppRelDAO whiteIPAppRelDAO;

    @Autowired
    public UpdateAppIdForWhiteIpMigrationTask(WhiteIPAppRelDAO whiteIPAppRelDAO) {
        this.whiteIPAppRelDAO = whiteIPAppRelDAO;
    }

    private String getTaskName() {
        return "migrate_white_app_rel";
    }

    private UpdateAppIdResult getInitUpdateAppIdResult() {
        UpdateAppIdResult result = new UpdateAppIdResult(getTaskName());
        result.setTotalRecords(0);
        result.setSuccessRecords(0);
        result.setSuccess(true);
        return result;
    }

    public UpdateAppIdResult execute(boolean isDryRun) {
        StopWatch watch = new StopWatch(getTaskName());
        try {
            return updateAppIdsForWhiteIps(isDryRun, watch);
        } catch (Throwable t) {
            log.warn("Fail to update appId of whiteIps", t);
            UpdateAppIdResult result = getInitUpdateAppIdResult();
            result.setSuccess(false);
            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("Update appIds task finished, time consuming:{}", watch.prettyPrint());
        }
    }

    public UpdateAppIdResult updateAppIdsForWhiteIps(boolean isDryRun, StopWatch watch) {
        UpdateAppIdResult result = getInitUpdateAppIdResult();
        watch.start("listAppRelByAppId");
        List<WhiteIPAppRelDTO> whiteIpAppRelList =
            whiteIPAppRelDAO.listAppRelByAppId(JobConstants.DEFAULT_ALL_BIZ_SET_ID);
        watch.stop();
        result.setTotalRecords(whiteIpAppRelList.size());
        if (isDryRun) {
            for (WhiteIPAppRelDTO whiteIPAppRelDTO : whiteIpAppRelList) {
                log.info(
                    "[DryRun]update (recordId={},appId={}) to ({},{})",
                    whiteIPAppRelDTO.getRecordId(),
                    whiteIPAppRelDTO.getAppId(),
                    whiteIPAppRelDTO.getRecordId(),
                    JobConstants.PUBLIC_APP_ID
                );
            }
        } else {
            watch.start("updateAppId");
            int affectedRowNum =
                whiteIPAppRelDAO.updateAppId(JobConstants.DEFAULT_ALL_BIZ_SET_ID, JobConstants.PUBLIC_APP_ID);
            watch.stop();
            result.setSuccessRecords(affectedRowNum);
            log.info(
                "{}/{} whiteIpAppRel records app_id updated:{}->{}",
                affectedRowNum,
                whiteIpAppRelList.size(),
                JobConstants.DEFAULT_ALL_BIZ_SET_ID,
                JobConstants.PUBLIC_APP_ID
            );
        }
        return result;
    }
}
