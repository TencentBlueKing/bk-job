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

package com.tencent.bk.job.common.util.json;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("all")
class LongTimestampSerializerTest {

    @Test
    void serialize() {
        TestClass testClass = new TestClass(1579089600L);
        assertThat(JsonUtils.toJson(testClass)).isEqualTo(
            "{\"time\":\"2020-01-15 20:00:00\",\"timeList\":[\"2020-01-15 20:00:00\",\"2020-01-15 20:00:00\"," +
                "\"2020-01-15 20:00:00\"]}");
        testClass = new TestClass(1579089600000L);
        assertThat(JsonUtils.toJson(testClass)).isEqualTo(
            "{\"time\":\"2020-01-15 20:00:00\",\"timeList\":[\"2020-01-15 20:00:00\",\"2020-01-15 20:00:00\"," +
                "\"2020-01-15 20:00:00\"]}");
    }

    static class TestClass {
        @JsonSerialize(using = LongTimestampSerializer.class)
        private Long time;

        @JsonSerialize(using = LongTimestampSerializer.class)
        private List<Long> timeList;

        public TestClass(Long time) {
            this.time = time;
            this.timeList = Arrays.asList(time, time, time);
        }
    }
}
