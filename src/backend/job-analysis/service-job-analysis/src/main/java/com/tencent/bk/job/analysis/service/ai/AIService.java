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

package com.tencent.bk.job.analysis.service.ai;

import com.tencent.bk.job.analysis.model.dto.AIChatHistoryDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface AIService {

    /**
     * 根据用户输入获取AI回答
     * 注意：默认使用当前线程上下文中的请求Cookie中的bk_ticket/bk_token调用大模型接口，
     * 非HTTP请求处理线程中调用需要额外实现登录态传递逻辑
     *
     * @param chatHistoryDTOList 历史聊天记录
     * @param userInput          用户输入
     * @return AI回答结果
     */
    AIAnswer getAIAnswer(List<AIChatHistoryDTO> chatHistoryDTOList, String userInput);

    /**
     * 根据用户输入获取AI回答（不传入历史聊天记录）
     * 注意：默认使用当前线程上下文中的请求Cookie中的bk_ticket/bk_token调用大模型接口，
     * 非HTTP请求处理线程中调用需要额外实现登录态传递逻辑
     *
     * @param userInput 用户输入
     * @return AI回答结果
     */
    AIAnswer getAIAnswer(String userInput);

    /**
     * 获取AI回答流（流式接口）
     *
     * @param chatHistoryDTOList  历史聊天记录
     * @param userInput           用户输入
     * @param partialRespConsumer AI回答流回调
     * @return AI回答结果Future
     */
    CompletableFuture<String> getAIAnswerStream(List<AIChatHistoryDTO> chatHistoryDTOList,
                                                String userInput,
                                                Consumer<String> partialRespConsumer);
}
