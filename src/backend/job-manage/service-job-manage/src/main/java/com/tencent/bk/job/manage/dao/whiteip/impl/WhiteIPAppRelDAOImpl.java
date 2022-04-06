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

package com.tencent.bk.job.manage.dao.whiteip.impl;

import com.tencent.bk.job.manage.dao.whiteip.WhiteIPAppRelDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPAppRelDTO;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.generated.tables.WhiteIpAppRel;
import org.jooq.generated.tables.records.WhiteIpAppRelRecord;
import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class WhiteIPAppRelDAOImpl implements WhiteIPAppRelDAO {
    private static final WhiteIpAppRel T_WHITE_IP_APP_REL = WhiteIpAppRel.WHITE_IP_APP_REL;

    @Override
    public int insertWhiteIPAppRel(DSLContext dslContext, String username, Long recordId, Long appId) {
        return dslContext.insertInto(T_WHITE_IP_APP_REL,
            T_WHITE_IP_APP_REL.RECORD_ID,
            T_WHITE_IP_APP_REL.APP_ID,
            T_WHITE_IP_APP_REL.CREATOR,
            T_WHITE_IP_APP_REL.CREATE_TIME
        ).values(
            recordId,
            appId,
            username,
            ULong.valueOf(System.currentTimeMillis())
        ).execute();
    }

    @Override
    public int deleteWhiteIPAppRelByRecordId(DSLContext dslContext, Long recordId) {
        return dslContext.deleteFrom(T_WHITE_IP_APP_REL).where(
            T_WHITE_IP_APP_REL.RECORD_ID.eq(recordId)
        ).execute();
    }

    @Override
    public int deleteWhiteIPAppRelByAppId(DSLContext dslContext, Long appId) {
        return dslContext.deleteFrom(T_WHITE_IP_APP_REL).where(
            T_WHITE_IP_APP_REL.APP_ID.eq(appId)
        ).execute();
    }

    @Override
    public List<Long> listAppIdByRecordId(DSLContext dslContext, Long recordId) {
        val records = dslContext.selectFrom(T_WHITE_IP_APP_REL).where(
            T_WHITE_IP_APP_REL.RECORD_ID.eq(recordId)
        ).fetch();
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream().map(WhiteIpAppRelRecord::getAppId).collect(Collectors.toList());
    }

    @Override
    public List<WhiteIPAppRelDTO> listAppRelByRecordIds(DSLContext dslContext, List<Long> recordIdList) {
        val records =
            dslContext.select(T_WHITE_IP_APP_REL.APP_ID, T_WHITE_IP_APP_REL.RECORD_ID).from(T_WHITE_IP_APP_REL).where(
                T_WHITE_IP_APP_REL.RECORD_ID.in(recordIdList)
            ).fetch();
        return records.stream().map(record ->
            new WhiteIPAppRelDTO(
                record.get(T_WHITE_IP_APP_REL.RECORD_ID).longValue(),
                record.get(T_WHITE_IP_APP_REL.APP_ID).longValue(),
                null,
                null
            )
        ).collect(Collectors.toList());
    }
}
