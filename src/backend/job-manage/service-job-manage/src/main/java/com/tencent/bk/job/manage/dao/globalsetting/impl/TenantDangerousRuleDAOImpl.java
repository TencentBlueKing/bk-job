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

import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.globalsetting.TenantDangerousRuleDAO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 对指定租户的高危语句规则进行操作的DAO实现
 */
@Repository
@Slf4j
public class TenantDangerousRuleDAOImpl extends BaseDangerousRuleDAOImpl implements TenantDangerousRuleDAO {

    @Autowired
    public TenantDangerousRuleDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        super(dslContext);
    }


    protected List<Condition> getBasicConditions() {
        return new ArrayList<>();
    }

    private List<Condition> buildTenantIdConditions(String tenantId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(T.TENANT_ID.eq(tenantId));
        return conditions;
    }

    @Override
    public List<DangerousRuleDTO> listDangerousRules(String tenantId, DangerousRuleDTO dangerousRuleQuery) {
        Integer scriptType = dangerousRuleQuery.getScriptType();
        List<Condition> conditions = buildTenantIdConditions(tenantId);
        if (dangerousRuleQuery.getStatus() != null) {
            conditions.add(T.STATUS.eq(JooqDataTypeUtil.getByteFromInteger(dangerousRuleQuery.getStatus())));
        }
        return listDangerousRulesByConditions(scriptType, conditions);
    }

}
