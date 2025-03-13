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
import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.crypto.scenario.DbPasswordCryptoService;
import com.tencent.bk.job.common.crypto.scenario.SensitiveParamCryptoService;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.jooq.JooqDataTypeUtil;
import com.tencent.bk.job.common.util.Utils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.ScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.tables.StepInstance;
import com.tencent.bk.job.execute.model.tables.StepInstanceConfirm;
import com.tencent.bk.job.execute.model.tables.StepInstanceFile;
import com.tencent.bk.job.execute.model.tables.StepInstanceScript;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRecord;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.jooq.types.UByte;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class StepInstanceDAOImpl extends BaseDAO implements StepInstanceDAO {

    private static final StepInstance T_STEP_INSTANCE = StepInstance.STEP_INSTANCE;
    private static final StepInstanceScript T_STEP_INSTANCE_SCRIPT = StepInstanceScript.STEP_INSTANCE_SCRIPT;
    private static final StepInstanceFile T_STEP_INSTANCE_FILE = StepInstanceFile.STEP_INSTANCE_FILE;
    private static final StepInstanceConfirm T_STEP_INSTANCE_CONFIRM = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;

    private static final TableField<?, ?>[] T_STEP_INSTANCE_ALL_FIELDS = {
        T_STEP_INSTANCE.ID,
        T_STEP_INSTANCE.STEP_ID,
        T_STEP_INSTANCE.TASK_INSTANCE_ID,
        T_STEP_INSTANCE.APP_ID,
        T_STEP_INSTANCE.NAME,
        T_STEP_INSTANCE.TYPE,
        T_STEP_INSTANCE.OPERATOR,
        T_STEP_INSTANCE.STATUS,
        T_STEP_INSTANCE.EXECUTE_COUNT,
        T_STEP_INSTANCE.TARGET_SERVERS,
        T_STEP_INSTANCE.START_TIME,
        T_STEP_INSTANCE.END_TIME,
        T_STEP_INSTANCE.TOTAL_TIME,
        T_STEP_INSTANCE.CREATE_TIME,
        T_STEP_INSTANCE.IGNORE_ERROR,
        T_STEP_INSTANCE.STEP_NUM,
        T_STEP_INSTANCE.STEP_ORDER,
        T_STEP_INSTANCE.BATCH,
        T_STEP_INSTANCE.ROLLING_CONFIG_ID
    };
    private static final TableField<?, ?>[] T_STEP_INSTANCE_SCRIPT_ALL_FIELDS = {
        T_STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID,
        T_STEP_INSTANCE_SCRIPT.TASK_INSTANCE_ID,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_CONTENT,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_PARAM,
        T_STEP_INSTANCE_SCRIPT.RESOLVED_SCRIPT_PARAM,
        T_STEP_INSTANCE_SCRIPT.EXECUTION_TIMEOUT,
        T_STEP_INSTANCE_SCRIPT.SYSTEM_ACCOUNT_ID,
        T_STEP_INSTANCE_SCRIPT.SYSTEM_ACCOUNT,
        T_STEP_INSTANCE_SCRIPT.DB_ACCOUNT_ID,
        T_STEP_INSTANCE_SCRIPT.DB_ACCOUNT,
        T_STEP_INSTANCE_SCRIPT.DB_TYPE,
        T_STEP_INSTANCE_SCRIPT.DB_PASSWORD,
        T_STEP_INSTANCE_SCRIPT.DB_PORT,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_SOURCE,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_ID,
        T_STEP_INSTANCE_SCRIPT.SCRIPT_VERSION_ID,
        T_STEP_INSTANCE_SCRIPT.IS_SECURE_PARAM,
        T_STEP_INSTANCE_SCRIPT.WINDOWS_INTERPRETER
    };
    private static final TableField<?, ?>[] T_STEP_INSTANCE_FILE_ALL_FIELDS = {
        T_STEP_INSTANCE_FILE.TASK_INSTANCE_ID,
        T_STEP_INSTANCE_FILE.STEP_INSTANCE_ID,
        T_STEP_INSTANCE_FILE.FILE_SOURCE,
        T_STEP_INSTANCE_FILE.FILE_TARGET_PATH,
        T_STEP_INSTANCE_FILE.FILE_TARGET_NAME,
        T_STEP_INSTANCE_FILE.RESOLVED_FILE_TARGET_PATH,
        T_STEP_INSTANCE_FILE.FILE_UPLOAD_SPEED_LIMIT,
        T_STEP_INSTANCE_FILE.FILE_DOWNLOAD_SPEED_LIMIT,
        T_STEP_INSTANCE_FILE.FILE_DUPLICATE_HANDLE,
        T_STEP_INSTANCE_FILE.NOT_EXIST_PATH_HANDLER,
        T_STEP_INSTANCE_FILE.EXECUTION_TIMEOUT,
        T_STEP_INSTANCE_FILE.SYSTEM_ACCOUNT_ID,
        T_STEP_INSTANCE_FILE.SYSTEM_ACCOUNT
    };
    private static final TableField<?, ?>[] T_STEP_INSTANCE_CONFIRM_ALL_FIELDS = {
        T_STEP_INSTANCE_CONFIRM.TASK_INSTANCE_ID,
        T_STEP_INSTANCE_CONFIRM.STEP_INSTANCE_ID,
        T_STEP_INSTANCE_CONFIRM.CONFIRM_MESSAGE,
        T_STEP_INSTANCE_CONFIRM.CONFIRM_REASON,
        T_STEP_INSTANCE_CONFIRM.CONFIRM_USERS,
        T_STEP_INSTANCE_CONFIRM.CONFIRM_ROLES,
        T_STEP_INSTANCE_CONFIRM.NOTIFY_CHANNELS
    };

    private final SensitiveParamCryptoService sensitiveParamCryptoService;
    private final DbPasswordCryptoService dbPasswordCryptoService;

    @Autowired
    public StepInstanceDAOImpl(DSLContextProviderFactory dslContextProviderFactory,
                               SensitiveParamCryptoService sensitiveParamCryptoService,
                               DbPasswordCryptoService dbPasswordCryptoService) {
        super(dslContextProviderFactory, T_STEP_INSTANCE.getName());
        this.sensitiveParamCryptoService = sensitiveParamCryptoService;
        this.dbPasswordCryptoService = dbPasswordCryptoService;
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public Long addStepInstanceBase(StepInstanceBaseDTO stepInstance) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        Record record = dsl().insertInto(
                t,
                t.ID,
                t.STEP_ID,
                t.TASK_INSTANCE_ID,
                t.APP_ID,
                t.NAME,
                t.TYPE,
                t.OPERATOR,
                t.STATUS,
                t.EXECUTE_COUNT,
                t.START_TIME,
                t.END_TIME,
                t.TOTAL_TIME,
                t.TARGET_SERVERS,
                t.CREATE_TIME,
                t.IGNORE_ERROR,
                t.STEP_NUM,
                t.STEP_ORDER,
                t.BATCH
            ).values(
                stepInstance.getId(),
                stepInstance.getStepId(),
                stepInstance.getTaskInstanceId(),
                stepInstance.getAppId(),
                stepInstance.getName(),
                stepInstance.getExecuteType().getValue().byteValue(),
                stepInstance.getOperator(),
                stepInstance.getStatus().getValue().byteValue(),
                stepInstance.getExecuteCount(),
                stepInstance.getStartTime(),
                stepInstance.getEndTime(),
                stepInstance.getTotalTime(),
                stepInstance.getTargetExecuteObjects() == null ? null :
                    JsonUtils.toJson(stepInstance.getTargetExecuteObjects()),
                stepInstance.getCreateTime(),
                stepInstance.isIgnoreError() ? Byte.valueOf("1") : Byte.valueOf("0"),
                stepInstance.getStepNum(),
                stepInstance.getStepOrder(),
                (short) stepInstance.getBatch()
            ).returning(t.ID)
            .fetchOne();
        return stepInstance.getId() != null ? stepInstance.getId() : record.getValue(t.ID);

    }

    @Override
    @MySQLOperation(table = "step_instance_script", op = DbOperationEnum.WRITE)
    public void addScriptStepInstance(StepInstanceDTO stepInstance) {
        Integer scriptSource = stepInstance.getScriptSource();
        byte scriptSourceByteValue = 1;
        if (scriptSource != null) {
            scriptSourceByteValue = scriptSource.byteValue();
        }
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        dsl().insertInto(t,
            t.STEP_INSTANCE_ID,
            t.TASK_INSTANCE_ID,
            t.SCRIPT_CONTENT,
            t.SCRIPT_TYPE,
            t.SCRIPT_PARAM,
            t.RESOLVED_SCRIPT_PARAM,
            t.EXECUTION_TIMEOUT,
            t.SYSTEM_ACCOUNT_ID,
            t.SYSTEM_ACCOUNT,
            t.DB_ACCOUNT_ID,
            t.DB_TYPE,
            t.DB_ACCOUNT,
            t.DB_PASSWORD,
            t.DB_PORT,
            t.SCRIPT_SOURCE,
            t.SCRIPT_ID,
            t.SCRIPT_VERSION_ID,
            t.IS_SECURE_PARAM,
            t.WINDOWS_INTERPRETER
        ).values(
            stepInstance.getId(),
            stepInstance.getTaskInstanceId(),
            stepInstance.getScriptContent(),
            stepInstance.getScriptType().getValue().byteValue(),
            sensitiveParamCryptoService.encryptParamIfNeeded(
                stepInstance.isSecureParam(), stepInstance.getScriptParam()),
            sensitiveParamCryptoService.encryptParamIfNeeded(
                stepInstance.isSecureParam(), stepInstance.getResolvedScriptParam()),
            stepInstance.getTimeout(),
            stepInstance.getAccountId(),
            stepInstance.getAccount(),
            stepInstance.getDbAccountId(),
            JooqDataTypeUtil.toByte(stepInstance.getDbType()),
            stepInstance.getDbAccount(),
            dbPasswordCryptoService.encryptDbPasswordIfNeeded(AccountCategoryEnum.DB, stepInstance.getDbPass()),
            stepInstance.getDbPort(),
            scriptSourceByteValue,
            stepInstance.getScriptId(),
            stepInstance.getScriptVersionId(),
            stepInstance.isSecureParam() ? JooqDataTypeUtil.toByte(1) :
                JooqDataTypeUtil.toByte(0),
            stepInstance.getWindowsInterpreter()
        ).execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_file", op = DbOperationEnum.WRITE)
    public void addFileStepInstance(StepInstanceDTO stepInstance) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        dsl().insertInto(t,
            t.STEP_INSTANCE_ID,
            t.TASK_INSTANCE_ID,
            t.FILE_SOURCE,
            t.FILE_TARGET_PATH,
            t.FILE_TARGET_NAME,
            t.FILE_UPLOAD_SPEED_LIMIT,
            t.FILE_DOWNLOAD_SPEED_LIMIT,
            t.FILE_DUPLICATE_HANDLE,
            t.NOT_EXIST_PATH_HANDLER,
            t.EXECUTION_TIMEOUT,
            t.SYSTEM_ACCOUNT_ID,
            t.SYSTEM_ACCOUNT
        ).values(
            stepInstance.getId(),
            stepInstance.getTaskInstanceId(),
            JsonUtils.toJson(stepInstance.getFileSourceList()),
            stepInstance.getFileTargetPath(),
            stepInstance.getFileTargetName(),
            stepInstance.getFileUploadSpeedLimit(),
            stepInstance.getFileDownloadSpeedLimit(),
            JooqDataTypeUtil.toByte(stepInstance.getFileDuplicateHandle()),
            JooqDataTypeUtil.toUByte(stepInstance.getNotExistPathHandler()),
            stepInstance.getTimeout(),
            stepInstance.getAccountId(),
            stepInstance.getAccount()
        ).execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_confirm", op = DbOperationEnum.WRITE)
    public void addConfirmStepInstance(StepInstanceDTO stepInstance) {
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        dsl().insertInto(t,
                t.STEP_INSTANCE_ID,
                t.TASK_INSTANCE_ID,
                t.CONFIRM_MESSAGE,
                t.CONFIRM_USERS,
                t.CONFIRM_ROLES,
                t.NOTIFY_CHANNELS)
            .values(
                stepInstance.getId(),
                stepInstance.getTaskInstanceId(),
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
    @MySQLOperation(table = "step_instance_script", op = DbOperationEnum.READ)
    public ScriptStepInstanceDTO getScriptStepInstance(Long taskInstanceId, long stepInstanceId) {
        Record record = dsl().select(T_STEP_INSTANCE_SCRIPT_ALL_FIELDS)
            .from(T_STEP_INSTANCE_SCRIPT)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_SCRIPT, taskInstanceId))
            .and(T_STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID.eq(stepInstanceId))
            .fetchOne();
        return extractScriptInfo(record);
    }

    private Condition buildTaskInstanceIdQueryCondition(Table<?> table,
                                                        Long taskInstanceId) {
        if (table instanceof StepInstance) {
            return TaskInstanceIdDynamicCondition.build(
                taskInstanceId,
                T_STEP_INSTANCE.TASK_INSTANCE_ID::eq
            );
        } else if (table instanceof StepInstanceScript) {
            return TaskInstanceIdDynamicCondition.build(
                taskInstanceId,
                T_STEP_INSTANCE_SCRIPT.TASK_INSTANCE_ID::eq
            );
        } else if (table instanceof StepInstanceFile) {
            return TaskInstanceIdDynamicCondition.build(
                taskInstanceId,
                T_STEP_INSTANCE_FILE.TASK_INSTANCE_ID::eq
            );
        } else if (table instanceof StepInstanceConfirm) {
            return TaskInstanceIdDynamicCondition.build(
                taskInstanceId,
                T_STEP_INSTANCE_CONFIRM.TASK_INSTANCE_ID::eq
            );
        } else {
            throw new IllegalArgumentException("Invalid table for building task_instance_id query condition");
        }
    }


    private ScriptStepInstanceDTO extractScriptInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        ScriptStepInstanceDTO stepInstance = new ScriptStepInstanceDTO();
        stepInstance.setTaskInstanceId(record.get(t.TASK_INSTANCE_ID));
        stepInstance.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        stepInstance.setScriptContent(record.get(t.SCRIPT_CONTENT));
        stepInstance.setScriptType(ScriptTypeEnum.valOf(record.get(t.SCRIPT_TYPE).intValue()));
        stepInstance.setSecureParam(record.get(t.IS_SECURE_PARAM).intValue() == 1);
        String encryptedScriptParam = record.get(t.SCRIPT_PARAM);

        // 敏感参数解密
        String scriptParam = sensitiveParamCryptoService.decryptParamIfNeeded(
            stepInstance.isSecureParam(),
            encryptedScriptParam
        );
        stepInstance.setScriptParam(scriptParam);
        String encryptedResolvedScriptParam = record.get(t.RESOLVED_SCRIPT_PARAM);
        String resolvedScriptParam = sensitiveParamCryptoService.decryptParamIfNeeded(
            stepInstance.isSecureParam(),
            encryptedResolvedScriptParam
        );
        stepInstance.setResolvedScriptParam(resolvedScriptParam);

        stepInstance.setTimeout(record.get(t.EXECUTION_TIMEOUT));
        stepInstance.setAccountId(record.get(t.SYSTEM_ACCOUNT_ID));
        stepInstance.setAccount(record.get(t.SYSTEM_ACCOUNT));
        stepInstance.setDbAccountId(record.get(t.DB_ACCOUNT_ID));
        stepInstance.setDbType(JooqDataTypeUtil.toInteger(record.get(t.DB_TYPE)));
        stepInstance.setDbAccount(record.get(t.DB_ACCOUNT));

        // 账号密码解密
        String encryptedDbPassword = record.get(t.DB_PASSWORD);
        String dbPassword = dbPasswordCryptoService.decryptDbPasswordIfNeeded(
            AccountCategoryEnum.DB,
            encryptedDbPassword
        );

        stepInstance.setDbPass(dbPassword);
        stepInstance.setDbPort(record.get(t.DB_PORT));
        Byte scriptSource = record.get(t.SCRIPT_SOURCE);
        if (scriptSource == null) {
            stepInstance.setScriptSource(1);
        } else {
            stepInstance.setScriptSource(scriptSource.intValue());
        }
        stepInstance.setScriptId(record.get(t.SCRIPT_ID));
        stepInstance.setScriptVersionId(record.get(t.SCRIPT_VERSION_ID));
        stepInstance.setWindowsInterpreter(record.get(t.WINDOWS_INTERPRETER));
        return stepInstance;
    }

    @Override
    @MySQLOperation(table = "step_instance_file", op = DbOperationEnum.READ)
    public FileStepInstanceDTO getFileStepInstance(Long taskInstanceId, long stepInstanceId) {
        Record record = dsl().select(T_STEP_INSTANCE_FILE_ALL_FIELDS)
            .from(T_STEP_INSTANCE_FILE)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_FILE, taskInstanceId))
            .and(T_STEP_INSTANCE_FILE.STEP_INSTANCE_ID.eq(stepInstanceId))
            .fetchOne();
        return extractFileInfo(record);
    }

    private FileStepInstanceDTO extractFileInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        FileStepInstanceDTO stepInstance = new FileStepInstanceDTO();
        stepInstance.setTaskInstanceId(record.get(t.TASK_INSTANCE_ID));
        stepInstance.setStepInstanceId(record.get(t.STEP_INSTANCE_ID));
        stepInstance.setTimeout(record.get(t.EXECUTION_TIMEOUT));
        List<FileSourceDTO> fileSourceList = JsonUtils.fromJson(record.get(t.FILE_SOURCE),
            new TypeReference<List<FileSourceDTO>>() {
            });
        stepInstance.setFileSourceList(fileSourceList);
        stepInstance.setFileTargetPath(record.get(t.FILE_TARGET_PATH));
        stepInstance.setFileTargetName(record.get(t.FILE_TARGET_NAME));
        stepInstance.setResolvedFileTargetPath(record.get(t.RESOLVED_FILE_TARGET_PATH));
        stepInstance.setAccountId(record.get(t.SYSTEM_ACCOUNT_ID));
        stepInstance.setAccount(record.get(t.SYSTEM_ACCOUNT));
        stepInstance.setFileUploadSpeedLimit(record.get(t.FILE_UPLOAD_SPEED_LIMIT));
        stepInstance.setFileDownloadSpeedLimit(record.get(t.FILE_DOWNLOAD_SPEED_LIMIT));
        stepInstance.setFileDuplicateHandle(JooqDataTypeUtil.toInteger(record.get(t.FILE_DUPLICATE_HANDLE)));
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
    @MySQLOperation(table = "step_instance_confirm", op = DbOperationEnum.READ)
    public ConfirmStepInstanceDTO getConfirmStepInstance(Long taskInstanceId, long stepInstanceId) {
        Record record = dsl().select(T_STEP_INSTANCE_CONFIRM_ALL_FIELDS)
            .from(T_STEP_INSTANCE_CONFIRM)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_CONFIRM, taskInstanceId))
            .and(T_STEP_INSTANCE_CONFIRM.STEP_INSTANCE_ID.eq(stepInstanceId))
            .fetchOne();
        return extractConfirmInfo(record);
    }

    private ConfirmStepInstanceDTO extractConfirmInfo(Record record) {
        if (record == null) {
            return null;
        }
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        ConfirmStepInstanceDTO stepInstanceDTO = new ConfirmStepInstanceDTO();
        stepInstanceDTO.setTaskInstanceId(record.get(t.TASK_INSTANCE_ID));
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
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public StepInstanceBaseDTO getStepInstanceBase(Long taskInstanceId, long stepInstanceId) {
        Record record = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(T_STEP_INSTANCE.ID.eq(stepInstanceId))
            .fetchOne();
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
        stepInstance.setExecuteType(StepExecuteTypeEnum.valOf(JooqDataTypeUtil.toInteger(record.get(t.TYPE))));
        stepInstance.setOperator(record.get(t.OPERATOR));
        stepInstance.setStatus(RunStatusEnum.valueOf(record.get(t.STATUS)));
        stepInstance.setExecuteCount(record.get(t.EXECUTE_COUNT));
        stepInstance.setStartTime(record.get(t.START_TIME));
        stepInstance.setEndTime(record.get(t.END_TIME));
        stepInstance.setTotalTime(record.get(t.TOTAL_TIME));
        if (StringUtils.isNotBlank(record.get(t.TARGET_SERVERS))) {
            ExecuteTargetDTO targetServers = JsonUtils.fromJson(record.get(t.TARGET_SERVERS), ExecuteTargetDTO.class);
            stepInstance.setTargetExecuteObjects(targetServers);
        }
        stepInstance.setCreateTime(record.get(t.CREATE_TIME));
        stepInstance.setIgnoreError(JooqDataTypeUtil.toInteger(record.get(t.IGNORE_ERROR)) != null
            && JooqDataTypeUtil.toInteger(record.get(t.IGNORE_ERROR)).equals(1));
        stepInstance.setStepNum(record.get(t.STEP_NUM));
        stepInstance.setStepOrder(record.get(t.STEP_ORDER));
        stepInstance.setBatch(record.get(t.BATCH));
        stepInstance.setRollingConfigId(record.get(t.ROLLING_CONFIG_ID));
        return stepInstance;
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public StepInstanceBaseDTO getStepInstanceBase(long stepInstanceId) {
        Record record = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.ID.eq(stepInstanceId))
            .fetchOne();
        return extractBaseInfo(record);
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public StepInstanceBaseDTO getFirstStepInstanceBase(Long taskInstanceId) {
        Record record = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .orderBy(T_STEP_INSTANCE.STEP_ORDER.asc())
            .limit(1)
            .fetchOne();
        return extractBaseInfo(record);
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public StepInstanceBaseDTO getNextStepInstance(Long taskInstanceId, int currentStepOrder) {
        Record record = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(T_STEP_INSTANCE.STEP_ORDER.gt(currentStepOrder))
            .orderBy(T_STEP_INSTANCE.STEP_ORDER.asc())
            .limit(1)
            .fetchOne();
        return extractBaseInfo(record);
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public List<StepInstanceBaseDTO> listStepInstanceBaseByTaskInstanceId(Long taskInstanceId) {
        Result<Record> result = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .orderBy(T_STEP_INSTANCE.STEP_ORDER.asc())
            .fetch();
        List<StepInstanceBaseDTO> stepInstanceList = new ArrayList<>();
        result.into(record -> stepInstanceList.add(extractBaseInfo(record)));
        return stepInstanceList;
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void resetStepStatus(Long taskInstanceId, long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .setNull(t.START_TIME)
            .setNull(t.END_TIME)
            .setNull(t.TOTAL_TIME)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void resetStepExecuteInfoForRetry(Long taskInstanceId, long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.STATUS, RunStatusEnum.RUNNING.getValue().byteValue())
            .setNull(t.END_TIME)
            .setNull(t.TOTAL_TIME)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void addStepExecuteCount(Long taskInstanceId, long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.EXECUTE_COUNT, t.EXECUTE_COUNT.plus(1))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepStatus(Long taskInstanceId, long stepInstanceId, int status) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.STATUS, JooqDataTypeUtil.toByte(status))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepStartTime(Long taskInstanceId, long stepInstanceId, Long startTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.START_TIME, startTime)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepStartTimeIfNull(Long taskInstanceId, long stepInstanceId, Long startTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.START_TIME, startTime)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .and(t.START_TIME.isNull())
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepEndTime(Long taskInstanceId, long stepInstanceId, Long endTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.END_TIME, endTime)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void addStepInstanceExecuteCount(Long taskInstanceId, long stepInstanceId) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.EXECUTE_COUNT, t.EXECUTE_COUNT.plus(1))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepTotalTime(Long taskInstanceId, long stepInstanceId, long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t)
            .set(t.TOTAL_TIME, totalTime)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepExecutionInfo(Long taskInstanceId,
                                        long stepInstanceId,
                                        RunStatusEnum status,
                                        Long startTime,
                                        Long endTime,
                                        Long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        UpdateSetMoreStep<StepInstanceRecord> updateSetMoreStep = buildBasicUpdateSetMoreStep(status,
            startTime, endTime, totalTime);
        if (updateSetMoreStep == null) {
            return;
        }
        updateSetMoreStep
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    private UpdateSetMoreStep<StepInstanceRecord> buildBasicUpdateSetMoreStep(RunStatusEnum status,
                                                                              Long startTime,
                                                                              Long endTime,
                                                                              Long totalTime) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        UpdateSetMoreStep<StepInstanceRecord> updateSetMoreStep = null;
        if (status != null) {
            updateSetMoreStep = dsl().update(t).set(t.STATUS,
                JooqDataTypeUtil.toByte(status.getValue()));
        }
        if (startTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(t).set(t.START_TIME, startTime);
            } else {
                updateSetMoreStep.set(t.START_TIME, startTime);
            }
        }
        if (endTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(t).set(t.END_TIME, endTime);
            } else {
                updateSetMoreStep.set(t.END_TIME, endTime);
            }
        }
        if (totalTime != null) {
            if (updateSetMoreStep == null) {
                updateSetMoreStep = dsl().update(t).set(t.TOTAL_TIME, totalTime);
            } else {
                updateSetMoreStep.set(t.TOTAL_TIME, totalTime);
            }
        }
        return updateSetMoreStep;
    }

    @Override
    @MySQLOperation(table = "step_instance_script", op = DbOperationEnum.WRITE)
    public void updateResolvedScriptParam(Long taskInstanceId,
                                          long stepInstanceId,
                                          boolean isSecureParam,
                                          String resolvedScriptParam) {
        StepInstanceScript t = StepInstanceScript.STEP_INSTANCE_SCRIPT;
        dsl().update(t)
            .set(t.RESOLVED_SCRIPT_PARAM, sensitiveParamCryptoService.encryptParamIfNeeded(
                isSecureParam, resolvedScriptParam
            ))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_SCRIPT, taskInstanceId))
            .and(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_file", op = DbOperationEnum.WRITE)
    public void updateResolvedSourceFile(Long taskInstanceId,
                                         long stepInstanceId,
                                         List<FileSourceDTO> resolvedFileSources) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        dsl().update(t)
            .set(t.FILE_SOURCE, JsonUtils.toJson(resolvedFileSources))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_FILE, taskInstanceId))
            .and(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_file", op = DbOperationEnum.WRITE)
    public void updateResolvedTargetPath(Long taskInstanceId, long stepInstanceId, String resolvedTargetPath) {
        StepInstanceFile t = StepInstanceFile.STEP_INSTANCE_FILE;
        dsl().update(t)
            .set(t.RESOLVED_FILE_TARGET_PATH, resolvedTargetPath)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_FILE, taskInstanceId))
            .and(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance_confirm", op = DbOperationEnum.WRITE)
    public void updateConfirmReason(Long taskInstanceId, long stepInstanceId, String confirmReason) {
        StepInstanceConfirm t = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;
        dsl().update(t).set(t.CONFIRM_REASON, confirmReason)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_CONFIRM, taskInstanceId))
            .and(t.STEP_INSTANCE_ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepOperator(Long taskInstanceId, long stepInstanceId, String operator) {
        StepInstance t = StepInstance.STEP_INSTANCE;
        dsl().update(t).set(t.OPERATOR, operator)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(t.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public StepInstanceBaseDTO getPreExecutableStepInstance(Long taskInstanceId, int currentStepOrder) {
        Record record = dsl()
            .select(T_STEP_INSTANCE_ALL_FIELDS)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .and(T_STEP_INSTANCE.STEP_ORDER.lt(currentStepOrder))
            .and(T_STEP_INSTANCE.TYPE.notIn(StepExecuteTypeEnum.MANUAL_CONFIRM.getValue().byteValue()))
            .orderBy(T_STEP_INSTANCE.STEP_ORDER.desc())
            .limit(1)
            .fetchOne();
        return extractBaseInfo(record);
    }

    @Override
    public Map<Long, Integer> listStepInstanceIdAndStepOrderMapping(Long taskInstanceId) {
        Result<Record2<Long, Integer>> result = dsl()
            .select(T_STEP_INSTANCE.ID, T_STEP_INSTANCE.STEP_ORDER)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .fetch();
        if (result.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> map = new HashMap<>();
        result.forEach(record -> map.put(record.get(T_STEP_INSTANCE.ID), record.get(T_STEP_INSTANCE.STEP_ORDER)));
        return map;
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public Long getStepInstanceId(Long taskInstanceId) {
        Result<Record1<Long>> records = dsl().select(T_STEP_INSTANCE.ID)
            .from(T_STEP_INSTANCE)
            .where(T_STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        } else {
            return records.get(0).get(T_STEP_INSTANCE.ID);
        }
    }

    @Override
    @MySQLOperation(table = "step_instance_script", op = DbOperationEnum.READ)
    public Byte getScriptTypeByStepInstanceId(Long taskInstanceId, long stepInstanceId) {
        Result<Record1<Byte>> records = dsl().select(T_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE)
            .from(T_STEP_INSTANCE_SCRIPT)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE_SCRIPT, taskInstanceId))
            .and(T_STEP_INSTANCE_SCRIPT.STEP_INSTANCE_ID.eq(stepInstanceId))
            .limit(1)
            .fetch();
        if (records.isEmpty()) {
            return null;
        } else {
            return records.get(0).get(T_STEP_INSTANCE_SCRIPT.SCRIPT_TYPE);
        }
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepCurrentBatch(Long taskInstanceId, long stepInstanceId, int batch) {
        dsl().update(T_STEP_INSTANCE)
            .set(T_STEP_INSTANCE.BATCH, JooqDataTypeUtil.toShort(batch))
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(T_STEP_INSTANCE.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepCurrentExecuteCount(Long taskInstanceId, long stepInstanceId, int executeCount) {
        dsl().update(T_STEP_INSTANCE)
            .set(T_STEP_INSTANCE.EXECUTE_COUNT, executeCount)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(T_STEP_INSTANCE.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.WRITE)
    public void updateStepRollingConfigId(Long taskInstanceId, long stepInstanceId, long rollingConfigId) {
        dsl().update(T_STEP_INSTANCE)
            .set(T_STEP_INSTANCE.ROLLING_CONFIG_ID, rollingConfigId)
            .where(buildTaskInstanceIdQueryCondition(T_STEP_INSTANCE, taskInstanceId))
            .and(T_STEP_INSTANCE.ID.eq(stepInstanceId))
            .execute();
    }

    @Override
    @MySQLOperation(table = "step_instance", op = DbOperationEnum.READ)
    public List<Long> getTaskStepInstanceIdList(Long taskInstanceId) {
        Result result =
            dsl().select(StepInstance.STEP_INSTANCE.ID).from(StepInstance.STEP_INSTANCE)
                .where(StepInstance.STEP_INSTANCE.TASK_INSTANCE_ID.eq(taskInstanceId))
                .orderBy(StepInstance.STEP_INSTANCE.ID.asc())
                .fetch();
        List<Long> stepInstanceIdList = new ArrayList<>();
        result.into(record -> {
            Long stepInstanceId = record.getValue(StepInstance.STEP_INSTANCE.ID);
            if (stepInstanceId != null) {
                stepInstanceIdList.add(stepInstanceId);
            }
        });
        return stepInstanceIdList;
    }
}
