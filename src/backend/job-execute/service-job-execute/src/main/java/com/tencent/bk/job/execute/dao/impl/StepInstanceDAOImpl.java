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

package com.tencent.bk.job.execute.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.ScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.UpdateSetMoreStep;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.StepInstance;
import org.jooq.generated.tables.StepInstanceConfirm;
import org.jooq.generated.tables.StepInstanceFile;
import org.jooq.generated.tables.StepInstanceScript;
import org.jooq.generated.tables.TaskInstance;
import org.jooq.generated.tables.records.StepInstanceRecord;
import org.jooq.types.UByte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.sum;

@Slf4j
@Repository
public class StepInstanceDAOImpl implements StepInstanceDAO {

    private static final TaskInstance TABLE_TASK_INSTANCE = TaskInstance.TASK_INSTANCE;
    private static final StepInstance TABLE_STEP_INSTANCE = StepInstance.STEP_INSTANCE;
    private static final StepInstanceScript TABLE_STEP_INSTANCE_SCRIPT = StepInstanceScript.STEP_INSTANCE_SCRIPT;
    private static final StepInstanceFile TABLE_STEP_INSTANCE_FILE = StepInstanceFile.STEP_INSTANCE_FILE;
    private DSLContext ctx;

    @Autowired
    public StepInstanceDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Long addStepInstanceBase(StepInstanceBaseDTO stepInstance) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.insertInto(t, t.STEP_ID, t.TASK_INSTANCE_ID, t.APP_ID, t.NAME, t.TYPE,
            t.OPERATOR, t.STATUS, t.EXECUTE_COUNT, t.START_TIME, t.END_TIME, t.TOTAL_TIME,
            t.TARGET_SERVERS, t.ABNORMAL_AGENT_IP_LIST, t.TOTAL_IP_NUM, t.ABNORMAL_AGENT_NUM,
            t.RUN_IP_NUM, t.FAIL_IP_NUM, t.SUCCESS_IP_NUM, t.CREATE_TIME, t.IGNORE_ERROR, t.STEP_NUM,
            t.STEP_ORDER)
            .values(stepInstance.getStepId(),
                stepInstance.getTaskInstanceId(),
                stepInstance.getAppId(),
                stepInstance.getName(),
                JooqDataTypeUtil.getByteFromInteger(stepInstance.getExecuteType()),
                stepInstance.getOperator(),
                JooqDataTypeUtil.getByteFromInteger(stepInstance.getStatus()),
                stepInstance.getExecuteCount(),
                stepInstance.getStartTime(),
                stepInstance.getEndTime(),
                stepInstance.getTotalTime(),
                stepInstance.getTargetServers() == null ? null : JsonUtils.toJson(stepInstance.getTargetServers()),
                stepInstance.getBadIpList(),
                stepInstance.getTotalIPNum(),
                stepInstance.getBadIPNum(),
                stepInstance.getRunIPNum(),
                stepInstance.getFailIPNum(),
                stepInstance.getSuccessIPNum(),
                stepInstance.getCreateTime(),
                stepInstance.isIgnoreError() ? Byte.valueOf("1") : Byte.valueOf("0"),
                stepInstance.getStepNum(),
                stepInstance.getStepOrder())
            .returning(t.ID).fetchOne();
        return record.getValue(t.ID);
    }

    @Override
    public void addScriptStepInstance(StepInstanceDTO stepInstance) {
        Integer scriptSource = stepInstance.getScriptSource();
        byte scriptSourceByteValue = 1;
        if (scriptSource != null) {
            scriptSourceByteValue = scriptSource.byteValue();
        }
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        ctx.insertInto(t, t.STEP_INSTANCE_ID, t.SCRIPT_CONTENT, t.SCRIPT_TYPE, t.SCRIPT_PARAM, t.RESOLVED_SCRIPT_PARAM,
            t.EXECUTION_TIMEOUT, t.SYSTEM_ACCOUNT_ID, t.SYSTEM_ACCOUNT, t.DB_ACCOUNT_ID,
            t.DB_TYPE, t.DB_ACCOUNT, t.DB_PASSWORD, t.DB_PORT, t.SCRIPT_SOURCE, t.SCRIPT_ID, t.SCRIPT_VERSION_ID,
            t.IS_SECURE_PARAM)
            .values(stepInstance.getId(),
                stepInstance.getScriptContent(),
                JooqDataTypeUtil.getByteFromInteger(stepInstance.getScriptType()),
                stepInstance.getScriptParam(),
                stepInstance.getResolvedScriptParam(),
                stepInstance.getTimeout(),
                stepInstance.getAccountId(),
                stepInstance.getAccount(),
                stepInstance.getDbAccountId(),
                JooqDataTypeUtil.getByteFromInteger(stepInstance.getDbType()),
                stepInstance.getDbAccount(),
                stepInstance.getDbPass(),
                stepInstance.getDbPort(),
                scriptSourceByteValue,
                stepInstance.getScriptId(),
                stepInstance.getScriptVersionId(),
                stepInstance.isSecureParam() ? JooqDataTypeUtil.getByteFromInteger(1) :
                    JooqDataTypeUtil.getByteFromInteger(0)
            ).execute();
    }

    @Override
    public void addFileStepInstance(StepInstanceDTO stepInstance) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        ctx.insertInto(t, t.STEP_INSTANCE_ID, t.FILE_SOURCE, t.FILE_TARGET_PATH, t.FILE_UPLOAD_SPEED_LIMIT,
            t.FILE_DOWNLOAD_SPEED_LIMIT, t.FILE_DUPLICATE_HANDLE, t.NOT_EXIST_PATH_HANDLER,
            t.EXECUTION_TIMEOUT, t.SYSTEM_ACCOUNT_ID, t.SYSTEM_ACCOUNT)
            .values(stepInstance.getId(),
                JsonUtils.toJson(stepInstance.getFileSourceList()),
                stepInstance.getFileTargetPath(),
                stepInstance.getFileUploadSpeedLimit(),
                stepInstance.getFileDownloadSpeedLimit(),
                JooqDataTypeUtil.getByteFromInteger(stepInstance.getFileDuplicateHandle()),
                JooqDataTypeUtil.getUByteFromInteger(stepInstance.getNotExistPathHandler()),
                stepInstance.getTimeout(),
                stepInstance.getAccountId(),
                stepInstance.getAccount()
            ).execute();
    }

    @Override
    public void addConfirmStepInstance(StepInstanceDTO stepInstance) {
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        ctx.insertInto(t, t.STEP_INSTANCE_ID, t.CONFIRM_MESSAGE, t.CONFIRM_USERS, t.CONFIRM_ROLES, t.NOTIFY_CHANNELS)
            .values(stepInstance.getId(),
                stepInstance.getConfirmMessage(),
                stepInstance.getConfirmUsers() == null ? null :
                    Utils.concatStringWithSeperator(stepInstance.getConfirmUsers(), ","),
                stepInstance.getConfirmRoles() == null ? null :
                    Utils.concatStringWithSeperator(stepInstance.getConfirmRoles(), ","),
                stepInstance.getNotifyChannels() == null ? null :
                    Utils.concatStringWithSeperator(stepInstance.getNotifyChannels(), ","))
            .execute();
    }

    @Override
    public ScriptStepInstanceDTO getScriptStepInstance(long stepInstanceId) {
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        Record record = ctx.select(t.STEP_INSTANCE_ID, t.SCRIPT_CONTENT, t.SCRIPT_TYPE, t.SCRIPT_PARAM,
            t.RESOLVED_SCRIPT_PARAM, t.EXECUTION_TIMEOUT, t.SYSTEM_ACCOUNT_ID, t.SYSTEM_ACCOUNT,
            t.DB_ACCOUNT_ID, t.DB_ACCOUNT, t.DB_TYPE, t.DB_PASSWORD, t.DB_PORT, t.SCRIPT_SOURCE, t.SCRIPT_ID,
            t.SCRIPT_VERSION_ID, t.IS_SECURE_PARAM
        ).from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId)).fetchOne();
        return extractScriptInfo(record);
    }

    private ScriptStepInstanceDTO extractScriptInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        ScriptStepInstanceDTO stepInstance = new ScriptStepInstanceDTO();
        stepInstance.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        stepInstance.setScriptContent(record.get(t.SCRIPT_CONTENT));
        stepInstance.setScriptType(JooqDataTypeUtil.getIntegerFromByte(record.get(t.SCRIPT_TYPE)));
        stepInstance.setScriptParam(record.get(t.SCRIPT_PARAM));
        stepInstance.setResolvedScriptParam(record.get(t.RESOLVED_SCRIPT_PARAM));
        stepInstance.setTimeout(record.get(t.EXECUTION_TIMEOUT));
        stepInstance.setAccountId(record.get(t.SYSTEM_ACCOUNT_ID));
        stepInstance.setAccount(record.get(t.SYSTEM_ACCOUNT));
        stepInstance.setDbAccountId(record.get(t.DB_ACCOUNT_ID));
        stepInstance.setDbType(JooqDataTypeUtil.getIntegerFromByte(record.get(t.DB_TYPE)));
        stepInstance.setDbAccount(record.get(t.DB_ACCOUNT));
        stepInstance.setDbPass(record.get(t.DB_PASSWORD));
        stepInstance.setDbPort(record.get(t.DB_PORT));
        stepInstance.setSecureParam(record.get(t.IS_SECURE_PARAM).intValue() == 1);
        Byte scriptSource = record.get(t.SCRIPT_SOURCE);
        if (scriptSource == null) {
            stepInstance.setScriptSource(1);
        } else {
            stepInstance.setScriptSource(scriptSource.intValue());
        }
        stepInstance.setScriptId(record.get(t.SCRIPT_ID));
        stepInstance.setScriptVersionId(record.get(t.SCRIPT_VERSION_ID));
        return stepInstance;
    }

    @Override
    public FileStepInstanceDTO getFileStepInstance(long stepInstanceId) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        Record record = ctx.select(t.STEP_INSTANCE_ID, t.FILE_SOURCE, t.RESOLVED_FILE_SOURCE, t.FILE_TARGET_PATH,
            t.RESOLVED_FILE_TARGET_PATH, t.FILE_UPLOAD_SPEED_LIMIT, t.FILE_DOWNLOAD_SPEED_LIMIT,
            t.FILE_DUPLICATE_HANDLE,
            t.NOT_EXIST_PATH_HANDLER, t.EXECUTION_TIMEOUT, t.SYSTEM_ACCOUNT_ID, t.SYSTEM_ACCOUNT)
            .from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId)).fetchOne();
        return extractFileInfo(record);
    }

    private FileStepInstanceDTO extractFileInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        FileStepInstanceDTO stepInstance = new FileStepInstanceDTO();
        stepInstance.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        stepInstance.setTimeout(record.get(t.EXECUTION_TIMEOUT));
        List<FileSourceDTO> fileSourceList = JsonUtils.fromJson(record.get(t.FILE_SOURCE),
            new TypeReference<List<FileSourceDTO>>() {
        });
        stepInstance.setFileSourceList(fileSourceList);
        if (StringUtils.isNotEmpty(record.get(t.RESOLVED_FILE_SOURCE))) {
            List<FileSourceDTO> resolvedFileSourceList = JsonUtils.fromJson(record.get(t.RESOLVED_FILE_SOURCE),
                new TypeReference<List<FileSourceDTO>>() {
            });
            stepInstance.setResolvedFileSourceList(resolvedFileSourceList);
        }
        stepInstance.setFileTargetPath(record.get(t.FILE_TARGET_PATH));
        stepInstance.setResolvedFileTargetPath(record.get(t.RESOLVED_FILE_TARGET_PATH));
        stepInstance.setAccountId(record.get(t.SYSTEM_ACCOUNT_ID));
        stepInstance.setAccount(record.get(t.SYSTEM_ACCOUNT));
        stepInstance.setFileUploadSpeedLimit(record.get(t.FILE_UPLOAD_SPEED_LIMIT));
        stepInstance.setFileDownloadSpeedLimit(record.get(t.FILE_DOWNLOAD_SPEED_LIMIT));
        stepInstance.setFileDuplicateHandle(JooqDataTypeUtil.getIntegerFromByte(record.get(t.FILE_DUPLICATE_HANDLE)));
        UByte notExistPathHandlerRecordValue = record.get(t.NOT_EXIST_PATH_HANDLER);
        if (notExistPathHandlerRecordValue == null) {
            // 默认为创建目录
            stepInstance.setNotExistPathHandler(1);
        } else {
            stepInstance.setNotExistPathHandler(notExistPathHandlerRecordValue.intValue());
        }
        return stepInstance;
    }

    @Override
    public ConfirmStepInstanceDTO getConfirmStepInstance(long stepInstanceId) {
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        Record record = ctx.select(t.STEP_INSTANCE_ID, t.CONFIRM_MESSAGE, t.CONFIRM_REASON, t.CONFIRM_USERS,
            t.CONFIRM_ROLES, t.NOTIFY_CHANNELS).from(t)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId)).fetchOne();
        return extractConfirmInfo(record);
    }

    private ConfirmStepInstanceDTO extractConfirmInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        ConfirmStepInstanceDTO stepInstanceDTO = new ConfirmStepInstanceDTO();
        stepInstanceDTO.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        stepInstanceDTO.setConfirmMessage(record.get(t.CONFIRM_MESSAGE));
        stepInstanceDTO.setConfirmReason(record.get(t.CONFIRM_REASON));
        stepInstanceDTO.setConfirmUsers(record.get(t.CONFIRM_USERS) == null ? null :
            Utils.getNotBlankSplitList(record.get(t.CONFIRM_USERS), ","));
        stepInstanceDTO.setConfirmRoles(record.get(t.CONFIRM_ROLES) == null ? null :
            Utils.getNotBlankSplitList(record.get(t.CONFIRM_ROLES), ","));
        stepInstanceDTO.setNotifyChannels(record.get(t.NOTIFY_CHANNELS) == null ? null :
            Utils.getNotBlankSplitList(record.get(t.NOTIFY_CHANNELS), ","));
        return stepInstanceDTO;
    }

    @Override
    public StepInstanceBaseDTO getStepInstanceBase(long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.select(t.ID, t.STEP_ID, t.TASK_INSTANCE_ID, t.APP_ID, t.NAME, t.TYPE, t.OPERATOR,
            t.STATUS, t.EXECUTE_COUNT, t.TARGET_SERVERS, t.ABNORMAL_AGENT_IP_LIST, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.TOTAL_IP_NUM, t.ABNORMAL_AGENT_NUM, t.RUN_IP_NUM, t.FAIL_IP_NUM, t.SUCCESS_IP_NUM,
            t.CREATE_TIME, t.IGNORE_ERROR, t.STEP_NUM, t.STEP_ORDER)
            .from(t)
            .where(t.ID.eq(stepInstanceId)).fetchOne();
        return extractBaseInfo(record);
    }

    private StepInstanceBaseDTO extractBaseInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceBaseDTO stepInstance = new StepInstanceBaseDTO();
        StepInstance t = StepInstance.STEP_INSTANCE;
        stepInstance.setId(record.get(t.ID));
        stepInstance.setAppId(record.get(t.APP_ID));
        stepInstance.setStepId(record.get(t.STEP_ID));
        stepInstance.setTaskInstanceId(record.get(t.TASK_INSTANCE_ID));
        stepInstance.setName(record.get(t.NAME));
        stepInstance.setExecuteType(JooqDataTypeUtil.getIntegerFromByte(record.get(t.TYPE)));
        stepInstance.setOperator(record.get(t.OPERATOR));
        stepInstance.setStatus(JooqDataTypeUtil.getIntegerFromByte(record.get(t.STATUS)));
        stepInstance.setExecuteCount(record.get(t.EXECUTE_COUNT));
        stepInstance.setStartTime(record.get(t.START_TIME));
        stepInstance.setEndTime(record.get(t.END_TIME));
        stepInstance.setTotalTime(record.get(t.TOTAL_TIME));
        stepInstance.setTotalIPNum(record.get(t.TOTAL_IP_NUM));
        if (StringUtils.isNotBlank(record.get(t.TARGET_SERVERS))) {
            ServersDTO targetServers = JsonUtils.fromJson(record.get(t.TARGET_SERVERS), ServersDTO.class);
            stepInstance.setTargetServers(targetServers);
            stepInstance.setIpList(targetServers.buildIpListStr());
        }
        stepInstance.setBadIpList(record.get(t.ABNORMAL_AGENT_IP_LIST));
        stepInstance.setBadIPNum(record.get(t.ABNORMAL_AGENT_NUM));
        stepInstance.setRunIPNum(record.get(t.RUN_IP_NUM));
        stepInstance.setFailIPNum(record.get(t.FAIL_IP_NUM));
        stepInstance.setSuccessIPNum(record.get(t.SUCCESS_IP_NUM));
        stepInstance.setCreateTime(record.get(t.CREATE_TIME));
        stepInstance.setIgnoreError(JooqDataTypeUtil.getIntegerFromByte(record.get(t.IGNORE_ERROR)) != null
            && JooqDataTypeUtil.getIntegerFromByte(record.get(t.IGNORE_ERROR)).equals(1));
        stepInstance.setStepNum(record.get(t.STEP_NUM));
        stepInstance.setStepOrder(record.get(t.STEP_ORDER));
        return stepInstance;
    }

    @Override
    public List<StepInstanceBaseDTO> listStepInstanceBaseByTaskInstanceId(long taskInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Result result = ctx.select(t.ID, t.STEP_ID, t.TASK_INSTANCE_ID, t.APP_ID, t.NAME, t.TYPE, t.OPERATOR,
            t.STATUS, t.EXECUTE_COUNT, t.TARGET_SERVERS, t.ABNORMAL_AGENT_IP_LIST, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.TOTAL_IP_NUM, t.ABNORMAL_AGENT_NUM, t.RUN_IP_NUM, t.FAIL_IP_NUM, t.SUCCESS_IP_NUM,
            t.CREATE_TIME, t.IGNORE_ERROR, t.STEP_NUM, t.STEP_ORDER)
            .from(t)
            .where(StepInstance.STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .orderBy(StepInstance.STEP_INSTANCE.ID.asc())
            .fetch();
        List<StepInstanceBaseDTO> stepInstanceList = new ArrayList<>();
        result.into(record -> {
            stepInstanceList.add(extractBaseInfo(record));
        });
        return stepInstanceList;
    }

    @Override
    public void resetStepStatus(long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).setNull(t.START_TIME).setNull(t.END_TIME).setNull(t.TOTAL_TIME).set(t.SUCCESS_IP_NUM, 0)
            .set(t.FAIL_IP_NUM, 0).set(t.RUN_IP_NUM, 0)
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void resetStepExecuteInfoForRetry(long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.START_TIME, System.currentTimeMillis())
            .set(t.EXECUTE_COUNT, t.EXECUTE_COUNT.plus(1))
            .set(t.STATUS, RunStatusEnum.RUNNING.getValue().byteValue())
            .setNull(t.END_TIME)
            .setNull(t.TOTAL_TIME)
            .set(t.SUCCESS_IP_NUM, 0)
            .set(t.FAIL_IP_NUM, 0)
            .set(t.RUN_IP_NUM, 0)
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void addStepExecuteCount(long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.EXECUTE_COUNT, t.EXECUTE_COUNT.plus(1))
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void updateStepStatus(long stepInstanceId, int status) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.STATUS, JooqDataTypeUtil.getByteFromInteger(status))
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void updateStepStartTime(long stepInstanceId, Long startTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.START_TIME, startTime)
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void updateStepStartTimeIfNull(long stepInstanceId, Long startTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.START_TIME, startTime)
            .where(t.ID.eq(stepInstanceId))
            .and(t.START_TIME.isNull())
            .execute();
    }

    @Override
    public void updateStepEndTime(long stepInstanceId, Long endTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.END_TIME, endTime)
            .where(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public void addTaskExecuteCount(long taskInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.EXECUTE_COUNT, t.EXECUTE_COUNT.plus(1))
            .where(t.TASK_INSTANCE_ID.eq(taskInstanceId)
                .and(t.STATUS.eq(JooqDataTypeUtil.getByteFromInteger(RunStatusEnum.BLANK.getValue())))).execute();
    }

    @Override
    public void updateStepTotalTime(long stepInstanceId, long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.TOTAL_TIME, totalTime)
            .where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void updateStepStatInfo(long stepInstanceId, int runIPNum, int successIPNum, int failIPNum) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.RUN_IP_NUM, runIPNum)
            .set(t.SUCCESS_IP_NUM, successIPNum)
            .set(t.FAIL_IP_NUM, failIPNum)
            .where(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public Long getFirstStepStartTime(long taskInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.select(min(t.START_TIME).as("time")).from(t)
            .where(t.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(t.START_TIME.isNotNull())
            .fetchOne();
        return record.get("time", Long.class);
    }

    @Override
    public Long getLastStepEndTime(long taskInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.select(max(t.END_TIME).as("time")).from(t)
            .where(t.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(t.END_TIME.isNotNull())
            .fetchOne();
        return record.get("time", Long.class);
    }

    @Override
    public long getAllStepTotalTime(long taskInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.select(sum(t.TOTAL_TIME).as("total_time"))
            .from(t)
            .where(t.TASK_INSTANCE_ID.eq(taskInstanceId))
            .fetchOne();
        return record.getValue("total_time", Long.class);
    }

    @Override
    public void updateStepExecutionInfo(long stepInstanceId, RunStatusEnum status, Long startTime, Long endTime,
                                        Long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        UpdateSetMoreStep<StepInstanceRecord> updateSetMoreStep = buildBasicUpdateSetMoreStep(status,
            startTime, endTime, totalTime);
        if (updateSetMoreStep == null) {
            return;
        }
        updateSetMoreStep.where(t.ID.eq(stepInstanceId)).execute();
    }

    private UpdateSetMoreStep<StepInstanceRecord> buildBasicUpdateSetMoreStep(RunStatusEnum status, Long startTime,
                                                                              Long endTime, Long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        UpdateSetMoreStep<StepInstanceRecord> updateSetMoreStep = null;
        if (status != null) {
            updateSetMoreStep = ctx.update(t).set(t.STATUS, JooqDataTypeUtil.getByteFromInteger(status.getValue()));
        }
        if (startTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.START_TIME, startTime);
            } else {
                updateSetMoreStep.set(t.START_TIME, startTime);
            }
        }
        if (endTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.END_TIME, endTime);
            } else {
                updateSetMoreStep.set(t.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.TOTAL_TIME, totalTime);
            } else {
                updateSetMoreStep.set(t.TOTAL_TIME, totalTime);
            }
        }
        return updateSetMoreStep;
    }

    @Override
    public void updateStepExecutionInfo(long stepInstanceId, RunStatusEnum status, Long startTime, Long endTime,
                                        Long totalTime, Integer runIPNum, Integer successIPNum, Integer failIPNum) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        UpdateSetMoreStep<StepInstanceRecord> updateSetMoreStep = buildBasicUpdateSetMoreStep(status,
            startTime, endTime, totalTime);
        if (runIPNum != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.RUN_IP_NUM, runIPNum);
            } else {
                updateSetMoreStep.set(t.RUN_IP_NUM, runIPNum);
            }
        }
        if (successIPNum != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.SUCCESS_IP_NUM, successIPNum);
            } else {
                updateSetMoreStep.set(t.SUCCESS_IP_NUM, successIPNum);
            }
        }
        if (failIPNum != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = ctx.update(t).set(t.FAIL_IP_NUM, failIPNum);
            } else {
                updateSetMoreStep.set(t.FAIL_IP_NUM, failIPNum);
            }
        }
        if (updateSetMoreStep == null) {
            return;
        }
        updateSetMoreStep.where(t.ID.eq(stepInstanceId)).execute();
    }

    @Override
    public void updateResolvedScriptParam(long stepInstanceId, String resolvedScriptParam) {
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        ctx.update(t).set(t.RESOLVED_SCRIPT_PARAM, resolvedScriptParam)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public void updateResolvedSourceFile(long stepInstanceId, List<FileSourceDTO> resolvedFileSources) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        ctx.update(t).set(t.FILE_SOURCE, JsonUtils.toJson(resolvedFileSources))
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public void updateResolvedTargetPath(long stepInstanceId, String resolvedTargetPath) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        ctx.update(t).set(t.RESOLVED_FILE_TARGET_PATH, resolvedTargetPath)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public void updateConfirmReason(long stepInstanceId, String confirmReason) {
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        ctx.update(t).set(t.CONFIRM_REASON, confirmReason)
            .where(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public void updateStepOperator(long stepInstanceId, String operator) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        ctx.update(t).set(t.OPERATOR, operator)
            .where(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    public StepInstanceBaseDTO getPreExecutableStepInstance(long taskInstanceId, long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = ctx.select(t.ID, t.STEP_ID, t.TASK_INSTANCE_ID, t.APP_ID, t.NAME, t.TYPE, t.OPERATOR,
            t.STATUS, t.EXECUTE_COUNT, t.TARGET_SERVERS, t.ABNORMAL_AGENT_IP_LIST, t.START_TIME, t.END_TIME,
            t.TOTAL_TIME, t.TOTAL_IP_NUM, t.ABNORMAL_AGENT_NUM, t.RUN_IP_NUM, t.FAIL_IP_NUM, t.SUCCESS_IP_NUM,
            t.CREATE_TIME, t.IGNORE_ERROR, t.STEP_NUM, t.STEP_ORDER)
            .from(t)
            .where(t.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(t.ID.lt(stepInstanceId))
            .and(t.TYPE.notIn(StepExecuteTypeEnum.MANUAL_CONFIRM.getValue().byteValue()))
            .orderBy(t.ID.desc())
            .limit(1)
            .fetchOne();
        return extractBaseInfo(record);
    }

    @Override
    public Long getStepInstanceId(long taskInstanceId) {
        Result<Record1<Long>> records = ctx.select(TABLE_STEP_INSTANCE.ID)
            .from(TABLE_STEP_INSTANCE)
            .where(TABLE_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        } else {
            return records.get(0).get(TABLE_STEP_INSTANCE.ID);
        }
    }

    @Override
    public Byte getScriptTypeByStepInstanceId(long stepInstanceId) {
        Result<Record1<Byte>> records = ctx.select(TABLE_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE)
            .from(TABLE_STEP_INSTANCE_SCRIPT)
            .where(TABLE_STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID.eq(stepInstanceId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        } else {
            return records.get(0).get(TABLE_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE);
        }
    }

    public Integer countStepInstanceByConditions(Collection<Condition> conditions) {
        return ctx.selectCount().from(TABLE_STEP_INSTANCE)
            .where(conditions).fetchOne().value1();
    }

    public Integer countStepInstanceScriptByConditions(Collection<Condition> conditions) {
        return ctx.selectCount().from(TABLE_STEP_INSTANCE_SCRIPT)
            .where(conditions).fetchOne().value1();
    }

    public Integer countStepInstanceFileByConditions(Collection<Condition> conditions) {
        return ctx.selectCount().from(TABLE_STEP_INSTANCE_FILE)
            .where(conditions).fetchOne().value1();
    }

    @Override
    public Integer count(Long appId, List<Long> stepIdList, StepExecuteTypeEnum stepExecuteType,
                         ScriptTypeEnum scriptType, RunStatusEnum runStatus, Long fromTime, Long toTime) {
        List<Condition> stepInstanceConditions = genStepInstanceConditions(appId, stepIdList, stepExecuteType,
            runStatus, fromTime, toTime);
        if (StepExecuteTypeEnum.EXECUTE_SCRIPT == stepExecuteType && scriptType != null) {
            int totalCount = 0;
            int offset = 0;
            int limit = 10000;
            List<Long> stepInstanceIds;
            do {
                // 1.查stepInstanceId
                stepInstanceIds = listStepInstanceIds(stepInstanceConditions, offset, limit);
                if (stepInstanceIds == null || stepInstanceIds.isEmpty()) {
                    break;
                }
                // 2.分批统计脚本步骤实例
                List<Condition> stepInstanceScriptConditions = new ArrayList<>();
                stepInstanceScriptConditions.add(TABLE_STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID.in(stepInstanceIds));
                stepInstanceScriptConditions.add(TABLE_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE.eq(scriptType.getValue().byteValue()));
                totalCount += countStepInstanceScriptByConditions(stepInstanceScriptConditions);
                offset += limit;
            } while (stepInstanceIds.size() == limit);
            return totalCount;
        } else {
            return countStepInstanceByConditions(stepInstanceConditions);
        }
    }

    private List<Long> listStepInstanceIds(Collection<Condition> conditions, int offset, int limit) {
        Result<Record1<Long>> records = ctx.select(TABLE_STEP_INSTANCE.ID)
            .from(TABLE_STEP_INSTANCE)
            .where(conditions)
            .limit(offset, limit)
            .fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(it -> it.get(TABLE_STEP_INSTANCE.ID));
        }
    }

    private List<Condition> genStepInstanceConditions(Long appId, List<Long> stepIdList,
                                                      StepExecuteTypeEnum stepExecuteType, RunStatusEnum runStatus,
                                                      Long fromTime, Long toTime) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE_STEP_INSTANCE.APP_ID.eq(appId));
        }
        if (stepIdList != null && !stepIdList.isEmpty()) {
            conditions.add(TABLE_STEP_INSTANCE.STEP_ID.in(stepIdList));
        }
        if (stepExecuteType != null) {
            conditions.add(TABLE_STEP_INSTANCE.TYPE.eq(stepExecuteType.getValue().byteValue()));
        }
        if (runStatus != null) {
            conditions.add(TABLE_STEP_INSTANCE.STATUS.eq(runStatus.getValue().byteValue()));
        }
        if (fromTime != null) {
            conditions.add(TABLE_STEP_INSTANCE.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TABLE_STEP_INSTANCE.CREATE_TIME.lessThan(toTime));
        }
        return conditions;
    }

    private List<Condition> genStepInstanceFileConditions(Collection<Long> stepInstanceIds,
                                                          DuplicateHandlerEnum fileDupliateHandle,
                                                          Boolean fileDupliateHandleNull,
                                                          NotExistPathHandlerEnum notExistPathHandler,
                                                          Boolean notExistPathHandlerNull) {
        List<Condition> conditions = new ArrayList<>();
        if (stepInstanceIds != null) {
            conditions.add(TABLE_STEP_INSTANCE_FILE.STEP_INSTANCE_ID.in(stepInstanceIds));
        }
        if (fileDupliateHandle != null) {
            conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.eq((byte) fileDupliateHandle.getId()));
        } else {
            if (fileDupliateHandleNull != null) {
                if (fileDupliateHandleNull) {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.isNull());
                } else {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.isNotNull());
                }
            } else {
                // fileDupliateHandle与fileDupliateHandleNull同时为null表示FILE_DUPLICATE_HANDLE字段不作任何筛选
            }
        }
        if (notExistPathHandler != null) {
            conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.eq(UByte.valueOf(notExistPathHandler.getValue())));
        } else {
            if (notExistPathHandlerNull != null) {
                if (notExistPathHandlerNull) {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.isNull());
                } else {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.isNotNull());
                }
            } else {
                // notExistPathHandler与notExistPathHandlerNull同时为null表示NOT_EXIST_PATH_HANDLER字段不作任何筛选
            }
        }
        return conditions;
    }

    private List<Condition> genConditions(Long appId, DuplicateHandlerEnum fileDupliateHandle,
                                          Boolean fileDupliateHandleNull, NotExistPathHandlerEnum notExistPathHandler
        , Boolean notExistPathHandlerNull, RunStatusEnum runStatus, Long fromTime, Long toTime) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE_STEP_INSTANCE.APP_ID.eq(appId));
        }
        if (fileDupliateHandle != null) {
            conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.eq((byte) fileDupliateHandle.getId()));
        } else {
            if (fileDupliateHandleNull != null) {
                if (fileDupliateHandleNull) {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.isNull());
                } else {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE.isNotNull());
                }
            } else {
                // fileDupliateHandle与fileDupliateHandleNull同时为null表示FILE_DUPLICATE_HANDLE字段不作任何筛选
            }
        }
        if (notExistPathHandler != null) {
            conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.eq(UByte.valueOf(notExistPathHandler.getValue())));
        } else {
            if (notExistPathHandlerNull != null) {
                if (notExistPathHandlerNull) {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.isNull());
                } else {
                    conditions.add(TABLE_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER.isNotNull());
                }
            } else {
                // notExistPathHandler与notExistPathHandlerNull同时为null表示NOT_EXIST_PATH_HANDLER字段不作任何筛选
            }
        }
        if (runStatus != null) {
            conditions.add(TABLE_STEP_INSTANCE.STATUS.eq(runStatus.getValue().byteValue()));
        }
        if (fromTime != null) {
            conditions.add(TABLE_STEP_INSTANCE.CREATE_TIME.greaterOrEqual(fromTime));
        }
        if (toTime != null) {
            conditions.add(TABLE_STEP_INSTANCE.CREATE_TIME.lessThan(toTime));
        }
        return conditions;
    }

    @Override
    public List<List<FileSourceDTO>> listFastPushFileSource(Long appId, DuplicateHandlerEnum fileDupliateHandle,
                                                            Boolean fileDupliateHandleNull,
                                                            NotExistPathHandlerEnum notExistPathHandler,
                                                            Boolean notExistPathHandlerNull, RunStatusEnum runStatus,
                                                            Long fromTime, Long toTime) {
        List<Condition> conditions = genConditions(appId, fileDupliateHandle, fileDupliateHandleNull,
            notExistPathHandler, notExistPathHandlerNull, runStatus, fromTime, toTime);
        val query =
            ctx.select(TABLE_STEP_INSTANCE_FILE.FILE_SOURCE).from(TABLE_STEP_INSTANCE).join(TABLE_STEP_INSTANCE_FILE)
            .on(TABLE_STEP_INSTANCE.ID.eq(TABLE_STEP_INSTANCE_FILE.STEP_INSTANCE_ID))
            .where(conditions);
        String sql = query.getSQL(ParamType.INLINED);
        try {
            Result<Record1<String>> records = query.fetch();
            List<List<FileSourceDTO>> resultList = new ArrayList<>();
            records.forEach(record -> {
                resultList.add(convertStringToFileSourceDTO((String) record.get(0)));
            });
            return resultList;
        } catch (Exception e) {
            log.error("Fail to query:{}", sql, e);
            throw new RuntimeException("Fail to listFastPushFileSource", e);
        }
    }

    @Override
    public Integer countFastPushFile(Long appId, DuplicateHandlerEnum fileDupliateHandle,
                                     Boolean fileDupliateHandleNull, NotExistPathHandlerEnum notExistPathHandler,
                                     Boolean notExistPathHandlerNull, RunStatusEnum runStatus, Long fromTime,
                                     Long toTime) {
        // 1.查stepInstanceId
        int totalCount = 0;
        int offset = 0;
        int limit = 10000;
        List<Long> stepInstanceIds;
        do {
            stepInstanceIds = listStepInstanceIds(genStepInstanceConditions(appId, null, null, null, fromTime,
                toTime), offset, limit);
            if (stepInstanceIds == null || stepInstanceIds.isEmpty()) {
                break;
            }
            // 2.分批统计文件步骤实例
            List<Condition> stepInstanceFileConditions = genStepInstanceFileConditions(stepInstanceIds,
                fileDupliateHandle, fileDupliateHandleNull, notExistPathHandler, notExistPathHandlerNull);
            totalCount += countStepInstanceFileByConditions(stepInstanceFileConditions);
            offset += limit;
        } while (stepInstanceIds.size() == limit);
        return totalCount;
    }

    private List<FileSourceDTO> convertStringToFileSourceDTO(String str) {
        return JsonUtils.fromJson(str, new TypeReference<ArrayList<FileSourceDTO>>() {
        });
    }
}
