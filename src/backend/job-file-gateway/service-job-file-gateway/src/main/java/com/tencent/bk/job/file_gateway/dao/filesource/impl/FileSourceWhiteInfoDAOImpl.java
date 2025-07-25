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

import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceWhiteInfoDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceWhiteInfo;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Repository
public class FileSourceWhiteInfoDAOImpl implements FileSourceWhiteInfoDAO {

    private static final FileSourceWhiteInfo defaultTable = FileSourceWhiteInfo.FILE_SOURCE_WHITE_INFO;
    private final DSLContext dslContext;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.ID,
        defaultTable.TYPE,
        defaultTable.CONTENT,
        defaultTable.REMARK,
        defaultTable.CREATOR,
        defaultTable.CREATE_TIME
    };

    @Autowired
    public FileSourceWhiteInfoDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public Integer insert(FileSourceWhiteInfoDTO fileSourceWhiteInfoDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.TYPE,
            defaultTable.CONTENT,
            defaultTable.REMARK,
            defaultTable.CREATOR
        ).values(
            null,
            fileSourceWhiteInfoDTO.getType(),
            fileSourceWhiteInfoDTO.getContent(),
            fileSourceWhiteInfoDTO.getRemark(),
            fileSourceWhiteInfoDTO.getCreator()
        ).returning(defaultTable.ID);
        val record = query.fetchOne();
        assert record != null;
        return record.getId();
    }

    @Override
    public List<FileSourceWhiteInfoDTO> list(Integer offset, Integer limit) {
        val query = dslContext.select(ALL_FIELDS)
            .from(defaultTable);
        Result<?> records;
        if (offset != null && offset >= 0 && limit != null && limit > 0) {
            records = query.limit(offset, limit).fetch();
        } else {
            records = query.fetch();
        }
        return records.map(this::convert);
    }

    @Override
    public boolean exists(String type, String content) {
        List<Condition> conditions = new ArrayList<>(2);
        conditions.add(defaultTable.TYPE.eq(type));
        conditions.add(defaultTable.CONTENT.eq(content));
        return dslContext.fetchExists(defaultTable, conditions);
    }

    @Override
    public int delete(Collection<Integer> ids) {
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.ID.in(ids))
            .execute();
    }

    private FileSourceWhiteInfoDTO convert(Record record) {
        return new FileSourceWhiteInfoDTO(
            record.get(defaultTable.ID),
            record.get(defaultTable.TYPE),
            record.get(defaultTable.CONTENT),
            record.get(defaultTable.REMARK),
            record.get(defaultTable.CREATOR),
            record.get(defaultTable.CREATE_TIME)
        );
    }
}
