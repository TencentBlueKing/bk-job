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

package com.tencent.bk.job.execute.dao.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.execute.dao.DangerousRecordDAO;
import com.tencent.bk.job.execute.model.DangerousRecordDTO;
import com.tencent.bk.job.execute.model.ScriptCheckResultDTO;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.generated.tables.DangerousRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class DangerousRecordDAOImpl implements DangerousRecordDAO {

    private static final DangerousRecord T = DangerousRecord.DANGEROUS_RECORD;
    private final DSLContext ctx;

    @Autowired
    public DangerousRecordDAOImpl(@Qualifier("job-execute-dsl-context") DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public PageData<DangerousRecordDTO> listPageDangerousRecord(DangerousRecordDTO query,
                                                                BaseSearchCondition baseSearchCondition) {
        int count = getPageDangerousRecordCount(query, baseSearchCondition);
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result result = ctx.select(T.ID, T.RULE_ID, T.RULE_EXPRESSION, T.APP_ID, T.APP_NAME, T.OPERATOR,
            T.SCRIPT_LANGUAGE,
            T.SCRIPT_CONTENT, T.CREATE_TIME, T.STARTUP_MODE, T.CLIENT, T.ACTION, T.CHECK_RESULT, T.EXT_DATA)
            .from(T)
            .where(buildSearchCondition(query, baseSearchCondition))
            .orderBy(T.ID.desc())
            .limit(start, length)
            .fetch();
        return buildDangerousRecordPageData(start, length, count, result);
    }

    @SuppressWarnings("all")
    private int getPageDangerousRecordCount(DangerousRecordDTO query, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildSearchCondition(query, baseSearchCondition);
        return ctx.selectCount().from(T).where(conditions).fetchOne(0, Integer.class);
    }

    private List<Condition> buildSearchCondition(DangerousRecordDTO query, BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        if (baseSearchCondition.getCreateTimeStart() != null) {
            conditions.add(T.CREATE_TIME.ge(baseSearchCondition.getCreateTimeStart()));
        }
        if (baseSearchCondition.getCreateTimeEnd() != null) {
            conditions.add(T.CREATE_TIME.le(baseSearchCondition.getCreateTimeEnd()));
        }
        if (query.getId() != null && query.getId() > 0) {
            conditions.add(T.ID.eq(query.getId()));
            return conditions;
        }
        if (StringUtils.isNotEmpty(query.getRuleExpression())) {
            conditions.add(T.RULE_EXPRESSION.like("%" + query.getRuleExpression() + "%"));
        }
        if (StringUtils.isNotEmpty(query.getOperator())) {
            conditions.add(T.OPERATOR.eq(query.getOperator()));
        }
        if (query.getStartupMode() != null) {
            conditions.add(T.STARTUP_MODE.eq(JooqDataTypeUtil.toByte(query.getStartupMode())));
        }
        if (query.getAction() != null) {
            conditions.add(T.ACTION.eq(JooqDataTypeUtil.toByte(query.getAction())));
        }
        if (StringUtils.isNotEmpty(query.getClient())) {
            conditions.add(T.CLIENT.eq(query.getClient()));
        }
        if (query.getAppId() != null) {
            conditions.add(T.APP_ID.eq(query.getAppId()));
        }
        return conditions;
    }

    private PageData<DangerousRecordDTO> buildDangerousRecordPageData(int start, int length, int count, Result result) {
        List<DangerousRecordDTO> records = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.into(record -> records.add(extractInfo(record)));
        }
        PageData<DangerousRecordDTO> pageData = new PageData<>();
        pageData.setData(records);
        pageData.setStart(start);
        pageData.setPageSize(length);
        pageData.setTotal(Long.valueOf(String.valueOf(count)));
        return pageData;
    }

    private DangerousRecordDTO extractInfo(Record record) {
        if (record == null) {
            return null;
        }
        DangerousRecordDTO dangerousRecord = new DangerousRecordDTO();
        dangerousRecord.setId(record.get(T.ID));
        dangerousRecord.setRuleId(record.get(T.RULE_ID));
        dangerousRecord.setRuleExpression(record.get(T.RULE_EXPRESSION));
        dangerousRecord.setAppId(record.get(T.APP_ID));
        dangerousRecord.setAppName(record.get(T.APP_NAME));
        dangerousRecord.setOperator(record.get(T.OPERATOR));
        dangerousRecord.setCreateTime(record.get(T.CREATE_TIME));
        dangerousRecord.setStartupMode(JooqDataTypeUtil.toInteger(record.get(T.STARTUP_MODE)));
        dangerousRecord.setClient(record.get(T.CLIENT));
        dangerousRecord.setAction(JooqDataTypeUtil.toInteger(record.get(T.ACTION)));
        dangerousRecord.setScriptLanguage(JooqDataTypeUtil.toInteger(record.get(T.SCRIPT_LANGUAGE)));
        dangerousRecord.setScriptContent(record.get(T.SCRIPT_CONTENT));
        dangerousRecord.setCheckResult(JsonUtils.fromJson(record.get(T.CHECK_RESULT),
            new TypeReference<ScriptCheckResultDTO>() {
        }));
        String extData = record.get(T.EXT_DATA);
        if (StringUtils.isNotEmpty(extData)) {
            dangerousRecord.setExtData(JsonUtils.fromJson(extData, new TypeReference<Map<String, String>>() {
            }));
        }
        return dangerousRecord;
    }

    @Override
    public boolean saveDangerousRecord(DangerousRecordDTO record) {
        int count = ctx.insertInto(T, T.RULE_ID, T.RULE_EXPRESSION, T.APP_ID, T.APP_NAME, T.OPERATOR, T.SCRIPT_LANGUAGE,
            T.SCRIPT_CONTENT, T.CREATE_TIME, T.STARTUP_MODE, T.CLIENT, T.ACTION, T.CHECK_RESULT, T.EXT_DATA)
            .values(record.getRuleId(),
                record.getRuleExpression(),
                record.getAppId(),
                record.getAppName(),
                record.getOperator(),
                JooqDataTypeUtil.toByte(record.getScriptLanguage()),
                record.getScriptContent(),
                record.getCreateTime(),
                JooqDataTypeUtil.toByte(record.getStartupMode()),
                record.getClient(),
                JooqDataTypeUtil.toByte(record.getAction()), JsonUtils.toJson(record.getCheckResult()),
                record.getExtData() == null ? "" : JsonUtils.toJson(record.getExtData()))
            .execute();
        return count > 0;
    }
}
