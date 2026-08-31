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

import com.tencent.bk.job.common.exception.InvalidParamException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathUtilTest {

    @Test
    void resolveSafelyAcceptsPathInsideBaseDir(@TempDir Path tempDir) {
        String baseDir = tempDir.toString();

        File single = PathUtil.resolveSafely(baseDir, "a1b2c3d4e5f6");
        assertThat(single.getParentFile()).isEqualTo(tempDir.toFile());

        File multiLevel = PathUtil.resolveSafely(baseDir, "import" + File.separator + "admin" + File.separator +
            "task.json");
        assertThat(multiLevel.getPath())
            .isEqualTo(tempDir.resolve("import").resolve("admin").resolve("task.json").toString());
    }

    @Test
    void resolveSafelyRejectsPathEscapingBaseDir(@TempDir Path tempDir) {
        String baseDir = tempDir.toString();

        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, ".."))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, ".." + File.separator + "outside.txt"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, "import", ".." + File.separator + ".."))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, tempDir.resolve("outside.txt").toString()))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, "unsafe..txt"))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    void resolveSafelyRejectsBlankInput(@TempDir Path tempDir) {
        assertThatThrownBy(() -> PathUtil.resolveSafely(tempDir.toString(), (String) null))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(tempDir.toString(), " "))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely("", "a.txt"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> PathUtil.resolveSafely(tempDir.toString(), (String[]) null))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    void resolveSafelyAcceptsMissingBaseDir(@TempDir Path tempDir) {
        Path missingBaseDir = tempDir.resolve("missing");

        File target = PathUtil.resolveSafely(missingBaseDir.toString(), "subdir", "a.txt");

        assertThat(target.toPath()).isEqualTo(missingBaseDir.resolve("subdir").resolve("a.txt"));
    }
}
