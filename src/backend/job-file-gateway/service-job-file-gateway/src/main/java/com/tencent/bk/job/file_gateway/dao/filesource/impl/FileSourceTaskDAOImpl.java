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
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileSourceTask;
import org.jooq.generated.tables.records.FileSourceTaskRecord;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
public class FileSourceTaskDAOImpl extends BaseDAOImpl implements FileSourceTaskDAO {

    private static final FileSourceTask defaultTable = FileSourceTask.FILE_SOURCE_TASK;
    private final FileTaskDAO fileTaskDAO;
    private final DSLContext defaultContext;

    @Autowired
    public FileSourceTaskDAOImpl(FileTaskDAO fileTaskDAO, DSLContext dslContext) {
        this.fileTaskDAO = fileTaskDAO;
        this.defaultContext = dslContext;
    }

    private void setDefaultValue(FileSourceTaskDTO fileSourceTaskDTO) {
        if (fileSourceTaskDTO.getFileCleared() == null) {
            fileSourceTaskDTO.setFileCleared(false);
        }
    }

    @Override
    public String insertFileSourceTask(DSLContext dslContext, FileSourceTaskDTO fileSourceTaskDTO) {
        setDefaultValue(fileSourceTaskDTO);
        String id = fileSourceTaskDTO.getId();
        if (id == null) {
            id = JobUUID.getUUID();
        }
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.BATCH_TASK_ID,
            defaultTable.APP_ID,
            defaultTable.STEP_INSTANCE_ID,
            defaultTable.EXECUTE_COUNT,
            defaultTable.FILE_SOURCE_ID,
            defaultTable.FILE_WORKER_ID,
            defaultTable.STATUS,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            id,
            fileSourceTaskDTO.getBatchTaskId(),
            fileSourceTaskDTO.getAppId(),
            fileSourceTaskDTO.getStepInstanceId(),
            fileSourceTaskDTO.getExecuteCount(),
            fileSourceTaskDTO.getFileSourceId(),
            fileSourceTaskDTO.getFileWorkerId(),
            fileSourceTaskDTO.getStatus(),
            fileSourceTaskDTO.getCreator(),
            fileSourceTaskDTO.getCreateTime(),
            System.currentTimeMillis()
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            int affectedRowNum = query.execute();
            if (affectedRowNum != 1) {
                log.error("Fail to insertFileSourceTask, fileSourceTaskDTO={}", JsonUtils.toJson(fileSourceTaskDTO));
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
            List<FileTaskDTO> fileTaskDTOList = fileSourceTaskDTO.getFileTaskList();
            for (FileTaskDTO fileTaskDTO : fileTaskDTOList) {
                fileTaskDTO.setFileSourceTaskId(id);
                // 插入FileTask
                fileTaskDAO.insertFileTask(dslContext, fileTaskDTO);
            }
            fileSourceTaskDTO.setFileTaskList(fileTaskDTOList);
            return id;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSourceTask(DSLContext dslContext, FileSourceTaskDTO fileSourceTaskDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.BATCH_TASK_ID, fileSourceTaskDTO.getBatchTaskId())
            .set(defaultTable.APP_ID, fileSourceTaskDTO.getAppId())
            .set(defaultTable.FILE_SOURCE_ID, fileSourceTaskDTO.getFileSourceId())
            .set(defaultTable.FILE_WORKER_ID, fileSourceTaskDTO.getFileWorkerId())
            .set(defaultTable.STATUS, fileSourceTaskDTO.getStatus())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileSourceTaskDTO.getId()));
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
    public int deleteById(DSLContext dslContext, String id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public FileSourceTaskDTO getFileSourceTaskById(DSLContext dslContext, String id) {
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
    public Long countFileSourceTasks(DSLContext dslContext, Long appId) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        return countFileSourceTasksByConditions(dslContext, conditions);
    }

    @Override
    public Long countFileSourceTasksByBatchTaskId(DSLContext dslContext, String batchTaskId, Byte status) {
        List<Condition> conditions = new ArrayList<>();
        if (batchTaskId != null) {
            conditions.add(defaultTable.BATCH_TASK_ID.eq(batchTaskId));
        }
        if (status != null) {
            conditions.add(defaultTable.STATUS.eq(status));
        }
        return countFileSourceTasksByConditions(dslContext, conditions);
    }

    public Long countFileSourceTasksByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Long.class);
    }

    public List<FileSourceTaskDTO> listByConditions(DSLContext dslContext, Collection<Condition> conditions,
                                                    Integer start, Integer pageSize) {
        val query = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    @Override
    public List<FileSourceTaskDTO> listFileSourceTasks(DSLContext dslContext, Long appId, Integer start,
                                                       Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        return listByConditions(dslContext, conditions, start, pageSize);
    }

    @Override
    public List<FileSourceTaskDTO> listTimeoutTasks(DSLContext dslContext, Long expireTimeMills,
                                                    Collection<Byte> statusSet, Integer start, Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        if (expireTimeMills != null && expireTimeMills > 0) {
            conditions.add(defaultTable.LAST_MODIFY_TIME.le(System.currentTimeMillis() - expireTimeMills));
        }
        if (statusSet != null && !statusSet.isEmpty()) {
            conditions.add(defaultTable.STATUS.in(statusSet));
        }
        return listByConditions(defaultContext, conditions, start, pageSize);
    }

    @Override
    public List<FileSourceTaskDTO> listByBatchTaskId(String batchTaskId) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(batchTaskId)) {
            conditions.add(defaultTable.BATCH_TASK_ID.eq(batchTaskId));
        }
        return listByConditions(defaultContext, conditions, null, null);
    }

    @Override
    public int deleteByBatchTaskId(DSLContext dslContext, String batchTaskId) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.BATCH_TASK_ID.eq(batchTaskId)
        ).execute();
    }

    private FileSourceTaskDTO convertRecordToDto(FileSourceTaskRecord record) {
        String id = record.getId();
        List<FileTaskDTO> fileTaskDTOList = fileTaskDAO.listFileTasks(id);
        return new FileSourceTaskDTO(
            id,
            record.getBatchTaskId(),
            record.getAppId(),
            record.getStepInstanceId(),
            record.getExecuteCount(),
            fileTaskDTOList,
            record.getFileSourceId(),
            record.getFileWorkerId(),
            record.getStatus(),
            record.getFileCleared(),
            record.getCreator(),
            record.getCreateTime(),
            record.getLastModifyTime()
        );
    }
}
