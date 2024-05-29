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

package com.tencent.bk.job.common.log.task;

import ch.qos.logback.classic.LoggerContext;
import com.tencent.bk.job.common.util.file.FileSizeUtil;
import com.tencent.bk.job.common.util.file.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * 根据磁盘使用量清理过期日志文件任务
 */
@Slf4j
public class ClearLogFileByVolumeUsageTask {

    private volatile boolean clearTaskRunning = false;

    /**
     * 最大容量字符串，单位支持B/KB/MB/GB/TB/PB
     */
    private final String maxSizeStr;

    public ClearLogFileByVolumeUsageTask(String maxSizeStr) {
        this.maxSizeStr = maxSizeStr;
    }

    /**
     * 检查磁盘容量并清理最旧的文件
     */
    public void checkVolumeAndClear() {
        if (StringUtils.isBlank(maxSizeStr)) {
            log.info("maxSizeStr is blank, ignore checkVolumeAndClear");
            return;
        }
        long maxSizeBytes = FileSizeUtil.parseFileSizeBytes(maxSizeStr);
        if (maxSizeBytes <= 0) {
            log.error("Cannot parse valid maxSizeBytes from maxSizeStr {}, ignore checkVolumeAndClear", maxSizeStr);
            return;
        }
        // 获取当前服务的日志根路径
        String appLogDirPath = getAppLogDirPath();
        if (StringUtils.isBlank(appLogDirPath)) {
            log.error("Cannot find valid appLogDirPath, ignore checkVolumeAndClear");
            return;
        }
        try {
            // 上一次清理任务还未跑完则不清理
            if (clearTaskRunning) {
                log.info("Last checkVolumeAndClear task still running, skip");
                return;
            }
            clearTaskRunning = true;
            int count = doCheckVolumeAndClear(maxSizeBytes, appLogDirPath);
            log.info("{} log file cleared", count);
        } catch (Exception e) {
            log.warn("Exception when checkVolumeAndClear", e);
        } finally {
            clearTaskRunning = false;
        }
    }

    private int doCheckVolumeAndClear(long maxSizeBytes, String appLogDirPath) {
        // 当前正在写入的日志文件不删除，防止日志采集丢失部分日志
        String notDeleteFileSuffix = ".log";
        return FileUtil.checkVolumeAndClearOldestFiles(
            maxSizeBytes,
            appLogDirPath,
            Collections.singleton(notDeleteFileSuffix)
        );
    }

    private String getAppLogDirPath() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        String propertyNameAppLogDir = "APP_LOG_DIR";
        return loggerContext.getProperty(propertyNameAppLogDir);
    }
}
