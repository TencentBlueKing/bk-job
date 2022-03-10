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
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Application;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 4/11/2019 22:46
 */
@Slf4j
@Repository
public class ApplicationInfoDAOImpl implements ApplicationInfoDAO {
    private static final Application defaultTable = Application.APPLICATION;
    private static final Application T_APP = Application.APPLICATION;

    private DSLContext context;

    @Autowired
    public ApplicationInfoDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    @Cacheable(value = "appInfoCache", key = "#appId", unless = "#result == null")
    public ApplicationInfoDTO getCacheAppInfoById(long appId) {
        return getAppInfoById(appId);
    }

    @Override
    public ApplicationInfoDTO getAppInfoById(long appId) {
        log.debug("{}|Query app info from db...", JobContextUtil.getRequestId());
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.eq(ULong.valueOf(appId)));
        Record9<ULong, String, String, String, Byte, String, String, Long, String> record = context
            .select(T_APP.APP_ID, T_APP.APP_NAME, T_APP.MAINTAINERS, T_APP.BK_SUPPLIER_ACCOUNT,
                T_APP.APP_TYPE, T_APP.SUB_APP_IDS, T_APP.TIMEZONE, T_APP.BK_OPERATE_DEPT_ID, T_APP.LANGUAGE)
            .from(T_APP).where(conditions).fetchOne();
        if (record != null) {
            return DbRecordMapper.convertRecordToApplicationInfo(record);
        }
        return null;
    }

    @Override
    public AppTypeEnum getAppTypeById(long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.eq(ULong.valueOf(appId)));
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
        if (optDeptId == null) {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.isNull());
        } else {
            conditions.add(T_APP.BK_OPERATE_DEPT_ID.eq(optDeptId));
        }
        val records = context.select(T_APP.APP_ID).from(T_APP).where(conditions).fetch();
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(it -> it.component1().longValue());
        }
    }

    @Override
    public List<ApplicationInfoDTO> getAppInfoByIds(List<Long> appIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T_APP.APP_ID.in(appIdList.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        Result<Record9<ULong, String, String, String, Byte, String, String, Long, String>> result =
            context
                .select(T_APP.APP_ID, T_APP.APP_NAME, T_APP.MAINTAINERS, T_APP.BK_SUPPLIER_ACCOUNT,
                    T_APP.APP_TYPE, T_APP.SUB_APP_IDS, T_APP.TIMEZONE, T_APP.BK_OPERATE_DEPT_ID, T_APP.LANGUAGE)
                .from(T_APP).where(conditions).fetch();
        List<ApplicationInfoDTO> applicationInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> applicationInfoList.add(DbRecordMapper.convertRecordToApplicationInfo(record)));
        }
        return applicationInfoList;
    }

    @Override
    public List<ApplicationInfoDTO> listAppInfo() {
        Result<Record9<ULong, String, String, String, Byte, String, String, Long, String>> result =
            context
                .select(T_APP.APP_ID, T_APP.APP_NAME, T_APP.MAINTAINERS, T_APP.BK_SUPPLIER_ACCOUNT,
                    T_APP.APP_TYPE, T_APP.SUB_APP_IDS, T_APP.TIMEZONE, T_APP.BK_OPERATE_DEPT_ID, T_APP.LANGUAGE)
                .from(T_APP).fetch();
        List<ApplicationInfoDTO> applicationInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> applicationInfoList.add(DbRecordMapper.convertRecordToApplicationInfo(record)));
        }
        return applicationInfoList;
    }

    @Override
    public List<ApplicationInfoDTO> listAppInfoByType(AppTypeEnum appType) {
        Result<Record9<ULong, String, String, String, Byte, String, String, Long, String>> result =
            context
                .select(defaultTable.APP_ID, defaultTable.APP_NAME, defaultTable.MAINTAINERS,
                    defaultTable.BK_SUPPLIER_ACCOUNT,
                    defaultTable.APP_TYPE, defaultTable.SUB_APP_IDS, defaultTable.TIMEZONE,
                    defaultTable.BK_OPERATE_DEPT_ID, T_APP.LANGUAGE)
                .from(defaultTable)
                .where(defaultTable.APP_TYPE.eq((byte) appType.getValue()))
                .fetch();
        List<ApplicationInfoDTO> applicationInfoList = new ArrayList<>();
        if (result != null && result.size() >= 1) {
            result.map(record -> applicationInfoList.add(DbRecordMapper.convertRecordToApplicationInfo(record)));
        }
        return applicationInfoList;
    }

    private void setDefaultValue(ApplicationInfoDTO applicationInfoDTO) {
        if (applicationInfoDTO.getId() == null) {
            applicationInfoDTO.setId(-1L);
        }
        if (applicationInfoDTO.getAppType() == null) {
            applicationInfoDTO.setAppType(AppTypeEnum.NORMAL);
        }
        if (applicationInfoDTO.getBkSupplierAccount() == null) {
            applicationInfoDTO.setBkSupplierAccount("-1");
        }
    }

    @Override
    public Long insertAppInfo(DSLContext dslContext, ApplicationInfoDTO applicationInfoDTO) {
        setDefaultValue(applicationInfoDTO);
        val subAppIds = applicationInfoDTO.getSubAppIds();
        String subAppIdsStr = null;
        if (subAppIds != null) {
            subAppIdsStr = subAppIds.stream().map(Object::toString).collect(Collectors.joining(";"));
        }
        val query = dslContext.insertInto(T_APP,
            T_APP.APP_ID,
            T_APP.APP_NAME,
            T_APP.APP_TYPE,
            T_APP.BK_SUPPLIER_ACCOUNT,
            T_APP.MAINTAINERS,
            T_APP.SUB_APP_IDS,
            T_APP.TIMEZONE,
            T_APP.BK_OPERATE_DEPT_ID,
            T_APP.LANGUAGE
        ).values(
            ULong.valueOf(applicationInfoDTO.getId()),
            applicationInfoDTO.getName(),
            (byte) (applicationInfoDTO.getAppType().getValue()),
            applicationInfoDTO.getBkSupplierAccount(),
            applicationInfoDTO.getMaintainers(),
            subAppIdsStr,
            applicationInfoDTO.getTimeZone(),
            applicationInfoDTO.getOperateDeptId(),
            applicationInfoDTO.getLanguage()
        );
        if (log.isDebugEnabled()) {
            log.info("SQL={}", query.getSQL(ParamType.INLINED));
        }
        try {
            query.execute();
        } catch (Exception e) {
            log.info("Fail to insertAppInfo:SQL={}", query.getSQL(ParamType.INLINED), e);
        }
        return applicationInfoDTO.getId();
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#applicationInfoDTO.getId()")
    public int updateAppInfo(DSLContext dslContext, ApplicationInfoDTO applicationInfoDTO) {
        setDefaultValue(applicationInfoDTO);
        if (applicationInfoDTO.getId() == -1L) {
            return -1;
        }
        val subAppIds = applicationInfoDTO.getSubAppIds();
        String subAppIdsStr = null;
        if (subAppIds != null) {
            subAppIdsStr = subAppIds.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        val query = dslContext.update(T_APP)
            .set(T_APP.APP_NAME, applicationInfoDTO.getName())
            .set(T_APP.APP_TYPE, (byte) (applicationInfoDTO.getAppType().getValue()))
            .set(T_APP.BK_SUPPLIER_ACCOUNT, applicationInfoDTO.getBkSupplierAccount())
            .set(T_APP.MAINTAINERS, applicationInfoDTO.getMaintainers())
            .set(T_APP.SUB_APP_IDS, subAppIdsStr)
            .set(T_APP.TIMEZONE, applicationInfoDTO.getTimeZone())
            .set(T_APP.BK_OPERATE_DEPT_ID, applicationInfoDTO.getOperateDeptId())
            .set(T_APP.LANGUAGE, applicationInfoDTO.getLanguage())
            .where(T_APP.APP_ID.eq(ULong.valueOf(applicationInfoDTO.getId())));
        int affectedNum = query.execute();
        if (log.isDebugEnabled()) {
            log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        }
        return affectedNum;
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int deleteAppInfoById(DSLContext dslContext, long appId) {
        int affectedNum = dslContext.deleteFrom(T_APP)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
        return affectedNum;
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int updateMaintainers(long appId, String maintainers) {
        int affectedNum = context.update(T_APP)
            .set(T_APP.MAINTAINERS, maintainers)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
        return affectedNum;
    }

    @Override
    @CacheEvict(value = "appInfoCache", key = "#appId")
    public int updateSubAppIds(long appId, String subAppIds) {
        int affectedNum = context.update(T_APP)
            .set(T_APP.SUB_APP_IDS, subAppIds)
            .where(T_APP.APP_ID.eq(ULong.valueOf(appId)))
            .execute();
        return affectedNum;
    }

    @Override
    public Integer countApps() {
        log.debug("countApps");
        return context.selectCount().from(T_APP)
            .fetchOne(0, Integer.class);
    }
}
