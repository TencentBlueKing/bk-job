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

package com.tencent.bk.job.file.worker.service;

import com.tencent.bk.job.common.exception.InvalidParamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DownloadFileTask 下载路径安全解析测试")
class DownloadFileTaskTest {

    @Test
    @DisplayName("合法的 taskId、filePath 与 filePrefix 应解析到 workspace/taskId 目录内")
    void shouldResolveValidDownloadPathInsideWorkspace(@TempDir Path tempDir) throws IOException {
        Path realBaseDir = tempDir.toRealPath();
        String baseDir = tempDir.toString();

        String pathWithPrefix = DownloadFileTask.resolveDownloadPathSafely(
            baseDir, "task_01", "/bucket/dir/test.txt", "prefix_1-"
        );
        assertThat(new File(pathWithPrefix).toPath())
            .isEqualTo(realBaseDir.resolve("task_01").resolve("bucket").resolve("dir").resolve("prefix_1-test.txt"));

        String pathWithoutPrefix = DownloadFileTask.resolveDownloadPathSafely(
            baseDir, "task_01", "test.txt", null
        );
        assertThat(new File(pathWithoutPrefix).toPath())
            .isEqualTo(realBaseDir.resolve("task_01").resolve("test.txt"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../etc/cron.d/x",
        "../",
        "..\\",
        "/etc/passwd",
        "a/b",
        "a\\b",
        "a..b",
        "prefix\0"
    })
    @DisplayName("含路径穿越或非法字符的 filePrefix 应直接抛出 InvalidParamException")
    void shouldRejectTraversalFilePrefix(String maliciousPrefix, @TempDir Path tempDir) {
        assertThatThrownBy(() -> DownloadFileTask.resolveDownloadPathSafely(
            tempDir.toString(), "task_01", "bucket/a.txt", maliciousPrefix
        )).isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../etc/passwd",
        "..\\..\\windows\\win.ini",
        "bucket/../../../etc/passwd",
        "bucket\\..\\..\\..\\windows\\win.ini",
        "..",
        "bucket/..",
        "bucket/.",
        "bucket/a\0b.txt",
        "/",
        "\\",
        "   "
    })
    @DisplayName("含路径穿越或非法文件名的 filePath 应直接抛出 InvalidParamException")
    void shouldRejectTraversalFilePath(String maliciousFilePath, @TempDir Path tempDir) {
        assertThatThrownBy(() -> DownloadFileTask.resolveDownloadPathSafely(
            tempDir.toString(), "task_01", maliciousFilePath, "safe_"
        )).isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "..",
        "../outside",
        "..\\outside",
        "a/../../outside",
        "a\\..\\..\\outside",
        "task\0id"
    })
    @DisplayName("含路径穿越的 taskId 应直接抛出 InvalidParamException")
    void shouldRejectTraversalTaskId(String maliciousTaskId, @TempDir Path tempDir) {
        assertThatThrownBy(() -> DownloadFileTask.resolveDownloadPathSafely(
            tempDir.toString(), maliciousTaskId, "bucket/a.txt", "safe_"
        )).isInstanceOf(InvalidParamException.class);
    }
}
