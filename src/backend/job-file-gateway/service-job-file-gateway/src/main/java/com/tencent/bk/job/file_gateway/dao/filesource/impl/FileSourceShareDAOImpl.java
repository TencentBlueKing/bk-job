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

package com.tencent.bk.job.file_gateway.dao.filesource.impl;

import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceShareDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceShare;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.BatchBindStep;
import org.jooq.DSLContext;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class FileSourceShareDAOImpl implements FileSourceShareDAO {

    private static final FileSourceShare tableFileSourceShare = FileSourceShare.FILE_SOURCE_SHARE;
    private final DSLContext dslContext;

    @Autowired
    public FileSourceShareDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public List<Long> getSharedAppIdList(Long appId, Integer fileSourceId) {
        val records = dslContext.select(tableFileSourceShare.APP_ID)
            .from(tableFileSourceShare)
            .where(tableFileSourceShare.FILE_SOURCE_ID.eq(fileSourceId))
            .and(tableFileSourceShare.APP_ID.notEqual(appId))
            .fetch();
        return records.map(record -> record.get(tableFileSourceShare.APP_ID));
    }

    @Override
    public void saveFileSourceShareInfo(Integer fileSourceId, FileSourceDTO fileSourceDTO) {
        String sql = null;
        try {
            List<Long> sharedAppIdList = fileSourceDTO.getSharedAppIdList();
            if (sharedAppIdList == null) {
                return;
            }
            // 删除旧数据
            val deleteFileSourceShareQuery = dslContext.deleteFrom(tableFileSourceShare)
                .where(tableFileSourceShare.FILE_SOURCE_ID.eq(fileSourceId));
            sql = deleteFileSourceShareQuery.getSQL(ParamType.INLINED);
            deleteFileSourceShareQuery.execute();
            // 插入业务私有Worker关系
            var insertFileSourceShareQuery = dslContext.insertInto(tableFileSourceShare,
                tableFileSourceShare.FILE_SOURCE_ID,
                tableFileSourceShare.APP_ID
            ).values(fileSourceId, fileSourceDTO.getAppId());
            sql = insertFileSourceShareQuery.getSQL(ParamType.INLINED);
            insertFileSourceShareQuery.execute();
            sharedAppIdList.remove(fileSourceDTO.getAppId());
            // 插入新数据
            if (fileSourceDTO.getPublicFlag()) {
                // 共享Worker
                if (!fileSourceDTO.getShareToAllApp() && !sharedAppIdList.isEmpty()) {
                    insertFileSourceShareQuery = dslContext.insertInto(tableFileSourceShare,
                        tableFileSourceShare.FILE_SOURCE_ID,
                        tableFileSourceShare.APP_ID
                    ).values(
                        (Integer) null,
                        null
                    );
                    BatchBindStep batchQuery = dslContext.batch(insertFileSourceShareQuery);
                    for (Long appId : sharedAppIdList) {
                        batchQuery = batchQuery.bind(
                            fileSourceId,
                            appId
                        );
                    }
                    sql = insertFileSourceShareQuery.getSQL(ParamType.INLINED);
                    int[] results = batchQuery.execute();
                    int affectedRowNum = 0;
                    for (int result : results) {
                        affectedRowNum += result;
                    }
                    log.info("{} file_source_share records inserted", affectedRowNum);
                }
            }
        } catch (Exception e) {
            log.error("error SQL={}", sql);
            throw e;
        }
    }
}
