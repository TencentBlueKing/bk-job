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

package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @since 27/9/2019 17:44
 */
public class TagUtilsTest {

    @Test
    public void buildDbTag() {
        assertEquals(TagUtils.buildDbTag(1L), "<1>");
    }

    @Test
    public void buildDbTagList() {
        assertEquals(TagUtils.buildDbTagList(Arrays.asList(1L, 2L, 3L)), "<1>,<2>,<3>");
        assertEquals(TagUtils.buildDbTagList(Collections.singletonList(1L)), "<1>");
        assertNull(TagUtils.buildDbTagList(Arrays.asList(-1L, -2L, -3L)));
        assertNull(TagUtils.buildDbTagList(null));
    }

    @Test
    public void decodeDbTag() {
        assertEquals(TagUtils.decodeDbTag("<0>,<1>,<2>,<3>"), Arrays.asList(1L, 2L, 3L));
        assertEquals(TagUtils.decodeDbTag(""), Collections.emptyList());
        assertEquals(TagUtils.decodeDbTag(" "), Collections.emptyList());
        assertEquals(TagUtils.decodeDbTag(null), Collections.emptyList());
        assertEquals(TagUtils.decodeDbTag(",<>,,,,<0>,,<1>,<2>,,,,"), Arrays.asList(1L, 2L));
        assertEquals(TagUtils.decodeDbTag("asdsadasd"), Collections.emptyList());
        assertEquals(TagUtils.decodeDbTag(",<>,,,,<0>,,<1>,<2>,,<-3>,,"), Arrays.asList(1L, 2L));
    }
}
