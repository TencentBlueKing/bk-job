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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileSourceType;
import org.jooq.generated.tables.FileWorker;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Repository
public class FileSourceTypeDAOImpl implements FileSourceTypeDAO {

    private static final FileSourceType defaultTable = FileSourceType.FILE_SOURCE_TYPE;
    private static final FileWorker tableFileWorker = FileWorker.FILE_WORKER;
    private final DSLContext dslContext;

    @Autowired
    public FileSourceTypeDAOImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    private void setDefaultValue(FileSourceTypeDTO fileSourceTypeDTO) {
        //
    }

    @Override
    public Integer upsertByWorker(DSLContext dslContext, FileSourceTypeDTO fileSourceTypeDTO) {
        AtomicInteger result = new AtomicInteger(-1);
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            FileSourceTypeDTO oldFileSourceTypeDTO = get(context, fileSourceTypeDTO.getWorkerId(),
                fileSourceTypeDTO.getStorageType(), fileSourceTypeDTO.getCode());
            if (oldFileSourceTypeDTO == null) {
                // 插入
                result.set(insert(context, fileSourceTypeDTO));
            } else {
                // 更新
                fileSourceTypeDTO.setId(oldFileSourceTypeDTO.getId());
                result.set(update(context, fileSourceTypeDTO));
            }
        });
        return result.get();
    }

    @Override
    public Integer insert(DSLContext dslContext, FileSourceTypeDTO fileSourceTypeDTO) {
        setDefaultValue(fileSourceTypeDTO);
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            (Integer) null,
            fileSourceTypeDTO.getWorkerId(),
            fileSourceTypeDTO.getStorageType(),
            fileSourceTypeDTO.getCode(),
            fileSourceTypeDTO.getName(),
            fileSourceTypeDTO.getIcon(),
            fileSourceTypeDTO.getLastModifier(),
            fileSourceTypeDTO.getLastModifyTime()
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
    public int update(DSLContext dslContext, FileSourceTypeDTO fileSourceTypeDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.WORKER_ID, fileSourceTypeDTO.getWorkerId())
            .set(defaultTable.STORAGE_TYPE, fileSourceTypeDTO.getStorageType())
            .set(defaultTable.CODE, fileSourceTypeDTO.getCode())
            .set(defaultTable.NAME, fileSourceTypeDTO.getName())
            .set(defaultTable.ICON, fileSourceTypeDTO.getIcon())
            .set(defaultTable.LAST_MODIFY_USER, fileSourceTypeDTO.getLastModifier())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileSourceTypeDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }


    @Override
    public int deleteById(DSLContext dslContext, Integer id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public FileSourceTypeDTO get(DSLContext dslContext, Long workerId, String storageType, String code) {
        val record = dslContext.selectFrom(defaultTable)
            .where(defaultTable.WORKER_ID.eq(workerId))
            .and(defaultTable.STORAGE_TYPE.eq(storageType))
            .and(defaultTable.CODE.eq(code))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public FileSourceTypeDTO getById(Integer id) {
        val record = dslContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
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
        val records = dslContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable).where(
                defaultTable.CODE.eq(code)
            ).fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> listByCodeOrderByVersion(String code) {
        val records = dslContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable)
            .join(tableFileWorker)
            .on(defaultTable.WORKER_ID.eq(tableFileWorker.ID))
            .where(defaultTable.CODE.eq(code))
            .orderBy(tableFileWorker.VERSION.desc(), tableFileWorker.LAST_MODIFY_TIME.desc())
            .fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> listOrderByVersion(DSLContext dslContext, String storageType) {
        List<Condition> conditions = new ArrayList<>();
        if (storageType != null) {
            conditions.add(defaultTable.STORAGE_TYPE.equal(storageType));
        }
        val records = dslContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable)
            .join(tableFileWorker)
            .on(defaultTable.WORKER_ID.eq(tableFileWorker.ID))
            .where(conditions)
            .orderBy(tableFileWorker.VERSION.desc(), tableFileWorker.LAST_MODIFY_TIME.desc())
            .fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<FileSourceTypeDTO> list(DSLContext dslContext, String storageType) {
        List<Condition> conditions = new ArrayList<>();
        if (storageType != null) {
            conditions.add(defaultTable.STORAGE_TYPE.equal(storageType));
        }
        val records = dslContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.STORAGE_TYPE,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.ICON,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable).where(conditions).orderBy(defaultTable.LAST_MODIFY_TIME.desc()).fetch();
        if (records == null || records.isEmpty()) {
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
            record.get(defaultTable.ICON),
            record.get(defaultTable.LAST_MODIFY_USER),
            record.get(defaultTable.LAST_MODIFY_TIME)
        );
    }
}
