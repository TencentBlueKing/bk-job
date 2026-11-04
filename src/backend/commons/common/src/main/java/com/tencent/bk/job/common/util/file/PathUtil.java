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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class PathUtil {

    /**
     * 将相对路径拼接到 baseDir 下，并保证拼接结果未越出 baseDir。
     * 相对路径中若含有 ..、绝对路径等可穿越的内容，抛出 InvalidParamException。
     *
     * @param baseDirPath   允许访问的根目录
     * @param relativePaths 逐级拼接的相对路径，可以是单个路径分量，也可以是含分隔符的多级路径；
     *                      以分隔符开头的路径按 baseDir 下的相对路径处理
     * @return 位于 baseDir 之内的目标文件
     */
    public static File resolveSafely(String baseDirPath, String... relativePaths) {
        if (StringUtils.isBlank(baseDirPath) || baseDirPath.indexOf('\0') >= 0 || relativePaths == null) {
            log.warn("PathUtil.resolveSafely rejected: reason=blank or invalid baseDirPath/relativePaths, "
                + "baseDirPath={}", LogUtil.sanitizeForLog(baseDirPath, 256));
            throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
        }

        String normalizedBaseDirPath = baseDirPath
            .replace('/', File.separatorChar)
            .replace('\\', File.separatorChar);
        Path basePath;
        try {
            basePath = Paths.get(normalizedBaseDirPath).toAbsolutePath().normalize();
        } catch (Exception e) {
            log.warn("PathUtil.resolveSafely rejected: reason=invalid baseDirPath syntax, baseDirPath={}",
                LogUtil.sanitizeForLog(baseDirPath, 256), e);
            throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
        }
        if (Files.exists(basePath)) {
            try {
                // 目录已存在时解析符号链接；目录不存在时允许上层后续创建。
                basePath = basePath.toRealPath();
            } catch (IOException e) {
                log.warn("PathUtil.resolveSafely rejected: reason=failed to resolve real path of baseDirPath, "
                    + "baseDirPath={}", LogUtil.sanitizeForLog(baseDirPath, 256), e);
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }
        }

        Path targetPath = basePath;
        for (String relativePath : relativePaths) {
            if (relativePath != null && relativePath.indexOf('\0') >= 0) {
                log.warn("PathUtil.resolveSafely rejected: reason=relativePath contains null byte, "
                        + "baseDirPath={}, relativePath={}",
                    LogUtil.sanitizeForLog(baseDirPath, 256),
                    LogUtil.sanitizeForLog(relativePath, 256));
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }
            String pathWithoutLeadingSeparator = trimLeadingSeparator(relativePath);
            if (StringUtils.isBlank(pathWithoutLeadingSeparator)) {
                log.warn("PathUtil.resolveSafely rejected: reason=relativePath is blank, "
                        + "baseDirPath={}, relativePath={}",
                    LogUtil.sanitizeForLog(baseDirPath, 256),
                    LogUtil.sanitizeForLog(relativePath, 256));
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }
            String normalizedRelativePath = pathWithoutLeadingSeparator
                .replace('/', File.separatorChar)
                .replace('\\', File.separatorChar);

            final Path relative;
            try {
                relative = Paths.get(normalizedRelativePath);
            } catch (Exception e) {
                log.warn("PathUtil.resolveSafely rejected: reason=invalid relativePath syntax, "
                        + "baseDirPath={}, relativePath={}",
                    LogUtil.sanitizeForLog(baseDirPath, 256),
                    LogUtil.sanitizeForLog(relativePath, 256), e);
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }
            if (relative.isAbsolute() || containsParentDirName(relative)) {
                log.warn("PathUtil.resolveSafely rejected: reason=relativePath is absolute or contains '..', "
                        + "baseDirPath={}, relativePath={}",
                    LogUtil.sanitizeForLog(baseDirPath, 256),
                    LogUtil.sanitizeForLog(relativePath, 256));
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }

            targetPath = targetPath.resolve(relative).normalize();
            if (!targetPath.startsWith(basePath)) {
                log.warn("PathUtil.resolveSafely rejected: reason=resolved path escapes baseDirPath, "
                        + "baseDirPath={}, relativePath={}, resolvedPath={}",
                    LogUtil.sanitizeForLog(baseDirPath, 256),
                    LogUtil.sanitizeForLog(relativePath, 256),
                    targetPath);
                throw new InvalidParamException(ErrorCode.ILLEGAL_FILE);
            }
        }
        return targetPath.toFile();
    }

    /**
     * 去掉路径开头的分隔符。存量数据中的文件路径形如 /import/admin/xxx.json，
     * 语义上是相对存储根目录的路径，而非文件系统的绝对路径。
     */
    private static String trimLeadingSeparator(String path) {
        if (path == null) {
            return null;
        }
        int index = 0;
        while (index < path.length() && (path.charAt(index) == '/' || path.charAt(index) == '\\')) {
            index++;
        }
        return path.substring(index);
    }

    /**
     * 判断路径中是否存在 .. 分量。逐个分量比较，避免误判 a..b 这类合法文件名。
     */
    private static boolean containsParentDirName(Path path) {
        for (Path name : path) {
            if ("..".equals(name.toString())) {
                return true;
            }
        }
        return false;
    }

    public static String joinFilePath(String path1, String path2) {
        if (path1 == null) return path2;
        if (path2 == null) return path1;
        path1 = path1.replace("/", File.separator);
        path1 = path1.replace("\\", File.separator);
        path2 = path2.replace("/", File.separator);
        path2 = path2.replace("\\", File.separator);
        while (path1.endsWith(File.separator)) {
            path1 = path1.substring(0, path1.length() - 1);
        }
        while (path2.startsWith(File.separator)) {
            path2 = path2.substring(1);
        }
        return path1 + File.separator + path2;
    }

    public static String getFileNameByPath(String filePath) {
        if (filePath == null) {
            return null;
        }
        String separator = "/";
        if (!filePath.contains("/") && filePath.contains("\\")) {
            separator = "\\";
        }
        int i = filePath.lastIndexOf(separator);
        return filePath.substring(i + 1);
    }
}
