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

import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerTagDAO;
import com.tencent.bk.job.file_gateway.model.dto.WorkerTagDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.FileWorkerTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@Slf4j
public class FileWorkerTagDAOImpl implements FileWorkerTagDAO {

    private static final FileWorkerTag defaultTable = FileWorkerTag.FILE_WORKER_TAG;
    private final DSLContext defaultContext;

    @Autowired
    public FileWorkerTagDAOImpl(DSLContext dslContext) {
        this.defaultContext = dslContext;
    }

    @Override
    public int batchInsert(Long workerId, List<String> tagList) {
        if (CollectionUtils.isEmpty(tagList)) {
            return 0;
        }
        val insertQuery = defaultContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.TAG
        ).values(
            (Long) null,
            null,
            null
        ).onDuplicateKeyIgnore();
        BatchBindStep batchQuery = defaultContext.batch(insertQuery);
        for (String tag : tagList) {
            batchQuery = batchQuery.bind(
                null,
                workerId,
                tag
            );
        }
        int[] results = batchQuery.execute();
        int affectedRowNum = 0;
        for (int result : results) {
            affectedRowNum += result;
        }
        return affectedRowNum;
    }

    @Override
    public int deleteById(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return 0;
        }
        return defaultContext.deleteFrom(defaultTable).where(
            defaultTable.ID.in(ids)
        ).execute();
    }

    @Override
    public List<WorkerTagDTO> listTagByWorkerId(Long workerId) {
        val records = defaultContext.select(
            defaultTable.ID,
            defaultTable.WORKER_ID,
            defaultTable.TAG
        ).from(defaultTable)
            .where(defaultTable.WORKER_ID.eq(workerId))
            .fetch();
        return records.map(this::convert);
    }

    @Override
    public List<Long> listWorkerIdByTag(Collection<String> tags) {
        val records = defaultContext.select(
            defaultTable.WORKER_ID
        ).from(defaultTable)
            .where(defaultTable.TAG.in(tags))
            .fetch();
        return records.map(record -> record.get(defaultTable.WORKER_ID));
    }

    private WorkerTagDTO convert(Record record) {
        WorkerTagDTO workerTagDTO = new WorkerTagDTO();
        workerTagDTO.setId(record.get(defaultTable.ID));
        workerTagDTO.setWorkerId(record.get(defaultTable.WORKER_ID));
        workerTagDTO.setTag(record.get(defaultTable.TAG));
        return workerTagDTO;
    }
}
