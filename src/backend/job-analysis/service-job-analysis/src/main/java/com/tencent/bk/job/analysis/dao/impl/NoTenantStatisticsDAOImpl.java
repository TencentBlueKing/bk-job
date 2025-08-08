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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.dao.NoTenantStatisticsDAO;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class NoTenantStatisticsDAOImpl extends BaseStatisticsDAO implements NoTenantStatisticsDAO {

    @Autowired
    public NoTenantStatisticsDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public int deleteStatisticsByDate(String date) {
        int totalAffectedRows = 0;
        int affectedRows;
        do {
            affectedRows = defaultDSLContext.deleteFrom(defaultTable).where(
                defaultTable.DATE.lessThan(date)
            ).limit(10000).execute();
            totalAffectedRows += affectedRows;
        } while (affectedRows == 10000);
        return totalAffectedRows;
    }

    @Override
    public boolean existsStatisticsByDate(String date) {
        List<Condition> conditions = getBasicConditions();
        if (StringUtils.isNotBlank(date)) {
            conditions.add(defaultTable.DATE.eq(date));
        }
        return existsStatisticsByConditions(conditions);
    }

    @Override
    public boolean existsStatistics(List<Long> inAppIdList,
                                    List<Long> notInAppIdList,
                                    String resource,
                                    String dimension,
                                    String dimensionValue,
                                    String date) {
        return existsStatisticsByConditions(
            genConditions(
                inAppIdList,
                notInAppIdList,
                resource,
                dimension,
                dimensionValue,
                date
            )
        );
    }

    @Override
    protected List<Condition> getBasicConditions() {
        return new ArrayList<>();
    }
}
