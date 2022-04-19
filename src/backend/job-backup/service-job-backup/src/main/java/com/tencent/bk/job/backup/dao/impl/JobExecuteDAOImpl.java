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

package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.dao.JobExecuteDAO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.generated.tables.*;
import org.jooq.generated.tables.records.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;

/**
 * @since 17/3/2021 15:46
 */
@Slf4j
public class JobExecuteDAOImpl implements JobExecuteDAO {

    private static final FileSourceTaskLog FILE_SOURCE_TASK_LOG_TABLE = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
    private static final List<Field<?>> FILE_SOURCE_TASK_LOG_FIELDS =
        Arrays.asList(FILE_SOURCE_TASK_LOG_TABLE.STEP_INSTANCE_ID, FILE_SOURCE_TASK_LOG_TABLE.EXECUTE_COUNT,
            FILE_SOURCE_TASK_LOG_TABLE.START_TIME, FILE_SOURCE_TASK_LOG_TABLE.END_TIME,
            FILE_SOURCE_TASK_LOG_TABLE.TOTAL_TIME, FILE_SOURCE_TASK_LOG_TABLE.STATUS,
            FILE_SOURCE_TASK_LOG_TABLE.FILE_SOURCE_BATCH_TASK_ID, FILE_SOURCE_TASK_LOG_TABLE.ROW_CREATE_TIME,
            FILE_SOURCE_TASK_LOG_TABLE.ROW_UPDATE_TIME);

    private static final GseTaskIpLog GSE_TASK_IP_LOG_TABLE = GseTaskIpLog.GSE_TASK_IP_LOG;
    private static final List<Field<?>> GSE_TASK_IP_LOG_FIELDS = Arrays.asList(GSE_TASK_IP_LOG_TABLE.STEP_INSTANCE_ID,
        GSE_TASK_IP_LOG_TABLE.EXECUTE_COUNT,
        GSE_TASK_IP_LOG_TABLE.IP, GSE_TASK_IP_LOG_TABLE.STATUS, GSE_TASK_IP_LOG_TABLE.START_TIME,
        GSE_TASK_IP_LOG_TABLE.END_TIME, GSE_TASK_IP_LOG_TABLE.TOTAL_TIME, GSE_TASK_IP_LOG_TABLE.ERROR_CODE,
        GSE_TASK_IP_LOG_TABLE.EXIT_CODE, GSE_TASK_IP_LOG_TABLE.TAG, GSE_TASK_IP_LOG_TABLE.LOG_OFFSET,
        GSE_TASK_IP_LOG_TABLE.DISPLAY_IP, GSE_TASK_IP_LOG_TABLE.IS_TARGET,
        GSE_TASK_IP_LOG_TABLE.ROW_CREATE_TIME, GSE_TASK_IP_LOG_TABLE.ROW_UPDATE_TIME,
        GSE_TASK_IP_LOG_TABLE.IS_SOURCE);

    private static final GseTaskLog GSE_TASK_LOG_TABLE = GseTaskLog.GSE_TASK_LOG;
    private static final List<Field<?>> GSE_TASK_LOG_FIELDS = Arrays.asList(GSE_TASK_LOG_TABLE.STEP_INSTANCE_ID,
        GSE_TASK_LOG_TABLE.EXECUTE_COUNT,
        GSE_TASK_LOG_TABLE.START_TIME, GSE_TASK_LOG_TABLE.END_TIME, GSE_TASK_LOG_TABLE.TOTAL_TIME,
        GSE_TASK_LOG_TABLE.STATUS, GSE_TASK_LOG_TABLE.GSE_TASK_ID, GSE_TASK_LOG_TABLE.ROW_CREATE_TIME,
        GSE_TASK_LOG_TABLE.ROW_UPDATE_TIME);

