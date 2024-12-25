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

package com.tencent.bk.job.backup.archive.dao.impl;

import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.model.ArchiveTaskExecutionDetail;
import com.tencent.bk.job.backup.archive.model.DbDataNode;
import com.tencent.bk.job.backup.archive.model.JobInstanceArchiveTaskInfo;
import com.tencent.bk.job.backup.archive.model.IdBasedArchiveProcess;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import com.tencent.bk.job.backup.model.tables.ArchiveTask;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class ArchiveTaskDAOImpl implements ArchiveTaskDAO {

    private final DSLContext ctx;
    private static final ArchiveTask T = ArchiveTask.ARCHIVE_TASK;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        T.TASK_TYPE,
        T.DATA_NODE,
        T.DB_NODE,
        T.DAY,
        T.HOUR,
        T.FROM_TIMESTAMP,
        T.TO_TIMESTAMP,
        T.PROCESS,
        T.STATUS,
        T.CREATE_TIME,
        T.LAST_UPDATE_TIME,
        T.TASK_START_TIME,
        T.TASK_END_TIME,
        T.TASK_COST,
        T.DETAIL
    };

    @Autowired
    public ArchiveTaskDAOImpl(@Qualifier("job-backup-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public JobInstanceArchiveTaskInfo getLatestArchiveTask(ArchiveTaskTypeEnum taskType) {
        Record record = ctx.select(ALL_FIELDS)
            .from(T)
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .orderBy(T.DAY.desc(), T.HOUR.desc())
            .limit(1)
            .fetchOne();

        return extract(record);
    }

    private JobInstanceArchiveTaskInfo extract(Record record) {
        if (record == null) {
            return null;
        }
        JobInstanceArchiveTaskInfo archiveTask = new JobInstanceArchiveTaskInfo();
        archiveTask.setTaskType(ArchiveTaskTypeEnum.valOf(record.get(T.TASK_TYPE)));
        archiveTask.setDbDataNode(DbDataNode.fromDataNodeId(record.get(T.DATA_NODE)));
        archiveTask.setDay(record.get(T.DAY));
        archiveTask.setHour(JooqDataTypeUtil.toInteger(record.get(T.HOUR)));
        archiveTask.setFromTimestamp(record.get(T.FROM_TIMESTAMP));
        archiveTask.setToTimestamp(record.get(T.TO_TIMESTAMP));
        archiveTask.setProcess(IdBasedArchiveProcess.fromPersistentProcess(record.get(T.PROCESS)));
        archiveTask.setStatus(ArchiveTaskStatusEnum.valOf(record.get(T.STATUS)));
        archiveTask.setCreateTime(record.get(T.CREATE_TIME));
        archiveTask.setLastUpdateTime(record.get(T.LAST_UPDATE_TIME));
        archiveTask.setTaskStartTime(record.get(T.TASK_START_TIME));
        archiveTask.setTaskEndTime(record.get(T.TASK_END_TIME));
        archiveTask.setTaskCost(record.get(T.TASK_COST));
        String detail = record.get(T.DETAIL);
        if (StringUtils.isNotBlank(detail)) {
            archiveTask.setDetail(JsonUtils.fromJson(detail, ArchiveTaskExecutionDetail.class));
        }
        return archiveTask;
    }

    @Override
    public void saveArchiveTask(JobInstanceArchiveTaskInfo jobInstanceArchiveTaskInfo) {
        long createTime = System.currentTimeMillis();
        ctx.insertInto(
                T,
                T.TASK_TYPE,
                T.DATA_NODE,
                T.DB_NODE,
                T.DAY,
                T.HOUR,
                T.FROM_TIMESTAMP,
                T.TO_TIMESTAMP,
                T.PROCESS,
                T.STATUS,
                T.CREATE_TIME,
                T.LAST_UPDATE_TIME,
                T.TASK_START_TIME,
                T.TASK_END_TIME,
                T.TASK_COST,
                T.DETAIL)
            .values(
                JooqDataTypeUtil.toByte(jobInstanceArchiveTaskInfo.getTaskType().getType()),
                jobInstanceArchiveTaskInfo.getDbDataNode().toDataNodeId(),
                jobInstanceArchiveTaskInfo.getDbDataNode().toDbNodeId(),
                jobInstanceArchiveTaskInfo.getDay(),
                JooqDataTypeUtil.toByte(jobInstanceArchiveTaskInfo.getHour()),
                jobInstanceArchiveTaskInfo.getFromTimestamp(),
                jobInstanceArchiveTaskInfo.getToTimestamp(),
                jobInstanceArchiveTaskInfo.getProcess() != null ?
                    jobInstanceArchiveTaskInfo.getProcess().toPersistentProcess() : null,
                JooqDataTypeUtil.toByte(jobInstanceArchiveTaskInfo.getStatus().getStatus()),
                createTime,
                createTime,
                null,
                null,
                null,
                null
            )
            .execute();
    }

    @Override
    public List<JobInstanceArchiveTaskInfo> listRunningTasks(ArchiveTaskTypeEnum taskType) {
        Result<Record> result = ctx.select(ALL_FIELDS)
            .from(T)
            .where(T.STATUS.eq(JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.RUNNING.getStatus())))
            .and(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .fetch();

        List<JobInstanceArchiveTaskInfo> tasks = new ArrayList<>(result.size());
        result.forEach(record -> tasks.add(extract(record)));
        return tasks;
    }

    @Override
    public List<JobInstanceArchiveTaskInfo> listTasks(ArchiveTaskTypeEnum taskType,
                                                      ArchiveTaskStatusEnum status,
                                                      int limit) {
        Result<Record> result = ctx.select(ALL_FIELDS)
            .from(T)
            .where(T.STATUS.eq(JooqDataTypeUtil.toByte(status.getStatus())))
            .and(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .limit(limit)
            .fetch();

        List<JobInstanceArchiveTaskInfo> tasks = new ArrayList<>(result.size());
        result.forEach(record -> tasks.add(extract(record)));
        return tasks;
    }

    @Override
    public Map<String, Integer> countScheduleTasksGroupByDb(ArchiveTaskTypeEnum taskType) {
        Result<Record2<String, Integer>> result = ctx.select(T.DB_NODE, DSL.count().as("task_count"))
            .from(T)
            .where(T.STATUS.in(
                JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.PENDING.getStatus()),
                JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.SUSPENDED.getStatus())))
            .and(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .groupBy(T.DB_NODE)
            .fetch();

        Map<String, Integer> dbAndTaskCount = new HashMap<>();
        result.forEach(record -> dbAndTaskCount.put(record.get(T.DB_NODE), (Integer) record.get("task_count")));
        return dbAndTaskCount;
    }


    @Override
    public void updateStartedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                         DbDataNode dataNode,
                                         Integer day,
                                         Integer hour,
                                         Long startTime) {
        ctx.update(T)
            .set(T.LAST_UPDATE_TIME, System.currentTimeMillis())
            .set(T.STATUS, JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.RUNNING.getStatus()))
            .set(T.TASK_START_TIME, startTime)
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DATA_NODE.eq(dataNode.toDataNodeId()))
            .and(T.DAY.eq(day))
            .and(T.HOUR.eq(hour.byteValue()))
            .execute();
    }

    @Override
    public void updateRunningExecuteInfo(ArchiveTaskTypeEnum taskType,
                                         DbDataNode dataNode,
                                         Integer day,
                                         Integer hour,
                                         IdBasedArchiveProcess process) {
        ctx.update(T)
            .set(T.LAST_UPDATE_TIME, System.currentTimeMillis())
            .set(T.PROCESS, process.toPersistentProcess())
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DATA_NODE.eq(dataNode.toDataNodeId()))
            .and(T.DAY.eq(day))
            .and(T.HOUR.eq(hour.byteValue()))
            .execute();
    }

    @Override
    public void updateCompletedExecuteInfo(ArchiveTaskTypeEnum taskType,
                                           DbDataNode dataNode,
                                           Integer day,
                                           Integer hour,
                                           ArchiveTaskStatusEnum status,
                                           IdBasedArchiveProcess process,
                                           Long endTime,
                                           Long cost,
                                           ArchiveTaskExecutionDetail detail) {
        ctx.update(T)
            .set(T.LAST_UPDATE_TIME, System.currentTimeMillis())
            .set(T.STATUS, JooqDataTypeUtil.toByte(status.getStatus()))
            .set(T.PROCESS, process != null ? process.toPersistentProcess() : null)
            .set(T.TASK_END_TIME, endTime)
            .set(T.TASK_COST, cost)
            .set(T.DETAIL, detail != null ? JsonUtils.toJson(detail) : null)
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DATA_NODE.eq(dataNode.toDataNodeId()))
            .and(T.DAY.eq(day))
            .and(T.HOUR.eq(hour.byteValue()))
            .execute();
    }

    @Override
    public JobInstanceArchiveTaskInfo getFirstScheduleArchiveTaskByDb(ArchiveTaskTypeEnum taskType, String dbNodeId) {
        Record record = ctx.select(ALL_FIELDS)
            .from(T)
            .where(T.STATUS.in(
                JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.PENDING.getStatus()),
                JooqDataTypeUtil.toByte(ArchiveTaskStatusEnum.SUSPENDED.getStatus())))
            .and(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DB_NODE.eq(dbNodeId))
            .orderBy(T.DAY.asc(), T.HOUR.asc(), T.DATA_NODE)
            .limit(1)
            .fetchOne();

        return extract(record);
    }

    @Override
    public void updateArchiveTaskStatus(ArchiveTaskTypeEnum taskType,
                                        DbDataNode dataNode,
                                        Integer day,
                                        Integer hour,
                                        ArchiveTaskStatusEnum status) {
        ctx.update(T)
            .set(T.STATUS, JooqDataTypeUtil.toByte(status.getStatus()))
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DATA_NODE.eq(dataNode.toDataNodeId()))
            .and(T.DAY.eq(day))
            .and(T.HOUR.eq(hour.byteValue()))
            .execute();
    }

    @Override
    public Map<ArchiveTaskStatusEnum, Integer> countTaskByStatus(ArchiveTaskTypeEnum taskType,
                                                                 List<ArchiveTaskStatusEnum> statusList) {
        Result<Record2<Byte, Integer>> result = ctx.select(T.STATUS, DSL.count().as("task_count"))
            .from(T)
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.STATUS.in(
                statusList.stream()
                    .map(statusEnum -> JooqDataTypeUtil.toByte(statusEnum.getStatus()))
                    .collect(Collectors.toList()))
            )
            .groupBy(T.STATUS)
            .fetch();
        Map<ArchiveTaskStatusEnum, Integer> taskCountGroupByStatus = new HashMap<>();
        result.forEach(record -> taskCountGroupByStatus.put(
            ArchiveTaskStatusEnum.valOf(record.get(T.STATUS).intValue()),
            (Integer) record.get("task_count"))
        );
        return taskCountGroupByStatus;
    }

    @Override
    public void updateExecutionDetail(ArchiveTaskTypeEnum taskType,
                                      DbDataNode dataNode,
                                      Integer day,
                                      Integer hour,
                                      ArchiveTaskExecutionDetail detail) {
        ctx.update(T)
            .set(T.DETAIL, detail != null ? JsonUtils.toJson(detail) : null)
            .where(T.TASK_TYPE.eq(JooqDataTypeUtil.toByte(taskType.getType())))
            .and(T.DATA_NODE.eq(dataNode.toDataNodeId()))
            .and(T.DAY.eq(day))
            .and(T.HOUR.eq(hour.byteValue()))
            .execute();
    }
}
