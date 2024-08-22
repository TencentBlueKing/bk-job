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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.tencent.bk.job.analysis.consts.AIChatStatusEnum;
import com.tencent.bk.job.analysis.listener.event.AIChatOperationEvent;
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.mq.AIChatOperationEventDispatcher;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import com.tencent.bk.job.analysis.service.ai.context.model.AsyncConsumerAndProducerPair;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AIChatHistoryService aiChatHistoryService;
    private final AIAnswerHandler aiAnswerHandler;
    private final AIService aiService;
    private final AIChatOperationEventDispatcher aiChatOperationEventDispatcher;
    private final ConcurrentHashMap<Long, CompletableFuture<String>> futureMap = new ConcurrentHashMap<>();

    @Autowired
    public ChatServiceImpl(AIChatHistoryService aiChatHistoryService,
                           AIAnswerHandler aiAnswerHandler,
                           AIService aiService, AIChatOperationEventDispatcher aiChatOperationEventDispatcher) {
        this.aiChatHistoryService = aiChatHistoryService;
        this.aiAnswerHandler = aiAnswerHandler;
        this.aiService = aiService;
        this.aiChatOperationEventDispatcher = aiChatOperationEventDispatcher;
    }

    @Override
    public AIChatRecord chatWithAI(String username, String userInput) {
        Long startTime = System.currentTimeMillis();
        // 1.保存初始聊天记录
        AIChatHistoryDTO aiChatHistoryDTO = aiChatHistoryService.buildAIChatHistoryDTO(
            username,
            startTime,
            userInput,
            userInput,
            AIChatStatusEnum.INIT.getStatus(),
            null
        );
        Long historyId = aiChatHistoryService.insertChatHistory(aiChatHistoryDTO);
        aiChatHistoryDTO.setId(historyId);
        return aiChatHistoryDTO.toAIChatRecord();
    }

    @Override
    public List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length) {
        return aiChatHistoryService.getLatestChatHistoryList(username, start, length);
    }

    @Override
    public StreamingResponseBody generateChatStream(String username, Long recordId) {
        // 1.获取最近已完成的聊天记录作为上下文
        List<AIChatHistoryDTO> chatHistoryDTOList = aiChatHistoryService.getLatestFinishedChatHistoryList(
            username,
            0,
            5
        );
        chatHistoryDTOList.sort(Comparator.comparing(AIChatHistoryDTO::getStartTime));
        // 2.查出指定的聊天记录
        AIChatHistoryDTO currentChatHistoryDTO = getChatHistory(username, recordId);
        int affectedNum = aiChatHistoryService.setAIAnswerReplying(recordId);
        if (affectedNum == 0) {
            log.info("AIAnswer is already replying, re-reply, recordId={}", recordId);
        }
        // 3.将AI回答流数据与接口输出流进行同步对接
        int inMemoryMessageMaxNum = 10000;
        AIAnswerStreamSynchronizer aiAnswerStreamSynchronizer = new AIAnswerStreamSynchronizer(inMemoryMessageMaxNum);
        AsyncConsumerAndProducerPair consumerAndProducerPair =
            aiAnswerStreamSynchronizer.buildAsyncConsumerAndProducerPair();
        CompletableFuture<String> future = aiService.getAIAnswerStream(
            chatHistoryDTOList,
            currentChatHistoryDTO.getAiInput(),
            consumerAndProducerPair.getConsumer()
        );
        future.whenComplete((content, throwable) -> {
            aiAnswerStreamSynchronizer.triggerEndEvent();
            // 4.处理AI回复内容
            aiAnswerHandler.handleAIAnswer(recordId, content, throwable);
            futureMap.remove(recordId);
        });
        futureMap.put(recordId, future);
        return consumerAndProducerPair.getProducer();
    }

    private AIChatHistoryDTO getChatHistory(String username, Long recordId) {
        AIChatHistoryDTO chatHistoryDTO = aiChatHistoryService.getChatHistory(username, recordId);
        if (chatHistoryDTO == null) {
            throw new NotFoundException(
                ErrorCode.AI_CHAT_HISTORY_NOT_FOUND_BY_ID,
                new String[]{String.valueOf(recordId)}
            );
        }
        return chatHistoryDTO;
    }

    public boolean terminateChat(String username, Long recordId) {
        CompletableFuture<String> future = futureMap.get(recordId);
        if (future == null) {
            log.info("Cannot find future for recordId={}, chat maybe finished or canceled just now", recordId);
            return false;
        }
        boolean result = future.cancel(true);
        log.info("Terminate chat, username={}, recordId={}, result={}", username, recordId, result);
        return result;
    }

    public boolean triggerTerminateChat(String username, Long recordId) {
        AIChatHistoryDTO chatHistoryDTO = getChatHistory(username, recordId);
        if (!chatHistoryDTO.isInitOrReplying()) {
            log.info("Cannot terminate chat, chat is already finished or canceled, recordId={}", recordId);
            return false;
        }
        aiChatOperationEventDispatcher.broadCastAIChatOperationEvent(
            AIChatOperationEvent.terminateChat(username, recordId)
        );
        return true;
    }
}
