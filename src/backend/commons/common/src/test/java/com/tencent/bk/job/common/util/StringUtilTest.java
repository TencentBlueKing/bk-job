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

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringUtilTest {

    @Test
    public void replaceByRegex() {
        Map<String, String> map = new HashMap<>();
        map.put("var1", "1111111");
        assertEquals("ccc1111111aaa{{var2}}ddd1111111", StringUtil.replaceByRegex("ccc{{var1}}aaa{{var2}}ddd{{var1}}"
            , "(\\{\\{(.*?)\\}\\})", map));
    }

    @Test
    public void removePrefixOrSuffix() {
        // remove prefix
        assertEquals("AAA", StringUtil.removePrefix("sssAAA", "s"));
        assertEquals("sAAA", StringUtil.removePrefix("sssAAA", "ss"));
        assertEquals("sssAAA", StringUtil.removePrefix("sssAAA", null));
        assertEquals("sssAAA", StringUtil.removePrefix("sssAAA", "A"));
        assertEquals("", StringUtil.removePrefix("AAA", "A"));
        assertNull(StringUtil.removePrefix(null, "ss"));
        // remove suffix
        assertNull(StringUtil.removeSuffix(null, "ss"));
        assertEquals("sss", StringUtil.removeSuffix("sssAAA", "A"));
        assertEquals("sssA", StringUtil.removeSuffix("sssAAA", "AA"));
        assertEquals("sssAAA", StringUtil.removeSuffix("sssAAA", null));
        assertEquals("sssAAA", StringUtil.removeSuffix("sssAAA", "s"));
        assertEquals("", StringUtil.removeSuffix("sss", "s"));
    }
}
