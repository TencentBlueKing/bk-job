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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.dao.AIChatHistoryDAO;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.tables.AiChatHistory;
import com.tencent.bk.job.common.mysql.dao.BaseDAOImpl;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 仿照AnalysisTaskDAOImpl实现一个AIChatHistoryDAOImpl
 */
@Slf4j
@Repository
public class AIChatHistoryDAOImpl extends BaseDAOImpl implements AIChatHistoryDAO {

    private static final AiChatHistory defaultTable = AiChatHistory.AI_CHAT_HISTORY;

    private final DSLContext dslContext;

    public AIChatHistoryDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    /**
     * 插入AI对话历史记录
     *
     * @param aiChatHistoryDTO AI对话历史记录
     * @return 记录ID
     */
    @SuppressWarnings("DataFlowIssue")
    @Override
    public Long insertAIChatHistory(AIChatHistoryDTO aiChatHistoryDTO) {
        val query = dslContext.insertInto(defaultTable,
                defaultTable.USERNAME,
                defaultTable.USER_INPUT,
                defaultTable.PROMPT_TEMPLATE_ID,
                defaultTable.AI_INPUT,
                defaultTable.STATUS,
                defaultTable.AI_ANSWER,
                defaultTable.ERROR_CODE,
                defaultTable.ERROR_MESSAGE,
                defaultTable.START_TIME,
                defaultTable.ANSWER_TIME,
                defaultTable.TOTAL_TIME,
                defaultTable.IS_DELETED
            )
            .values(
                aiChatHistoryDTO.getUsername(),
                aiChatHistoryDTO.getUserInput(),
                aiChatHistoryDTO.getPromptTemplateId(),
                aiChatHistoryDTO.getAiInput(),
                JooqDataTypeUtil.getByteFromInteger(aiChatHistoryDTO.getStatus()),
                aiChatHistoryDTO.getAiAnswer(),
                aiChatHistoryDTO.getErrorCode(),
                aiChatHistoryDTO.getErrorMessage(),
                JooqDataTypeUtil.buildULong(aiChatHistoryDTO.getStartTime()),
                JooqDataTypeUtil.buildULong(aiChatHistoryDTO.getAnswerTime()),
                JooqDataTypeUtil.buildULong(aiChatHistoryDTO.getTotalTime()),
                JooqDataTypeUtil.buildUByte(aiChatHistoryDTO.getIsDeleted() ? 1 : 0)
            )
            .returning(defaultTable.ID);
        val sql = query.getSQL(ParamType.INLINED);
        try {
            return query.fetchOne().getId();
        } catch (Exception e) {
            log.error(sql);
            throw e;
        }
    }

