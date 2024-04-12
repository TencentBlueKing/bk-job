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

package com.tencent.bk.job.common.task;

import com.tencent.bk.job.common.task.config.JvmDiagnosticFileClearByLastModifyTimeProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class JvmDiagnosticFileClearScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(JvmDiagnosticFileClearScheduledTasks.class);

    private final ClearJvmDiagnosticFileByLastModifyTimeTask clearJvmDiagnosticFileByLastModifyTimeTask;

    public JvmDiagnosticFileClearScheduledTasks(
        JvmDiagnosticFileClearByLastModifyTimeProperties jvmDiagnosticFileClearByLastModifyTimeProperties) {
        if (jvmDiagnosticFileClearByLastModifyTimeProperties.isEnabled()) {
            this.clearJvmDiagnosticFileByLastModifyTimeTask = new ClearJvmDiagnosticFileByLastModifyTimeTask(
                jvmDiagnosticFileClearByLastModifyTimeProperties.getKeepHours()
            );
        } else {
            this.clearJvmDiagnosticFileByLastModifyTimeTask = null;
        }
    }

    /**
     * 清理：每小时根据最后修改时间清理一次JVM诊断文件
     */
    @Scheduled(cron = "05 30 * * * ?")
    public void clearJvmDiagnosticFileByLastModifyTime() {
        if (clearJvmDiagnosticFileByLastModifyTimeTask == null) {
            log.debug("clearJvmDiagnosticFileByLastModifyTime not enabled, ignore");
            return;
        }
        logger.info("clearJvmDiagnosticFileByLastModifyTime start");
        try {
            clearJvmDiagnosticFileByLastModifyTimeTask.checkLastModifyTimeAndClear();
        } catch (Exception e) {
            logger.error("clearJvmDiagnosticFileByLastModifyTime fail", e);
        }
    }

}
