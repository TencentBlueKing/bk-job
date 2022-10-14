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
import org.jooq.generated.tables.WhiteIpIp;
import org.jooq.generated.tables.records.WhiteIpIpRecord;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class WhiteIPIPDAOImpl implements WhiteIPIPDAO {
    private static final WhiteIpIp T_WHITE_IP_IP = WhiteIpIp.WHITE_IP_IP;

    private final DSLContext dslContext;

    @Autowired
    public WhiteIPIPDAOImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void insertWhiteIPIP(WhiteIPIPDTO whiteIPIPDTO) {
        dslContext.insertInto(T_WHITE_IP_IP,
            T_WHITE_IP_IP.RECORD_ID,
            T_WHITE_IP_IP.HOST_ID,
            T_WHITE_IP_IP.IP,
            T_WHITE_IP_IP.IP_V6,
            T_WHITE_IP_IP.CLOUD_AREA_ID,
            T_WHITE_IP_IP.CREATOR,
            T_WHITE_IP_IP.CREATE_TIME,
            T_WHITE_IP_IP.LAST_MODIFY_USER,
            T_WHITE_IP_IP.LAST_MODIFY_TIME
        ).values(
            whiteIPIPDTO.getRecordId(),
            whiteIPIPDTO.getHostId(),
            whiteIPIPDTO.getIp(),
            whiteIPIPDTO.getIpv6(),
            whiteIPIPDTO.getCloudAreaId(),
            whiteIPIPDTO.getCreator(),
            ULong.valueOf(whiteIPIPDTO.getCreateTime()),
            whiteIPIPDTO.getLastModifier(),
            ULong.valueOf(whiteIPIPDTO.getLastModifyTime())
        ).returning(T_WHITE_IP_IP.ID)
            .fetchOne();
    }

    @Override
    public int deleteWhiteIPIPByRecordId(Long recordId) {
        return dslContext.deleteFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.RECORD_ID.eq(recordId)
        ).execute();
    }

    @Override
    public List<WhiteIPIPDTO> getWhiteIPIPByRecordId(Long recordId) {
        val records = dslContext.selectFrom(T_WHITE_IP_IP).where(
            T_WHITE_IP_IP.RECORD_ID.eq(recordId)
        ).fetch();
        return records.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<WhiteIPIPDTO> listWhiteIPIPByRecordIds(List<Long> recordIdList) {
        val records = dslContext.selectFrom(T_WHITE_IP_IP)
            .where(T_WHITE_IP_IP.RECORD_ID.in(recordIdList))
            .fetch();
        return records.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public List<WhiteIPIPDTO> listWhiteIPIPWithNullHostId(int start, int limit) {
        val records = dslContext.selectFrom(T_WHITE_IP_IP)
            .where(T_WHITE_IP_IP.HOST_ID.isNull())
            .limit(start, limit)
            .fetch();
        return records.stream().map(this::convert).collect(Collectors.toList());
    }

    @Override
    public int updateHostIdById(Long id, Long hostId) {
        return dslContext.update(T_WHITE_IP_IP)
            .set(T_WHITE_IP_IP.HOST_ID, hostId)
            .where(T_WHITE_IP_IP.ID.eq(id))
            .execute();
    }

    private WhiteIPIPDTO convert(WhiteIpIpRecord record) {
        return new WhiteIPIPDTO(
            record.getId(),
            record.getRecordId(),
            record.getCloudAreaId(),
            record.getHostId(),
            record.getIp(),
            record.getIpV6(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        );
    }
}
