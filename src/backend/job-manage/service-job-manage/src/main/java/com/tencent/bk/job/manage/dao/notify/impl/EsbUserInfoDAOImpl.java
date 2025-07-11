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

package com.tencent.bk.job.manage.dao.notify.impl;

import com.tencent.bk.job.manage.dao.notify.EsbUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.EsbUserInfoDTO;
import com.tencent.bk.job.manage.model.tables.EsbUserInfo;
import com.tencent.bk.job.manage.model.tables.records.EsbUserInfoRecord;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Repository
public class EsbUserInfoDAOImpl implements EsbUserInfoDAO {

    private final DSLContext dslContext;
    private static final Logger logger = LoggerFactory.getLogger(EsbUserInfoDAOImpl.class);
    private static final EsbUserInfo T_ESB_USER_INFO = EsbUserInfo.ESB_USER_INFO;
    private static final EsbUserInfo defaultTable = T_ESB_USER_INFO;

    @Autowired
    public EsbUserInfoDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int insertEsbUserInfo(EsbUserInfoDTO esbUserInfoDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.USERNAME,
            defaultTable.DISPLAY_NAME,
            defaultTable.LOGO,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            esbUserInfoDTO.getId(),
            esbUserInfoDTO.getUsername(),
            esbUserInfoDTO.getDisplayName(),
            esbUserInfoDTO.getLogo(),
            ULong.valueOf(esbUserInfoDTO.getLastModifyTime())
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            logger.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteEsbUserInfoById(Long id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public List<EsbUserInfoDTO> listEsbUserInfo() {
        val records = dslContext.selectFrom(defaultTable).fetch();
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(record -> new EsbUserInfoDTO(
                record.getId(),
                record.getUsername(),
                record.getDisplayName(),
                record.getLogo(),
                record.getLastModifyTime().longValue()
            ));
        }
    }

    @Override
    public List<EsbUserInfoDTO> listEsbUserInfo(String prefixStr, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.startsWith(prefixStr));
        return listEsbUserInfoByConditions(dslContext, conditions, limit);
    }

    @Override
    public List<EsbUserInfoDTO> listEsbUserInfo(Collection<String> userNames) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.in(userNames));
        return listEsbUserInfoByConditions(dslContext, conditions, null);
    }

    @Override
    public List<String> listExistUserName(Collection<String> userNames) {
        if (CollectionUtils.isEmpty(userNames)) {
            return Collections.emptyList();
        }
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.in(userNames));
        val baseQuery = dslContext.select(defaultTable.USERNAME)
            .from(defaultTable)
            .where(conditions);
        Result<Record1<String>> records = baseQuery.fetch();
        return records.map(record -> record.get(defaultTable.USERNAME));
    }

    private List<EsbUserInfoDTO> listEsbUserInfoByConditions(DSLContext dslContext, List<Condition> conditions,
                                                             Long limit) {
        val baseQuery = dslContext.selectFrom(defaultTable)
            .where(conditions);
        Result<EsbUserInfoRecord> records;
        if (null != limit && limit > 0) {
            records = baseQuery.limit(limit).fetch();
        } else {
            records = baseQuery.fetch();
        }
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(record -> new EsbUserInfoDTO(
                record.getId(),
                record.getUsername(),
                record.getDisplayName(),
                record.getLogo(),
                record.getLastModifyTime().longValue()
            ));
        }
    }
}
