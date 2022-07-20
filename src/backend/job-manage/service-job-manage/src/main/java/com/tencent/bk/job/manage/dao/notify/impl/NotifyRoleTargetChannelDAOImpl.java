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

package com.tencent.bk.job.manage.dao.notify.impl;

import com.tencent.bk.job.common.RequestIdLogger;
import com.tencent.bk.job.common.util.SimpleRequestIdLogger;
import com.tencent.bk.job.manage.dao.notify.NotifyRoleTargetChannelDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyRoleTargetChannelDTO;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.NotifyRoleTargetChannel;
import org.jooq.types.ULong;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyRoleTargetChannelDAOImpl implements NotifyRoleTargetChannelDAO {
    private static final RequestIdLogger logger =
        new SimpleRequestIdLogger(LoggerFactory.getLogger(NotifyRoleTargetChannelDAOImpl.class));
    private static final NotifyRoleTargetChannel T_NOTIFY_ROLE_TARGET_CHANNEL =
        NotifyRoleTargetChannel.NOTIFY_ROLE_TARGET_CHANNEL;

    private final DSLContext dslContext;

    @Autowired
    public NotifyRoleTargetChannelDAOImpl(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public Long insert(NotifyRoleTargetChannelDTO notifyRoleTargetChannelDTO) {
        val query = dslContext.insertInto(T_NOTIFY_ROLE_TARGET_CHANNEL,
            T_NOTIFY_ROLE_TARGET_CHANNEL.ROLE_TARGET_ID,
            T_NOTIFY_ROLE_TARGET_CHANNEL.CHANNEL,
            T_NOTIFY_ROLE_TARGET_CHANNEL.CREATOR,
            T_NOTIFY_ROLE_TARGET_CHANNEL.CREATE_TIME,
            T_NOTIFY_ROLE_TARGET_CHANNEL.LAST_MODIFY_USER,
            T_NOTIFY_ROLE_TARGET_CHANNEL.LAST_MODIFY_TIME
        ).values(
            notifyRoleTargetChannelDTO.getRoleTargetId(),
            notifyRoleTargetChannelDTO.getChannel(),
            notifyRoleTargetChannelDTO.getCreator(),
            ULong.valueOf(notifyRoleTargetChannelDTO.getCreateTime()),
            notifyRoleTargetChannelDTO.getLastModifier(),
            ULong.valueOf(notifyRoleTargetChannelDTO.getLastModifyTime())
        ).returning(T_NOTIFY_ROLE_TARGET_CHANNEL.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            Record record = query.fetchOne();
            assert record != null;
            return record.get(T_NOTIFY_ROLE_TARGET_CHANNEL.ID);
        } catch (Exception e) {
            logger.errorWithRequestId(sql);
            throw e;
        }
    }

    @Override
    public int deleteByRoleTargetId(DSLContext dslContext, Long roleTargetId) {
        //1.无从表
        //2.直接删主表
        return dslContext.deleteFrom(T_NOTIFY_ROLE_TARGET_CHANNEL).where(
            T_NOTIFY_ROLE_TARGET_CHANNEL.ROLE_TARGET_ID.eq(roleTargetId)
        ).execute();
    }

    @Override
    public List<NotifyRoleTargetChannelDTO> listByRoleTargetId(DSLContext dslContext,
                                                               Long roleTargetId) {
        val records = dslContext.selectFrom(T_NOTIFY_ROLE_TARGET_CHANNEL).where(
            T_NOTIFY_ROLE_TARGET_CHANNEL.ROLE_TARGET_ID.eq(roleTargetId)
        ).fetch();
        return records.map(record -> new NotifyRoleTargetChannelDTO(
            record.getId(),
            record.getRoleTargetId(),
            record.getChannel(),
            record.getCreator(),
            record.getCreateTime().longValue(),
            record.getLastModifyUser(),
            record.getLastModifyTime().longValue()
        ));
    }
}
