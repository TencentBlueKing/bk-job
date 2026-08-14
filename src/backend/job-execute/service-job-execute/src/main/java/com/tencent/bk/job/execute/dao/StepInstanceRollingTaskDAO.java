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

package com.tencent.bk.job.execute.dao;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchFinishResult;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;

import java.util.List;

/**
 * 步骤滚动任务DAO
 */
public interface StepInstanceRollingTaskDAO {

    /**
     * 查询步骤滚动任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动批次
     * @return 步骤滚动任务
     */
    StepInstanceRollingTaskDTO queryRollingTask(Long taskInstanceId,
                                                long stepInstanceId,
                                                int executeCount,
                                                int batch);

    /**
     * 查询步骤滚动任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数;如果不为null，会根据executeCount过滤
     * @param batch          滚动执行批次;如果不为null，会根据batch过滤
     * @return 步骤滚动任务
     */
    List<StepInstanceRollingTaskDTO> listRollingTasks(Long taskInstanceId,
                                                      long stepInstanceId,
                                                      Integer executeCount,
                                                      Integer batch);

    /**
     * 保存步骤滚动任务
     *
     * @param rollingTask 滚动任务
     * @return 步骤滚动任务ID
     */
    long saveRollingTask(StepInstanceRollingTaskDTO rollingTask);

    /**
     * 更新滚动任务
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动执行批次
     * @param status         任务状态；如果不更新传入null
     * @param startTime      任务开始时间；如果不更新传入null
     * @param endTime        任务结束时间；如果不更新传入null
     * @param totalTime      任务执行总时间；如果不更新传入null
     */
    void updateRollingTask(Long taskInstanceId,
                           long stepInstanceId,
                           int executeCount,
                           int batch,
                           RunStatusEnum status,
                           Long startTime,
                           Long endTime, Long totalTime);

    /**
     * 更新滚动任务的下发信息（并行错峰模式）
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动执行批次
     * @param dispatchTime   计划下发时刻；如果不更新传入null
     * @param dispatched     是否已下发；如果不更新传入null
     */
    void updateDispatchInfo(Long taskInstanceId,
                            long stepInstanceId,
                            int executeCount,
                            int batch,
                            Long dispatchTime,
                            Boolean dispatched);

    /**
     * 并行错峰模式：单批次终态跃迁 + 步骤完成判定（同一事务内，借 batch=1 锚点行 FOR UPDATE 串行化本步骤完成判定）。
     * <p>
     * 步骤：① SELECT ... batch=1 FOR UPDATE 抢锚点；② 幂等闸门 UPDATE 本批为终态(WHERE status NOT IN 终态集)，affected==0
     * 直接返回 {@link ScatterBatchFinishResult#ALREADY_FINAL}；③ COUNT(终态批次)==totalBatch 返回
     * {@link ScatterBatchFinishResult#LAST_BATCH}，否则 {@link ScatterBatchFinishResult#NOT_LAST_BATCH}。
     *
     * @param taskInstanceId 作业实例 ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          当前到达终态的批次
     * @param status         批次终态
     * @param startTime      批次开始时间
     * @param endTime        批次结束时间
     * @param totalTime      批次总耗时
     * @param totalBatch     步骤总批次数
     * @return 完成判定结果
     */
    ScatterBatchFinishResult finishBatchAndCheckAllDone(Long taskInstanceId,
                                                        long stepInstanceId,
                                                        int executeCount,
                                                        int batch,
                                                        RunStatusEnum status,
                                                        Long startTime,
                                                        Long endTime,
                                                        Long totalTime,
                                                        int totalBatch);
}