    private static final OperationLog OPERATION_LOG_TABLE = OperationLog.OPERATION_LOG;
    private static final List<Field<?>> OPERATION_LOG_FIELDS = Arrays.asList(OPERATION_LOG_TABLE.ID,
        OPERATION_LOG_TABLE.TASK_INSTANCE_ID, OPERATION_LOG_TABLE.OP_CODE, OPERATION_LOG_TABLE.OPERATOR,
        OPERATION_LOG_TABLE.DETAIL, OPERATION_LOG_TABLE.CREATE_TIME, OPERATION_LOG_TABLE.ROW_CREATE_TIME,
        OPERATION_LOG_TABLE.ROW_UPDATE_TIME);

    private static final StepInstance STEP_INSTANCE_TABLE = StepInstance.STEP_INSTANCE;
    private static final List<Field<?>> STEP_INSTANCE_FIELDS = Arrays.asList(STEP_INSTANCE_TABLE.ID,
        STEP_INSTANCE_TABLE.STEP_ID,
        STEP_INSTANCE_TABLE.TASK_INSTANCE_ID, STEP_INSTANCE_TABLE.APP_ID, STEP_INSTANCE_TABLE.NAME,
        STEP_INSTANCE_TABLE.TYPE, STEP_INSTANCE_TABLE.OPERATOR, STEP_INSTANCE_TABLE.STATUS,
        STEP_INSTANCE_TABLE.EXECUTE_COUNT, STEP_INSTANCE_TABLE.TARGET_SERVERS,
        STEP_INSTANCE_TABLE.ABNORMAL_AGENT_IP_LIST, STEP_INSTANCE_TABLE.START_TIME,
        STEP_INSTANCE_TABLE.END_TIME, STEP_INSTANCE_TABLE.TOTAL_TIME, STEP_INSTANCE_TABLE.TOTAL_IP_NUM,
        STEP_INSTANCE_TABLE.ABNORMAL_AGENT_NUM, STEP_INSTANCE_TABLE.RUN_IP_NUM, STEP_INSTANCE_TABLE.FAIL_IP_NUM,
        STEP_INSTANCE_TABLE.SUCCESS_IP_NUM, STEP_INSTANCE_TABLE.CREATE_TIME, STEP_INSTANCE_TABLE.IGNORE_ERROR,
        STEP_INSTANCE_TABLE.ROW_CREATE_TIME, STEP_INSTANCE_TABLE.ROW_UPDATE_TIME, STEP_INSTANCE_TABLE.STEP_NUM,
        STEP_INSTANCE_TABLE.STEP_ORDER);

    private static final StepInstanceConfirm STEP_INSTANCE_CONFIRM_TABLE = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
    private static final List<Field<?>> STEP_INSTANCE_CONFIRM_FIELDS =
        Arrays.asList(STEP_INSTANCE_CONFIRM_TABLE.STEP_INSTANCE_ID, STEP_INSTANCE_CONFIRM_TABLE.CONFIRM_MESSAGE,
        STEP_INSTANCE_CONFIRM_TABLE.CONFIRM_USERS, STEP_INSTANCE_CONFIRM_TABLE.CONFIRM_ROLES,
        STEP_INSTANCE_CONFIRM_TABLE.NOTIFY_CHANNELS, STEP_INSTANCE_CONFIRM_TABLE.ROW_CREATE_TIME,
        STEP_INSTANCE_CONFIRM_TABLE.ROW_UPDATE_TIME, STEP_INSTANCE_CONFIRM_TABLE.CONFIRM_REASON);

