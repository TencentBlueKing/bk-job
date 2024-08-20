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
import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;
import com.tencent.bk.job.analysis.model.web.resp.AIChatRecord;
import com.tencent.bk.job.analysis.service.ai.AIChatHistoryService;
import com.tencent.bk.job.analysis.service.ai.AIService;
import com.tencent.bk.job.analysis.service.ai.ChatService;
import com.tencent.bk.job.analysis.service.ai.context.model.MessagePartEvent;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final AIChatHistoryService aiChatHistoryService;
    private final AIService aiService;

    @Autowired
    public ChatServiceImpl(AIChatHistoryService aiChatHistoryService,
                           AIService aiService) {
        this.aiChatHistoryService = aiChatHistoryService;
        this.aiService = aiService;
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
    public StreamingResponseBody getChatStream(String username, Long recordId) {
        // 1.获取最近已完成的聊天记录作为上下文
        List<AIChatHistoryDTO> chatHistoryDTOList = aiChatHistoryService.getLatestFinishedChatHistoryList(
            username,
            0,
            5
        );
        chatHistoryDTOList.sort(Comparator.comparing(AIChatHistoryDTO::getStartTime));
        // 2.查出指定的聊天记录
        AIChatHistoryDTO currentChatHistoryDTO = aiChatHistoryService.getChatHistory(username, recordId);
        if (currentChatHistoryDTO == null) {
            throw new NotFoundException(
                ErrorCode.AI_CHAT_HISTORY_NOT_FOUND_BY_ID,
                new String[]{String.valueOf(recordId)}
            );
        }
        int affectedNum = aiChatHistoryService.setAIAnswerReplying(recordId);
        if (affectedNum == 0) {
            log.info("AIAnswer is already replying, re-reply, recordId={}", recordId);
        }
        // 3.获取AI回答放入阻塞队列
        LinkedBlockingQueue<MessagePartEvent> messageQueue = new LinkedBlockingQueue<>(10000);
        AtomicBoolean isFinished = new AtomicBoolean(false);
        StreamingResponseBody streamingResponseBody = outputStream -> {
            while (!isFinished.get()) {
                try {
                    MessagePartEvent event = messageQueue.take();
                    if (event.isEnd()) {
                        break;
                    }
                    String partMessage = event.getMessagePart();
                    Response<AIAnswer> respBody = Response.buildSuccessResp(AIAnswer.successAnswer(partMessage));
                    String message = JsonUtils.toJson(respBody) + "\n";
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (InterruptedException e) {
                    log.debug("Interrupted when take message from queue", e);
                } catch (IOException e) {
                    log.error("Write resp to output stream failed", e);
                }
            }
            outputStream.close();
        };
        Consumer<String> partialRespConsumer =
            messagePart -> messageQueue.offer(MessagePartEvent.normalEvent(messagePart));
        CompletableFuture<String> future = aiService.getAIAnswerStream(
            chatHistoryDTOList,
            currentChatHistoryDTO.getAiInput(),
            partialRespConsumer
        );
        future.whenComplete((content, throwable) -> {
            isFinished.set(true);
            messageQueue.offer(MessagePartEvent.endEvent());
            // 4.将AI回答写入DB
            AIAnswer aiAnswer;
            if (throwable == null) {
                aiAnswer = AIAnswer.successAnswer(content);
            } else {
                aiAnswer = AIAnswer.failAnswer(throwable.getMessage());
            }
            int affectedRow = aiChatHistoryService.finishAIAnswer(recordId, aiAnswer);
            log.info(
                "AIAnswer finished, recordId={}, length={}, affectedRow={}",
                recordId,
                content.length(),
                affectedRow
            );
        });
        return streamingResponseBody;
    }

}
