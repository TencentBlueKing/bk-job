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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class FileUtil {

    /**
     * 创建文件的父目录
     *
     * @param path 文件路径
     * @return 最终文件父目录是否存在
     */
    private static boolean checkOrCreateParentDirsForFile(String path) {
        File theFile = new File(path);
        File parentDir = theFile.getParentFile();
        if (!parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                log.warn(
                    "mkdir parent dir fail!dir:{}",
                    parentDir.getAbsolutePath()
                );
                return false;
            }
            if (!parentDir.setWritable(true, false)) {
                log.warn(
                    "set parent dir writeable fail!dir:{}",
                    parentDir.getAbsolutePath()
                );
                return false;
            }
        }
        return true;
    }

    /**
     * 将字节数组数据保存至文件
     *
     * @param path         文件路径
     * @param contentBytes 字节数组
     * @return 是否保存成功
     */
    public static boolean saveBytesToFile(String path, byte[] contentBytes) {
        if (!checkOrCreateParentDirsForFile(path)) {
            return false;
        }
        boolean isSuccess = false;
        if (contentBytes == null || contentBytes.length == 0) {
            return false;
        }
        try (FileOutputStream out = new FileOutputStream(path)) {
            out.write(contentBytes);
            out.flush();
            File file = new File(path);
            isSuccess = file.setExecutable(true, false);
        } catch (IOException e) {
            log.warn("Save fail", e);
        }

        return isSuccess;
    }

    /**
     * 将base64编码字符串的内容保存至文件
     *
     * @param path      文件路径
     * @param base64Str base64编码字符串内容
     * @return 是否保存成功
     */
    public static boolean saveBase64StrToFile(String path, String base64Str) {
        byte[] contentBytes = Base64Util.decodeContentToByte(base64Str);
        File theFile = new File(path);
        if (theFile.exists() && theFile.isFile()) {
            if (!theFile.delete()) {
                log.warn(
                    "delete old file fail!dir:{}",
                    theFile.getAbsolutePath()
                );
                return false;
            }
        }
        if (!saveBytesToFile(path, contentBytes)) {
            log.warn(
                "save file failed!fileName:{}",
                theFile.getAbsolutePath()
            );
            return false;
        }
        return true;
    }

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

    /**
     * 将 InputStream 流中内容写入文件
     *
     * @param ins        流
     * @param targetPath 目标文件路径
     * @return 文件Md5
     * @throws InterruptedException 写入过程中被中断异常
     */
    public static String writeInsToFile(InputStream ins, String targetPath) throws InterruptedException {
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
            int length;
            while ((length = ins.read(content)) > 0) {
                fos.write(content, 0, length);
                Thread.sleep(0);
            }
            closeFos(fos);
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
            closeStreams(ins, fis);
        }
    }

    /**
     * 将 InputStream 流中内容写入文件
     *
     * @param ins        流
     * @param targetPath 目标文件路径
     * @param fileSize   文件大小
     * @param speed      用于观测写入速度
     * @param process    用于观测写入进度
     * @return 文件Md5
     * @throws InterruptedException 写入过程中被中断异常
     */
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
            closeFos(fos);
            log.info("targetPath:{},totalLength={},fileSize={}", targetPath, totalLength, fileSize);
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
            closeStreams(ins, fis);
        }
    }

    private static void closeFos(FileOutputStream fos) {
        if (fos != null) {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                log.error("Fail to close fos", e);
            }
        }
    }

    private static void closeStreams(
        InputStream ins,
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
    }

    /**
     * 删除空目录
     *
     * @param directory 目录文件
     */
    public static void deleteEmptyDirectory(File directory) {
        if (directory == null) {
            log.warn("Directory is null!");
            return;
        }

        if (isEmpty(directory.toPath())) {
            boolean delete = directory.delete();
            if (delete) {
                log.info("Delete empty directory {}", directory.getPath());
            } else {
                log.warn("Delete directory {} failed!", directory.getPath());
            }
        } else {
            log.debug("Directory {} not empty!", directory.getPath());
        }
    }

    private static boolean isEmpty(Path path) {
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            } catch (IOException e) {
                return false;
            }
        }

        return false;
    }
}
