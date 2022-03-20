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
import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 4/11/2019 22:46
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
        T_APP.IS_DELETED
    };

    private final DSLContext context;

    @Autowired
    public ApplicationDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    @Cacheable(value = "appInfoCache", key = "#appId", unless = "#result == null")
    public ApplicationDTO getCacheAppById(long appId) {
        return getAppById(appId);
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
        applicationDTO.setSubAppIds(splitSubAppIds(record.get(T_APP.SUB_APP_IDS)));
        applicationDTO.setTimeZone(record.get(T_APP.TIMEZONE));
        applicationDTO.setOperateDeptId(record.get(T_APP.BK_OPERATE_DEPT_ID));
        applicationDTO.setLanguage(record.get(T_APP.LANGUAGE));
        applicationDTO.setDeleted(Bool.isTrue(record.get(T_APP.IS_DELETED).intValue()));
        return applicationDTO;
    }

    private static List<Long> splitSubAppIds(String appIds) {
        List<Long> appIdList = new LinkedList<>();
        if (StringUtils.isNotBlank(appIds)) {
            for (String appIdStr : appIds.split("[,;]")) {
                if (StringUtils.isNotBlank(appIdStr)) {
                    appIdList.add(Long.valueOf(appIdStr));
                }
            }

        }
        return appIdList;
    }

    @Override
    public AppTypeEnum getAppTypeById(long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())));
        Record1<Byte> record = context.select(T_APP.APP_TYPE).from(T_APP).where(conditions).fetchOne();
        if (record != null) {
            return AppTypeEnum.valueOf(record.get(T_APP.APP_TYPE));
        } else {
            return null;
        }
    }

    @Override
    public List<Long> getSubAppIds(long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(T_APP.APP_TYPE.eq(JooqDataTypeUtil.getByteFromInteger(AppTypeEnum.APP_SET.getValue())));
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())));
        Record1<String> record = context.select(T_APP.SUB_APP_IDS).from(T_APP).where(conditions).fetchOne();
        if (record != null && StringUtils.isNotBlank(record.get(T_APP.SUB_APP_IDS))) {
            List<Long> subAppIds = new ArrayList<>();
            for (String subAppId : record.get(T_APP.SUB_APP_IDS).split("[,;]")) {
                subAppIds.add(Long.valueOf(subAppId));
            }
            return subAppIds;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<Long> getNormalAppIdsByOptDeptId(Long optDeptId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_TYPE.eq(JooqDataTypeUtil.getByteFromInteger(AppTypeEnum.NORMAL.getValue())));
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())));
        if (optDeptId == null) {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.isNull());
        } else {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.eq(optDeptId));
        }
        val records = context.select(T_APP.APP_ID).from(T_APP).where(conditions).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(it -> it.component1().longValue());
        }
    }

    @Override
    public List<ApplicationDTO> listAppsByAppIds(List<Long> appIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.in(appIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        conditions.add(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())));
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
    public List<ApplicationDTO> listAllBizApps() {
        Result<Record> result = context
            .select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
            .fetch();
        List<ApplicationDTO> applicationList = new ArrayList<>();
        if (result.size() > 0) {
            result.map(record -> applicationList.add(extract(record)));
        }
        return applicationList;
    }

    @Override
    public List<ApplicationDTO> listAllBizSetApps() {
        Result<Record> result = context
            .select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.BK_SCOPE_TYPE.equal(ResourceScopeTypeEnum.BIZ_SET.getValue()))
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
            .fetch();
        List<ApplicationDTO> applicationList = new ArrayList<>();
        if (result.size() > 0) {
            result.map(record -> applicationList.add(extract(record)));
        }
        return applicationList;
    }

    @Override
    public List<ApplicationDTO> listAppsByType(AppTypeEnum appType) {
        Result<Record> result = context
            .select(ALL_FIELDS)
            .from(T_APP)
            .where(T_APP.APP_TYPE.eq((byte) appType.getValue()))
            .and(T_APP.IS_DELETED.eq(UByte.valueOf(Bool.FALSE.getValue())))
            .fetch();
        List<ApplicationDTO> applicationInfoList = new ArrayList<>();
        if (result.size() > 0) {
            result.map(record -> applicationInfoList.add(extract(record)));
        }
        return applicationInfoList;
    }

    private void setDefaultValue(ApplicationDTO applicationDTO) {
        if (applicationDTO.getId() == null) {
            applicationDTO.setId(-1L);
        }
        if (applicationDTO.getAppType() == null) {
            applicationDTO.setAppType(AppTypeEnum.NORMAL);
        }
        if (applicationDTO.getBkSupplierAccount() == null) {
            applicationDTO.setBkSupplierAccount("-1");
        }
    }

    @Override
    public Long insertApp(DSLContext dslContext, ApplicationDTO applicationDTO) {
        setDefaultValue(applicationDTO);
        val subAppIds = applicationDTO.getSubAppIds();
        String subAppIdsStr = null;
        if (subAppIds != null) {
            subAppIdsStr = subAppIds.stream().map(Object::toString).collect(Collectors.joining(";"));
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
            T_APP.IS_DELETED
        ).values(
            ULong.valueOf(applicationDTO.getId()),
            applicationDTO.getName(),
            (byte) (applicationDTO.getAppType().getValue()),
            applicationDTO.getBkSupplierAccount(),
            applicationDTO.getMaintainers(),
            subAppIdsStr,
            applicationDTO.getTimeZone(),
            applicationDTO.getOperateDeptId(),
            applicationDTO.getLanguage(),
            scope == null ? null : scope.getType().getValue(),
            scope == null ? null : scope.getId(),
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
    @CacheEvict(value = "appInfoCache", key = "#applicationDTO.getId()")
    public int updateApp(DSLContext dslContext, ApplicationDTO applicationDTO) {
        setDefaultValue(applicationDTO);
        if (applicationDTO.getId() == -1L) {
            return -1;
        }
        val subAppIds = applicationDTO.getSubAppIds();
        String subAppIdsStr = null;
        if (subAppIds != null) {
            subAppIdsStr = subAppIds.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        val query = dslContext.update(T_APP)
            .set(T_APP.APP_NAME, applicationDTO.getName())
            .set(T_APP.APP_TYPE, (byte) (applicationDTO.getAppType().getValue()))
            .set(T_APP.BK_SUPPLIER_ACCOUNT, applicationDTO.getBkSupplierAccount())
            .set(T_APP.MAINTAINERS, applicationDTO.getMaintainers())
            .set(T_APP.SUB_APP_IDS, subAppIdsStr)
            .set(T_APP.TIMEZONE, applicationDTO.getTimeZone())
            .set(T_APP.BK_OPERATE_DEPT_ID, applicationDTO.getOperateDeptId())
            .set(T_APP.LANGUAGE, applicationDTO.getLanguage())
            .where(T_APP.APP_ID.eq(ULong.valueOf(applicationDTO.getId())));
        return query.execute();
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int deleteAppInfoById(DSLContext dslContext, long appId) {
        return dslContext.update(T_APP)
            .set(T_APP.IS_DELETED, UByte.valueOf(Bool.TRUE.getValue()))
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int updateMaintainers(long appId, String maintainers) {
        return context.update(T_APP)
            .set(T_APP.MAINTAINERS, maintainers)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int updateSubAppIds(long appId, String subAppIds) {
        return context.update(T_APP)
            .set(T_APP.SUB_APP_IDS, subAppIds)
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

    @Override
    public void restoreApp(Long appId) {
        context.update(T_APP)
            .set(T_APP.IS_DELETED, UByte.valueOf(Bool.FALSE.getValue()))
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
    }
}
