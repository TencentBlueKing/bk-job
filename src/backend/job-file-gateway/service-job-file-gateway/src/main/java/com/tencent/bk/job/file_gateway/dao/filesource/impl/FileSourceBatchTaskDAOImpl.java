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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceBatchTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBatchTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileSourceBatchTask;
import org.jooq.generated.tables.records.FileSourceBatchTaskRecord;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
public class FileSourceBatchTaskDAOImpl extends BaseDAOImpl implements FileSourceBatchTaskDAO {

    private static final FileSourceBatchTask defaultTable = FileSourceBatchTask.FILE_SOURCE_BATCH_TASK;
    private final FileSourceTaskDAO fileSourceTaskDAO;

    @Autowired
    public FileSourceBatchTaskDAOImpl(FileSourceTaskDAO fileSourceTaskDAO) {
        this.fileSourceTaskDAO = fileSourceTaskDAO;
    }

    private void setDefaultValue(FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
        if (fileSourceBatchTaskDTO.getFileCleared() == null) {
            fileSourceBatchTaskDTO.setFileCleared(false);
        }
    }

    @Override
    public String insertFileSourceBatchTask(DSLContext dslContext, FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
        setDefaultValue(fileSourceBatchTaskDTO);
        String id = JobUUID.getUUID();
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.STEP_INSTANCE_ID,
            defaultTable.EXECUTE_COUNT,
            defaultTable.STATUS,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            id,
            fileSourceBatchTaskDTO.getAppId(),
            fileSourceBatchTaskDTO.getStepInstanceId(),
            fileSourceBatchTaskDTO.getExecuteCount(),
            fileSourceBatchTaskDTO.getStatus(),
            fileSourceBatchTaskDTO.getCreator(),
            fileSourceBatchTaskDTO.getCreateTime(),
            System.currentTimeMillis()
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            int affectedRowNum = query.execute();
            if (affectedRowNum != 1) {
                log.error("Fail to insertFileSourceBatchTask, fileSourceBatchTaskDTO={}",
                    JsonUtils.toJson(fileSourceBatchTaskDTO));
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            List<FileSourceTaskDTO> fileSourceTaskDTOList = fileSourceBatchTaskDTO.getFileSourceTaskList();
            if (fileSourceTaskDTOList == null) {
                return id;
            }
            for (FileSourceTaskDTO fileSourceTaskDTO : fileSourceTaskDTOList) {
                fileSourceTaskDTO.setBatchTaskId(id);
                // 插入FileSourceTask
                fileSourceTaskDAO.insertFileSourceTask(dslContext, fileSourceTaskDTO);
            }
            fileSourceBatchTaskDTO.setFileSourceTaskList(fileSourceTaskDTOList);
            return id;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSourceBatchTask(DSLContext dslContext, FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.APP_ID, fileSourceBatchTaskDTO.getAppId())
            .set(defaultTable.STATUS, fileSourceBatchTaskDTO.getStatus())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileSourceBatchTaskDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileClearStatus(DSLContext dslContext, List<String> taskIdList, boolean fileCleared) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.FILE_CLEARED, fileCleared)
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.in(taskIdList));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteFileSourceBatchTaskById(DSLContext dslContext, String id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public FileSourceBatchTaskDTO getFileSourceBatchTaskById(DSLContext dslContext, String id) {
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
    public Long countFileSourceBatchTasks(DSLContext dslContext, Long appId) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        return countFileSourceBatchTasksByConditions(dslContext, conditions);
    }

    public Long countFileSourceBatchTasksByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Long.class);
    }

    @Override
    public List<FileSourceBatchTaskDTO> listFileSourceBatchTasks(DSLContext dslContext, Long appId, Integer start,
                                                                 Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        val query = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    private FileSourceBatchTaskDTO convertRecordToDto(FileSourceBatchTaskRecord record) {
        String id = record.getId();
        List<FileSourceTaskDTO> fileSourceTaskDTOList = fileSourceTaskDAO.listByBatchTaskId(id);
        return new FileSourceBatchTaskDTO(
            id,
            record.getAppId(),
            record.getStepInstanceId(),
            record.getExecuteCount(),
            fileSourceTaskDTOList,
            record.getStatus(),
            record.getFileCleared(),
            record.getCreator(),
            record.getCreateTime(),
            record.getLastModifyTime()
        );
    }
}
