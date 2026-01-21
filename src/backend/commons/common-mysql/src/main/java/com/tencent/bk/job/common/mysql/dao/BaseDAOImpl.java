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

package com.tencent.bk.job.common.mysql.dao;

import lombok.extern.slf4j.Slf4j;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.ResultQuery;
import org.jooq.SelectConditionStep;
import org.jooq.SelectLimitStep;
import org.jooq.conf.ParamType;
import org.slf4j.helpers.MessageFormatter;

import java.util.Collections;
import java.util.List;

/**
 * 抽取通用查询方法
 */
@Slf4j
public class BaseDAOImpl {
    public <RecordClazz extends Record, DTOClazz> List<DTOClazz> listPage(SelectLimitStep<RecordClazz> query,
                                                                          Integer start,
                                                                          Integer pageSize,
                                                                          RecordDTOConverter<RecordClazz, DTOClazz> converter) {
        Result<RecordClazz> records = null;
        if (start == null || start < 0) {
            start = 0;
        }
        if (pageSize == null || pageSize < 0) {
            pageSize = -1;
        }
        ResultQuery<RecordClazz> finalQuery = query;
        String sql = "";
        try {
            if (pageSize >= 0) {
                finalQuery = query.limit(start, pageSize);
            }
            sql = finalQuery.getSQL(ParamType.INLINED);
            log.debug("SQL={}", sql);
            records = finalQuery.fetch();
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "error SQL={}",
                sql
            ).getMessage();
            log.error(msg, e);
        }
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(converter::convert);
        }
    }

    public <RecordClazz extends Record, DTOClazz> DTOClazz fetchOne(SelectConditionStep<RecordClazz> query,
                                                                    RecordDTOConverter<RecordClazz, DTOClazz> converter) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("SQL={}", query.getSQL(ParamType.INLINED));
            }
            RecordClazz record = query.fetchOne();
            return record == null ? null : converter.convert(record);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "error SQL={}",
                query.getSQL(ParamType.INLINED)
            ).getMessage();
            log.error(msg, e);
            throw e;
        }
    }

}
