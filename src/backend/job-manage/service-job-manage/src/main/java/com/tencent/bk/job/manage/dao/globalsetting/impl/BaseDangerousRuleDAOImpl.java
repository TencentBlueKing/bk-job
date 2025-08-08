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

import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.tables.DangerousRule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 高危语句规则操作基础DAO实现，封装部分公共逻辑
 */
@Slf4j
abstract class BaseDangerousRuleDAOImpl {

    protected static final DangerousRule T = DangerousRule.DANGEROUS_RULE;
    protected final DSLContext dslContext;

    protected static final TableField<?, ?>[] ALL_FIELDS = {
        T.ID,
        T.EXPRESSION,
        T.DESCRIPTION,
        T.PRIORITY,
        T.SCRIPT_TYPE,
        T.CREATOR,
        T.CREATE_TIME,
        T.LAST_MODIFY_USER,
        T.LAST_MODIFY_TIME,
        T.ACTION,
        T.STATUS,
    };

    public BaseDangerousRuleDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }


    abstract protected List<Condition> getBasicConditions();

    protected List<DangerousRuleDTO> listDangerousRulesByConditions(Integer scriptType, List<Condition> conditions) {
        val records =
            dslContext.select(ALL_FIELDS)
                .from(T)
                .where(conditions)
                .orderBy(T.PRIORITY)
                .fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<DangerousRuleDTO> dangerousRuleList = records.map(this::convertRecordToDto);
            if (scriptType == null) {
                return dangerousRuleList;
            }
            int typeFlag = 1 << scriptType - 1;
            return dangerousRuleList.stream()
                .filter(rule -> (rule.getScriptType() & (typeFlag)) == typeFlag)
                .collect(Collectors.toList());
        }
    }

    protected DangerousRuleDTO convertRecordToDto(Record record) {
        return new DangerousRuleDTO(
            record.get(T.ID),
            record.get(T.EXPRESSION),
            record.get(T.DESCRIPTION),
            record.get(T.PRIORITY),
            record.get(T.SCRIPT_TYPE),
            record.get(T.CREATOR),
            record.get(T.CREATE_TIME).longValue(),
            record.get(T.LAST_MODIFY_USER),
            record.get(T.LAST_MODIFY_TIME).longValue(),
            record.get(T.ACTION).intValue(),
            record.get(T.STATUS).intValue()
        );
    }

}
