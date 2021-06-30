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

package com.tencent.bk.job.execute.engine.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 文件路径解析器
 */
public class FilePathUtils {
    /**
     * 解析全路径文件的文件名和路径
     */
    public static Pair<String, String> parseDirAndFileName(String fullFilePath) {
        String dir;
        String fileName;
        int lastFilePathSeparatorEndIndex = Math.max(fullFilePath.lastIndexOf('/'), fullFilePath.lastIndexOf('\\'));
        if (lastFilePathSeparatorEndIndex == -1) {
            fileName = fullFilePath;
            dir = "";
            return new ImmutablePair<>(dir.trim(), fileName.trim());
        } else if (lastFilePathSeparatorEndIndex == 0) {
            fileName = fullFilePath.substring(1);
            dir = fullFilePath.substring(0, 1);
            return new ImmutablePair<>(dir.trim(), fileName.trim());
        }

        int lastFilePathSeparatorStartIndex = lastFilePathSeparatorEndIndex;
        for (int index = lastFilePathSeparatorEndIndex - 1; index >= 0; index--) {
            if (!isFilePathSeparator(fullFilePath.charAt(index))) {
                break;
            } else {
                lastFilePathSeparatorStartIndex = index;
            }
        }

        fileName = fullFilePath.substring(lastFilePathSeparatorEndIndex + 1);
        dir = fullFilePath.substring(0, lastFilePathSeparatorStartIndex);
        return new ImmutablePair<>(dir.trim(), fileName.trim());
    }

    private static boolean isFilePathSeparator(char ch) {
        return ch == '/' || ch == '\\';
    }

    /**
     * 标准化GSE返回的文件路径
     *
     * @param filePath GSE返回的文件路径
     * @return 标准化文件路径
     */
    public static String standardizedGSEFilePath(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return "";
        }
        // GSE BUG
        // GSE对于Windows的路径，不管下发参数dir中是否包含路径分隔符，都会默认加上/,比如C:\Users\/；这里需要对路径进行标准化
        Pair<String, String> dirAndFileName = FilePathUtils.parseDirAndFileName(filePath);
        String dir = dirAndFileName.getLeft();
        String fileName = dirAndFileName.getRight();
        int lastBackSlashIndex = dir.lastIndexOf("\\");
        if (lastBackSlashIndex == -1) {
            return dir + fileName;
        }
        // "C:\/"这种路径，需要删除掉最后一个GSE添加的"/"
        if (lastBackSlashIndex == dir.length() - 2) {
            return dir.substring(0, lastBackSlashIndex + 1) + fileName;
        } else {
            return dir + fileName;
        }
    }

    /**
     * 构造文件路径
     *
     * @param targetPath 目标路径
     * @param fileName   文件名
     * @return 文件路径
     */
    public static String appendFileName(String targetPath, String fileName) {
        String standardDirPath = standardizedDirPath(targetPath);
        if (StringUtils.isEmpty(fileName)) {
            return standardDirPath;
        } else {
            return standardDirPath + fileName;
        }
    }

    /**
     * 构造目录路径
     *
     * @param targetPath 目标路径
     * @param dirName    目录名
     * @return 文件路径
     */
    public static String appendDirName(String targetPath, String dirName) {
        String standardDirPath = standardizedDirPath(targetPath);
        String pathSeparator = targetPath.startsWith("/") ? "/" : "\\";
        if (StringUtils.isEmpty(dirName)) {
            return standardDirPath;
        } else {
            return standardDirPath + dirName + pathSeparator;
        }
    }

    /**
     * 从目录路径解析目录名
     *
     * @param dirPath 目录路径
     * @return 目录名
     */
    public static String parseDirName(String dirPath) {
        String[] paths = dirPath.split("[/\\\\]");
        if (paths.length == 0) {
            return dirPath;
        }
        for (int i = paths.length - 1; i >= 0; i--) {
            if (StringUtils.isNotEmpty(paths[i])) {
                return paths[i];
            }
        }
        return "";
    }

    /**
     * 标准化目录路径
     *
     * @param dir 目录路径
     * @return 标准化之后的目录路径
     */
    public static String standardizedDirPath(String dir) {
        if (StringUtils.isEmpty(dir)) {
            return "";
        }
        String standardDir = dir;
        if (!dir.endsWith("/") && !dir.endsWith("\\")) {
            boolean isLinuxPath = dir.startsWith("/");
            if (isLinuxPath) {
                standardDir = dir + "/";
            } else {
                // 如果使用/作为windows路径分隔符，那么路径后缀加上/；否则按照通用的习惯，加上更为常见的\作为windows的分隔符
                if (dir.contains("/")) {
                    standardDir += "/";
                } else {
                    standardDir += "\\";
                }
            }
        }
        return standardDir;
    }
}
