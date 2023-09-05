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

import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTypeDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceType;
import com.tencent.bk.job.file_gateway.model.tables.FileWorker;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class FileSourceTypeDAOImpl implements FileSourceTypeDAO {

    private static final FileSourceType defaultTable = FileSourceType.FILE_SOURCE_TYPE;
    private static final FileWorker tableFileWorker = FileWorker.FILE_WORKER;
    private final DSLContext dslContext;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.ID,
        defaultTable.WORKER_ID,
        defaultTable.STORAGE_TYPE,
        defaultTable.CODE,
        defaultTable.NAME,
        defaultTable.ENABLED,
        defaultTable.ICON,
        defaultTable.LAST_MODIFY_USER,
        defaultTable.LAST_MODIFY_TIME
    };

    @Autowired
    public FileSourceTypeDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public Integer batchInsert(List<FileSourceTypeDTO> fileSourceTypeList) {
        if (CollectionUtils.isEmpty(fileSourceTypeList)) {
            return 0;
        }
        val insertQuery = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ENABLED,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            (Integer) null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        BatchBindStep batchQuery = dslContext.batch(insertQuery);
        for (FileSourceTypeDTO fileSourceTypeDTO : fileSourceTypeList) {
            batchQuery = batchQuery.bind(
                null,
                fileSourceTypeDTO.getWorkerId(),
                fileSourceTypeDTO.getStorageType(),
                fileSourceTypeDTO.getCode(),
                fileSourceTypeDTO.getName(),
                fileSourceTypeDTO.getEnabled(),
                fileSourceTypeDTO.getIcon(),
                fileSourceTypeDTO.getLastModifier(),
                fileSourceTypeDTO.getLastModifyTime()
            );
        }
        int[] results = batchQuery.execute();
        int affectedRowNum = 0;
        for (int result : results) {
            affectedRowNum += result;
        }
        return affectedRowNum;
    }

    private Query buildUpdateByIdQuery(FileSourceTypeDTO fileSourceTypeDTO) {
        return dslContext.update(defaultTable)
            .set(defaultTable.WORKER_ID, fileSourceTypeDTO.getWorkerId())
            .set(defaultTable.STORAGE_TYPE, fileSourceTypeDTO.getStorageType())
            .set(defaultTable.CODE, fileSourceTypeDTO.getCode())
            .set(defaultTable.NAME, fileSourceTypeDTO.getName())
            .set(defaultTable.ENABLED, fileSourceTypeDTO.getEnabled())
            .set(defaultTable.ICON, fileSourceTypeDTO.getIcon())
            .set(defaultTable.LAST_MODIFY_USER, fileSourceTypeDTO.getLastModifier())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileSourceTypeDTO.getId()));
    }

    @Override
    public int batchUpdate(List<FileSourceTypeDTO> fileSourceTypeList) {
        if (CollectionUtils.isEmpty(fileSourceTypeList)) {
            return 0;
        }
        List<Query> queryList = new ArrayList<>();
        for (FileSourceTypeDTO fileSourceTypeDTO : fileSourceTypeList) {
            queryList.add(buildUpdateByIdQuery(fileSourceTypeDTO));
        }
        int[] results = dslContext.batch(queryList).execute();
        int affectedNum = 0;
        for (int result : results) {
            affectedNum += result;
        }
        return affectedNum;
    }

    @Override
    public int batchDeleteByIds(Collection<Integer> ids) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.in(ids)
        ).execute();
    }

    @Override
    public FileSourceTypeDTO getById(Integer id) {
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(defaultTable.ID.eq(id)
            ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public FileSourceTypeDTO getByCode(String code) {
        List<FileSourceTypeDTO> fileSourceTypeDTOList = listByCode(code);
        if (fileSourceTypeDTOList.isEmpty()) return null;
        return fileSourceTypeDTOList.get(0);
    }

    @Override
    public List<FileSourceTypeDTO> listByCode(String code) {
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable).where(
                defaultTable.CODE.eq(code)
            ).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> listByWorkerId(Long workerId) {
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable).where(
                defaultTable.WORKER_ID.eq(workerId)
            ).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> listByCodeOrderByVersion(String code) {
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .join(tableFileWorker)
            .on(defaultTable.WORKER_ID.eq(tableFileWorker.ID))
            .where(defaultTable.CODE.eq(code))
            .orderBy(tableFileWorker.VERSION.desc(), tableFileWorker.LAST_MODIFY_TIME.desc())
            .fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> listEnabledTypeOrderByVersion(String storageType) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ENABLED.equal(true));
        if (storageType != null) {
            conditions.add(defaultTable.STORAGE_TYPE.equal(storageType));
        }
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .join(tableFileWorker)
            .on(defaultTable.WORKER_ID.eq(tableFileWorker.ID))
            .where(conditions)
            .orderBy(tableFileWorker.VERSION.desc(), tableFileWorker.LAST_MODIFY_TIME.desc())
            .fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    private FileSourceTypeDTO convertRecordToDto(Record record) {
        return new FileSourceTypeDTO(
            record.get(defaultTable.ID),
            record.get(defaultTable.WORKER_ID),
            record.get(defaultTable.STORAGE_TYPE),
            record.get(defaultTable.CODE),
            record.get(defaultTable.NAME),
            record.get(defaultTable.ENABLED),
            record.get(defaultTable.ICON),
            record.get(defaultTable.LAST_MODIFY_USER),
            record.get(defaultTable.LAST_MODIFY_TIME)
        );
    }
}
