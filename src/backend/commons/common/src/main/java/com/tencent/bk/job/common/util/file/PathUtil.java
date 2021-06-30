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

package com.tencent.bk.job.common.util.file;

import java.io.File;

public class PathUtil {
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
