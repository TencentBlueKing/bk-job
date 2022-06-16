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

import com.tencent.bk.job.common.gse.util.FilePathUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FilePathUtilsTest {
    @Test
    public void testParseDirAndFileName() {
        Pair<String, String> fileNameAndDir = FilePathUtils.parseDirAndFileName("/tmp/1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("/tmp");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("/");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("/");
        assertThat(fileNameAndDir.getRight()).isEqualTo("");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("/tmp/");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("/tmp");
        assertThat(fileNameAndDir.getRight()).isEqualTo("");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("/tmp//test//1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("/tmp//test");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("C:\\");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("C:");
        assertThat(fileNameAndDir.getRight()).isEqualTo("");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("C:\\1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("C:");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("C:\\/1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("C:");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("C:\\test\\REGEX:*.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("C:\\test");
        assertThat(fileNameAndDir.getRight()).isEqualTo("REGEX:*.log");

        fileNameAndDir = FilePathUtils.parseDirAndFileName("/1.log");
        assertThat(fileNameAndDir.getLeft()).isEqualTo("/");
        assertThat(fileNameAndDir.getRight()).isEqualTo("1.log");
    }

    @Test
    void testParseDirName() {
        String dirPath = "/tmp/test/";
        String dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("test");

        dirPath = "/tmp/test";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("test");

        dirPath = "C:\\Program Files\\";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("Program Files");

        dirPath = "C:\\Program Files";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("Program Files");

        dirPath = "/tmp///test//";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("test");

        dirPath = "/tmp";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("tmp");

        dirPath = "/";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("/");

        dirPath = "C:\\";
        dirName = FilePathUtils.parseDirName(dirPath);
        assertThat(dirName).isEqualTo("C:");
    }
}
