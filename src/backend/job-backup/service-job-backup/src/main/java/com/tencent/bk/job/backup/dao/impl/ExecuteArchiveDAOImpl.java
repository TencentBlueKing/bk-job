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

package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.dao.ExecuteArchiveDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
import org.jooq.Loader;
import org.jooq.LoaderError;
import org.jooq.TableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;


@Slf4j
public class ExecuteArchiveDAOImpl implements ExecuteArchiveDAO {

    private static final Logger ARCHIVE_FAILED_LOGGER = LoggerFactory.getLogger("ArchiveFailedLogger");

    private final DSLContext context;

    public ExecuteArchiveDAOImpl(DSLContext context) {
        log.info("Init ExecuteArchiveDAO.");
        this.context = context;
    }

    @Override
    public Integer batchInsert(List<? extends TableRecord<?>> recordList, int bulkSize) throws IOException {
        long start = System.currentTimeMillis();
        int successInsertedRecords = 0;
        String table = recordList.get(0).getTable().getName();
        boolean success = true;
        try {
            Loader<?> loader =
                context.loadInto(recordList.get(0).getTable())
                    // 由于这里是批量写入，jooq 不允许使用 onDuplicateKeyIgnore/onDuplicateKeyUpdate.
                    // 否则会报错"Cannot apply bulk loading with onDuplicateKey flags"
                    // 所以这里暂时使用 onDuplicateKeyError 错误处理方式，等后续流程进一步判断是否是主键冲突错误
                    // issue 参考：https://github.com/jOOQ/jOOQ/issues/12740
                    .onDuplicateKeyError()
                    .bulkAfter(bulkSize)
                    .loadRecords(recordList)
                    .fieldsCorresponding()
                    .execute();
            successInsertedRecords = loader.stored();
            String bulkInsertResult = successInsertedRecords == recordList.size() ? "success" : "fail";
            log.info(
                "InsertBulk: Load {} data|result|{}|executed|{}|processed|{}|stored|{}|ignored|{}|errors|{}",
                table,
                bulkInsertResult,
                loader.executed(),
                loader.processed(),
                loader.stored(),
                loader.ignored(),
                loader.errors().size()
            );
            if (CollectionUtils.isNotEmpty(loader.errors())) {
                for (LoaderError error : loader.errors()) {
                    ARCHIVE_FAILED_LOGGER.error("Error while load {} data, exception: {}， error row: {}", table,
                        error.exception().getMessage(), error.row());
                }
                if (hasDuplicateError(loader.errors())) {
                    // 如果存在主键冲突的数据，尝试每一条记录单独插入，就可以使用 onDuplicateKeyIgnore 错误处理方式
                    successInsertedRecords = insertSingle(recordList);
                }
            }
        } catch (IOException e) {
            String errorMsg = String.format("Error while loading %s data!", table);
            log.error(errorMsg, e);
            success = false;
            throw e;
        } finally {
            log.info("Load data to {} done! success: {}, total: {}, inserted: {}, cost: {}ms", table, success,
                recordList.size(), successInsertedRecords, System.currentTimeMillis() - start);
        }

        return successInsertedRecords;
    }

    private boolean hasDuplicateError(List<LoaderError> errors) {
        // 通过 mysql 执行的错误消息判断是否是由于数据唯一性冲突引起的
        return errors.stream().anyMatch(
            error -> error.exception().getMessage() != null
                && error.exception().getMessage().contains("Duplicate entry"));
    }

    private int insertSingle(List<? extends TableRecord<?>> recordList) throws IOException {
        int successInsertedRecords;
        String table = recordList.get(0).getTable().getName();
        try {
            Loader<?> loader =
                context.loadInto(recordList.get(0).getTable())
                    .onDuplicateKeyIgnore()
                    .loadRecords(recordList)
                    .fieldsCorresponding()
                    .execute();
            successInsertedRecords = loader.stored() + loader.ignored();
            String bulkInsertResult = successInsertedRecords == recordList.size() ? "success" : "fail";
            log.info(
                "InsertSingle: Load {} data|result|{}|executed|{}|processed|{}|stored|{}|ignored|{}|errors|{}",
                table,
                bulkInsertResult,
                loader.executed(),
                loader.processed(),
                loader.stored(),
                loader.ignored(),
                loader.errors().size()
            );
            if (CollectionUtils.isNotEmpty(loader.errors())) {
                for (LoaderError error : loader.errors()) {
                    ARCHIVE_FAILED_LOGGER.error("InsertSingle: Error while load {} data, exception: {}, error row: {}",
                        table, error.row(), error.exception().getMessage());
                }
            }
        } catch (IOException e) {
            String errorMsg = String.format("Error while loading %s data!", table);
            log.error(errorMsg, e);
            throw e;
        }
        return successInsertedRecords;
    }
}