    private static final StepInstanceFile STEP_INSTANCE_FILE_TABLE = StepInstanceFile.STEP_INSTANCE_FILE;
    private static final List<Field<?>> STEP_INSTANCE_FILE_FIELDS =
        Arrays.asList(STEP_INSTANCE_FILE_TABLE.STEP_INSTANCE_ID, STEP_INSTANCE_FILE_TABLE.FILE_SOURCE,
        STEP_INSTANCE_FILE_TABLE.RESOLVED_FILE_SOURCE, STEP_INSTANCE_FILE_TABLE.FILE_TARGET_PATH,
        STEP_INSTANCE_FILE_TABLE.RESOLVED_FILE_TARGET_PATH, STEP_INSTANCE_FILE_TABLE.FILE_UPLOAD_SPEED_LIMIT,
        STEP_INSTANCE_FILE_TABLE.FILE_DOWNLOAD_SPEED_LIMIT, STEP_INSTANCE_FILE_TABLE.FILE_DUPLICATE_HANDLE,
        STEP_INSTANCE_FILE_TABLE.NOT_EXIST_PATH_HANDLER, STEP_INSTANCE_FILE_TABLE.EXECUTION_TIMEOUT,
        STEP_INSTANCE_FILE_TABLE.SYSTEM_ACCOUNT_ID, STEP_INSTANCE_FILE_TABLE.SYSTEM_ACCOUNT,
        STEP_INSTANCE_FILE_TABLE.ROW_CREATE_TIME, STEP_INSTANCE_FILE_TABLE.ROW_UPDATE_TIME);

    private static final StepInstanceScript STEP_INSTANCE_SCRIPT_TABLE = StepInstanceScript.STEP_INSTANCE_SCRIPT;
    private static final List<Field<?>> STEP_INSTANCE_SCRIPT_FIELDS =
        Arrays.asList(STEP_INSTANCE_SCRIPT_TABLE.STEP_INSTANCE_ID, STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_CONTENT,
        STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_TYPE, STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_PARAM,
        STEP_INSTANCE_SCRIPT_TABLE.RESOLVED_SCRIPT_PARAM, STEP_INSTANCE_SCRIPT_TABLE.EXECUTION_TIMEOUT,
        STEP_INSTANCE_SCRIPT_TABLE.SYSTEM_ACCOUNT_ID, STEP_INSTANCE_SCRIPT_TABLE.SYSTEM_ACCOUNT,
        STEP_INSTANCE_SCRIPT_TABLE.DB_ACCOUNT_ID, STEP_INSTANCE_SCRIPT_TABLE.DB_TYPE,
        STEP_INSTANCE_SCRIPT_TABLE.DB_ACCOUNT, STEP_INSTANCE_SCRIPT_TABLE.DB_PASSWORD,
        STEP_INSTANCE_SCRIPT_TABLE.DB_PORT, STEP_INSTANCE_SCRIPT_TABLE.ROW_CREATE_TIME,
        STEP_INSTANCE_SCRIPT_TABLE.ROW_UPDATE_TIME, STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_SOURCE,
        STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_ID, STEP_INSTANCE_SCRIPT_TABLE.SCRIPT_VERSION_ID,
        STEP_INSTANCE_SCRIPT_TABLE.IS_SECURE_PARAM);

    private static final StepInstanceVariable STEP_INSTANCE_VARIABLE_TABLE =
        StepInstanceVariable.STEP_INSTANCE_VARIABLE;
    private static final List<Field<?>> STEP_INSTANCE_VARIABLE_FIELDS =
        Arrays.asList(STEP_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID, STEP_INSTANCE_VARIABLE_TABLE.STEP_INSTANCE_ID,
        STEP_INSTANCE_VARIABLE_TABLE.TYPE, STEP_INSTANCE_VARIABLE_TABLE.PARAM_VALUES,
        STEP_INSTANCE_VARIABLE_TABLE.ROW_CREATE_TIME, STEP_INSTANCE_VARIABLE_TABLE.ROW_UPDATE_TIME);
    private static final TaskInstance TASK_INSTANCE_TABLE = TaskInstance.TASK_INSTANCE;
    private static final List<Field<?>> TASK_INSTANCE_FIELDS = Arrays.asList(TASK_INSTANCE_TABLE.ID,
        TASK_INSTANCE_TABLE.APP_ID, TASK_INSTANCE_TABLE.TASK_ID,
        TASK_INSTANCE_TABLE.TASK_TEMPLATE_ID, TASK_INSTANCE_TABLE.NAME, TASK_INSTANCE_TABLE.TYPE,
        TASK_INSTANCE_TABLE.OPERATOR, TASK_INSTANCE_TABLE.STATUS, TASK_INSTANCE_TABLE.CURRENT_STEP_ID,
        TASK_INSTANCE_TABLE.STARTUP_MODE, TASK_INSTANCE_TABLE.TOTAL_TIME, TASK_INSTANCE_TABLE.CALLBACK_URL, TASK_INSTANCE_TABLE.CALLBACK,
        TASK_INSTANCE_TABLE.IS_DEBUG_TASK, TASK_INSTANCE_TABLE.CRON_TASK_ID, TASK_INSTANCE_TABLE.CREATE_TIME,
        TASK_INSTANCE_TABLE.START_TIME, TASK_INSTANCE_TABLE.END_TIME, TASK_INSTANCE_TABLE.APP_CODE,
        TASK_INSTANCE_TABLE.ROW_CREATE_TIME, TASK_INSTANCE_TABLE.ROW_UPDATE_TIME);

