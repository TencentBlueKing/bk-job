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

import com.tencent.bk.job.manage.dao.notify.NotifyBlackUserInfoDAO;
import com.tencent.bk.job.manage.model.dto.notify.NotifyBlackUserInfoDTO;
import com.tencent.bk.job.manage.model.tables.NotifyBlackUserInfo;
import com.tencent.bk.job.manage.model.web.vo.notify.NotifyBlackUserInfoVO;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyBlackUserInfoDAOImpl implements NotifyBlackUserInfoDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotifyBlackUserInfoDAOImpl.class);
    private static final NotifyBlackUserInfo T_ESB_USER_INFO = NotifyBlackUserInfo.NOTIFY_BLACK_USER_INFO;
    private static final NotifyBlackUserInfo defaultTable = T_ESB_USER_INFO;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.ID,
        defaultTable.TENANT_ID,
        defaultTable.USERNAME,
        defaultTable.DISPLAY_NAME,
        defaultTable.CREATOR,
        defaultTable.LAST_MODIFY_TIME
    };

    private final DSLContext dslContext;

    @Autowired
    public NotifyBlackUserInfoDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public Long insertNotifyBlackUserInfo(NotifyBlackUserInfoDTO notifyBlackUserInfoDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.TENANT_ID,
            defaultTable.USERNAME,
            defaultTable.DISPLAY_NAME,
            defaultTable.CREATOR,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            notifyBlackUserInfoDTO.getTenantId(),
            notifyBlackUserInfoDTO.getUsername(),
            notifyBlackUserInfoDTO.getDisplayName(),
            notifyBlackUserInfoDTO.getCreator(),
            ULong.valueOf(notifyBlackUserInfoDTO.getLastModifyTime())
        ).returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            Record record = query.fetchOne();
            assert record != null;
            return record.get(defaultTable.ID);
        } catch (Exception e) {
            logger.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteAllNotifyBlackUser(String tenantId) {
        return dslContext.deleteFrom(defaultTable).where(defaultTable.TENANT_ID.eq(tenantId)).execute();
    }

    @Override
    public List<NotifyBlackUserInfoDTO> listNotifyBlackUserInfo(String tenantId) {
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(defaultTable.TENANT_ID.eq(tenantId))
            .fetch();
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(record -> new NotifyBlackUserInfoDTO(
                record.get(defaultTable.ID),
                record.get(defaultTable.TENANT_ID),
                record.get(defaultTable.USERNAME),
                record.get(defaultTable.DISPLAY_NAME),
                record.get(defaultTable.CREATOR),
                record.get(defaultTable.LAST_MODIFY_TIME).longValue()
            ));
        }
    }

    @Override
    public List<NotifyBlackUserInfoVO> listNotifyBlackUserInfo(String tenantId, Integer start, Integer limit) {
        if (null == start) start = 0;
        if (null == limit) limit = -1;
        val baseQuery = dslContext.select(ALL_FIELDS).from(defaultTable).where(defaultTable.TENANT_ID.eq(tenantId));
        Result<Record> records;
        if (start > 0 && limit > 0) {
            records = baseQuery.offset(start).limit(limit).fetch();
        } else if (start > 0) {
            records = baseQuery.offset(start).fetch();
        } else {
            records = baseQuery.fetch();
        }
        if (records.isEmpty()) {
            return new ArrayList<>();
        } else {
            return records.map(record -> new NotifyBlackUserInfoVO(
                record.get(defaultTable.ID),
                record.get(defaultTable.USERNAME),
                record.get(defaultTable.DISPLAY_NAME),
                record.get(defaultTable.CREATOR),
                record.get(defaultTable.LAST_MODIFY_TIME).longValue()
            ));
        }
    }
}
