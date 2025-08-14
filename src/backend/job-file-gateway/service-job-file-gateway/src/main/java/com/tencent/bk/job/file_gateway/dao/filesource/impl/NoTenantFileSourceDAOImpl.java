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

import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceShareDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.NoTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSource;
import com.tencent.bk.job.file_gateway.util.JooqTypeUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class NoTenantFileSourceDAOImpl extends BaseFileSourceDAOImpl implements NoTenantFileSourceDAO {

    private static final FileSource defaultTable = FileSource.FILE_SOURCE;
    private final DSLContext dslContext;

    @Autowired
    public NoTenantFileSourceDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext,
                                     FileSourceShareDAO fileSourceShareDAO,
                                     FileSourceTypeDAO fileSourceTypeDAO) {
        super(dslContext, fileSourceShareDAO, fileSourceTypeDAO);
        this.dslContext = dslContext;
    }

    private List<Condition> buildIdConditions(Integer id) {
        List<Condition> conditionList = new ArrayList<>();
        conditionList.add(defaultTable.ID.eq(id));
        return conditionList;
    }

    @Override
    public int updateFileSourceStatus(Integer fileSourceId, Integer status) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.STATUS, JooqTypeUtil.convertToByte(status))
            .where(defaultTable.ID.eq(fileSourceId));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public FileSourceDTO getFileSourceById(Integer id) {
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(buildIdConditions(id))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public FileSourceDTO getFileSourceByCode(String code) {
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(buildCodeConditions(code))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    private List<Condition> buildCodeConditions(String code) {
        List<Condition> conditionList = new ArrayList<>();
        conditionList.add(defaultTable.CODE.eq(code));
        return conditionList;
    }

    @Override
    public FileSourceDTO getFileSourceByCode(Long appId, String code) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.CODE.eq(code));
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions)
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public List<FileSourceDTO> listEnabledFileSource(Integer start, Integer pageSize) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ENABLE.eq(true));
        return listFileSourceByConditions(conditions, start, pageSize);
    }

    @Override
    public boolean existsFileSourceUsingCredential(Long appId, String credentialId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.CREDENTIAL_ID.eq(credentialId));
        val query = dslContext.selectZero().from(defaultTable)
            .where(conditions)
            .limit(1);
        return !query.fetch().isEmpty();
    }

}
