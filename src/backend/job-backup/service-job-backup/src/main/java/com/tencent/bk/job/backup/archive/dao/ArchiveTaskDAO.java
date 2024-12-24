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

package com.tencent.bk.job.backup.archive.dao;

import com.tencent.bk.job.backup.archive.model.ArchiveTaskExecutionDetail;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;

import java.util.List;
import java.util.Map;

/**
 * 归档任务 DAO
 */
public interface ArchiveTaskDAO {

    /**
     * 获取最新的归档任务
     *
     * @param taskType 归档任务类型
     */
    JobInstanceArchiveTaskInfo getLatestArchiveTask(ArchiveTaskTypeEnum taskType);

    void saveArchiveTask(JobInstanceArchiveTaskInfo jobInstanceArchiveTaskInfo);

    List<JobInstanceArchiveTaskInfo> listRunningTasks(ArchiveTaskTypeEnum taskType);

    /**
     * 获取归档任务
     *
     * @param taskType 查询条件 - 任务类型
     * @param status   查询条件 - 任务状态
     * @param limit    查询条件 - 查询最大数量
     * @return 归档任务列表
     */
    List<JobInstanceArchiveTaskInfo> listTasks(ArchiveTaskTypeEnum taskType,
                                               ArchiveTaskStatusEnum status,
                                               int limit);

    /**
     * 返回根据 db 分组的归档任务数量
     *
     * @param taskType 归档任务类型
     * @return key: db 名称; value: 任务数量
     */
    Map<String, Integer> countScheduleTasksGroupByDb(ArchiveTaskTypeEnum taskType);


    /**
     * 更新归档任务执行信息 - 启动后
     *
     * @param taskType  任务类型
     * @param dataNode  数据节点
     * @param day       归档数据所在天
     * @param hour      归档数据所在小时
     * @param startTime 任务开始时间
     */
    void updateStartedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                  DbDataNode dataNode,
                                  Integer day,
                                  Integer hour,
                                  Long startTime);

    /**
     * 更新归档任务执行信息 - 运行中
     *
     * @param taskType 任务类型
     * @param dataNode 数据节点
     * @param day      归档数据所在天
     * @param hour     归档数据所在小时
     * @param process  进度
     */
    void updateRunningExecuteInfo(ArchiveTaskTypeEnum taskType,
                                  DbDataNode dataNode,
                                  Integer day,
                                  Integer hour,
                                  IdBasedArchiveProcess process);

    /**
     * 更新归档任务执行信息 - 结束
     *
     * @param taskType 任务类型
     * @param dataNode 数据节点
     * @param day      归档数据所在天
     * @param hour     归档数据所在小时
     * @param status   任务状态
     * @param process  进度
     * @param endTime  结束时间
     * @param cost     任务耗时
     * @param detail   执行详情
     */
    void updateCompletedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                    DbDataNode dataNode,
                                    Integer day,
                                    Integer hour,
                                    ArchiveTaskStatusEnum status,
                                    IdBasedArchiveProcess process,
                                    Long endTime,
                                    Long cost,
                                    ArchiveTaskExecutionDetail detail);

    JobInstanceArchiveTaskInfo getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum taskType, String dbNodeId);

    void updateArchiveTaskStatus(ArchiveTaskTypeEnum taskType,
                                 DbDataNode dataNode,
                                 Integer day,
                                 Integer hour,
                                 ArchiveTaskStatusEnum status);

    Map<ArchiveTaskStatusEnum, Integer> countTaskByStatus(ArchiveTaskTypeEnum taskType,
                                                          List<ArchiveTaskStatusEnum> statusList);

    void updateExecutionDetail(ArchiveTaskTypeEnum taskType,
                               DbDataNode dataNode,
                               Integer day,
                               Integer hour,
                               ArchiveTaskExecutionDetail detail);
}
