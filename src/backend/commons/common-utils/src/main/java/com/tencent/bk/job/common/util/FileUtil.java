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

package com.tencent.bk.job.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class FileUtil {

    private static void tryToCreateFile(File file) {
        try {
            boolean flag = file.mkdirs();
        } catch (Exception e) {
            //创建目录失败
            String msg = String.format("Fail to create dir:%s", file.getAbsolutePath());
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public static String writeInsToFile(InputStream ins, String targetPath, Long fileSize, AtomicInteger speed,
                                        AtomicInteger process) throws InterruptedException {
        File file = new File(targetPath);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            tryToCreateFile(parentFile);
        }
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            fos = new FileOutputStream(targetPath);
            int batchSize = 20480;
            byte[] content = new byte[batchSize];
            long totalLength = 0;
            int length = 0;
            long lastSpeedWatchTime = System.currentTimeMillis();
            long lastSpeedWatchFileSize = totalLength;
            long currentSpeedWatchTime;
            long timeDelta = 0;
            while ((length = ins.read(content)) > 0) {
                fos.write(content, 0, length);
                totalLength += length;
                process.set((int) (totalLength / (float) fileSize * 100));
                currentSpeedWatchTime = System.currentTimeMillis();
                timeDelta = currentSpeedWatchTime - lastSpeedWatchTime;
                if (timeDelta >= 1000) {
                    //计算速度 KB/s
                    long fileSizeDelta = totalLength - lastSpeedWatchFileSize;
                    speed.set((int) (fileSizeDelta / timeDelta));
                    lastSpeedWatchTime = currentSpeedWatchTime;
                    lastSpeedWatchFileSize = totalLength;
                    log.info("progress: {}KB/{}KB, speed: {}KB/s", totalLength / 1000, fileSize / 1000, speed.get());
                }
                Thread.sleep(0);
            }
            currentSpeedWatchTime = System.currentTimeMillis();
            timeDelta = currentSpeedWatchTime - lastSpeedWatchTime;
            long fileSizeDelta = totalLength - lastSpeedWatchFileSize;
            if (timeDelta > 0) {
                speed.set((int) (fileSizeDelta / timeDelta));
                log.info("progress: {}KB/{}KB, speed: {}KB/s", totalLength / 1000, fileSize / 1000, speed.get());
            }
            fis = new FileInputStream(targetPath);
            return DigestUtils.md5Hex(fis);
        } catch (FileNotFoundException e) {
            log.error("File not found:{}", targetPath, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            log.error("IOException occurred:{}", targetPath, e);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            log.info("Download interrupted, targetPath:{}", targetPath);
            throw e;
        } finally {
            closeStreams(ins, fos, fis);
        }
    }

    private static void closeStreams(
        InputStream ins,
        FileOutputStream fos,
        FileInputStream fis
    ) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                log.error("Fail to close fis", e);
            }
        }
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException e) {
                log.error("Fail to close ins", e);
            }
        }
        if (fos != null) {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                log.error("Fail to close fos", e);
            }
        }
    }
}
