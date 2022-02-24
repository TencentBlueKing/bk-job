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

package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Loader;
import org.jooq.LoaderError;
import org.jooq.TableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @since 17/3/2021 15:46
 */
@Slf4j
public class ExecuteArchiveDAOImpl implements ExecuteArchiveDAO {

    private static final Logger ARCHIVE_FAILED_LOGGER = LoggerFactory.getLogger("ArchiveFailedLogger");

    private final DSLContext context;

    public ExecuteArchiveDAOImpl(DSLContext context) {
        log.info("Init ExecuteArchiveDAO.");
        this.context = context;
    }

    @Override
    public Integer batchInsert(List<Field<?>> fieldList, List<? extends TableRecord<?>> recordList, int bulkSize)
        throws IOException {
        long start = System.currentTimeMillis();
        int executeResult = 0;
        String table = recordList.get(0).getTable().getName();
        boolean success = true;
        try {
            Loader<?> loader =
                context.loadInto(recordList.get(0).getTable())
                    .onErrorIgnore()
                    .bulkAfter(bulkSize)
                    .loadRecords(recordList)
                    .fields(fieldList)
                    .execute();
            executeResult = loader.stored();
            log.info("Load {} data result|executed|{}|processed|{}|stored|{}|ignored|{}|errors|{}", table,
                loader.executed(), loader.processed(), loader.stored(), loader.ignored(), loader.errors().size());
            if (CollectionUtils.isNotEmpty(loader.errors())) {
                for (LoaderError error : loader.errors()) {
                    ARCHIVE_FAILED_LOGGER.error("Error while load {} data, error row: {}, exception: {}", table,
                        error.row(), error.exception().getMessage());
                }
            }
        } catch (IOException e) {
            String errorMsg = String.format("Error while batch loading %s data!", table);
            log.error(errorMsg, e);
            success = false;
            throw e;
        } finally {
            log.info("Batch insert to {} done! success: {}, total: {}, inserted: {}, cost: {}ms", table, success,
                recordList.size(), executeResult, System.currentTimeMillis() - start);
        }

        return executeResult;
    }
}
