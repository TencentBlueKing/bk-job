/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.common.util.file.PathUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Date;
import java.util.Iterator;

/**
 * 根据最后修改时间清理JVM诊断文件（堆Dump文件、JVM错误日志等）
 */
@Slf4j
public class ClearJvmDiagnosticFileByLastModifyTimeTask {

    private volatile boolean clearTaskRunning = false;

    /**
     * 需要保留的时间：单位为小时
     */
    private final Integer keepHours;

    public ClearJvmDiagnosticFileByLastModifyTimeTask(Integer keepHours) {
        this.keepHours = keepHours;
    }

    /**
     * 检查最后修改时间并清理过期文件
     */
    public void checkLastModifyTimeAndClear() {
        if (keepHours == null || keepHours < 0) {
            log.info("keepHours({}) is invalid, ignore checkLastModifyTimeAndClear", keepHours);
            return;
        }
        // 获取Helm Chart Release名称
        String releaseName = getHelmChartReleaseName();
        if (StringUtils.isBlank(releaseName)) {
            log.info("application not running in container, ignore checkLastModifyTimeAndClear");
            return;
        }
        // 获取JVM诊断文件的存储路径
        String jvmDiagnosticFileDirPath = getJvmDiagnosticFileDirPath();
        if (StringUtils.isBlank(jvmDiagnosticFileDirPath)) {
            log.error("Cannot find valid jvmDiagnosticFileDirPath, ignore checkLastModifyTimeAndClear");
            return;
        }
        try {
            // 上一次清理任务还未跑完则不清理
            if (clearTaskRunning) {
                log.info("Last checkLastModifyTimeAndClear task still running, skip");
                return;
            }
            clearTaskRunning = true;
            int count = doCheckLastModifyTimeAndClear(releaseName, jvmDiagnosticFileDirPath);
            log.info("{} jvm diagnostic file cleared", count);
        } catch (Exception e) {
            log.warn("Exception when checkLastModifyTimeAndClear", e);
        } finally {
            clearTaskRunning = false;
        }
    }

    private int doCheckLastModifyTimeAndClear(String releaseName, String jvmDiagnosticFileDirPath) {
        log.debug(
            "doCheckLastModifyTimeAndClear: releaseName={}, jvmDiagnosticFileDirPath={}",
            releaseName,
            jvmDiagnosticFileDirPath
        );
        Date thresholdDate = new Date(System.currentTimeMillis() - keepHours * 3600L * 1000L);
        Iterator<File> fileIterator = FileUtils.iterateFiles(
            new File(jvmDiagnosticFileDirPath),
            new AgeFileFilter(thresholdDate),
            null
        );
        int count = 0;
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (!file.getName().startsWith(releaseName)) {
                // 只清理当前Release下Pod产生的诊断文件
                continue;
            }
            boolean result = file.delete();
            if (result) {
                count += 1;
            }
            log.info("Delete file:{}, result={}", file.getAbsolutePath(), result);
        }
        return count;
    }

    /**
     * 获取从环境变量注入的Helm Chart Release名称
     *
     * @return Helm Chart Release名称
     */
    private String getHelmChartReleaseName() {
        return System.getenv("BK_JOB_RELEASE_NAME");
    }

    /**
     * 获取JVM诊断文件所在路径
     *
     * @return JVM诊断文件所在路径
     */
    private String getJvmDiagnosticFileDirPath() {
        String storageBaseDirPath = System.getenv("BK_JOB_STORAGE_BASE_DIR");
        if (StringUtils.isBlank(storageBaseDirPath)) {
            return null;
        }
        String jvmDirName = "jvm";
        return PathUtil.joinFilePath(storageBaseDirPath, jvmDirName);
    }
}
