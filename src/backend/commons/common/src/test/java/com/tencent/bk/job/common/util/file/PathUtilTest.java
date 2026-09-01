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

        // 存量数据中的文件路径以分隔符开头，语义上仍是相对存储根目录的路径
        File leadingSeparator = PathUtil.resolveSafely(baseDir, File.separator + "import" + File.separator + "admin"
            + File.separator + "task.json");
        assertThat(leadingSeparator.getPath())
            .isEqualTo(tempDir.resolve("import").resolve("admin").resolve("task.json").toString());

        // 文件名中间的 .. 不构成路径穿越
        File dotsInFileName = PathUtil.resolveSafely(baseDir, "task..json");
        assertThat(dotsInFileName.getPath()).isEqualTo(tempDir.resolve("task..json").toString());
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
        assertThatThrownBy(() -> PathUtil.resolveSafely(baseDir, File.separator + ".." + File.separator
            + "outside.txt")).isInstanceOf(InvalidParamException.class);
    }

    @Test
    void resolveSafelyKeepsAbsoluteLikePathInsideBaseDir(@TempDir Path tempDir) {
        // 形如 /etc/passwd 的输入无法与存量相对路径区分，统一按 baseDir 下的相对路径处理，结果不会越出 baseDir
        File target = PathUtil.resolveSafely(tempDir.toString(), File.separator + "etc" + File.separator + "passwd");

        assertThat(target.toPath()).isEqualTo(tempDir.resolve("etc").resolve("passwd"));
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
