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

package com.tencent.bk.job.manage.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.CommonCredential;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.crypto.CredentialCryptoService;
import com.tencent.bk.job.manage.dao.CredentialDAO;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceCredentialDisplayDTO;
import com.tencent.bk.job.manage.model.tables.Credential;
import com.tencent.bk.job.manage.model.tables.records.CredentialRecord;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.UpdateConditionStep;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Repository
public class CredentialDAOImpl implements CredentialDAO {

    private static final Credential defaultTable = Credential.CREDENTIAL;
    private final CredentialCryptoService credentialCryptoService;
    private final DSLContext dslContext;

    @Autowired
    public CredentialDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext,
                             CredentialCryptoService credentialCryptoService) {
        this.credentialCryptoService = credentialCryptoService;
        this.dslContext = dslContext;
    }

    @Override
    public String insertCredential(CredentialDTO credentialDTO) {
        String id = JobUUID.getUUID();
        String sql = null;
        String credentialStr = JsonUtils.toJson(credentialDTO.getCredential());
        log.debug("Save credentialStr={}", credentialStr);
        try {
            val query = dslContext.insertInto(defaultTable,
                defaultTable.ID,
                defaultTable.APP_ID,
                defaultTable.NAME,
                defaultTable.TYPE,
                defaultTable.DESCRIPTION,
                defaultTable.VALUE,
                defaultTable.CREATOR,
                defaultTable.CREATE_TIME,
                defaultTable.LAST_MODIFY_USER,
                defaultTable.LAST_MODIFY_TIME
            ).values(
                id,
                credentialDTO.getAppId(),
                credentialDTO.getName(),
                credentialDTO.getType(),
                credentialDTO.getDescription(),
                credentialCryptoService.encryptCredential(credentialStr),
                credentialDTO.getCreator(),
                credentialDTO.getCreateTime(),
                credentialDTO.getLastModifyUser(),
                credentialDTO.getLastModifyTime()
            );
            sql = query.getSQL(ParamType.INLINED);
            query.execute();
            return id;
        } catch (Exception e) {
            if (sql != null) {
                log.error(sql);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public String updateCredentialById(CredentialDTO credentialDTO) {
        String sql = null;
        String credentialStr = JsonUtils.toJson(credentialDTO.getCredential());
        log.debug("Update credentialStr={}", credentialStr);
        try {
            UpdateConditionStep<CredentialRecord> query = dslContext.update(defaultTable)
                .set(defaultTable.APP_ID, credentialDTO.getAppId())
                .set(defaultTable.NAME, credentialDTO.getName())
                .set(defaultTable.TYPE, credentialDTO.getType())
                .set(defaultTable.DESCRIPTION, credentialDTO.getDescription())
                .set(defaultTable.VALUE, credentialCryptoService.encryptCredential(credentialStr))
                .set(defaultTable.LAST_MODIFY_USER, credentialDTO.getLastModifyUser())
                .set(defaultTable.LAST_MODIFY_TIME, System.currentTimeMillis())
                .where(defaultTable.ID.eq(credentialDTO.getId()));
            sql = query.getSQL(ParamType.INLINED);
            query.execute();
            return credentialDTO.getId();
        } catch (Exception e) {
            if (sql != null) {
                log.error(sql);
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public int deleteCredentialById(String id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public CredentialDTO getCredentialById(String id) {
        val record = dslContext.select(
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.NAME,
            defaultTable.TYPE,
            defaultTable.DESCRIPTION,
            defaultTable.VALUE,
            defaultTable.CREATOR,
            defaultTable.CREATE_TIME,
            defaultTable.LAST_MODIFY_USER,
            defaultTable.LAST_MODIFY_TIME
        ).from(defaultTable).where(
            defaultTable.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public List<ServiceCredentialDisplayDTO> listCredentialDisplayInfoByIds(Collection<String> ids) {
        val records = dslContext.select(
            defaultTable.ID,
            defaultTable.APP_ID,
            defaultTable.NAME
        ).from(defaultTable).where(
            defaultTable.ID.in(ids)
        ).fetch();
        return records.map(this::convertRecordToDisplayDto);
    }

    private List<Condition> buildConditionList(
        CredentialDTO credentialQuery,
        BaseSearchCondition baseSearchCondition
    ) {
        List<Condition> conditions = new ArrayList<>();
        if (credentialQuery.getId() != null) {
            conditions.add(defaultTable.ID.eq(credentialQuery.getId()));
        }
        if (credentialQuery.getAppId() != null) {
            conditions.add(defaultTable.APP_ID.eq(credentialQuery.getAppId()));
        }
        if (StringUtils.isNotBlank(credentialQuery.getName())) {
            conditions.add(defaultTable.NAME.like("%" + credentialQuery.getName() + "%"));
        }
        if (StringUtils.isNotBlank(credentialQuery.getDescription())) {
            conditions.add(defaultTable.DESCRIPTION.like("%" + credentialQuery.getDescription() + "%"));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getCreator())) {
            conditions.add(defaultTable.CREATOR.like("%" + credentialQuery.getName() + "%"));
        }
        if (StringUtils.isNotBlank(baseSearchCondition.getLastModifyUser())) {
            conditions.add(defaultTable.LAST_MODIFY_USER.like("%" + credentialQuery.getName() + "%"));
        }
        return conditions;
    }

    /**
     * 查询符合条件的凭证数量
     */
    private long getPageCredentialCount(CredentialDTO credentialQuery, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(credentialQuery, baseSearchCondition);
        return getPageCredentialCount(conditions);
    }

    /**
     * 查询符合条件的凭证数量
     */
    private long getPageCredentialCount(Collection<Condition> conditions) {
        Long count = dslContext
            .selectCount()
            .from(defaultTable)
            .where(conditions)
            .fetchOne(0, Long.class);
        if (count != null) {
            return count;
        } else {
            log.error("Fail to count credential from db");
        }
        return -1L;
    }

    @Override
    public PageData<CredentialDTO> listCredentials(
        CredentialDTO credentialQuery,
        BaseSearchCondition baseSearchCondition
    ) {
        long count = getPageCredentialCount(credentialQuery, baseSearchCondition);
        List<Condition> conditions = buildConditionList(credentialQuery, baseSearchCondition);
        return listPageCredentialByConditions(baseSearchCondition, conditions, count);
    }

    @Override
    public PageData<CredentialDTO> listCredentialBasicInfo(Long appId, BaseSearchCondition baseSearchCondition) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        long count = getPageCredentialCount(conditions);
        return listPageCredentialBasicInfoByConditions(baseSearchCondition, conditions, count);
    }

    public PageData<CredentialDTO> listPageCredentialBasicInfoByConditions(
        BaseSearchCondition baseSearchCondition,
        Collection<Condition> conditions,
        long count
    ) {
        Integer start = baseSearchCondition.getStart();
        Integer length = baseSearchCondition.getLength();
        val query =
            dslContext.select(
                defaultTable.ID,
                defaultTable.NAME
            ).from(defaultTable)
                .where(conditions)
                .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        Result<Record2<String, String>> records;
        if (length != null && length > 0) {
            records = query.limit(start, length).fetch();
        } else {
            records = query.offset(start).fetch();
        }
        List<CredentialDTO> credentials = new ArrayList<>();
        if (records.size() != 0) {
            records.forEach(record -> {
                CredentialDTO credentialDTO = new CredentialDTO();
                credentialDTO.setId(record.get(defaultTable.ID));
                credentialDTO.setName(record.get(defaultTable.NAME));
                credentials.add(credentialDTO);
            });
        }

        PageData<CredentialDTO> credentialPageData = new PageData<>();
        credentialPageData.setTotal(count);
        credentialPageData.setPageSize(length);
        credentialPageData.setData(credentials);
        credentialPageData.setStart(start);
        return credentialPageData;
    }

    public PageData<CredentialDTO> listPageCredentialByConditions(
        BaseSearchCondition baseSearchCondition,
        Collection<Condition> conditions,
        long count
    ) {
        Collection<SortField<?>> orderFields = new ArrayList<>();
        if (StringUtils.isBlank(baseSearchCondition.getOrderField())) {
            orderFields.add(defaultTable.LAST_MODIFY_TIME.desc());
        } else {
            String orderField = baseSearchCondition.getOrderField();
            if ("name".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(defaultTable.NAME.asc());
                } else {
                    orderFields.add(defaultTable.NAME.desc());
                }
            } else if ("createTime".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(defaultTable.CREATE_TIME.asc());
                } else {
                    orderFields.add(defaultTable.CREATE_TIME.desc());
                }
            } else if ("lastModifyTime".equals(orderField)) {
                if (baseSearchCondition.getOrder() == 1) {
                    orderFields.add(defaultTable.LAST_MODIFY_TIME.asc());
                } else {
                    orderFields.add(defaultTable.LAST_MODIFY_TIME.desc());
                }
            }
        }

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        val records =
            dslContext.select(
                defaultTable.ID,
                defaultTable.APP_ID,
                defaultTable.NAME,
                defaultTable.TYPE,
                defaultTable.DESCRIPTION,
                defaultTable.VALUE,
                defaultTable.CREATOR,
                defaultTable.CREATE_TIME,
                defaultTable.LAST_MODIFY_USER,
                defaultTable.LAST_MODIFY_TIME
            )
                .from(defaultTable)
                .where(conditions)
                .orderBy(orderFields)
                .limit(start, length).fetch();
        List<CredentialDTO> credentials = new ArrayList<>();
        if (records.size() != 0) {
            records.map(record -> {
                credentials.add(convertRecordToDto(record));
                return null;
            });
        }

        PageData<CredentialDTO> credentialPageData = new PageData<>();
        credentialPageData.setTotal(count);
        credentialPageData.setPageSize(length);
        credentialPageData.setData(credentials);
        credentialPageData.setStart(start);
        return credentialPageData;
    }

    private CredentialDTO convertRecordToDto(Record record) {
        try {
            String credentialStr = credentialCryptoService.decryptCredential(
                record.get(defaultTable.VALUE)
            );
            log.debug("Get credential from DB:{}", credentialStr);
            return new CredentialDTO(
                record.get(defaultTable.ID),
                record.get(defaultTable.APP_ID),
                record.get(defaultTable.NAME),
                record.get(defaultTable.TYPE),
                record.get(defaultTable.DESCRIPTION),
                JsonUtils.fromJson(
                    credentialStr,
                    new TypeReference<CommonCredential>() {
                    }),
                record.get(defaultTable.CREATOR),
                record.get(defaultTable.CREATE_TIME),
                record.get(defaultTable.LAST_MODIFY_USER),
                record.get(defaultTable.LAST_MODIFY_TIME)
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ServiceCredentialDisplayDTO convertRecordToDisplayDto(Record record) {
        return new ServiceCredentialDisplayDTO(
            record.get(defaultTable.ID),
            record.get(defaultTable.APP_ID),
            record.get(defaultTable.NAME)
        );
    }
}
