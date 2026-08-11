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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalStatusEnum;
import com.tencent.bk.job.analysis.dao.ApprovalTaskDAO;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.analysis.model.tables.ApprovalTask;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class ApprovalTaskDAOImpl implements ApprovalTaskDAO {

    private static final ApprovalTask defaultTable = ApprovalTask.APPROVAL_TASK;

    private final DSLContext dslContext;

    public ApprovalTaskDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public long insertApprovalTask(ApprovalTaskDTO task) {
        Record record = dslContext.insertInto(defaultTable)
            .set(defaultTable.APPROVAL_TASK_ID, task.getApprovalTaskId())
            .set(defaultTable.TENANT_ID, task.getTenantId())
            .set(defaultTable.APP_ID, task.getAppId())
            .set(defaultTable.OPERATION_TYPE, task.getOperationType())
            .set(defaultTable.OPERATION_PARAMS, task.getOperationParams())
            .set(defaultTable.PARAMS_SCHEMA_VERSION, task.getParamsSchemaVersion())
            .set(defaultTable.RESOLVED_SUMMARY, task.getResolvedSummary())
            .set(defaultTable.CREATOR, task.getCreator())
            .set(defaultTable.APP_CODE, task.getAppCode() == null ? "" : task.getAppCode())
            .set(defaultTable.APPROVAL_CHANNEL, task.getApprovalChannel())
            .set(defaultTable.STATUS, task.getStatus())
            .set(defaultTable.EXPIRE_AT, JooqDataTypeUtil.buildULong(task.getExpireAt()))
            .set(defaultTable.CREATE_TIME, JooqDataTypeUtil.buildULong(task.getCreateTime()))
            .returning(defaultTable.ID)
            .fetchOne();
        if (record == null) {
            return 0L;
        }
        Long id = record.get(defaultTable.ID);
        return id == null ? 0L : id;
    }

    @Override
    public ApprovalTaskDTO getByApprovalTaskId(String approvalTaskId) {
        Record record = dslContext.select(
                defaultTable.ID,
                defaultTable.APPROVAL_TASK_ID,
                defaultTable.TENANT_ID,
                defaultTable.APP_ID,
                defaultTable.OPERATION_TYPE,
                defaultTable.OPERATION_PARAMS,
                defaultTable.PARAMS_SCHEMA_VERSION,
                defaultTable.RESOLVED_SUMMARY,
                defaultTable.CREATOR,
                defaultTable.APP_CODE,
                defaultTable.APPROVAL_CHANNEL,
                defaultTable.APPROVAL_TICKET_ID,
                defaultTable.TICKET_FETCHED_AT,
                defaultTable.STATUS,
                defaultTable.APPROVER,
                defaultTable.APPROVED_AT,
                defaultTable.EXECUTE_RESULT,
                defaultTable.EXPIRE_AT,
                defaultTable.CONSUMED_AT,
                defaultTable.DISPATCHED_AT,
                defaultTable.CREATE_TIME
            )
            .from(defaultTable)
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .fetchOne();
        if (record == null) {
            return null;
        }
        return convertToDTO(record);
    }

    @Override
    public int bindTicketIdIfAbsent(String approvalTaskId, String approvalTicketId) {
        return dslContext.update(defaultTable)
            .set(defaultTable.APPROVAL_TICKET_ID, approvalTicketId)
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.APPROVAL_TICKET_ID.isNull())
            .execute();
    }

    @Override
    public int updateTicketFetchedAt(String approvalTaskId, long fetchedAt) {
        return dslContext.update(defaultTable)
            .set(defaultTable.TICKET_FETCHED_AT, JooqDataTypeUtil.buildULong(fetchedAt))
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .execute();
    }

    @Override
    public int casConsumeToExecuting(String approvalTaskId,
                                    String approver,
                                    long approvedAt,
                                    long consumedAt,
                                    long now) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, ApprovalStatusEnum.EXECUTING.name())
            .set(defaultTable.APPROVER, approver)
            .set(defaultTable.APPROVED_AT, JooqDataTypeUtil.buildULong(approvedAt))
            .set(defaultTable.CONSUMED_AT, JooqDataTypeUtil.buildULong(consumedAt))
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.PENDING.name()))
            .and(defaultTable.EXPIRE_AT.gt(JooqDataTypeUtil.buildULong(now)))
            .execute();
    }

    @Override
    public int markDispatched(String approvalTaskId, long dispatchedAt) {
        return dslContext.update(defaultTable)
            .set(defaultTable.DISPATCHED_AT, JooqDataTypeUtil.buildULong(dispatchedAt))
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.EXECUTING.name()))
            .execute();
    }

    @Override
    public int markExecuted(String approvalTaskId, String executeResultJson) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, ApprovalStatusEnum.EXECUTED.name())
            .set(defaultTable.EXECUTE_RESULT, executeResultJson)
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.EXECUTING.name()))
            .execute();
    }

    @Override
    public int markFailed(String approvalTaskId, String errorJson) {
        // 前置条件限定 EXECUTING：状态机中不存在 PENDING → FAILED
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, ApprovalStatusEnum.FAILED.name())
            .set(defaultTable.EXECUTE_RESULT, errorJson)
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.EXECUTING.name()))
            .execute();
    }

    @Override
    public int markRejected(String approvalTaskId, String approver, Long approvedAt, String comment) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, ApprovalStatusEnum.REJECTED.name())
            .set(defaultTable.APPROVER, approver)
            .set(defaultTable.APPROVED_AT, JooqDataTypeUtil.buildULong(approvedAt))
            .set(defaultTable.EXECUTE_RESULT, comment)
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.PENDING.name()))
            .execute();
    }

    @Override
    public int markCanceled(String approvalTaskId) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, ApprovalStatusEnum.CANCELED.name())
            .where(defaultTable.APPROVAL_TASK_ID.eq(approvalTaskId))
            .and(defaultTable.STATUS.eq(ApprovalStatusEnum.PENDING.name()))
            .execute();
    }

    @Override
    public int deleteByCreateTimeBefore(long maxCreateTime, int limit) {
        // 显式排除 EXECUTING：这类任务正是需要人工介入的对象，静默删掉会毁掉排障线索
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.CREATE_TIME.lessOrEqual(JooqDataTypeUtil.buildULong(maxCreateTime)))
            .and(defaultTable.STATUS.ne(ApprovalStatusEnum.EXECUTING.name()))
            .limit(limit)
            .execute();
    }

    private ApprovalTaskDTO convertToDTO(Record record) {
        ApprovalTaskDTO dto = new ApprovalTaskDTO();
        dto.setId(record.get(defaultTable.ID));
        dto.setApprovalTaskId(record.get(defaultTable.APPROVAL_TASK_ID));
        dto.setTenantId(record.get(defaultTable.TENANT_ID));
        dto.setAppId(record.get(defaultTable.APP_ID));
        dto.setOperationType(record.get(defaultTable.OPERATION_TYPE));
        dto.setOperationParams(record.get(defaultTable.OPERATION_PARAMS));
        dto.setParamsSchemaVersion(record.get(defaultTable.PARAMS_SCHEMA_VERSION));
        dto.setResolvedSummary(record.get(defaultTable.RESOLVED_SUMMARY));
        dto.setCreator(record.get(defaultTable.CREATOR));
        dto.setAppCode(record.get(defaultTable.APP_CODE));
        dto.setApprovalChannel(record.get(defaultTable.APPROVAL_CHANNEL));
        dto.setApprovalTicketId(record.get(defaultTable.APPROVAL_TICKET_ID));
        dto.setTicketFetchedAt(JooqDataTypeUtil.buildLong(record.get(defaultTable.TICKET_FETCHED_AT)));
        dto.setStatus(record.get(defaultTable.STATUS));
        dto.setApprover(record.get(defaultTable.APPROVER));
        dto.setApprovedAt(JooqDataTypeUtil.buildLong(record.get(defaultTable.APPROVED_AT)));
        dto.setExecuteResult(record.get(defaultTable.EXECUTE_RESULT));
        dto.setExpireAt(JooqDataTypeUtil.buildLong(record.get(defaultTable.EXPIRE_AT)));
        dto.setConsumedAt(JooqDataTypeUtil.buildLong(record.get(defaultTable.CONSUMED_AT)));
        dto.setDispatchedAt(JooqDataTypeUtil.buildLong(record.get(defaultTable.DISPATCHED_AT)));
        dto.setCreateTime(JooqDataTypeUtil.buildLong(record.get(defaultTable.CREATE_TIME)));
        return dto;
    }
}
