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
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
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
    /**
     * {@link #convertRecordToBasicInfoDto} 所需的最小字段集，避免为了基本信息去拉 custom_info 这类大字段
     */
    protected final TableField<?, ?>[] BASIC_INFO_FIELDS = {
        defaultTable.ID,
        defaultTable.APP_ID,
        defaultTable.CODE,
        defaultTable.ALIAS,
        defaultTable.ENABLE
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

    /**
     * 文件源对指定业务可见的判定条件：命中本业务那行共享记录（归属业务自己那行在保存时无条件写入，
     * 故这一支同时覆盖「归属」与「显式共享」），或者「公共 + 全业务共享」。
     * <p>
     * 显式共享这一支必须叠加 PUBLIC = true —— 私有文件源不对其他业务开放，
     * 即使 file_source_share 里残留了历史共享记录；
     * 但归属业务自身必须放行，所以是 {@code APP_ID = appId OR PUBLIC = true} 而不是只判 PUBLIC。
     * <p>
     * 归属这一支刻意保持用 share 表判定、不改成直接判 {@code file.APP_ID = appId}：
     * 查询与 share 表是 join 关系，直接判归属会让归属业务的每一行共享记录都命中，
     * 使文件源在列表里重复出现（列表未去重，而计数用的是 countDistinct）。
     * <p>
     * 只适用于与 {@link #tableFileSourceShare} join 之后的查询。
     */
    protected Condition genAppScopeCondition(Long appId) {
        return tableFileSourceShare.APP_ID.eq(appId)
            .and(defaultTable.APP_ID.eq(appId).or(defaultTable.PUBLIC.eq(true)))
            .or(defaultTable.PUBLIC.eq(true).and(defaultTable.SHARE_TO_ALL_APP.eq(true)));
    }

    protected FileSourceBasicInfoDTO convertRecordToBasicInfoDto(Record record) {
        FileSourceBasicInfoDTO fileSourceBasicInfoDTO = new FileSourceBasicInfoDTO();
        fileSourceBasicInfoDTO.setId(record.get(defaultTable.ID));
        fileSourceBasicInfoDTO.setAppId(record.get(defaultTable.APP_ID));
        fileSourceBasicInfoDTO.setCode(record.get(defaultTable.CODE));
        fileSourceBasicInfoDTO.setAlias(record.get(defaultTable.ALIAS));
        fileSourceBasicInfoDTO.setEnable(record.get(defaultTable.ENABLE));
        return fileSourceBasicInfoDTO;
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
