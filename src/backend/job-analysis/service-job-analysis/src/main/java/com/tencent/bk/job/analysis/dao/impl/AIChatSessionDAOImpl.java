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

import com.tencent.bk.job.analysis.dao.AIChatSessionDAO;
import com.tencent.bk.job.analysis.model.dto.AIChatSessionDTO;
import com.tencent.bk.job.analysis.model.tables.AiChatSession;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class AIChatSessionDAOImpl implements AIChatSessionDAO {

    private static final AiChatSession defaultTable = AiChatSession.AI_CHAT_SESSION;

    private final DSLContext dslContext;

    public AIChatSessionDAOImpl(@Qualifier("job-analysis-dsl-context") DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public AIChatSessionDTO getSession(Long appId, String username, Integer sceneType, String sceneResourceId) {
        Record record = dslContext.select(
                defaultTable.ID,
                defaultTable.APP_ID,
                defaultTable.USERNAME,
                defaultTable.SCENE_TYPE,
                defaultTable.SCENE_RESOURCE_ID,
                defaultTable.AI_SESSION_ID,
                defaultTable.SESSION_NAME,
                defaultTable.CREATE_TIME,
                defaultTable.UPDATE_TIME
            )
            .from(defaultTable)
            .where(defaultTable.APP_ID.eq(appId))
            .and(defaultTable.USERNAME.eq(username))
            .and(defaultTable.SCENE_TYPE.eq(sceneType.byteValue()))
            .and(defaultTable.SCENE_RESOURCE_ID.eq(sceneResourceId))
            .fetchOne();
        if (record == null) {
            return null;
        }
        return convertToDTO(record);
    }

    @Override
    public void upsertSession(AIChatSessionDTO dto) {
        ULong now = JooqDataTypeUtil.buildULong(System.currentTimeMillis());
        dslContext.insertInto(defaultTable)
            .set(defaultTable.APP_ID, dto.getAppId())
            .set(defaultTable.USERNAME, dto.getUsername())
            .set(defaultTable.SCENE_TYPE, dto.getSceneType().byteValue())
            .set(defaultTable.SCENE_RESOURCE_ID, dto.getSceneResourceId())
            .set(defaultTable.AI_SESSION_ID, dto.getAiSessionId())
            .set(defaultTable.SESSION_NAME, dto.getSessionName())
            .set(defaultTable.CREATE_TIME, now)
            .set(defaultTable.UPDATE_TIME, now)
            .onDuplicateKeyUpdate()
            .set(defaultTable.AI_SESSION_ID, dto.getAiSessionId())
            .set(defaultTable.SESSION_NAME, dto.getSessionName())
            .set(defaultTable.UPDATE_TIME, now)
            .execute();
    }

    @Override
    public int deleteByCreateTimeBefore(long maxCreateTime, int limit) {
        return dslContext.deleteFrom(defaultTable)
            .where(defaultTable.CREATE_TIME.lessOrEqual(JooqDataTypeUtil.buildULong(maxCreateTime)))
            .orderBy(defaultTable.CREATE_TIME)
            .limit(limit)
            .execute();
    }

    private AIChatSessionDTO convertToDTO(Record record) {
        AIChatSessionDTO dto = new AIChatSessionDTO();
        dto.setId(record.get(defaultTable.ID));
        dto.setAppId(record.get(defaultTable.APP_ID));
        dto.setUsername(record.get(defaultTable.USERNAME));
        dto.setSceneType(record.get(defaultTable.SCENE_TYPE).intValue());
        dto.setSceneResourceId(record.get(defaultTable.SCENE_RESOURCE_ID));
        dto.setAiSessionId(record.get(defaultTable.AI_SESSION_ID));
        dto.setSessionName(record.get(defaultTable.SESSION_NAME));
        dto.setCreateTime(record.get(defaultTable.CREATE_TIME).longValue());
        dto.setUpdateTime(record.get(defaultTable.UPDATE_TIME).longValue());
        return dto;
    }
}
