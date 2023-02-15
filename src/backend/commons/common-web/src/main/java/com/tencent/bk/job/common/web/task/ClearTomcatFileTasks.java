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

package com.tencent.bk.job.common.web.task;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 清理内置tomcat文件
 */
@Slf4j
@EnableScheduling
@Component
@ConditionalOnProperty(value = "tomcat.file.clear-enabled", havingValue = "true")
public class ClearTomcatFileTasks {

    @Value("${server.port}")
    private int port;

    @Value("${server.tomcat.basedir:}")
    private String baseDir;

    @Value("${tomcat.file.expired.keep-days:10}")
    private int days;

    /**
     * 凌晨两点清理tomcat长时间不使用的临时文件：
     * 如果自定义目录，需包含端口信息，如：/tmp/tomcat-job-${spring.application.name}-${server.port}
     * 未配置临时目录，则清理默认路径/tmp/tomcat.port.***
     */
    @Scheduled(cron = "${tomcat.file.expired.clear-cron:0 0 2 * * ?}")
    public void clearTomcatTmpExpiredFile() {
        log.info(Thread.currentThread().getId() + ":clearTomcatTmpExpiredFile start");
        try {
            String path = baseDir;
            if (StringUtils.isEmpty(path)) {
                path = System.getProperty("catalina.home");
            }
            if (StringUtils.isNotEmpty(path)) {
                String[] dirs = path.split(File.pathSeparator);
                if (dirs[dirs.length - 1].contains(port + "")) {
                    deleteExpiredFile(new File(path));
                }
            }
            log.info(Thread.currentThread().getId() + ":clearTomcatTmpExpiredFile end");
        } catch (Exception e) {
            log.error("clearTomcatExpiredFile fail", e);
        }
    }

    private void deleteExpiredFile(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isFile() && isExpired(files[i].lastModified())) {
                    files[i].delete();
                } else if (files[i].isDirectory()) {
                    deleteExpiredFile(files[i]);
                }
            }
        }
    }

    private boolean isExpired(long lastTime) {
        long currTime = System.currentTimeMillis();
        long diff = currTime - lastTime;
        long thDay = days * 24 * 60 * 60 * 1000;
        if (diff > thDay) {
            return true;
        }
        return false;
    }
}
