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

package com.tencent.bk.job.backup.archive.service;

import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskExecutionDetail;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.common.mysql.JobTransactional;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class ArchiveTaskService {

    private final ArchiveTaskDAO archiveTaskDAO;

    public ArchiveTaskService(ArchiveTaskDAO archiveTaskDAO) {
        this.archiveTaskDAO = archiveTaskDAO;
    }

    /**
     * 获取最新创建的归档任务
     *
     * @param taskType 归档任务类型
     */
    public JobInstanceArchiveTaskInfo getLatestArchiveTask(ArchiveTaskTypeEnum taskType) {
        return archiveTaskDAO.getLatestArchiveTask(taskType);
    }

    public void saveArchiveTask(JobInstanceArchiveTaskInfo jobInstanceArchiveTaskInfo) {
        archiveTaskDAO.saveArchiveTask(jobInstanceArchiveTaskInfo);
    }

    @JobTransactional(transactionManager = "jobBackupTransactionManager")
    public void saveArchiveTasks(Collection<JobInstanceArchiveTaskInfo> archiveTaskList) {
        if (CollectionUtils.isNotEmpty(archiveTaskList)) {
            archiveTaskList.forEach(archiveTaskDAO::saveArchiveTask);
        }
    }

    public List<JobInstanceArchiveTaskInfo> listRunningTasks(ArchiveTaskTypeEnum taskType) {
        return archiveTaskDAO.listRunningTasks(taskType);
    }

    /**
     * 获取归档任务
     *
     * @param taskType 查询条件 - 任务类型
     * @param status   查询条件 - 任务状态
     * @param limit    查询条件 - 查询最大数量
     * @return 归档任务列表
     */
    public List<JobInstanceArchiveTaskInfo> listTasks(ArchiveTaskTypeEnum taskType,
                                                      ArchiveTaskStatusEnum status,
                                                      int limit) {
        return archiveTaskDAO.listTasks(taskType, status, limit);
    }

    /**
     * 返回根据 db 分组的归档任务数量
     *
     * @param taskType 归档任务类型
     * @return key: db 名称; value: 任务数量
     */
    public Map<String, Integer> countScheduleTasksGroupByDb(ArchiveTaskTypeEnum taskType) {
        return archiveTaskDAO.countScheduleTasksGroupByDb(taskType);
    }


    /**
     * 更新归档任务执行信息 - 启动后
     *
     * @param taskType  任务类型
     * @param dataNode  数据节点
     * @param day       归档数据所在天
     * @param hour      归档数据所在小时
     * @param startTime 任务开始时间
     */
    public void updateStartedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                         DbDataNode dataNode,
                                         Integer day,
                                         Integer hour,
                                         Long startTime) {
        archiveTaskDAO.updateStartedExecuteInfo(
            taskType,
            dataNode,
            day,
            hour,
            startTime
        );

    }

    /**
     * 更新归档任务执行信息 - 运行中
     *
     * @param taskType 任务类型
     * @param dataNode 数据节点
     * @param day      归档数据所在天
     * @param hour     归档数据所在小时
     * @param process  进度
     */
    public void updateRunningExecuteInfo(ArchiveTaskTypeEnum taskType,
                                         DbDataNode dataNode,
                                         Integer day,
                                         Integer hour,
                                         IdBasedArchiveProcess process) {
        archiveTaskDAO.updateRunningExecuteInfo(
            taskType,
            dataNode,
            day,
            hour,
            process
        );

    }

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
    public void updateCompletedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                           DbDataNode dataNode,
                                           Integer day,
                                           Integer hour,
                                           ArchiveTaskStatusEnum status,
                                           IdBasedArchiveProcess process,
                                           Long endTime,
                                           Long cost,
                                           ArchiveTaskExecutionDetail detail) {
        archiveTaskDAO.updateCompletedExecuteInfo(
            taskType,
            dataNode,
            day,
            hour,
            status,
            process,
            endTime,
            cost,
            detail
        );
    }

    public JobInstanceArchiveTaskInfo getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum taskType, String dbNodeId) {
        return archiveTaskDAO.getFirstScheduleArchiveTaskByDb(taskType, dbNodeId);
    }

    /**
     * 更新归档任务执行状态
     *
     * @param taskType 任务类型
     * @param dataNode 数据节点
     * @param day      归档数据所在天
     * @param hour     归档数据所在小时
     * @param status   任务状态
     */
    public void updateArchiveTaskStatus(ArchiveTaskTypeEnum taskType,
                                        DbDataNode dataNode,
                                        Integer day,
                                        Integer hour,
                                        ArchiveTaskStatusEnum status) {
        archiveTaskDAO.updateArchiveTaskStatus(taskType, dataNode, day, hour, status);
    }

    /**
     * 按照任务类型和状态，统计归档任务数量
     *
     * @param taskType   任务类型
     * @param statusList 状态列表
     * @return 数量
     */
    public Map<ArchiveTaskStatusEnum, Integer> countTaskByStatus(ArchiveTaskTypeEnum taskType,
                                                                 List<ArchiveTaskStatusEnum> statusList) {
        return archiveTaskDAO.countTaskByStatus(taskType, statusList);
    }

    /**
     * 更新归档任务执行详情
     *
     * @param taskType 任务类型
     * @param dataNode 数据节点
     * @param day      归档数据所在天
     * @param hour     归档数据所在小时
     * @param detail   执行详情
     */
    public void updateExecutionDetail(ArchiveTaskTypeEnum taskType,
                                      DbDataNode dataNode,
                                      Integer day,
                                      Integer hour,
                                      ArchiveTaskExecutionDetail detail) {
        archiveTaskDAO.updateExecutionDetail(taskType, dataNode, day, hour, detail);
    }
}
