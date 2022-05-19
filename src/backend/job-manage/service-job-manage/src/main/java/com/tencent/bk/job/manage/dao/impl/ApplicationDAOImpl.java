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

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationAttrsDO;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
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
        T_APP.MAINTAINERS,
        T_APP.BK_SUPPLIER_ACCOUNT,
        T_APP.APP_TYPE,
        T_APP.SUB_APP_IDS,
        T_APP.TIMEZONE,
        T_APP.BK_OPERATE_DEPT_ID,
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
    public ApplicationDTO getAppById(long appId) {
        Record record = context.select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
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
        applicationDTO.setMaintainers(record.get(T_APP.MAINTAINERS));
        applicationDTO.setBkSupplierAccount(record.get(T_APP.BK_SUPPLIER_ACCOUNT));
        applicationDTO.setAppType(AppTypeEnum.valueOf(record.get(T_APP.APP_TYPE)));
        applicationDTO.setSubBizIds(splitSubBizIds(record.get(T_APP.SUB_APP_IDS)));
        applicationDTO.setTimeZone(record.get(T_APP.TIMEZONE));
        applicationDTO.setOperateDeptId(record.get(T_APP.BK_OPERATE_DEPT_ID));
        applicationDTO.setLanguage(record.get(T_APP.LANGUAGE));
        applicationDTO.setDeleted(Bool.isTrue(record.get(T_APP.IS_DELETED).intValue()));
        applicationDTO.setAttrs(JsonUtils.fromJson(record.get(T_APP.ATTRS), ApplicationAttrsDO.class));
        return applicationDTO;
    }

    private static List<Long> splitSubBizIds(String bizIdsStr) {
        List<Long> bizIdList = new LinkedList<>();
        if (StringUtils.isNotBlank(bizIdsStr)) {
            for (String bizIdStr : bizIdsStr.split("[,;]")) {
                if (StringUtils.isNotBlank(bizIdStr)) {
                    bizIdList.add(Long.valueOf(bizIdStr));
                }
            }

        }
        return bizIdList;
    }

    private List<Condition> getBasicNotDeletedConditions() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(0)));
        return conditions;
    }

    @Override
    public List<Long> getBizIdsByOptDeptId(Long optDeptId) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.eq(ResourceScopeTypeEnum.BIZ.getValue()));
        if (optDeptId == null) {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.isNull());
        } else {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.eq(optDeptId));
        }
        val records = context.select(T_APP.BK_SCOPE_ID).from(T_APP).where(conditions).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(it -> Long.parseLong(it.component1()));
        }
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
        conditions.add(T_APP.APP_ID.in(appIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        return listAppsByConditions(conditions);
    }

    @Override
    public List<ApplicationDTO> listBizAppsByBizIds(Collection<Long> bizIdList) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.BK_SCOPE_TYPE.eq(ResourceScopeTypeEnum.BIZ.getValue()));
        conditions.add(
            T_APP.BK_SCOPE_ID.in(bizIdList.parallelStream().map(Object::toString)
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
    public List<ApplicationDTO> listAppsByType(AppTypeEnum appType) {
        List<Condition> conditions = getBasicNotDeletedConditions();
        conditions.add(T_APP.APP_TYPE.eq((byte) appType.getValue()));
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
        val subBizIds = applicationDTO.getSubBizIds();
        String subBizIdsStr = null;
        if (subBizIds != null) {
            subBizIdsStr = subBizIds.stream().map(Object::toString).collect(Collectors.joining(";"));
        }
        ResourceScope scope = applicationDTO.getScope();
        val query = dslContext.insertInto(T_APP,
            T_APP.APP_NAME,
            T_APP.APP_TYPE,
            T_APP.BK_SUPPLIER_ACCOUNT,
            T_APP.MAINTAINERS,
            T_APP.SUB_APP_IDS,
            T_APP.TIMEZONE,
            T_APP.BK_OPERATE_DEPT_ID,
            T_APP.LANGUAGE,
            T_APP.BK_SCOPE_TYPE,
            T_APP.BK_SCOPE_ID,
            T_APP.ATTRS,
            T_APP.IS_DELETED
        ).values(
            applicationDTO.getName(),
            (byte) (applicationDTO.getAppType().getValue()),
            applicationDTO.getBkSupplierAccount(),
            applicationDTO.getMaintainers(),
            subBizIdsStr,
            applicationDTO.getTimeZone(),
            applicationDTO.getOperateDeptId(),
            applicationDTO.getLanguage(),
            scope == null ? null : scope.getType().getValue(),
            scope == null ? null : scope.getId(),
            applicationDTO.getAttrs() == null ? null : JsonUtils.toJson(applicationDTO.getAttrs()),
            UByte.valueOf(Bool.FALSE.getValue())
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
            log.error("Fail to insertAppInfo:SQL={}", query.getSQL(ParamType.INLINED), e);
            throw e;
        }
    }

    @Override
    public Long insertAppWithSpecifiedAppId(DSLContext dslContext,
                                            ApplicationDTO applicationDTO) {
        val subBizIds = applicationDTO.getSubBizIds();
        String subBizIdsStr = null;
        if (subBizIds != null) {
            subBizIdsStr = subBizIds.stream().map(Object::toString).collect(Collectors.joining(";"));
        }
        ResourceScope scope = applicationDTO.getScope();
        val query = dslContext.insertInto(T_APP,
            T_APP.APP_ID,
            T_APP.APP_NAME,
            T_APP.APP_TYPE,
            T_APP.BK_SUPPLIER_ACCOUNT,
            T_APP.MAINTAINERS,
            T_APP.SUB_APP_IDS,
            T_APP.TIMEZONE,
            T_APP.BK_OPERATE_DEPT_ID,
            T_APP.LANGUAGE,
            T_APP.BK_SCOPE_TYPE,
            T_APP.BK_SCOPE_ID,
            T_APP.ATTRS,
            T_APP.IS_DELETED
        ).values(
            JooqDataTypeUtil.buildULong(applicationDTO.getId()),
            applicationDTO.getName(),
            (byte) (applicationDTO.getAppType().getValue()),
            applicationDTO.getBkSupplierAccount(),
            applicationDTO.getMaintainers(),
            subBizIdsStr,
            applicationDTO.getTimeZone(),
            applicationDTO.getOperateDeptId(),
            applicationDTO.getLanguage(),
            scope == null ? null : scope.getType().getValue(),
            scope == null ? null : scope.getId(),
            applicationDTO.getAttrs() == null ? null : JsonUtils.toJson(applicationDTO.getAttrs()),
            UByte.valueOf(Bool.FALSE.getValue())
        );
        try {
            query.execute();
        } catch (Exception e) {
            log.info("Fail to insertAppInfo:SQL={}", query.getSQL(ParamType.INLINED), e);
        }
        return applicationDTO.getId();
    }

    @Override
    public int updateApp(DSLContext dslContext, ApplicationDTO applicationDTO) {
        List<Long> subBizIds = applicationDTO.getSubBizIds();
        String subBizIdsStr = null;
        if (subBizIds != null) {
            subBizIdsStr = subBizIds.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        val query = dslContext.update(T_APP)
            .set(T_APP.APP_NAME, applicationDTO.getName())
            .set(T_APP.BK_SUPPLIER_ACCOUNT, applicationDTO.getBkSupplierAccount())
            .set(T_APP.MAINTAINERS, applicationDTO.getMaintainers())
            .set(T_APP.SUB_APP_IDS, subBizIdsStr)
            .set(T_APP.TIMEZONE, applicationDTO.getTimeZone())
            .set(T_APP.BK_OPERATE_DEPT_ID, applicationDTO.getOperateDeptId())
            .set(T_APP.LANGUAGE, applicationDTO.getLanguage())
            .set(T_APP.ATTRS, applicationDTO.getAttrs() == null ? null : JsonUtils.toJson(applicationDTO.getAttrs()))
            .set(T_APP.APP_TYPE, (byte) applicationDTO.getAppType().getValue())
            .where(T_APP.APP_ID.eq(ULong.valueOf(applicationDTO.getId())));
        return query.execute();
    }

    @Override
    public int restoreDeletedApp(DSLContext dslContext, long appId) {
        val query = dslContext.update(T_APP)
            .set(T_APP.IS_DELETED, UByte.valueOf(Bool.FALSE.getValue()))
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
    public int updateMaintainers(long appId, String maintainers) {
        return context.update(T_APP)
            .set(T_APP.MAINTAINERS, maintainers)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
    }

    @Override
    public int updateSubBizIds(long appId, String subBizIds) {
        return context.update(T_APP)
            .set(T_APP.SUB_APP_IDS, subBizIds)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
    }

    @Override
    public Integer countApps() {
        return context.selectCount()
            .from(T_APP)
            .where(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
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
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
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