    @Override
    public int updateChatHistoryStatus(Long historyId, int status) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, JooqDataTypeUtil.getByteFromInteger(status))
            .where(defaultTable.ID.eq(historyId))
            .execute();
    }

    @Override
    public int updateChatHistoryStatusAndAIAnswer(Long historyId,
                                                  Integer status,
                                                  String aiAnswer,
                                                  String errorCode,
                                                  String errorMessage,
                                                  Long aiAnswerTime) {
        return dslContext.update(defaultTable)
            .set(defaultTable.STATUS, JooqDataTypeUtil.getByteFromInteger(status))
            .set(defaultTable.AI_ANSWER, aiAnswer)
            .set(defaultTable.ERROR_CODE, errorCode)
            .set(defaultTable.ERROR_MESSAGE, errorMessage)
            .set(defaultTable.ANSWER_TIME, JooqDataTypeUtil.buildULong(aiAnswerTime))
            .where(defaultTable.ID.eq(historyId))
            .execute();
    }

    @Override
    public List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.eq(username));
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        return listByConditions(conditions, start, length);
    }

    @Override
    public List<AIChatHistoryDTO> getLatestFinishedChatHistoryList(String username, Integer start, Integer length) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.eq(username));
        conditions.add(defaultTable.STATUS.eq((byte) AIChatStatusEnum.FINISHED.getStatus()));
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        return listByConditions(conditions, start, length);
    }

    @Override
    public AIChatHistoryDTO getChatHistory(String username, Long id) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.eq(username));
        conditions.add(defaultTable.ID.eq(id));
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        val query = dslContext.select(
                defaultTable.ID,
                defaultTable.USERNAME,
                defaultTable.USER_INPUT,
                defaultTable.PROMPT_TEMPLATE_ID,
                defaultTable.AI_INPUT,
                defaultTable.STATUS,
                defaultTable.AI_ANSWER,
                defaultTable.ERROR_CODE,
                defaultTable.ERROR_MESSAGE,
                defaultTable.START_TIME,
                defaultTable.ANSWER_TIME,
                defaultTable.TOTAL_TIME
            )
            .from(defaultTable)
            .where(conditions);
        val record = query.fetchOne();
        if (record == null) {
            return null;
        }
        return convertRecordToDto(record);
    }

    @Override
    public int softDeleteChatHistory(String username, Integer limit) {
        Collection<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.eq(username));
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        return dslContext.update(defaultTable)
            .set(defaultTable.IS_DELETED, JooqDataTypeUtil.buildUByte(1))
            .where(conditions)
            .orderBy(defaultTable.START_TIME)
            .limit(limit)
            .execute();
    }

    @Override
    public Long getMaxIdBeforeOrEqualStartTime(long maxStartTime) {
        Long existStartTime = dslContext.select(defaultTable.START_TIME)
            .from(defaultTable)
            .where(defaultTable.START_TIME.lessOrEqual(JooqDataTypeUtil.buildULong(maxStartTime)))
            .orderBy(defaultTable.START_TIME.desc())
            .limit(1)
            .fetchOneInto(Long.class);
        if (existStartTime == null) {
            return null;
        }
        Long maxId = dslContext.select(DSL.max(defaultTable.ID))
            .from(defaultTable)
            .where(defaultTable.START_TIME.equal(JooqDataTypeUtil.buildULong(existStartTime)))
            .fetchOneInto(Long.class);
        if (log.isDebugEnabled()) {
            log.debug("existStartTime={},maxId={}", existStartTime, maxId);
        }
        return maxId;
    }

    @Override
    public int deleteChatHistory(long maxId, int limit) {
        return deleteChatHistory(null, maxId, limit);
    }

    @Override
    public int deleteChatHistory(String username, long maxId, int limit) {
        List<Condition> conditions = new ArrayList<>();
        if (StringUtils.isNotBlank(username)) {
            conditions.add(defaultTable.USERNAME.eq(username));
        }
        conditions.add(defaultTable.ID.lessOrEqual(maxId));
        return dslContext.deleteFrom(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.ID)
            .limit(limit)
            .execute();
    }

    @Override
    public List<String> listAllUserOfChatHistory() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        return dslContext.selectDistinct(defaultTable.USERNAME)
            .from(defaultTable)
            .where(conditions)
            .fetch()
            .into(String.class);
    }

    @Override
    public Long getFirstIdAfterOffset(String username, int offset) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(defaultTable.USERNAME.eq(username));
        conditions.add(defaultTable.IS_DELETED.eq(JooqDataTypeUtil.buildUByte(0)));
        return dslContext.select(defaultTable.ID)
            .from(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.ID.desc())
            .offset(offset)
            .limit(1)
            .fetchOneInto(Long.class);
    }

    private List<AIChatHistoryDTO> listByConditions(Collection<Condition> conditions,
                                                    Integer start,
                                                    Integer length) {
        val query = dslContext.select(
                defaultTable.ID,
                defaultTable.USERNAME,
                defaultTable.USER_INPUT,
                defaultTable.PROMPT_TEMPLATE_ID,
                defaultTable.AI_INPUT,
                defaultTable.STATUS,
                defaultTable.AI_ANSWER,
                defaultTable.ERROR_CODE,
                defaultTable.ERROR_MESSAGE,
                defaultTable.START_TIME,
                defaultTable.ANSWER_TIME,
                defaultTable.TOTAL_TIME
            )
            .from(defaultTable)
            .where(conditions)
            .orderBy(defaultTable.START_TIME.desc());
        return listPage(query, start, length, this::convertRecordToDto);
    }

    private AIChatHistoryDTO convertRecordToDto(Record record) {
        AIChatHistoryDTO aiChatHistoryDTO = new AIChatHistoryDTO();
        aiChatHistoryDTO.setId(record.get(defaultTable.ID));
        aiChatHistoryDTO.setUsername(record.get(defaultTable.USERNAME));
        aiChatHistoryDTO.setUserInput(record.get(defaultTable.USER_INPUT));
        aiChatHistoryDTO.setPromptTemplateId(record.get(defaultTable.PROMPT_TEMPLATE_ID));
        aiChatHistoryDTO.setAiInput(record.get(defaultTable.AI_INPUT));
        aiChatHistoryDTO.setStatus(JooqDataTypeUtil.getIntegerFromByte(record.get(defaultTable.STATUS)));
        aiChatHistoryDTO.setAiAnswer(record.get(defaultTable.AI_ANSWER));
        aiChatHistoryDTO.setErrorCode(record.get(defaultTable.ERROR_CODE));
        aiChatHistoryDTO.setErrorMessage(record.get(defaultTable.ERROR_MESSAGE));
        aiChatHistoryDTO.setStartTime(JooqDataTypeUtil.buildLong(record.get(defaultTable.START_TIME)));
        aiChatHistoryDTO.setAnswerTime(JooqDataTypeUtil.buildLong(record.get(defaultTable.ANSWER_TIME)));
        aiChatHistoryDTO.setTotalTime(JooqDataTypeUtil.buildLong(record.get(defaultTable.TOTAL_TIME)));
        return aiChatHistoryDTO;
    }
}
