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

package com.tencent.bk.job.file_gateway.dao.filesource.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.mysql.dao.BaseDAOImpl;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceTask;
import com.tencent.bk.job.file_gateway.model.tables.records.FileSourceTaskRecord;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
public class FileSourceTaskDAOImpl extends BaseDAOImpl implements FileSourceTaskDAO {

    private static final FileSourceTask defaultTable = FileSourceTask.FILE_SOURCE_TASK;
    private final FileTaskDAO fileTaskDAO;
    private final DSLContext dslContext;

    @Autowired
    public FileSourceTaskDAOImpl(FileTaskDAO fileTaskDAO,
                                 @Qualifier("job-file-gateway-dsl-context") DSLContext dslContext) {
        this.fileTaskDAO = fileTaskDAO;
        this.dslContext = dslContext;
    }

    private void setDefaultValue(FileSourceTaskDTO fileSourceTaskDTO) {
        if (fileSourceTaskDTO.getFileCleared() == null) {
            fileSourceTaskDTO.setFileCleared(false);
        }
    }

    @Override
    public String insertFileSourceTask(FileSourceTaskDTO fileSourceTaskDTO) {
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
                Long fileTaskId = fileTaskDAO.insertFileTask(fileTaskDTO);
                log.debug("{} inserted, id={}", fileTaskDTO, fileTaskId);
            }
            fileSourceTaskDTO.setFileTaskList(fileTaskDTOList);
            return id;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSourceTask(FileSourceTaskDTO fileSourceTaskDTO) {
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
    public int updateFileClearStatus(List<String> taskIdList, boolean fileCleared) {
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
    public int deleteById(String id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public FileSourceTaskDTO getFileSourceTaskById(String id) {
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
    public FileSourceTaskDTO getFileSourceTaskByIdForUpdate(String id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).forUpdate().fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public Long countFileSourceTasksByBatchTaskId(String batchTaskId, Byte status) {
        List<Condition> conditions = new ArrayList<>();
        if (batchTaskId != null) {
            conditions.add(defaultTable.BATCH_TASK_ID.eq(batchTaskId));
        }
        if (status != null) {
            conditions.add(defaultTable.STATUS.eq(status));
        }
        return countFileSourceTasksByConditions(conditions);
    }

    public Long countFileSourceTasksByConditions(Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Long.class);
    }

    public List<FileSourceTaskDTO> listByConditions(Collection<Condition> conditions,
                                                    Integer start, Integer pageSize) {
        val query = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    @Override
    public List<FileSourceTaskDTO> listByBatchTaskId(String batchTaskId) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(batchTaskId)) {
            conditions.add(defaultTable.BATCH_TASK_ID.eq(batchTaskId));
        }
        return listByConditions(conditions, null, null);
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
