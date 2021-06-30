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

package com.tencent.bk.job.file_gateway.dao.filesource.impl;

import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileTask;
import org.jooq.generated.tables.records.FileTaskRecord;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class FileTaskDAOImpl extends BaseDAOImpl implements FileTaskDAO {

    private static final FileTask defaultTable = FileTask.FILE_TASK;
    private DSLContext dslContext;

    @Autowired
    public FileTaskDAOImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    private void setDefaultValue(FileTaskDTO fileTaskDTO) {
        //
    }

    @Override
    public Long insertFileTask(DSLContext dslContext, FileTaskDTO fileTaskDTO) {
        setDefaultValue(fileTaskDTO);
        val query = dslContext.insertInto(defaultTable,
            defaultTable.FILE_SOURCE_TASK_ID,
            defaultTable.FILE_PATH,
            defaultTable.DOWNLOAD_PATH,
            defaultTable.STATUS,
            defaultTable.PROGRESS,
            defaultTable.ERROR_MSG,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            fileTaskDTO.getFileSourceTaskId(),
            fileTaskDTO.getFilePath(),
            fileTaskDTO.getDownloadPath(),
            fileTaskDTO.getStatus(),
            fileTaskDTO.getProgress(),
            fileTaskDTO.getErrorMsg(),
            fileTaskDTO.getCreateTime(),
            fileTaskDTO.getLastModifyTime()
        ).returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.fetchOne().getId();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileTask(DSLContext dslContext, FileTaskDTO fileTaskDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.FILE_SOURCE_TASK_ID, fileTaskDTO.getFileSourceTaskId())
            .set(defaultTable.FILE_PATH, fileTaskDTO.getFilePath())
            .set(defaultTable.DOWNLOAD_PATH, fileTaskDTO.getDownloadPath())
            .set(defaultTable.FILE_SIZE, fileTaskDTO.getFileSize())
            .set(defaultTable.STATUS, fileTaskDTO.getStatus())
            .set(defaultTable.PROGRESS, fileTaskDTO.getProgress())
            .set(defaultTable.ERROR_MSG, fileTaskDTO.getErrorMsg())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileTaskDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int resetFileTasks(DSLContext dslContext, String fileSourceTaskId) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.DOWNLOAD_PATH, (String) null)
            .set(defaultTable.STATUS, TaskStatusEnum.INIT.getStatus())
            .set(defaultTable.PROGRESS, 0)
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }


    @Override
    public int deleteFileTaskById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public int deleteFileTaskByFileSourceTaskId(DSLContext dslContext, String fileSourceTaskId) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId)
        ).execute();
    }

    @Override
    public FileTaskDTO getFileTaskById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public FileTaskDTO getOneFileTask(DSLContext dslContext, String fileSourceTaskId, String filePath) {
        List<Condition> conditions = new ArrayList<>();
        if (fileSourceTaskId != null) {
            conditions.add(defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId));
        }
        if (filePath != null) {
            conditions.add(defaultTable.FILE_PATH.eq(filePath));
        }
        val record = dslContext.selectFrom(defaultTable).where(
            conditions
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public Long countFileTasks(DSLContext dslContext, String fileSourceTaskId) {
        List<Condition> conditions = new ArrayList<>();
        if (fileSourceTaskId != null) {
            conditions.add(defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId));
        }
        return countFileTasksByConditions(dslContext, conditions);
    }

    public Long countFileTasksByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Long.class);
    }

    @Override
    public List<FileTaskDTO> listFileTasks(String fileSourceTaskId, Integer start, Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        if (fileSourceTaskId != null) {
            conditions.add(defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId));
        }
        val query = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    @Override
    public List<String> listTimeoutFileSourceTaskIds(DSLContext dslContext, Long expireTimeMills,
                                                     Collection<Byte> statusSet, Integer start, Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        if (expireTimeMills != null) {
            conditions.add(defaultTable.LAST_MODIFY_TIME.le(System.currentTimeMillis() - expireTimeMills));
        }
        if (statusSet != null && !statusSet.isEmpty()) {
            conditions.add(defaultTable.STATUS.in(statusSet));
        }
        val query = dslContext.selectDistinct(defaultTable.FILE_SOURCE_TASK_ID).from(defaultTable)
            .where(conditions);
        Result<Record1<String>> records = null;
        if (start != null && start > 0 && pageSize != null && pageSize > 0) {
            records = query.limit(start, pageSize).fetch();
        } else {
            records = query.fetch();
        }
        return records.parallelStream().map(record -> record.get(defaultTable.FILE_SOURCE_TASK_ID)).collect(Collectors.toList());
    }

    @Override
    public List<FileTaskDTO> listFileTasks(String fileSourceTaskId) {
        return listFileTasks(fileSourceTaskId, 0, -1);
    }

    @Override
    public Long countFileTask(String fileSourceTaskId, Byte status) {
        List<Condition> conditions = new ArrayList<>();
        if (fileSourceTaskId != null) {
            conditions.add(defaultTable.FILE_SOURCE_TASK_ID.eq(fileSourceTaskId));
        }
        if (status != null) {
            conditions.add(defaultTable.STATUS.eq(status));
        }
        return countFileTasksByConditions(dslContext, conditions);
    }

    private FileTaskDTO convertRecordToDto(FileTaskRecord record) {
        return new FileTaskDTO(
            record.getId(),
            record.getFileSourceTaskId(),
            record.getFilePath(),
            record.getDownloadPath(),
            record.getFileSize(),
            record.getStatus(),
            record.getProgress(),
            record.getErrorMsg(),
            record.getCreateTime(),
            record.getLastModifyTime()
        );
    }
}
