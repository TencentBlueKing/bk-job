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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationProgress;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationProgressDAO;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationProgressStatus;
import com.tencent.bk.job.manage.model.tables.CryptoPasswordRotationProgress;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.TableField;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * job-manage 数据库的旧密码轮换进度 DAO
 * <p>
 * 显式指定 Bean 名以避免轻量化部署模式（job-assemble）下
 * 多 service 同名 DAOImpl 注册冲突。
 */
@Slf4j
@Repository("jobManagePasswordRotationProgressDAOImpl")
public class PasswordRotationProgressDAOImpl implements PasswordRotationProgressDAO {

    private static final CryptoPasswordRotationProgress TB =
        CryptoPasswordRotationProgress.CRYPTO_PASSWORD_ROTATION_PROGRESS;

    private static final TableField<?, ?>[] ALL_FIELDS = {
        TB.ID,
        TB.TARGET_PASSWORD_FINGERPRINT,
        TB.TABLE_NAME,
        TB.FIELD_NAME,
        TB.LAST_PROCESSED_PK,
        TB.PROCESSED_ROWS,
        TB.RE_ENCRYPTED_ROWS,
        TB.SKIPPED_ROWS,
        TB.STATUS,
        TB.LAST_ERROR,
        TB.TOTAL_ROWS
    };

    private final DSLContext ctx;

    @Autowired
    public PasswordRotationProgressDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public PasswordRotationProgress loadOrCreate(String targetPasswordFingerprint,
                                                 String tableName,
                                                 String fieldName) {
        List<Condition> conditions = buildUniqueKeyConditions(
            targetPasswordFingerprint, tableName, fieldName);
        Record record = ctx.select(ALL_FIELDS)
            .from(TB)
            .where(conditions)
            .fetchOne();

        if (record != null) {
            return toProgress(record);
        }

        ctx.insertInto(TB)
            .set(TB.TARGET_PASSWORD_FINGERPRINT, targetPasswordFingerprint)
            .set(TB.TABLE_NAME, tableName)
            .set(TB.FIELD_NAME, fieldName)
            .set(TB.PROCESSED_ROWS, 0L)
            .set(TB.RE_ENCRYPTED_ROWS, 0L)
            .set(TB.SKIPPED_ROWS, 0L)
            .set(TB.STATUS, PasswordRotationProgressStatus.PENDING.name())
            .onDuplicateKeyIgnore()
            .execute();

        record = ctx.select(ALL_FIELDS)
            .from(TB)
            .where(conditions)
            .fetchOne();

        return toProgress(record);
    }

    @Override
    public void updateProgress(PasswordRotationProgress progress) {
        ctx.update(TB)
            .set(TB.LAST_PROCESSED_PK, progress.getLastProcessedPk())
            .set(TB.PROCESSED_ROWS, progress.getProcessedRows())
            .set(TB.RE_ENCRYPTED_ROWS, progress.getReEncryptedRows())
            .set(TB.SKIPPED_ROWS, progress.getSkippedRows())
            .set(TB.STATUS, progress.getStatus())
            .set(TB.LAST_ERROR, progress.getLastError())
            .set(TB.TOTAL_ROWS, progress.getTotalRows())
            .where(TB.ID.eq(ULong.valueOf(progress.getId())))
            .execute();
    }

    private List<Condition> buildUniqueKeyConditions(String targetPasswordFingerprint,
                                                     String tableName,
                                                     String fieldName) {
        List<Condition> conditions = new ArrayList<>(3);
        conditions.add(TB.TARGET_PASSWORD_FINGERPRINT.eq(targetPasswordFingerprint));
        conditions.add(TB.TABLE_NAME.eq(tableName));
        conditions.add(TB.FIELD_NAME.eq(fieldName));
        return conditions;
    }

    private PasswordRotationProgress toProgress(Record record) {
        if (record == null) {
            return null;
        }
        PasswordRotationProgress p = new PasswordRotationProgress();
        ULong id = record.get(TB.ID);
        p.setId(id == null ? null : id.longValue());
        p.setTargetPasswordFingerprint(record.get(TB.TARGET_PASSWORD_FINGERPRINT));
        p.setTableName(record.get(TB.TABLE_NAME));
        p.setFieldName(record.get(TB.FIELD_NAME));
        p.setLastProcessedPk(record.get(TB.LAST_PROCESSED_PK));
        Long processedRows = record.get(TB.PROCESSED_ROWS);
        p.setProcessedRows(processedRows == null ? 0L : processedRows);
        Long reEncryptedRows = record.get(TB.RE_ENCRYPTED_ROWS);
        p.setReEncryptedRows(reEncryptedRows == null ? 0L : reEncryptedRows);
        Long skippedRows = record.get(TB.SKIPPED_ROWS);
        p.setSkippedRows(skippedRows == null ? 0L : skippedRows);
        p.setStatus(record.get(TB.STATUS));
        p.setLastError(record.get(TB.LAST_ERROR));
        p.setTotalRows(record.get(TB.TOTAL_ROWS));
        return p;
    }
}
