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

package com.tencent.bk.job.ticket.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.util.JobUUID;
import com.tencent.bk.job.common.util.crypto.AESUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.ticket.config.JobTicketConfig;
import com.tencent.bk.job.ticket.dao.CredentialDAO;
import com.tencent.bk.job.ticket.model.credential.CommonCredential;
import com.tencent.bk.job.ticket.model.dto.CredentialDTO;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record10;
import org.jooq.Result;
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

    @Autowired
    public CredentialDAOImpl(JobTicketConfig jobTicketConfig) {
        this.jobTicketConfig = jobTicketConfig;
    }

    @Override
    public String insertCredential(DSLContext dslContext, CredentialDTO credentialDTO) {
        String id = JobUUID.getUUID();
        String sql = null;
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
                    JsonUtils.toJson(credentialDTO.getCredential()),
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
        try {
            UpdateConditionStep<CredentialRecord> query = dslContext.update(defaultTable)
                .set(defaultTable.APP_ID, credentialDTO.getAppId())
                .set(defaultTable.NAME, credentialDTO.getName())
                .set(defaultTable.TYPE, credentialDTO.getType())
                .set(defaultTable.DESCRIPTION, credentialDTO.getDescription())
                .set(defaultTable.VALUE, AESUtils.encryptToBase64EncodedCipherText(
                    JsonUtils.toJson(credentialDTO.getCredential()),
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
        Record10<String, Long, String, String, String, String, String, Long, String, Long> record) {
        try {
            return new CredentialDTO(
                record.get(defaultTable.ID),
                record.get(defaultTable.APP_ID),
                record.get(defaultTable.NAME),
                record.get(defaultTable.TYPE),
                record.get(defaultTable.DESCRIPTION),
                JsonUtils.fromJson(
                    AESUtils.decryptToPlainText(
                        Base64Util.decodeContentToByte(record.get(defaultTable.VALUE)),
                        jobTicketConfig.getEncryptPassword()),
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
