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

import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.dao.filesource.CurrentTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceShareDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.util.JooqTypeUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
public class CurrentTenantFileSourceDAOImpl extends BaseFileSourceDAOImpl implements CurrentTenantFileSourceDAO {
    private final DSLContext dslContext;

    @Autowired
    public CurrentTenantFileSourceDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext,
                                          FileSourceShareDAO fileSourceShareDAO,
                                          FileSourceTypeDAO fileSourceTypeDAO) {
        super(dslContext, fileSourceShareDAO, fileSourceTypeDAO);
        this.dslContext = dslContext;
    }

    private void setDefaultValue(FileSourceDTO fileSourceDTO) {
        if (fileSourceDTO.getStatus() == null) {
            fileSourceDTO.setStatus(0);
        }
    }

    @Override
    public Integer insertFileSource(FileSourceDTO fileSourceDTO) {
        setDefaultValue(fileSourceDTO);
        val insertFileSourceQuery = dslContext.insertInto(defaultTable,
            defaultTable.ID,
            defaultTable.TENANT_ID,
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
            null,
            JobContextUtil.getTenantId(),
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
            val record = insertFileSourceQuery.fetchOne();
            assert record != null;
            Integer fileSourceId = record.getId();
            fileSourceShareDAO.saveFileSourceShareInfo(fileSourceId, fileSourceDTO);
            return fileSourceId;
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateFileSource(FileSourceDTO fileSourceDTO) {
        val query = dslContext.update(defaultTable);
        var updateSetStep = query.set(defaultTable.APP_ID, fileSourceDTO.getAppId());
        if (StringUtils.isNotBlank(fileSourceDTO.getCode())) {
            updateSetStep = updateSetStep.set(defaultTable.CODE, fileSourceDTO.getCode());
        }
        if (StringUtils.isNotBlank(fileSourceDTO.getAlias())) {
            updateSetStep = updateSetStep.set(defaultTable.ALIAS, fileSourceDTO.getAlias());
        }
        if (fileSourceDTO.getFileSourceType() != null) {
            updateSetStep = updateSetStep.set(defaultTable.TYPE, fileSourceDTO.getFileSourceType().getCode());
        }
        if (fileSourceDTO.getFileSourceInfoMap() != null) {
            updateSetStep = updateSetStep.set(
                defaultTable.CUSTOM_INFO,
                JsonUtils.toJson(fileSourceDTO.getFileSourceInfoMap())
            );
        }
        if (fileSourceDTO.getPublicFlag() != null) {
            updateSetStep = updateSetStep.set(defaultTable.PUBLIC, fileSourceDTO.getPublicFlag());
        }
        if (fileSourceDTO.getShareToAllApp() != null) {
            updateSetStep = updateSetStep.set(defaultTable.SHARE_TO_ALL_APP, fileSourceDTO.getShareToAllApp());
        }
        if (StringUtils.isNotBlank(fileSourceDTO.getCredentialId())) {
            updateSetStep = updateSetStep.set(defaultTable.CREDENTIAL_ID, fileSourceDTO.getCredentialId());
        }
        if (fileSourceDTO.getFilePrefix() != null) {
            updateSetStep = updateSetStep.set(defaultTable.FILE_PREFIX, fileSourceDTO.getFilePrefix());
        }
        if (StringUtils.isNotBlank(fileSourceDTO.getWorkerSelectScope())) {
            updateSetStep = updateSetStep.set(defaultTable.WORKER_SELECT_SCOPE, fileSourceDTO.getWorkerSelectScope());
        }
        if (StringUtils.isNotBlank(fileSourceDTO.getWorkerSelectMode())) {
            updateSetStep = updateSetStep.set(defaultTable.WORKER_SELECT_MODE, fileSourceDTO.getWorkerSelectMode());
        }
        if (fileSourceDTO.getWorkerId() != null) {
            updateSetStep = updateSetStep.set(defaultTable.WORKER_ID, fileSourceDTO.getWorkerId());
        }
        if (fileSourceDTO.getEnable() != null) {
            updateSetStep = updateSetStep.set(defaultTable.ENABLE, fileSourceDTO.getEnable());
        }
        if (StringUtils.isNotBlank(fileSourceDTO.getLastModifyUser())) {
            updateSetStep = updateSetStep.set(defaultTable.LAST_MODIFY_USER, fileSourceDTO.getLastModifyUser());
        }
        updateSetStep = updateSetStep.set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis());
        val finalStep = updateSetStep.where(buildIdConditions(fileSourceDTO.getId()));
        val sql = finalStep.getSQL(ParamType.INLINED);
        try {
            fileSourceShareDAO.saveFileSourceShareInfo(fileSourceDTO.getId(), fileSourceDTO);
            return finalStep.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    private List<Condition> buildIdConditions(Integer id) {
        List<Condition> conditions = buildTenantConditions();
        conditions.add(defaultTable.ID.eq(id));
        return conditions;
    }

    private List<Condition> buildTenantConditions() {
        List<Condition> conditions = new ArrayList<>();
        String tenantId = JobContextUtil.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new InternalException("tenantId is blank");
        }
        conditions.add(defaultTable.TENANT_ID.eq(tenantId));
        return conditions;
    }

    @Override
    public int deleteFileSourceById(Integer id) {
        return dslContext.deleteFrom(defaultTable)
            .where(buildIdConditions(id))
            .execute();
    }

    @Override
    public int enableFileSourceById(String username, Long appId, Integer id, Boolean enableFlag) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.ENABLE, enableFlag)
            .set(defaultTable.LAST_MODIFY_USER, username)
            .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
            .where(buildConditions(id, appId));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    private List<Condition> buildConditions(Integer id, Long appId) {
        List<Condition> conditions = buildIdConditions(id);
        conditions.add(defaultTable.APP_ID.eq(appId));
        return conditions;
    }

    @Override
    public FileSourceDTO getFileSourceById(Integer id) {
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(buildIdConditions(id))
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public List<FileSourceBasicInfoDTO> listFileSourceByIds(Collection<Integer> ids) {
        val records = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(buildConditions(ids))
            .fetch();
        return records.map(this::convertRecordToBasicInfoDto);
    }

    private List<Condition> buildConditions(Collection<Integer> ids) {
        List<Condition> conditions = buildTenantConditions();
        conditions.add(defaultTable.ID.in(ids));
        return conditions;
    }

    @Override
    public FileSourceDTO getFileSourceByCode(Long appId, String code) {
        List<Condition> conditions = buildTenantConditions();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.CODE.eq(code));
        val record = dslContext.select(ALL_FIELDS)
            .from(defaultTable)
            .where(conditions)
            .fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    private Collection<Condition> genAvailableLikeConditions(Long appId, String credentialId, String alias) {
        List<Condition> conditions = buildTenantConditions();
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
        List<Condition> conditions = buildTenantConditions();
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
    public Integer countFileSource(Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genAvailableConditions(appId, credentialId, alias);
        return countFileSourcesByConditions(conditions);
    }

    @Override
    public Integer countAvailableLikeFileSource(Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genAvailableLikeConditions(appId, credentialId, alias);
        return countAvailableFileSourcesByConditions(conditions);
    }

    public Integer countFileSourcesByConditions(Collection<Condition> conditions) {
        val query = dslContext.select(DSL.count(defaultTable.ID))
            .from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    public Integer countAvailableFileSourcesByConditions(Collection<Condition> conditions) {
        val query = dslContext.select(DSL.countDistinct(defaultTable.ID))
            .from(defaultTable)
            .join(tableFileSourceShare)
            .on(defaultTable.ID.eq(tableFileSourceShare.FILE_SOURCE_ID))
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    @Override
    public Boolean checkFileSourceExists(Long appId, String alias) {
        List<Condition> conditions = buildTenantConditions();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(alias)) {
            conditions.add(defaultTable.ALIAS.eq(alias));
        }
        return countWorkTableFileSourcesByConditions(conditions) > 0;
    }

    @Override
    public Integer countWorkTableFileSource(Long appId, String credentialId, String alias) {
        Collection<Condition> conditions = genWorkTableConditions(appId, credentialId, alias);
        return countWorkTableFileSourcesByConditions(conditions);
    }

    @Override
    public Integer countWorkTableFileSource(List<Long> appIdList, List<Integer> idList) {
        Collection<Condition> conditions = genWorkTableConditions(appIdList, idList);
        return countWorkTableFileSourcesByConditions(conditions);
    }

    public Integer countWorkTableFileSourcesByConditions(Collection<Condition> conditions) {
        val query = dslContext.select(DSL.countDistinct(defaultTable.ID))
            .from(defaultTable)
            .where(conditions);
        return query.fetchOne(0, Integer.class);
    }

    @Override
    public List<FileSourceDTO> listAvailableFileSource(Long appId,
                                                       String credentialId,
                                                       String alias,
                                                       Integer start,
                                                       Integer pageSize) {
        Collection<Condition> conditions = genAvailableLikeConditions(appId, credentialId, alias);
        return listFileSourceByShareConditions(conditions, start, pageSize);
    }

    private Collection<Condition> genWorkTableConditions(Long appId, String credentialId, String alias) {
        List<Condition> conditions = buildTenantConditions();
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
        List<Condition> conditions = buildTenantConditions();
        if (appIdList != null) {
            conditions.add(defaultTable.APP_ID.in(appIdList));
        }
        if (idList != null) {
            conditions.add(defaultTable.ID.in(idList));
        }
        return conditions;
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(Long appId, String credentialId,
                                                       String alias, Integer start, Integer pageSize) {
        Collection<Condition> conditions = genWorkTableConditions(appId, credentialId, alias);
        return listFileSourceByConditions(conditions, start, pageSize);
    }

    @Override
    public List<FileSourceDTO> listWorkTableFileSource(List<Long> appIdList,
                                                       List<Integer> idList, Integer start, Integer pageSize) {
        Collection<Condition> conditions = genWorkTableConditions(appIdList, idList);
        return listFileSourceByConditions(conditions, start, pageSize);
    }

    @Override
    public boolean existsCode(Long appId, String code) {
        List<Condition> conditions = buildAppIdCodeConditions(appId, code);
        val query = dslContext.selectZero().from(defaultTable)
            .where(conditions)
            .limit(1);
        return !query.fetch().isEmpty();
    }

    private List<Condition> buildAppIdCodeConditions(Long appId, String code) {
        List<Condition> conditions = buildTenantConditions();
        conditions.add(defaultTable.APP_ID.eq(appId));
        conditions.add(defaultTable.CODE.eq(code));
        return conditions;
    }

    @Override
    public boolean existsCodeExceptId(Long appId, String code, Integer exceptId) {
        List<Condition> conditions = buildAppIdCodeConditions(appId, code);
        conditions.add(defaultTable.ID.notEqual(exceptId));
        val query = dslContext.selectZero().from(defaultTable)
            .where(conditions)
            .limit(1);
        return !query.fetch().isEmpty();
    }

    @Override
    public boolean existsFileSource(Long appId, Integer id) {
        List<Condition> conditions = buildConditions(id, appId);
        val query = dslContext.selectZero().from(defaultTable)
            .where(conditions)
            .limit(1);
        return !query.fetch().isEmpty();
    }

    @Override
    public Integer getFileSourceIdByCode(Long appId, String code) {
        List<Condition> conditions = buildAppIdCodeConditions(appId, code);
        val query = dslContext.select(defaultTable.ID)
            .from(defaultTable)
            .where(conditions);
        val result = query.fetchOne();
        if (result != null) {
            return result.get(defaultTable.ID);
        }
        return null;
    }

    private FileSourceBasicInfoDTO convertRecordToBasicInfoDto(Record record) {
        FileSourceBasicInfoDTO fileSourceBasicInfoDTO = new FileSourceBasicInfoDTO();
        fileSourceBasicInfoDTO.setId(record.get(defaultTable.ID));
        fileSourceBasicInfoDTO.setAppId(record.get(defaultTable.APP_ID));
        fileSourceBasicInfoDTO.setCode(record.get(defaultTable.CODE));
        fileSourceBasicInfoDTO.setAlias(record.get(defaultTable.ALIAS));
        return fileSourceBasicInfoDTO;
    }
}
