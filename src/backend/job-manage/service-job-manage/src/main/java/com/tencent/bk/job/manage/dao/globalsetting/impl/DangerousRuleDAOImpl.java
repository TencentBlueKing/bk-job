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

import com.tencent.bk.job.manage.api.common.constants.EnableStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import com.tencent.bk.job.manage.model.query.DangerousRuleQuery;
import com.tencent.bk.job.manage.model.tables.DangerousRule;
import com.tencent.bk.job.manage.model.tables.records.DangerousRuleRecord;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class DangerousRuleDAOImpl implements DangerousRuleDAO {

    private static final DangerousRule T = DangerousRule.DANGEROUS_RULE;
    private final DSLContext dslContext;

    @Autowired
    public DangerousRuleDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    private void setDefaultValue(DangerousRuleDTO dangerousRuleDTO) {
        if (dangerousRuleDTO.getScriptType() == null) {
            dangerousRuleDTO.setScriptType(ScriptTypeEnum.SHELL.getValue());
        }
    }

    @Override
    public Long insertDangerousRule(DangerousRuleDTO dangerousRuleDTO) {
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
    public int updateDangerousRule(DangerousRuleDTO dangerousRuleDTO) {
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
    public int deleteDangerousRuleById(Long id) {
        return dslContext.deleteFrom(T).where(
            T.ID.eq(id)
        ).execute();
    }

    @Override
    public DangerousRuleDTO getDangerousRuleById(Long id) {
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
    public DangerousRuleDTO getDangerousRuleByPriority(int priority) {
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
    public List<DangerousRuleDTO> listDangerousRules() {
        val records = dslContext.selectFrom(T).orderBy(T.PRIORITY).fetch();
        if (records.isEmpty()) {
            return Collections.emptyList();
        } else {
            return records.map(this::convertRecordToDto);
        }
    }

    @Override
    public List<DangerousRuleDTO> listDangerousRules(DangerousRuleDTO dangerousRuleQuery) {
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
            return dangerousRuleList.stream()
                .filter(rule -> (rule.getScriptType() & (typeFlag)) == typeFlag)
                .collect(Collectors.toList());
        }
    }

    @Override
    public List<DangerousRuleDTO> listDangerousRules(DangerousRuleQuery query) {
        List<Condition> conditions = buildConditionList(query);
        Result<DangerousRuleRecord> records = dslContext.selectFrom(T)
            .where(conditions)
            .orderBy(T.PRIORITY)
            .fetch();
        return records.map(this::convertRecordToDto);
    }

    private List<Condition> buildConditionList(DangerousRuleQuery query) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(query.getExpression())) {
            conditions.add(T.EXPRESSION.like("%" + query.getExpression() + "%"));
        }
        if (StringUtils.isNotBlank(query.getDescription())) {
            conditions.add(T.DESCRIPTION.like("%" + query.getDescription() + "%"));
        }
        if (query.getScriptTypeList() != null) {
            List<Byte> typeList = query.getScriptTypeList();
            int scriptType = 0;
            for (Byte type : typeList) {
                if (type > 0 && type <= 8) {
                    scriptType |= 1 << (type - 1);
                }
            }
            conditions.add(DSL.bitAnd(T.SCRIPT_TYPE, scriptType).greaterThan(0));
        }
        if (query.getAction() != null) {
            conditions.add(T.ACTION.in(query.getAction()));
        }
        return conditions;
    }

    @Override
    public int getMaxPriority() {
        val record = dslContext.select(DSL.max(T.PRIORITY)).from(T).fetchOne();
        if (record == null || record.value1() == null) {
            return 0;
        } else {
            return record.value1();
        }
    }

    @Override
    public int getMinPriority() {
        val record = dslContext.select(DSL.min(T.PRIORITY)).from(T).fetchOne();
        if (record == null || record.value1() == null) {
            return 0;
        } else {
            return record.value1();
        }
    }

    @Override
    public int updateDangerousRuleStatus(String userName, Long id, EnableStatusEnum status) {
        val query = dslContext.update(T)
            .set(T.LAST_MODIFY_USER, userName)
            .set(T.LAST_MODIFY_TIME, ULong.valueOf(System.currentTimeMillis()))
            .set(T.STATUS, (byte) status.getValue())
            .where(T.ID.eq(id));
        try {
            return query.execute();
        } catch (Exception e) {
            val sql = query.getSQL(ParamType.INLINED);
            log.error(sql);
            throw e;
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
