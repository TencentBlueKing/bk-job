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

package com.tencent.bk.job.backup.archive.dao.impl;

import com.tencent.bk.job.backup.archive.ArchiveException;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.execute.model.tables.StepInstanceFile;
import com.tencent.bk.job.execute.model.tables.StepInstanceScript;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
import org.jooq.Loader;
import org.jooq.LoaderError;
import org.jooq.LoaderFieldMapper;
import org.jooq.Table;
import org.jooq.TableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
public class JobInstanceColdDAOImpl implements JobInstanceColdDAO {

    private static final Logger ARCHIVE_FAILED_LOGGER = LoggerFactory.getLogger("ArchiveFailedLogger");

    /**
     * 写入归档库（冷 DB）时需要忽略的字段，key: 表名，value: 字段名集合。
     * <p>
     * 背景：热库 step_instance_script、step_instance_file 表新增了 account_var 字段，
     * 但归档库数据量过大，暂时无法执行加字段的 DDL，若写入该字段会导致整批数据插入失败。
     * 因此暂时在写入归档库时不插入 account_var 字段（该字段归档后丢失）。
     * <p>
     * TODO 后续若归档库补齐了 account_var 字段，需要从这里移除对应配置，恢复该字段的归档。
     */
    private static final Map<String, Set<String>> COLD_DB_IGNORED_COLUMNS = new HashMap<>();

    static {
        COLD_DB_IGNORED_COLUMNS.put(
            StepInstanceScript.STEP_INSTANCE_SCRIPT.getName(),
            Collections.singleton(StepInstanceScript.STEP_INSTANCE_SCRIPT.ACCOUNT_VAR.getName())
        );
        COLD_DB_IGNORED_COLUMNS.put(
            StepInstanceFile.STEP_INSTANCE_FILE.getName(),
            Collections.singleton(StepInstanceFile.STEP_INSTANCE_FILE.ACCOUNT_VAR.getName())
        );
    }

    private final DSLContext context;

    public JobInstanceColdDAOImpl(DSLContext coldDbDslContext) {
        log.info("Init JobInstanceColdDAOImpl.");
        this.context = coldDbDslContext;
    }

    @Override
    public Integer batchInsert(List<? extends TableRecord<?>> recordList, int bulkSize) throws ArchiveException {
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
                    .fields(buildColdDbFieldMapper(recordList.get(0).getTable()))
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
                    if (isDuplicateError(error)) {
                        log.info("LoadDataDuplicateError. table: {}, exception: {}, row: {}",
                            table, error.exception().getMessage(), error.row());
                    } else {
                        ARCHIVE_FAILED_LOGGER.error("Error while load {} data, exception: {}， error row: {}", table,
                            error.exception().getMessage(), error.row());
                    }
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
            throw new ArchiveException(errorMsg, e);
        } finally {
            log.info("Load data to {} done! success: {}, total: {}, inserted: {}, cost: {}ms", table, success,
                recordList.size(), successInsertedRecords, System.currentTimeMillis() - start);
        }

        return successInsertedRecords;
    }

    /**
     * 构造写入归档库的字段映射：与 fieldsCorresponding() 行为一致（按字段名对应到目标表），
     * 但会跳过 COLD_DB_IGNORED_COLUMNS 中配置的字段（返回 null 表示该字段不写入）。
     * 后续若归档库补齐了被忽略的字段，移除 COLD_DB_IGNORED_COLUMNS 中的配置即可恢复插入。
     *
     * @param table 目标表
     * @return 字段映射
     */
    private LoaderFieldMapper buildColdDbFieldMapper(Table<?> table) {
        Set<String> ignoredColumns = COLD_DB_IGNORED_COLUMNS.getOrDefault(table.getName(), Collections.emptySet());
        return ctx -> {
            if (ignoredColumns.contains(ctx.field().getName())) {
                return null;
            }
            return table.field(ctx.field());
        };
    }

    private boolean hasDuplicateError(List<LoaderError> errors) {
        // 通过 mysql 执行的错误消息判断是否是由于数据唯一性冲突引起的
        return errors.stream().anyMatch(this::isDuplicateError);
    }

    private boolean isDuplicateError(LoaderError error) {
        return error.exception().getMessage() != null
            && error.exception().getMessage().contains("Duplicate entry");
    }

    private int insertSingle(List<? extends TableRecord<?>> recordList) throws IOException {
        int successInsertedRecords;
        String table = recordList.get(0).getTable().getName();
        try {
            Loader<?> loader =
                context.loadInto(recordList.get(0).getTable())
                    .onDuplicateKeyIgnore()
                    .loadRecords(recordList)
                    .fields(buildColdDbFieldMapper(recordList.get(0).getTable()))
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
