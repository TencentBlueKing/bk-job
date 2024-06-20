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

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.model.migration.BkPlatformInfo;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.RequireTaskParam;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.anotation.UpgradeTaskInputParam;
import com.tencent.bk.job.upgrader.client.JobClient;
import com.tencent.bk.job.upgrader.task.param.JobManageServerAddress;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

/**
 * 3.9.3版本开始，改为使用基于BK-REPO的全局配置统一方案，该任务输出平台信息全局配置base.js文件，以便于让用户迁移之前已配置的数据：
 * 将该任务生成的base.js文件导入BK-REPO的blueking项目下bk-config仓库下的bk_job目录下，覆盖原有的base.js即可。
 */
@SuppressWarnings("unused")
@Slf4j
@RequireTaskParam(value = {
    @UpgradeTaskInputParam(value = JobManageServerAddress.class),
})
@UpgradeTask(
    dataStartVersion = "3.0.0.0",
    targetVersion = "3.9.3-alpha.1",
    targetExecuteTime = ExecuteTimeEnum.AFTER_UPDATE_JOB)
public class ExportGlobalSettingsTask extends BaseUpgradeTask {

    private JobClient jobManageClient;

    @SuppressWarnings("unused")
    public ExportGlobalSettingsTask(Properties properties) {
        super(properties);
    }

    @Override
    public void init() {
        super.init();
        jobManageClient = getJobManageClient();
    }

    @Override
    public boolean execute(String[] args) {
        log.info(getName() + " for version " + getTargetVersion() + " begin to run...");
        try {
            BkPlatformInfo bkPlatformInfo = jobManageClient.getBkPlatformInfo();
            String bkPlatformInfoStr = JsonUtils.toJson(bkPlatformInfo);
            String fileName = "base.js";
            String baseJsStr = "__platCfgCallback__(" + bkPlatformInfoStr + ")";
            log.info("Exported " + fileName + ":");
            log.info(baseJsStr);
            File outputFile = new File(fileName);
            if (!outputFile.exists()) {
                boolean result = outputFile.createNewFile();
                if (!result) {
                    log.error("Fail to create file {}", fileName);
                }
            }
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
                bw.write(baseJsStr);
            }
            log.info("BkPlatformInfo has been export to file {}, please check current dir", fileName);
            return true;
        } catch (Throwable e) {
            log.error("ExportGlobalSettingsTask fail", e);
            return false;
        }
    }
}
