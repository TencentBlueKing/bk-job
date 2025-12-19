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

package com.tencent.bk.job.manage.dao.globalsetting.impl;

import com.tencent.bk.job.common.constant.TenantIdConstants;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.manage.dao.globalsetting.GlobalSettingDAO;
import com.tencent.bk.job.manage.model.dto.GlobalSettingDTO;
import com.tencent.bk.job.manage.model.tables.GlobalSetting;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.concurrent.atomic.AtomicInteger;

@Repository
@Slf4j
public class GlobalSettingDAOImpl implements GlobalSettingDAO {

    private final DSLContext dslContext;

    private static final GlobalSetting defaultTable = GlobalSetting.GLOBAL_SETTING;
    private static final TableField<?, ?>[] ALL_FIELDS = {
        defaultTable.KEY,
        defaultTable.VALUE,
        defaultTable.DECRIPTION,
        defaultTable.TENANT_ID
    };

    @Autowired
    public GlobalSettingDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public int upsertGlobalSetting(GlobalSettingDTO globalSettingDTO) {
        AtomicInteger affectedNum = new AtomicInteger(0);
        deleteGlobalSetting(globalSettingDTO.getKey());
        affectedNum.set(insertGlobalSetting(globalSettingDTO));
        return affectedNum.get();
    }

    @Override
    public int insertGlobalSetting(GlobalSettingDTO globalSettingDTO) {
        val query = dslContext.insertInto(defaultTable,
            defaultTable.KEY,
            defaultTable.VALUE,
            defaultTable.DECRIPTION,
            defaultTable.TENANT_ID
        ).values(
            globalSettingDTO.getKey(),
            globalSettingDTO.getValue(),
            globalSettingDTO.getDescription(),
            globalSettingDTO.getTenantId()
        );
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateGlobalSetting(GlobalSettingDTO globalSettingDTO) {
        val query = dslContext.update(defaultTable)
            .set(defaultTable.VALUE, globalSettingDTO.getValue())
            .set(defaultTable.DECRIPTION, globalSettingDTO.getDescription())
            .where(defaultTable.KEY.eq(globalSettingDTO.getKey())
            .and(defaultTable.TENANT_ID.eq(globalSettingDTO.getTenantId())));
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.execute();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int deleteGlobalSetting(String key) {
        return dslContext.deleteFrom(defaultTable).where(
            defaultTable.KEY.eq(key)
        ).execute();
    }

    /**
     * 兼容平台级别资源的查询，此类资源不是租户内独有的
     */
    @Override
    public GlobalSettingDTO getGlobalSetting(String key) {
        return getGlobalSetting(key, null);
    }

    @Override
    public GlobalSettingDTO getGlobalSetting(String key, String tenantId) {
        val query = dslContext.select(ALL_FIELDS).from(defaultTable).where(defaultTable.KEY.eq(key));
        if (tenantId != null) {
            query.and(defaultTable.TENANT_ID.eq(tenantId));
        } else {
            query.and(defaultTable.TENANT_ID.eq(TenantIdConstants.DEFAULT_TENANT_ID));
        }
        val record = query.fetchOne();
        if (record == null) {
            return null;
        } else {
            return new GlobalSettingDTO(
                record.get(defaultTable.KEY),
                record.get(defaultTable.VALUE),
                record.get(defaultTable.DECRIPTION),
                record.get(defaultTable.TENANT_ID)
            );
        }
    }
}
