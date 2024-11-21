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

package com.tencent.bk.job.backup.archive.model;

import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import lombok.Data;
import lombok.ToString;

/**
 * 作业实例归档任务信息
 */
@Data
@ToString
public class JobInstanceArchiveTaskInfo {
    /**
     * 归档任务类型
     */
    private ArchiveTaskTypeEnum taskType;
    /**
     * 归档数据节点信息
     */
    private DbDataNode dbDataNode;

    /**
     * 归档任务所在天.比如 20240806
     */
    private Integer day;
    /**
     * 归档任务所在小时。 1-24
     */
    private Integer hour;
    /**
     * 归档任务时间范围-from timestamp
     */
    private Long fromTimestamp;
    /**
     * 归档任务时间范围-to timestamp
     */
    private Long toTimestamp;
    /**
     * 归档进度
     */
    private TimeAndIdBasedArchiveProcess process;
    /**
     * 归档任务状态
     */
    private ArchiveTaskStatusEnum status;
    /**
     * 归档任务创建时间
     */
    private Long createTime;
    /**
     * 归档任务最后更新时间
     */
    private Long lastUpdateTime;

    public JobInstanceArchiveTaskInfo clone() {
        JobInstanceArchiveTaskInfo archiveTask = new JobInstanceArchiveTaskInfo();
        archiveTask.setTaskType(taskType);
        archiveTask.setDbDataNode(dbDataNode.clone());
        archiveTask.setDay(day);
        archiveTask.setHour(hour);
        archiveTask.setFromTimestamp(fromTimestamp);
        archiveTask.setToTimestamp(toTimestamp);
        archiveTask.setProcess(process.clone());
        archiveTask.setStatus(status);
        archiveTask.setCreateTime(createTime);
        archiveTask.setLastUpdateTime(lastUpdateTime);
        return archiveTask;
    }

    public String buildTaskUniqueId() {
        return taskType.getType() + ":" + day + ":" + hour + ":" + dbDataNode.toDataNodeId();
    }
}