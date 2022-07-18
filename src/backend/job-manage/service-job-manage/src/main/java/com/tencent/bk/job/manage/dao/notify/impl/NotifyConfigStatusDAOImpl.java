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

import com.tencent.bk.job.manage.dao.notify.NotifyConfigStatusDAO;
import lombok.val;
import org.joda.time.DateTimeUtils;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.NotifyConfigStatus;
import org.jooq.types.ULong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/**
 * @Description
 * @Date 2020/1/2
 * @Version 1.0
 */
@Repository
public class NotifyConfigStatusDAOImpl implements NotifyConfigStatusDAO {

    private static final Logger logger = LoggerFactory.getLogger(NotifyConfigStatusDAOImpl.class);
    private static final NotifyConfigStatus T_NOTIFY_CONFIG_STATUS = NotifyConfigStatus.NOTIFY_CONFIG_STATUS;
    private static final NotifyConfigStatus defaultTable = T_NOTIFY_CONFIG_STATUS;

    @Override
    public int insertNotifyConfigStatus(DSLContext dslContext, String userName, Long appId) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.USERNAME,
            defaultTable.APP_ID,
            defaultTable.LAST_MODIFY_TIME
        ).values(
            userName,
            appId,
            ULong.valueOf(DateTimeUtils.currentTimeMillis())
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
    public boolean exist(DSLContext dslContext, String userName, Long appId) {
        val records = dslContext.selectFrom(defaultTable)
            .where(defaultTable.USERNAME.eq(userName))
            .and(defaultTable.APP_ID.eq(appId))
            .fetch();
        return records.size() > 0;
    }
}
