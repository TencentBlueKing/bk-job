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

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.manage.dao.ApplicationFavorDAO;
import com.tencent.bk.job.manage.model.dto.ApplicationFavorDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.ApplicationFavor;
import org.jooq.generated.tables.records.ApplicationFavorRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class ApplicationFavorDAOImpl implements ApplicationFavorDAO {
    private static final ApplicationFavor defaultTable = ApplicationFavor.APPLICATION_FAVOR;

    private DSLContext dslContext;

    @Autowired
    public ApplicationFavorDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.dslContext = context;
    }

    @Override
    public int insertAppFavor(String username, long appId) {
        String sql = null;
        try {
            val query = dslContext.insertInto(defaultTable,
                defaultTable.USERNAME,
                defaultTable.APP_ID,
                defaultTable.FAVOR_TIME
            ).values(
                username,
                appId,
                System.currentTimeMillis()
            ).onDuplicateKeyUpdate().set(defaultTable.FAVOR_TIME, System.currentTimeMillis());
            sql = query.getSQL(ParamType.INLINED);
            return query.execute();
        } catch (Exception e) {
            if (sql != null) {
                log.error(sql);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ApplicationFavorDTO> getAppFavorListByUsername(String username) {
        List<Condition> conditions = new ArrayList<>();
        if (username != null) {
            conditions.add(defaultTable.USERNAME.eq(username));
        }
        val query = dslContext.selectFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.FAVOR_TIME.desc());
        Result<ApplicationFavorRecord> records = null;
        records = query.fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.map(this::convertRecordToDto);
    }

    @Override
    public int deleteAppFavor(String username, long appId) {
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.USERNAME.eq(username))
            .and(defaultTable.APP_ID.eq(appId))
            .execute();
    }

    @Override
    public int deleteAppFavorByAppId(long appId) {
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.APP_ID.eq(appId))
            .execute();
    }

    @Override
    public int deleteAppFavorByUser(String username) {
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.USERNAME.eq(username))
            .execute();
    }

    private ApplicationFavorDTO convertRecordToDto(ApplicationFavorRecord record) {
        try {
            return new ApplicationFavorDTO(
                record.getUsername(),
                record.getAppId(),
                record.getFavorTime()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
