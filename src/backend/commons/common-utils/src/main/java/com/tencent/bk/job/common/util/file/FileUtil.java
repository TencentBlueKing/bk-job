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

package com.tencent.bk.job.common.util.file;

import com.tencent.bk.job.common.util.Base64Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
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
            String msg = MessageFormatter.format(
                "File not found:{}",
                targetPath
            ).getMessage();
            log.error(msg, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            String msg = MessageFormatter.format(
                "IOException occurred:{}",
                targetPath
            ).getMessage();
            log.error(msg, e);
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
            String msg = MessageFormatter.format(
                "File not found:{}",
                targetPath
            ).getMessage();
            log.error(msg, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            String msg = MessageFormatter.format(
                "IOException occurred:{}",
                targetPath
            ).getMessage();
            log.error(msg, e);
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
     * @return 是否删除了目录
     */
    public static boolean deleteEmptyDirectory(File directory) {
        if (directory == null) {
            log.warn("Directory is null!");
            return false;
        }

        if (isEmpty(directory.toPath())) {
            boolean delete = directory.delete();
            if (delete) {
                log.info("Delete empty directory {}", directory.getPath());
            } else {
                log.warn("Delete directory {} failed!", directory.getPath());
            }
            return delete;
        } else {
            log.debug("Directory {} not empty!", directory.getPath());
            return false;
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

    /**
     * 删除文件，若删除失败则记录路径
     *
     * @param file          文件
     * @param failedPathSet 删除失败时用于记录路径的Set
     * @return 是否删除成功
     */
    public static boolean deleteFileAndRecordIfFail(File file, Set<String> failedPathSet) {
        try {
            FileUtils.deleteQuietly(file);
            return true;
        } catch (Exception e) {
            failedPathSet.add(file.getAbsolutePath());
            FormattingTuple message = MessageFormatter.format(
                "Fail to delete file {}",
                file.getAbsolutePath()
            );
            log.warn(message.getMessage(), e);
            return false;
        }
    }

    /**
     * 显示某个目录的磁盘使用信息
     *
     * @param path         目录路径
     * @param maxSizeBytes 最大可用字节数
     * @param currentSize  当前已使用的字节数
     */
    public static void showVolumeUsage(String path, long maxSizeBytes, long currentSize) {
        log.info(
            "VolumeUsage: path={},currentSize={},maxSize={}",
            path,
            FileSizeUtil.getFileSizeStr(currentSize),
            FileSizeUtil.getFileSizeStr(maxSizeBytes)
        );
    }

    /**
     * 检查指定目录下的磁盘使用是否超出限制，超出限制则清理最旧的文件
     *
     * @param maxSizeBytes  最大限制字节数
     * @param targetDirPath 目标目录路径
     * @return 被成功清理的文件数量
     */
    public static int checkVolumeAndClearOldestFiles(long maxSizeBytes, String targetDirPath) {
        return checkVolumeAndClearOldestFiles(maxSizeBytes, targetDirPath, null);
    }

    /**
     * 检查指定目录下的磁盘使用是否超出限制，超出限制则清理最旧的文件
     *
     * @param maxSizeBytes  最大限制字节数
     * @param targetDirPath 目标目录路径
     * @param exceptSuffixs 不删除的文件后缀名
     * @return 被成功清理的文件数量
     */
    public static int checkVolumeAndClearOldestFiles(long maxSizeBytes,
                                                     String targetDirPath,
                                                     Set<String> exceptSuffixs) {
        if (null == targetDirPath) {
            throw new IllegalArgumentException("TargetDirPath cannot be null");
        }
        File targetDirFile = new File(targetDirPath);
        if (!targetDirFile.exists() && log.isDebugEnabled()) {
            log.debug("TargetDir({}) not exists yet, ignore clear", targetDirPath);
            return -1;
        }
        long currentSize = FileUtils.sizeOfDirectory(targetDirFile);
        if (log.isDebugEnabled()) {
            showVolumeUsage(targetDirFile.getAbsolutePath(), maxSizeBytes, currentSize);
        }
        File[] files = targetDirFile.listFiles();
        if (files == null || files.length == 0) return 0;
        List<File> fileList = new ArrayList<>(Arrays.asList(files));
        fileList.sort(Comparator.comparingLong(File::lastModified));
        // 记录删除失败的文件，下次不再列出
        Set<String> deleteFailedFilePathSet = new HashSet<>();
        // 记录忽略的文件，下次不再列出
        Set<String> ignoredFilePathSet = new HashSet<>();
        int count = 0;
        while (currentSize > maxSizeBytes) {
            if (fileList.isEmpty()) {
                // 上一次拿到的文件列表已删完，空间依然超限，说明删除过程中又新产生了许多文件，重新列出
                files = targetDirFile.listFiles();
                if (files == null || files.length == 0) return count;
                fileList.addAll(Arrays.stream(files)
                    .filter(file -> !deleteFailedFilePathSet.contains(file.getAbsolutePath()))
                    .filter(file -> !ignoredFilePathSet.contains(file.getAbsolutePath()))
                    .collect(Collectors.toList())
                );
                fileList.sort(Comparator.comparingLong(File::lastModified));
            }
            if (fileList.isEmpty()) {
                if (!deleteFailedFilePathSet.isEmpty()) {
                    log.warn(
                        "Volume still overlimit after clear, ignoredFilePathSet={}, deleteFailedFilePathSet={}",
                        ignoredFilePathSet,
                        deleteFailedFilePathSet
                    );
                } else {
                    log.info("Volume still overlimit after clear, ignoredFilePathSet={}", ignoredFilePathSet);
                }
                return count;
            }
            File oldestFile = fileList.remove(0);
            // 符合指定后缀名的文件不删除
            if (matchSuffixs(oldestFile.getName(), exceptSuffixs)) {
                ignoredFilePathSet.add(oldestFile.getAbsolutePath());
                continue;
            }
            if (deleteFileAndRecordIfFail(oldestFile, deleteFailedFilePathSet)) {
                count += 1;
                log.info("Delete file {} because of volume overlimit", oldestFile.getAbsolutePath());
                currentSize = FileUtils.sizeOfDirectory(targetDirFile);
                showVolumeUsage(targetDirFile.getAbsolutePath(), maxSizeBytes, currentSize);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("{} files deleted because of volume overlimit", count);
        } else if (count > 0) {
            log.info("{} files deleted because of volume overlimit", count);
        }
        return count;
    }

    /**
     * 判断文件名是否以指定的某些后缀名结尾
     *
     * @param fileName 文件名
     * @param suffixs  后缀名集合
     * @return 文件名是否以后缀名集合中的任意一个结尾
     */
    private static boolean matchSuffixs(String fileName, Set<String> suffixs) {
        if (CollectionUtils.isEmpty(suffixs)) {
            return false;
        }
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        for (String suffix : suffixs) {
            if (fileName.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
