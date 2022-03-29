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

import com.tencent.bk.job.manage.dao.whiteip.WhiteIPIPDAO;
import com.tencent.bk.job.manage.model.dto.whiteip.WhiteIPIPDTO;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.generated.tables.WhiteIpIp;
import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class WhiteIPIPDAOImpl implements WhiteIPIPDAO {
    private static final WhiteIpIp T_WHITE_IP_IP = WhiteIpIp.WHITE_IP_IP;

    @Override
    public Long insertWhiteIPIP(DSLContext dslContext, WhiteIPIPDTO whiteIPIPDTO) {
        Record record = dslContext.insertInto(T_WHITE_IP_IP,
            T_WHITE_IP_IP.RECORD_ID,
            T_WHITE_IP_IP.IP,
            T_WHITE_IP_IP.CLOUD_AREA_ID,
            T_WHITE_IP_IP.CREATOR,
            T_WHITE_IP_IP.CREATE_TIME,
            T_WHITE_IP_IP.LAST_MODIFY_USER,
            T_WHITE_IP_IP.LAST_MODIFY_TIME
        ).values(
            whiteIPIPDTO.getRecordId(),
            whiteIPIPDTO.getIp(),
            whiteIPIPDTO.getCloudAreaId(),
            whiteIPIPDTO.getCreator(),
            ULong.valueOf(whiteIPIPDTO.getCreateTime()),
            whiteIPIPDTO.getLastModifier(),
            ULong.valueOf(whiteIPIPDTO.getLastModifyTime())
        ).returning(T_WHITE_IP_IP.ID)
            .fetchOne();
        return record.get(T_WHITE_IP_IP.ID);
    }

    @Override
    public int deleteWhiteIPIPById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.ID.eq(id)
        ).execute();
    }

    @Override
    public int deleteWhiteIPIPByRecordId(DSLContext dslContext, Long recordId) {
        return dslContext.deleteFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.RECORD_ID.eq(recordId)
        ).execute();
    }

    @Override
    public WhiteIPIPDTO getWhiteIPIPById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return new WhiteIPIPDTO(
                record.getId(),
                record.getRecordId(),
                record.getCloudAreaId(),
                record.getIp(),
                record.getCreator(),
                record.getCreateTime().longValue(),
                record.getLastModifyUser(),
                record.getLastModifyTime().longValue()
            );
        }
    }

    @Override
    public List<WhiteIPIPDTO> getWhiteIPIPByRecordId(DSLContext dslContext, Long recordId) {
        val records = dslContext.selectFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.RECORD_ID.eq(recordId)
        ).fetch();
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream().map(record ->
            new WhiteIPIPDTO(
                record.getId(),
                record.getRecordId(),
                record.getCloudAreaId(),
                record.getIp(),
                record.getCreator(),
                record.getCreateTime().longValue(),
                record.getLastModifyUser(),
                record.getLastModifyTime().longValue()
            )
        ).collect(Collectors.toList());
    }

    @Override
    public int updateWhiteIPIPById(DSLContext dslContext, WhiteIPIPDTO whiteIPIPDTO) {
        return dslContext.update(T_WHITE_IP_IP)
            .set(T_WHITE_IP_IP.RECORD_ID, whiteIPIPDTO.getRecordId())
            .set(T_WHITE_IP_IP.IP, whiteIPIPDTO.getIp())
            .set(T_WHITE_IP_IP.CREATOR, whiteIPIPDTO.getCreator())
            .set(T_WHITE_IP_IP.CREATE_TIME, ULong.valueOf(whiteIPIPDTO.getCreateTime()))
            .set(T_WHITE_IP_IP.LAST_MODIFY_USER, whiteIPIPDTO.getLastModifier())
            .set(T_WHITE_IP_IP.LAST_MODIFY_TIME, ULong.valueOf(whiteIPIPDTO.getLastModifyTime()))
            .where(T_WHITE_IP_IP.ID.eq(whiteIPIPDTO.getId()))
            .execute();
    }

    @Override
    public List<WhiteIPIPDTO> listWhiteIPIPByRecordIds(DSLContext dslContext, List<Long> recordIdList) {
        val records =
            dslContext.selectFrom(T_WHITE_IP_IP).where(
                T_WHITE_IP_IP.RECORD_ID.in(recordIdList)
            ).fetch();
        if (records == null) {
            return new ArrayList<>();
        }
        return records.stream().map(record ->
            new WhiteIPIPDTO(
                record.getId(),
                record.getRecordId(),
                record.getCloudAreaId(),
                record.getIp(),
                record.getCreator(),
                record.getCreateTime().longValue(),
                record.getLastModifyUser(),
                record.getLastModifyTime().longValue()
            )
        ).collect(Collectors.toList());
    }
}
