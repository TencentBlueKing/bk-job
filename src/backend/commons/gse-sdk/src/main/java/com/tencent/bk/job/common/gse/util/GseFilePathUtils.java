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

package com.tencent.bk.job.common.gse.util;

import com.tencent.bk.job.common.util.FilePathUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * GSE文件路径解析器
 */
public class GseFilePathUtils {

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
}
