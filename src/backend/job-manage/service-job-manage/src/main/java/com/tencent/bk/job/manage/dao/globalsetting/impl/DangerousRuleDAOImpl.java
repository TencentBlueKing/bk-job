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

package com.tencent.bk.job.manage.dao.globalsetting.impl;

import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.DangerousRule;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DangerousRuleDAOImpl implements DangerousRuleDAO {

    private static final DangerousRule T = DangerousRule.DANGEROUS_RULE;

    private void setDefaultValue(DangerousRuleDTO dangerousRuleDTO) {
        if (dangerousRuleDTO.getScriptType() == null) {
            dangerousRuleDTO.setScriptType(ScriptTypeEnum.SHELL.getValue());
        }
    }

    @Override
    public Long insertDangerousRule(DSLContext dslContext, DangerousRuleDTO dangerousRuleDTO) {
        setDefaultValue(dangerousRuleDTO);
        val query = dslContext.insertInto(T,
            T.EXPRESSION,
            T.DESCRIPTION,
            T.PRIORITY,
            T.SCRIPT_TYPE,
            T.CREATOR,
            T.CREATE_TIME,
            T.LAST_MODIFY_USER,
            T.LAST_MODIFY_TIME,
            T.ACTION,
            T.STATUS
        ).values(
            dangerousRuleDTO.getExpression(),
            dangerousRuleDTO.getDescription(),
            dangerousRuleDTO.getPriority(),
            dangerousRuleDTO.getScriptType(),
            dangerousRuleDTO.getCreator(),
            ULong.valueOf(dangerousRuleDTO.getCreateTime()),
            dangerousRuleDTO.getLastModifier(),
            ULong.valueOf(dangerousRuleDTO.getLastModifyTime()),
            JooqDataTypeUtil.getByteFromInteger(dangerousRuleDTO.getAction()),
            JooqDataTypeUtil.getByteFromInteger(dangerousRuleDTO.getStatus())
        ).returning(T.ID);
        try {
            return query.fetchOne().getId();
        } catch (Exception e) {
            val sql = query.getSQL(ParamType.INLINED);
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateDangerousRule(DSLContext dslContext, DangerousRuleDTO dangerousRuleDTO) {
        val query = dslContext.update(T)
            .set(T.EXPRESSION, dangerousRuleDTO.getExpression())
            .set(T.DESCRIPTION, dangerousRuleDTO.getDescription())
            .set(T.PRIORITY, dangerousRuleDTO.getPriority())
            .set(T.SCRIPT_TYPE, dangerousRuleDTO.getScriptType())
            .set(T.LAST_MODIFY_USER, dangerousRuleDTO.getLastModifier())
            .set(T.LAST_MODIFY_TIME, ULong.valueOf(System.currentTimeMillis()))
            .set(T.ACTION, JooqDataTypeUtil.getByteFromInteger(dangerousRuleDTO.getAction()))
            .set(T.STATUS, JooqDataTypeUtil.getByteFromInteger(dangerousRuleDTO.getStatus()))
            .where(T.ID.eq(dangerousRuleDTO.getId()));
        try {
            return query.execute();
        } catch (Exception e) {
            val sql = query.getSQL(ParamType.INLINED);
            log.error(sql);
            throw e;
        }
    }


    @Override
    public int deleteDangerousRuleById(DSLContext dslContext, Long id) {
        return dslContext.deleteFrom(T).where(
            T.ID.eq(id)
        ).execute();
    }

    @Override
    public DangerousRuleDTO getDangerousRuleById(DSLContext dslContext, Long id) {
        val record = dslContext.selectFrom(T).where(
            T.ID.eq(id)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public DangerousRuleDTO getDangerousRuleByPriority(DSLContext dslContext, int priority) {
        val record = dslContext.selectFrom(T).where(
            T.PRIORITY.eq(priority)
        ).fetchOne();
        if (record == null) {
            return null;
        } else {
            return convertRecordToDto(record);
        }
    }

    @Override
    public List<DangerousRuleDTO> listDangerousRules(DSLContext dslContext) {
        val records = dslContext.selectFrom(T).orderBy(T.PRIORITY).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<DangerousRuleDTO> listDangerousRules(DSLContext dslContext, DangerousRuleDTO dangerousRuleQuery) {
        Integer scriptType = dangerousRuleQuery.getScriptType();
        List<Condition> conditions = new ArrayList<>();
        if (dangerousRuleQuery.getStatus() != null) {
            conditions.add(T.STATUS.eq(JooqDataTypeUtil.getByteFromInteger(dangerousRuleQuery.getStatus())));
        }
        val records =
            dslContext.selectFrom(T).where(conditions).orderBy(T.PRIORITY).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            List<DangerousRuleDTO> dangerousRuleList = records.map(this::convertRecordToDto);
            if (scriptType == null) {
                return dangerousRuleList;
            }
            int typeFlag = 1 << scriptType - 1;
            return dangerousRuleList.stream().filter(rule -> (rule.getScriptType() & (typeFlag)) == typeFlag).collect(Collectors.toList());
        }
    }

    @Override
    public int getMaxPriority(DSLContext dslContext) {
        val record = dslContext.select(DSL.max(T.PRIORITY)).from(T).fetchOne();
        if (record == null || record.value1() == null) {
            return 0;
        } else {
            return record.value1();
        }
    }

    @Override
    public int getMinPriority(DSLContext dslContext) {
        val record = dslContext.select(DSL.min(T.PRIORITY)).from(T).fetchOne();
        if (record == null || record.value1() == null) {
            return 0;
        } else {
            return record.value1();
        }
    }

    private DangerousRuleDTO convertRecordToDto(Record record) {
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
            record.get(T.STATUS).intValue());
    }
}
