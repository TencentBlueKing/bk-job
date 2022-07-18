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

import com.tencent.bk.job.manage.dao.notify.NotifyTemplateDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyTemplateDTO;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record13;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.NotifyTemplate;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyTemplateDAOImpl implements NotifyTemplateDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotifyTemplateDAOImpl.class);
    private static final NotifyTemplate T_NOTIFY_TEMPLATE = NotifyTemplate.NOTIFY_TEMPLATE;
    private static final NotifyTemplate defaultTable = T_NOTIFY_TEMPLATE;

    @Override
    public int insertNotifyTemplate(DSLContext dslContext, NotifyTemplateDTO notifyTemplateDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.CHANNEL,
            defaultTable.TITLE,
            defaultTable.CONTENT,
            defaultTable.TITLE_EN,
            defaultTable.CONTENT_EN,
            defaultTable.IS_DEFAULT,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            notifyTemplateDTO.getCode(),
            notifyTemplateDTO.getName(),
            notifyTemplateDTO.getChannel(),
            notifyTemplateDTO.getTitle(),
            notifyTemplateDTO.getContent(),
            notifyTemplateDTO.getTitleEn(),
            notifyTemplateDTO.getContentEn(),
            notifyTemplateDTO.isDefault(),
            notifyTemplateDTO.getCreator(),
            ULong.valueOf(notifyTemplateDTO.getCreateTime()),
            notifyTemplateDTO.getLastModifyUser(),
            ULong.valueOf(notifyTemplateDTO.getLastModifyTime())
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
    public int updateNotifyTemplateById(DSLContext dslContext, NotifyTemplateDTO notifyTemplateDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.CODE, notifyTemplateDTO.getCode())
            .set(defaultTable.NAME, notifyTemplateDTO.getName())
            .set(defaultTable.CHANNEL, notifyTemplateDTO.getChannel())
            .set(defaultTable.TITLE, notifyTemplateDTO.getTitle())
            .set(defaultTable.CONTENT, notifyTemplateDTO.getContent())
            .set(defaultTable.TITLE_EN, notifyTemplateDTO.getTitleEn())
            .set(defaultTable.CONTENT_EN, notifyTemplateDTO.getContentEn())
            .set(defaultTable.LAST_MODIFY_USER, notifyTemplateDTO.getLastModifyUser())
            .set(defaultTable.LAST_MODIFY_TIME, ULong.valueOf(notifyTemplateDTO.getLastModifyTime()))
            .where(defaultTable.ID.eq(notifyTemplateDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            logger.error(sql);
            throw e;
        }
    }

    private NotifyTemplateDTO convert(Record13<Integer, String, String, String, String, String, String, String,
        Boolean, String, ULong, String, ULong> record) {
        return new NotifyTemplateDTO(
            record.get(defaultTable.ID),
            record.get(defaultTable.CODE),
            record.get(defaultTable.NAME),
            record.get(defaultTable.CHANNEL),
            record.get(defaultTable.TITLE),
            record.get(defaultTable.CONTENT),
            record.get(defaultTable.TITLE_EN),
            record.get(defaultTable.CONTENT_EN),
            record.get(defaultTable.IS_DEFAULT),
            record.get(defaultTable.CREATOR),
            record.get(defaultTable.CREATE_TIME).longValue(),
            record.get(defaultTable.LAST_MODIFY_USER),
            record.get(defaultTable.LAST_MODIFY_TIME).longValue()
        );
    }

    @Override
    public NotifyTemplateDTO getNotifyTemplate(DSLContext dslContext, String channelCode, String messageTypeCode,
                                               boolean isDefault) {
        val record = dslContext.select(
            defaultTable.ID,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.CHANNEL,
            defaultTable.TITLE,
            defaultTable.CONTENT,
            defaultTable.TITLE_EN,
            defaultTable.CONTENT_EN,
            defaultTable.IS_DEFAULT,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable)
            .where(defaultTable.CHANNEL.eq(channelCode))
            .and(defaultTable.CODE.eq(messageTypeCode))
            .and(defaultTable.IS_DEFAULT.eq(isDefault))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convert(record);
        }
    }

    @Override
    public List<NotifyTemplateDTO> listNotifyTemplateByCode(DSLContext dslContext, String code) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.CODE.eq(code));
        return listNotifyTemplateByConditions(dslContext, conditions);
    }

    @Override
    public boolean existsNotifyTemplate(DSLContext dslContext, String channelCode, String messageTypeCode,
                                        boolean isDefault) {
        Integer count = dslContext.selectCount().from(defaultTable)
            .where(defaultTable.CHANNEL.eq(channelCode))
            .and(defaultTable.CODE.eq(messageTypeCode))
            .and(defaultTable.IS_DEFAULT.eq(isDefault))
            .fetchOne(0, Integer.class);
        assert count != null;
        return count > 0;
    }

    private List<NotifyTemplateDTO> listNotifyTemplateByConditions(DSLContext dslContext, List<Condition> conditions) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        val baseQuery = dslContext.select(
            defaultTable.ID,
            defaultTable.CODE,
            defaultTable.NAME,
            defaultTable.CHANNEL,
            defaultTable.TITLE,
            defaultTable.CONTENT,
            defaultTable.TITLE_EN,
            defaultTable.CONTENT_EN,
            defaultTable.IS_DEFAULT,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        )
            .from(defaultTable)
            .where(conditions);
        Result<Record13<Integer, String, String, String, String, String, String, String, Boolean, String, ULong,
            String, ULong>> records;
        records = baseQuery.fetch();
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(this::convert);
        }
    }
}
