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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.mysql.dao.BaseDAOImpl;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceShareDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.tables.FileSource;
import com.tencent.bk.job.file_gateway.model.tables.FileSourceShare;
import com.tencent.bk.job.file_gateway.util.JooqTypeUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.TableField;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 文件源DAO操作基础类，封装部分公共逻辑
 */
@Slf4j
public class BaseFileSourceDAOImpl extends BaseDAOImpl {

    protected final FileSource defaultTable = FileSource.FILE_SOURCE;
    protected final FileSourceShare tableFileSourceShare = FileSourceShare.FILE_SOURCE_SHARE;
    private final DSLContext dslContext;
    protected final FileSourceShareDAO fileSourceShareDAO;
    protected final FileSourceTypeDAO fileSourceTypeDAO;
    protected final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.ID,
        defaultTable.TENANT_ID,
        defaultTable.APP_ID,
        defaultTable.CODE,
        defaultTable.ALIAS,
        defaultTable.STATUS,
        defaultTable.TYPE,
        defaultTable.ENDPOINT_DOMAIN,
        defaultTable.CUSTOM_INFO,
        defaultTable.PUBLIC,
        defaultTable.SHARE_TO_ALL_APP,
        defaultTable.CREDENTIAL_ID,
        defaultTable.FILE_PREFIX,
        defaultTable.WORKER_SELECT_SCOPE,
        defaultTable.WORKER_SELECT_MODE,
        defaultTable.WORKER_ID,
        defaultTable.ENABLE,
        defaultTable.CREATOR,
        defaultTable.CREATE_TIME,
        defaultTable.LAST_MODIFY_USER,
        defaultTable.LAST_MODIFY_TIME
    };

    public BaseFileSourceDAOImpl(DSLContext dslContext,
                                 FileSourceShareDAO fileSourceShareDAO,
                                 FileSourceTypeDAO fileSourceTypeDAO) {
        this.dslContext = dslContext;
        this.fileSourceShareDAO = fileSourceShareDAO;
        this.fileSourceTypeDAO = fileSourceTypeDAO;
    }

    protected List<FileSourceDTO> listFileSourceByConditions(Collection<Condition> conditions,
                                                             Integer start,
                                                             Integer pageSize) {
        val query = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    protected List<FileSourceDTO> listFileSourceByShareConditions(Collection<Condition> conditions,
                                                                  Integer start,
                                                                  Integer pageSize) {
        val query = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .join(tableFileSourceShare)
            .on(defaultTable.ID.eq(tableFileSourceShare.FILE_SOURCE_ID))
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    protected FileSourceDTO convertRecordToDto(Record record) {
        Integer id = record.get(defaultTable.ID);
        Long appId = record.get(defaultTable.APP_ID);
        FileSourceDTO fileSourceDTO = new FileSourceDTO();
        fileSourceDTO.setId(id);
        fileSourceDTO.setAppId(appId);
        fileSourceDTO.setCode(record.get(defaultTable.CODE));
        fileSourceDTO.setAlias(record.get(defaultTable.ALIAS));
        fileSourceDTO.setStatus(JooqTypeUtil.convertToInt(record.get(defaultTable.STATUS)));
        fileSourceDTO.setFileSourceType(fileSourceTypeDAO.getByCode(record.get(defaultTable.TYPE)));
        fileSourceDTO.setPublicFlag(record.get(defaultTable.PUBLIC));
        fileSourceDTO.setSharedAppIdList(fileSourceShareDAO.getSharedAppIdList(appId, id));
        fileSourceDTO.setShareToAllApp(record.get(defaultTable.SHARE_TO_ALL_APP));
        fileSourceDTO.setCredentialId(record.get(defaultTable.CREDENTIAL_ID));
        fileSourceDTO.setFilePrefix(record.get(defaultTable.FILE_PREFIX));
        fileSourceDTO.setWorkerSelectScope(record.get(defaultTable.WORKER_SELECT_SCOPE));
        fileSourceDTO.setWorkerSelectMode(record.get(defaultTable.WORKER_SELECT_MODE));
        fileSourceDTO.setWorkerId(record.get(defaultTable.WORKER_ID));
        fileSourceDTO.setEnable(record.get(defaultTable.ENABLE));
        fileSourceDTO.setCreator(record.get(defaultTable.CREATOR));
        fileSourceDTO.setCreateTime(record.get(defaultTable.CREATE_TIME));
        fileSourceDTO.setLastModifyUser(record.get(defaultTable.LAST_MODIFY_USER));
        fileSourceDTO.setLastModifyTime(record.get(defaultTable.LAST_MODIFY_TIME));
        String customInfoStr = record.get(defaultTable.CUSTOM_INFO);
        if (StringUtils.isNotBlank(customInfoStr)) {
            fileSourceDTO.setFileSourceInfoMap(JsonUtils.fromJson(customInfoStr, new TypeReference<Map<String,
                Object>>() {
            }));
        }
        return fileSourceDTO;
    }
}
