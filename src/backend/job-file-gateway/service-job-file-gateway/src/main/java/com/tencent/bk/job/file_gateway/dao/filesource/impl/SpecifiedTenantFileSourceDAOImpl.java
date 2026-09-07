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

import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceShareDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTypeDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.SpecifiedTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBasicInfoDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
public class SpecifiedTenantFileSourceDAOImpl extends BaseFileSourceDAOImpl implements SpecifiedTenantFileSourceDAO {

    private final DSLContext dslContext;

    @Autowired
    public SpecifiedTenantFileSourceDAOImpl(@Qualifier("job-file-gateway-dsl-context") DSLContext dslContext,
                                            FileSourceShareDAO fileSourceShareDAO,
                                            FileSourceTypeDAO fileSourceTypeDAO) {
        super(dslContext, fileSourceShareDAO, fileSourceTypeDAO);
        this.dslContext = dslContext;
    }

    @Override
    public List<FileSourceBasicInfoDTO> listFileSourceBasicInfoByIds(String tenantId, Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        val records = dslContext.select(BASIC_INFO_FIELDS)
            .from(defaultTable)
            .where(defaultTable.TENANT_ID.eq(tenantId))
            .and(defaultTable.ID.in(ids))
            .fetch();
        return records.map(this::convertRecordToBasicInfoDto);
    }

    @Override
    public Set<Integer> listFileSourceIdsInAppScope(String tenantId, Long appId, Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptySet();
        }
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.TENANT_ID.eq(tenantId));
        conditions.add(defaultTable.ID.in(ids));
        conditions.add(genAppScopeCondition(appId));
        val records = dslContext.selectDistinct(defaultTable.ID)
            .from(defaultTable)
            .join(tableFileSourceShare)
            .on(defaultTable.ID.eq(tableFileSourceShare.FILE_SOURCE_ID))
            .where(conditions)
            .fetch();
        return new HashSet<>(records.map(record -> record.get(defaultTable.ID)));
    }

}
