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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.dao.AIChatHistoryDAO;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * AI聊天记录管理服务
 */
@Slf4j
@Service
public class AIChatHistoryServiceImpl implements AIChatHistoryService {

    private final AIChatHistoryDAO aiChatHistoryDAO;

    @Autowired
    public AIChatHistoryServiceImpl(AIChatHistoryDAO aiChatHistoryDAO) {
        this.aiChatHistoryDAO = aiChatHistoryDAO;
    }

    @Override
    public AIChatHistoryDTO buildAIChatHistoryDTO(String username,
                                                  Long appId,
                                                  Long startTime,
                                                  AIPromptDTO aiPromptDTO,
                                                  Integer status,
                                                  AIAnswer aiAnswer) {
        AIChatHistoryDTO aiChatHistoryDTO = new AIChatHistoryDTO();
        aiChatHistoryDTO.setUsername(username);
        aiChatHistoryDTO.setAppId(appId);
        aiChatHistoryDTO.setUserInput(aiPromptDTO.getRawPrompt());
        aiChatHistoryDTO.setPromptTemplateId(aiPromptDTO.getPromptTemplateId());
        aiChatHistoryDTO.setAiInput(aiPromptDTO.getRenderedPrompt());
        aiChatHistoryDTO.setStatus(status);
        if (aiAnswer != null) {
            aiChatHistoryDTO.setAiAnswer(aiAnswer.getContent());
            aiChatHistoryDTO.setErrorCode(String.valueOf(aiAnswer.getErrorCode()));
            aiChatHistoryDTO.setErrorMessage(aiAnswer.getErrorMessage());
            aiChatHistoryDTO.setAnswerTime(System.currentTimeMillis());
        }
        aiChatHistoryDTO.setStartTime(startTime);
        aiChatHistoryDTO.updateTotalTime();
        aiChatHistoryDTO.setIsDeleted(false);
        return aiChatHistoryDTO;
    }

    @Override
    public Long insertChatHistory(AIChatHistoryDTO aiChatHistoryDTO) {
        return aiChatHistoryDAO.insertAIChatHistory(aiChatHistoryDTO);
    }

    @Override
    public boolean existsChatHistory(String username) {
        return aiChatHistoryDAO.existsChatHistory(username);
    }

    @Override
    public int setAIAnswerReplying(Long historyId) {
        return aiChatHistoryDAO.updateChatHistoryStatus(historyId, AIChatStatusEnum.REPLYING.getStatus());
    }

    @Override
    public int finishAIAnswer(Long historyId, AIAnswer aiAnswer) {
        return aiChatHistoryDAO.updateChatHistoryStatusAndAIAnswer(
            historyId,
            AIChatStatusEnum.FINISHED.getStatus(),
            aiAnswer.getContent(),
            aiAnswer.getErrorCode(),
            aiAnswer.getErrorMessage(),
            System.currentTimeMillis()
        );
    }

    @Override
    public int terminateAIAnswer(Long historyId) {
        return aiChatHistoryDAO.updateChatHistoryStatusAndAIAnswer(
            historyId,
            AIChatStatusEnum.TERMINATED.getStatus(),
            AIChatStatusEnum.TERMINATED.getDescription(),
            "1",
            null,
            System.currentTimeMillis()
        );
    }

    /**
     * 从DB获取最近的聊天记录列表，按起始时间升序排列
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近的聊天记录列表
     */
    @Override
    public List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length) {
        List<AIChatHistoryDTO> aiChatHistoryList = aiChatHistoryDAO.getLatestChatHistoryList(username, start, length);
        aiChatHistoryList.sort(Comparator.comparing(AIChatHistoryDTO::getStartTime));
        return aiChatHistoryList;
    }

    /**
     * 从DB获取最近已完成的聊天记录列表，按起始时间升序排列
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近已完成的聊天记录列表
     */
    @Override
    public List<AIChatHistoryDTO> getLatestFinishedChatHistoryList(String username, Integer start, Integer length) {
        List<AIChatHistoryDTO> aiChatHistoryList = aiChatHistoryDAO.getLatestFinishedChatHistoryList(
            username,
            start,
            length
        );
        aiChatHistoryList.sort(Comparator.comparing(AIChatHistoryDTO::getStartTime));
        return aiChatHistoryList;
    }

    @Override
    public AIChatHistoryDTO getChatHistory(String username, Long id) {
        return aiChatHistoryDAO.getChatHistory(username, id);
    }

    /**
     * 从DB中分批软删除聊天记录（优先删除创建时间较早的）
     *
     * @param username 用户名
     * @return 删除的记录条数
     */
    @Override
    public int softDeleteChatHistory(String username) {
        int batchSize = 1000;
        int deletedCount;
        int deletedTotalCount = 0;
        do {
            deletedCount = aiChatHistoryDAO.softDeleteChatHistory(username, batchSize);
            deletedTotalCount += deletedCount;
        } while (deletedCount == batchSize);
        log.info("{} chat history of user {} soft deleted", deletedTotalCount, username);
        return deletedTotalCount;
    }
}
