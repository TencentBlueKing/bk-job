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

import static org.assertj.core.api.Assertions.assertThat;

public class FilterUtilsTest {

    @Test
    void testRemoveCommonPunctuation() {
        String origin = "哈哈哈这是一些汉字再加一些生僻字峄😀A-Z a-z 0-9 _ - | ! # @ $ & % ^ ~ = + .()。，（）【】「」《》";
        assertThat(FilterUtils.removeCommonPunctuations(origin)).isEqualTo("哈哈哈这是一些汉字再加一些生僻字峄😀AZaz09()。，（）【】「」《》");
    }

    @Test
    void testRemoveLetter() {
        String origin = "哈哈哈这是一些汉字再加一些生僻字峄😀asbxUASDUbasdkhjASDuaisdhasDKJhaSIDgiuasgdiuGIUDa1234567890!@#$%^&*()" +
            "_+。，（）【】「」《》";
        assertThat(FilterUtils.removeCommonLetters(origin)).isEqualTo("哈哈哈这是一些汉字再加一些生僻字峄😀1234567890!@#$%^&*()" +
            "_+。，（）【】「」《》");
    }

    @Test
    void testRemoveChinese() {
        String origin = "哈哈哈这是一些汉字再加一些生僻字峄😀asbxUASDUbasdkhjASDuaisdhasDKJhaSIDgiuasgdiuGIUDa1234567890!@#$%^&*()" +
            "_+。，（）【】「」《》";
        assertThat(FilterUtils.removeChinese(origin)).isEqualTo(
            "😀asbxUASDUbasdkhjASDuaisdhasDKJhaSIDgiuasgdiuGIUDa1234567890!@#$%^&*()_+。，（）【】「」《》");
    }

    @Test
    void testRemoveNumber() {
        String origin = "哈哈哈这是一些汉字再加一些生僻字峄😀asbxUASDUbasdkhjASDuaisdhasDKJhaSIDgiuasgdiuGIUDa1234567890!@#$%^&*()" +
            "_+。，（）【】「」《》";
        assertThat(FilterUtils.removeNumbers(origin)).isEqualTo(
            "哈哈哈这是一些汉字再加一些生僻字峄😀asbxUASDUbasdkhjASDuaisdhasDKJhaSIDgiuasgdiuGIUDa!@#$%^&*()_+。，（）【】「」《》");
    }

    @Test
    void testCheckOnlyLetterAndPunctuation() {
        String origin = "abcd1234";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin, 8)).isTrue();
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin, 7)).isFalse();
        origin = "abcd_-|!#@$&%^~=+. 1234";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin, 23)).isTrue();
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin, 22)).isFalse();
        origin = "哈哈你好😀abcd1234";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "哈哈你好1234";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "😀";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "abcd【】1234";
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(origin)).isFalse();

        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(" ")).isTrue();
        assertThat(FilterUtils.checkOnlyLetterAndNumberAndPunctuation(" ", -1)).isFalse();
    }

    @Test
    void testcheckOnlyChineseAndLetterAndNumberAndPunctuation() {
        String origin = "哈哈哈峄abcd1234";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 16)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 15)).isFalse();
        origin = "哈哈哈峄abcd_-|!#@$&%^~=+. 1234";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 31)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 30)).isFalse();
        origin = "哈哈你好😀abcd1234";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "哈哈你好";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 8)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 7)).isFalse();
        origin = "😀";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "abcd1234【】";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isFalse();
        origin = "1234";
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 4)).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(origin, 3)).isFalse();

        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(" ")).isTrue();
        assertThat(FilterUtils.checkOnlyChineseAndLetterAndNumberAndPunctuation(" ", -1)).isFalse();
    }
}
