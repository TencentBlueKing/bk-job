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
import com.tencent.bk.job.common.mysql.dao.BaseDAOImpl;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceBatchTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBatchTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceBatchTask;
import com.tencent.bk.job.file_gateway.model.tables.records.FileSourceBatchTaskRecord;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class FileSourceBatchTaskDAOImpl extends BaseDAOImpl implements FileSourceBatchTaskDAO {

    private static final FileSourceBatchTask defaultTable = FileSourceBatchTask.FILE_SOURCE_BATCH_TASK;
    private final DSLContext dslContext;
    private final FileSourceTaskDAO fileSourceTaskDAO;

    @Autowired
    public FileSourceBatchTaskDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext,
                                      FileSourceTaskDAO fileSourceTaskDAO) {
        this.dslContext = dslContext;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
    }

    private void setDefaultValue(FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
        if (fileSourceBatchTaskDTO.getFileCleared() == null) {
            fileSourceBatchTaskDTO.setFileCleared(false);
        }
    }

    @Override
    public String insertFileSourceBatchTask(FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
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
                fileSourceTaskDAO.insertFileSourceTask(fileSourceTaskDTO);
            }
            fileSourceBatchTaskDTO.setFileSourceTaskList(fileSourceTaskDTOList);
            return id;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSourceBatchTask(FileSourceBatchTaskDTO fileSourceBatchTaskDTO) {
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
    public FileSourceBatchTaskDTO getBatchTaskById(String id) {
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
    public FileSourceBatchTaskDTO getBatchTaskByIdForUpdate(String id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).forUpdate().fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
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
