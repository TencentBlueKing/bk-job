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

package com.tencent.bk.job.logsvr.util;

import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 集合名称工具类测试用例
 */
public class CollectionNameUtilTest {

    @Test
    @DisplayName("集合名称工具类-测试构建日志集合名")
    public void testBuildLogCollectionName() {
        assertThat(CollectionNameUtil.buildLogCollectionName("2025_01_01", LogTypeEnum.SCRIPT)).isEqualTo(
            "job_log_script_2025_01_01");
        assertThat(CollectionNameUtil.buildLogCollectionName("2025_05_01", LogTypeEnum.SCRIPT)).isEqualTo(
            "job_log_script_2025_05_01");
        assertThat(CollectionNameUtil.buildLogCollectionName("2025_01_01", LogTypeEnum.FILE)).isEqualTo(
            "job_log_file_2025_01_01");
        assertThat(CollectionNameUtil.buildLogCollectionName("2025_05_01", LogTypeEnum.FILE)).isEqualTo(
            "job_log_file_2025_05_01");
    }

    @Test
    @DisplayName("集合名称工具类-测试获取类型名称")
    public void testGetLogTypeName() {
        assertThat(CollectionNameUtil.getLogTypeName(LogTypeEnum.SCRIPT)).isEqualTo("script");
        assertThat(CollectionNameUtil.getLogTypeName(LogTypeEnum.FILE)).isEqualTo("file");
    }

    @Test
    @DisplayName("集合名称工具类-构建日志集合名称中不带日期的部分")
    public void testGetCollectionNamePrefix() {
        assertThat(CollectionNameUtil.buildCollectionNamePrefix(LogTypeEnum.SCRIPT))
            .isEqualTo("job_log_script_");
        assertThat(CollectionNameUtil.buildCollectionNamePrefix(LogTypeEnum.FILE))
            .isEqualTo("job_log_file_");
    }

    @Test
    @DisplayName("集合名称工具类-测试从日志集合名中提取日期部分")
    public void testCollectionNameToDateStr() {
        assertThat(CollectionNameUtil.collectionNameToDateStr("job_log_script_2025_01_01"))
            .isEqualTo("2025_01_01");
        assertThat(CollectionNameUtil.collectionNameToDateStr("job_log_file_2024_12_31"))
            .isEqualTo("2024_12_31");
        assertThat(CollectionNameUtil.collectionNameToDateStr("job_log_api_2025_01_01"))
            .isNull();
    }
}
