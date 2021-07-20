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

package com.tencent.bk.job.file_gateway.dao.filesource.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.util.JooqTypeUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.FileSource;
import org.jooq.generated.tables.FileSourceShare;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class FileSourceDAOImpl extends BaseDAOImpl implements FileSourceDAO {

    private static final FileSource defaultTable = FileSource.FILE_SOURCE;
    private static final FileSourceShare tableFileSourceShare = FileSourceShare.FILE_SOURCE_SHARE;
    private final DSLContext defaultContext;
    private final FileSourceTypeDAO fileSourceTypeDAO;

    @Autowired
    public FileSourceDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext defaultContext,
                             FileSourceTypeDAO fileSourceTypeDAO) {
        this.defaultContext = defaultContext;
        this.fileSourceTypeDAO = fileSourceTypeDAO;
    }

    private void setDefaultValue(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO.getStatus() == null) {
            fileSourceDTO.setStatus(0);
        }
    }

    @Override
    public Integer insertFileSource(DSLContext dslContext, FileSourceDTO fileSourceDTO) {
        setDefaultValue(fileSourceDTO);
        val insertFileSourceQuery = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.CODE,
            defaultTable.ALIAS,
            defaultTable.STATUS,
            defaultTable.TYPE,
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
        ).values(
            (Integer) null,
            fileSourceDTO.getAppId(),
            fileSourceDTO.getCode(),
            fileSourceDTO.getAlias(),
            JooqTypeUtil.convertToByte(fileSourceDTO.getStatus()),
            fileSourceDTO.getFileSourceType().getCode(),
            JsonUtils.toJson(fileSourceDTO.getFileSourceInfoMap()),
            fileSourceDTO.getPublicFlag(),
            fileSourceDTO.getShareToAllApp(),
            fileSourceDTO.getCredentialId(),
            fileSourceDTO.getFilePrefix(),
            fileSourceDTO.getWorkerSelectScope(),
            fileSourceDTO.getWorkerSelectMode(),
            fileSourceDTO.getWorkerId(),
            fileSourceDTO.getEnable(),
            fileSourceDTO.getCreator(),
            fileSourceDTO.getCreateTime(),
            fileSourceDTO.getLastModifyUser(),
            fileSourceDTO.getLastModifyTime()
        ).returning(defaultTable.ID);

        var sql = insertFileSourceQuery.getSQL(ParamType.INLINED);
        try {
            Integer fileSourceId = insertFileSourceQuery.fetchOne().getId();
            saveFileSourceShareInfo(dslContext, fileSourceId, fileSourceDTO);
            return fileSourceId;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    private List<Long> getSharedAppIdList(DSLContext dslContext, Long appId, Integer fileSourceId) {
        val records = dslContext.select(tableFileSourceShare.APP_ID)
            .from(tableFileSourceShare)
            .where(tableFileSourceShare.FILE_SOURCE_ID.eq(fileSourceId))
            .and(tableFileSourceShare.APP_ID.notEqual(appId))
            .fetch();
        return records.map(record -> record.get(tableFileSourceShare.APP_ID));
    }

    private void saveFileSourceShareInfo(DSLContext dslContext, Integer fileSourceId, FileSourceDTO fileSourceDTO) {
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
                if (!fileSourceDTO.getShareToAllApp() && sharedAppIdList.size() > 0) {
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
                }
            }
        } catch (Exception e) {
            log.error("error SQL={}", sql);
            throw e;
        }
    }

    @Override
    public int updateFileSource(DSLContext dslContext, FileSourceDTO fileSourceDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.APP_ID, fileSourceDTO.getAppId())
            .set(defaultTable.CODE, fileSourceDTO.getCode())
            .set(defaultTable.ALIAS, fileSourceDTO.getAlias())
            .set(defaultTable.TYPE, fileSourceDTO.getFileSourceType().getCode())
            .set(defaultTable.CUSTOM_INFO, JsonUtils.toJson(fileSourceDTO.getFileSourceInfoMap()))
            .set(defaultTable.PUBLIC, fileSourceDTO.getPublicFlag())
            .set(defaultTable.SHARE_TO_ALL_APP, fileSourceDTO.getShareToAllApp())
            .set(defaultTable.CREDENTIAL_ID, fileSourceDTO.getCredentialId())
            .set(defaultTable.FILE_PREFIX, fileSourceDTO.getFilePrefix())
            .set(defaultTable.WORKER_SELECT_SCOPE, fileSourceDTO.getWorkerSelectScope())
            .set(defaultTable.WORKER_SELECT_MODE, fileSourceDTO.getWorkerSelectMode())
            .set(defaultTable.WORKER_ID, fileSourceDTO.getWorkerId())
            .set(defaultTable.ENABLE, fileSourceDTO.getEnable())
            .set(defaultTable.LAST_MODIFY_USER, fileSourceDTO.getLastModifyUser())
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(fileSourceDTO.getId()));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            saveFileSourceShareInfo(dslContext, fileSourceDTO.getId(), fileSourceDTO);
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSourceStatus(DSLContext dslContext, Integer fileSourceId, Integer status) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.STATUS, JooqTypeUtil.convertToByte(status))
            .where(defaultTable.ID.eq(fileSourceId));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }


    @Override
    public int deleteFileSourceById(DSLContext dslContext, Integer id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public int enableFileSourceById(DSLContext dslContext, String username, Long appId, Integer id,
                                    Boolean enableFlag) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.ENABLE, enableFlag)
            .set(defaultTable.LAST_MODIFY_USER, username)
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(defaultTable.ID.eq(id).and(defaultTable.APP_ID.eq(appId)));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public FileSourceDTO getFileSourceById(DSLContext dslContext, Integer id) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public FileSourceDTO getFileSourceByCode(DSLContext dslContext, String code) {
        val record = dslContext.selectFrom(defaultTable).where(
            defaultTable.CODE.eq(code)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    private Collection<Condition> genAvailableLikeConditions(Long appId, String credentialId, String alias) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ENABLE.eq(true));
        if (appId != null) {
            conditions.add(tableFileSourceShare.APP_ID.eq(appId).or(defaultTable.SHARE_TO_ALL_APP.eq(true)));
        }
        if (StringUtils.isNotBlank(credentialId)) {
            conditions.add(defaultTable.CREDENTIAL_ID.eq(credentialId));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(defaultTable.ALIAS.like("%" + alias + "%"));
        }
        return conditions;
    }

    private Collection<Condition> genAvailableConditions(Long appId, String credentialId, String alias) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.ENABLE.eq(true));
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(credentialId)) {
            conditions.add(defaultTable.CREDENTIAL_ID.eq(credentialId));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(defaultTable.ALIAS.eq(alias));
        }
        return conditions;
    }

    @Override
    public Integer countFileSource(DSLContext dslContext, Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genAvailableConditions(appId, credentialId, alias);
        return countFileSourcesByConditions(dslContext, conditions);
    }

    @Override
    public Integer countAvailableLikeFileSource(DSLContext dslContext, Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genAvailableLikeConditions(appId, credentialId, alias);
        return countAvailableFileSourcesByConditions(dslContext, conditions);
    }

    public Integer countFileSourcesByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.count(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    public Integer countAvailableFileSourcesByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .join(tableFileSourceShare)
            .on(defaultTable.ID.eq(tableFileSourceShare.FILE_SOURCE_ID))
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    @Override
    public Boolean checkFileSourceExists(DSLContext dslContext, Long appId, String alias) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(defaultTable.ALIAS.eq(alias));
        }
        return countWorkTableFileSourcesByConditions(dslContext, conditions) > 0;
    }

    @Override
    public Integer countWorkTableFileSource(DSLContext dslContext, Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genWorkTableConditions(appId, credentialId, alias);
        return countWorkTableFileSourcesByConditions(dslContext, conditions);
    }

    @Override
    public Integer countWorkTableFileSource(DSLContext dslContext, List<Long> appIdList, List<Integer> idList) {
        Collection<Condition> conditions = genWorkTableConditions(appIdList, idList);
        return countWorkTableFileSourcesByConditions(dslContext, conditions);
    }

    public Integer countWorkTableFileSourcesByConditions(DSLContext dslContext, Collection<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    @Override
    public List<FileSourceDTO> listAvailableFileSource(DSLContext dslContext, Long appId, String credentialId, String
        alias, Integer start, Integer pageSize) {
        Collection<Condition> conditions = genAvailableLikeConditions(appId, credentialId, alias);
        return listFileSourceByShareConditions(dslContext, conditions, start, pageSize);
    }

    private Collection<Condition> genWorkTableConditions(Long appId, String credentialId, String alias) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(credentialId)) {
            conditions.add(defaultTable.CREDENTIAL_ID.eq(credentialId));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(defaultTable.ALIAS.like("%" + alias + "%"));
        }
        return conditions;
    }

    private Collection<Condition> genWorkTableConditions(List<Long> appIdList, List<Integer> idList) {
        List<Condition> conditions = new ArrayList<>();
        if (appIdList != null) {
            conditions.add(defaultTable.APP_ID.in(appIdList));
        }
        if (idList != null) {
            conditions.add(defaultTable.ID.in(idList));
        }
        return conditions;
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(DSLContext dslContext, Long appId, String credentialId,
                                                       String alias, Integer start, Integer pageSize) {
        Collection<Condition> conditions = genWorkTableConditions(appId, credentialId, alias);
        return listFileSourceByConditions(dslContext, conditions, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(DSLContext dslContext, List<Long> appIdList,
                                                       List<Integer> idList, Integer start, Integer pageSize) {
        Collection<Condition> conditions = genWorkTableConditions(appIdList, idList);
        return listFileSourceByConditions(dslContext, conditions, start, pageSize);
    }

    @Override
    public boolean existsCode(String code) {
        val query = defaultContext.selectZero().from(defaultTable)
            .where(defaultTable.CODE.eq(code)).limit(1);
        return query.fetch().size() > 0;
    }

    @Override
    public boolean existsFileSource(Long appId, Integer id) {
        val query = defaultContext.selectZero().from(defaultTable)
            .where(defaultTable.APP_ID.eq(appId))
            .and(defaultTable.ID.eq(id))
            .limit(1);
        return query.fetch().size() > 0;
    }

    private List<FileSourceDTO> listFileSourceByConditions(DSLContext dslContext, Collection<Condition> conditions,
                                                           Integer start, Integer pageSize) {
        val query = dslContext.select(
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.CODE,
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
        ).from(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    private List<FileSourceDTO> listFileSourceByShareConditions(DSLContext dslContext,
                                                                Collection<Condition> conditions, Integer start,
                                                                Integer pageSize) {
        val query = dslContext.select(
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.CODE,
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
        ).from(defaultTable)
            .join(tableFileSourceShare)
            .on(defaultTable.ID.eq(tableFileSourceShare.FILE_SOURCE_ID))
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        return listPage(query, start, pageSize, this::convertRecordToDto);
    }

    private FileSourceDTO convertRecordToDto(Record record) {
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
        fileSourceDTO.setSharedAppIdList(getSharedAppIdList(defaultContext, appId, id));
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
