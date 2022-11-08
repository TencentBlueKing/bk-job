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

package com.tencent.bk.job.upgrader.task;

import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.model.job.UpdateAppIdForWhiteIpMigrationReq;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

/**
 * IP白名单AppId更新，全业务ID->代表所有业务的ID
 */
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class),
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.7.0",
    targetExecuteTime = ExecuteTimeEnum.AFTER_UPDATE_JOB,
    priority = 1)
public class UpdateAppIdForWhiteIpMigrationTask extends BaseUpgradeTask {

    public UpdateAppIdForWhiteIpMigrationTask(Properties properties) {
        super(properties);
    }

    @Override
    public boolean execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        try {
            // 1.迁移IP白名单数据的appId
            log.info("job.manage.updateAppIdForWhiteIpMigrationTask start ...");
            Response<String> manageResp = post(buildMigrationTaskUrl(getJobManageUrl(),
                "/migration/action/updateAppIdForWhiteIpMigrationTask"),
                JsonUtils.toJson(new UpdateAppIdForWhiteIpMigrationReq(false)));
            log.info("job.manage.updateAppIdForWhiteIpMigrationTask done, result: {}", manageResp);
            return manageResp.isSuccess();
        } catch (Throwable e) {
            log.error("UpdateAppIdForWhiteIpMigrationTask fail", e);
            return false;
        }
    }
}
