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

package com.tencent.bk.job.common.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PathTest {

    @Test
    void pathTest() {
        Path p = Path.newPath("root");
        assertThat(p.toString()).isEqualTo("root");
        p = p.child("first");
        assertThat(p.toString()).isEqualTo("root.first");
        p = p.child("second");
        assertThat(p.toString()).isEqualTo("root.first.second");
        p = p.index(0);
        assertThat(p.toString()).isEqualTo("root.first.second[0]");
        p = p.child("third");
        p = p.index(93);
        assertThat(p.toString()).isEqualTo("root.first.second[0].third[93]");
        p = p.parent();
        assertThat(p.toString()).isEqualTo("root.first.second[0].third");
        p = p.parent();
        assertThat(p.toString()).isEqualTo("root.first.second[0]");
        p = p.key("key");
        assertThat(p.toString()).isEqualTo("root.first.second[0][key]");
    }
}
