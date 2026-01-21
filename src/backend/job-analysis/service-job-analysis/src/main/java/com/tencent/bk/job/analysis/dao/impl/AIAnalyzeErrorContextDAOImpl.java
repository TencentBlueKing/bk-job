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

import com.tencent.bk.job.analysis.dao.AIAnalyzeErrorContextDAO;
import com.tencent.bk.job.analysis.model.dto.AIAnalyzeErrorContextDTO;
import com.tencent.bk.job.analysis.model.tables.AiAnalyzeErrorContext;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class AIAnalyzeErrorContextDAOImpl implements AIAnalyzeErrorContextDAO {

    private static final AiAnalyzeErrorContext defaultTable = AiAnalyzeErrorContext.AI_ANALYZE_ERROR_CONTEXT;

    private final DSLContext dslContext;

    public AIAnalyzeErrorContextDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public int insert(AIAnalyzeErrorContextDTO aiAnalyzeErrorContextDTO) {
        val query = dslContext.insertInto(defaultTable,
                defaultTable.AI_CHAT_HISTORY_ID,
                defaultTable.TASK_INSTANCE_ID,
                defaultTable.STEP_INSTANCE_ID,
                defaultTable.EXECUTE_COUNT,
                defaultTable.BATCH,
                defaultTable.EXECUTE_OBJECT_TYPE,
                defaultTable.EXECUTE_OBJECT_RESOURCE_ID,
                defaultTable.MODE
            )
            .values(
                aiAnalyzeErrorContextDTO.getAiChatHistoryId(),
                aiAnalyzeErrorContextDTO.getTaskInstanceId(),
                aiAnalyzeErrorContextDTO.getStepInstanceId(),
                aiAnalyzeErrorContextDTO.getExecuteCount(),
                JooqDataTypeUtil.getShortFromInteger(aiAnalyzeErrorContextDTO.getBatch()),
                JooqDataTypeUtil.getByteFromInteger(aiAnalyzeErrorContextDTO.getExecuteObjectType()),
                aiAnalyzeErrorContextDTO.getExecuteObjectResourceId(),
                JooqDataTypeUtil.getByteFromInteger(aiAnalyzeErrorContextDTO.getMode())
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
    public int delete(List<Long> chatHistoryIdList) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.AI_CHAT_HISTORY_ID.in(chatHistoryIdList));
        return dslContext.deleteFrom(defaultTable)
            .where(conditions)
            .execute();
    }

    private AIAnalyzeErrorContextDTO convertRecordToDto(Record record) {
        AIAnalyzeErrorContextDTO aiAnalyzeErrorContextDTO = new AIAnalyzeErrorContextDTO();
        aiAnalyzeErrorContextDTO.setTaskInstanceId(record.get(defaultTable.TASK_INSTANCE_ID));
        aiAnalyzeErrorContextDTO.setStepInstanceId(record.get(defaultTable.STEP_INSTANCE_ID));
        aiAnalyzeErrorContextDTO.setExecuteCount(record.get(defaultTable.EXECUTE_COUNT));
        aiAnalyzeErrorContextDTO.setBatch(JooqDataTypeUtil.getIntegerFromShort(record.get(defaultTable.BATCH)));
        aiAnalyzeErrorContextDTO.setExecuteObjectType(
            JooqDataTypeUtil.getIntegerFromByte(record.get(defaultTable.EXECUTE_OBJECT_TYPE))
        );
        aiAnalyzeErrorContextDTO.setExecuteObjectResourceId(record.get(defaultTable.EXECUTE_OBJECT_RESOURCE_ID));
        aiAnalyzeErrorContextDTO.setMode(
            JooqDataTypeUtil.getIntegerFromByte(record.get(defaultTable.MODE))
        );
        return aiAnalyzeErrorContextDTO;
    }

}
