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

package com.tencent.bk.job.execute.service.rolling.impl;

import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.model.db.StepFileSourceRollingConfigDO;
import com.tencent.bk.job.execute.service.rolling.FileSourceBatchCalculator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * 按文件滚动分批策略测试用例
 */
public class FileSourceBatchCalculatorTest {
    /**
     * 测试空文件源列表
     */
    @Test
    public void testEmptyFileSourceList() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        Assertions.assertThat(fileBatchList).isEmpty();
    }

    /**
     * 测试逐机器逐文件分批
     */
    @Test
    public void testSingleExecuteObjectSingleFileBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        fileSourceList.add(buildFileSource(new long[]{1}, new String[]{"file_1"}));
        fileSourceList.add(buildFileSource(new long[]{2, 3}, new String[]{"file_2", "file_3"}));
        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        Assertions.assertThat(fileBatchList).hasSize(5);
    }

    /**
     * 测试单文件多机器的情况
     */
    @Test
    public void testSingleFileMultiExecuteObjectsBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        fileSourceList.add(buildFileSource(new long[]{1, 2, 3}, new String[]{"file_1"}));
        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(2);
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        Assertions.assertThat(fileBatchList).hasSize(2);
    }

    /**
     * 测试多文件单机器的情况
     */
    @Test
    public void testMultiFilesSingleExecuteObjectBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        fileSourceList.add(buildFileSource(new long[]{1}, new String[]{"file_1", "file_2", "file_3"}));
        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(2);
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        Assertions.assertThat(fileBatchList).hasSize(2);
    }

    /**
     * 测试贪心算法合并多个文件源
     */
    @Test
    public void testGreedyMergeFileSources() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        fileSourceList.add(buildFileSource(new long[]{1}, new String[]{"file_1"}));
        fileSourceList.add(buildFileSource(new long[]{2}, new String[]{"file_2"}));
        fileSourceList.add(buildFileSource(new long[]{3}, new String[]{"file_3"}));
        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(3);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(3);
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        Assertions.assertThat(fileBatchList).hasSize(1);
    }

    /**
     * 测试无限制的滚动配置
     */
    @Test
    public void testNoLimitRollingConfig() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        fileSourceList.add(buildFileSource(new long[]{1, 2, 3}, new String[]{"file_1", "file_2", "file_3"}));
        StepFileSourceRollingConfigDO fileSourceRollingConfig = new StepFileSourceRollingConfigDO();
        fileSourceRollingConfig.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(Integer.MAX_VALUE);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(Integer.MAX_VALUE);
        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        Assertions.assertThat(fileBatchList).hasSize(1);
    }

    /**
     * 测试混合情况：既有需要拆分的文件源，又有可以合并的文件源
     */
    @Test
    public void testMixedFileSourcesBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        // 需要拆分的文件源（机器数超限）
        fileSourceList.add(buildFileSource(new long[]{1, 2, 3, 4}, new String[]{"file_1"}));
        // 可以合并的文件源
        fileSourceList.add(buildFileSource(new long[]{5}, new String[]{"file_2"}));
        fileSourceList.add(buildFileSource(new long[]{6}, new String[]{"file_3"}));
        // 需要拆分的文件源（文件数超限）
        fileSourceList.add(buildFileSource(new long[]{7}, new String[]{"file_4", "file_5", "file_6"}));

        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(2);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(2);

        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        Assertions.assertThat(fileBatchList).hasSize(5); // 2(机器拆分) + 1(合并) + 2(文件拆分)
    }

    /**
     * 测试超大文件源的分批（机器数和文件数都超限）
     */
    @Test
    public void testLargeFileSourceBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        // 创建包含10台机器和10个文件的超大文件源
        long[] hostIds = new long[10];
        String[] filePaths = new String[10];
        for (int i = 0; i < 10; i++) {
            hostIds[i] = i + 1;
            filePaths[i] = "file_" + (i + 1);
        }
        fileSourceList.add(buildFileSource(hostIds, filePaths));

        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(3);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(4);

        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        // 机器分4批(10/3=4)，文件分3批(10/4=3)，笛卡尔积=12
        Assertions.assertThat(fileBatchList).hasSize(12);
    }

    /**
     * 测试边界值：刚好达到限制的情况
     */
    @Test
    public void testBoundaryValueBatch() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        // 刚好达到机器数限制
        fileSourceList.add(buildFileSource(new long[]{1, 2}, new String[]{"file_1"}));
        // 刚好达到文件数限制
        fileSourceList.add(buildFileSource(new long[]{3}, new String[]{"file_2", "file_3"}));

        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(2);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(2);

        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        // 第一个文件源刚好达到机器限制，不拆分；第二个刚好达到文件限制，不拆分；可以合并
        Assertions.assertThat(fileBatchList).hasSize(2);
    }

    /**
     * 测试多个文件源的复杂合并情况
     */
    @Test
    public void testMultipleFileSourcesMerge() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        // 添加多个可以合并的文件源
        fileSourceList.add(buildFileSource(new long[]{1}, new String[]{"file_1"}));
        fileSourceList.add(buildFileSource(new long[]{2}, new String[]{"file_2"}));
        fileSourceList.add(buildFileSource(new long[]{3}, new String[]{"file_3"}));
        fileSourceList.add(buildFileSource(new long[]{4}, new String[]{"file_4"}));
        fileSourceList.add(buildFileSource(new long[]{5}, new String[]{"file_5"}));

        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(3);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(3);

        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        // 前3个合并为一批，后2个合并为一批
        Assertions.assertThat(fileBatchList).hasSize(2);
    }

    /**
     * 测试文件源部分超限的情况
     */
    @Test
    public void testPartialExceedLimit() {
        FileSourceBatchCalculator fileSourceBatchCalculator = new FileSourceBatchCalculatorImpl();
        List<FileSourceDTO> fileSourceList = new ArrayList<>();
        // 机器数刚好不超限，文件数超限
        fileSourceList.add(buildFileSource(new long[]{1, 2}, new String[]{"file_1", "file_2", "file_3"}));
        // 文件数刚好不超限，机器数超限
        fileSourceList.add(buildFileSource(new long[]{3, 4, 5}, new String[]{"file_4", "file_5"}));

        StepFileSourceRollingConfigDO fileSourceRollingConfig = buildStepFileSourceRollingConfig();
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(2);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(2);

        List<StepInstanceFileBatchDTO> fileBatchList = fileSourceBatchCalculator.calc(
            1L,
            1L,
            fileSourceList,
            fileSourceRollingConfig
        );
        System.out.println("fileBatchList=" + JsonUtils.toJson(fileBatchList));
        // 第一个文件源按文件拆分(2批)，第二个按机器拆分(2批)
        Assertions.assertThat(fileBatchList).hasSize(4);
    }

    private FileSourceDTO buildFileSource(long[] hostIds, String[] filePaths) {
        FileSourceDTO fileSource = new FileSourceDTO();
        ExecuteTargetDTO servers = new ExecuteTargetDTO();
        List<ExecuteObject> executeObjects = new ArrayList<>();
        for (long hostId : hostIds) {
            ExecuteObject executeObject = new ExecuteObject(new HostDTO(hostId));
            executeObjects.add(executeObject);
        }
        servers.setExecuteObjects(executeObjects);
        fileSource.setServers(servers);
        List<FileDetailDTO> fileList = new ArrayList<>();
        for (String filePath : filePaths) {
            fileList.add(new FileDetailDTO(filePath));
        }
        fileSource.setFiles(fileList);
        return fileSource;
    }

    private StepFileSourceRollingConfigDO buildStepFileSourceRollingConfig() {
        StepFileSourceRollingConfigDO fileSourceRollingConfig = new StepFileSourceRollingConfigDO();
        fileSourceRollingConfig.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        fileSourceRollingConfig.setMaxExecuteObjectNumInBatch(1);
        fileSourceRollingConfig.setMaxFileNumOfSingleExecuteObject(1);
        return fileSourceRollingConfig;
    }
}
