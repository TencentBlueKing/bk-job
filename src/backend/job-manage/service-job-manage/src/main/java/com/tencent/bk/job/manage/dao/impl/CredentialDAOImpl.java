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
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.config.JobTicketConfig;
import com.tencent.bk.job.manage.dao.CredentialDAO;
import com.tencent.bk.job.manage.model.credential.CommonCredential;
import com.tencent.bk.job.manage.model.dto.CredentialDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record10;
import org.jooq.Result;
import org.jooq.SortField;
import org.jooq.UpdateConditionStep;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Credential;
import org.jooq.generated.tables.records.CredentialRecord;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class CredentialDAOImpl implements CredentialDAO {

    private static final Credential defaultTable = Credential.CREDENTIAL;
    private final JobTicketConfig jobTicketConfig;
    private final DSLContext defaultDSLContext;

    @Autowired
    public CredentialDAOImpl(JobTicketConfig jobTicketConfig, DSLContext dslContext) {
        this.jobTicketConfig = jobTicketConfig;
        this.defaultDSLContext = dslContext;
    }

    @Override
    public String insertCredential(DSLContext dslContext, CredentialDTO credentialDTO) {
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
                AESUtils.encryptToBase64EncodedCipherText(
                    credentialStr,
                    jobTicketConfig.getEncryptPassword()
                ),
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
    public String updateCredentialById(DSLContext dslContext, CredentialDTO credentialDTO) {
        String sql = null;
        String credentialStr = JsonUtils.toJson(credentialDTO.getCredential());
        log.debug("Update credentialStr={}", credentialStr);
        try {
            UpdateConditionStep<CredentialRecord> query = dslContext.update(defaultTable)
                .set(defaultTable.APP_ID, credentialDTO.getAppId())
                .set(defaultTable.NAME, credentialDTO.getName())
                .set(defaultTable.TYPE, credentialDTO.getType())
                .set(defaultTable.DESCRIPTION, credentialDTO.getDescription())
                .set(defaultTable.VALUE, AESUtils.encryptToBase64EncodedCipherText(
                    credentialStr,
                    jobTicketConfig.getEncryptPassword()))
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
    public int deleteCredentialById(DSLContext dslContext, String id) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.ID.eq(id)
        ).execute();
    }

    @Override
    public CredentialDTO getCredentialById(DSLContext dslContext, String id) {
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
     * 查询符合条件的凭据数量
     */
    private long getPageCredentialCount(CredentialDTO credentialQuery, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(credentialQuery, baseSearchCondition);
        Long count = defaultDSLContext
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

    public PageData<CredentialDTO> listPageCredentialByConditions(
        BaseSearchCondition baseSearchCondition,
        List<Condition> conditions,
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
            defaultDSLContext.select(
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

    private List<Condition> generateConditions(Long appId, String id, String name, String description, String creator
        , String lastModifyUser) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(defaultTable.APP_ID.eq(appId));
        }
        if (StringUtils.isNotBlank(id)) {
            conditions.add(defaultTable.ID.eq(id));
        }
        if (name != null) {
            conditions.add(defaultTable.NAME.like("%" + name + "%"));
        }
        if (description != null) {
            conditions.add(defaultTable.DESCRIPTION.like("%" + description + "%"));
        }
        if (creator != null) {
            conditions.add(defaultTable.CREATOR.like("%" + creator + "%"));
        }
        if (lastModifyUser != null) {
            conditions.add(defaultTable.LAST_MODIFY_USER.like("%" + lastModifyUser + "%"));
        }
        return conditions;
    }

    private List<Condition> generateConditions(List<Long> appIdList, List<String> idList) {
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
    public Integer countCredentials(DSLContext dslContext, Long appId, String id, String name, String description,
                                    String creator, String lastModifyUser) {
        List<Condition> conditions = generateConditions(appId, id, name, description, creator, lastModifyUser);
        return countCredentialByConditions(dslContext, conditions);
    }

    @Override
    public Integer countCredentials(DSLContext dslContext, List<Long> appIdList, List<String> idList) {
        List<Condition> conditions = generateConditions(appIdList, idList);
        return countCredentialByConditions(dslContext, conditions);
    }

    @Override
    public List<CredentialDTO> listCredentials(DSLContext dslContext, List<Long> appIdList, List<String> idList,
                                               Integer start, Integer pageSize) {
        List<Condition> conditions = generateConditions(appIdList, idList);
        return listCredentialsByConditions(dslContext, conditions, start, pageSize);
    }

    @Override
    public List<CredentialDTO> listCredentials(DSLContext dslContext, Long appId, String id, String name,
                                               String description, String creator, String lastModifyUser,
                                               Integer start, Integer pageSize) {
        List<Condition> conditions = generateConditions(appId, id, name, description, creator, lastModifyUser);
        return listCredentialsByConditions(dslContext, conditions, start, pageSize);
    }

    private List<CredentialDTO> listCredentialsByConditions(DSLContext dslContext, Collection<Condition> conditions,
                                                            Integer start, Integer limit) {
        val query = dslContext.select(
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
        ).from(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.LAST_MODIFY_TIME.desc());
        Result<Record10<String, Long, String, String, String, String, String, Long, String, Long>> records = null;
        if (start == null || start < 0) {
            start = 0;
        }
        if (limit == null || limit < 0) {
            limit = -1;
        }
        if (limit < 0) {
            records = query.fetch();
        } else {
            records = query.limit(start, limit).fetch();
        }
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public Integer countCredentialByAppId(DSLContext dslContext, Long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.APP_ID.eq(appId));
        return countCredentialByConditions(dslContext, conditions);
    }

    private Integer countCredentialByConditions(DSLContext dslContext, List<Condition> conditions) {
        val query = dslContext.select(
            DSL.countDistinct(defaultTable.ID)
        ).from(defaultTable)
            .where(conditions);
        val count = query.fetchOne(0, Integer.class);
        return count;
    }

    private CredentialDTO convertRecordToDto(
        Record record) {
        try {
            String credentialStr = AESUtils.decryptToPlainText(
                Base64Util.decodeContentToByte(record.get(defaultTable.VALUE)),
                jobTicketConfig.getEncryptPassword()
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
}