    private static final TaskInstanceVariable TASK_INSTANCE_VARIABLE_TABLE =
        TaskInstanceVariable.TASK_INSTANCE_VARIABLE;
    private static final List<Field<?>> TASK_INSTANCE_VARIABLE_FIELDS = Arrays.asList(TASK_INSTANCE_VARIABLE_TABLE.ID,
        TASK_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID,
        TASK_INSTANCE_VARIABLE_TABLE.NAME, TASK_INSTANCE_VARIABLE_TABLE.TYPE,
        TASK_INSTANCE_VARIABLE_TABLE.VALUE, TASK_INSTANCE_VARIABLE_TABLE.IS_CHANGEABLE,
        TASK_INSTANCE_VARIABLE_TABLE.ROW_CREATE_TIME, TASK_INSTANCE_VARIABLE_TABLE.ROW_UPDATE_TIME);
    private final DSLContext context;

    public JobExecuteDAOImpl(DSLContext context) {
        log.info("Init ExecuteReadDAO.");
        this.context = context;
    }

    @Override
    public Long getMaxNeedArchiveTaskInstanceId(Long endTime) {
        Record1<Long> maxNeedTaskInstanceIdRecord =
            context.select(max(TASK_INSTANCE_TABLE.ID))
                .from(TASK_INSTANCE_TABLE)
                .where(TASK_INSTANCE_TABLE.CREATE_TIME.lessOrEqual(endTime))
                .fetchOne();
        if (maxNeedTaskInstanceIdRecord != null) {
            Long maxNeedTaskInstanceId = (Long) maxNeedTaskInstanceIdRecord.get(0);
            if (maxNeedTaskInstanceId != null) {
                return maxNeedTaskInstanceId;
            }
        }
        return 0L;
    }

    @Override
    public Long getMaxNeedArchiveStepInstanceId(Long taskInstanceId) {
        Record1<Long> maxNeedStepInstanceIdRecord =
            context.select(max(STEP_INSTANCE_TABLE.ID))
                .from(STEP_INSTANCE_TABLE)
                .where(STEP_INSTANCE_TABLE.TASK_INSTANCE_ID.lessOrEqual(taskInstanceId))
                .fetchOne();
        if (maxNeedStepInstanceIdRecord != null) {
            Long maxNeedStepInstanceId = (Long) maxNeedStepInstanceIdRecord.get(0);
            if (maxNeedStepInstanceId != null) {
                return maxNeedStepInstanceId;
            }
        }
        return 0L;
    }

    public <T extends Record> Long getFirstInstanceId(Table<T> table, TableField<T, Long> field) {
        Record1<Long> firstInstanceIdRecord = context.select(min(field)).from(table).fetchOne();
        if (firstInstanceIdRecord != null && firstInstanceIdRecord.get(0) != null) {
            return (Long) firstInstanceIdRecord.get(0);
        }
        return 0L;
    }

