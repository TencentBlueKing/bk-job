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
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
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
     * 返回根据 db 分组的归档任务数量
     *
     * @param taskType 归档任务类型
     * @return key: db 名称; value: 任务数量
     */
    public Map<String, Integer> countScheduleTasksGroupByDb(ArchiveTaskTypeEnum taskType) {
        return archiveTaskDAO.countScheduleTasksGroupByDb(taskType);
    }

    public void updateTask(JobInstanceArchiveTaskInfo archiveTask) {
        archiveTaskDAO.updateTask(archiveTask);
    }

    public JobInstanceArchiveTaskInfo getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum taskType, String dbNodeId) {
        return archiveTaskDAO.getFirstScheduleArchiveTaskByDb(taskType, dbNodeId);
    }
}
