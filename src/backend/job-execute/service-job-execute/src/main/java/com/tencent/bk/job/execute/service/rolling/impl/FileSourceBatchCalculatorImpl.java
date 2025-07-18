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

package com.tencent.bk.job.execute.service.rolling.impl;

import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.StepInstanceFileBatchDTO;
import com.tencent.bk.job.execute.model.db.StepFileSourceRollingConfigDO;
import com.tencent.bk.job.execute.service.rolling.FileSourceBatchCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 源文件滚动批次计算器实现
 */
@Slf4j
@Service
public class FileSourceBatchCalculatorImpl implements FileSourceBatchCalculator {

    /**
     * 根据源文件滚动策略计算每个滚动批次中需要分发的源文件
     * 滚动策略规则：
     * 以单个源文件组为单位，判断其中源文件目标机器与文件数量是否超限
     * 若不超限，按顺序将相邻的尽可能多的源文件组划分为一个滚动批次
     * 若任一参数超限，则将该源文件组按照滚动参数拆分为多个滚动批次
     *
     * @param taskInstanceId          作业实例ID
     * @param stepInstanceId          步骤实例ID
     * @param fileSourceList          源文件列表
     * @param fileSourceRollingConfig 源文件滚动策略
     * @return 源文件滚动批次列表
     */
    @Override
    public List<StepInstanceFileBatchDTO> calc(Long taskInstanceId,
                                               Long stepInstanceId,
                                               List<FileSourceDTO> fileSourceList,
                                               StepFileSourceRollingConfigDO fileSourceRollingConfig) {
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return Collections.emptyList();
        }
        List<StepInstanceFileBatchDTO> resultList = new ArrayList<>();
        Integer maxExecuteObjectNumInBatch = fileSourceRollingConfig.getMaxExecuteObjectNumInBatch();
        Integer maxFileNumOfSingleExecuteObject = fileSourceRollingConfig.getMaxFileNumOfSingleExecuteObject();
        if (maxExecuteObjectNumInBatch == null || maxExecuteObjectNumInBatch <= 0) {
            maxExecuteObjectNumInBatch = Integer.MAX_VALUE;
        }
        if (maxFileNumOfSingleExecuteObject == null || maxFileNumOfSingleExecuteObject <= 0) {
            maxFileNumOfSingleExecuteObject = Integer.MAX_VALUE;
        }
        for (int i = 0; i < fileSourceList.size(); i++) {
            FileSourceDTO fileSourceDTO = fileSourceList.get(i);
            if (fileSourceDTO.getFileNum() > maxFileNumOfSingleExecuteObject
                || fileSourceDTO.getExecuteObjectNum() > maxExecuteObjectNumInBatch) {
                // 一组源文件需要拆分为多个滚动批次
                List<StepInstanceFileBatchDTO> fileBatchList = splitFileSourceIntoBatch(
                    fileSourceDTO,
                    taskInstanceId,
                    stepInstanceId,
                    maxExecuteObjectNumInBatch,
                    maxFileNumOfSingleExecuteObject
                );
                resultList.addAll(fileBatchList);
            } else {
                // 一组源文件的目标机器与文件数量均未超限，使用贪心算法匹配尽可能多组源文件作为同一个滚动批次，提高分发效率
                StepInstanceFileBatchDTO fileBatch = greedySearchFileSourceIntoBatch(
                    fileSourceList,
                    i,
                    taskInstanceId,
                    stepInstanceId,
                    maxExecuteObjectNumInBatch,
                    maxFileNumOfSingleExecuteObject
                );
                resultList.add(fileBatch);
                i += fileBatch.getFileSourceList().size() - 1;
            }
        }
        // 设置滚动批次
        for (int i = 0; i < resultList.size(); i++) {
            StepInstanceFileBatchDTO fileBatch = resultList.get(i);
            fileBatch.setBatch(i + 1);
        }
        // 打印滚动分批计算结果
        logCalcResult(fileSourceList, resultList);
        return resultList;
    }

    /**
     * 将一组源文件拆分为多个滚动批次
     *
     * @param fileSourceDTO                   一组源文件
     * @param maxExecuteObjectNumInBatch      单批次最大源执行对象数
     * @param maxFileNumOfSingleExecuteObject 单个执行对象的最大并发文件数
     * @return 拆分后的多个滚动批次
     */
    private List<StepInstanceFileBatchDTO> splitFileSourceIntoBatch(FileSourceDTO fileSourceDTO,
                                                                    Long taskInstanceId,
                                                                    Long stepInstanceId,
                                                                    Integer maxExecuteObjectNumInBatch,
                                                                    Integer maxFileNumOfSingleExecuteObject) {
        List<StepInstanceFileBatchDTO> resultList = new ArrayList<>();
        int executeObjectNum = fileSourceDTO.getExecuteObjectNum();
        int fileNum = fileSourceDTO.getFileNum();
        List<List<ExecuteObject>> executeObjectGroupList = new ArrayList<>();
        List<List<FileDetailDTO>> fileGroupList = new ArrayList<>();
        // 源机器分组
        int startIndex = 0;
        int endIndex;
        do {
            endIndex = Math.min(startIndex + maxExecuteObjectNumInBatch, executeObjectNum);
            executeObjectGroupList.add(fileSourceDTO.getServers().getExecuteObjects().subList(startIndex, endIndex));
            startIndex = endIndex;
        } while (startIndex < executeObjectNum);
        // 源文件分组
        startIndex = 0;
        do {
            endIndex = Math.min(startIndex + maxFileNumOfSingleExecuteObject, fileNum);
            fileGroupList.add(fileSourceDTO.getFiles().subList(startIndex, endIndex));
            startIndex = endIndex;
        } while (startIndex < fileNum);
        // 源机器分组与源文件分组作笛卡尔积，生成多个滚动分发批次
        for (List<ExecuteObject> executeObjectGroup : executeObjectGroupList) {
            for (List<FileDetailDTO> fileGroup : fileGroupList) {
                StepInstanceFileBatchDTO fileBatch = new StepInstanceFileBatchDTO();
                fileBatch.setTaskInstanceId(taskInstanceId);
                fileBatch.setStepInstanceId(stepInstanceId);
                FileSourceDTO batchFileSource = buildBatchFileSource(fileSourceDTO, executeObjectGroup, fileGroup);
                fileBatch.setFileSourceList(Collections.singletonList(batchFileSource));
                resultList.add(fileBatch);
            }
        }
        return resultList;
    }

    /**
     * 使用划分好批次的机器与文件构建一个新的滚动批次源文件信息
     *
     * @param fileSourceDTO      未划分批次的源文件信息
     * @param executeObjectGroup 执行对象分组
     * @param fileGroup          文件分组
     * @return 单个滚动批次的源文件信息
     */
    private FileSourceDTO buildBatchFileSource(FileSourceDTO fileSourceDTO,
                                               List<ExecuteObject> executeObjectGroup,
                                               List<FileDetailDTO> fileGroup) {
        FileSourceDTO batchFileSource = fileSourceDTO.clone();
        batchFileSource.getServers().setExecuteObjects(executeObjectGroup);
        batchFileSource.setFiles(fileGroup);
        return batchFileSource;
    }

    /**
     * 使用贪心算法顺序搜索尽可能多组源文件作为同一个滚动批次
     *
     * @param fileSourceList                  源文件组列表
     * @param startIndex                      搜索起始索引
     * @param taskInstanceId                  任务实例ID
     * @param stepInstanceId                  步骤实例ID
     * @param maxExecuteObjectNumInBatch      单批次最大源执行对象数
     * @param maxFileNumOfSingleExecuteObject 单个执行对象的最大并发文件数
     * @return 一个滚动批次中的源文件
     */
    private StepInstanceFileBatchDTO greedySearchFileSourceIntoBatch(List<FileSourceDTO> fileSourceList,
                                                                     int startIndex,
                                                                     Long taskInstanceId,
                                                                     Long stepInstanceId,
                                                                     Integer maxExecuteObjectNumInBatch,
                                                                     Integer maxFileNumOfSingleExecuteObject) {
        FileSourceDTO fileSourceDTO = fileSourceList.get(startIndex);
        int executeObjectNumInBatch = fileSourceDTO.getExecuteObjectNum();
        int fileNumInBatch = fileSourceDTO.getFileNum();
        int endIndex = startIndex;
        do {
            endIndex++;
            if (endIndex >= fileSourceList.size()) {
                // 已经匹配到最后一个源文件，匹配完成
                break;
            }
            fileSourceDTO = fileSourceList.get(endIndex);
            executeObjectNumInBatch += fileSourceDTO.getExecuteObjectNum();
            fileNumInBatch += fileSourceDTO.getFileNum();
            if (fileNumInBatch > maxFileNumOfSingleExecuteObject
                || executeObjectNumInBatch > maxExecuteObjectNumInBatch) {
                // 超限，匹配完成
                break;
            }
        } while (true);
        StepInstanceFileBatchDTO fileBatch = new StepInstanceFileBatchDTO();
        fileBatch.setTaskInstanceId(taskInstanceId);
        fileBatch.setStepInstanceId(stepInstanceId);
        fileBatch.setFileSourceList(fileSourceList.subList(startIndex, endIndex));
        return fileBatch;
    }

    /**
     * 日志打印滚动分批计算结果
     *
     * @param fileSourceList 源文件列表
     * @param fileBatchList  滚动批次列表
     */
    private void logCalcResult(List<FileSourceDTO> fileSourceList, List<StepInstanceFileBatchDTO> fileBatchList) {
        if (log.isDebugEnabled()) {
            List<FileSourceDTO> fileSourceListToLog = fileSourceList;
            int maxElementToLog = 20;
            if (fileSourceList.size() > maxElementToLog) {
                fileSourceListToLog = fileSourceList.subList(0, maxElementToLog);
            }
            List<StepInstanceFileBatchDTO> fileBatchListToLog = fileBatchList;
            if (fileBatchList.size() > maxElementToLog) {
                fileBatchListToLog = fileBatchList.subList(0, maxElementToLog);
            }
            log.debug(
                "fileSourceList.size={}, fileSourceList={}, fileBatchList.size={}, fileBatchList={}",
                fileSourceList.size(),
                fileBatchList.size(),
                fileSourceListToLog.stream().map(FileSourceDTO::getSimpleDesc).collect(Collectors.toList()),
                fileBatchListToLog.stream().map(StepInstanceFileBatchDTO::getSimpleDesc).collect(Collectors.toList())
            );
        } else {
            log.info("fileSourceList.size={}, fileBatchList.size={}", fileSourceList.size(), fileBatchList.size());
        }
    }
}
