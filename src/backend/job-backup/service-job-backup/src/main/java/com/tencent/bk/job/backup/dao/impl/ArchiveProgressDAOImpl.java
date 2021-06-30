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

import com.tencent.bk.job.backup.dao.ArchiveProgressDAO;
import com.tencent.bk.job.backup.model.dto.ArchiveProgressDTO;
import com.tencent.bk.job.backup.model.tables.ArchiveProgress;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Repository
public class ArchiveProgressDAOImpl implements ArchiveProgressDAO {

    private final DSLContext ctx;
    private final ArchiveProgress T = ArchiveProgress.ARCHIVE_PROGRESS;

    @Autowired
    public ArchiveProgressDAOImpl(@Qualifier("job-backup-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public ArchiveProgressDTO queryArchiveProgress(String table) {
        Record record = ctx.select(T.TABLE_NAME, T.LAST_ARCHIVED_ID, T.LAST_ARCHIVE_TIME,
            T.LAST_DELETED_ID, T.LAST_DELETE_TIME)
            .from(T)
            .where(T.TABLE_NAME.eq(table))
            .fetchOne();

        return extract(record);
    }

    private ArchiveProgressDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        ArchiveProgressDTO archiveProgress = new ArchiveProgressDTO();
        archiveProgress.setTableName(record.get(T.TABLE_NAME));
        archiveProgress.setLastArchivedId(record.get(T.LAST_ARCHIVED_ID));
        archiveProgress.setLastArchiveTime(record.get(T.LAST_ARCHIVE_TIME));
        archiveProgress.setLastDeletedId(record.get(T.LAST_DELETED_ID));
        archiveProgress.setLastDeleteTime(record.get(T.LAST_DELETE_TIME));
        return archiveProgress;
    }

    @Override
    public void saveArchiveProgress(ArchiveProgressDTO archiveProgress) {
        ctx.insertInto(T, T.TABLE_NAME, T.LAST_ARCHIVED_ID, T.LAST_ARCHIVE_TIME)
            .values(archiveProgress.getTableName(), archiveProgress.getLastArchivedId(),
                archiveProgress.getLastArchiveTime())
            .onDuplicateKeyUpdate()
            .set(T.LAST_ARCHIVED_ID, archiveProgress.getLastArchivedId())
            .set(T.LAST_ARCHIVE_TIME, archiveProgress.getLastArchiveTime())
            .execute();
    }

    @Override
    public void saveDeleteProgress(ArchiveProgressDTO archiveProgress) {
        ctx.insertInto(T, T.TABLE_NAME, T.LAST_DELETED_ID, T.LAST_DELETE_TIME)
            .values(archiveProgress.getTableName(), archiveProgress.getLastDeletedId(),
                archiveProgress.getLastDeleteTime())
            .onDuplicateKeyUpdate()
            .set(T.LAST_DELETED_ID, archiveProgress.getLastDeletedId())
            .set(T.LAST_DELETE_TIME, archiveProgress.getLastDeleteTime())
            .execute();
    }
}
