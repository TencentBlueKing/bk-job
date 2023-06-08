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

import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Application;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 业务DAO
 */
@Slf4j
@Repository
public class ApplicationDAOImpl implements ApplicationDAO {
    private static final Application T_APP = Application.APPLICATION;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        T_APP.APP_ID,
        T_APP.BK_SCOPE_TYPE,
        T_APP.BK_SCOPE_ID,
        T_APP.APP_NAME,
        T_APP.BK_SUPPLIER_ACCOUNT,
        T_APP.TIMEZONE,
        T_APP.LANGUAGE,
        T_APP.IS_DELETED,
        T_APP.ATTRS
    };

    private final DSLContext context;

    @Autowired
    public ApplicationDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public boolean existBiz(long bizId) {
        val records = context.selectZero()
            .from(T_APP)
            .where(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.byteValue())))
            .and(T_APP.BK_SCOPE_TYPE.eq(ResourceScopeTypeEnum.BIZ.getValue()))
            .and(T_APP.BK_SCOPE_ID.eq("" + bizId))
            .limit(1)
            .fetch();
        return records.size() > 0;
    }

    @Override
    public ApplicationDTO getAppById(long appId) {
        Record record = context.select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.byteValue())))
            .fetchOne();
        if (record != null) {
            return extract(record);
        }
        return null;
    }

    public static ApplicationDTO extract(Record record) {
        if (record == null) {
            return null;
        }
        ApplicationDTO applicationDTO = new ApplicationDTO();
        applicationDTO.setId(record.get(T_APP.APP_ID).longValue());
        String scopeType = record.get(T_APP.BK_SCOPE_TYPE);
        String scopeId = record.get(T_APP.BK_SCOPE_ID);
        applicationDTO.setScope(new ResourceScope(scopeType, scopeId));
        applicationDTO.setName(record.get(T_APP.APP_NAME));
        applicationDTO.setBkSupplierAccount(record.get(T_APP.BK_SUPPLIER_ACCOUNT));
        applicationDTO.setTimeZone(record.get(T_APP.TIMEZONE));
        applicationDTO.setLanguage(record.get(T_APP.LANGUAGE));
        applicationDTO.setAttrs(JsonUtils.fromJson(record.get(T_APP.ATTRS), ApplicationAttrsDO.class));
        applicationDTO.setDeleted(Bool.isTrue(record.get(T_APP.IS_DELETED).byteValue()));
        return applicationDTO;
    }

    private List<Condition> getBasicNotDeletedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(0)));
        return conditions;
    }

    private List<ApplicationDTO> listAppsByConditions(List<Condition> conditions) {
        if (conditions == null) {
            conditions = new ArrayList<>();
        }
        Result<Record> result = context
            .select(ALL_FIELDS)
            .from(T_APP)
            .where(conditions)
            .fetch();
        List<ApplicationDTO> applicationList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> applicationList.add(extract(record)));
        }
        return applicationList;
    }

    @Override
    public List<ApplicationDTO> listAllApps() {
        List<Condition> conditions = getBasicNotDeletedConditions();
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listAppsByAppIds(List<Long> appIdList) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.APP_ID.in(appIdList.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listBizAppsByBizIds(Collection<Long> bizIdList) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.eq(ResourceScopeTypeEnum.BIZ.getValue()));
        conditions.add(
            T_APP.BK_SCOPE_ID.in(bizIdList.stream().map(Object::toString)
                .collect(Collectors.toList()))
        );
        return listAppsByConditions(conditions);
    }

    @Override
    public List<Long> listAllBizAppBizIds() {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.equal(ResourceScopeTypeEnum.BIZ.getValue()));
        Result<Record1<String>> records = context
            .select(T_APP.BK_SCOPE_ID)
            .from(T_APP)
            .where(conditions)
            .fetch();
        return records.map(record -> Long.parseLong(record.get(T_APP.BK_SCOPE_ID)));
    }

    @Override
    public List<ApplicationDTO> listAllBizApps() {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.equal(ResourceScopeTypeEnum.BIZ.getValue()));
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listAllBizAppsWithDeleted() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.BK_SCOPE_TYPE.equal(ResourceScopeTypeEnum.BIZ.getValue()));
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listAllBizSetAppsWithDeleted() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.BK_SCOPE_TYPE.equal(ResourceScopeTypeEnum.BIZ_SET.getValue()));
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listAppsByScopeType(ResourceScopeTypeEnum scopeType) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.eq(scopeType.getValue()));
        return listAppsByConditions(conditions);
    }

    @Override
    public Long insertApp(DSLContext dslContext, ApplicationDTO applicationDTO) {
        ResourceScope scope = applicationDTO.getScope();
        val query = dslContext.insertInto(T_APP,
            T_APP.APP_NAME,
            T_APP.BK_SUPPLIER_ACCOUNT,
            T_APP.TIMEZONE,
            T_APP.LANGUAGE,
            T_APP.BK_SCOPE_TYPE,
            T_APP.BK_SCOPE_ID,
            T_APP.ATTRS,
            T_APP.IS_DELETED
        ).values(
            applicationDTO.getName(),
            applicationDTO.getBkSupplierAccount(),
            applicationDTO.getTimeZone(),
            applicationDTO.getLanguage(),
            scope == null ? null : scope.getType().getValue(),
            scope == null ? null : scope.getId(),
            applicationDTO.getAttrs() == null ? null : JsonUtils.toJson(applicationDTO.getAttrs()),
            UByte.valueOf(Bool.FALSE.byteValue())
        );
        try {
            val record = query.returning(T_APP.APP_ID).fetchOne();
            if (record != null) {
                return record.getAppId().longValue();
            } else {
                throw new InternalException(
                    "Get null id after insert, please check db generator",
                    ErrorCode.INTERNAL_ERROR
                );
            }
        } catch (Exception e) {
            String msg = MessageFormatter.arrayFormat(
                "Fail to insertAppInfo:SQL={}",
                new String[]{query.getSQL(ParamType.INLINED)}
            ).getMessage();
            log.error(msg, e);
            throw e;
        }
    }

    @Override
    public int updateApp(DSLContext dslContext, ApplicationDTO applicationDTO) {
        val query = dslContext.update(T_APP)
            .set(T_APP.APP_NAME, applicationDTO.getName())
            .set(T_APP.BK_SUPPLIER_ACCOUNT, applicationDTO.getBkSupplierAccount())
            .set(T_APP.TIMEZONE, applicationDTO.getTimeZone())
            .set(T_APP.LANGUAGE, applicationDTO.getLanguage())
            .set(T_APP.ATTRS, applicationDTO.getAttrs() == null ? null : JsonUtils.toJson(applicationDTO.getAttrs()))
            .where(T_APP.APP_ID.eq(ULong.valueOf(applicationDTO.getId())));
        return query.execute();
    }

    @Override
    public int restoreDeletedApp(DSLContext dslContext, long appId) {
        val query = dslContext.update(T_APP)
            .set(T_APP.IS_DELETED, UByte.valueOf(Bool.FALSE.byteValue()))
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)));
        int affectedNum = query.execute();
        if (log.isDebugEnabled()) {
            log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        }
        return affectedNum;
    }

    @Override
    public int deleteAppByIdSoftly(DSLContext dslContext, long appId) {
        val query = dslContext.update(T_APP)
            .set(T_APP.IS_DELETED, UByte.valueOf(1))
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)));
        int affectedNum = query.execute();
        if (log.isDebugEnabled()) {
            log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        }
        return affectedNum;
    }

    @Override
    public Integer countApps() {
        return context.selectCount()
            .from(T_APP)
            .where(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.byteValue())))
            .fetchOne(0, Integer.class);
    }

    @Override
    public Integer countBizSetAppsWithDeleted() {
        return context.selectCount()
            .from(T_APP)
            .where(T_APP.BK_SCOPE_TYPE.eq(ResourceScopeTypeEnum.BIZ_SET.getValue()))
            .fetchOne(0, Integer.class);
    }

    @Override
    public ApplicationDTO getAppByScope(ResourceScope scope) {
        Record record = context.select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.BK_SCOPE_TYPE.eq(scope.getType().getValue()))
            .and(T_APP.BK_SCOPE_ID.eq(scope.getId()))
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.byteValue())))
            .fetchOne();
        if (record != null) {
            return extract(record);
        }
        return null;
    }

    @Override
    public ApplicationDTO getAppByScopeIncludingDeleted(ResourceScope scope) {
        Record record = context.select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.BK_SCOPE_TYPE.eq(scope.getType().getValue()))
            .and(T_APP.BK_SCOPE_ID.eq(scope.getId()))
            .fetchOne();
        if (record != null) {
            return extract(record);
        }
        return null;
    }
}