    @Override
    public List<Field<?>> getFileSourceTaskLogFields() {
        return FILE_SOURCE_TASK_LOG_FIELDS;
    }

    @Override
    public List<Field<?>> getGseTaskIpLogFields() {
        return GSE_TASK_IP_LOG_FIELDS;
    }

    @Override
    public List<Field<?>> getGseTaskLogFields() {
        return GSE_TASK_LOG_FIELDS;
    }

    @Override
    public List<Field<?>> getStepInstanceFields() {
        return STEP_INSTANCE_FIELDS;
    }

    @Override
    public List<Field<?>> getStepInstanceConfirmFields() {
        return STEP_INSTANCE_CONFIRM_FIELDS;
    }

    @Override
    public List<Field<?>> getStepInstanceFileFields() {
        return STEP_INSTANCE_FILE_FIELDS;
    }

    @Override
    public List<Field<?>> getStepInstanceScriptFields() {
        return STEP_INSTANCE_SCRIPT_FIELDS;
    }

    @Override
    public List<Field<?>> getStepInstanceVariableFields() {
        return STEP_INSTANCE_VARIABLE_FIELDS;
    }

    @Override
    public List<Field<?>> getTaskInstanceFields() {
        return TASK_INSTANCE_FIELDS;
    }

    @Override
    public List<Field<?>> getTaskInstanceVariableFields() {
        return TASK_INSTANCE_VARIABLE_FIELDS;
    }

    @Override
    public List<Field<?>> getOperationLogFields() {
        return OPERATION_LOG_FIELDS;
    }

