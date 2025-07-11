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
import com.tencent.bk.job.analysis.model.dto.AIPromptDTO;
import com.tencent.bk.job.analysis.model.web.resp.AIAnswer;

import java.util.List;

public interface AIChatHistoryService {
    /**
     * 构建AI聊天记录
     *
     * @param username    用户名
     * @param appId       Job业务ID
     * @param startTime   开始时间
     * @param aiPromptDTO AI提示符信息
     * @param status      对话状态
     * @param aiAnswer    AI回答
     * @return AI聊天记录
     */
    AIChatHistoryDTO buildAIChatHistoryDTO(String username,
                                           Long appId,
                                           Long startTime,
                                           AIPromptDTO aiPromptDTO,
                                           Integer status,
                                           AIAnswer aiAnswer);

    /**
     * 插入聊天记录
     *
     * @param aiChatHistoryDTO AI聊天记录
     * @return 插入记录的id
     */
    Long insertChatHistory(AIChatHistoryDTO aiChatHistoryDTO);

    /**
     * 判断用户是否存在聊天记录
     *
     * @param username 用户名
     * @return 是否存在聊天记录
     */
    boolean existsChatHistory(String username);

    /**
     * 更新聊天记录状态为正在回答中
     *
     * @param historyId AI聊天记录ID
     * @return 受影响的行数
     */
    int setAIAnswerReplying(Long historyId);

    /**
     * 更新聊天记录状态
     *
     * @param historyId AI聊天记录ID
     * @param aiAnswer  AI回答内容
     * @return 受影响的行数
     */
    int finishAIAnswer(Long historyId, AIAnswer aiAnswer);

    /**
     * 终止聊天
     *
     * @param historyId AI聊天记录ID
     * @return 受影响的行数
     */
    int terminateAIAnswer(Long historyId);


    /**
     * 获取最近的聊天记录列表
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近的聊天记录列表
     */
    List<AIChatHistoryDTO> getLatestChatHistoryList(String username, Integer start, Integer length);

    /**
     * 获取最近已完成的聊天记录列表
     *
     * @param username 用户名
     * @param start    起始位置
     * @param length   长度
     * @return 最近的聊天记录列表
     */
    List<AIChatHistoryDTO> getLatestFinishedChatHistoryList(String username, Integer start, Integer length);

    /**
     * 根据用户名与ID获取聊天记录
     *
     * @param username 用户名
     * @param id       ID
     * @return 聊天记录
     */
    AIChatHistoryDTO getChatHistory(String username, Long id);

    /**
     * 软删除聊天记录
     *
     * @param username 用户名
     * @return 删除的聊天记录数量
     */
    int softDeleteChatHistory(String username);
}