    @Override
    public List<FileSourceTaskLogRecord> listFileSourceTaskLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(FILE_SOURCE_TASK_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(FILE_SOURCE_TASK_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(FILE_SOURCE_TASK_LOG_FIELDS)
                .from(FILE_SOURCE_TASK_LOG_TABLE)
                .where(conditions)
                .fetch();

        return record.into(FILE_SOURCE_TASK_LOG_TABLE);
    }

    @Override
    public List<GseTaskIpLogRecord> listGseTaskIpLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(GSE_TASK_IP_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(GSE_TASK_IP_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(GSE_TASK_IP_LOG_FIELDS)
                .from(GSE_TASK_IP_LOG_TABLE)
                .where(conditions)
                .fetch();

        return record.into(GSE_TASK_IP_LOG_TABLE);
    }

    @Override
    public List<GseTaskLogRecord> listGseTaskLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(GSE_TASK_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(GSE_TASK_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(GSE_TASK_LOG_FIELDS)
                .from(GSE_TASK_LOG_TABLE)
                .where(conditions)
                .fetch();

        return record.into(GSE_TASK_LOG_TABLE);
    }

    @Override
    public List<StepInstanceRecord> listStepInstance(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_TABLE.ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_TABLE.ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(STEP_INSTANCE_FIELDS)
                .from(STEP_INSTANCE_TABLE)
                .where(conditions)
                .fetch();

        return record.into(STEP_INSTANCE_TABLE);
    }

    @Override
    public List<StepInstanceConfirmRecord> listStepInstanceConfirm(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_CONFIRM_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_CONFIRM_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(STEP_INSTANCE_CONFIRM_FIELDS)
                .from(STEP_INSTANCE_CONFIRM_TABLE)
                .where(conditions)
                .fetch();

        return record.into(STEP_INSTANCE_CONFIRM_TABLE);
    }

    @Override
    public List<StepInstanceFileRecord> listStepInstanceFile(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_FILE_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_FILE_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(STEP_INSTANCE_FILE_FIELDS)
                .from(STEP_INSTANCE_FILE_TABLE)
                .where(conditions)
                .fetch();

        return record.into(STEP_INSTANCE_FILE_TABLE);
    }

    @Override
    public List<StepInstanceScriptRecord> listStepInstanceScript(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_SCRIPT_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_SCRIPT_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(STEP_INSTANCE_SCRIPT_FIELDS)
                .from(STEP_INSTANCE_SCRIPT_TABLE)
                .where(conditions)
                .fetch();

        return record.into(STEP_INSTANCE_SCRIPT_TABLE);
    }

    @Override
    public List<StepInstanceVariableRecord> listStepInstanceVariable(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_VARIABLE_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_VARIABLE_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(STEP_INSTANCE_VARIABLE_FIELDS)
                .from(STEP_INSTANCE_VARIABLE_TABLE)
                .where(conditions)
                .fetch();

        return record.into(STEP_INSTANCE_VARIABLE_TABLE);
    }

    @Override
    public List<TaskInstanceRecord> listTaskInstance(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_TABLE.ID.greaterThan(start));
        conditions.add(TASK_INSTANCE_TABLE.ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(TASK_INSTANCE_FIELDS)
                .from(TASK_INSTANCE_TABLE)
                .where(conditions)
                .fetch();

        return record.into(TASK_INSTANCE_TABLE);
    }

    @Override
    public List<TaskInstanceVariableRecord> listTaskInstanceVariable(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID.greaterThan(start));
        conditions.add(TASK_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(TASK_INSTANCE_VARIABLE_FIELDS)
                .from(TASK_INSTANCE_VARIABLE_TABLE)
                .where(conditions)
                .fetch();

        return record.into(TASK_INSTANCE_VARIABLE_TABLE);
    }

    @Override
    public List<OperationLogRecord> listOperationLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(OPERATION_LOG_TABLE.TASK_INSTANCE_ID.greaterThan(start));
        conditions.add(OPERATION_LOG_TABLE.TASK_INSTANCE_ID.lessOrEqual(stop));

        Result<Record> record =
            context.select(OPERATION_LOG_FIELDS)
                .from(OPERATION_LOG_TABLE)
                .where(conditions)
                .fetch();

        return record.into(OPERATION_LOG_TABLE);
    }

    @Override
    public int deleteFileSourceTaskLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(FILE_SOURCE_TASK_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(FILE_SOURCE_TASK_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(FILE_SOURCE_TASK_LOG_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteGseTaskIpLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(GSE_TASK_IP_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(GSE_TASK_IP_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(GSE_TASK_IP_LOG_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteGseTaskLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(GSE_TASK_LOG_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(GSE_TASK_LOG_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(GSE_TASK_LOG_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteOperationLog(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(OPERATION_LOG_TABLE.TASK_INSTANCE_ID.greaterThan(start));
        conditions.add(OPERATION_LOG_TABLE.TASK_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(OPERATION_LOG_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteStepInstance(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_TABLE.ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_TABLE.ID.lessOrEqual(stop));
        return context.delete(STEP_INSTANCE_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteStepInstanceConfirm(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_CONFIRM_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_CONFIRM_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(STEP_INSTANCE_CONFIRM_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteStepInstanceFile(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_FILE_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_FILE_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(STEP_INSTANCE_FILE_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteStepInstanceScript(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_SCRIPT_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_SCRIPT_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(STEP_INSTANCE_SCRIPT_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteStepInstanceVariable(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(STEP_INSTANCE_VARIABLE_TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(STEP_INSTANCE_VARIABLE_TABLE.STEP_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(STEP_INSTANCE_VARIABLE_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteTaskInstance(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_TABLE.ID.greaterThan(start));
        conditions.add(TASK_INSTANCE_TABLE.ID.lessOrEqual(stop));
        return context.delete(TASK_INSTANCE_TABLE).where(conditions).execute();
    }

    @Override
    public int deleteTaskInstanceVariable(Long start, Long stop) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TASK_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID.greaterThan(start));
        conditions.add(TASK_INSTANCE_VARIABLE_TABLE.TASK_INSTANCE_ID.lessOrEqual(stop));
        return context.delete(TASK_INSTANCE_VARIABLE_TABLE).where(conditions).execute();
    }
}
